package com.nodes.chatclient.store.model;

import java.util.Map;

public final class ConversationUi {

    public final String peerId;
    public final Map<String, ChatMessage> messages;
    public String lastMessage;
    public long lastTimestamp;
    public int unreadCount;
    public boolean isOnline;
    public boolean isTyping;

    public ConversationUi(
            String peerId,
            String lastMessage,
            long lastTimestamp,
            int unreadCount,
            boolean isOnline,
            boolean isTyping,
            Map<String, ChatMessage> messages
    ) {
        this.peerId = peerId;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
        this.unreadCount = unreadCount;
        this.isOnline = isOnline;
        this.isTyping = isTyping;
        this.messages = messages;
    }
}
