package com.groot.server.chat.service;

import com.groot.server.chat.config.DataStoreConfig;
import com.groot.server.chat.database.ChatClient;
import com.groot.server.chat.database.ChatRoom;
import com.groot.server.chat.database.ChatServer;
import com.groot.server.chat.repository.ChatRoomRepository;
import com.groot.server.chat.repository.ClientRepository;
import com.groot.server.chat.repository.CoordinationRepository;
import com.groot.server.chat.util.DataValidator;
import com.groot.server.chat.util.MessageGenerator;
import com.groot.server.chat.util.MessageSender;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class ChatRoomServiceImpl implements ChatRoomService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatRoomServiceImpl.class);
    private final CoordinationService coordinationService = new CoordinationServiceImpl();
    private final LeaderService leaderService = new LeaderServiceImpl();
    private final ChatRoomRepository repository = new ChatRoomRepository();
    private final ClientRepository clientRepository = new ClientRepository();
    private final CoordinationRepository coordinationRepository = new CoordinationRepository();

    @Override
    public void getAllChatRooms(Socket socket) {
        LOGGER.info("Getting all the chat rooms ...");
        try {
            List<String> chatRoomList = repository.getAllChatRoomIds();
            LOGGER.info("Successfully retrieved the chat rooms {} ...", chatRoomList.toArray());
            MessageSender.send(socket.getOutputStream(), MessageGenerator.roomListMessage(chatRoomList));
        } catch (Exception e) {
            LOGGER.error("Chat rooms retrieval error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void getAllClients(Socket socket) {
        try {
            ChatClient client = clientRepository.getClientBySocket(socket);
            if (Objects.nonNull(client)) {
                LOGGER.info("Getting all the clients of the chat room {} ...", client.getRoomId());
                ChatRoom chatRoom = repository.getChatRoomById(client.getRoomId());
                if (Objects.nonNull(chatRoom)) {
                    List<String> clients = chatRoom.getAllClients().stream().map(ChatClient::getIdentity).collect(Collectors.toList());
                    LOGGER.info("Successfully retrieved the clients {} ...", clients);
                    MessageSender.send(socket.getOutputStream(),
                            MessageGenerator.roomContentsMessage(client.getRoomId(), chatRoom.getOwner(), clients));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Clients retrieval error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean createNewChatRoom(Socket socket, String roomId) {
        LOGGER.info("Validating chat room {} ...", roomId);
        try {
            if (!DataValidator.validateString(roomId)) {
                LOGGER.error("Chat room {} not a valid string ...", roomId);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.createRoomMessage(roomId, "false"));
                return false;
            }
            ChatClient client = clientRepository.getClientBySocket(socket);
            if (Objects.isNull(client)) {
                LOGGER.error("Client not exists ...");
                MessageSender.send(socket.getOutputStream(), MessageGenerator.createRoomMessage(roomId, "false"));
                return false;
            }
            // check uniqueness locally
            ChatRoom chatRoom = repository.getChatRoomById(roomId);
            if (Objects.nonNull(chatRoom)) {
                LOGGER.error("Chat room {} already exists ...", roomId);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.createRoomMessage(roomId, "false"));
                return false;
            }
            if (coordinationService.isLeaderAcceptedChatRoom(roomId)) {
                LOGGER.info("Chat room {} is is accepted by the leader ...", roomId);
                return saveChatRoom(roomId, client);
            }
            LOGGER.error("Chat room {} already exists in another server ...", roomId);
            MessageSender.send(socket.getOutputStream(), MessageGenerator.createRoomMessage(roomId, "false"));
            return false;
        } catch (Exception e) {
            LOGGER.error("Chat room validation error ... {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean saveChatRoom(String roomId, ChatClient client) {
        LOGGER.info("Creating chat room {} ...", roomId);
        try {
            // create the chat room
            ChatRoom chatRoom = new ChatRoom(roomId, client.getIdentity());
            repository.createChatRoom(chatRoom);
            LOGGER.info("Successfully saved the chat room {} ...", roomId);
            MessageSender.send(client.getSocket().getOutputStream(), MessageGenerator.createRoomMessage(roomId, "true"));

            ChatRoom oldChatRoom = repository.getChatRoomById(client.getRoomId());
            broadCastMessage(MessageGenerator.roomChangeMessage(client.getIdentity(), oldChatRoom.getRoomId(), roomId),
                    oldChatRoom.getRoomId(), "");

            // remove owner from old chat room and add owner to the chat room
            repository.removeClientFromChatRoom(oldChatRoom.getRoomId(), client);
            client.setRoomId(roomId);
            repository.addClientToChatRoom(roomId, client);

            if (DataStoreConfig.getInstance().isLeaderMyself()) {
                leaderService.createGlobalChatRoom(roomId);
            } else {
                // inform the leader
                coordinationService.notifyChatRoomCreation(roomId);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Chat room saving error ... {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void createMainHallChatRoom() {
        LOGGER.info("Creating the MainHall chat room for the server {} ...",
                DataStoreConfig.getInstance().getCurrent().getServerId());
        try {
            repository.createChatRoom(new ChatRoom(DataStoreConfig.getInstance().getMainHall(), ""));
            LOGGER.info("Successfully saved the chat room {} ...", DataStoreConfig.getInstance().getMainHall());
        } catch (Exception e) {
            LOGGER.error("Main hall creation error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean joinChatRoom(Socket socket, String roomId) {
        try {
            ChatClient client = clientRepository.getClientBySocket(socket);
            // check the owner
            if (repository.getChatRoomById(client.getRoomId()).getOwner().equals(client.getIdentity())) {
                LOGGER.error("Client {} is the owner of the current chat room ...", client.getIdentity());
                MessageSender.send(socket.getOutputStream(),
                        MessageGenerator.roomChangeMessage(client.getIdentity(), roomId, roomId));
                return true;
            }
            ChatRoom chatRoom = repository.getChatRoomById(roomId);
            if (Objects.isNull(chatRoom)) {
                String globalServerId = coordinationService.getChatRoomOwningServer(roomId, client.getIdentity());
                if (Objects.nonNull(globalServerId)) {
                    LOGGER.info("Successfully retrieved the server {} of the chat room {} ...", globalServerId, roomId);
                    return addToRoomInNewServer(roomId, client.getIdentity(), globalServerId);
                }
                LOGGER.error("Chat room {} is not available in the system ...", roomId);
                MessageSender.send(socket.getOutputStream(),
                        MessageGenerator.roomChangeMessage(client.getIdentity(), roomId, roomId));
                return true;
            }
            // room is in same server
            repository.removeClientFromChatRoom(client.getRoomId(), client);
            LOGGER.info("Removed the Client {} from the chat room {} ...", client.getIdentity(), client.getRoomId());
            broadCastMessage(MessageGenerator.roomChangeMessage(client.getIdentity(), client.getRoomId(), roomId), client.getRoomId(), "");
            repository.addClientToChatRoom(roomId, client);
            LOGGER.info("Added the Client {} to the chat room {} ...", client.getIdentity(), roomId);
            broadCastMessage(MessageGenerator.roomChangeMessage(client.getIdentity(), client.getRoomId(), roomId), roomId, "");
            client.setRoomId(roomId);
            return true;
        } catch (Exception e) {
            LOGGER.error("Chat room joining error ... {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean addToRoomInNewServer(String roomId, String identity, String serverId) {
        try  {
            if (Objects.nonNull(serverId)) {
                ChatClient client = clientRepository.getClientById(identity);
                ChatServer server = DataStoreConfig.getInstance().getNeighbourById(serverId);

                if (Objects.nonNull(client) && Objects.nonNull(server)) {
                    MessageSender.send(client.getSocket().getOutputStream(),
                            MessageGenerator.routeMessage(roomId, server.getServerAddress(), server.getClientsPort()));
                    LOGGER.info("Chat room {} exists in server {} {}:{} ...", roomId, serverId, server.getServerAddress(), server.getClientsPort());
                    repository.removeClientFromChatRoom(client.getRoomId(), client);
                    LOGGER.info("Removed the Client {} from the chat room {} ...", client.getIdentity(), client.getRoomId());
                    clientRepository.removeClient(identity);
                    LOGGER.info("Removed the Client {} from the server ...", client.getIdentity());
                    broadCastMessage(MessageGenerator.roomChangeMessage(client.getIdentity(), client.getRoomId(), roomId),
                            client.getRoomId(), client.getIdentity());
                    // close the connection with the client
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Client adding to new server error ... {}", e.getMessage(), e);
        }
        return true;
    }

    @Override
    public boolean deleteChatRoom(Socket socket, String roomId) {
        try {
            ChatClient client = clientRepository.getClientBySocket(socket);
            if (Objects.isNull(client)) {
                LOGGER.error("Client not exists ...");
                return false;
            }
            ChatRoom chatRoom = repository.getChatRoomById(roomId);
            if (Objects.isNull(chatRoom)) {
                LOGGER.error("Chat room {} not exists ...", roomId);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.deleteRoomMessageToClient(roomId, "false"));
                return false;
            }
            LOGGER.info("Successfully retrieved the chat room {} ...", roomId);
            if (!chatRoom.getOwner().equals(client.getIdentity())) {
                LOGGER.error("Client {} is not the owner of the chat room {} ...", client.getIdentity(), roomId);
                MessageSender.send(socket.getOutputStream(), MessageGenerator.deleteRoomMessageToClient(roomId, "false"));
                return false;
            }
            String mainHall = DataStoreConfig.getInstance().getMainHall();
            MessageSender.send(socket.getOutputStream(), MessageGenerator.deleteRoomMessageToClient(roomId, "true"));
            for (ChatClient participant : chatRoom.getAllClients()) {
                addToMainHall(participant);
                broadCastMessage(MessageGenerator.roomChangeMessage(participant.getIdentity(), chatRoom.getRoomId(),
                        mainHall), chatRoom.getRoomId(), "");
                broadCastMessage(MessageGenerator.roomChangeMessage(participant.getIdentity(), chatRoom.getRoomId(),
                        mainHall), mainHall, participant.getIdentity());
                participant.setRoomId(mainHall);
            }
            repository.deleteChatRoom(roomId);
            LOGGER.info("Successfully deleted the chat room {} ...", roomId);
            coordinationRepository.deleteGlobalChatRoom(roomId);
            LOGGER.info("Successfully deleted the chat room {} from global list ...", roomId);
            coordinationService.informChatRoomDeletion(roomId);
            return true;
        } catch (Exception e) {
            LOGGER.error("Chat room deletion error ... {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void sendMessage(Socket socket, String content) {
        try {
            ChatClient client = clientRepository.getClientBySocket(socket);
            if (Objects.nonNull(client)) {
                LOGGER.info("Broadcasting the message by client {} ...", client.getIdentity());
                broadCastMessage(MessageGenerator.message(client.getIdentity(), content), client.getRoomId(), client.getIdentity());
            }
        } catch (Exception e) {
            LOGGER.error("Message broadcasting error ... {}", e.getMessage(), e);
        }
    }

    @Override
    public void addToMainHall(ChatClient client) {
        repository.addClientToChatRoom(DataStoreConfig.getInstance().getMainHall(), client);
        LOGGER.info("Added the Client {} to the Main Hall ...", client.getIdentity());
    }

    @Override
    public void broadCastMessage(JSONObject message, String roomId, String identity) {
        try {
            for (ChatClient client : repository.getAllClientsOfChatRoom(roomId)) {
                if (!client.getIdentity().equals(identity)) {
                    MessageSender.send(client.getSocket().getOutputStream(), message);
                    LOGGER.info("Sent message to client {} ...", client.getIdentity());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Message sending error ... {}", e.getMessage(), e);
        }
    }
}
