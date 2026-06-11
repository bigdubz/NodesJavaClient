package com.nodes.chatclient.ws;

import com.nodes.chatclient.ws.messages.*;

public interface ServerMessageHandlers {
    void onEncryptedRelay(ServerEncryptedRelay.Payload payload);

    void onUserOnline(ServerUserOnline.Payload payload);
    void onUserOffline(ServerUserOffline.Payload payload);
}
