package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.store.model.Conversation;
import com.nodes.chatclient.ui.vm.ConversationsViewModel;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

import java.util.function.Consumer;

public final class ConversationsController {

    private final Parent root;

    public ConversationsController(
            ConversationsViewModel vm,
            Consumer<String> onConversationSelected
    ) {
        ListView<Conversation> list = new ListView<>();
        list.setItems(vm.getConversations());

        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Conversation item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                setText(
                        item.peerId
                                + "   "
                                + item.lastMessage
                                + "   "
                                + (item.unreadCount > 0 ? "(" + item.unreadCount + ")" : "")
                                + "   "
                                + (item.isOnline ? "●" : "○")
                );
            }
        });

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
        pane.setTop(title);
        pane.setCenter(list);

        this.root = pane;
    }

    public Parent getRoot() {
        return root;
    }
}
