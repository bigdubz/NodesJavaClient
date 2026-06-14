package com.nodes.chatclient.e2ee.types;

import java.util.Arrays;

public record BundleOneTimePrekey(int keyId, byte[] publicKey) {
    public BundleOneTimePrekey {
        publicKey = copy(publicKey);
    }

    public byte[] publicKey() {
        return copy(publicKey);
    }

    private static byte[] copy(byte[] bytes) {
        return bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }
}
