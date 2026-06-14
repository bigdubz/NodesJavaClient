package com.nodes.chatclient.e2ee.crypto;

import com.nodes.chatclient.e2ee.mappers.PayloadMapper;
import com.nodes.chatclient.e2ee.protos.ProtoOuterPayload;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;
import com.nodes.chatclient.e2ee.types.InternalMessage;
import com.nodes.chatclient.e2ee.types.Session;
import com.nodes.chatclient.util.Helper;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class ControlChannel {
    private static final String CONTROL_AD_LABEL = "control-v1";

    private ControlChannel() {
    }

    public static EncryptedMessage encrypt(
            Session session,
            InternalMessage internalMessage,
            String fromUserId,
            String toUserId
    ) throws Exception {
        if (session.signingPrivateKey == null) {
            throw new Exception("Missing signing private key for local session");
        }
        if (session.rootKey == null) {
            throw new Exception("Missing root key for control channel");
        }
        if (session.dhPublicKey == null) {
            throw new Exception("Missing local ratchet public key for control channel");
        }

        byte[] key = KeyDerivation.deriveControlMessageKey(session.rootKey);
        byte[] iv = Sodium.INSTANCE.randomBytesBuf(24);
        byte[] associatedData = constructAssociatedData(
                fromUserId,
                session.localDeviceId,
                toUserId,
                session.remoteDeviceId,
                session.dhPublicKey
        );

        byte[] serialized = PayloadMapper.serialize(internalMessage);
        byte[] cipherText = AeadCipher.encrypt(key, iv, serialized, associatedData);

        EncryptedMessage msg = new EncryptedMessage(
                fromUserId,
                session.localDeviceId,
                toUserId,
                session.remoteDeviceId,
                session.dhPublicKey,
                0,
                0,
                iv,
                cipherText,
                null,
                ProtoOuterPayload.OuterPayload.Channel.CONTROL
        );
        msg.sign(session.signingPrivateKey);

        return msg;
    }

    public static InternalMessage decrypt(Session session, EncryptedMessage msg) throws Exception {
        if (session.remoteSigningPublicKey == null) {
            throw new Exception("Missing remote signing public key for session");
        }
        if (msg.signature == null) {
            throw new Exception("Missing control message signature");
        }
        if (!MessageAuth.verify(MessageAuth.signatureInput(msg), msg.signature, session.remoteSigningPublicKey)) {
            throw new Exception("Invalid control message signature");
        }
        if (session.rootKey == null) {
            throw new Exception("Missing root key for control channel");
        }

        byte[] key = KeyDerivation.deriveControlMessageKey(session.rootKey);
        byte[] associatedData = constructAssociatedData(
                msg.fromUserId,
                msg.fromDeviceId,
                msg.toUserId,
                msg.toDeviceId,
                msg.dhPublicKey
        );
        byte[] plain = AeadCipher.decrypt(key, msg.iv, msg.cipherText, associatedData);

        return PayloadMapper.deserialize(plain);
    }

    private static byte[] constructAssociatedData(
            String senderId,
            String senderDeviceId,
            String receiverId,
            String receiverDeviceId,
            byte[] publicKey
    ) {
        byte[] labelBytes = CONTROL_AD_LABEL.getBytes(StandardCharsets.UTF_8);
        byte[] channelBytes = ProtoOuterPayload.OuterPayload.Channel.CONTROL.name().getBytes(StandardCharsets.UTF_8);
        byte[] senderBytes = senderId.getBytes(StandardCharsets.UTF_8);
        byte[] senderDeviceBytes = senderDeviceId.getBytes(StandardCharsets.UTF_8);
        byte[] receiverBytes = receiverId.getBytes(StandardCharsets.UTF_8);
        byte[] receiverDeviceBytes = receiverDeviceId.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(
                4 + labelBytes.length +
                4 + channelBytes.length +
                4 + senderBytes.length +
                4 + senderDeviceBytes.length +
                4 + receiverBytes.length +
                4 + receiverDeviceBytes.length +
                4 + publicKey.length
        );
        Helper.loadToBuffer(buffer,
                labelBytes,
                channelBytes,
                senderBytes,
                senderDeviceBytes,
                receiverBytes,
                receiverDeviceBytes);

        buffer.putInt(publicKey.length);
        buffer.put(publicKey);

        return buffer.array();
    }
}
