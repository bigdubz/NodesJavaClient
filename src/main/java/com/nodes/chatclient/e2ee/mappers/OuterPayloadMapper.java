package com.nodes.chatclient.e2ee.mappers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nodes.chatclient.e2ee.protos.ProtoOuterPayload;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;

import java.util.Base64;

public final class OuterPayloadMapper {
    public static byte[] serialize(EncryptedMessage msg) {
        ProtoOuterPayload.OuterPayload.Builder payload = ProtoOuterPayload.OuterPayload.newBuilder()
                .setFromUserId(msg.fromUserId)
                .setFromDeviceId(msg.fromDeviceId)
                .setToUserId(msg.toUserId)
                .setToDeviceId(msg.toDeviceId)
                .setMessageNumber(msg.messageNumber)
                .setPreviousChainLength(msg.previousChainLength)
                .setIv(ByteString.copyFrom(msg.iv))
                .setCiphertext(ByteString.copyFrom(msg.cipherText))
                .setSignature(ByteString.copyFrom(msg.signature));

        if (msg.dhPublicKey != null) {
            payload.setDhPublicKey(ByteString.copyFrom(msg.dhPublicKey));
        }
        if (msg.senderIdentityKey != null) {
            payload.setSenderIdentityKey(ByteString.copyFrom(msg.senderIdentityKey));
        }
        if (msg.oneTimePrekeyId != null) {
            payload.setOneTimePrekeyId(msg.oneTimePrekeyId);
        }

        return payload.build().toByteArray();
    }

    public static EncryptedMessage deserializeBase64(String encoded) {
        try {
            return deserialize(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid outer payload base64", e);
        }
    }

    public static EncryptedMessage deserialize(byte[] bytes) {
        try {
            ProtoOuterPayload.OuterPayload proto = ProtoOuterPayload.OuterPayload.parseFrom(bytes);
            EncryptedMessage message = new EncryptedMessage(
                    proto.getFromUserId(),
                    proto.getFromDeviceId(),
                    proto.getToUserId(),
                    proto.getToDeviceId(),
                    proto.getDhPublicKey().toByteArray(),
                    proto.getMessageNumber(),
                    proto.getPreviousChainLength(),
                    proto.getIv().toByteArray(),
                    proto.getCiphertext().toByteArray(),
                    proto.getSignature().toByteArray()
            );
            if (!proto.getSenderIdentityKey().isEmpty()) {
                message.senderIdentityKey = proto.getSenderIdentityKey().toByteArray();
            }
            if (proto.hasOneTimePrekeyId()) {
                message.oneTimePrekeyId = proto.getOneTimePrekeyId();
            }
            return message;
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Invalid outer payload bytes", e);
        }
    }
}
