package com.nodes.chatclient.e2ee.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record LocalUserBundle(
        String userId,
        String deviceId,
        int registrationId,
        byte[] sk,
        byte[] ik,
        byte[] spk,
        byte[] spkSignature,
        BundleOneTimePrekey[] opks
) {
    private int[] bytesToJsonArray(byte[] bytes) {
        int[] out = new int[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            out[i] = bytes[i] & 0xff;
        }

        return out;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> body = new HashMap<>();
        body.put("registrationId", registrationId);
        body.put("sk", bytesToJsonArray(sk));
        body.put("ik", bytesToJsonArray(ik));
        body.put("spk", bytesToJsonArray(spk));
        body.put("spkSignature", bytesToJsonArray(spkSignature));

        List<Map<String, Object>> opksArray = new ArrayList<>();
        for (BundleOneTimePrekey opk : opks) {
            Map<String, Object> map = new HashMap<>();
            map.put("keyId", opk.keyId());
            map.put("publicKey", bytesToJsonArray(opk.publicKey()));
            opksArray.add(map);
        }

        body.put("opks", opksArray);

        return body;
    }
}
