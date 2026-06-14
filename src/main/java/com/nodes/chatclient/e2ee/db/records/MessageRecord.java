package com.nodes.chatclient.e2ee.db.records;

public final class MessageRecord {
    public String messageId;
    public String conversationId;

    public String senderUserId;
    public String senderDeviceId;

    public long createdAt;
    public long receivedAt;

    public boolean isOutgoing;

    public int type;
    public int deliveryStatus;

    public Integer controlType;

    public String body;

    public String reaction;
    public Integer reactionIsRemoved;

    public String referencedMessageId;
}
