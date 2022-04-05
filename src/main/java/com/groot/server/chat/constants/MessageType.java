package com.groot.server.chat.constants;

import java.util.Arrays;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public enum MessageType {
    NEW_IDENTITY("newidentity"),
    LIST("list"),
    WHO("who"),
    ROUTE("route"),
    CREATE_ROOM("createroom"),
    JOIN_ROOM("joinroom"),
    ROOM_CHANGE("roomchange"),
    ROOM_LIST("roomlist"),
    ROOM_CONTENTS("roomcontents"),
    DELETE_ROOM("deleteroom"),
    MOVE_JOIN("movejoin"),
    SERVER_CHANGE("serverchange"),
    MESSAGE("message"),
    QUIT("quit"),
    IAM_UP("iamup"),
    ELECTION("election"),
    COORDINATOR("coordinator"),
    HEARTBEAT("heartbeat"),
    HEARTBEAT_RESPONSE("heartbeatresponse"),
    GLOBALS("globals"),
    UNAVAILABLE("unavailable");

    private final String type;

    MessageType(String type) {
        this.type = type;
    }

    public static MessageType get(String text) {
        return Arrays.stream(MessageType.values()).filter(env -> env.type.equals(text)).findFirst().orElse(UNAVAILABLE);
    }

    @Override
    public String toString() {
        return type;
    }
}
