package com.nodes.chatclient.ws.messages;

public class ClientMessageSeen implements ClientMessage {
    public final String type = "MESSAGE_SEEN";
    public final Payload payload;

    public ClientMessageSeen(String messageId) {
        this.payload = new Payload(messageId);
    }

    @Override
    public String type() {
        return type;
    }

    public record Payload(String messageId) {}
}
