package com.nodes.chatclient.ws.messages;

public final class ServerAuthError implements ServerMessage {
    public final String type = "AUTH_ERROR";
    public final Payload payload;

    public ServerAuthError(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String error;
    }
}