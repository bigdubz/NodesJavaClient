package com.nodes.chatclient.ws.messages;

public final class ServerAuthOk implements ServerMessage {
    public final String type = "AUTH_OK";
    public final Payload payload;

    public ServerAuthOk(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String userId;
    }
}
