package com.nodes.chatclient.store;

import com.nodes.chatclient.http.dto.ConversationRowDto;
import com.nodes.chatclient.http.dto.MessageRowDto;
import com.nodes.chatclient.store.events.StoreListener;
import com.nodes.chatclient.util.Pair;
import com.nodes.chatclient.ws.ServerHandlers;
import com.nodes.chatclient.ws.messages.*;
import com.nodes.chatclient.store.model.*;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ChatStore implements ServerHandlers {

    private final Executor storeExecutor = Executors.newSingleThreadExecutor(
            r -> {
                Thread t = new Thread(r, "chat-store");
                t.setDaemon(true);
                return t;
            }
    );

    private final String selfUserId;

    private final Map<String, Conversation> conversations = new HashMap<>();
    private final Map<String, Presence> presence = new HashMap<>();
    private volatile String activeConversationPeerId;

    private final List<StoreListener> listeners = new ArrayList<>();

    public ChatStore(String selfUserId) {
        this.selfUserId = Objects.requireNonNull(selfUserId, "selfUserId");
    }

    public void addListener(StoreListener listener) {
        listeners.add(listener);
    }

    public void removeListener(StoreListener listener) {
        listeners.remove(listener);
    }

    private void notifyConversationsUpdated() {
        listeners.forEach(StoreListener::onConversationsUpdated);
    }

    private void notifyMessageListUpdated(String receiver) {
        listeners.forEach(l -> l.onMessageListUpdated(receiver));
    }

    private void notifyTypingStatusUpdated(String receiver) {
        listeners.forEach(l -> l.onTypingStatusUpdated(receiver));
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

                Presence pr = presence.computeIfAbsent(
                        row.peerId,
                        Presence::new
                );

                pr.online = row.isOnline;
            }
            notifyConversationsUpdated();
        });
    }

    public void mergeHistory(String receiver, List<MessageRowDto> rows) {
        storeExecutor.execute(() -> {
            Conversation convo = conversations.computeIfAbsent(
                    receiver,
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
                msg.read = row.seen == 1;

                if (row.reactions != null) {
                    msg.reactions.putAll(row.reactions);
                }

                convo.messages.put(msg.messageId, msg);

                if (msg.createdAt > convo.lastTimestamp) {
                    convo.lastTimestamp = msg.createdAt;
                    convo.lastMessage = msg.text;
                }
            }

            notifyMessageListUpdated(receiver);
        });
    }

    public void bulkMarkMessagesAsSeen(String receiver, List<String> messages) {
        if (messages.isEmpty()) return;
        Optional<Conversation> conv = getConversation(receiver);
        if (conv.isPresent()) {
            Conversation convo = conv.get();
            int count = 0;
            for (String messageId : messages) {
                ChatMessage msg = convo.messages.get(messageId);
                if (msg != null) {
                    msg.read = true;
                    count++;
                }
            }
            if (count > 0) {
                convo.unreadCount = Math.max(convo.unreadCount - count, 0);
                notifyMessageListUpdated(receiver);
                notifyConversationsUpdated();
            }
        }
    }

    public List<ConversationUi> getConversationsSnapshot() {
        return conversations.values()
                .stream()
                .sorted(Comparator.comparingLong(m -> m.lastTimestamp))
                .map(Conversation::toUi)
                .toList().reversed();
    }

    public List<ChatMessageUi> getMessagesSnapshot(String peerId) {
        Conversation convo = conversations.get(peerId);
        if (convo == null || convo.messages.isEmpty()) {
            return Collections.emptyList();
        }

        return convo.messages.values()
                .stream()
                .sorted(Comparator.comparingLong(m -> m.createdAt))
                .map(ChatMessage::toUi)
                .toList();
    }

    public boolean getIsTyping(String peerId) {
        return presence.getOrDefault(peerId, new Presence(peerId)).isTyping;
    }

    public OptionalLong getOldestMessageTimestamp(String peerId) {
        Conversation convo = conversations.get(peerId);
        if (convo == null || convo.messages.isEmpty()) {
            return OptionalLong.empty();
        }

        return convo.messages.values()
                .stream()
                .mapToLong(m -> m.createdAt)
                .min();
    }

    public void addOutgoingMessage(String receiver, ChatMessage msg) {
        storeExecutor.execute(() -> {
            Conversation convo = conversations.computeIfAbsent(
                    receiver,
                    Conversation::new
            );
            convo.messages.put(msg.messageId, msg);
            convo.lastTimestamp = msg.createdAt;
            convo.lastMessage = msg.text;
            notifyMessageListUpdated(receiver);
            notifyConversationsUpdated();
        });
    }

    public void resetChat(String peerId) {
        if (conversations.containsKey(peerId)) {
            conversations.get(peerId).messages.clear();
        }
    }

    @Override
    public void onAuthOk(ServerAuthOk.Payload payload) {}

    @Override
    public void onAuthError(ServerAuthError.Payload payload) {}

    @Override
    public void onChatMessage(ServerChatMessage.Payload p) {
        storeExecutor.execute(() -> {
            String receiver = p.fromUserId;

            Conversation convo = conversations.computeIfAbsent(
                    receiver, Conversation::new
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

            convo.messages.put(msg.messageId, msg);
            convo.lastTimestamp = msg.createdAt;
            convo.lastMessage = msg.text;

            boolean isActive = receiver.equals(activeConversationPeerId);

            if (!isActive) {
                convo.unreadCount++;
            }

            notifyMessageListUpdated(receiver);
            notifyConversationsUpdated();
        });
    }

    @Override
    public void onMessageDelivered(ServerMessageDelivered.Payload p) {
        storeExecutor.execute(() -> findMessage(p.clientId).ifPresent(msgPair -> {
            String convoId = msgPair.v1();
            ChatMessage msg = msgPair.v2();
            boolean changedId = !msg.messageId.equals(p.messageId);

            Conversation convo = conversations.get(convoId);
            if (convo != null && changedId) {
                convo.messages.remove(msg.messageId);
                msg.messageId = p.messageId;
                convo.messages.put(msg.messageId, msg);
            }

            msg.delivered = true;
            notifyMessageListUpdated(msg.toUserId);
        }));
    }

    @Override
    public void onMessageSeen(ServerMessageSeen.Payload p) {
        storeExecutor.execute(() -> findMessage(p.messageId).ifPresent(msgPair -> {
            ChatMessage msg = msgPair.v2();
            if (!msg.read) {
                msg.read = true;
            }
            notifyMessageListUpdated(msg.toUserId);
        }));
    }

    @Override
    public void onUserTyping(ServerUserTyping.Payload p) {
        storeExecutor.execute(() -> {
            Presence pr = presence.computeIfAbsent(
                    p.fromUserId, Presence::new
            );
            pr.isTyping = p.isTyping;
            conversations.computeIfPresent(p.fromUserId, (k, v) -> {
                v.isTyping = p.isTyping;
                return v;
            });
            notifyTypingStatusUpdated(p.fromUserId);
            notifyConversationsUpdated();
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
            conversations.computeIfPresent(p.userId, (k, v) -> {
                v.isOnline = true;
                return v;
            });
            notifyConversationsUpdated();
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
            conversations.computeIfPresent(p.userId, (k, v) -> {
                v.isOnline = false;
                return v;
            });
            notifyConversationsUpdated();
        });
    }

    @Override
    public void onAddReaction(ServerAddReaction.Payload p) {
        storeExecutor.execute(() -> findMessage(p.messageId).ifPresent(msgPair -> {
            String convoId = msgPair.v1(); // receiver
            ChatMessage msg = msgPair.v2();
            msg.reactions.put(p.userId, p.reaction);
            notifyMessageListUpdated(convoId);
        }));
    }

    @Override
    public void onRemoveReaction(ServerRemoveReaction.Payload p) {
        storeExecutor.execute(() -> findMessage(p.messageId).ifPresent(msgPair -> {
            String convoId = msgPair.v1(); // receiver
            ChatMessage msg = msgPair.v2();
            msg.reactions.remove(p.userId);
            notifyMessageListUpdated(convoId);
        }));
    }

    /**
     *  Finds the message with the messageId passed and returns it with the conversation id
     *  of the conversation in which the message exists as a Pair where v1 is Conversation ID (String)
     *  and v2 is the ChatMessage. Optional is used because the message might not exist.
     *
     *  @param messageId the id to search for
     *  @return an Optional of the Pair
     *  @see Pair
     *  @see Optional
     */
    private Optional<Pair<String, ChatMessage>> findMessage(String messageId) {
        for (Conversation c : conversations.values()) {
            ChatMessage m = c.messages.get(messageId);
            if (m != null) return Optional.of(new Pair<>(c.peerId, m));
        }
        return Optional.empty();
    }
}
