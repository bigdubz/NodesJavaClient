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

import java.util.Objects;

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

        conversationsVM = new ConversationsViewModel(ctx, store);
        activeChatVM = new ChatViewModel(ctx, store);
        chatController = new ChatController();

        ConversationsController conversationsController =
                new ConversationsController(
                        conversationsVM,
                        this::openChat,
                        this::showAddContactOverlay,
                        this::onDeleteConversation
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

        store.loadLocalHistory(peerId);

        Platform.runLater(() -> layout.setCenter(chatController.getRoot()));
    }

    private void onDeleteConversation(String peerId) {
        if (Objects.equals(activeChatVM.getPeerId(), peerId)) {
            store.setActiveConversation(null);
            store.resetChat(peerId);
            activeChatVM.reset();
            chatController.reset();
            Platform.runLater(() -> layout.setCenter(null));
        }
    }

    private void showAddContactOverlay() {
        Region dimBackground = new Region();
        dimBackground.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        dimBackground.getStyleClass().add("dim-background");

        Label boxLabel = new Label("Add New Contact by Username");
        boxLabel.setAlignment(Pos.CENTER_LEFT);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("input-field");

        Button closeButton = new Button("X");
        closeButton.getStyleClass().addAll("button", "close-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(10, boxLabel, spacer, closeButton);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label();
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setVisible(false);

        VBox fieldBox = new VBox(5, usernameField, messageLabel);
        fieldBox.setAlignment(Pos.CENTER_LEFT);

        Button addButton = new Button("Add Contact");
        addButton.getStyleClass().addAll("button");
        addButton.setOnAction(e -> {
            String username = usernameField.getText();
            if (Objects.equals(username, ctx.userId)) {
                messageLabel.setText("Cannot add yourself!");
                messageLabel.getStyleClass().add("error");
                messageLabel.setVisible(true);
                return;
            }
            ctx.contactProvisioningService.addContact(ctx.jwt, username)
                    .thenAccept(res -> Platform.runLater(() -> {
                        if (res) {
                            messageLabel.setText(username + " added to contacts!");
                            messageLabel.getStyleClass().remove("error");
                            store.loadLocalConversations();
                        } else {
                            messageLabel.setText(username + " is not a registered user.");
                            messageLabel.getStyleClass().add("error");
                        }
                        messageLabel.setVisible(true);
                    }));
        });

        VBox addContactBox = new VBox(12, topRow, fieldBox, addButton);
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
