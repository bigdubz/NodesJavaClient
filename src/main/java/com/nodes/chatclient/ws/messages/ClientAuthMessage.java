package com.nodes.chatclient.ws.messages;

public class ClientAuthMessage implements ClientMessage {
    public final String type = "AUTH";
    public final Payload payload;

    public ClientAuthMessage(String userId, String deviceId, String token) {
        this.payload = new Payload(userId, deviceId, token);
    }

    @Override
    public String type() {
        return "";
    }

    public record Payload(String userId, String deviceId, String token) {}
}
