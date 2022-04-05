package com.groot.server.chat;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.ChatServer;
import com.groot.server.chat.database.DataStore;
import com.groot.server.chat.handler.ClientServerHandler;
import com.groot.server.chat.handler.ServerServerHandler;
import com.groot.server.chat.service.*;
import com.groot.server.chat.util.*;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class ChatServerRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServerRunner.class);
    private static final ChatRoomService chatRoomService = new ChatRoomServiceImpl();
    private static final FastBullyElectionService electionService = new FastBullyElectionServiceImpl();
    private static final HeartBeatService heartBeatService = new HeartBeatServiceImpl();

    public static void main(String[] args) {
        LOGGER.info("Server started ...");

        // load command line args
        CmdLineReader reader = new CmdLineReader();
        CmdLineParser parser = new CmdLineParser(reader);
        try {
            parser.parseArgument(args);
            String serverId = reader.getServerId();
            String serversConfiguration = reader.getServersConfiguration();
            List<ChatServer> availableServers = ConfigFileReader.read(serversConfiguration);
            initializeDataStore(availableServers, serverId);
            DataStore store = DataStoreConfig.getInstance();

            if (Objects.nonNull(store) && Objects.nonNull(store.getCurrent())) {
                ChatServer current = store.getCurrent();
                LOGGER.info("Server {} started on {} with clients port: {} and coordination port {} ... ",
                        current.getServerId(), current.getServerAddress(), current.getClientsPort(), current.getCoordinationPort());
                chatRoomService.createMainHallChatRoom();

                // A process Pi recovers from failure
                electionService.recoverFromFailure();
                startClientServerCommunication(current.getClientsPort());
                startServerServerCommunication(current.getCoordinationPort());

                // start heart beating
                new Thread(() -> {
                    while (true) {
                        try {
                            heartBeatService.start();
                            Thread.sleep(60 * 1000);
                        } catch (Exception e) {
                            LOGGER.error("An error occurred while heart beating ... {}", e.getMessage(), e);
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            LOGGER.error("Server starting error ... {}", e.getMessage(), e);
        }
    }

    private static void initializeDataStore(List<ChatServer> servers, String serverId) {
        DataStore store = DataStoreConfig.getInstance();
        for (ChatServer server : servers) {
            if (server.getServerId().equals(serverId)) {
                store.setCurrent(server);
            } else {
                store.addNeighbour(server);
                store.addView(server);
            }
        }
    }

    private static void startClientServerCommunication(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        new Thread(() -> {
            Socket socket;
            ClientServerHandler clientServerHandler;
            while (true) {
                try {
                    socket = serverSocket.accept();
                    LOGGER.info("New client connected ...");
                    clientServerHandler = new ClientServerHandler(socket);
                    clientServerHandler.start();
                } catch (Exception e) {
                    LOGGER.error("New client connection error ... {}", e.getMessage(), e);
                }
            }
        }).start();
    }

    private static void startServerServerCommunication(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        new Thread(() -> {
            Socket socket;
            ServerServerHandler serverServerHandler;
            while (true) {
                try {
                    socket = serverSocket.accept();
                    LOGGER.info("New server request received ...");
                    serverServerHandler = new ServerServerHandler(socket);
                    serverServerHandler.start();
                } catch (Exception e) {
                    LOGGER.error("New server connection error ... {}", e.getMessage(), e);
                }
            }
        }).start();
    }
}
