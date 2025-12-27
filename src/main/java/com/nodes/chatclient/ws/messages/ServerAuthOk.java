package com.nodes.chatclient.ws.messages;

public record ServerAuthOk(Payload payload) implements ServerMessage {
    public static final String type = "AUTH_OK";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String userId;
    }
}
