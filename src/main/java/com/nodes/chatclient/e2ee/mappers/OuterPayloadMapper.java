package com.nodes.chatclient.e2ee.mappers;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nodes.chatclient.e2ee.protos.ProtoOuterPayload;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
        if (msg.senderSigningKey != null) {
            payload.setSenderSigningKey(ByteString.copyFrom(msg.senderSigningKey));
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
            try {
                byte[] decoded = Base64.getDecoder().decode(encoded);
                return deserialize(Base64.getDecoder().decode(new String(decoded, StandardCharsets.US_ASCII)));
            } catch (IllegalArgumentException ignored) {
                throw new IllegalArgumentException("Invalid outer payload base64/protobuf: " + describeEncoded(encoded), e);
            }
        }
    }

    public static EncryptedMessage deserializeRelayBlob(JsonNode blob) {
        if (blob == null || blob.isNull()) {
            throw new IllegalArgumentException("Missing encrypted relay blob");
        }

        if (blob.isTextual()) {
            return deserializeBase64(blob.asText());
        }

        throw new IllegalArgumentException("Unsupported encrypted relay blob shape: " + blob.getNodeType());
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
            if (!proto.getSenderSigningKey().isEmpty()) {
                message.senderSigningKey = proto.getSenderSigningKey().toByteArray();
            }
            if (proto.hasOneTimePrekeyId()) {
                message.oneTimePrekeyId = proto.getOneTimePrekeyId();
            }
            return message;
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Invalid outer payload bytes", e);
        }
    }

    private static String describeEncoded(String encoded) {
        if (encoded == null) {
            return "null";
        }

        int length = encoded.length();
        String prefix = encoded.substring(0, Math.min(24, length));
        return "length=" + length + ", prefix=" + prefix;
    }
}
