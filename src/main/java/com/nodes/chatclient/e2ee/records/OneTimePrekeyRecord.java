package com.nodes.chatclient.e2ee.records;

import java.util.Arrays;

public record OneTimePrekeyRecord(
        int keyId,
        byte[] publicKey,
        byte[] privateKey,
        boolean isUsed
) {
    public OneTimePrekeyRecord {
        publicKey = copy(publicKey);
        privateKey = copy(privateKey);
    }

    public byte[] publicKey() {
        return copy(publicKey);
    }

    public byte[] privateKey() {
        return copy(privateKey);
    }

    private static byte[] copy(byte[] bytes) {
        return bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }
}
