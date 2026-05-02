package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.protos.ProtoSession;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.types.Session;
import com.nodes.chatclient.e2ee.types.UserKeyBundle;
import com.nodes.chatclient.e2ee.utils.CryptoUtils;

public class X3DHService {

    public X3DHResult initiateHandshake(LocalIdentity self, UserKeyBundle remoteBundle) throws Exception {
        BundleVerifier.requireValid(remoteBundle);

        byte[] remoteOneTimePreKey = null;
        boolean usedOneTimePreKey = false;

        if (remoteBundle.getOneTimePrekeys() != null &&
            !remoteBundle.getOneTimePrekeys().isEmpty()) {
            remoteOneTimePreKey = remoteBundle.getOneTimePrekeys().getFirst();
            usedOneTimePreKey = true;
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

        byte[] dh1 = CryptoUtils.dh(self.getIdentityPrivateKey(), remoteBundle.getSignedPrekey());

        byte[] dh2 = CryptoUtils.dh(ephPrivateKey, remoteBundle.identityPublicKey);

        byte[] dh3 = CryptoUtils.dh(ephPrivateKey, remoteBundle.getSignedPrekey());

        byte[] secret;

        if (usedOneTimePreKey) {
            byte[] dh4 = CryptoUtils.dh(ephPrivateKey, remoteOneTimePreKey);

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
                remoteBundle.getSignedPrekey()
        );

        PreKeyMessage preKeyMessage = new PreKeyMessage(
                self.getUserId(),
                self.getDeviceId(),
                self.getIdentityPublicKey(),
                ephPublicKey,
                0,
                usedOneTimePreKey ? 0 : null
        );

        return  new X3DHResult(session, preKeyMessage, usedOneTimePreKey);
    }

    private Session createInitialSession(LocalIdentity self,
                                         UserKeyBundle remoteBundle,
                                         byte[] rootKey,
                                         byte[] localDhPrivateKey,
                                         byte[] localDhPublicKey,
                                         byte[] remoteDhPublicKey) {
        Session session = Session.createInitial(
                rootKey,
                localDhPrivateKey,
                localDhPublicKey,
                remoteDhPublicKey,
                self.getDeviceId(),
                remoteBundle.getDeviceId(),
                true // initiator is always true here
        );

        session.state = ProtoSession.SessionProto.State.ACTIVE;
        session.sessionVersion = 1;

        session.signingPrivateKey = self.getSigningPrivateKey();
        session.signingPublicKey = self.getSigningPublicKey();

        session.remoteSigningPublicKey = remoteBundle.getIdentityPublicKey();

        session.sendingChainKey = null;
        session.receivingChainKey = null;

        session.previousRemoteDHPublicKey = null;
        session.previousChainLength = 0;

        session.sendingMessageNumber = 0;
        session.receivingMessageNumber = 0;

        return session;
    }
}
