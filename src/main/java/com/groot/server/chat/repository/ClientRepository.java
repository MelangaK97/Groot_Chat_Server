package com.groot.server.chat.repository;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.ChatClient;
import com.groot.server.chat.database.DataStore;

import java.net.Socket;
import java.util.Objects;

/**
 * @author Melanga Kasun
 * @date (Sat) 19-Feb-2022
 */
public class ClientRepository {
    private DataStore store;

    public void createClient(ChatClient client) {
        store = DataStoreConfig.getInstance();
        store.addClient(client);
    }

    public ChatClient getClientById(String identity) {
        store = DataStoreConfig.getInstance();
        return store.getClient(identity);
    }

    public void removeClient(String identity) {
        store = DataStoreConfig.getInstance();
        store.removeClient(identity);
    }

    public ChatClient getClientBySocket(Socket socket) {
        store = DataStoreConfig.getInstance();
        for (ChatClient client : store.getClients().values()) {
            if (Objects.equals(client.getSocket(), socket)) {
                return client;
            }
        }
        return null;
    }
}
