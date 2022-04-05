package com.groot.server.chat.service;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.ChatServer;
import com.groot.server.chat.database.DataStore;
import com.groot.server.chat.util.*;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Melanga Kasun
 * @date (Tue) 22-Mar-2022
 */

public class FastBullyElectionServiceImpl implements FastBullyElectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FastBullyElectionServiceImpl.class);

    @Override
    public boolean startElection() {
        long t2 = 10000;
        try {
            LOGGER.info("Starting the election since the leader is not available ...");
            Map<String, Socket> answers = new HashMap<>();
            ExecutorService executorService = Executors.newCachedThreadPool();
            DataStore store = DataStoreConfig.getInstance();
            // Pi sends an election message to every process with higher priority number
            store.getNeighbours().forEach((identity, server) -> {
                if (server.getPriority() > store.getCurrent().getPriority()) {
                    executorService.execute(() -> {
                        try {
                            Socket socket = new Socket(server.getServerAddress(), server.getCoordinationPort());
                            // Pi sends an election message.
                            MessageSender.send(socket.getOutputStream(), MessageGenerator.electionMessage());
                            socket.setSoTimeout(Math.toIntExact(t2));
                            LOGGER.info("Sent election message to server {} ...", server.getServerId());
                            // Pi waits for view messages for the interval T2
                            JSONObject response = MessageReader.read(socket.getInputStream());
                            LOGGER.info("Received answer from server {} ...", server.getServerId());
                            if (Objects.nonNull(response) && response.get("type").equals("answer")) {
                                synchronized (answers) {
                                    answers.put(server.getServerId(), socket);
                                }
                            }
                        } catch (SocketTimeoutException e) {
                            LOGGER.error("Socket timed out ... {}", e.getMessage());
                        } catch (Exception e) {
                            LOGGER.error("An error occurred while connecting with server {} ... {}", identity, e.getMessage());
                        }
                    });
                }
            });
            // Pi waits for view messages for the interval T2
            LOGGER.info("Waiting for receiving the neighbour answers ...");
            executorService.shutdown();
            if (executorService.awaitTermination(t2, TimeUnit.MILLISECONDS)) {
                return electLeaderFromAnswers(answers);
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("Election starting error ... {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean electLeaderFromAnswers(Map<String, Socket> answers) {
        try {
            DataStore store = DataStoreConfig.getInstance();
            long t3 = 10000;
            if (answers.isEmpty()) {
                // Pi is the coordinator
                store.setLeader(store.getCurrent());
                LOGGER.info("Updated leader as myself since there are no answers ...");
                // update lower priority neighbours
                broadcastToLowerNeighbours();
                return true;
            }
            boolean isLeaderUpdated = false;
            while (!answers.isEmpty()) {
                String leaderServerId = comparePriorities(new ArrayList<>(answers.keySet()));
                JSONObject response;
                try {
                    // Pi determines the highest priority number of the answering processes
                    Socket socket = answers.get(leaderServerId);
                    MessageSender.send(socket.getOutputStream(),
                            MessageGenerator.nominationMessage(store.getCurrent().getServerId()));
                    LOGGER.info("Sent nomination message to server {} ...", leaderServerId);
                    // Pi waits for a coordinator message for the interval T3.
                    socket.setSoTimeout(Math.toIntExact(t3));
                    response = MessageReader.read(socket.getInputStream());
                    Thread.sleep(t3);
                    if (response.get("type").equals("coordinator")) {
                        socket.close();
                        isLeaderUpdated = true;
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    LOGGER.error("Socket timed out ... {}", e.getMessage());
                    answers.remove(leaderServerId);
                } catch (Exception e) {
                    LOGGER.error("An error occurred ... {}", e.getMessage(), e);
                }
            }
            if (!isLeaderUpdated) {
                LOGGER.info("Restart the election since the leader is not updated ...");
                startElection();
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Leader electing error ... {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void broadcastToLowerNeighbours() {
        DataStore store = DataStoreConfig.getInstance();
        store.getNeighbours().forEach((identity, server) -> new Thread(() -> {
            try {
                if (server.getPriority() < store.getCurrent().getPriority()) {
                    Socket socket = new Socket(server.getServerAddress(), server.getCoordinationPort());
                    MessageSender.send(socket.getOutputStream(),
                            MessageGenerator.coordinatorMessage(store.getCurrent().getServerId()));
                    LOGGER.info("Sent coordinator message to server {} ...", server.getServerId());
                }
            } catch (IOException e) {
                LOGGER.error("Message broadcasting error ... {}", e.getMessage(), e);
            }
        }).start());
    }

    @Override
    public void recoverFromFailure() {
        long t2 = 1000;
        try {
            DataStore store = DataStoreConfig.getInstance();
            ExecutorService executorService = Executors.newCachedThreadPool();
            HashMap<String, String> views = new HashMap<>();
            store.getNeighbours().forEach((identity, server) -> executorService.execute(() -> {
                try {
                    Socket socket = new Socket(server.getServerAddress(), server.getCoordinationPort());
                    // Pi sends an IamUp message.
                    MessageSender.send(socket.getOutputStream(), MessageGenerator.iamUpMessage(store.getCurrent().getServerId()));
                    LOGGER.info("Sent IamUp message to server {} ...", server.getServerId());
                    JSONObject response = MessageReader.read(socket.getInputStream());
                    LOGGER.info("Received views from server {} ...", server.getServerId());
                    if (Objects.nonNull(response) && response.get("type").equals("view")) {
                        store.addNewGlobalChatRoom("MainHall-"+identity, identity);
                        synchronized (views) {
                            views.put(identity, response.get("views").toString());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Unable tot connect to server {} ... {}", identity, e.getMessage());
                }
            }));
            // Pi waits for view messages for the interval T2
            LOGGER.info("Waiting for receiving the neighbour views ...");
            executorService.shutdown();
            if (executorService.awaitTermination(t2, TimeUnit.MILLISECONDS) && !views.isEmpty()) {
                LOGGER.info("Received the views of {} running server(s) ...", views.size());
                // Pi compares its view with the received views
                List<String> serverView = new ArrayList<>(views.values());
                List<String> currentView = new ArrayList<>(store.getViews().keySet());
                List<String> newViews = compareViews(currentView, serverView);
                if (!newViews.isEmpty()) {
                    // Pi updates its view.
                    updateView(newViews);
                }
                List<String> activeNeighbours = new ArrayList<>(views.keySet());
                String leaderServer = comparePriorities(activeNeighbours);
                ChatServer tmpServer;
                if (leaderServer.equals(store.getCurrent().getServerId())) {
                    // Pi sends a coordinator message to other processes with lower priority number
                    store.setLeader(store.getCurrent());
                    LOGGER.info("Updated leader as myself with the highest priority ...");
                    tmpServer = store.getNeighbourById(activeNeighbours.get(0));
                    ServerBroadcaster.broadcast(MessageGenerator.coordinatorMessage(store.getCurrent().getServerId()));
                } else {
                    tmpServer = store.getNeighbourById(leaderServer);
                    // Admit the highest priority numbered process as the coordinator.
                    store.setLeader(tmpServer);
                    LOGGER.info("Updated leader as server {} with the highest priority ...", leaderServer);
                }
                Socket tmpSocket = new Socket(tmpServer.getServerAddress(), tmpServer.getCoordinationPort());
                MessageSender.send(tmpSocket.getOutputStream(), MessageGenerator.requestGlobalsMessage());
                LOGGER.info("Retrieving global clients and chat rooms ...");
                JSONObject response = MessageReader.read(tmpSocket.getInputStream());
                if (Objects.nonNull(response) && response.get("type").equals("globals")) {
                    store.updateGlobalClients((List<String>) response.get("clients"));
                    store.updateGlobalChatRooms((Map<String, String>) response.get("chatrooms"));
                    LOGGER.info("Updated the global clients and chat rooms with server {} data ...", tmpServer.getServerId());
                }
            } else {
                // Pi is the coordinator
                store.setLeader(store.getCurrent());
                LOGGER.info("Updated leader as myself since there are no other views ...");
            }
        } catch (Exception e) {
            LOGGER.error("Failure recovery error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public List<String> compareViews(List<String> currentView, List<String> serverView) {
        LOGGER.info("Comparing the views ...");
        Set<String> serverViewSet = new HashSet<>();
        serverView.forEach(views -> Collections.addAll(serverViewSet, views.split(",")));
        for (String view : currentView) {
            if (serverViewSet.contains(view)) {
                serverViewSet.remove(view);
                currentView.remove(view);
            }
        }
        return new ArrayList<>(serverViewSet);
    }

    @Override
    public void updateView(List<String> newViews) {
        LOGGER.info("Updating the current view ...");
        DataStore store = DataStoreConfig.getInstance();
        newViews.forEach(serverId -> {
            try {
                store.addView(store.getNeighbourById(serverId));
            } catch (Exception e) {
                LOGGER.error("View updating error ... {}", e.getMessage(), e);
            }
        });
    }

    private String comparePriorities(List<String> neighbours) {
        LOGGER.info("Comparing the priorities ...");
        DataStore store = DataStoreConfig.getInstance();
        ChatServer leader = DataStoreConfig.getInstance().getCurrent();
        for (String neighbour : neighbours) {
            if (leader.getPriority() < store.getNeighbourById(neighbour).getPriority()) {
                leader = store.getNeighbourById(neighbour);
            }
        }
        return leader.getServerId();
    }
}
