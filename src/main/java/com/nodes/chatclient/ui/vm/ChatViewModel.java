package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.ChatStore;
import com.nodes.chatclient.store.model.ChatMessage;
import com.nodes.chatclient.store.events.StoreListener;

import com.nodes.chatclient.store.model.ChatMessageUi;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public final class ChatViewModel implements StoreListener {

    private final AppContext ctx;
    private final ChatStore store;
    private final String selfId;
    private String peerId;

    private boolean loadingHistory = false;
    private boolean hasMoreHistory = true;

    private final ObservableList<ChatMessageUi> messages = FXCollections.observableArrayList();
    private HistoryPrependListener historyListener;

    private final BooleanProperty isTypingProperty = new SimpleBooleanProperty(false);

    private volatile boolean disposed = false;

    public ChatViewModel(AppContext ctx, ChatStore store) {
        this.ctx = ctx;
        this.store = store;
        this.selfId = ctx.userId;
    }

    public void setPeer(String peerId) {
        disposed = false;
        this.peerId = peerId;

        reload();
        store.addListener(this);
    }

    public void setHistoryListener(HistoryPrependListener h) {
        this.historyListener = h;
    }

    public String getPeerId() {
        return peerId;
    }

    public ObservableList<ChatMessageUi> getMessages() {
        return messages;
    }

    private void reload() {
        if (disposed || peerId == null) return;
        List<ChatMessageUi> snapshot = store.getMessagesSnapshot(peerId);
        int oldSize = messages.size();
        Platform.runLater(() -> {
            messages.setAll(snapshot);

            if (snapshot.size() != oldSize && historyListener != null) {
                historyListener.onHistoryPrepended();
            }
        });
    }

    @Override
    public void onMessageListUpdated(String receiver) {
        if (receiver.equals(this.peerId)) {
            reload();
        }
    }

    @Override
    public void onTypingStatusUpdated(String receiver) {
        if (receiver.equals(this.peerId)) {
            isTypingProperty.set(store.getIsTyping(this.peerId));
        }
    }

    public void sendIsTyping(boolean isTyping) {
        ctx.wsService.sendIsTypingAsync(peerId, isTyping);
    }

    public void sendReaction(String messageId, String emoji) {
        ctx.wsService.sendReactionAsync(messageId, emoji, peerId);
    }

    public void removeReaction(String messageId) {
        ctx.wsService.sendRemoveReactionAsync(messageId, peerId);
    }

    public void sendMessage(String text, String replyingTo) {
        String clientId = clientIdGenerator();
        long createdAt = System.currentTimeMillis();
        ChatMessage local = ChatMessage.outgoing(
                clientId,
                ctx.userId,
                peerId,
                text,
                createdAt,
                replyingTo
        );
        store.addOutgoingMessage(peerId, local);

        ctx.sessionProvisioningService.ensureSessionsAsync(ctx.jwt, peerId)
                .thenAccept(ready -> {
                    if (!ready) {
                        System.err.println("Unable to establish session with " + peerId);
                        return;
                    }

                    try {
                        ctx.messageEncryptionService.encryptTextForUser(
                                peerId,
                                clientId,
                                createdAt,
                                text,
                                replyingTo
                        ).forEach(encrypted -> ctx.wsService.sendEncryptedAsync(
                                encrypted.toUserId(),
                                encrypted.toDeviceId(),
                                encrypted.blob()
                        ));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to encrypt message for " + peerId + " " + e.getMessage(), e);
                    }
                })
                .exceptionally(err -> {
                    System.err.println("Failed to establish session with " + peerId + ": " + err.getMessage());
                    return null;
                });
    }

    public void loadOlderHistory() {
        hasMoreHistory = false;
    }

    public void markVisibleMessagesAsSeen() {
        List<String> messages =
                getMessages().stream()
                        .filter(m -> !m.fromUserId.equals(ctx.userId) && !m.read)
                        .map(c -> c.messageId)
                        .toList();

        store.bulkMarkMessagesAsSeen(peerId, messages);
        for (String m : messages) {
            ctx.wsService.sendMessageSeenAsync(m);
        }
    }

    private String clientIdGenerator() {
        return "client-" + System.nanoTime();
    }

    public boolean isLoadingHistory() {
        return loadingHistory;
    }

    public void setLoadingHistory(boolean v) {
        loadingHistory = v;
    }

    public void reset() {
        disposed = true;
        loadingHistory = false;
        hasMoreHistory = true;
        this.messages.clear();
        historyListener = null;
        store.removeListener(this);
    }

    public BooleanProperty isTypingProperty() {
        return isTypingProperty;
    }

    public interface HistoryPrependListener {
        void onHistoryPrepended();
    }

    public String getSelfId() {
        return selfId;
    }
}
