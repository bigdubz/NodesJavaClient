package com.nodes.chatclient.http.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.e2ee.types.LocalUserBundle;
import com.nodes.chatclient.e2ee.types.RemoteUserBundle;
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
            String jwt
    ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(bundleStateUri())
                    .timeout(config.httpRequestTimeout())
                    .header("Authorization", "Bearer " + jwt)
                    .GET()
                    .build();

            return httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::handleBundleStatusResponse);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Void> uploadBundleAsync(
            String jwt,
            LocalUserBundle body
    ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uploadBundleUri())
                    .timeout(config.httpRequestTimeout())
                    .header("Authorization", "Bearer " + jwt)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(body.toMap())
                    ))
                    .build();

            return httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::handleUploadBundleResponse);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<RemoteUserBundle> downloadBundleAsync(
            String jwt,
            String userId
    ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(downloadBundleUri(userId))
                    .timeout(config.httpRequestTimeout())
                    .header("Authorization", "Bearer " + jwt)
                    .GET()
                    .build();

            return httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::handleDownloadBundleResponse);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private BundleStatusResponse handleBundleStatusResponse(
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

    private Void handleUploadBundleResponse(HttpResponse<String> response) {
        int status = response.statusCode();

        if (status != 200 && status != 201 && status != 204) {
            throw new RuntimeException(
                    "Failed to upload bundle (HTTP " + status + "): " + response.body()
            );
        }

        return null;
    }

    private RemoteUserBundle handleDownloadBundleResponse(HttpResponse<String> response) {
        int status = response.statusCode();

        if (status != 200) {
            throw new RuntimeException("Failed to download bundle (HTTP " + status + "): " + response.body());
        }

        try {
            return mapper.readValue(
                    response.body(),
                    RemoteUserBundle.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid bundle download response", e);
        }
    }

    private URI bundleStateUri() {
        return config.httpBaseUri().resolve("/e2ee/bundle/status");
    }

    private URI uploadBundleUri() {
        return config.httpBaseUri().resolve("/e2ee/bundle/upload");
    }

    private URI downloadBundleUri(String userId) {
        return config.httpBaseUri().resolve("/e2ee/bundle/download" + "?userId=" + url(userId));
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
