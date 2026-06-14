package com.nodes.chatclient.e2ee.utils;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}
