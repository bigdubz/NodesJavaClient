package com.nodes.chatclient.ws.messages;

public record ServerAuthError(Payload payload) implements ServerMessage {
    public static final String type = "AUTH_ERROR";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String error;
    }
}