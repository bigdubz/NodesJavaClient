package com.nodes.chatclient.e2ee.types;

import com.nodes.chatclient.e2ee.protos.ProtoSession;
import com.nodes.chatclient.e2ee.utils.SkippedKeyStore;

public final class Session {
    public ProtoSession.SessionProto.State state;
    public int sessionVersion;
    public byte[] rootKey;
    public byte[] sendingChainKey;
    public byte[] receivingChainKey;

    public byte[] signingPrivateKey;
    public byte[] signingPublicKey;
    public byte[] remoteSigningPublicKey;

    public long sendingMessageNumber = 0;
    public long receivingMessageNumber = 0;
    public String remoteDeviceId;
    public String localDeviceId;

    public SkippedKeyStore skippedKeys = new SkippedKeyStore(1000);

    public byte[] dhPrivateKey;
    public byte[] dhPublicKey;
    public byte[] remoteDHPublicKey;
    public byte[] previousRemoteDHPublicKey;
    public long previousChainLength;

    public boolean initiator;

    public Session() { }

    public static Session createInitial(
            byte[] rootKey,
            byte[] dhPrivateKey,
            byte[] dhPublicKey,
            byte[] remoteDHPublicKey,
            String localDeviceId,
            String remoteDeviceId,
            boolean initiator
    ) {
        Session s = new Session();

        s.rootKey = rootKey;
        s.dhPrivateKey = dhPrivateKey;
        s.dhPublicKey = dhPublicKey;
        s.remoteDHPublicKey = remoteDHPublicKey;
        s.localDeviceId = localDeviceId;
        s.remoteDeviceId = remoteDeviceId;
        s.initiator = initiator;

        s.sendingMessageNumber = 0;
        s.receivingMessageNumber = 0;
        s.previousChainLength = 0;

        return s;
    }

    public Session(byte[] rootKey, byte[] dhPrivateKey, byte[] dhPublicKey,
                   byte[] remotePub, String localDeviceId, boolean initiator) {
        this.rootKey = rootKey;
        this.dhPrivateKey = dhPrivateKey;
        this.dhPublicKey = dhPublicKey;
        this.remoteDHPublicKey = remotePub;
        this.localDeviceId = localDeviceId;
        this.initiator = initiator;
    }

    static public Session fromProto(ProtoSession.SessionProto proto) {
        Session session = new Session(
                proto.getRootKey().toByteArray(),
                proto.getDhPrivateKey().toByteArray(),
                proto.getDhPublicKey().toByteArray(),
                proto.hasRemoteDhPublicKey() ? proto.getRemoteDhPublicKey().toByteArray() : null,
                proto.getLocalDeviceId(),
                proto.getInitiator()
        );

        session.state = proto.getState();
        session.sessionVersion = proto.getSessionVersion();
        session.remoteDeviceId = proto.getRemoteDeviceId();

        session.signingPrivateKey = proto.getSigningPrivateKey().toByteArray();
        session.signingPublicKey = proto.getSigningPublicKey().toByteArray();

        if (proto.hasRemoteSigningPublicKey()) {
            session.remoteSigningPublicKey = proto.getRemoteSigningPublicKey().toByteArray();
        }

        if (proto.hasSendingChain()) {
            session.sendingChainKey = proto.getSendingChain().getChainKey().toByteArray();
            session.sendingMessageNumber = proto.getSendingChain().getMessageNumber();
        }

        if (proto.hasReceivingChain()) {
            session.receivingChainKey = proto.getReceivingChain().getChainKey().toByteArray();
            session.receivingMessageNumber = proto.getReceivingChain().getMessageNumber();
        }

        if (proto.hasPreviousRemoteDhPublicKey()) {
            session.previousRemoteDHPublicKey =
                    proto.getPreviousRemoteDhPublicKey().toByteArray();
        }

        session.previousChainLength = proto.getPreviousSendingChainLength();

        // Skipped keys
        for (ProtoSession.SessionProto.SkippedKey k : proto.getSkippedKeysList()) {
            session.skippedKeys.put(
                    k.getRatchetKeyId(),
                    k.getKey().toByteArray()
            );
        }

        return session;
    }

    public void clearSkippedKeys() {
        skippedKeys.clear();
    }
}
