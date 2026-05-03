package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.types.RemoteUserKeyBundle;
import com.nodes.chatclient.e2ee.utils.CryptoUtils;

import java.util.Base64;

public class BundleVerifier {


    private BundleVerifier() { }

    public static boolean verifySignedPreKey(RemoteUserKeyBundle bundle) {
        if (bundle == null) {
            return false;
        }

        try {
            byte[] signingPublicKey = Base64.getDecoder().decode(bundle.signingPublicKey());
            byte[] signedPreKey = Base64.getDecoder().decode(bundle.signedPrekey());
            byte[] signature = Base64.getDecoder().decode(bundle.signedPrekeySignature());

            return CryptoUtils.verify(
                    signedPreKey,
                    signature,
                    signingPublicKey
            );

        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static void requireValid(RemoteUserKeyBundle bundle) {
        if (!verifySignedPreKey(bundle)) {
            throw new IllegalArgumentException("Invalid signed pre-key bundle");
        }
    }
}
