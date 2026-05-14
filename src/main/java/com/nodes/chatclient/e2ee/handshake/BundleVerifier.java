package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.http.dto.RemoteUserBundle;
import com.nodes.chatclient.e2ee.utils.CryptoUtils;

public class BundleVerifier {


    private BundleVerifier() { }

    public static boolean verifySignedPrekey(RemoteUserBundle bundle) {
        if (bundle == null) {
            return false;
        }

        try {
            return CryptoUtils.verify(
                    bundle.spk(),
                    bundle.spkSignature(),
                    bundle.sk()
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
