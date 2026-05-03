package com.nodes.chatclient.e2ee.handshake;

import java.util.Arrays;

public record PreKeyMessage(String fromUserId, String fromDeviceId, byte[] senderIdentityKey, byte[] senderEphemeralKey,
                            int signedPreKeyId, Integer oneTimePreKeyId) {
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

    @Override
    public byte[] senderIdentityKey() {
        return copy(senderIdentityKey);
    }

    @Override
    public byte[] senderEphemeralKey() {
        return copy(senderEphemeralKey);
    }

    public boolean hasOneTimePreKey() {
        return oneTimePreKeyId != null;
    }

    private static byte[] copy(byte[] input) {
        return input == null ? null : Arrays.copyOf(input, input.length);
    }
}
