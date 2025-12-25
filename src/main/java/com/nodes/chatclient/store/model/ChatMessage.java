package com.nodes.chatclient.store.model;

import java.util.HashMap;
import java.util.Map;

public final class ChatMessage {

    public String messageId;
    public final String fromUserId;
    public final String toUserId;
    public final String text;
    public final long createdAt;
    public final String replyingTo;

    public boolean delivered = false;
    public boolean read = false;

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

    public ChatMessageUi toUi() {
        return new ChatMessageUi(
                messageId,
                fromUserId,
                toUserId,
                text,
                createdAt,
                replyingTo,
                delivered,
                read,
                Map.copyOf(reactions)
        );
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

    public static ChatMessage outgoing(
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
