package com.nodes.chatclient.config;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;


public final class ClientConfig {
    private final URI httpBaseUri;
    private final URI wsUri;

    private final Duration httpConnectTimeout;
    private final Duration httpRequestTimeout;

    private final Duration wsConnectTimeout;

    private final Duration heartbeatInterval;
    private final Duration heartbeatTimeout;

    private final Duration reconnectInitialDelay;
    private final Duration reconnectMaxDelay;

    private ClientConfig(Builder b) {
        this.httpBaseUri = require(b.httpBaseUri, "httpBaseUri");
        this.wsUri = require(b.wsUri, "wsUri");

        this.httpConnectTimeout = require(b.httpConnectTimeout, "httpConnectTimeout");
        this.httpRequestTimeout = require(b.httpRequestTimeout, "httpRequestTimeout");

        this.wsConnectTimeout = require(b.wsConnectTimeout, "wsConnectTimeout");

        this.heartbeatInterval = require(b.heartbeatInterval, "heartbeatInterval");
        this.heartbeatTimeout = require(b.heartbeatTimeout, "heartbeatTimeout");

        this.reconnectInitialDelay = require(b.reconnectInitialDelay, "reconnectInitialDelay");
        this.reconnectMaxDelay = require(b.reconnectMaxDelay, "reconnectMaxDelay");
    }

    private static <T> T require(T value, String name) {
        return Objects.requireNonNull(value, name + " must not be null");
    }

    public URI httpBaseUri() {
        return httpBaseUri;
    }

    public URI wsUri() {
        return wsUri;
    }

    public Duration httpConnectTimeout() {
        return httpConnectTimeout;
    }

    public Duration httpRequestTimeout() {
        return httpRequestTimeout;
    }

    public Duration wsConnectTimeout() {
        return wsConnectTimeout;
    }

    public Duration heartbeatInterval() {
        return heartbeatInterval;
    }

    public Duration heartbeatTimeout() {
        return heartbeatTimeout;
    }

    public Duration reconnectInitialDelay() {
        return reconnectInitialDelay;
    }

    public Duration reconnectMaxDelay() {
        return reconnectMaxDelay;
    }

    public static ClientConfig localDev() {
        return builder()
                .httpBaseUri(URI.create("https://api.nodesya.website"))
                .wsUri(URI.create("wss://api.nodesya.website"))
                .httpConnectTimeout(Duration.ofSeconds(5))
                .httpRequestTimeout(Duration.ofSeconds(10))
                .wsConnectTimeout(Duration.ofSeconds(10))
                .heartbeatInterval(Duration.ofSeconds(25))
                .heartbeatTimeout(Duration.ofSeconds(75))
                .reconnectInitialDelay(Duration.ofMillis(500))
                .reconnectMaxDelay(Duration.ofSeconds(20))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private URI httpBaseUri;
        private URI wsUri;

        private Duration httpConnectTimeout;
        private Duration httpRequestTimeout;
        private Duration wsConnectTimeout;

        private Duration heartbeatInterval;
        private Duration heartbeatTimeout;

        private Duration reconnectInitialDelay;
        private Duration reconnectMaxDelay;

        private Builder() {}

        public Builder httpBaseUri(URI uri) {
            this.httpBaseUri = uri;
            return this;
        }

        public Builder wsUri(URI uri) {
            this.wsUri = uri;
            return this;
        }

        public Builder httpConnectTimeout(Duration d) {
            this.httpConnectTimeout = d;
            return this;
        }

        public Builder httpRequestTimeout(Duration d) {
            this.httpRequestTimeout = d;
            return this;
        }

        public Builder wsConnectTimeout(Duration d) {
            this.wsConnectTimeout = d;
            return this;
        }

        public Builder heartbeatInterval(Duration d) {
            this.heartbeatInterval = d;
            return this;
        }

        public Builder heartbeatTimeout(Duration d) {
            this.heartbeatTimeout = d;
            return this;
        }

        public Builder reconnectInitialDelay(Duration d) {
            this.reconnectInitialDelay = d;
            return this;
        }

        public Builder reconnectMaxDelay(Duration d) {
            this.reconnectMaxDelay = d;
            return this;
        }

        public ClientConfig build() {
            return new ClientConfig(this);
        }
    }
}