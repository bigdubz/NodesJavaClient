package com.nodes.chatclient.e2ee.types;

import com.nodes.chatclient.e2ee.crypto.MessageAuth;

public final class EncryptedMessage {
    public final String fromUserId;
    public final String fromDeviceId;
    public final String toUserId;
    public final String toDeviceId;

    public byte[] dhPublicKey;
    public long messageNumber;
    public final long previousChainLength;

    public byte[] iv;
    public byte[] cipherText; // encrypted payload
    public byte[] signature;

    public EncryptedMessage(String fromUserId, String fromDeviceId,
                            String toUserId, String toDeviceId,
                            byte[] dhPublicKey, long messageNumber, long previousChainLength,
                            byte[] iv, byte[] cipherText, byte[] signature) {
        this.fromUserId = fromUserId;
        this.fromDeviceId = fromDeviceId;
        this.toUserId = toUserId;
        this.toDeviceId = toDeviceId;

        this.dhPublicKey = dhPublicKey;
        this.messageNumber = messageNumber;
        this.previousChainLength = previousChainLength;

        this.iv = iv;
        this.cipherText = cipherText;
        this.signature = signature;
    }

    public void sign(byte[] signingKey) throws Exception {
        this.signature = MessageAuth.sign(MessageAuth.signatureInput(this), signingKey);
    }
}
