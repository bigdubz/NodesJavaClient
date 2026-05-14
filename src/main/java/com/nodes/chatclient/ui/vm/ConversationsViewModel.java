package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.ChatStore;
import com.nodes.chatclient.store.events.StoreListener;
import com.nodes.chatclient.store.model.ConversationUi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public final class ConversationsViewModel implements StoreListener {

    private final ChatStore store;
    private final AppContext ctx;

    private final ObservableList<ConversationUi> conversations =
            FXCollections.observableArrayList();

    public ConversationsViewModel(AppContext ctx, ChatStore store) {
        this.store = store;
        this.ctx = ctx;

        store.addListener(this);
        updateFromStore();
    }

    public ObservableList<ConversationUi> getConversations() {
        return conversations;
    }

    private void updateFromStore() {
        List<ConversationUi> snapshot = store.getConversationsSnapshot();
        Platform.runLater(() -> conversations.setAll(snapshot));
    }

    public void deleteContact(String peerId) {
        if (!ctx.contactProvisioningService.deleteContact(peerId)) {
            throw new RuntimeException("Failed to delete contact");
        }
        store.loadLocalConversations();
    }

    @Override
    public void onConversationsUpdated() {
        updateFromStore();
    }

    @Override
    public void onMessageListUpdated(String receiver) {
        // ignore – ConversationView changes already reflected through unread counts
        updateFromStore();
    }

    public void reset() {
        conversations.clear();
        store.removeListener(this);
    }
}
