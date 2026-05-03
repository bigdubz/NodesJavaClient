package com.nodes.chatclient.http.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.http.dto.BundleStatusResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class BundlesApi {

    private final ClientConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public BundlesApi(ClientConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.mapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public CompletableFuture<BundleStatusResponse> getBundleStatusAsync(
            String jwt,
            String userId,
            String deviceId
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(bundleStateUri(userId, deviceId))
                .timeout(config.httpRequestTimeout())
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleBundleStatusResponse);
    }

    public BundleStatusResponse handleBundleStatusResponse(
            HttpResponse<String> response
    ) {
        int status = response.statusCode();
        if (status != 200) {
            throw new RuntimeException("Failed to fetch bundle status (HTTP " + status + "): " + response.body());
        }

        try {
            return mapper.readValue(
                    response.body(),
                    BundleStatusResponse.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid bundle status response", e);
        }
    }

    private URI bundleStateUri(String userId, String deviceId) {
        String qs = "?userId=" + url(userId) +
                    "&deviceId=" + url(deviceId);
        return config.httpBaseUri().resolve("/e2ee/bundle-status" + qs);
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
