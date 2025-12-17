package com.nodes.chatclient.ws.messages;

public final class ServerUserTyping implements ServerMessage {
    public final String type = "USER_TYPING";
    public final Payload payload;

    public ServerUserTyping(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String fromUserId;
        public boolean isTyping;
    }
}