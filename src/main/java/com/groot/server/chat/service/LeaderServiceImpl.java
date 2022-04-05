package com.groot.server.chat.service;

import com.groot.server.chat.repository.CoordinationRepository;
import com.groot.server.chat.repository.LeaderRepository;
import com.groot.server.chat.util.MessageGenerator;
import com.groot.server.chat.util.MessageSender;
import com.groot.server.chat.util.ServerBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;

/**
 * @author Melanga Kasun
 * @date (Wed) 02-Mar-2022
 */
public class LeaderServiceImpl implements LeaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderServiceImpl.class);
    private final LeaderRepository repository = new LeaderRepository();
    private final CoordinationRepository coordinationRepository = new CoordinationRepository();

    @Override
    public void checkClientRegistered(Socket socket, String identity, String serverId) {
        try {
            LOGGER.info("Checking the client {} global availability ...", identity);
            if (coordinationRepository.isGlobalClient(identity) || repository.isPendingClient(identity)) {
                LOGGER.error("Client {} already exists ...", identity);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.clientAvailabilityMessage(identity, "false", serverId));
            } else {
                LOGGER.info("Client {} not exists ...", identity);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.clientAvailabilityMessage(identity, "true", serverId));
            }
        } catch (Exception e) {
            LOGGER.error("Client register data checking error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void checkChatRoomRegistered(Socket socket, String roomId, String serverId) {
        try {
            LOGGER.info("Checking the chat room {} global availability ...", roomId);
            if (coordinationRepository.isGlobalChatRoom(roomId) || repository.isPendingChatRoom(roomId, serverId)) {
                LOGGER.error("Chat room {} already exists ...", roomId);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.chatRoomAvailabilityMessage(roomId, "false", serverId));
            } else {
                LOGGER.info("Chat room {} not exists ...", roomId);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.chatRoomAvailabilityMessage(roomId, "true", serverId));
            }
        } catch (Exception e) {
            LOGGER.error("Chat room register data checking error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void createGlobalClient(String identity) {
        try {
            LOGGER.info("Adding the client {} to global list ...", identity);
            // remove client from pending
            repository.removePendingClient(identity);
            LOGGER.info("Successfully removed the client {} from pending list ...", identity);
            // add client to the global list
            coordinationRepository.addNewGlobalClient(identity);
            LOGGER.info("Successfully added the client {} to global list ...", identity);
            ServerBroadcaster.broadcast(MessageGenerator.updateGlobalClientsMessage(identity));
        } catch (Exception e) {
            LOGGER.error("Global client creation error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void createGlobalChatRoom(String roomId) {
        try {
            LOGGER.info("Adding the chat room {} to global list ...", roomId);
            // remove client from pending
            String serverId = repository.removePendingChatRoom(roomId);
            LOGGER.info("Successfully removed the chat room {} from pending list ...", roomId);
            // add client to the global list
            coordinationRepository.addNewGlobalChatRoom(roomId, serverId);
            LOGGER.info("Successfully added the chat room {} to global list ...", roomId);
            ServerBroadcaster.broadcast(MessageGenerator.updateGlobalChatRoomsMessage(roomId, serverId));
        } catch (Exception e) {
            LOGGER.error("Global chat room creation error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void getServerByChatRoomId(Socket socket, String roomId) {
        try {
            LOGGER.info("Getting the server of the chat room {} ...", roomId);
            // remove client from pending
            String globalServerId = coordinationRepository.getGlobalServerByChatRoomId(roomId);
            LOGGER.info("Successfully retrieved the server {} of the chat room {} ...", globalServerId, roomId);
            MessageSender.send(socket.getOutputStream(),
                    MessageGenerator.chatRoomOwningServerMessage(roomId, globalServerId));
        } catch (Exception e) {
            LOGGER.error("Server retrieving error ... {}", e.getMessage(), e);
        }
    }
}
