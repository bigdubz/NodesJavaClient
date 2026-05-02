package com.nodes.chatclient.e2ee.types;

import java.util.List;

public class UserKeyBundle {
    public final String userId;
    public final String deviceId;
    public final int registrationId;

    public final byte[] signingPublicKey;
    public final byte[] identityPublicKey;
    public final byte[] signedPrekey;
    public final byte[] signedPrekeySignature;

    public final List<byte[]> oneTimePrekeys;

    public UserKeyBundle(String userId,
                         String deviceId,
                         int registrationId,
                         byte[] signingPublicKey,
                         byte[] identityPublicKey,
                         byte[] signedPrekey,
                         byte[] signedPrekeySignature,
                         List<byte[]> oneTimePrekeys) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.registrationId = registrationId;
        this.signingPublicKey = signingPublicKey;
        this.identityPublicKey = identityPublicKey;
        this.signedPrekey = signedPrekey;
        this.signedPrekeySignature = signedPrekeySignature;
        this.oneTimePrekeys = oneTimePrekeys;
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

    public byte[] getSigningPublicKey() {
        return signingPublicKey;
    }

    public byte[] getIdentityPublicKey() {
        return identityPublicKey;
    }

    public byte[] getSignedPrekey() {
        return signedPrekey;
    }

    public byte[] getSignedPrekeySignature() {
        return signedPrekeySignature;
    }

    public List<byte[]> getOneTimePrekeys() {
        return oneTimePrekeys;
    }
}
