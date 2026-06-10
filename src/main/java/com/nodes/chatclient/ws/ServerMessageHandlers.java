package com.nodes.chatclient.ws;

import com.nodes.chatclient.ws.messages.*;

public interface ServerMessageHandlers {
    void onEncryptedRelay(ServerEncryptedRelay.Payload payload);

    void onMessageDelivered(ServerMessageDelivered.Payload payload);
    void onMessageSeen(ServerMessageSeen.Payload payload);
    void onUserTyping(ServerUserTyping.Payload payload);

    void onUserOnline(ServerUserOnline.Payload payload);
    void onUserOffline(ServerUserOffline.Payload payload);
}
