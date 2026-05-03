package com.nodes.chatclient.e2ee.types;

public record RemoteUserKeyBundle(String userId, String deviceId, int registrationId, byte[] signingPublicKey,
                                  byte[] identityPublicKey, byte[] signedPrekey, byte[] signedPrekeySignature,
                                  byte[] oneTimePrekey) {
}
