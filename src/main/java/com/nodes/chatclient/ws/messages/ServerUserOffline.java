package com.nodes.chatclient.ws.messages;

public final class ServerUserOffline implements ServerMessage {
    public final String type = "USER_OFFLINE";
    public final Payload payload;

    public ServerUserOffline(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String userId;
        public long lastSeen;
    }
}