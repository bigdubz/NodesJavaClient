package com.nodes.chatclient.ws.messages;

public record ClientAck(Payload payload) implements ClientMessage {
    public static final String type = "ACK";

    public ClientAck(String payload) {
        this(new Payload(payload));
    }

    @Override
    public String type() {
        return type;
    }

    public record Payload(String blobhash) {
    }
}
