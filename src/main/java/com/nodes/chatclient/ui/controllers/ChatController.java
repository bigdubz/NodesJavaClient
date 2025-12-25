package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.store.model.ChatMessage;
import com.nodes.chatclient.store.model.ChatMessageUi;
import com.nodes.chatclient.ui.cells.MessageCell;
import com.nodes.chatclient.ui.vm.ChatViewModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;

import java.util.stream.IntStream;

public class ChatController {

    private Parent root;
    private ChatViewModel vm;
    private ScrollAnchor pendingAnchor;
    private ListView<ChatMessageUi> messages;
    private ListChangeListener<ChatMessageUi> messageListener;

    public ChatController() {}

    public void setVm(ChatViewModel vm) {
        if (this.vm != null) {
            this.vm.getMessages().removeListener(messageListener);
            this.vm.setHistoryListener(null);
        }

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
        messages.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (vm.isLoadingHistory() && e.getDeltaY() > 0) {
                e.consume();
            }
        });
        messageListener = c -> {
            if (!vm.isLoadingHistory()) {
                Platform.runLater(() -> {
                    int last = vm.getMessages().size() - 1;
                    if (last >= 0) {
                        messages.scrollTo(vm.getMessages().size() - 1);
                    }
                });
            }
            vm.markVisibleMessagesAsSeen();
        };

        vm.getMessages().addListener(messageListener);
        vm.setHistoryListener(() -> Platform.runLater(() -> restoreAnchor(pendingAnchor)));

        TextField input = new TextField();
        input.setPromptText("Send a message...");
        input.getStyleClass().add("input-field");

        Button send = new Button("Send");
        send.getStyleClass().add("button");

        input.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (e.isShiftDown()) return;

                e.consume();
                sendMessage(input);
            }
        });
        send.setOnAction(e -> sendMessage(input));

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
    private static VirtualFlow<ListCell<ChatMessage>> getVirtualFlow(ListView<ChatMessageUi> list) {
        return (VirtualFlow<ListCell<ChatMessage>>) list.lookup(".virtual-flow");
    }

    private ScrollAnchor captureAnchor() {
        VirtualFlow<?> flow = getVirtualFlow(messages);
        if (flow == null) return null;

        for (int i = 0; i < flow.getCellCount(); i++) {
            IndexedCell<?> cell = flow.getCell(i);
            if (cell != null && cell.isVisible()) {
                System.out.println("captured anchor");
                ChatMessageUi msg = (ChatMessageUi) cell.getItem();
                double offset = cell.getLayoutY();
                return new ScrollAnchor(msg.messageId, offset);
            }
        }

        return null;
    }

    private void restoreAnchor(ScrollAnchor anchor) {
        VirtualFlow<?> flow = getVirtualFlow(messages);
        if (anchor == null || flow == null || !vm.isLoadingHistory()) return;

        int index = IntStream.range(0,
                messages.getItems().size())
                .filter(i -> messages
                                .getItems()
                                .get(i)
                                .messageId.equals(anchor.message))
                .findFirst().orElse(-1);

        if (index < 0) return;
        messages.scrollTo(index);
        Platform.runLater(() -> {
            IndexedCell<?> cell = flow.getCell(index);
            if (cell != null) {
                double delta = cell.getLayoutY() - anchor.offsetY();
                flow.scrollPixels(delta);
                vm.setLoadingHistory(false);
            }
        });
    }

    public Parent getRoot() {
        return root;
    }

    private void installInfiniteScroll(ListView<ChatMessageUi> list) {
        list.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin == null) return;

            // Defer one more pulse so layout happens
            Platform.runLater(() -> {
                ScrollBar vBar = (ScrollBar) list.lookup(".scroll-bar:vertical");
                if (vBar == null) return;

                vBar.valueProperty().addListener((o, oldV, newV) -> {
                    if (newV.doubleValue() == 0) {
                        pendingAnchor = captureAnchor();
                        vm.loadOlderHistory();
                    }
                });
            });
        });
    }

    private void sendMessage(TextField input) {
        String text = input.getText().trim();
        if (!text.isEmpty()) {
            vm.sendMessage(text);
            input.clear();
        }
    }

    public void reset() {
        pendingAnchor = null;
    }

    private record ScrollAnchor(String message, double offsetY) {}
}
