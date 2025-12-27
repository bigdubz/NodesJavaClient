package com.nodes.chatclient.ws.messages;

public record ServerMessageDelivered(Payload payload) implements ServerMessage {
    public static final String type = "MESSAGE_DELIVERED";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String messageId;
        public String clientId;
    }
}