package com.nodes.chatclient.store.events;

public interface StoreListener {

    default void onConversationsUpdated() {}
    default void onMessageListUpdated(String peerId) {}
}
