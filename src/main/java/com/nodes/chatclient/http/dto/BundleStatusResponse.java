package com.nodes.chatclient.http.dto;

public class BundleStatusResponse {
    public String userId;
    public String deviceId;
    public boolean bundleMissing;

    public int oneTimePrekeyCount;
    public int oneTimePrekeyTarget;
    public int maxOneTimePrekeysPerUpload;

    public boolean signedPrekeyStale;
    public Integer signedPrekeyId;
    public long signedPrekeyCreatedAt;

    public long lastBundleUploadAt;
    public long serverTime;
}
