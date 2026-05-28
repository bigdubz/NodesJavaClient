package com.nodes.chatclient.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.ws.messages.*;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
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

    private volatile String bootstrapJwt;
    private volatile String deviceToken;
    private volatile String userId;
    private volatile String deviceId;

    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicLong generation = new AtomicLong(0);

    private volatile long lastPongNanos;

    private ScheduledFuture<?> pingTask;
    private ScheduledFuture<?> pongWatchdog;

    private CompletableFuture<Void> authFuture;

    private final Queue<WsEnvelope> messageQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean routingEnabled = false;

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

    public void setAuth(String userId, String deviceId, String bootstrapJwt) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.bootstrapJwt = bootstrapJwt;
        this.deviceToken = null;
    }

    public String deviceToken() {
        return deviceToken;
    }

    private void handleAuthOk(ServerAuthOk.Payload p) {
        if (p == null) {
            failAuth("Authentication failed: empty AUTH_OK payload.");
            return;
        }
        if (!Objects.equals(userId, p.userId) || !Objects.equals(deviceId, p.deviceId)) {
            failAuth("Authentication failed: server identity echo did not match.");
            return;
        }
        if (isBlank(p.deviceToken)) {
            failAuth("Authentication failed: server did not return a device token.");
            return;
        }

        deviceToken = p.deviceToken;
        state = State.AUTHENTICATED;
        if (authFuture != null && !authFuture.isDone()) {
            authFuture.complete(null);
        }
    }

    public void enableRoutingAndFlush() {
        routingEnabled = true;

        WsEnvelope env;
        while ((env = messageQueue.poll()) != null) {
            System.out.println("DEQUEUED: {\"type\":\"" + env.type + "\",\"payload\":" + env.payload + "}");
            router.route(env);
        }
    }

    public void disableRouting() {
        routingEnabled = false;
    }

    private void handleAuthError(ServerAuthError.Payload p) {
        failAuth("Authentication failed.");
    }

    private void failAuth(String message) {
        state = State.CONNECTED;
        if (authFuture != null && !authFuture.isDone()) {
            authFuture.completeExceptionally(new RuntimeException(message));
        }
    }

    public CompletableFuture<Void> connectThenWaitAuth() {
        authFuture = new CompletableFuture<>();
        long gen = generation.incrementAndGet();
        scheduleConnect(gen, Duration.ZERO);
        return authFuture;
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

    public void sendAckAsync(
            String blobHash
    ) {
        if (webSocket == null) return;
        sendAsync(new ClientAck(blobHash));
    }

    public void sendEncryptedAsync(
            String toUserId,
            String toDeviceId,
            String blob
    ) {
        if (webSocket == null) return;
        sendAsync(new ClientEncryptedSend(toUserId, toDeviceId, blob));
    }

    public void sendReactionAsync(
            String messageId,
            String emoji,
            String toUserId
    ) {
        if (webSocket == null) return;
        ClientAddReaction msg = new ClientAddReaction(messageId, emoji, toUserId);

//        InternalMessage msgi = InternalMessage.reaction(
//                UUID.randomUUID().toString(),
//                System.currentTimeMillis(),
//                messageId,
//                emoji,
//                false
//        );

        sendAsync(msg);
    }

    public void sendRemoveReactionAsync(
            String messageId,
            String toUserId
    ) {
        if (webSocket == null) return;
        ClientRemoveReaction msg = new ClientRemoveReaction(messageId, toUserId);

//        InternalMessage msgi = InternalMessage.reaction(
//            UUID.randomUUID().toString(),
//            System.currentTimeMillis(),
//            messageId,
//            null, // seems wrong
//            true
//        );

        sendAsync(msg);
    }

    public void sendMessageSeenAsync(
            String messageId
    ) {
        if (webSocket == null) return;
        ClientMessageSeen msg = new ClientMessageSeen(messageId);

//        InternalMessage msgi = InternalMessage.control(
//                UUID.randomUUID().toString(),
//                System.currentTimeMillis(),
//                READ_RECEIPT,
//                messageId
//        );

        sendAsync(msg);
    }

    public void sendIsTypingAsync(
            String toUserId,
            boolean isTyping
    ) {
        if (webSocket == null) return;
        ClientTypingMessage msg = new ClientTypingMessage(toUserId, isTyping);

//        InternalMessage msgi = InternalMessage.control(
//                UUID.randomUUID().toString(),
//                System.currentTimeMillis(),
//                TYPING,
//                null
//        );

        sendAsync(msg);
    }

    public void sendAsync(Object message) {
        if (state != State.AUTHENTICATED) return;
        try {
            System.out.println("SENDING: " + mapper.writeValueAsString(message));
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
        if (bootstrapJwt == null || userId == null || deviceId == null) return;

        try {
            ClientAuthMessage msg = new ClientAuthMessage(userId, deviceId, bootstrapJwt);
            webSocket.sendText(mapper.writeValueAsString(msg), true).join();
        } catch (Exception ignored) {}
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isAuthResponse(WsEnvelope env) {
        return ServerAuthOk.type.equals(env.type) || ServerAuthError.type.equals(env.type);
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
            if (gen != generation.get()) return CompletableFuture.completedFuture(null);

            buffer.append(data);
            if (last) {
                try {
                    WsEnvelope env = mapper.readValue(buffer.toString(), WsEnvelope.class);
                    buffer.setLength(0);
                    if (!isAuthResponse(env) && (state != State.AUTHENTICATED || !routingEnabled)) {
                        System.out.println("QUEUED: " + data);
                        messageQueue.add(env);
                    } else {
                        System.out.println("RECEIVED THROUGH WS: " + data);
                        router.route(env);
                    }
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
