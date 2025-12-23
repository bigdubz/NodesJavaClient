package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.store.model.ChatMessage;
import com.nodes.chatclient.ui.vm.ChatViewModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.*;

public class ChatController {

    private final Parent root;
    private final ChatViewModel vm;
    private int lastChange;
    private ScrollAnchor pendingAnchor;
    private final ListView<ChatMessage> messages;

    public ChatController(ChatViewModel vm) {
        this.vm = vm;
        Label title = new Label(vm.getPeerId());
        title.setPadding(new Insets(5));

        messages = new ListView<>();
        messages.setItems(vm.getMessages());
        messages.setCellFactory(lv -> {
            MessageCell cell = new MessageCell(vm.getPeerId());
            cell.prefWidthProperty().bind(lv.widthProperty());
            return cell;
        });
        installInfiniteScroll(messages);
        messages.setStyle("-fx-background-insets: 0;");
        vm.getMessages().addListener((ListChangeListener<ChatMessage>) change -> {
                change.next();
                if (change.getAddedSize() - lastChange == 1) {
                    messages.scrollTo(vm.getMessages().size());
                }
                lastChange = change.getAddedSize();
            }
        );
        vm.setHistoryListener(() -> Platform.runLater(() -> restoreAnchor(pendingAnchor)));

        TextField input = new TextField();
        input.setPromptText("Send a message...");
        input.getStyleClass().add("input-field");

        Button send = new Button("Send");
        send.getStyleClass().add("button");

        send.setOnAction(e -> {
            String text = input.getText().trim();
            if (!text.isEmpty()) {
                vm.sendMessage(text);
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

    @SuppressWarnings("unchecked")
    private static VirtualFlow<ListCell<ChatMessage>> getVirtualFlow(ListView<ChatMessage> list) {
        return (VirtualFlow<ListCell<ChatMessage>>) list.lookup(".virtual-flow");
    }

    private ScrollAnchor captureAnchor() {
        VirtualFlow<?> flow = getVirtualFlow(messages);
        if (flow == null) return null;

        for (int i = 0; i < flow.getCellCount(); i++) {
            IndexedCell<?> cell = flow.getCell(i);
            if (cell != null && cell.isVisible()) {
                ChatMessage msg = (ChatMessage) cell.getItem();
                double offset = cell.getLayoutY();
                return new ScrollAnchor(msg, offset);
            }
        }

        return null;
    }

    private void restoreAnchor(ScrollAnchor anchor) {
        if (anchor == null) return;
        VirtualFlow<?> flow = getVirtualFlow(messages);
        if (flow == null) return;

        int index = messages.getItems().indexOf(anchor.message);
        if (index < 0) return;

        messages.scrollTo(index);

        Platform.runLater(() -> {
            IndexedCell<?> cell = flow.getCell(index);
            if (cell == null) return;

            double delta = cell.getLayoutY() - anchor.offsetY();
            flow.scrollPixels(delta);
        });
    }

    public Parent getRoot() {
        return root;
    }

    private void installInfiniteScroll(ListView<ChatMessage> list) {
        list.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin == null) return;

            // Defer one more pulse so layout happens
            Platform.runLater(() -> {
                ScrollBar vBar = (ScrollBar) list.lookup(".scroll-bar:vertical");
                if (vBar == null) return;

                vBar.valueProperty().addListener((o, oldV, newV) -> {
                    if (newV.doubleValue() == 0) {
                        pendingAnchor = captureAnchor();
                        vm.loadOlderHistory(list);
                    }
                });
            });
        });
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

    private record ScrollAnchor(ChatMessage message, double offsetY) {}
}
