package com.nodes.chatclient.store.model;

import java.util.HashMap;
import java.util.Map;

public final class ChatMessage {

    public final String messageId;
    public final String fromUserId;
    public final String toUserId;
    public final String text;
    public final long createdAt;
    public final String replyingTo;

    public boolean delivered;
    public boolean read;

    public final Map<String, String> reactions = new HashMap<>();

    private ChatMessage(
            String messageId,
            String fromUserId,
            String toUserId,
            String text,
            long createdAt,
            String replyingTo
    ) {
        this.messageId = messageId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.text = text;
        this.createdAt = createdAt;
        this.replyingTo = replyingTo;
    }

    public static ChatMessage incoming(
            String messageId,
            String fromUserId,
            String toUserId,
            String text,
            long createdAt,
            String replyingTo
    ) {
        return new ChatMessage(
                messageId,
                fromUserId,
                toUserId,
                text,
                createdAt,
                replyingTo
        );
    }

    public static ChatMessage fromHistory(
            String messageId,
            String fromUserId,
            String toUserId,
            String text,
            long createdAt,
            String replyingTo
    ) {
        return new ChatMessage(
                messageId,
                fromUserId,
                toUserId,
                text,
                createdAt,
                replyingTo
        );
    }
}
