package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.store.ChatStore;
import com.nodes.chatclient.store.events.StoreListener;
import com.nodes.chatclient.store.model.Conversation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public final class ConversationsViewModel implements StoreListener {

    private final ChatStore store;

    private final ObservableList<Conversation> conversations =
            FXCollections.observableArrayList();

    public ConversationsViewModel(ChatStore store) {
        this.store = store;

        // bootstrap
        updateFromStore();

        // register listener
        store.addListener(this);
    }

    public ObservableList<Conversation> getConversations() {
        return conversations;
    }

    private void updateFromStore() {
        List<Conversation> snapshot = store.getConversationsSnapshot();
        Platform.runLater(() -> {
            conversations.setAll(snapshot);
        });
    }

    @Override
    public void onConversationsUpdated() {
        updateFromStore();
    }

    @Override
    public void onMessageListUpdated(String peerId) {
        // ignore – ConversationView changes already reflected through unread counts
        updateFromStore();
    }
}
