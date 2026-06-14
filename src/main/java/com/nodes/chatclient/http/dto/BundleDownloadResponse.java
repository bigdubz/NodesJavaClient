package com.nodes.chatclient.http.dto;

public record BundleDownloadResponse(String type, RemoteUserBundle[] payload) {
}
