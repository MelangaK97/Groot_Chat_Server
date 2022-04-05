package com.groot.server.chat.service;

import java.net.Socket;

/**
 * @author Melanga Kasun
 * @date (Sun) 20-Feb-2022
 */
public interface CoordinationService {
    void notifyClientCreation(String identity);
    void notifyChatRoomCreation(String roomId);
    String getChatRoomOwningServer(String roomId, String identity);
    void informChatRoomDeletion(String roomId);
    void informClientDeletion(String identity);
    boolean isLeaderAcceptedClient(String identity);
    boolean isLeaderAcceptedChatRoom(String roomId);
    void updateGlobalClients(String identity, boolean isAdded);
    void updateGlobalChatRooms(String roomId, String serverId, boolean isAdded);
    void sendCurrentView(Socket socket, String serverId);
    void participateElection(Socket socket);
    void updateLeader(String leader);
    void sendGlobalData(Socket socket);
}
