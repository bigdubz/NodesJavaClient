package com.nodes.chatclient.ws.messages;

public record ServerMessageSeen(Payload payload) implements ServerMessage {
    public static final String type = "MESSAGE_SEEN";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String messageId;
    }
}
