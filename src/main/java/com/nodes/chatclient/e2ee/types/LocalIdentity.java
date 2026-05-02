package com.nodes.chatclient.e2ee.types;

public final class LocalIdentity {

    private final String userId;
    private final String deviceId;
    private final int registrationId;

    private final byte[] identityPublicKey;
    private final byte[] identityPrivateKey;

    private final byte[] signingPublicKey;
    private final byte[] signingPrivateKey;

    private final long createdAt;

    // constructor + getters
    public LocalIdentity(String userId,
                         String deviceId,
                         int registrationId,
                         byte[] identityPublicKey,
                         byte[] identityPrivateKey,
                         byte[] signingPublicKey,
                         byte[] signingPrivateKey,
                         long createdAt) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.registrationId = registrationId;
        this.identityPublicKey = identityPublicKey;
        this.identityPrivateKey = identityPrivateKey;
        this.signingPublicKey = signingPublicKey;
        this.signingPrivateKey = signingPrivateKey;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public int getRegistrationId() {
        return registrationId;
    }

    public byte[] getIdentityPublicKey() {
        return identityPublicKey;
    }

    public byte[] getIdentityPrivateKey() {
        return identityPrivateKey;
    }

    public byte[] getSigningPublicKey() {
        return signingPublicKey;
    }

    public byte[] getSigningPrivateKey() {
        return signingPrivateKey;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}