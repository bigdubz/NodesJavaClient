package com.nodes.chatclient.ws.messages;

public final class ClientAddReaction implements ClientMessage {
    public final String type = "ADD_REACTION";
    public final Payload payload;

    public ClientAddReaction(String messageId, String reaction, String toUserId) {
        this.payload = new Payload(messageId, reaction, toUserId);
    }

    @Override
    public String type() {
        return type;
    }

    public record Payload(String messageId, String reaction, String toUserId) {}
}