package com.nodes.chatclient.e2ee.types;

import com.nodes.chatclient.http.dto.OpkDto;

public record RemoteUserBundle(
        String userId,
        String deviceId,
        int registrationId,
        byte[] sk,
        byte[] ik,
        byte[] spk,
        byte[] spkSignature,
        OpkDto opk
) { }
