package com.groot.server.chat.util;

import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * @author Melanga Kasun
 * @date (Sat) 26-Feb-2022
 */
public class MessageGenerator {

    @SuppressWarnings("unchecked")
    public static JSONObject roomListMessage(List<String> roomList) {
        JSONObject message = new JSONObject();
        message.put("type", "roomlist");
        message.put("rooms", roomList);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject roomContentsMessage(String roomId, String owner, List<String> clients) {
        JSONObject message = new JSONObject();
        message.put("type", "roomcontents");
        message.put("roomid", roomId);
        message.put("owner", owner);
        message.put("identities", clients);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject createRoomMessage(String roomId, String isApproved) {
        JSONObject message = new JSONObject();
        message.put("type", "createroom");
        message.put("roomid", roomId);
        message.put("approved", isApproved);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject chatRoomAvailabilityMessage(String roomId, String isApproved, String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "createroom");
        message.put("roomid", roomId);
        message.put("approved", isApproved);
        message.put("serverid", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject addGlobalChatRoomMessage(String roomId) {
        JSONObject message = new JSONObject();
        message.put("type", "createroom");
        message.put("roomid", roomId);
        message.put("created", "true");
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject updateGlobalChatRoomsMessage(String roomId, String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "createroom");
        message.put("roomid", roomId);
        message.put("approved", "true");
        message.put("serverid", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject roomChangeMessage(String identity, String former, String roomId) {
        JSONObject message = new JSONObject();
        message.put("type", "roomchange");
        message.put("identity", identity);
        message.put("former", former);
        message.put("roomid", roomId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject routeMessage(String roomId, String serverAddress, int clientsPort) {
        JSONObject message = new JSONObject();
        message.put("type", "route");
        message.put("roomid", roomId);
        message.put("host", serverAddress);
        message.put("port", String.valueOf(clientsPort));
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject message(String identity, String content) {
        JSONObject message = new JSONObject();
        message.put("type", "message");
        message.put("identity", identity);
        message.put("content", content);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject newIdentityMessage(String isApproved) {
        JSONObject message = new JSONObject();
        message.put("type", "newidentity");
        message.put("approved", isApproved);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject clientAvailabilityMessage(String identity, String approved, String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "newidentity");
        message.put("identity", identity);
        message.put("approved", approved);
        message.put("serverid", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject addGlobalClientMessage(String identity) {
        JSONObject message = new JSONObject();
        message.put("type", "newidentity");
        message.put("identity", identity);
        message.put("created", "true");
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject updateGlobalClientsMessage(String identity) {
        JSONObject message = new JSONObject();
        message.put("type", "newidentity");
        message.put("identity", identity);
        message.put("approved", "true");
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject serverChangeMessage(String serverId, String isApproved) {
        JSONObject message = new JSONObject();
        message.put("type", "serverchange");
        message.put("approved", isApproved);
        message.put("serverid", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject deleteRoomMessageToClient(String roomId, String isApproved) {
        JSONObject message = new JSONObject();
        message.put("type", "deleteroom");
        message.put("roomid", roomId);
        message.put("approved", isApproved);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject deleteRoomMessageToServer(String roomId, String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "deleteroom");
        message.put("roomid", roomId);
        message.put("serverid", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject requestChatRoomOwningServerMessage(String roomId) {
        JSONObject message = new JSONObject();
        message.put("type", "joinroom");
        message.put("roomid", roomId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject chatRoomOwningServerMessage(String roomId, String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "joinroom");
        message.put("roomid", roomId);
        message.put("serverid", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject deleteClientMessageToServer(String identity, String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "quit");
        message.put("identity", identity);
        message.put("serverid", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject iamUpMessage(String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "iamup");
        message.put("serverid", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject coordinatorMessage(String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "coordinator");
        message.put("leader", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject viewMessage(List<String> views) {
        JSONObject message = new JSONObject();
        message.put("type", "view");
        message.put("views", views);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject electionMessage() {
        JSONObject message = new JSONObject();
        message.put("type", "election");
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject nominationMessage(String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "nomination");
        message.put("serverid", serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject answerMessage() {
        JSONObject message = new JSONObject();
        message.put("type", "answer");
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject heartBeatMessage(String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "heartbeat");
        message.put("serverid",serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject heartBeatResponse(String serverId) {
        JSONObject message = new JSONObject();
        message.put("type", "heartbeatresponse");
        message.put("serverid",serverId);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject requestGlobalsMessage() {
        JSONObject message = new JSONObject();
        message.put("type", "globals");
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject globalDataMessage(List<String> clients, Map<String, String> chatRooms) {
        JSONObject message = new JSONObject();
        message.put("type", "globals");
        message.put("clients", clients);
        message.put("chatrooms", chatRooms);
        return message;
    }
}
