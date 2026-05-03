package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.protos.ProtoSession;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.types.Session;
import com.nodes.chatclient.e2ee.types.RemoteUserKeyBundle;
import com.nodes.chatclient.e2ee.utils.CryptoUtils;

public class X3DHService {

    public X3DHResult initiateHandshake(LocalIdentity self, RemoteUserKeyBundle remoteBundle) throws Exception {
        BundleVerifier.requireValid(remoteBundle);

        byte[] remoteOneTimePrekey = null;
        boolean usedOneTimePrekey = false;

        if (remoteBundle.oneTimePrekey() != null) {
            remoteOneTimePrekey = remoteBundle.oneTimePrekey();
            usedOneTimePrekey = true;
        }

        byte[][] eph = CryptoUtils.generateKeyPair();
        byte[] ephPublicKey = eph[0];
        byte[] ephPrivateKey = eph[1];

        /*
         * X3DH:
         *
         * DH1 = IK_A x SPK_B
         * DH2 = EK_A x IK_B
         * DH3 = EK_A x SPK_B
         * DH4 = EK_A x OPK_B (optional)
         */

        byte[] secret;
        byte[] dh1 = CryptoUtils.dh(self.identityPrivateKey(), remoteBundle.signedPrekey());
        byte[] dh2 = CryptoUtils.dh(ephPrivateKey, remoteBundle.identityPublicKey());
        byte[] dh3 = CryptoUtils.dh(ephPrivateKey, remoteBundle.signedPrekey());

        if (usedOneTimePrekey) {
            byte[] dh4 = CryptoUtils.dh(ephPrivateKey, remoteOneTimePrekey);
            secret = CryptoUtils.concat(CryptoUtils.concat(dh1, dh2), CryptoUtils.concat(dh3, dh4));

        } else {
            secret = CryptoUtils.concat(CryptoUtils.concat(dh1, dh2), dh3);
        }

        byte[] rootKey = CryptoUtils.hash(secret);

        Session session = createInitialSession(
                self,
                remoteBundle,
                rootKey,
                ephPrivateKey,
                ephPublicKey,
                remoteBundle.signedPrekey()
        );

        PrekeyMessage prekeyMessage = new PrekeyMessage(
                self.userId(),
                self.deviceId(),
                self.identityPublicKey(),
                ephPublicKey,
                0,
                usedOneTimePrekey ? 0 : null
        );

        return new X3DHResult(session, prekeyMessage, usedOneTimePrekey);
    }

    private Session createInitialSession(LocalIdentity self,
                                         RemoteUserKeyBundle remoteBundle,
                                         byte[] rootKey,
                                         byte[] localDhPrivateKey,
                                         byte[] localDhPublicKey,
                                         byte[] remoteDhPublicKey) {
        Session session = Session.createInitial(
                rootKey,
                localDhPrivateKey,
                localDhPublicKey,
                remoteDhPublicKey,
                self.deviceId(),
                remoteBundle.deviceId(),
                true // initiator is always true here
        );

        session.state = ProtoSession.SessionProto.State.ACTIVE;
        session.sessionVersion = 1;

        session.signingPrivateKey = self.signingPrivateKey();
        session.signingPublicKey = self.signingPublicKey();

        session.remoteSigningPublicKey = remoteBundle.identityPublicKey();

        session.sendingChainKey = null;
        session.receivingChainKey = null;

        session.previousRemoteDHPublicKey = null;
        session.previousChainLength = 0;

        session.sendingMessageNumber = 0;
        session.receivingMessageNumber = 0;

        return session;
    }
}
