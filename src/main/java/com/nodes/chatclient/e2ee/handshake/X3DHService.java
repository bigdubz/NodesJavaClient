package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.crypto.KeyMaterial;
import com.nodes.chatclient.e2ee.crypto.KeyDerivation;
import com.nodes.chatclient.e2ee.protos.ProtoSession;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.types.Session;
import com.nodes.chatclient.http.dto.RemoteUserBundle;

import java.util.Arrays;

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

        byte[] prefix = new byte[] { (byte) 0xFF };

        if (usedOneTimePrekey) {
            byte[] dh4 = KeyMaterial.dh(ephPrivateKey, remoteOneTimePrekey);
            secret = KeyMaterial.concat(prefix,
                    KeyMaterial.concat(KeyMaterial.concat(dh1, dh2), KeyMaterial.concat(dh3, dh4)));
        } else {
            secret = KeyMaterial.concat(prefix, KeyMaterial.concat(KeyMaterial.concat(dh1, dh2), dh3));
        }

        byte[] extracted = KeyDerivation.hkdfExtract(new byte[32], secret);
        byte[] initialRootKey = KeyDerivation.hkdfExpand(extracted, "initial-root", 32);

        byte[][] initialRatchetKeyPair = KeyMaterial.generateX25519KeyPair();
        byte[] initialRatchetPublicKey = initialRatchetKeyPair[0];
        byte[] initialRatchetPrivateKey = initialRatchetKeyPair[1];

        byte[] initialDh = KeyMaterial.dh(initialRatchetPrivateKey, remoteBundle.spk());
        byte[][] initialRatchet = KeyDerivation.kdfRoot(initialRootKey, initialDh);

        Integer oneTimePrekeyId = usedOneTimePrekey ? remoteBundle.opk().keyId() : null;

        Session session = Session.createInitial(
                initialRatchet[0],
                initialRatchetPrivateKey,
                initialRatchetPublicKey,
                remoteBundle.spk(),
                self.deviceId(),
                remoteBundle.deviceId(),
                true
        );

        session.state = ProtoSession.SessionProto.State.ACTIVE;
        session.sessionVersion = 1;
        session.signingPrivateKey = self.signingPrivateKey();
        session.signingPublicKey = self.signingPublicKey();
        session.remoteSigningPublicKey = remoteBundle.sk();

        session.sendingChainKey = initialRatchet[1];
        session.receivingChainKey = null;
        session.previousRemoteDHPublicKey = null;
        session.previousChainLength = 0;
        session.oneTimePrekeyId = oneTimePrekeyId;

        PrekeyMessage prekeyMessage = new PrekeyMessage(
                self.userId(),
                self.deviceId(),
                self.identityPublicKey(),
                ephPublicKey,
                0,
                oneTimePrekeyId
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

        byte[] expanded = KeyDerivation.hkdfExpand(rootKey, "session-init", 64);
        byte[] messageChainRoot = Arrays.copyOfRange(expanded, 0, 32);
        byte[] controlChainRoot = Arrays.copyOfRange(expanded, 32, 64);

//        byte[][] initialSendingRatchet = KeyDerivation.kdfRoot(
//                rootKey,
//                KeyMaterial.dh(localDhPrivateKey, remoteDhPublicKey)
//        );

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

        session.remoteSigningPublicKey = remoteBundle.sk();

        session.sendingChainKey = messageChainRoot;
        session.receivingChainKey = null;

        session.previousRemoteDHPublicKey = null;
        session.previousChainLength = 0;
        session.oneTimePrekeyId = oneTimePrekeyId;

        session.sendingMessageNumber = 0;
        session.receivingMessageNumber = 0;

        return session;
    }
}
