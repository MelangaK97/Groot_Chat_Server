package com.groot.server.chat.service;

import java.net.Socket;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public interface ClientService {
    boolean createNewClient(Socket socket, String identity);
    boolean removeClient(Socket socket, boolean isFailed);
    void moveJoin(Socket socket, String identity, String former, String roomId);
}
