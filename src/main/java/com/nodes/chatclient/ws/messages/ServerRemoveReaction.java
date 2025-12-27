package com.nodes.chatclient.ws.messages;

public record ServerRemoveReaction(Payload payload) implements ServerMessage {
    public static final String type = "REMOVE_REACTION";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String messageId;
        public String userId;
    }
}