package com.nodes.chatclient.store.model;

import java.util.Map;

public record ConversationUi(String peerId, String lastMessage, long lastTimestamp, int unreadCount, boolean isOnline,
                             boolean isTyping, Map<String, ChatMessage> messages) {

}
