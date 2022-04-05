package com.groot.server.chat.util;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.ChatServer;
import com.groot.server.chat.database.DataStore;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Melanga Kasun
 * @date (Thu) 03-Mar-2022
 */
public class ServerBroadcaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerBroadcaster.class);

    public static void broadcast(JSONObject message) throws InterruptedException {
        DataStore store = DataStoreConfig.getInstance();
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<String> disconnected = new ArrayList<>();
        for (ChatServer server : store.getViews().values()) {
            executorService.execute(() -> {
                try {
                    Socket socket = new Socket(server.getServerAddress(), server.getCoordinationPort());
                    MessageSender.send(socket.getOutputStream(), message);
                    LOGGER.info("Sent message to server {} ...", server.getServerId());
                } catch (Exception e) {
                    LOGGER.error("An error occurred while broadcasting to server {} ... {}",
                            server.getServerId(), e.getMessage());
                    disconnected.add(server.getServerId());
                }
            });
        }
        executorService.shutdown();
        if (executorService.awaitTermination(10000, TimeUnit.MILLISECONDS) && !disconnected.isEmpty()) {
            store.removeViews(disconnected);
        }
    }
}
