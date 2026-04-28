package com.nodes.chatclient.e2ee.mappers;

import com.google.protobuf.ByteString;
import com.nodes.chatclient.e2ee.protos.ProtoSession;
import com.nodes.chatclient.e2ee.types.Session;

import java.util.Map;

public class SessionMapper {

    public static ProtoSession.SessionProto serialize(Session session) {

        ProtoSession.SessionProto.Builder builder =
                ProtoSession.SessionProto.newBuilder()
                        .setSessionVersion(session.sessionVersion)
                        .setLocalDeviceId(session.localDeviceId)
                        .setRemoteDeviceId(session.remoteDeviceId)
                        .setInitiator(session.initiator)
                        .setState(session.state != null
                                ? session.state
                                : ProtoSession.SessionProto.State.ACTIVE)
                        .setRootKey(ByteString.copyFrom(session.rootKey))
                        .setDhPrivateKey(ByteString.copyFrom(session.dhPrivateKey))
                        .setDhPublicKey(ByteString.copyFrom(session.dhPublicKey))
                        .setPreviousSendingChainLength(session.previousChainLength)
                        .setSigningPrivateKey(ByteString.copyFrom(session.signingPrivateKey))
                        .setSigningPublicKey(ByteString.copyFrom(session.signingPublicKey));

        if (session.remoteSigningPublicKey != null) {
            builder.setRemoteSigningPublicKey(
                    ByteString.copyFrom(session.remoteSigningPublicKey)
            );
        }

        if (session.remoteDHPublicKey != null) {
            builder.setRemoteDhPublicKey(
                    ByteString.copyFrom(session.remoteDHPublicKey)
            );
        }

        if (session.previousRemoteDHPublicKey != null) {
            builder.setPreviousRemoteDhPublicKey(
                    ByteString.copyFrom(session.previousRemoteDHPublicKey)
            );
        }

        if (session.sendingChainKey != null) {
            builder.setSendingChain(
                    ProtoSession.SessionProto.ChainState.newBuilder()
                            .setChainKey(ByteString.copyFrom(session.sendingChainKey))
                            .setMessageNumber(session.sendingMessageNumber)
                            .build()
            );
        }

        if (session.receivingChainKey != null) {
            builder.setReceivingChain(
                    ProtoSession.SessionProto.ChainState.newBuilder()
                            .setChainKey(ByteString.copyFrom(session.receivingChainKey))
                            .setMessageNumber(session.receivingMessageNumber)
                            .build()
            );
        }

        for (Map.Entry<String, byte[]> entry : session.skippedKeys.entrySet()) {
            builder.addSkippedKeys(
                    ProtoSession.SessionProto.SkippedKey.newBuilder()
                            .setRatchetKeyId(entry.getKey())
                            .setKey(ByteString.copyFrom(entry.getValue()))
                            .build()
            );
        }

        return builder.build();
    }

    public static Session deserialize(ProtoSession.SessionProto proto) {
        return Session.fromProto(proto);
    }
}