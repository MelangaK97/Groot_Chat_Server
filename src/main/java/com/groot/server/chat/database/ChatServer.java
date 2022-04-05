package com.groot.server.chat.database;

/**
 * @author Melanga Kasun
 * @date (Sun) 20-Feb-2022
 */
public class ChatServer {
    private final String serverId;
    private final String serverAddress;
    private final int clientsPort;
    private final int coordinationPort;
    private final int priority;

    public ChatServer(String serverId, String serverAddress, int clientsPort, int coordinationPort) {
        this.serverId = serverId;
        this.serverAddress = serverAddress;
        this.clientsPort = clientsPort;
        this.coordinationPort = coordinationPort;
        this.priority = Integer.parseInt(serverId.substring(1));
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getClientsPort() {
        return clientsPort;
    }

    public int getCoordinationPort() {
        return coordinationPort;
    }

    public int getPriority() {
        return priority;
    }
}
