package com.nodes.chatclient.e2ee.db.records;

import java.util.Arrays;

public record SignedPrekeyRecord(
        int keyId,
        byte[] publicKey,
        byte[] privateKey,
        byte[] signature,
        long createdAt,
        boolean isActive
) {
    public SignedPrekeyRecord {
        publicKey = copy(publicKey);
        privateKey = copy(privateKey);
        signature = copy(signature);
    }

    public byte[] publicKey() {
        return copy(publicKey);
    }

    public byte[] privateKey() {
        return copy(privateKey);
    }

    public byte[] signature() {
        return copy(signature);
    }

    private static byte[] copy(byte[] bytes) {
        return bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }
}
