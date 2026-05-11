package com.nodes.chatclient.http.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.http.dto.ContactDownloadResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ContactsApi {

    private final ClientConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ContactsApi(ClientConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.mapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public CompletableFuture<ContactDownloadResponse> downloadContactAsync(
            String jwt,
            String userId
    ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(downloadContactUri(userId))
                    .timeout(config.httpRequestTimeout())
                    .header("Authorization", "Bearer " + jwt)
                    .GET()
                    .build();

            return httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::handleDownloadContactResponse);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private ContactDownloadResponse handleDownloadContactResponse(HttpResponse<String> response) {
        int status = response.statusCode();

        if (status != 200) {
            throw new RuntimeException("Failed to download contact (HTTP " + status + "): " + response.body());
        }

        try {
            return mapper.readValue(
                    response.body(),
                    ContactDownloadResponse.class
            );
        } catch (Exception e) {
            System.out.println(response.body());
            throw new RuntimeException("Invalid contact download response", e);
        }
    }

    private URI downloadContactUri(String userId) {
        return config.httpBaseUri().resolve("/e2ee/contact" + "?userId=" + url(userId));
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
