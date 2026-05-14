package com.nodes.chatclient.e2ee.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SkippedKeyStore extends LinkedHashMap<String, byte[]> {

    private final int maxSize;

    public SkippedKeyStore(int maxSize) {
        super(16, 0.75f, false);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
        return size() > maxSize;
    }
}