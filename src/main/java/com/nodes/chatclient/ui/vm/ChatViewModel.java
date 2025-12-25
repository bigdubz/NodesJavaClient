package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.ChatStore;
import com.nodes.chatclient.store.model.ChatMessage;
import com.nodes.chatclient.store.events.StoreListener;

import com.nodes.chatclient.store.model.ChatMessageUi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.OptionalLong;

public final class ChatViewModel implements StoreListener {

    private final AppContext ctx;
    private final ChatStore store;
    private String peerId;

    private boolean loadingHistory = false;
    private boolean hasMoreHistory = true;

    private final ObservableList<ChatMessageUi> messages = FXCollections.observableArrayList();
    private HistoryPrependListener historyListener;

    private volatile boolean disposed = false;

    public ChatViewModel(AppContext ctx, ChatStore store) {
        this.ctx = ctx;
        this.store = store;
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

    public interface HistoryPrependListener {
        void onHistoryPrepended();
    }
}
