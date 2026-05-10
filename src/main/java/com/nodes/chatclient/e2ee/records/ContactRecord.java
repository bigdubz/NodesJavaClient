package com.nodes.chatclient.e2ee.records;

public record ContactRecord(
        String userId,
        String deviceId,
        byte[] identityKey,
        byte[] signingKey
) {
}
