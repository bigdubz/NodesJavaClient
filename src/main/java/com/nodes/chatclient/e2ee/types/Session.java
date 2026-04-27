package com.nodes.chatclient.e2ee.types;

import com.nodes.chatclient.e2ee.protos.ProtoSession;
import com.nodes.chatclient.e2ee.utils.SkippedKeyStore;

public class Session {
    public ProtoSession.SessionProto.State state;
    public int sessionVersion;
    public byte[] rootKey;
    public byte[] sendingChainKey;
    public byte[] receivingChainKey;

    public long sendingMessageNumber = 1;
    public long receivingMessageNumber = 1;
    public String remoteDeviceId;
    public String localDeviceId;

    public SkippedKeyStore skippedKeys = new SkippedKeyStore(1000);

    public byte[] dhPrivateKey;
    public byte[] dhPublicKey;
    public byte[] remoteDHPublicKey;
    public byte[] previousRemoteDHPublicKey;
    public long previousChainLength;

    public boolean initiator;
    public boolean canRatchet = false;
    public byte[] signingPrivateKey;
    public byte[] remoteSigningPublicKey;

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
        session.sendingChainKey = proto.getSendingChain().getChainKey().toByteArray();
        session.receivingChainKey = proto.getReceivingChain().getChainKey().toByteArray();

        session.sendingMessageNumber = proto.getSendingChain().getMessageNumber();
        session.receivingMessageNumber = proto.getReceivingChain().getMessageNumber();
        session.remoteDeviceId = proto.getRemoteDeviceId();
        if (proto.hasPreviousRemoteDhPublicKey()) {
            session.previousRemoteDHPublicKey = proto.getPreviousRemoteDhPublicKey().toByteArray();
        }
        session.previousChainLength = proto.getPreviousChainLength();

        for (ProtoSession.SessionProto.SkippedKey k : proto.getSkippedKeysList()) {
            session.skippedKeys.put(
                    k.getRatchetKeyId(),
                    k.getKey().toByteArray()
            );
        }

        return session;
    }
}
