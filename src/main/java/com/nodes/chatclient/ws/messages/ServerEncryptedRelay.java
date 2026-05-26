package com.nodes.chatclient.ws.messages;

import com.fasterxml.jackson.databind.JsonNode;

public record ServerEncryptedRelay(Payload payload) {

    public static final class Payload {
        public String toUserId;
        public String toDeviceId;
        public String fromUserId;
        public String fromDeviceId;
        public JsonNode blob;
    }
}
