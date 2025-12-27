package com.nodes.chatclient.ws.messages;

public class ClientChatMessage implements ClientMessage {
    public final String type = "CHAT_MESSAGE";
    public final Payload payload;

    public ClientChatMessage(
            String toUserId,
            String text,
            String clientId,
            String replyingTo
    ) {
        this.payload = new ClientChatMessage.Payload(toUserId, text, clientId, replyingTo);
    }

    @Override
    public String type() {
        return "";
    }

    public record Payload(String toUserId, String text, String clientId, String replyingTo) {}
}
