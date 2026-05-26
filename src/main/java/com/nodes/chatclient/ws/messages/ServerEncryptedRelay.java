package com.nodes.chatclient.ws.messages;

public record ServerEncryptedRelay(Payload payload) {

    public static final class Payload {
        public String toUserId;
        public String toDeviceId;
        public String fromUserId;
        public String fromDeviceId;
        public byte[] blob;
    }
}
