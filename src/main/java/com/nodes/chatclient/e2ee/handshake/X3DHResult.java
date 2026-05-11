package com.nodes.chatclient.e2ee.handshake;

import com.nodes.chatclient.e2ee.types.Session;

public record X3DHResult(Session session, PrekeyMessage prekeyMessage, boolean usedOneTimePrekey) {

}
