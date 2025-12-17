package com.nodes.chatclient.ws.messages;

public final class ServerAddReaction implements ServerMessage {
    public final String type = "ADD_REACTION";
    public final Payload payload;

    public ServerAddReaction(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String messageId;
        public String userId;
        public String reaction;
    }
}