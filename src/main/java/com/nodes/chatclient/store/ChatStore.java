package com.nodes.chatclient.store;

import com.nodes.chatclient.e2ee.provisioning.MessageDecryptionService;
import com.nodes.chatclient.e2ee.records.ContactRecord;
import com.nodes.chatclient.e2ee.records.MessageRecord;
import com.nodes.chatclient.e2ee.db.stores.ContactStore;
import com.nodes.chatclient.e2ee.db.stores.MessageStore;
import com.nodes.chatclient.e2ee.mappers.OuterPayloadMapper;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;
import com.nodes.chatclient.e2ee.types.InternalMessage;
import com.nodes.chatclient.store.events.StoreListener;
import com.nodes.chatclient.util.Pair;
import com.nodes.chatclient.ws.ServerHandlers;
import com.nodes.chatclient.ws.messages.*;
import com.nodes.chatclient.store.model.*;

import java.sql.SQLException;
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
    private final String selfDeviceId;
    private final ContactStore contactStore;
    private final MessageStore messageStore;
    private final MessageDecryptionService decryptionService;

    private final Map<String, Conversation> conversations = new HashMap<>();
    private final Map<String, Presence> presence = new HashMap<>();
    private volatile String activeConversationPeerId;

    private final List<StoreListener> listeners = new ArrayList<>();

    public ChatStore(String selfUserId,
                     String selfDeviceId,
                     ContactStore contactStore,
                     MessageStore messageStore,
                     MessageDecryptionService decryptionService) {
        this.selfUserId = Objects.requireNonNull(selfUserId, "selfUserId");
        this.selfDeviceId = Objects.requireNonNull(selfDeviceId, "selfDeviceId");
        this.contactStore = Objects.requireNonNull(contactStore, "contactStore");
        this.messageStore = Objects.requireNonNull(messageStore, "messageStore");
        this.decryptionService = Objects.requireNonNull(decryptionService, "messageDecryptionService");
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

    public void loadLocalConversations() {
        storeExecutor.execute(() -> {
            try {
                Set<String> contactIds = new HashSet<>();
                for (ContactRecord contact : contactStore.getAll()) {
                    contactIds.add(contact.userId());
                    Conversation convo = conversations.computeIfAbsent(
                            contact.userId(),
                            Conversation::new
                    );

                    applyLastMessage(convo);
                }
                conversations.keySet().removeIf(peerId -> !contactIds.contains(peerId));

                notifyConversationsUpdated();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load contacts from local database", e);
            }
        });
    }

    public void loadLocalHistory(String peerId) {
        storeExecutor.execute(() -> {
            try {
                Conversation convo = conversations.computeIfAbsent(
                        peerId,
                        Conversation::new
                );

                convo.messages.clear();
                for (MessageRecord row : messageStore.getConversation(peerId)) {
                    if (row.type != 0) {
                        continue;
                    }

                    ChatMessage msg = fromMessageRecord(peerId, row);
                    convo.messages.put(msg.messageId, msg);
                }

                applyLastMessage(convo);
                notifyMessageListUpdated(peerId);
                notifyConversationsUpdated();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load chat history from local database", e);
            }
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
            persistTextMessage(receiver, msg, true, selfDeviceId, 0);
            notifyMessageListUpdated(receiver);
            notifyConversationsUpdated();
        });
    }

    public void resetChat(String peerId) {
        if (conversations.containsKey(peerId)) {
            conversations.get(peerId).messages.clear();
        }
    }

    private ChatMessage fromMessageRecord(String peerId, MessageRecord row) {
        ChatMessage msg = ChatMessage.fromHistory(
                row.messageId,
                row.senderUserId,
                row.isOutgoing ? peerId : selfUserId,
                row.body,
                row.createdAt,
                row.referencedMessageId
        );

        msg.delivered = row.deliveryStatus >= 1;
        msg.read = row.deliveryStatus >= 2;

        return msg;
    }

    private void applyLastMessage(Conversation convo) throws SQLException {
        convo.lastMessage = "";
        convo.lastTimestamp = 0;

        List<MessageRecord> rows = messageStore.getConversation(convo.peerId);
        for (MessageRecord row : rows) {
            if (row.type != 0) {
                continue;
            }

            if (row.createdAt >= convo.lastTimestamp) {
                convo.lastTimestamp = row.createdAt;
                convo.lastMessage = row.body;
            }
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
    public void onEncryptedRelay(ServerEncryptedRelay.Payload payload) {
        storeExecutor.execute(() -> {
            try {
                EncryptedMessage encryptedMessage = OuterPayloadMapper.deserializeRelayBlob(payload.blob);
                InternalMessage decryptedMessage = decryptionService.decryptMessage(encryptedMessage);
                if (decryptedMessage == null) {
                    return;
                }

                if (decryptedMessage.type == InternalMessage.Type.TEXT) {
                    addIncomingTextMessage(encryptedMessage, decryptedMessage);
                }
            } catch (Exception e) {
                System.err.println("Failed to handle encrypted relay: " + e.getMessage());
            }
        });
    }

    private void addIncomingTextMessage(EncryptedMessage encryptedMessage, InternalMessage decryptedMessage) {
        String peerId = encryptedMessage.fromUserId;
        Conversation convo = conversations.computeIfAbsent(peerId, Conversation::new);

        if (convo.messages.containsKey(decryptedMessage.messageId)) {
            return;
        }

        ChatMessage msg = ChatMessage.incoming(
                decryptedMessage.messageId,
                encryptedMessage.fromUserId,
                selfUserId,
                decryptedMessage.body,
                decryptedMessage.createdAt,
                decryptedMessage.referencedMessageId
        );

        convo.messages.put(msg.messageId, msg);
        convo.lastTimestamp = msg.createdAt;
        convo.lastMessage = msg.text;

        boolean isActive = peerId.equals(activeConversationPeerId);
        if (!isActive) {
            convo.unreadCount++;
        }

        persistTextMessage(peerId, msg, false, encryptedMessage.fromDeviceId, isActive ? 2 : 1);
        notifyMessageListUpdated(peerId);
        notifyConversationsUpdated();
    }

    private void persistTextMessage(
            String conversationId,
            ChatMessage msg,
            boolean isOutgoing,
            String senderDeviceId,
            int deliveryStatus
    ) {
        MessageRecord record = new MessageRecord();
        record.messageId = msg.messageId;
        record.conversationId = conversationId;
        record.senderUserId = msg.fromUserId;
        record.senderDeviceId = senderDeviceId;
        record.createdAt = msg.createdAt;
        record.receivedAt = System.currentTimeMillis();
        record.isOutgoing = isOutgoing;
        record.type = 0;
        record.deliveryStatus = deliveryStatus;
        record.body = msg.text;
        record.referencedMessageId = msg.replyingTo;

        try {
            messageStore.insert(record);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist message " + msg.messageId, e);
        }
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
