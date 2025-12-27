package com.nodes.chatclient.ws.messages;

public final class ClientRemoveReaction implements ClientMessage {
    public final String type = "REMOVE_REACTION";
    public final Payload payload;

    public ClientRemoveReaction(String messageId, String toUserId) {
        this.payload = new Payload(messageId, toUserId);
    }

    @Override
    public String type() {
        return type;
    }

    public record Payload(String messageId, String toUserId) {}
}