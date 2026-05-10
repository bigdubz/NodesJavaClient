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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

public final class MainController {

    private final StackPane root = new StackPane();
    private final BorderPane layout = new BorderPane();

    private final ConversationsViewModel conversationsVM;
    private final ChatViewModel activeChatVM;
    private final ChatController chatController;

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
        activeChatVM = new ChatViewModel(ctx, store);
        chatController = new ChatController();

        ConversationsController conversationsController =
                new ConversationsController(
                        conversationsVM,
                        this::openChat,
                        this::showAddContactOverlay
                );

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("button");
        logoutBtn.setOnAction(e -> onLogout.run());

        HBox topBar = new HBox(logoutBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10));

        layout.setLeft(conversationsController.getRoot());
        layout.setTop(topBar);

        root.getChildren().add(layout);
    }

    private void openChat(String peerId) {
        store.setActiveConversation(peerId);
        store.resetChat(peerId);

        activeChatVM.reset();
        chatController.reset();

        activeChatVM.setPeer(peerId);
        chatController.setVm(activeChatVM);

        long cursor = Long.MAX_VALUE;

        int defaultMessageLimit = 50;
        ctx.chatApi
                .getHistoryAsync(ctx.jwt, peerId, cursor, defaultMessageLimit)
                .thenAccept(rows -> store.mergeHistory(peerId, rows));

        Platform.runLater(() -> layout.setCenter(chatController.getRoot()));
    }

    private void showAddContactOverlay() {
        Region dimBackground = new Region();
        dimBackground.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        dimBackground.getStyleClass().add("dim-background");

        Label boxLabel = new Label("Add New Contact by Username");
        boxLabel.setAlignment(Pos.CENTER_LEFT);
        boxLabel.getStyleClass().add("box-label");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("input-field");

        Button closeButton = new Button("X");
        closeButton.getStyleClass().addAll("button", "close-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox closeRow = new HBox(10, boxLabel, spacer, closeButton);
        closeRow.setAlignment(Pos.CENTER_LEFT);

        VBox fieldBox = new VBox(usernameField);
        fieldBox.setAlignment(Pos.CENTER_LEFT);

        Button addButton = new Button("Add Contact");
        addButton.getStyleClass().addAll("button");
        addButton.setOnAction(e -> {
            String username = usernameField.getText();
            ctx.bundleProvisioningService.addContact(ctx.jwt, username);
        });

        VBox addContactBox = new VBox(12, closeRow, fieldBox, addButton);
        addContactBox.getStyleClass().add("add-contact-box");
        addContactBox.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(dimBackground, addContactBox);

        closeButton.setOnAction(e -> root.getChildren().remove(overlay));

        root.getChildren().add(overlay);

        usernameField.requestFocus();
    }

    public Parent getRoot() {
        return root;
    }

    public void reset() {
        layout.setCenter(null);
        conversationsVM.reset();
        store.setActiveConversation(null);
    }
}
