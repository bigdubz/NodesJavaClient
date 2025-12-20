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
        vm.getMessages().addListener((ListChangeListener<ChatMessage>) c -> {
            Platform.runLater(() -> messages.scrollTo(vm.getMessages().size() - 1));
        });

        TextField input = new TextField();
        input.setPromptText("Send a message...");
        input.getStyleClass().add("chat-input");

        Button send = new Button("Send");
        send.getStyleClass().add("chat-send");

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

            HBox box = new HBox();
            box.setFillHeight(false);
            box.setMaxWidth(Double.MAX_VALUE);
            box.setPrefWidth(Region.USE_COMPUTED_SIZE);
            box.getStyleClass().add("msg-row");

            boolean fromMe = !m.fromUserId.equals(selfId);

            if (fromMe) {
                box.setAlignment(Pos.CENTER_RIGHT);
                box.getStyleClass().add("msg-right");
            } else {
                box.setAlignment(Pos.CENTER_LEFT);
                box.getStyleClass().add("msg-left");
            }

            Label text = new Label(m.text);
            text.getStyleClass().add("msg-text");
            text.setWrapText(true);

            text.maxWidthProperty().bind(
                    getListView().widthProperty()
                            .multiply(0.80)
                            .subtract(30) // padding
            );

            box.getChildren().add(text);

            HBox.setHgrow(box, Priority.ALWAYS);
            setGraphic(box);
        }
    }
}
