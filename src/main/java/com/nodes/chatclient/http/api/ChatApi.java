package com.nodes.chatclient.http.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.http.dto.ConversationRowDto;
import com.nodes.chatclient.http.dto.MessageRowDto;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class ChatApi {
    private final ClientConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public ChatApi(
            ClientConfig config,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.mapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public CompletableFuture<List<ConversationRowDto>> getConversationsAsync(
            String jwt
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(conversationsUri())
                .timeout(config.httpRequestTimeout())
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleConversationsResponse);
    }

    public List<ConversationRowDto> getConversations(String jwt) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(conversationsUri())
                .timeout(config.httpRequestTimeout())
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return handleConversationsResponse(response);
    }

    public CompletableFuture<List<MessageRowDto>> getHistoryAsync(
            String jwt,
            String peerId,
            Long before,
            int limit
    ) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(historyUri(peerId, before, limit))
                .timeout(config.httpRequestTimeout())
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleHistoryResponse);
    }

    public List<MessageRowDto> getHistory(
            String jwt,
            String peerId,
            Long before,
            int limit
    ) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(historyUri(peerId, before, limit))
                .timeout(config.httpRequestTimeout())
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleHistoryResponse(response);
    }

    private URI conversationsUri() {
        return config.httpBaseUri().resolve("/conversations");
    }

    private URI historyUri(String peerId, Long before, int limit) {
        StringBuilder qs = new StringBuilder();
        qs.append("?user=").append(url(peerId));
        qs.append("&limit=").append(limit);

        if (before != null) {
            qs.append("&before=").append(before);
        }

        return config.httpBaseUri().resolve("/history" + qs);
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private List<ConversationRowDto> handleConversationsResponse(
            HttpResponse<String> response
    ) {
        int status = response.statusCode();
        if (status != 200) {
            throw new RuntimeException("Failed to fetch conversations (HTTP " + status + "): " + response.body());
        }

        try {
            return mapper.readValue(
                    response.body(),
                    new TypeReference<>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid conversations response", e);
        }
    }

    private List<MessageRowDto> handleHistoryResponse(
            HttpResponse<String> response
    ) {
        int status = response.statusCode();
        if (status != 200) {
            throw new RuntimeException("Failed to fetch history (HTTP " + status + "): " + response.body());
        }

        try {
            return mapper.readValue(
                    response.body(),
                    new TypeReference<>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid history response", e);
        }
    }
}
