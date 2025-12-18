package com.nodes.chatclient.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.ws.messages.*;

import java.util.*;
import java.util.function.Consumer;

public final class WsMessageRouter {
    private final ObjectMapper mapper;
    private final Map<String, List<Consumer<JsonNode>>> handlers = new HashMap<>();
    private Consumer<WsEnvelope> fallbackHandler = env -> {};

    public WsMessageRouter(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public <T> void on(
            String type,
            Class<T> payloadClass,
            Consumer<T> handler
    ) {
        handlers
                .computeIfAbsent(type, k -> new ArrayList<>())
                .add(payload -> {
                    try {
                        T msg = mapper.treeToValue(payload, payloadClass);
                        handler.accept(msg);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to deserialize payload for type " + type, e
                        );
                    }
                });
    }

    public void setFallbackHandler(Consumer<WsEnvelope> fallbackHandler) {
        this.fallbackHandler = fallbackHandler != null ? fallbackHandler : env -> {};
    }

    public void route (WsEnvelope env) {
        if (env == null || env.type == null) return;

        List<Consumer<JsonNode>> list = handlers.get(env.type);
        if (list != null) {
            for (Consumer<JsonNode> handler : list) {
                handler.accept(env.payload);
            }
        } else {
            fallbackHandler.accept(env);
        }
    }

    public void registerServerHandlers(ServerHandlers h)  {
        Objects.requireNonNull(h, "handlers");

        on("AUTH_OK", ServerAuthOk.Payload.class, h::onAuthOk);
        on("AUTH_ERROR", ServerAuthError.Payload.class, h::onAuthError);

        on("CHAT_MESSAGE", ServerChatMessage.Payload.class, h::onChatMessage);

        on("MESSAGE_DELIVERED", ServerMessageDelivered.Payload.class, h::onMessageDelivered);
        on("MESSAGE_SEEN", ServerMessageSeen.Payload.class, h::onMessageSeen);

        on("USER_TYPING", ServerUserTyping.Payload.class, h::onUserTyping);

        on("USER_ONLINE", ServerUserOnline.Payload.class, h::onUserOnline);
        on("USER_OFFLINE", ServerUserOffline.Payload.class, h::onUserOffline);

        on("ADD_REACTION", ServerAddReaction.Payload.class, h::onAddReaction);
        on("REMOVE_REACTION", ServerRemoveReaction.Payload.class, h::onRemoveReaction);
    }

    public interface ServerHandlers {
        void onAuthOk(ServerAuthOk.Payload payload);
        void onAuthError(ServerAuthError.Payload payload);

        void onChatMessage(ServerChatMessage.Payload payload);

        void onMessageDelivered(ServerMessageDelivered.Payload payload);
        void onMessageSeen(ServerMessageSeen.Payload payload);

        void onUserTyping(ServerUserTyping.Payload payload);

        void onUserOnline(ServerUserOnline.Payload payload);
        void onUserOffline(ServerUserOffline.Payload payload);

        void onAddReaction(ServerAddReaction.Payload payload);
        void onRemoveReaction(ServerRemoveReaction.Payload payload);
    }
}
