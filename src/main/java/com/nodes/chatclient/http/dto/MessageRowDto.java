package com.nodes.chatclient.http.dto;

import java.util.Map;

public final class MessageRowDto {
    public String messageId;
    public String fromUserId;
    public String toUserId;
    public String text;
    public long createdAt;

    public int delivered; // 0 / 1
    public int seen;      // 0 / 1

    public String replyingTo;

    // userId -> reaction
    public Map<String, String> reactions;
}
