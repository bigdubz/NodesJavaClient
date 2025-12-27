package com.nodes.chatclient.ws.messages;

public class ClientAuthMessage implements ClientMessage {
    public final String type = "AUTH";
    public final Payload payload;

    public ClientAuthMessage(String userId, String token) {
        this.payload = new Payload(userId, token);
    }

    @Override
    public String type() {
        return "";
    }

    public record Payload(String userId, String token) {}
}
