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
        byte[][] opks
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

        List<int[]> opksArray = new ArrayList<>();
        for (byte[] opk : opks) {
            opksArray.add(bytesToJsonArray(opk));
        }

        body.put("opks", opksArray);

        return body;
    }
}
