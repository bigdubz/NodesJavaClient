package com.nodes.chatclient.ws;

import com.nodes.chatclient.ws.messages.ServerAuthError;
import com.nodes.chatclient.ws.messages.ServerAuthOk;

public interface ServerAuthHandlers {
    void onAuthOk(ServerAuthOk.Payload payload);
    void onAuthError(ServerAuthError.Payload payload);
}
