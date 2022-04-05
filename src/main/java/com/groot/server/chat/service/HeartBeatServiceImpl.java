package com.groot.server.chat.service;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.ChatServer;
import com.groot.server.chat.database.DataStore;
import com.groot.server.chat.util.MessageGenerator;
import com.groot.server.chat.util.MessageReader;
import com.groot.server.chat.util.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HeartBeatServiceImpl implements HeartBeatService{
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatServiceImpl.class);
    private final FastBullyElectionService electionService = new FastBullyElectionServiceImpl();

    @Override
    public void start(){
        long t1 = 10000;
        DataStore store = DataStoreConfig.getInstance();
        if (store.isLeaderMyself()) {
            LOGGER.info("Sending heart beat to all views ...");
            List<String> disconnected = new ArrayList<>();
            store.getViews().forEach((identity, server) -> {
                try {
                    Socket socket = new Socket(server.getServerAddress(), server.getCoordinationPort());
                    LOGGER.info("Sending heart beat to server {} ...", identity);
                    MessageSender.send(socket.getOutputStream(),
                            MessageGenerator.heartBeatMessage(store.getCurrent().getServerId()));
                    socket.setSoTimeout(Math.toIntExact(t1));
                    MessageReader.read(socket.getInputStream());
                    LOGGER.info("Received response heartbeat from server {} ...", identity);
                    socket.close();
                } catch (Exception e) {
                    LOGGER.error("Communicating with the server {} error ... {}", identity, e.getMessage());
                    disconnected.add(identity);
                }
            });
            if (!disconnected.isEmpty()) {
                LOGGER.info("Removing disconnected servers ...");
                store.removeViews(disconnected);
            }
        } else {
            ChatServer leader = store.getLeader();
            try {
                Socket socket = new Socket(leader.getServerAddress(), leader.getCoordinationPort());
                LOGGER.info("Sending heart beat to leader server {} ...", leader.getServerId());
                MessageSender.send(socket.getOutputStream(),
                        MessageGenerator.heartBeatMessage(store.getCurrent().getServerId()));
                socket.setSoTimeout(Math.toIntExact(t1));
                MessageReader.read(socket.getInputStream());
                LOGGER.info("Received response heartbeat from leader ...");
                socket.close();
            } catch (Exception e) {
                LOGGER.error("Communicate with the leader error ... {}", e.getMessage());
                electionService.startElection();
            }
        }
    }

    @Override
    public void sendResponse(Socket socket, String serverId) {
        try {
            MessageSender.send(socket.getOutputStream(), MessageGenerator.heartBeatResponse(serverId));
            LOGGER.info("Sent response for heartbeat...");
        } catch (Exception e) {
            LOGGER.error("Response sending error ... {}", e.getMessage(), e);
        }
    }
}
