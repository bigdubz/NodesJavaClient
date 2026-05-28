package com.nodes.chatclient.ws.messages;

public record ClientEncryptedSend(Payload payload) implements ClientMessage {
    public static final String type = "ENCRYPTED_SEND";

    public ClientEncryptedSend(String toUserId, String toDeviceId, String blob) {
        this(new Payload(toUserId, toDeviceId, blob));
    }

    @Override
    public String type() {
        return type;
    }

    public record Payload(String toUserId, String toDeviceId, String blob) {
    }
}
