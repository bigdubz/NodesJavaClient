package com.nodes.chatclient.ws.messages;

public final class ServerUserOnline implements ServerMessage {
    public final String type = "USER_ONLINE";
    public final Payload payload;

    public ServerUserOnline(Payload payload) {
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
