package com.nodes.chatclient.ui.cells;

import com.nodes.chatclient.store.model.ConversationUi;
import com.nodes.chatclient.util.TimeFormat;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.*;

public class ConversationCell extends ListCell<ConversationUi> {

    @Override
    protected void updateItem(ConversationUi item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        boolean unread = item.unreadCount > 0;

        Label name = new Label(item.peerId);
        name.getStyleClass().add("conversation-name");
        if (unread) name.getStyleClass().add("unread");

        Region statusDot = new Region();
        statusDot.getStyleClass().addAll(
                "status-dot",
                item.isOnline ? "status-online" : "status-offline"
        );

        HBox nameRow = new HBox(6, name, statusDot);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label preview = new Label(item.lastMessage != null ? item.lastMessage : "");
        preview.getStyleClass().add("conversation-preview");
        if (unread) preview.getStyleClass().add("unread");

        preview.setTextOverrun(OverrunStyle.ELLIPSIS);
        preview.setWrapText(false);

        Label time = new Label(TimeFormat.conversationTime(item.lastTimestamp));
        time.getStyleClass().add("conversation-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox messageRow = new HBox(6, preview, spacer, time);
        messageRow.setAlignment(Pos.CENTER_LEFT);

        VBox leftBox = new VBox(4, nameRow, messageRow);

        VBox rightBox = new VBox(6);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        if (unread) {
            Label unreadLabel = new Label(String.valueOf(item.unreadCount));
            unreadLabel.getStyleClass().add("unread-badge");
            rightBox.getChildren().add(unreadLabel);
        }

        BorderPane row = new BorderPane();
        row.setLeft(leftBox);
        row.setRight(rightBox);
        row.getStyleClass().add("conversation-row");

        preview.maxWidthProperty().bind(
                row.widthProperty().subtract(120)
        );

        setGraphic(row);
    }

}
