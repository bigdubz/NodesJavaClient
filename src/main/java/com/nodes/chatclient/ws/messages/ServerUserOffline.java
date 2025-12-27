package com.nodes.chatclient.ws.messages;

public record ServerUserOffline(Payload payload) implements ServerMessage {
    public static final String type = "USER_OFFLINE";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String userId;
        public long lastSeen;
    }
}