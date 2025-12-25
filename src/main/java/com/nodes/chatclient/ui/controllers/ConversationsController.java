package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.store.model.ConversationUi;
import com.nodes.chatclient.ui.cells.ConversationCell;
import com.nodes.chatclient.ui.vm.ConversationsViewModel;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public final class ConversationsController {

    private final Parent root;

    public ConversationsController(
            ConversationsViewModel vm,
            Consumer<String> onConversationSelected
    ) {
        ListView<ConversationUi> list = new ListView<>();
        list.setItems(vm.getConversations());
        list.getStyleClass().add("conversations-list");
        list.setCellFactory(lv -> new ConversationCell());

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

    public Parent getRoot() {
        return root;
    }
}
