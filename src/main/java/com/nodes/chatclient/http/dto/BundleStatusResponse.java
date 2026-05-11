package com.nodes.chatclient.http.dto;

public record BundleStatusResponse(String userId, String deviceId, boolean bundleMissing, int oneTimePrekeyCount,
                                   int oneTimePrekeyTarget, int maxOneTimePrekeysPerUpload, boolean signedPrekeyStale,
                                   Integer signedPrekeyId, long signedPrekeyCreatedAt, long lastBundleUploadAt,
                                   long serverTime) {
}
