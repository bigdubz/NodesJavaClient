package com.nodes.chatclient.e2ee.types;

import com.nodes.chatclient.e2ee.protos.ProtoEncryptedPayload.EncryptedPayload;

public final class InternalMessage {
    public enum Type { TEXT, REACTION, CONTROL }
    public final Type type;

    public final String messageId;
    public String referencedMessageId;
    public final long createdAt;

    // Text fields
    public String body;

    // Reaction fields
    public String reaction;
    public boolean isRemoved;

    // Control fields
    public EncryptedPayload.ControlMessage.Type controlType;

    public static InternalMessage text(String messageId, long createdAt, String body, String referencedMessageId) {
        InternalMessage m = new InternalMessage(Type.TEXT, messageId, createdAt);
        m.body = body;
        m.referencedMessageId = referencedMessageId;
        return m;
    }

    public static InternalMessage reaction(String messageId, long createdAt,
                                           String referencedMessageId, String emoji, boolean isRemoved) {
        InternalMessage m = new InternalMessage(Type.REACTION, messageId, createdAt);
        m.referencedMessageId = referencedMessageId;
        m.reaction = emoji;
        m.isRemoved = isRemoved;
        return m;
    }

    public static InternalMessage control(String messageId, long createdAt,
                                          EncryptedPayload.ControlMessage.Type cType, String referencedMessageId) {
        InternalMessage m = new InternalMessage(Type.CONTROL, messageId, createdAt);
        m.controlType = cType;
        m.referencedMessageId = referencedMessageId;
        return m;
    }

    private InternalMessage(Type type, String messageId, long createdAt) {
        this.type = type;
        this.messageId = messageId;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "InternalMessage{" +
                "type=" + type +
                ", messageId='" + messageId + '\'' +
                ", referencedMessageId='" + referencedMessageId + '\'' +
                ", createdAt=" + createdAt +
                ", body='" + body + '\'' +
                ", reaction='" + reaction + '\'' +
                ", isRemoved=" + isRemoved +
                ", controlType=" + controlType +
                '}';
    }
}
