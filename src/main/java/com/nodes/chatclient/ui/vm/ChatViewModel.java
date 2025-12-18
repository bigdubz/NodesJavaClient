package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.store.ChatStore;
import com.nodes.chatclient.store.model.ChatMessage;
import com.nodes.chatclient.ui.fx.FxDispatcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Comparator;

public final class ChatViewModel {
    private final ChatStore store;
    private final String peerId;

    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();

    public ChatViewModel(ChatStore store, String peerId) {
        this.store = store;
        this.peerId = peerId;
//        store.addListener(this::refresh);
    }

    public ObservableList<ChatMessage> getMessages() {
        return FXCollections.observableArrayList(
                store.getMessagesSnapshot(peerId)
        );
    }

    public String getPeerId() {
        return peerId;
    }
}
