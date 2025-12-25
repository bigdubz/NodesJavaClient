package com.nodes.chatclient.store.model;

import java.util.Map;

public class ConversationUi {

    public final String peerId;
    public final Map<String, ChatMessage> messages;
    public String lastMessage = "";
    public long lastTimestamp = 0;
    public int unreadCount = 0;
    public boolean isOnline = false;

    public ConversationUi(
            String peerId,
            String lastMessage,
            long lastTimestamp,
            int unreadCount,
            boolean isOnline,
            Map<String, ChatMessage> messages
    ) {
        this.peerId = peerId;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
        this.unreadCount = unreadCount;
        this.isOnline = isOnline;
        this.messages = messages;
    }
}
