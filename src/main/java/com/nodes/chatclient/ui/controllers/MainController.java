package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.ChatStore;
import com.nodes.chatclient.ui.vm.ChatViewModel;
import com.nodes.chatclient.ui.vm.ConversationsViewModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public final class MainController {

    private final BorderPane root = new BorderPane();

    private final ConversationsViewModel conversationsVM;

    private final AppContext ctx;
    private final ChatStore store;

    public MainController(
            AppContext ctx,
            ChatStore store,
            Runnable onLogout
    ) {
        this.ctx = ctx;
        this.store = store;

        conversationsVM = new ConversationsViewModel(store);

        ConversationsController conversationsController =
                new ConversationsController(
                        conversationsVM,
                        this::openChat
                );

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("button");
        logoutBtn.setOnAction(e -> onLogout.run());

        HBox topBar = new HBox(logoutBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10));

        root.setLeft(conversationsController.getRoot());
        root.setTop(topBar);
    }

    private void openChat(String peerId) {
        store.setActiveConversation(peerId);

        ChatViewModel chatVM = new ChatViewModel(ctx, store, peerId);
        ChatController chatController = new ChatController(chatVM);

        long cursor = store.getConversation(peerId)
                .map(c -> c.lastTimestamp + 1) // +1 because backend compares timestamp with < not <=
                .orElse(Long.MAX_VALUE);

        int defaultLimit = 50;
        ctx.chatApi
                .getHistoryAsync(ctx.jwt, peerId, cursor, defaultLimit)
                .thenAccept(rows -> store.mergeHistory(peerId, rows));

        Platform.runLater(() -> root.setCenter(chatController.getRoot()));
    }

    public Parent getRoot() {
        return root;
    }

    public void reset() {
        root.setCenter(null);
        conversationsVM.reset();
        store.setActiveConversation(null);
    }
}
