package com.nodes.chatclient.store;

import com.nodes.chatclient.http.dto.ConversationRowDto;
import com.nodes.chatclient.http.dto.MessageRowDto;
import com.nodes.chatclient.store.events.StoreListener;
import com.nodes.chatclient.ws.WsMessageRouter;
import com.nodes.chatclient.ws.messages.*;
import com.nodes.chatclient.store.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ChatStore implements WsMessageRouter.ServerHandlers {

    private final Executor storeExecutor = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "chat-store")
    );

    private final String selfUserId;

    private final Map<String, Conversation> conversations = new HashMap<>();
    private final Map<String, List<ChatMessage>> messages = new ConcurrentHashMap<>();
    private final Map<String, Presence> presence = new HashMap<>();
    private volatile String activeConversationPeerId;

    private final List<StoreListener> listeners = new ArrayList<>();

    public ChatStore(String selfUserId) {
        this.selfUserId = Objects.requireNonNull(selfUserId, "selfUserId");
    }

    public void addListener(StoreListener listener) {
        listeners.add(listener);
    }

    private void notifyConversationsUpdated() {
        listeners.forEach(StoreListener::onConversationsUpdated);
    }

    private void notifyMessageListUpdated(String peerId) {
        listeners.forEach(l -> l.onMessageListUpdated(peerId));
    }

    public void setActiveConversation(String peerId) {
        this.activeConversationPeerId = peerId;
    }

    public Optional<Conversation> getConversation(String peerId) {
        return Optional.ofNullable(conversations.get(peerId));
    }

    public void mergeConversations(List<ConversationRowDto> rows) {
        storeExecutor.execute(() -> {
            for (ConversationRowDto row : rows) {
                Conversation convo = conversations.computeIfAbsent(
                        row.peerId,
                        Conversation::new
                );

                convo.lastMessage = row.lastMessage;
                convo.lastTimestamp = Math.max(convo.lastTimestamp, row.lastTimestamp);
                convo.unreadCount = Math.max(convo.unreadCount, row.unreadCount);

                Presence pr = presence.computeIfAbsent(
                        row.peerId,
                        Presence::new
                );

                pr.online = row.isOnline;
            }

            notifyConversationsUpdated();
        });
    }

    public void mergeHistory(String peerId, List<MessageRowDto> rows) {
        storeExecutor.execute(() -> {
            Conversation convo = conversations.computeIfAbsent(
                    peerId,
                    Conversation::new
            );

            for (MessageRowDto row : rows) {
                if (convo.messages.containsKey(row.messageId)) {
                    continue;
                }

                ChatMessage msg = ChatMessage.fromHistory(
                        row.messageId,
                        row.fromUserId,
                        row.toUserId,
                        row.text,
                        row.createdAt,
                        row.replyingTo
                );

                msg.delivered = row.delivered == 1;
                msg.read = row.read == 1;

                if (row.reactions != null) {
                    msg.reactions.putAll(row.reactions);
                }

                convo.messages.put(msg.messageId, msg);

                if (msg.createdAt > convo.lastTimestamp) {
                    convo.lastTimestamp = msg.createdAt;
                    convo.lastMessage = msg.text;
                }
            }

            notifyMessageListUpdated(peerId);
        });
    }

    public List<Conversation> getConversationsSnapshot() {
        return List.copyOf(conversations.values());
    }

    public List<ChatMessage> getMessagesSnapshot(String peerId) {
        List<ChatMessage> list = messages.get(peerId);

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        return List.copyOf(list);
    }

    @Override
    public void onAuthOk(ServerAuthOk.Payload payload) {
        System.out.println("WS AUTH OK");
    }

    @Override
    public void onAuthError(ServerAuthError.Payload payload) {

    }

    @Override
    public void onChatMessage(ServerChatMessage.Payload p) {
        storeExecutor.execute(() -> {
            String peerId = p.fromUserId;

            Conversation convo = conversations.computeIfAbsent(
                    peerId, Conversation::new
            );


            if (convo.messages.containsKey(p.messageId)) {
                return;
            }

            ChatMessage msg = ChatMessage.incoming(
                    p.messageId,
                    p.fromUserId,
                    selfUserId,
                    p.text,
                    p.createdAt,
                    p.replyingTo
            );

            messages.computeIfAbsent(peerId, k -> new ArrayList<>()).add(msg);
            convo.messages.put(msg.messageId, msg);
            convo.lastTimestamp = msg.createdAt;
            convo.lastMessage = msg.text;

            boolean isActive = peerId.equals(activeConversationPeerId);

            if (!isActive) {
                convo.unreadCount++;
            }

            notifyConversationsUpdated();
            notifyMessageListUpdated(peerId);
        });
    }

    @Override
    public void onMessageDelivered(ServerMessageDelivered.Payload p) {
        storeExecutor.execute(() -> {
            findMessage(p.messageId).ifPresent(msg -> {
                if (!msg.delivered) {
                    msg.delivered = true;
                    notifyMessageListUpdated(p.clientId);
                }
            });
        });
    }

    @Override
    public void onMessageSeen(ServerMessageSeen.Payload p) {
        storeExecutor.execute(() -> {
            findMessage(p.messageId).ifPresent(msg -> {
                if (!msg.read) {
                    msg.read = true;
                }
            });
        });
    }

    @Override
    public void onUserTyping(ServerUserTyping.Payload p) {
        storeExecutor.execute(() -> {
            Presence pr = presence.computeIfAbsent(
                    p.fromUserId, Presence::new
            );
            pr.isTyping = p.isTyping;
            notifyMessageListUpdated(p.fromUserId);
        });
    }

    @Override
    public void onUserOnline(ServerUserOnline.Payload p) {
        storeExecutor.execute(() -> {
            Presence pr = presence.computeIfAbsent(
                    p.userId, Presence::new
            );
            pr.online = true;
            pr.lastSeen = null;
            notifyConversationsUpdated();
            notifyMessageListUpdated(p.userId);
        });
    }

    @Override
    public void onUserOffline(ServerUserOffline.Payload p) {
        storeExecutor.execute(() -> {
            Presence pr = presence.computeIfAbsent(
                    p.userId, Presence::new
            );
            pr.online = false;
            pr.lastSeen = p.lastSeen;
            notifyConversationsUpdated();
            notifyMessageListUpdated(p.userId);
        });
    }

    @Override
    public void onAddReaction(ServerAddReaction.Payload p) {
        storeExecutor.execute(() -> {
            findMessage(p.messageId).ifPresent(msg -> {
                msg.reactions.put(p.userId, p.reaction);
                notifyMessageListUpdated(p.userId);
            });
        });
    }

    @Override
    public void onRemoveReaction(ServerRemoveReaction.Payload p) {
        storeExecutor.execute(() -> {
            findMessage(p.messageId).ifPresent(msg -> {
                msg.reactions.remove(p.userId);
                notifyMessageListUpdated(p.userId);
            });
        });
    }

    private Optional<ChatMessage> findMessage(String messageId) {
        for (Conversation c : conversations.values()) {
            ChatMessage m = c.messages.get(messageId);
            if (m != null) return Optional.of(m);
        }
        return Optional.empty();
    }
}
