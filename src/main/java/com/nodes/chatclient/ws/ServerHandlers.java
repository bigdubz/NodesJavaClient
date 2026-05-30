package com.nodes.chatclient.ws;

import com.nodes.chatclient.ws.messages.*;

public interface ServerHandlers {
    void onAuthOk(ServerAuthOk.Payload payload);
    void onAuthError(ServerAuthError.Payload payload);

    void onChatMessage(ServerChatMessage.Payload payload);
    void onEncryptedRelay(ServerEncryptedRelay.Payload payload);

    void onMessageDelivered(ServerMessageDelivered.Payload payload);
    void onMessageSeen(ServerMessageSeen.Payload payload);
    void onUserTyping(ServerUserTyping.Payload payload);

    void onUserOnline(ServerUserOnline.Payload payload);
    void onUserOffline(ServerUserOffline.Payload payload);

    void onAddReaction(ServerAddReaction.Payload payload);
    void onRemoveReaction(ServerRemoveReaction.Payload payload);
}
