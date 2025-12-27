package com.nodes.chatclient.ws.messages;

public record ServerUserOnline(Payload payload) implements ServerMessage {
    public static final String type = "USER_ONLINE";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String userId;
    }
}
