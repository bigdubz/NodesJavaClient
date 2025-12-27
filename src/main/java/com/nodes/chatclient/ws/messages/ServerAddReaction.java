package com.nodes.chatclient.ws.messages;

public record ServerAddReaction(Payload payload) implements ServerMessage {
    public static final String type = "ADD_REACTION";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String messageId;
        public String userId;
        public String reaction;
    }
}