package com.nodes.chatclient.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class WsEnvelope {
    public String type;
    public JsonNode payload;

    public WsEnvelope() {

    }

    public WsEnvelope(String type, JsonNode payload) {
        this.type = type;
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "WsEnvelope{" +
                "type='" + type + '\'' +
                ", payload=" + payload +
                '}';
    }
}
