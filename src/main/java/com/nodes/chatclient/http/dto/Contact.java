package com.nodes.chatclient.http.dto;

public record Contact(String userId, String deviceId, byte[] ik, byte[] sk) {
}
