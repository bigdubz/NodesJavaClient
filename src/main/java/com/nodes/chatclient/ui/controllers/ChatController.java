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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

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
            cell.setMaxWidth(Double.MAX_VALUE);
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
    }

    public Parent getRoot() {
        return root;
    }

    private static class MessageCell extends ListCell<ChatMessage> {
        private final String selfId;

        MessageCell(String selfId) {
            this.selfId = selfId;
            getStyleClass().add("msg-cell");

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

            boolean fromMe = m.fromUserId.equals(selfId);

            HBox row = new HBox();
            row.getStyleClass().add("msg-row");
            row.setAlignment(Pos.TOP_LEFT);
            row.setFillHeight(false);
            row.setMaxWidth(Double.MAX_VALUE);

            // Message text
            TextArea text = new TextArea(m.text);
            text.setWrapText(true);
            text.setEditable(false);
            text.setFocusTraversable(false);
            text.setMouseTransparent(false);
            text.setPickOnBounds(false);

            text.setPrefRowCount(1);
            text.setMinHeight(Region.USE_PREF_SIZE);
            text.setMaxHeight(Region.USE_PREF_SIZE);

            text.getStyleClass().add("msg-text");
            text.getStyleClass().add(fromMe ? "msg-right" : "msg-left");

            text.maxWidthProperty().bind(
                    getListView().widthProperty().multiply(0.75)
            );

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (fromMe) {
                row.getChildren().addAll(spacer, text);
            } else {
                row.getChildren().addAll(text, spacer);
            }

            setGraphic(row);
        }
    }
}
