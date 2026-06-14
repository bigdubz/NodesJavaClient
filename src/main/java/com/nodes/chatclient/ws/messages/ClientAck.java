package com.nodes.chatclient.ws.messages;

public final class ClientAck implements ClientMessage {
    public final String type = "ACK";
    public final Payload payload;

    public ClientAck(String payload) {
        this.payload = new Payload(payload);
    }

    @Override
    public String type() {
        return type;
    }

    public record Payload(String blobHash) {
    }
}
