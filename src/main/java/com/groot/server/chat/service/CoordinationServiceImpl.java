package com.groot.server.chat.service;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.ChatServer;
import com.groot.server.chat.database.DataStore;
import com.groot.server.chat.repository.CoordinationRepository;
import com.groot.server.chat.repository.LeaderRepository;
import com.groot.server.chat.util.MessageGenerator;
import com.groot.server.chat.util.MessageReader;
import com.groot.server.chat.util.MessageSender;
import com.groot.server.chat.util.ServerBroadcaster;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

/**
 * @author Melanga Kasun
 * @date (Sun) 20-Feb-2022
 */
public class CoordinationServiceImpl implements CoordinationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationServiceImpl.class);
    private final CoordinationRepository repository = new CoordinationRepository();
    private final LeaderRepository leaderRepository = new LeaderRepository();
    private final FastBullyElectionService electionService = new FastBullyElectionServiceImpl();

    @Override
    public void notifyClientCreation(String identity) {
        try {
            Socket socket = getLeaderSocket();
            LOGGER.info("Notifying the leader about the client {} creation ...", identity);
            MessageSender.send(socket.getOutputStream(), MessageGenerator.addGlobalClientMessage(identity));
        } catch (IOException e) {
            LOGGER.error("Client creation notifying error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void notifyChatRoomCreation(String roomId) {
        try {
            Socket socket = getLeaderSocket();
            LOGGER.info("Notifying the leader about the chat room {} creation ...", roomId);
            MessageSender.send(socket.getOutputStream(), MessageGenerator.addGlobalChatRoomMessage(roomId));
        } catch (Exception e) {
            LOGGER.error("Chat room creation notifying error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public String getChatRoomOwningServer(String roomId, String identity) {
        long t1 = 10000;
        try {
            if (DataStoreConfig.getInstance().isLeaderMyself()) {
                return repository.getGlobalServerByChatRoomId(roomId);
            } else {
                Socket socket = getLeaderSocket();
                MessageSender.send(socket.getOutputStream(), MessageGenerator.requestChatRoomOwningServerMessage(roomId));
                LOGGER.info("Requesting the owner of the chat room {} from leader ...", roomId);

                // wait T1 time for the response
                Thread.sleep(t1);
                JSONObject response = MessageReader.read(socket.getInputStream());
                LOGGER.info("Received the owner details from leader ...");
                return response.get("serverid").toString();
            }
        } catch (Exception e) {
            LOGGER.error("Room owning server finding error ... {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void informClientDeletion(String identity) {
        try {
            ServerBroadcaster.broadcast(MessageGenerator.deleteClientMessageToServer(identity,
                    DataStoreConfig.getInstance().getCurrent().getServerId()));
            LOGGER.info("Informed neighbours about the client {} deletion ...", identity);
        } catch (Exception e) {
            LOGGER.error("Client creation informing error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void informChatRoomDeletion(String roomId) {
        try {
            ServerBroadcaster.broadcast(MessageGenerator.deleteRoomMessageToServer(roomId,
                    DataStoreConfig.getInstance().getCurrent().getServerId()));
            LOGGER.info("Informed neighbours about the chat room {} deletion ...", roomId);
        } catch (Exception e) {
            LOGGER.error("Chat room creation informing error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isLeaderAcceptedClient(String identity) {
        try {
            if (DataStoreConfig.getInstance().isLeaderMyself()) {
                return !repository.isGlobalClient(identity) && !leaderRepository.isPendingClient(identity);
            } else {
                Socket socket = getLeaderSocket();
                MessageSender.send(socket.getOutputStream(),
                        MessageGenerator.clientAvailabilityMessage(identity, "forwarded",
                                DataStoreConfig.getInstance().getCurrent().getServerId()));
                LOGGER.info("Requesting client creation permission from leader ...");
                JSONObject response = MessageReader.read(socket.getInputStream());
                socket.close();
                LOGGER.info("Received the permission: {} from leader ...", response.get("approved"));
                return response.get("identity").equals(identity) && response.get("approved").equals("true");
            }
        } catch (Exception e) {
            LOGGER.error("Client creation acceptance error ... {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isLeaderAcceptedChatRoom(String roomId) {
        try {
            if (DataStoreConfig.getInstance().isLeaderMyself()) {
                return !repository.isGlobalChatRoom(roomId) &&
                        !leaderRepository.isPendingChatRoom(roomId, DataStoreConfig.getInstance().getCurrent().getServerId());
            } else {
                Socket socket = getLeaderSocket();
                MessageSender.send(socket.getOutputStream(),
                        MessageGenerator.chatRoomAvailabilityMessage(roomId, "forwarded",
                                DataStoreConfig.getInstance().getCurrent().getServerId()));
                LOGGER.info("Requesting chat room creation permission from leader ...");
                JSONObject response = MessageReader.read(socket.getInputStream());
                socket.close();
                LOGGER.info("Received the permission: {} from leader ...", response.get("approved"));
                return response.get("roomid").equals(roomId) && response.get("approved").equals("true");
            }
        } catch (Exception e) {
            LOGGER.error("Chat room creation acceptance error ... {}", e.getMessage(), e);
            return false;
        }
    }

    private Socket getLeaderSocket() {
        try {
            DataStore store = DataStoreConfig.getInstance();
            ChatServer leader = store.getLeader();
            if (Objects.nonNull(leader)) {
                return new Socket(leader.getServerAddress(), leader.getCoordinationPort());
            } else {
                LOGGER.error("Unable to find the leader ...");
                electionService.startElection();
            }
        } catch (Exception e) {
            LOGGER.error("Leader socket getting error ... {}", e.getMessage(), e);
            electionService.startElection();
        }
        return getLeaderSocket();
    }

    @Override
    public void updateGlobalClients(String identity, boolean isAdded) {
        try {
            if (isAdded) {
                // add client to the global list
                repository.addNewGlobalClient(identity);
                LOGGER.info("Successfully added the client {} to global list ...", identity);
            } else {
                // remove client from the global list
                repository.deleteGlobalClient(identity);
                LOGGER.info("Successfully removed the client {} from global list ...", identity);
            }
        } catch (Exception e) {
            LOGGER.error("Global clients updating error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void updateGlobalChatRooms(String roomId, String serverId, boolean isAdded) {
        try {
            if (isAdded) {
                // add chat room to the global list
                repository.addNewGlobalChatRoom(roomId, serverId);
                LOGGER.info("Successfully added the chat room {} to global list ...", roomId);
            } else {
                // remove chat room from the global list
                repository.deleteGlobalChatRoom(roomId);
                LOGGER.info("Successfully removed the chat room {} from global list ...", roomId);
            }
        } catch (Exception e) {
            LOGGER.error("Global chat rooms updating error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendCurrentView(Socket socket, String serverId) {
        try {
            DataStore store = DataStoreConfig.getInstance();
            MessageSender.send(socket.getOutputStream(),
                    MessageGenerator.viewMessage(new ArrayList<>(store.getViews().keySet())));
            LOGGER.info("Sent views to the server {} ...", serverId);
            store.addView(store.getNeighbourById(serverId));
            LOGGER.info("Added server {} into views ...", serverId);
            store.addNewGlobalChatRoom("MainHall-" + serverId, serverId);
        } catch (Exception e) {
            LOGGER.error("Current view setting error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void participateElection(Socket socket) {
        long t4 = 10000;
        try {
            MessageSender.send(socket.getOutputStream(), MessageGenerator.answerMessage());
            socket.setSoTimeout(Math.toIntExact(t4));
            LOGGER.info("Sent the availability for the election to the requested server ...");
            JSONObject response = MessageReader.read(socket.getInputStream());

            DataStore store = DataStoreConfig.getInstance();
            if (Objects.isNull(response)) {
                LOGGER.info("Restarting the election since the coordinator is not responding ...");
                electionService.startElection();
            } else if (response.get("type").equals("nomination")) {
                // update lower priority neighbours
                LOGGER.info("Nominated as the leader by server {} ...", response.get("serverid"));
                electionService.broadcastToLowerNeighbours();
                MessageSender.send(socket.getOutputStream(),
                        MessageGenerator.coordinatorMessage(store.getCurrent().getServerId()));
            } else if (response.get("type").equals("coordinator")) {
                store.setLeader(store.getNeighbourById(response.get("leader").toString()));
                LOGGER.info("Updated leader as server {} ...", store.getLeader().getServerId());
            }
        } catch (Exception e) {
            LOGGER.error("Election participation error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void updateLeader(String leader) {
        try {
            DataStore store = DataStoreConfig.getInstance();
            store.setLeader(store.getNeighbourById(leader));
            LOGGER.info("Updated leader as server {} ...", leader);
        } catch (Exception e) {
            LOGGER.error("Leader updating error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendGlobalData(Socket socket) {
        try {
            DataStore store = DataStoreConfig.getInstance();
            LOGGER.info("Sending global data to requested server ...");
            MessageSender.send(socket.getOutputStream(),
                    MessageGenerator.globalDataMessage(store.getGlobalClients(), store.getGlobalChatRooms()));
        } catch (Exception e) {
            LOGGER.error("Global data setting error ... {}", e.getMessage(), e);
        }
    }
}
