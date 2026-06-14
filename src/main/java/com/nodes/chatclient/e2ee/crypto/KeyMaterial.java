package com.nodes.chatclient.e2ee.crypto;

import com.goterl.lazysodium.interfaces.Sign;

import java.security.MessageDigest;

public final class KeyMaterial {

    private KeyMaterial() {
    }

    public static byte[] dh(byte[] privateKey, byte[] publicKey) {
        byte[] shared = new byte[32];
        Sodium.INSTANCE.cryptoScalarMult(shared, privateKey, publicKey);
        return shared;
    }

    public static byte[][] generateX25519KeyPair() {
        byte[] privateKey = Sodium.INSTANCE.randomBytesBuf(32);
        byte[] publicKey = new byte[32];
        Sodium.INSTANCE.cryptoScalarMultBase(publicKey, privateKey);
        return new byte[][]{publicKey, privateKey};
    }

    public static byte[][] generateEd25519KeyPair() {
        byte[] publicKey = new byte[Sign.ED25519_PUBLICKEYBYTES];
        byte[] privateKey = new byte[Sign.ED25519_SECRETKEYBYTES];
        Sodium.INSTANCE.cryptoSignKeypair(publicKey, privateKey);
        return new byte[][]{publicKey, privateKey};
    }

    public static byte[] hash(byte[] input) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(input);
    }

    public static byte[] hash(String input) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(input.getBytes());
    }

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
