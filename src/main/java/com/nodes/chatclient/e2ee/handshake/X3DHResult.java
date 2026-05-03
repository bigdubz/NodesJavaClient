package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.types.Session;

public class X3DHResult {

    private final Session session;
    private final PrekeyMessage prekeyMessage;
    private final boolean usedOneTimePrekey;

    public X3DHResult(Session session, PrekeyMessage prekeyMessage, boolean usedOneTimePrekey) {
        this.session = session;
        this.prekeyMessage = prekeyMessage;
        this.usedOneTimePrekey = usedOneTimePrekey;
    }

    public Session getSession() {
        return session;
    }

    public PrekeyMessage getPrekeyMessage() {
        return prekeyMessage;
    }

    public boolean isUsedOneTimePrekey() {
        return usedOneTimePrekey;
    }
}
