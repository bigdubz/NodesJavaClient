package com.nodes.chatclient.store.model;

public final class Presence {

    public final String userId;

    public boolean online = false;
    public Long lastSeen = null;
    public boolean isTyping = false;

    public Presence(String userId) {
        this.userId = userId;
    }
}
