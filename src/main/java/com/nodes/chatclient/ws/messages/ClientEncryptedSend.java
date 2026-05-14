package com.nodes.chatclient.ws.messages;

public final class ClientEncryptedSend implements ClientMessage {
    public final String type = "ENCRYPTED_SEND";
    public final Payload payload;

    public ClientEncryptedSend(String toUserId, String toDeviceId, String blob) {
        this.payload = new Payload(toUserId, toDeviceId, blob);
    }

    @Override
    public String type() {
        return type;
    }

    public record Payload(String toUserId, String toDeviceId, String blob) {
    }
}
