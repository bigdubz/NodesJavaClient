package com.nodes.chatclient.store.model;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Conversation {
    public final String peerId;
    public final Map<String, ChatMessage> messages = new LinkedHashMap<>();

    public String lastMessage = "";
    public long lastTimestamp = 0;
    public int unreadCount = 0;

    public Conversation(String peerId) {
        this.peerId = peerId;
    }
}
