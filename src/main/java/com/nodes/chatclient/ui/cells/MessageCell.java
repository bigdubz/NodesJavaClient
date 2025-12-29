package com.nodes.chatclient.ui.cells;

import com.nodes.chatclient.store.model.ChatMessageUi;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class MessageCell extends ListCell<ChatMessageUi> {
    private final String toUserId;

    public MessageCell(String toUserId, Consumer<ChatMessageUi> onReplyRequested) {
        this.toUserId = toUserId;
        getStyleClass().add("msg-cell");
        setPrefWidth(0);
        setMaxWidth(Double.MAX_VALUE);

        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && getItem() != null) {
                onReplyRequested.accept(getItem());
            }
        });
    }

    @Override
    public void updateSelected(boolean selected) {
        // Do NOT call super.updateSelected -> prevents :selected CSS state,
        // but mouse events still fire normally.
    }

    @Override protected void updateItem(ChatMessageUi m, boolean empty) {
        super.updateItem(m, empty);

        if (empty || m == null) {
            setGraphic(null);
            return;
        }

        boolean fromMe = !m.fromUserId.equals(toUserId);

        Label username = new Label(m.fromUserId);
        username.getStyleClass().add("msg-username");

        Label text = new Label(m.text);
        text.setWrapText(true);
        text.getStyleClass().add("msg-text");

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("msg-bubble");
        bubble.setMaxWidth(Region.USE_PREF_SIZE);
        bubble.getChildren().add(username);

        ChatMessageUi replied;
        if (m.replyingTo != null) {
            replied = getListView()
                    .getItems()
                    .stream()
                    .filter(msg -> msg.messageId.equals(m.replyingTo))
                    .findFirst()
                    .orElse(null);
            if (replied != null) {
                VBox replyBox = createReplyPreview(replied, fromMe);
                bubble.getChildren().add(replyBox);
            }
        }
        bubble.getChildren().add(text);

        HBox.setHgrow(bubble, Priority.NEVER);

        HBox row = new HBox(bubble);
        row.getStyleClass().add("msg-row");
        row.setFillHeight(false);
        row.setMaxWidth(Double.MAX_VALUE);

        if (fromMe) {
            text.getStyleClass().add("msg-text-right");
            text.setAlignment(Pos.CENTER_RIGHT);
            bubble.setAlignment(Pos.CENTER_RIGHT);
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
        }

        setGraphic(row);
    }

    private VBox createReplyPreview(ChatMessageUi replied, boolean fromMe) {
        Label replyUser = new Label(replied.fromUserId);
        replyUser.getStyleClass().add("msg-reply-username");

        Label replyText = new Label(replied.text);
        replyText.setWrapText(true);
        replyText.setMaxWidth(300);
        replyText.setTextOverrun(OverrunStyle.ELLIPSIS);
        replyText.getStyleClass().add("msg-reply-text");
        replyText.setMaxHeight(100);

        VBox box = new VBox(1, replyUser, replyText);
        box.getStyleClass().add("msg-reply-box");
        if (fromMe) box.getStyleClass().add("me");
        return box;
    }
}
