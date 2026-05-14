package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.crypto.MessageAuth;
import com.nodes.chatclient.http.dto.RemoteUserBundle;

public final class BundleVerifier {


    private BundleVerifier() { }

    public static boolean verifySignedPrekey(RemoteUserBundle bundle) {
        if (bundle == null) {
            return false;
        }

        try {
            return MessageAuth.verify(
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
