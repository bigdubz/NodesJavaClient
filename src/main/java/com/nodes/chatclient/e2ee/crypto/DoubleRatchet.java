package com.nodes.chatclient.e2ee.crypto;

import com.nodes.chatclient.e2ee.mappers.PayloadMapper;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;
import com.nodes.chatclient.e2ee.types.InternalMessage;
import com.nodes.chatclient.e2ee.types.Session;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class DoubleRatchet {

    private DoubleRatchet() {
    }

    public static InternalMessage decrypt(Session session, EncryptedMessage msg) throws Exception {
        if (session.remoteSigningPublicKey == null) {
            throw new Exception("Missing remote signing public key for session");
        }
        if (msg.signature == null) {
            throw new Exception("Missing message signature");
        }
        if (!MessageAuth.verify(MessageAuth.signatureInput(msg), msg.signature, session.remoteSigningPublicKey)) {
            throw new Exception("Invalid message signature");
        }

        byte[] messageKey;
        String id = KeyDerivation.ratchetId(msg.dhPublicKey, msg.messageNumber);
        if (session.skippedKeys.containsKey(id)) {
            messageKey = session.skippedKeys.remove(id);
            return decryptWithKey(messageKey, msg);
        }

        if (session.remoteDHPublicKey == null ||
                !Arrays.equals(msg.dhPublicKey, session.remoteDHPublicKey)) {
            skipKeys(session, session.remoteDHPublicKey, msg.previousChainLength);
            advanceReceivingRatchet(session, msg.dhPublicKey);
        }

        if (!Arrays.equals(msg.dhPublicKey, session.remoteDHPublicKey)) {
            throw new Exception("Missing skipped key for old message");
        }

        if (msg.messageNumber < session.receivingMessageNumber) {
            throw new Exception("old message");
        }

        skipKeys(session, msg.dhPublicKey, msg.messageNumber);

        messageKey = KeyDerivation.deriveMessageKey(session.receivingChainKey);
        InternalMessage plain = decryptWithKey(messageKey, msg);

        session.receivingChainKey = KeyDerivation.advanceChainKey(session.receivingChainKey);
        session.receivingMessageNumber++;

        return plain;
    }

    public static EncryptedMessage encrypt(Session session, String message, String fromUserId, String toUserId)
            throws Exception {
        InternalMessage internalMessage = InternalMessage.text(
                java.util.UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                message,
                null
        );

        return encrypt(session, internalMessage, fromUserId, toUserId);
    }

    public static EncryptedMessage encrypt(Session session, InternalMessage internalMessage, String fromUserId, String toUserId)
            throws Exception {
        if (session.signingPrivateKey == null) {
            throw new Exception("Missing signing private key for local session");
        }
        if (session.sendingChainKey == null) {
            if (session.remoteDHPublicKey == null) {
                throw new Exception("Missing sending chain and remote ratchet public key for session");
            }
            advanceSendingRatchet(session);
        }
        if (session.dhPublicKey == null) {
            throw new Exception("Missing local ratchet public key for session");
        }

        byte[] messageKey = KeyDerivation.deriveMessageKey(session.sendingChainKey);
        byte[] iv = Sodium.INSTANCE.randomBytesBuf(24);

        byte[] associatedData = constructAssociatedData(
                fromUserId,
                session.localDeviceId,
                toUserId,
                session.remoteDeviceId,
                session.sendingMessageNumber,
                session.dhPublicKey
        );

        byte[] serialized = PayloadMapper.serialize(internalMessage);
        byte[] cipherText = AeadCipher.encrypt(messageKey, iv, serialized, associatedData);

        EncryptedMessage msg = new EncryptedMessage(
                fromUserId,
                session.localDeviceId,
                toUserId,
                session.remoteDeviceId,
                session.dhPublicKey,
                session.sendingMessageNumber,
                session.previousChainLength,
                iv,
                cipherText,
                null
        );

        msg.sign(session.signingPrivateKey);

        session.sendingChainKey = KeyDerivation.advanceChainKey(session.sendingChainKey);
        session.sendingMessageNumber++;

        return msg;
    }

    private static void skipKeys(Session session, byte[] ratchetPublicKey, long until) throws Exception {
        if (until - session.receivingMessageNumber > 100) {
            throw new Exception("Too many messages");
        }

        while (session.receivingMessageNumber < until) {
            byte[] skipped = KeyDerivation.deriveMessageKey(session.receivingChainKey);
            session.skippedKeys.put(
                    KeyDerivation.ratchetId(ratchetPublicKey, session.receivingMessageNumber),
                    skipped
            );

            session.receivingChainKey = KeyDerivation.advanceChainKey(session.receivingChainKey);
            session.receivingMessageNumber++;
        }
    }

    private static InternalMessage decryptWithKey(byte[] key, EncryptedMessage msg) throws Exception {
        byte[] associatedData = constructAssociatedData(
                msg.fromUserId,
                msg.fromDeviceId,
                msg.toUserId,
                msg.toDeviceId,
                msg.messageNumber,
                msg.dhPublicKey
        );
        byte[] plain = AeadCipher.decrypt(key, msg.iv, msg.cipherText, associatedData);

        return PayloadMapper.deserialize(plain);
    }

    private static void advanceReceivingRatchet(Session session, byte[] newRemotePublicKey) throws Exception {
        session.previousRemoteDHPublicKey = session.remoteDHPublicKey;

        byte[] dh1 = KeyMaterial.dh(session.dhPrivateKey, newRemotePublicKey);
        byte[][] rk1 = KeyDerivation.kdfRoot(session.rootKey, dh1);

        session.rootKey = rk1[0];
        session.receivingChainKey = rk1[1];
        session.remoteDHPublicKey = newRemotePublicKey;
        session.receivingMessageNumber = 0;

        advanceSendingRatchet(session);
    }

    private static void advanceSendingRatchet(Session session) throws Exception {
        session.previousChainLength = session.sendingMessageNumber;

        byte[][] keys = KeyMaterial.generateX25519KeyPair();
        session.dhPublicKey = keys[0];
        session.dhPrivateKey = keys[1];

        byte[] dh2 = KeyMaterial.dh(session.dhPrivateKey, session.remoteDHPublicKey);
        byte[][] rk2 = KeyDerivation.kdfRoot(session.rootKey, dh2);

        session.rootKey = rk2[0];
        session.sendingChainKey = rk2[1];
        session.sendingMessageNumber = 0;
    }

    private static byte[] constructAssociatedData(
            String senderId,
            String senderDeviceId,
            String receiverId,
            String receiverDeviceId,
            long messageNumber,
            byte[] publicKey
    ) {
        byte[] senderBytes = senderId.getBytes(StandardCharsets.UTF_8);
        byte[] senderDeviceBytes = senderDeviceId.getBytes(StandardCharsets.UTF_8);
        byte[] receiverBytes = receiverId.getBytes(StandardCharsets.UTF_8);
        byte[] receiverDeviceBytes = receiverDeviceId.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(
                4 + senderBytes.length +
                4 + senderDeviceBytes.length +
                4 + receiverBytes.length +
                4 + receiverDeviceBytes.length +
                8 + publicKey.length
        );
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.putInt(senderBytes.length);
        buffer.put(senderBytes);
        buffer.putInt(senderDeviceBytes.length);
        buffer.put(senderDeviceBytes);
        buffer.putInt(receiverBytes.length);
        buffer.put(receiverBytes);
        buffer.putInt(receiverDeviceBytes.length);
        buffer.put(receiverDeviceBytes);
        buffer.putLong(messageNumber);
        buffer.put(publicKey);

        return buffer.array();
    }
}
