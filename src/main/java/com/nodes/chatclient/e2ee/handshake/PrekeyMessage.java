package com.nodes.chatclient.e2ee.handshake;

import java.util.Arrays;

public record PrekeyMessage(String fromUserId, String fromDeviceId, byte[] senderIdentityKey, byte[] senderEphemeralKey,
                            int signedPrekeyId, Integer oneTimePrekeyId) {
    public PrekeyMessage(
            String fromUserId,
            String fromDeviceId,
            byte[] senderIdentityKey,
            byte[] senderEphemeralKey,
            int signedPrekeyId,
            Integer oneTimePrekeyId
    ) {
        this.fromUserId = fromUserId;
        this.fromDeviceId = fromDeviceId;
        this.senderIdentityKey = copy(senderIdentityKey);
        this.senderEphemeralKey = copy(senderEphemeralKey);
        this.signedPrekeyId = signedPrekeyId;
        this.oneTimePrekeyId = oneTimePrekeyId;
    }

    @Override
    public byte[] senderIdentityKey() {
        return copy(senderIdentityKey);
    }

    @Override
    public byte[] senderEphemeralKey() {
        return copy(senderEphemeralKey);
    }

    public boolean hasOneTimePrekey() {
        return oneTimePrekeyId != null;
    }

    private static byte[] copy(byte[] input) {
        return input == null ? null : Arrays.copyOf(input, input.length);
    }
}
