package com.nodes.chatclient.e2ee.types;

import com.nodes.chatclient.e2ee.crypto.MessageAuth;

public final class EncryptedMessage {
    public final String fromUserId;
    public final String fromDeviceId;
    public final String toUserId;
    public final String toDeviceId;

    public final byte[] dhPublicKey;
    public final long messageNumber;
    public final long previousChainLength;

    public final byte[] iv;
    public final byte[] cipherText; // encrypted payload
    public byte[] signature;
    public byte[] senderIdentityKey;
    public byte[] senderSigningKey;
    public Integer oneTimePrekeyId;

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

    public void attachPrekeyMetadata(byte[] senderIdentityKey, byte[] senderSigningKey, Integer oneTimePrekeyId) {
        this.senderIdentityKey = senderIdentityKey;
        this.senderSigningKey = senderSigningKey;
        this.oneTimePrekeyId = oneTimePrekeyId;
    }

    public void sign(byte[] signingKey) throws Exception {
        this.signature = MessageAuth.sign(MessageAuth.signatureInput(this), signingKey);
    }
}
