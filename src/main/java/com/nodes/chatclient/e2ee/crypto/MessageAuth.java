package com.nodes.chatclient.e2ee.crypto;

import com.goterl.lazysodium.interfaces.Sign;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

public final class MessageAuth {

    private MessageAuth() {
    }

    public static byte[] sign(byte[] message, byte[] privateKey) {
        byte[] signature = new byte[Sign.BYTES];
        Sodium.INSTANCE.cryptoSignDetached(signature, message, message.length, privateKey);
        return signature;
    }

    public static boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
        return Sodium.INSTANCE.cryptoSignVerifyDetached(signature, message, message.length, publicKey);
    }

    public static byte[] signatureInput(EncryptedMessage msg) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeInt(0x454D5331); // "EMS1"
        out.writeByte(1);
        writeField(out, msg.fromUserId);
        writeField(out, msg.fromDeviceId);
        writeField(out, msg.toUserId);
        writeField(out, msg.toDeviceId);

        writeOptionalField(out, msg.dhPublicKey);
        out.writeLong(msg.messageNumber);
        out.writeLong(msg.previousChainLength);

        writeField(out, msg.iv);
        writeField(out, msg.cipherText);


        writeOptionalField(out, msg.senderIdentityKey);
        writeOptionalField(out, msg.senderSigningKey);
        writeOptionalInt(out, msg.oneTimePrekeyId);
        out.flush();

        return baos.toByteArray();
    }

    private static void writeField(DataOutputStream out, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static void writeField(DataOutputStream out, byte[] value) throws Exception {
        out.writeInt(value.length);
        out.write(value);
    }

    private static void writeOptionalField(DataOutputStream out, byte[] value) throws Exception {
        if (value == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(value.length);
            out.write(value);
        }
    }

    private static void writeOptionalInt(DataOutputStream out, Integer value) throws Exception {
        if (value == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeInt(value);
        }
    }
}
