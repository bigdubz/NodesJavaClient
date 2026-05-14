package com.nodes.chatclient.e2ee.crypto;

import com.goterl.lazysodium.interfaces.AEAD;

public final class AeadCipher {

    private AeadCipher() {
    }

    public static byte[] encrypt(byte[] key, byte[] nonce, byte[] message, byte[] associatedData) {
        byte[] ciphertext = new byte[message.length + AEAD.XCHACHA20POLY1305_IETF_ABYTES];

        Sodium.INSTANCE.cryptoAeadXChaCha20Poly1305IetfEncrypt(
                ciphertext,
                null,
                message,
                message.length,
                associatedData,
                associatedData.length,
                null,
                nonce,
                key
        );

        return ciphertext;
    }

    public static byte[] decrypt(byte[] key, byte[] nonce, byte[] ciphertext, byte[] associatedData) throws Exception {
        byte[] message = new byte[ciphertext.length - AEAD.XCHACHA20POLY1305_IETF_ABYTES];

        boolean success = Sodium.INSTANCE.cryptoAeadXChaCha20Poly1305IetfDecrypt(
                message,
                null,
                null,
                ciphertext,
                ciphertext.length,
                associatedData,
                associatedData.length,
                nonce,
                key
        );

        if (!success) {
            throw new Exception("Decryption failed: Integrity check failed.");
        }

        return message;
    }
}
