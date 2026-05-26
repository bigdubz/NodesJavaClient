package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.crypto.KeyMaterial;
import com.nodes.chatclient.e2ee.crypto.KeyDerivation;
import com.nodes.chatclient.e2ee.protos.ProtoSession;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.types.Session;
import com.nodes.chatclient.http.dto.RemoteUserBundle;

public final class X3DHService {

    public X3DHResult initiateHandshake(LocalIdentity self, RemoteUserBundle remoteBundle) throws Exception {
        BundleVerifier.requireValid(remoteBundle);

        byte[] remoteOneTimePrekey = null;
        boolean usedOneTimePrekey = false;

        if (remoteBundle.opk() != null) {
            remoteOneTimePrekey = remoteBundle.opk().publicKey();
            usedOneTimePrekey = true;
        }

        byte[][] eph = KeyMaterial.generateX25519KeyPair();
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
        byte[] dh1 = KeyMaterial.dh(self.identityPrivateKey(), remoteBundle.spk());
        byte[] dh2 = KeyMaterial.dh(ephPrivateKey, remoteBundle.ik());
        byte[] dh3 = KeyMaterial.dh(ephPrivateKey, remoteBundle.spk());

        if (usedOneTimePrekey) {
            byte[] dh4 = KeyMaterial.dh(ephPrivateKey, remoteOneTimePrekey);
            secret = KeyMaterial.concat(KeyMaterial.concat(dh1, dh2), KeyMaterial.concat(dh3, dh4));

        } else {
            secret = KeyMaterial.concat(KeyMaterial.concat(dh1, dh2), dh3);
        }

        byte[] rootKey = KeyMaterial.hash(secret);

        Session session = createInitialSession(
                self,
                remoteBundle,
                rootKey,
                ephPrivateKey,
                ephPublicKey,
                remoteBundle.spk(),
                usedOneTimePrekey ? remoteBundle.opk().keyId() : null
        );

        PrekeyMessage prekeyMessage = new PrekeyMessage(
                self.userId(),
                self.deviceId(),
                self.identityPublicKey(),
                ephPublicKey,
                0,
                usedOneTimePrekey ? remoteBundle.opk().keyId() : null
        );

        return new X3DHResult(session, prekeyMessage, usedOneTimePrekey);
    }

    private Session createInitialSession(LocalIdentity self,
                                         RemoteUserBundle remoteBundle,
                                         byte[] rootKey,
                                         byte[] localDhPrivateKey,
                                         byte[] localDhPublicKey,
                                         byte[] remoteDhPublicKey,
                                         Integer oneTimePrekeyId) throws Exception {
        byte[][] initialSendingRatchet = KeyDerivation.kdfRoot(
                rootKey,
                KeyMaterial.dh(localDhPrivateKey, remoteDhPublicKey)
        );

        Session session = Session.createInitial(
                initialSendingRatchet[0],
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

        session.remoteSigningPublicKey = remoteBundle.sk();

        session.sendingChainKey = initialSendingRatchet[1];
        session.receivingChainKey = null;

        session.previousRemoteDHPublicKey = null;
        session.previousChainLength = 0;
        session.oneTimePrekeyId = oneTimePrekeyId;

        session.sendingMessageNumber = 0;
        session.receivingMessageNumber = 0;

        return session;
    }
}
