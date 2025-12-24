package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.ChatStore;
import com.nodes.chatclient.store.model.ChatMessage;
import com.nodes.chatclient.store.events.StoreListener;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.OptionalLong;

public final class ChatViewModel implements StoreListener {

    private final AppContext ctx;
    private final ChatStore store;
    private final String peerId;

    private boolean loadingHistory = false;
    private boolean hasMoreHistory = true;

    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private HistoryPrependListener historyListener;

    public ChatViewModel(AppContext ctx, ChatStore store, String peerId) {
        this.ctx = ctx;
        this.store = store;
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

    public ObservableList<ChatMessage> getMessages() {
        return messages;
    }

    private void reload() {
        if (peerId == null) return;
        List<ChatMessage> snapshot = store.getMessagesSnapshot(peerId);
        int oldSize = messages.size();
        Platform.runLater(() -> {
            messages.setAll(snapshot);

            int newSize = messages.size();
            int added = newSize - oldSize;
            if (added > 0 && historyListener != null) {
                historyListener.onHistoryPrepended();
            }
        });
    }

    @Override
    public void onMessageListUpdated(String peerId) {
        if (peerId.equals(this.peerId)) {
            reload();
        }
    }

    public void sendMessage(String text) {
        String clientId = clientIdGenerator();
        ChatMessage local = ChatMessage.outgoing(
                clientId,
                ctx.userId,
                peerId,
                text,
                System.currentTimeMillis(),
                null
        );
        store.addOutgoingMessage(peerId, local);
        ctx.wsService.sendChatMessageAsync(
                peerId,
                text,
                clientId,
                null
        );
    }

    public void loadOlderHistory() {
        if (loadingHistory || !hasMoreHistory) return;

        loadingHistory = true;

        OptionalLong cursorOpt = store.getOldestMessageTimestamp(peerId);
        long cursor = cursorOpt.orElse(Long.MAX_VALUE);

        ctx.chatApi
                .getHistoryAsync(ctx.jwt, peerId, cursor, 50)
                .thenAccept(rows -> {
                    if (rows.isEmpty()) {
                        hasMoreHistory = false;
                    } else {
                        store.mergeHistory(peerId, rows);
                    }
                });
    }

    public void markVisibleMessagesAsSeen() {
        List<ChatMessage> messages = getMessages();

        for (ChatMessage m : messages) {
            if (!m.fromUserId.equals(ctx.userId) && !m.read) {
                ctx.wsService.sendMessageSeenAsync(m.messageId);
                m.read = true; // optimistic
            }
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

    public interface HistoryPrependListener {
        void onHistoryPrepended();
    }
}
