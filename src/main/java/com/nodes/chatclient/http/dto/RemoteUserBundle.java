package com.nodes.chatclient.http.dto;

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
