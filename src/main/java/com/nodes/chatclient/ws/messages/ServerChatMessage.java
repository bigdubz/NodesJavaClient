package com.nodes.chatclient.ws.messages;

public record ServerChatMessage(Payload payload) implements ServerMessage {
    public static final String type = "CHAT_MESSAGE";

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public String fromUserId;
        public String text;
        public String messageId;
        public long createdAt;
        public String replyingTo;
    }
}