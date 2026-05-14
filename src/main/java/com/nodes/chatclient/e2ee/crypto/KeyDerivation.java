package com.nodes.chatclient.e2ee.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.HexFormat;

public final class KeyDerivation {
    private static final String HMAC_ALGO = "HmacSHA256";

    private KeyDerivation() {
    }

    public static byte[] hkdfExtract(byte[] salt, byte[] inputKeyMaterial) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        if (salt == null || salt.length == 0) {
            salt = new byte[32];
        }
        mac.init(new SecretKeySpec(salt, HMAC_ALGO));
        return mac.doFinal(inputKeyMaterial);
    }

    public static byte[] hkdfExpand(byte[] pseudorandomKey, String info, int length) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(pseudorandomKey, HMAC_ALGO));

        byte[] outputKeyMaterial = new byte[length];
        byte[] t = new byte[0];
        int generatedBytes = 0;
        byte counter = 1;

        while (generatedBytes < length) {
            mac.update(t);
            mac.update(info.getBytes());
            mac.update(counter);
            t = mac.doFinal();

            int toCopy = Math.min(length - generatedBytes, t.length);
            System.arraycopy(t, 0, outputKeyMaterial, generatedBytes, toCopy);
            generatedBytes += toCopy;
            counter++;
        }

        return outputKeyMaterial;
    }

    public static byte[][] kdfRoot(byte[] rootKey, byte[] dhOutput) throws Exception {
        byte[] prk = hkdfExtract(rootKey, dhOutput);
        byte[] okm = hkdfExpand(prk, "RootRatchet", 64);

        byte[] newRoot = Arrays.copyOfRange(okm, 0, 32);
        byte[] chain = Arrays.copyOfRange(okm, 32, 64);

        return new byte[][]{newRoot, chain};
    }

    public static byte[] deriveMessageKey(byte[] chainKey) throws Exception {
        return hkdfExpand(chainKey, "MessageKey", 32);
    }

    public static byte[] advanceChainKey(byte[] chainKey) throws Exception {
        return hkdfExpand(chainKey, "ChainStep", 32);
    }

    public static String ratchetId(byte[] dhPublicKey, long messageNumber) throws Exception {
        byte[] hash = KeyMaterial.hash(dhPublicKey);
        return HexFormat.of().formatHex(hash) + ":" + messageNumber;
    }
}
