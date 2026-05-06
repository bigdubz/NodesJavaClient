package com.nodes.chatclient.http;

import com.nodes.chatclient.config.ClientConfig;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class HttpClientFactory {
    private final ClientConfig config;
    private final Executor executor;

    public HttpClientFactory(ClientConfig config) {
        this(config, Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors())
        ));
    }

    public HttpClientFactory(ClientConfig config, Executor executor) {
        this.config = Objects.requireNonNull(config, "config");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public HttpClient create() {
        return HttpClient.newBuilder()
                .connectTimeout(config.httpConnectTimeout())
                .executor(executor)
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public HttpClient createWithRequestTimeout(Duration requestTimeout) {
        return HttpClient.newBuilder()
                .connectTimeout(config.httpConnectTimeout())
                .executor(executor)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }
}
