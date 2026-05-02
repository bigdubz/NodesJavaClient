package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.types.Session;

public class X3DHResult {

    private final Session session;
    private final PreKeyMessage preKeyMessage;
    private final boolean usedOneTimePreKey;

    public X3DHResult(Session session, PreKeyMessage preKeyMessage, boolean usedOneTimePreKey) {
        this.session = session;
        this.preKeyMessage = preKeyMessage;
        this.usedOneTimePreKey = usedOneTimePreKey;
    }

    public Session getSession() {
        return session;
    }

    public PreKeyMessage getPreKeyMessage() {
        return preKeyMessage;
    }

    public boolean isUsedOneTimePreKey() {
        return usedOneTimePreKey;
    }
}
