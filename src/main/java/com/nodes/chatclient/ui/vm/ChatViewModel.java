package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.ChatStore;
import com.nodes.chatclient.store.model.ChatMessage;
import com.nodes.chatclient.store.events.StoreListener;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public final class ChatViewModel implements StoreListener {
    private final ChatStore store;
    private final String peerId;

    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();

    public ChatViewModel(ChatStore store, String peerId) {
        this.store = store;
        this.peerId = peerId;

        reload();

        store.addListener(this);
    }

    public String getPeerId() {
        return peerId;
    }

    public ObservableList<ChatMessage> getMessages() {
        return messages;
    }

    private void reload() {
        List<ChatMessage> snapshot = store.getMessagesSnapshot(peerId);
        Platform.runLater(() -> {
            messages.setAll(snapshot);
        });
    }

    @Override
    public void onMessageListUpdated(String peerId) {
        if (peerId.equals(this.peerId)) {
            reload();
        }
    }

    public void sendMessage(String text, AppContext ctx) {
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

    private String clientIdGenerator() {
        return "client-" + System.nanoTime();
    }
}
