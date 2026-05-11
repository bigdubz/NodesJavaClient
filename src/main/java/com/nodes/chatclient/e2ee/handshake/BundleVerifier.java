package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.http.dto.RemoteUserBundle;
import com.nodes.chatclient.e2ee.utils.CryptoUtils;

import java.util.Base64;

public class BundleVerifier {


    private BundleVerifier() { }

    public static boolean verifySignedPrekey(RemoteUserBundle bundle) {
        if (bundle == null) {
            return false;
        }

        try {
            byte[] signingPublicKey = Base64.getDecoder().decode(bundle.sk());
            byte[] signedPrekey = Base64.getDecoder().decode(bundle.spk());
            byte[] signature = Base64.getDecoder().decode(bundle.spkSignature());

            return CryptoUtils.verify(
                    signedPrekey,
                    signature,
                    signingPublicKey
            );

        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static void requireValid(RemoteUserBundle bundle) {
        if (!verifySignedPrekey(bundle)) {
            throw new IllegalArgumentException("Invalid signed pre-key bundle");
        }
    }
}
