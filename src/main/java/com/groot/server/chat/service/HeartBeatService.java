package com.groot.server.chat.service;

import java.net.Socket;

public interface HeartBeatService {
    void start();
    void sendResponse(Socket socket, String serverId);
}
