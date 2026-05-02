package com.nodes.chatclient.e2ee.handshake;

import java.util.Arrays;

public class PreKeyMessage {
    private final String fromUserId;
    private final String fromDeviceId;

    private final byte[] senderIdentityKey;
    private final byte[] senderEphemeralKey;

    private final int signedPreKeyId;
    private final Integer oneTimePreKeyId;

    public PreKeyMessage(
            String fromUserId,
            String fromDeviceId,
            byte[] senderIdentityKey,
            byte[] senderEphemeralKey,
            int signedPreKeyId,
            Integer oneTimePreKeyId
    ) {
        this.fromUserId = fromUserId;
        this.fromDeviceId = fromDeviceId;
        this.senderIdentityKey = copy(senderIdentityKey);
        this.senderEphemeralKey = copy(senderEphemeralKey);
        this.signedPreKeyId = signedPreKeyId;
        this.oneTimePreKeyId = oneTimePreKeyId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getFromDeviceId() {
        return fromDeviceId;
    }

    public byte[] getSenderIdentityKey() {
        return copy(senderIdentityKey);
    }

    public byte[] getSenderEphemeralKey() {
        return copy(senderEphemeralKey);
    }

    public int getSignedPreKeyId() {
        return signedPreKeyId;
    }

    public Integer getOneTimePreKeyId() {
        return oneTimePreKeyId;
    }

    public boolean hasOneTimePreKey() {
        return oneTimePreKeyId != null;
    }

    private static byte[] copy(byte[] input) {
        return input == null ? null : Arrays.copyOf(input, input.length);
    }
}
