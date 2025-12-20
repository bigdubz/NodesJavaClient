package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.store.model.Conversation;
import com.nodes.chatclient.ui.vm.ConversationsViewModel;
import com.nodes.chatclient.util.TimeFormat;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public final class ConversationsController {

    private final Parent root;

    public ConversationsController(
            ConversationsViewModel vm,
            Consumer<String> onConversationSelected
    ) {
        ListView<Conversation> list = getConversationListView(vm);

        list.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldV, newV) -> {
                    if (newV != null) {
                        onConversationSelected.accept(newV.peerId);
                    }
                });

        Label title = new Label("Conversations");
        BorderPane.setMargin(title, new Insets(10));

        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("conversations-root");
        pane.setTop(title);
        pane.setCenter(list);

        this.root = pane;
    }

    private static ListView<Conversation> getConversationListView(ConversationsViewModel vm) {
        ListView<Conversation> list = new ListView<>();
        list.setItems(vm.getConversations());
        list.getStyleClass().add("conversations-list");

        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Conversation item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                boolean unread = item.unreadCount > 0;

                Label name = new Label(item.peerId);
                name.getStyleClass().add("conversation-name");
                if (unread) name.getStyleClass().add("unread");

                Region statusDot = new Region();
                statusDot.getStyleClass().addAll(
                        "status-dot",
                        item.isOnline ? "status-online" : "status-offline"
                );

                HBox nameRow = new HBox(6, name, statusDot);
                nameRow.setAlignment(Pos.CENTER_LEFT);

                Label preview = new Label(item.lastMessage != null ? item.lastMessage : "");
                preview.getStyleClass().add("conversation-preview");
                if (unread) preview.getStyleClass().add("unread");

                preview.setTextOverrun(OverrunStyle.ELLIPSIS);
                preview.setWrapText(false);

                Label time = new Label(TimeFormat.conversationTime(item.lastTimestamp));
                time.getStyleClass().add("conversation-time");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox messageRow = new HBox(6, preview, spacer, time);
                messageRow.setAlignment(Pos.CENTER_LEFT);

                VBox leftBox = new VBox(4, nameRow, messageRow);

                VBox rightBox = new VBox(6);
                rightBox.setAlignment(Pos.CENTER_RIGHT);

                if (unread) {
                    Label unreadLabel = new Label(String.valueOf(item.unreadCount));
                    unreadLabel.getStyleClass().add("unread-badge");
                    rightBox.getChildren().add(unreadLabel);
                }

                BorderPane row = new BorderPane();
                row.setLeft(leftBox);
                row.setRight(rightBox);
                row.getStyleClass().add("conversation-row");

                preview.maxWidthProperty().bind(
                        row.widthProperty().subtract(120)
                );

                setGraphic(row);
            }
        });

        return list;
    }

    public Parent getRoot() {
        return root;
    }
}
