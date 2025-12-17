package com.nodes.chatclient.ws.messages;

public final class ClientTypingMessage implements ClientMessage {
    public final String type = "USER_TYPING";
    public final Payload payload;

    public ClientTypingMessage(String toUserId, boolean isTyping) {
        this.payload = new Payload(toUserId, isTyping);
    }

    @Override
    public String type() {
        return type;
    }

    public static final class Payload {
        public final String toUserId;
        public final boolean isTyping;

        public Payload(String toUserId, boolean isTyping) {
            this.toUserId = toUserId;
            this.isTyping = isTyping;
        }
    }
}
