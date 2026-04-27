package com.nodes.chatclient.e2ee.mappers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nodes.chatclient.e2ee.protos.ProtoOuterPayload;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;

public class OuterPayloadMapper {
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

        return payload.build().toByteArray();
    }

    public static EncryptedMessage deserialize(byte[] bytes) {
        try {
            ProtoOuterPayload.OuterPayload proto = ProtoOuterPayload.OuterPayload.parseFrom(bytes);
            return new EncryptedMessage(
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
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Invalid outer payload bytes", e);
        }
    }
}
