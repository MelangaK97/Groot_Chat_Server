package com.groot.server.chat.database;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Melanga Kasun
 * @date (Sun) 20-Feb-2022
 */
public class ChatRoom {
    private final String roomId;
    private final String owner;
    private final List<ChatClient> clients;

    public ChatRoom(String roomId, String owner) {
        this.roomId = roomId;
        this.owner = owner;
        this.clients = new ArrayList<>();
    }

    public String getOwner() {
        return owner;
    }

    public String getRoomId() {
        return roomId;
    }

    public void removeClient(ChatClient client) {
        this.clients.remove(client);
    }

    public void addClient(ChatClient client) {
        this.clients.add(client);
    }

    public List<ChatClient> getAllClients() {
        return this.clients;
    }
}
