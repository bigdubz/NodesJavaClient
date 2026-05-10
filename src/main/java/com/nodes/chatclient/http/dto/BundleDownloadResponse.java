package com.nodes.chatclient.http.dto;

import com.nodes.chatclient.e2ee.types.RemoteUserBundle;

public class BundleDownloadResponse {
    public String type;
    public RemoteUserBundle[] payload;
}
