package com.nodes.chatclient.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.http.dto.LoginRequest;
import com.nodes.chatclient.http.dto.LoginResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class AuthApi {
    private final ClientConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public AuthApi(
            ClientConfig config,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.mapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public CompletableFuture<LoginResponse> loginAsync(
            String userId,
            String password
    ) {
        try {
            LoginRequest body = new LoginRequest(userId, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(loginUri())
                    .timeout(config.httpRequestTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(body)
                    ))
                    .build();

            return httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::handleLoginResponse);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public LoginResponse login(
            String userId,
            String password
    ) throws Exception {
        LoginRequest body = new LoginRequest(userId, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(loginUri())
                .timeout(config.httpRequestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(body)
                ))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return handleLoginResponse(response);
    }

    private URI loginUri() {
        return config.httpBaseUri().resolve("/login");
    }

    private LoginResponse handleLoginResponse(HttpResponse<String> response) {
        int status = response.statusCode();

        if (status != 200) {
            throw new RuntimeException("Login failed (HTTP " + status + "): " + response.body());
        }

        try {
            return mapper.readValue(response.body(), LoginResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid login response", e);
        }
    }
}
