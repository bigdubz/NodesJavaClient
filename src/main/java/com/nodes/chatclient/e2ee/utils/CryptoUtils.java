package com.nodes.chatclient.e2ee.utils;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.interfaces.AEAD;
import com.goterl.lazysodium.interfaces.Sign;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

public class CryptoUtils {
    public static final LazySodiumJava ls = new LazySodiumJava(new SodiumJava());
    private static final String HMAC_ALGO = "HmacSHA256";

    // --- Libsodium Wrappers ---

    public static byte[] dh(byte[] privKey, byte[] pubKey) {
        byte[] shared = new byte[32];
        ls.cryptoScalarMult(shared, privKey, pubKey);
        return shared;
    }

    public static byte[] encrypt(byte[] key, byte[] nonce, byte[] message, byte[] ad) {
        // message + MAC tag (16 bytes)
        byte[] ciphertext = new byte[message.length + AEAD.XCHACHA20POLY1305_IETF_ABYTES];

        ls.cryptoAeadXChaCha20Poly1305IetfEncrypt(
                ciphertext,
                null,
                message,
                message.length,
                ad,
                ad.length,
                null,
                nonce,
                key
        );
        return ciphertext;
    }

    public static byte[] decrypt(byte[] key, byte[] nonce, byte[] ciphertext, byte[] ad) throws Exception {
        byte[] message = new byte[ciphertext.length - AEAD.XCHACHA20POLY1305_IETF_ABYTES];

        // Note: decrypt signature order is slightly different!
        boolean success = ls.cryptoAeadXChaCha20Poly1305IetfDecrypt(
                message,
                null,
                null,
                ciphertext,
                ciphertext.length,
                ad,
                ad.length,
                nonce,
                key
        );

        if (!success) throw new Exception("Decryption failed: Integrity check failed.");
        return message;
    }

    public static byte[] constructAD(
            String senderId, String senderDeviceId, String receiverId, String receiverDeviceId,
            long msgNum, byte[] pubKey
    ) {
        byte[] sBytes = senderId.getBytes(StandardCharsets.UTF_8);
        byte[] sdBytes = senderDeviceId.getBytes(StandardCharsets.UTF_8);
        byte[] rBytes = receiverId.getBytes(StandardCharsets.UTF_8);
        byte[] rdBytes = receiverDeviceId.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(
                4 + sBytes.length +
                4 + sdBytes.length +
                4 + rBytes.length +
                4 + rdBytes.length +
                8 + pubKey.length
        );
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.putInt(sBytes.length);
        buffer.put(sBytes);
        buffer.putInt(sdBytes.length);
        buffer.put(sdBytes);
        buffer.putInt(rBytes.length);
        buffer.put(rBytes);
        buffer.putInt(rdBytes.length);
        buffer.put(rdBytes);
        buffer.putLong(msgNum);
        buffer.put(pubKey);

        return buffer.array();
    }

    public static byte[] hash(byte[] input) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(input);
    }

    public static String ratchetId(byte[] dhPubKey, long msgNum) throws Exception {
        byte[] hash = hash(dhPubKey);
        return HexFormat.of().formatHex(hash) + ":" + msgNum;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    public static byte[][] kdfRoot(byte[] rootKey, byte[] dhOut) throws Exception {
        byte[] prk = hkdfExtract(rootKey, dhOut);
        byte[] okm = hkdfExpand(prk, "RootRatchet", 64);

        byte[] newRoot = Arrays.copyOfRange(okm, 0, 32);
        byte[] chain   = Arrays.copyOfRange(okm, 32, 64);

        return new byte[][]{newRoot, chain};
    }

    public static byte[] deriveMessageKey(byte[] chainKey) throws Exception {
        return hkdfExpand(chainKey, "MessageKey", 32);
    }

    public static byte[] advanceChainKey(byte[] chainKey) throws Exception {
        return hkdfExpand(chainKey, "ChainStep", 32);
    }

    public static byte[][] generateKeyPair() {
        byte[] priv = ls.randomBytesBuf(32);
        byte[] pub = new byte[32];
        ls.cryptoScalarMultBase(pub, priv);
        return new byte[][]{pub, priv};
    }

    public static byte[][] generateEd25519KeyPair() {
        byte[] pub = new byte[Sign.ED25519_PUBLICKEYBYTES];
        byte[] priv = new byte[Sign.ED25519_SECRETKEYBYTES];
        ls.cryptoSignKeypair(pub, priv);
        return new byte[][]{pub, priv};
    }

    public static byte[] sign(byte[] message, byte[] privKey) {
        byte[] signature = new byte[Sign.BYTES];
        ls.cryptoSignDetached(signature, message, message.length, privKey);
        return signature;
    }

    public static boolean verify(byte[] message, byte[] signature, byte[] pubKey) {
        return ls.cryptoSignVerifyDetached(signature, message, message.length, pubKey);
    }

    public static void writeField(DataOutputStream out, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public static void writeField(DataOutputStream out, byte[] value) throws Exception {
        out.writeInt(value.length);
        out.write(value);
    }

    public static void writeOptionalField(DataOutputStream out, byte[] value) throws Exception {
        if (value == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(value.length);
            out.write(value);
        }
    }

    public static byte[] signatureInput(EncryptedMessage msg) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeInt(0x454D5331); // "ENCRYPTED MESSAGE SIGNATURE V1 (EMS1)"
        // 4-byte signed big-endian length ^
        // raw bytes follow

        out.writeByte(1); // version number
        writeField(out, msg.fromUserId);
        writeField(out, msg.fromDeviceId);
        writeField(out, msg.toUserId);
        writeField(out, msg.toDeviceId);
        writeOptionalField(out, msg.dhPublicKey);
        out.writeLong(msg.messageNumber);
        out.writeLong(msg.previousChainLength);
        writeField(out, msg.iv);
        writeField(out, msg.cipherText);
        out.flush();

        return baos.toByteArray();
    }

    public static byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        if (salt == null || salt.length == 0) salt = new byte[32];
        mac.init(new SecretKeySpec(salt, HMAC_ALGO));
        return mac.doFinal(ikm);
    }

    public static byte[] hkdfExpand(byte[] prk, String info, int length) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(prk, HMAC_ALGO));
        byte[] okm = new byte[length];
        byte[] t = new byte[0];
        int generatedBytes = 0;
        byte counter = 1;
        while (generatedBytes < length) {
            mac.update(t);
            mac.update(info.getBytes());
            mac.update(counter);
            t = mac.doFinal();
            int toCopy = Math.min(length - generatedBytes, t.length);
            System.arraycopy(t, 0, okm, generatedBytes, toCopy);
            generatedBytes += toCopy;
            counter++;
        }
        return okm;
    }
}
