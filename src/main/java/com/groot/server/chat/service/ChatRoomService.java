package com.groot.server.chat.service;

import com.groot.server.chat.database.ChatClient;
import org.json.simple.JSONObject;

import java.net.Socket;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public interface ChatRoomService {
    void getAllChatRooms(Socket socket);
    void getAllClients(Socket socket);
    boolean createNewChatRoom(Socket socket, String roomId);
    void createMainHallChatRoom();
    boolean joinChatRoom(Socket socket, String roomId);
    boolean deleteChatRoom(Socket socket, String roomId);
    void sendMessage(Socket socket, String content);
    void addToMainHall(ChatClient client);
    void broadCastMessage(JSONObject message, String roomId, String identity);
}
