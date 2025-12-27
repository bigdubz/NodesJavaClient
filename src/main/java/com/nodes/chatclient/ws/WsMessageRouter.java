package com.nodes.chatclient.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.ws.messages.*;

import java.util.*;
import java.util.function.Consumer;

public final class WsMessageRouter {
    private final ObjectMapper mapper;

    private final Map<String, List<Consumer<JsonNode>>> coreHandlers = new HashMap<>();
    private final Map<String, List<Consumer<JsonNode>>> sessionHandlers = new HashMap<>();

    public WsMessageRouter(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public <T> void onCore(
            String type,
            Class<T> payloadClass,
            Consumer<T> handler
    ) {
        register(coreHandlers, type, payloadClass, handler);
    }

    public <T> void onSession(
            String type,
            Class<T> payloadClass,
            Consumer<T> handler
    ) {
        register(sessionHandlers, type, payloadClass, handler);
    }

    private <T> void register(
            Map<String, List<Consumer<JsonNode>>> map,
            String type,
            Class<T> payloadClass,
            Consumer<T> handler
    ) {
        map.computeIfAbsent(type, k -> new ArrayList<>())
                .add(payload -> {
                    try {
                        handler.accept(mapper.treeToValue(payload, payloadClass));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to deserialize payload for type " + type, e);
                    }
                });
    }

    public void route (WsEnvelope env) {
        if (env == null || env.type == null) return;

        dispatch(coreHandlers, env);
        dispatch(sessionHandlers, env);
    }

    private void dispatch(
            Map<String, List<Consumer<JsonNode>>> map,
            WsEnvelope env
    ) {
        List<Consumer<JsonNode>> list = map.get(env.type);
        if (list != null) {
            for (Consumer<JsonNode> c : list) {
                c.accept(env.payload);
            }
        }
    }

    public void registerServerHandlers(ServerHandlers h)  {
        Objects.requireNonNull(h, "handlers");

        onCore("AUTH_OK", ServerAuthOk.Payload.class, h::onAuthOk);
        onCore("AUTH_ERROR", ServerAuthError.Payload.class, h::onAuthError);

        onSession("CHAT_MESSAGE", ServerChatMessage.Payload.class, h::onChatMessage);

        onSession("MESSAGE_DELIVERED", ServerMessageDelivered.Payload.class, h::onMessageDelivered);
        onSession("MESSAGE_SEEN", ServerMessageSeen.Payload.class, h::onMessageSeen);

        onSession("USER_TYPING", ServerUserTyping.Payload.class, h::onUserTyping);

        onSession("USER_ONLINE", ServerUserOnline.Payload.class, h::onUserOnline);
        onSession("USER_OFFLINE", ServerUserOffline.Payload.class, h::onUserOffline);

        onSession("ADD_REACTION", ServerAddReaction.Payload.class, h::onAddReaction);
        onSession("REMOVE_REACTION", ServerRemoveReaction.Payload.class, h::onRemoveReaction);
    }

    public void clearHandlers() {
        sessionHandlers.clear();
    }
}
