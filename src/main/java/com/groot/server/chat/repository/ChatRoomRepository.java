package com.groot.server.chat.repository;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.ChatClient;
import com.groot.server.chat.database.ChatRoom;
import com.groot.server.chat.database.DataStore;

import java.util.List;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class ChatRoomRepository {
    private DataStore store;

    public void createChatRoom(ChatRoom chatRoom) {
        store = DataStoreConfig.getInstance();
        store.addChatRoom(chatRoom);
    }

    public ChatRoom getChatRoomById(String roomId) {
        store = DataStoreConfig.getInstance();
        return store.getChatRoom(roomId);
    }

    public void removeClientFromChatRoom(String roomId, ChatClient client) {
        store = DataStoreConfig.getInstance();
        store.removeFromChatRoom(roomId, client);
    }

    public void addClientToChatRoom(String roomId, ChatClient client) {
        store = DataStoreConfig.getInstance();
        store.addToChatRoom(roomId, client);
    }

    public List<ChatClient> getAllClientsOfChatRoom(String roomId) {
        store = DataStoreConfig.getInstance();
        return store.getAllClientsOfChatRoom(roomId);
    }

    public List<String> getAllChatRoomIds() {
        store = DataStoreConfig.getInstance();
        return store.getAllChatRoomIds();
    }

    public void deleteChatRoom(String roomId) {
        store = DataStoreConfig.getInstance();
        store.deleteChatRoom(roomId);
    }
}
