package com.groot.server.chat.repository;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.DataStore;

import java.util.List;
import java.util.Map;

/**
 * @author Melanga Kasun
 * @date (Wed) 02-Mar-2022
 */
public class CoordinationRepository {
    private DataStore store;

    public void addNewGlobalClient(String identity) {
        store = DataStoreConfig.getInstance();
        store.addNewGlobalClient(identity);
    }

    public boolean isGlobalClient(String identity) {
        store = DataStoreConfig.getInstance();
        return store.isGlobalClient(identity);
    }

    public void deleteGlobalClient(String identity) {
        store = DataStoreConfig.getInstance();
        store.deleteGlobalClient(identity);
    }

    public void updateGlobalClients(List<String> clients) {
        store = DataStoreConfig.getInstance();
        store.updateGlobalClients(clients);
    }

    public void addNewGlobalChatRoom(String roomId, String serverId) {
        store = DataStoreConfig.getInstance();
        store.addNewGlobalChatRoom(roomId, serverId);
    }

    public boolean isGlobalChatRoom(String roomId) {
        store = DataStoreConfig.getInstance();
        return store.isGlobalChatRoom(roomId);
    }

    public String getGlobalServerByChatRoomId(String roomId) {
        store = DataStoreConfig.getInstance();
        return store.getGlobalServerByChatRoomId(roomId);
    }

    public void deleteGlobalChatRoom(String roomId) {
        store = DataStoreConfig.getInstance();
        store.deleteGlobalChatRoom(roomId);
    }

    public void updateGlobalChatRooms(Map<String, String> chatRooms) {
        store = DataStoreConfig.getInstance();
        store.updateGlobalChatRooms(chatRooms);
    }
}
