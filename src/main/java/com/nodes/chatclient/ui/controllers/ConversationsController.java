package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.store.model.ConversationUi;
import com.nodes.chatclient.ui.cells.ConversationCell;
import com.nodes.chatclient.ui.vm.ConversationsViewModel;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public final class ConversationsController {

    private final Parent root;
    private final ConversationsViewModel vm;
    private final ListView<ConversationUi> list;
    private final Consumer<String> onDeleteConversation;

    public ConversationsController(
            ConversationsViewModel vm,
            Consumer<String> onConversationSelected,
            Runnable onAddContact,
            Consumer<String> onDeleteConversation
    ) {
        this.vm = vm;
        this.onDeleteConversation = onDeleteConversation;
        list = new ListView<>();
        list.setItems(vm.getConversations());
        list.getStyleClass().add("conversations-list");
        list.setCellFactory(lv -> new ConversationCell(
                this::onDeleteConversation
        ));

        list.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                var selected = list.getSelectionModel().getSelectedItem();

                if (selected != null) {
                    onConversationSelected.accept(selected.peerId());
                }
            }
        });

        Label title = new Label("Conversations");
        title.setPadding(new Insets(10));
        BorderPane.setMargin(title, new Insets(10));
        Button btnAddContact = new Button("Add Contacts");
        btnAddContact.getStyleClass().add("button");
        btnAddContact.setOnAction(event -> onAddContact.run());
        HBox top = new HBox(20, title, btnAddContact);

        BorderPane pane = new BorderPane();
        pane.getStyleClass().add("conversations-root");
        pane.setTop(top);
        pane.setCenter(list);

        this.root = pane;
    }

    private void onDeleteConversation(ConversationUi conversation) {
        if (conversation != null) {
            onDeleteConversation.accept(conversation.peerId());
            vm.deleteContact(conversation.peerId());
        }
    }

    public Parent getRoot() {
        return root;
    }
}
