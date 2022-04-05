package com.groot.server.chat.handler;

import com.groot.server.chat.constants.MessageType;
import com.groot.server.chat.service.*;
import com.groot.server.chat.util.MessageReader;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.Objects;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class ServerServerHandler extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerServerHandler.class);
    private final CoordinationService coordinationService = new CoordinationServiceImpl();
    private final LeaderService leaderService = new LeaderServiceImpl();
    private final HeartBeatService heartBeatService = new HeartBeatServiceImpl();
    private final Socket socket;

    public ServerServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        JSONObject request;
        MessageType type;
        try {
            request = MessageReader.read(socket.getInputStream());
            LOGGER.info("Received request ... {}", request);
            type = MessageType.get(request.get("type").toString());

            switch (type) {
                case NEW_IDENTITY:
                    // request from server for check the identity uniqueness
                    if (Objects.nonNull(request.get("approved")) && request.get("approved").equals("forwarded")
                            && Objects.nonNull(request.get("serverid"))) {
                        leaderService.checkClientRegistered(socket, request.get("identity").toString(),
                                request.get("serverid").toString());
                    }
                    // request from server about the client creation
                    else if (Objects.nonNull(request.get("created")) && request.get("created").equals("true")) {
                        leaderService.createGlobalClient(request.get("identity").toString());
                        socket.close();
                    }
                    // request from leader to update global client list
                    else if (Objects.nonNull(request.get("approved")) && request.get("approved").equals("true")) {
                        coordinationService.updateGlobalClients(request.get("identity").toString(), true);
                        socket.close();
                    }
                    break;
                case CREATE_ROOM:
                    // request from server for check the identity uniqueness
                    if (Objects.nonNull(request.get("approved")) && request.get("approved").equals("forwarded")
                            && Objects.nonNull(request.get("serverid"))) {
                        leaderService.checkChatRoomRegistered(socket, request.get("roomid").toString(),
                                request.get("serverid").toString());
                    }
                    // request from server about the client creation
                    else if (Objects.nonNull(request.get("created")) && request.get("created").equals("true")) {
                        leaderService.createGlobalChatRoom(request.get("roomid").toString());
                        socket.close();
                    }
                    // request from leader to update global client list
                    else if (Objects.nonNull(request.get("approved")) && request.get("approved").equals("true")
                            && Objects.nonNull(request.get("serverid"))) {
                        coordinationService.updateGlobalChatRooms(request.get("roomid").toString(),
                                request.get("serverid").toString(), true);
                        socket.close();
                    }
                    break;
                case JOIN_ROOM:
                    leaderService.getServerByChatRoomId(socket, request.get("roomid").toString());
                    break;
                case DELETE_ROOM:
                    coordinationService.updateGlobalChatRooms(request.get("roomid").toString(),
                            request.get("serverid").toString(), false);
                    socket.close();
                    break;
                case QUIT:
                    coordinationService.updateGlobalClients(request.get("identity").toString(), false);
                    socket.close();
                    break;
                case IAM_UP:
                    coordinationService.sendCurrentView(socket, request.get("serverid").toString());
                    break;
                case ELECTION:
                    coordinationService.participateElection(socket);
                    break;
                case COORDINATOR:
                    coordinationService.updateLeader(request.get("leader").toString());
                    socket.close();
                    break;
                case GLOBALS:
                    coordinationService.sendGlobalData(socket);
                    break;
                case HEARTBEAT:
                    heartBeatService.sendResponse(socket, request.get("serverid").toString());
                    break;
                default:
                    LOGGER.error("Message type {} not acceptable ...", type);
                    socket.close();
                    break;
            }
        } catch (NullPointerException e) {
            LOGGER.error("Connection closed with the connected server ... {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred ... {}", e.getMessage(), e);
        }
    }
}
