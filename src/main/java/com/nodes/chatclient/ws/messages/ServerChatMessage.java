package com.nodes.chatclient.ws.messages;

public final class ServerChatMessage implements ServerMessage {
    public final String type = "CHAT_MESSAGE";
    public final Payload payload;

    public ServerChatMessage(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String fromUserId;
        public String text;
        public String messageId;
        public long createdAt;
        public boolean isOnline;
        public String replyingTo;
    }
}