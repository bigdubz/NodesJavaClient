package com.nodes.chatclient.ui.cells;

import com.nodes.chatclient.store.model.ChatMessageUi;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MessageCell extends ListCell<ChatMessageUi> {
    private final String toUserId;

    public MessageCell(String toUserId) {
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
    protected void updateItem(ChatMessageUi m, boolean empty) {
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

        VBox bubble = new VBox(2, username, text);
        bubble.getStyleClass().add("msg-bubble");
        bubble.setMaxWidth(Region.USE_PREF_SIZE);

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
}
