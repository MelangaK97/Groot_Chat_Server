package com.groot.server.chat.database;

import java.net.Socket;

/**
 * @author Melanga Kasun
 * @date (Sun) 20-Feb-2022
 */
public class ChatClient {
    private final String identity;
    private final Socket socket;
    private String roomId;

    public ChatClient(String identity, String roomId, Socket socket) {
        this.identity = identity;
        this.roomId = roomId;
        this.socket = socket;
    }

    public String getIdentity() {
        return identity;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
