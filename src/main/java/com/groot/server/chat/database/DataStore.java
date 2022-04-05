package com.groot.server.chat.database;

import java.util.*;

/**
 * @author Melanga Kasun
 * @date (Sun) 20-Feb-2022
 */
public class DataStore {
    private String mainHall;
    private ChatServer current;
    private ChatServer leader;
    private final HashMap<String, ChatServer> views = new HashMap<>();
    // other neighbour servers in the distributed chat server
    private final HashMap<String, ChatServer> neighbours = new HashMap<>();
    // chat clients and chat rooms exists in the current server
    private final HashMap<String, ChatClient> clients = new HashMap<>();
    private final HashMap<String, ChatRoom> chatRooms = new HashMap<>();
    // clients and chat rooms exists in the entire system
    private final List<String> globalClients = new ArrayList<>();
    private final HashMap<String, String> globalChatRooms = new HashMap<>();
    // leader approved clients and chat rooms before actual creation
    private final List<String> pendingClients = new ArrayList<>();
    private final HashMap<String, String> pendingChatRooms = new HashMap<>();

    public void setCurrent(ChatServer current) {
        this.current = current;
        this.mainHall = "MainHall-" + current.getServerId();
    }

    public ChatServer getCurrent() {
        return current;
    }

    public String getMainHall() {
        return mainHall;
    }

    public void setLeader(ChatServer leader) {
        this.leader = leader;
    }

    public ChatServer getLeader() {
        return leader;
    }

    public boolean isLeaderMyself() {
        return current.getServerId().equals(leader.getServerId())
                && current.getServerAddress().equals(leader.getServerAddress())
                && current.getClientsPort() == leader.getClientsPort()
                && current.getCoordinationPort() == leader.getCoordinationPort();
    }

    public void addView(ChatServer server) {
        if (Objects.nonNull(server)) {
            synchronized (this.views) {
                if (!this.views.containsKey(server.getServerId())) {
                    this.views.put(server.getServerId(), server);
                }
            }
        }
    }

    public Map<String, ChatServer> getViews() {
        synchronized (this.views) {
            return views;
        }
    }

    public void removeViews(List<String> identities) {
        synchronized (this.views) {
            identities.forEach(this.views::remove);
        }
    }

    public void addNeighbour(ChatServer server) {
        if (Objects.nonNull(server)) {
            synchronized (this.neighbours) {
                this.neighbours.put(server.getServerId(), server);
            }
        }
    }

    public Map<String, ChatServer> getNeighbours() {
        return neighbours;
    }

    public ChatServer getNeighbourById(String serverId) {
        return this.neighbours.get(serverId);
    }

    public void addClient(ChatClient client) {
        synchronized (this.clients) {
            this.clients.put(client.getIdentity(), client);
        }
    }

    public ChatClient getClient(String identity) {
        synchronized (this.neighbours) {
            return this.clients.getOrDefault(identity, null);
        }
    }

    public Map<String, ChatClient> getClients() {
        return clients;
    }

    public void removeClient(String identity) {
        synchronized (this.clients) {
            this.clients.remove(identity);
        }
    }

    public void addChatRoom(ChatRoom chatRoom) {
        synchronized (this.chatRooms) {
            this.chatRooms.put(chatRoom.getRoomId(), chatRoom);
        }
    }

    public ChatRoom getChatRoom(String roomId) {
        return this.chatRooms.getOrDefault(roomId, null);
    }

    public List<String> getAllChatRoomIds() {
        return new ArrayList<>(this.globalChatRooms.keySet());
    }

    public void removeFromChatRoom(String roomId, ChatClient client) {
        synchronized (this.chatRooms) {
            this.chatRooms.get(roomId).removeClient(client);
        }
    }

    public void addToChatRoom(String roomId, ChatClient client) {
        synchronized (this.chatRooms) {
            this.chatRooms.get(roomId).addClient(client);
        }
    }

    public List<ChatClient> getAllClientsOfChatRoom(String roomId) {
        return this.chatRooms.get(roomId).getAllClients();
    }

    public void deleteChatRoom(String roomId) {
        synchronized (this.chatRooms) {
            this.chatRooms.remove(roomId);
        }
    }

    public boolean isGlobalClient(String identity) {
        return this.globalClients.contains(identity);
    }

    public void addNewGlobalClient(String identity) {
        synchronized (this.globalClients) {
            this.globalClients.add(identity);
        }
    }

    public List<String> getGlobalClients() {
        return this.globalClients;
    }

    public void deleteGlobalClient(String identity) {
        synchronized (this.globalClients) {
            this.globalClients.remove(identity);
        }
    }

    public void updateGlobalClients(List<String> clients) {
        synchronized (this.globalClients) {
            clients.forEach(client -> {
                if (!this.globalClients.contains(client)) {
                    this.globalClients.add(client);
                }
            });
        }
    }

    public boolean isGlobalChatRoom(String roomId) {
        return this.globalChatRooms.containsKey(roomId);
    }

    public void addNewGlobalChatRoom(String roomId, String serverId) {
        synchronized (this.globalChatRooms) {
            this.globalChatRooms.put(roomId, serverId);
        }
    }

    public Map<String, String> getGlobalChatRooms() {
        return globalChatRooms;
    }

    public String getGlobalServerByChatRoomId(String roomId) {
        return this.globalChatRooms.get(roomId);
    }

    public void deleteGlobalChatRoom(String roomId) {
        synchronized (this.globalChatRooms) {
            this.globalChatRooms.remove(roomId);
        }
    }

    public void updateGlobalChatRooms(Map<String, String> chatRooms) {
        synchronized (this.globalChatRooms) {
            chatRooms.forEach((roomId, server) -> {
                if (!this.globalChatRooms.containsKey(roomId)) {
                    this.globalChatRooms.put(roomId, server);
                }
            });
        }
    }

    public boolean isPendingClient(String identity) {
        synchronized (this.pendingClients) {
            if (!this.pendingClients.contains(identity)) {
                this.pendingClients.add(identity);
                return false;
            }
        }
        return true;
    }

    public void removePendingClient(String identity) {
        synchronized (this.pendingClients) {
            this.pendingClients.remove(identity);
        }
    }

    public boolean isPendingChatRoom(String roomId, String serverId) {
        synchronized (this.pendingChatRooms) {
            if (!this.pendingChatRooms.containsKey(roomId)) {
                this.pendingChatRooms.put(roomId, serverId);
                return false;
            } else {
                return true;
            }
        }
    }

    public String removePendingChatRoom(String roomId) {
        String serverId = this.pendingChatRooms.get(roomId);
        if (Objects.nonNull(serverId)) {
            synchronized (this.pendingChatRooms) {
                this.pendingChatRooms.remove(roomId);
            }
        }
        return serverId;
    }
}
