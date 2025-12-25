package com.nodes.chatclient.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.ws.messages.*;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class WsService {
    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        AUTHENTICATED
    }

    private final ClientConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final WsMessageRouter router;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "ws-scheduler");
                t.setDaemon(true);
                return t;
            }
    );

    private volatile WebSocket webSocket;
    private volatile State state = State.DISCONNECTED;

    private volatile String jwt;
    private volatile String userId;

    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicLong generation = new AtomicLong(0);

    private volatile long lastPongNanos;

    private ScheduledFuture<?> pingTask;
    private ScheduledFuture<?> pongWatchdog;

    public WsService(
            ClientConfig config,
            HttpClient httpClient,
            ObjectMapper mapper,
            WsMessageRouter router
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.router = Objects.requireNonNull(router, "router");

        router.onCore("AUTH_OK", ServerAuthOk.Payload.class, this::handleAuthOk);
        router.onCore("AUTH_ERROR", ServerAuthError.Payload.class, this::handleAuthError);
    }

    public void setAuth(String userId, String jwt) {
        this.userId = userId;
        this.jwt = jwt;
    }

    public State getState() {
        return state;
    }

    private void handleAuthOk(ServerAuthOk.Payload p) {
        state = State.AUTHENTICATED;
    }

    private void handleAuthError(ServerAuthError.Payload p) {
        System.out.println("WsService → AUTH_ERROR");
        // maybe logout? maybe reconnect?
    }


    public void connect() {
        long gen = generation.incrementAndGet();
        scheduleConnect(gen, Duration.ZERO);
    }

    public void disconnect() {
        generation.incrementAndGet();
        stopHeartbeat();
        state = State.DISCONNECTED;

        WebSocket ws = this.webSocket;
        this.webSocket = null;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "client disconnect");
            } catch (Exception ignored) {}
        }
    }

    public void sendChatMessageAsync(
            String toUserId,
            String text,
            String clientId,
            String replyingTo
    ) {
        if (webSocket == null) return;

        ClientChatMessage msg = new ClientChatMessage(
                toUserId,
                text,
                clientId,
                replyingTo
        );

        sendAsync(msg);
    }

    public void sendMessageSeenAsync(
            String messageId
    ) {
        if (webSocket == null) return;
        ClientMessageSeen msg = new ClientMessageSeen(messageId);
        sendAsync(msg);
    }

    public void sendAsync(Object message) {
        if (state != State.AUTHENTICATED) return;
        try {
            webSocket.sendText(mapper.writeValueAsString(message), true);
        } catch (Exception ignored) {
        }
    }

    private void scheduleConnect(long gen, Duration delay) {
        scheduler.schedule(() -> {
            if (gen != generation.get()) return;

            state = State.CONNECTING;

            httpClient.newWebSocketBuilder()
                    .connectTimeout(config.wsConnectTimeout())
                    .buildAsync(config.wsUri(), new Listener(gen))
                    .whenComplete((ws, err) -> {
                        if (gen != generation.get()) return;

                        if (err != null) {
                            scheduleReconnect(gen);
                            return;
                        }

                        webSocket = ws;
                        lastPongNanos = System.nanoTime();
                        state = State.CONNECTED;
                        reconnectAttempts.set(0);

                        startHeartbeat(gen);
                        sendAuth();
                    })
                    .join();

        }, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void scheduleReconnect(long gen) {
        stopHeartbeat();
        state = State.DISCONNECTED;

        int attempt = reconnectAttempts.incrementAndGet();
        long delayMs = Math.min(
                config.reconnectMaxDelay().toMillis(),
                config.reconnectInitialDelay().toMillis() * (1L << Math.min(attempt, 6))
        );

        scheduleConnect(gen, Duration.ofMillis(delayMs));
    }

    private void sendAuth() {
        if (jwt == null || userId == null) return;

        try {
            ClientAuthMessage msg = new ClientAuthMessage(userId, jwt);
            webSocket.sendText(mapper.writeValueAsString(msg), true).join();
        } catch (Exception ignored) {}
    }

    private void startHeartbeat(long gen) {
        stopHeartbeat();

        pingTask = scheduler.scheduleAtFixedRate(() -> {
            if (gen != generation.get()) return;
            WebSocket ws = webSocket;
            if (ws != null) {
                ws.sendPing(ByteBuffer.wrap(new byte[]{1}));
            }
        }, config.heartbeatInterval().toMillis(), config.heartbeatInterval().toMillis(), TimeUnit.MILLISECONDS);

        pongWatchdog = scheduler.scheduleAtFixedRate(() -> {
            long age = System.nanoTime() - lastPongNanos;
            if (age > config.heartbeatTimeout().toNanos()) {
                WebSocket ws = webSocket;
                webSocket = null;
                if (ws != null) ws.abort();
                scheduleReconnect(gen);
            }
        }, config.heartbeatInterval().toMillis(), config.heartbeatInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeat() {
        if (pingTask != null) pingTask.cancel(true);
        if (pongWatchdog != null) pongWatchdog.cancel(true);
        pingTask = null;
        pongWatchdog = null;
    }


    private final class Listener implements WebSocket.Listener {
        private final long gen;
        private final StringBuilder buffer = new StringBuilder();

        Listener(long gen) {
            this.gen = gen;
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            System.out.println("RECEIVED THROUGH WS: " + data);
            if (gen != generation.get()) return CompletableFuture.completedFuture(null);

            buffer.append(data);
            if (last) {
                try {
                    WsEnvelope env = mapper.readValue(buffer.toString(), WsEnvelope.class);
                    buffer.setLength(0);
                    router.route(env);
                } catch (Exception ignored) {}
            }

            ws.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
            ws.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket ws, ByteBuffer message) {
            lastPongNanos = System.nanoTime();
            ws.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            if (gen == generation.get()) {
                scheduleReconnect(gen);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            if (gen == generation.get()) {
                scheduleReconnect(gen);
            }
            ws.request(1);
        }
    }
}
