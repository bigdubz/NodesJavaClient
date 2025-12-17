package com.nodes.chatclient.ws.messages;

public final class ServerMessageDelivered implements ServerMessage {
    public final String type = "MESSAGE_DELIVERED";
    public final Payload payload;

    public ServerMessageDelivered(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String messageId;
        public String clientId;
    }
}