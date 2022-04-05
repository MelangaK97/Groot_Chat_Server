package com.groot.server.chat.service;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.ChatClient;
import com.groot.server.chat.database.ChatRoom;
import com.groot.server.chat.database.DataStore;
import com.groot.server.chat.repository.ChatRoomRepository;
import com.groot.server.chat.repository.ClientRepository;
import com.groot.server.chat.repository.CoordinationRepository;
import com.groot.server.chat.util.DataValidator;
import com.groot.server.chat.util.MessageGenerator;
import com.groot.server.chat.util.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.Objects;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class ClientServiceImpl implements ClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientServiceImpl.class);
    private final CoordinationService coordinationService = new CoordinationServiceImpl();
    private final ChatRoomService chatRoomService = new ChatRoomServiceImpl();
    private final LeaderService leaderService = new LeaderServiceImpl();
    private final ClientRepository repository = new ClientRepository();
    private final ChatRoomRepository chatRoomRepository = new ChatRoomRepository();
    private final CoordinationRepository coordinationRepository = new CoordinationRepository();

    @Override
    public boolean createNewClient(Socket socket, String identity) {
        LOGGER.info("Validating the client {} ...", identity);
        try {
            // validate the string
            if (!DataValidator.validateString(identity)) {
                LOGGER.error("Client {} not a valid string ...", identity);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.newIdentityMessage("false"));
                return false;
            }
            // check uniqueness locally
            if (Objects.nonNull(repository.getClientById(identity))) {
                LOGGER.error("Client {} already exists ...", identity);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.newIdentityMessage("false"));
                return false;
            }
            if (coordinationService.isLeaderAcceptedClient(identity)) {
                LOGGER.info("Client {} is is accepted by the leader ...", identity);
                return saveClient(socket, identity);
            }
            LOGGER.error("Client {} already exists in another server ...", identity);
            MessageSender.send(socket.getOutputStream(), MessageGenerator.newIdentityMessage("false"));
            return false;
        } catch (Exception e) {
            LOGGER.error("Client validation error ... {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean saveClient(Socket socket, String identity) {
        try {
            LOGGER.info("Creating client {} ...", identity);
            // create the client
            ChatClient client = new ChatClient(identity, DataStoreConfig.getInstance().getMainHall(), socket);
            repository.createClient(client);
            LOGGER.info("Successfully saved the client {} ...", identity);
            MessageSender.send(socket.getOutputStream(), MessageGenerator.newIdentityMessage("true"));
            // add to main hall
            chatRoomService.addToMainHall(client);

            if (DataStoreConfig.getInstance().isLeaderMyself()) {
                leaderService.createGlobalClient(identity);
            } else {
                // inform the leader
                coordinationService.notifyClientCreation(identity);
            }
            // broadcast message to main hall
            chatRoomService.broadCastMessage(MessageGenerator.roomChangeMessage(identity, "", DataStoreConfig.getInstance().getMainHall()),
                    DataStoreConfig.getInstance().getMainHall(), "");
            return true;
        } catch (Exception e) {
            LOGGER.error("Client saving error ... {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeClient(Socket socket, boolean isFailed) {
        try {
            ChatClient client = repository.getClientBySocket(socket);
            if (Objects.isNull(client)) {
                LOGGER.error("Client not exists ...");
                return false;
            }
            LOGGER.info("Deleting client {} ...", client.getIdentity());
            ChatRoom chatRoom = chatRoomRepository.getChatRoomById(client.getRoomId());
            if (Objects.isNull(chatRoom)) {
                LOGGER.error("Chat room {} not exists ...", client.getRoomId());
                return false;
            }
            if (!chatRoom.getOwner().equals(client.getIdentity())) {
                chatRoomRepository.removeClientFromChatRoom(chatRoom.getRoomId(), client);
                LOGGER.info("Removed the Client {} from the chat room {} ...", client.getIdentity(), chatRoom.getRoomId());
                if (!isFailed) {
                    chatRoomService.broadCastMessage(MessageGenerator.roomChangeMessage(client.getIdentity(), chatRoom.getRoomId(), ""),
                            chatRoom.getRoomId(), client.getIdentity());
                }
            } else {
                String mainHall = DataStoreConfig.getInstance().getMainHall();
                for (ChatClient participant : chatRoom.getAllClients()) {
                    chatRoomService.addToMainHall(participant);
                    chatRoomService.broadCastMessage(MessageGenerator.roomChangeMessage(participant.getIdentity(), "",
                            mainHall), chatRoom.getRoomId(), participant.getIdentity());
                    chatRoomService.broadCastMessage(MessageGenerator.roomChangeMessage(participant.getIdentity(), "",
                            mainHall), mainHall, participant.getIdentity());
                    participant.setRoomId(mainHall);
                }
                chatRoomRepository.deleteChatRoom(chatRoom.getRoomId());
                LOGGER.info("Successfully deleted the chat room {} ...", chatRoom.getRoomId());
                coordinationRepository.deleteGlobalChatRoom(chatRoom.getRoomId());
                LOGGER.info("Successfully deleted the chat room {} from global list ...", chatRoom.getRoomId());
                coordinationService.informChatRoomDeletion(chatRoom.getRoomId());
                if (!isFailed) {
                    MessageSender.send(socket.getOutputStream(), MessageGenerator.deleteRoomMessageToClient(chatRoom.getRoomId(), "true"));
                }
            }
            repository.removeClient(client.getIdentity());
            LOGGER.info("Successfully deleted the client {} ...", client.getIdentity());
            coordinationRepository.deleteGlobalClient(client.getIdentity());
            LOGGER.info("Successfully deleted the client {} from global list ...", client.getIdentity());
            coordinationService.informClientDeletion(client.getIdentity());
            MessageSender.send(socket.getOutputStream(), MessageGenerator.roomChangeMessage(client.getIdentity(), client.getRoomId(), ""));
            return true;
        } catch (Exception e) {
            LOGGER.error("Client removing error ... {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void moveJoin(Socket socket, String identity, String former, String roomId) {
        // since the client already saved in another server no need to check the validity
        try {
            DataStore store = DataStoreConfig.getInstance();
            // create the client
            ChatClient client = new ChatClient(identity, store.getMainHall(), socket);
            repository.createClient(client);
            LOGGER.info("Successfully saved the client {} ...", identity);
            MessageSender.send(socket.getOutputStream(),
                    MessageGenerator.serverChangeMessage(store.getCurrent().getServerId(), "true"));

            ChatRoom chatRoom = chatRoomRepository.getChatRoomById(roomId);
            if (Objects.isNull(chatRoom)) {
                LOGGER.error("Chat room {} not exists ...", roomId);
                LOGGER.info("Adding client {} to the Main Hall ...", identity);
                chatRoomService.addToMainHall(client);
                // broadcast message to main hall
                chatRoomService.broadCastMessage(MessageGenerator.roomChangeMessage(identity, former, store.getMainHall()),
                        store.getMainHall(), identity);
            } else {
                chatRoomRepository.addClientToChatRoom(roomId, client);
                client.setRoomId(roomId);
                LOGGER.info("Added the Client {} to the chat room {} ...", identity, roomId);
                chatRoomService.broadCastMessage(MessageGenerator.roomChangeMessage(identity, former, roomId), roomId, "");
            }
        } catch (Exception e) {
            LOGGER.error("Client shifting error ... {}", e.getMessage(), e);
        }
    }
}
