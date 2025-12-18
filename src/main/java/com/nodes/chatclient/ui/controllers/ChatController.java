package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.model.ChatMessage;
import com.nodes.chatclient.ui.vm.ChatViewModel;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ChatController {

    private final Parent root;
    private final ChatViewModel vm;

    public ChatController(AppContext ctx, ChatViewModel vm) {
        this.vm = vm;
        Label title = new Label(vm.getPeerId());
        title.setPadding(new Insets(5));

        ListView<ChatMessage> messages = new ListView<>();
        messages.setItems(vm.getMessages());
        messages.setCellFactory(lv -> new MessageCell(vm.getPeerId()));

        TextField input = new TextField();
        input.setPromptText("Send a message...");

        Button send = new Button("Send");

        send.setOnAction(e -> {
            String text = input.getText().trim();
            if (!text.isEmpty()) {
                vm.sendMessage(text, ctx);
                input.clear();
            }
        });

        HBox bottom = new HBox(10, input, send);
        bottom.setPadding(new Insets(10));
        HBox.setHgrow(input, Priority.ALWAYS);

        BorderPane pane = new BorderPane();
        pane.setTop(title);
        pane.setCenter(messages);
        pane.setBottom(bottom);

        this.root = pane;
    }

    public Parent getRoot() {
        return root;
    }

    private static class MessageCell extends ListCell<ChatMessage> {
        private final String peerId;

        MessageCell(String peerId) {
            this.peerId = peerId;
        }

        @Override
        protected void updateItem(ChatMessage item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            boolean isIncoming = !item.fromUserId.equals(peerId);
            String display = (isIncoming ? "< " : "> ") + item.text;
            setText(display);
        }
    }
}
