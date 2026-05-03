package com.nodes.chatclient.e2ee.mappers;

import com.nodes.chatclient.e2ee.types.InternalMessage;
import com.nodes.chatclient.e2ee.protos.ProtoEncryptedPayload.EncryptedPayload;

public class PayloadMapper {

    public static byte[] serialize(InternalMessage msg) {
        EncryptedPayload.Builder payload = EncryptedPayload.newBuilder()
                .setMessageId(msg.messageId)
                .setCreatedAt(msg.createdAt);

        switch (msg.type) {
            case TEXT:
                EncryptedPayload.TextMessage.Builder tb = EncryptedPayload.TextMessage.newBuilder()
                                .setBody(msg.body);

                if (msg.referencedMessageId != null) {
                    tb.setReferencedMessageId(msg.referencedMessageId);
                }
                payload.setText(tb.build());
                break;

            case REACTION:
                payload.setReaction(EncryptedPayload.ReactionMessage.newBuilder()
                        .setReferencedMessageId(msg.referencedMessageId)
                        .setReaction(msg.reaction)
                        .setIsRemoved(msg.isRemoved)
                        .build());
                break;

            case CONTROL:
                EncryptedPayload.ControlMessage.Builder cb = EncryptedPayload.ControlMessage.newBuilder()
                        .setType(msg.controlType);
                if (msg.referencedMessageId != null) cb.setReferencedMessageId(msg.referencedMessageId);
                payload.setControl(cb.build());
                break;
        }

        return payload.build().toByteArray();
    }

    public static InternalMessage deserialize(byte[] bytes) throws Exception {
        EncryptedPayload proto = EncryptedPayload.parseFrom(bytes);

        switch (proto.getContentCase()) {
            case TEXT:
                EncryptedPayload.TextMessage text = proto.getText();
                return InternalMessage.text(
                        proto.getMessageId(),
                        proto.getCreatedAt(),
                        text.getBody(),
                        text.hasReferencedMessageId() ? text.getReferencedMessageId() : null
                );

            case REACTION:
                EncryptedPayload.ReactionMessage reaction = proto.getReaction();
                return InternalMessage.reaction(
                        proto.getMessageId(),
                        proto.getCreatedAt(),
                        reaction.getReferencedMessageId(),
                        reaction.getReaction(),
                        reaction.getIsRemoved()
                );

            case CONTROL:
                EncryptedPayload.ControlMessage control = proto.getControl();
                if ((control.getType() == EncryptedPayload.ControlMessage.Type.ACK ||
                    control.getType() == EncryptedPayload.ControlMessage.Type.READ_RECEIPT) &&
                    !control.hasReferencedMessageId()) {
                    throw new IllegalStateException("ACK/READ RECEIPTS messages must have a referenced message ID");
                }
                return InternalMessage.control(
                        proto.getMessageId(),
                        proto.getCreatedAt(),
                        control.getType(),
                        control.hasReferencedMessageId() ? control.getReferencedMessageId() : null
                );

            default:
                throw new Exception("Unknown encrypted payload content.");
        }
    }
}
