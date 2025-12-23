package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.model.ChatMessage;
import com.nodes.chatclient.ui.vm.ChatViewModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ChatController {

    private final Parent root;
    private final ChatViewModel vm;

    public ChatController(AppContext ctx, ChatViewModel vm) {
        this.vm = vm;
        Label title = new Label(vm.getPeerId());
        title.setPadding(new Insets(5));

        ListView<ChatMessage> messages = new ListView<>();
        messages.setItems(vm.getMessages());
        messages.setCellFactory(lv -> {
            MessageCell cell = new MessageCell(vm.getPeerId());
            cell.prefWidthProperty().bind(lv.widthProperty());
            return cell;
        });
        messages.setStyle("-fx-background-insets: 0;");
        vm.getMessages().addListener((ListChangeListener<ChatMessage>) c ->
                Platform.runLater(() ->
                        messages.scrollTo(vm.getMessages().size() - 1)
                )
        );

        TextField input = new TextField();
        input.setPromptText("Send a message...");
        input.getStyleClass().add("input-field");

        Button send = new Button("Send");
        send.getStyleClass().add("button");

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
        this.root.applyCss();
    }

    public Parent getRoot() {
        return root;
    }

    private static class MessageCell extends ListCell<ChatMessage> {
        private final String toUserId;

        MessageCell(String toUserId) {
            this.toUserId = toUserId;
            getStyleClass().add("msg-cell");
            setPrefWidth(0);
            setMaxWidth(Double.MAX_VALUE);

            setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && getItem() != null) {
                    // TODO: reply to this message
                }
            });
        }

        @Override
        public void updateSelected(boolean selected) {
            // Do NOT call super.updateSelected -> prevents :selected CSS state,
            // but mouse events still fire normally.
        }

        @Override
        protected void updateItem(ChatMessage m, boolean empty) {
            super.updateItem(m, empty);

            if (empty || m == null) {
                setGraphic(null);
                return;
            }

            boolean fromMe = !m.fromUserId.equals(toUserId);

            HBox row = new HBox();
            row.getStyleClass().add("msg-row");
            row.prefWidthProperty().bind(getListView().widthProperty().subtract(20));

            Label text = new Label(m.text);
            text.setWrapText(true);
            text.getStyleClass().add("msg-text");

            text.maxWidthProperty().bind(
                    getListView().widthProperty().multiply(0.75)
            );

            if (fromMe) {
                text.getStyleClass().add("msg-text-right");
                row.setAlignment(Pos.CENTER_RIGHT);
            } else {
                row.setAlignment(Pos.CENTER_LEFT);
            }

            row.getChildren().add(text);
            setGraphic(row);
        }
    }
}
