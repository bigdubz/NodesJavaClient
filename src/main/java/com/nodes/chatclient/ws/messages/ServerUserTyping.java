package com.nodes.chatclient.ws.messages;

public record ServerUserTyping(Payload payload) implements ServerMessage {
    public static final String type = "USER_TYPING";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String fromUserId;
        public boolean isTyping;
    }
}