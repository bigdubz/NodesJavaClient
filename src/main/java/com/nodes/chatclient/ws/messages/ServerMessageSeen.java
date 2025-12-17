package com.nodes.chatclient.ws.messages;

public final class ServerMessageSeen implements ServerMessage {
    public final String type = "MESSAGE_SEEN";
    public final Payload payload;

    public ServerMessageSeen(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String messageId;
    }
}
