package com.nodes.chatclient.e2ee.crypto;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;

final class Sodium {
    static final LazySodiumJava INSTANCE = new LazySodiumJava(new SodiumJava());

    private Sodium() {
    }
}
