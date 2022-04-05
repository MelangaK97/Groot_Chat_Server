package com.groot.server.chat.handler;

import com.groot.server.chat.constants.MessageType;
import com.groot.server.chat.service.ChatRoomService;
import com.groot.server.chat.service.ChatRoomServiceImpl;
import com.groot.server.chat.service.ClientService;
import com.groot.server.chat.service.ClientServiceImpl;
import com.groot.server.chat.util.MessageReader;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.net.SocketException;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class ClientServerHandler extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientServerHandler.class);
    private final ClientService clientService = new ClientServiceImpl();
    private final ChatRoomService chatRoomService = new ChatRoomServiceImpl();
    private boolean isConnected = true;
    private final Socket socket;

    public ClientServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        JSONObject request;
        MessageType type;
        try {
            while (isConnected) {
                request = MessageReader.read(socket.getInputStream());
                LOGGER.info("Received request ... {}", request);
                type = MessageType.get(request.get("type").toString());
                switch (type) {
                    case NEW_IDENTITY:
                        isConnected = clientService.createNewClient(socket, request.get("identity").toString());
                        break;
                    case LIST:
                        chatRoomService.getAllChatRooms(socket);
                        break;
                    case WHO:
                        chatRoomService.getAllClients(socket);
                        break;
                    case CREATE_ROOM:
                        chatRoomService.createNewChatRoom(socket, request.get("roomid").toString());
                        break;
                    case JOIN_ROOM:
                        isConnected = chatRoomService.joinChatRoom(socket, request.get("roomid").toString());
                        break;
                    case MOVE_JOIN:
                        clientService.moveJoin(socket, request.get("identity").toString(),
                                request.get("former").toString(), request.get("roomid").toString());
                        break;
                    case DELETE_ROOM:
                        chatRoomService.deleteChatRoom(socket, request.get("roomid").toString());
                        break;
                    case MESSAGE:
                        chatRoomService.sendMessage(socket, request.get("content").toString());
                        break;
                    case QUIT:
                        isConnected = clientService.removeClient(socket, false);
                        break;
                    default:
                        LOGGER.error("Message type {} not acceptable ...", type);
                        break;
                }
            }
            socket.close();
        } catch (SocketException e) {
            LOGGER.error("Connection failed with the client ... {}", e.getMessage());
            isConnected = clientService.removeClient(socket, true);
        } catch (NullPointerException e) {
            LOGGER.error("Unable to communicate with the client ... {}", e.getMessage());
            isConnected = false;
        } catch (Exception e) {
            LOGGER.error("An error occurred ... {}", e.getMessage(), e);
        }
    }
}
