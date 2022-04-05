package com.groot.server.chat.service;

import java.net.Socket;

/**
 * @author Melanga Kasun
 * @date (Wed) 02-Mar-2022
 */
public interface LeaderService {
    void checkClientRegistered(Socket socket, String identity, String serverId);
    void checkChatRoomRegistered(Socket socket, String roomId, String serverId);
    void createGlobalClient(String identity);
    void createGlobalChatRoom(String roomId);
    void getServerByChatRoomId(Socket socket, String roomId);
}
