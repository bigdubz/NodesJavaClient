package com.nodes.chatclient.ws.messages;

public final class ServerRemoveReaction implements ServerMessage {
    public final String type = "REMOVE_REACTION";
    public final Payload payload;

    public ServerRemoveReaction(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String messageId;
        public String userId;
    }
}