package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.types.UserKeyBundle;
import com.nodes.chatclient.e2ee.utils.CryptoUtils;

import java.util.Base64;

public class BundleVerifier {


    private BundleVerifier() {
    }

    public static boolean verifySignedPreKey(UserKeyBundle bundle) {
        if (bundle == null) {
            return false;
        }

        try {
            byte[] signingPublicKey = Base64.getDecoder().decode(bundle.getSigningPublicKey());
            byte[] signedPreKey = Base64.getDecoder().decode(bundle.getSignedPrekey());
            byte[] signature = Base64.getDecoder().decode(bundle.getSignedPrekeySignature());

            return CryptoUtils.verify(
                    signedPreKey,
                    signature,
                    signingPublicKey
            );

        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static void requireValid(UserKeyBundle bundle) {
        if (!verifySignedPreKey(bundle)) {
            throw new IllegalArgumentException("Invalid signed pre-key bundle");
        }
    }
}
