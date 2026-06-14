package com.nodes.chatclient.store.model;

import java.util.Map;

public record ChatMessageUi(String messageId, String fromUserId, String toUserId, String text, long createdAt,
                            String replyingTo, boolean delivered, boolean read, Map<String, String> reactions) {

}
