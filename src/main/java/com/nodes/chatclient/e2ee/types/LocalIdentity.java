package com.nodes.chatclient.e2ee.types;

import java.util.Arrays;

public record LocalIdentity(String userId, String deviceId, int registrationId, byte[] identityPublicKey,
                            byte[] identityPrivateKey, byte[] signingPublicKey, byte[] signingPrivateKey,
                            long createdAt) {

    public LocalIdentity {
        identityPublicKey = copy(identityPublicKey);
        identityPrivateKey = copy(identityPrivateKey);
        signingPublicKey = copy(signingPublicKey);
        signingPrivateKey = copy(signingPrivateKey);
    }

    public byte[] identityPublicKey() {
        return copy(identityPublicKey);
    }

    public byte[] identityPrivateKey() {
        return copy(identityPrivateKey);
    }

    public byte[] signingPublicKey() {
        return copy(signingPublicKey);
    }

    public byte[] signingPrivateKey() {
        return copy(signingPrivateKey);
    }

    private static byte[] copy(byte[] bytes) {
        return bytes == null ? null : Arrays.copyOf(bytes, bytes.length);
    }
}
