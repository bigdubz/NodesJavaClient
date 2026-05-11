package com.nodes.chatclient.http.dto;

public record ContactDownloadResponse(
        String type,
        Contact[] payload
) {}