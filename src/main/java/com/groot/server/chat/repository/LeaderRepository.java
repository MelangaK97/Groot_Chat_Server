package com.groot.server.chat.repository;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.DataStore;

/**
 * @author Melanga Kasun
 * @date (Wed) 02-Mar-2022
 */
public class LeaderRepository {
    private DataStore store;

    public boolean isPendingClient(String identity) {
        store = DataStoreConfig.getInstance();
        return store.isPendingClient(identity);
    }

    public void removePendingClient(String identity) {
        store = DataStoreConfig.getInstance();
        store.removePendingClient(identity);
    }

    public boolean isPendingChatRoom(String roomId, String serverId) {
        store = DataStoreConfig.getInstance();
        return store.isPendingChatRoom(roomId, serverId);
    }

    public String removePendingChatRoom(String roomId) {
        store = DataStoreConfig.getInstance();
        return store.removePendingChatRoom(roomId);
    }
}
