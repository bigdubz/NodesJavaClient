package com.nodes.chatclient.store.model;

import java.util.Map;

public final class ChatMessageUi {
    public final String messageId;
    public final String fromUserId;
    public final String toUserId;
    public final String text;
    public final long createdAt;
    public final String replyingTo;
    public final boolean delivered;
    public boolean read;
    public final Map<String, String> reactions;

    public ChatMessageUi(
            String messageId,
            String fromUserId,
            String toUserId,
            String text,
            long createdAt,
            String replyingTo,
            boolean delivered,
            boolean read,
            Map<String, String> reactions
    ) {
        this.messageId = messageId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.text = text;
        this.createdAt = createdAt;
        this.replyingTo = replyingTo;
        this.delivered = delivered;
        this.read = read;
        this.reactions = reactions;
    }

}
