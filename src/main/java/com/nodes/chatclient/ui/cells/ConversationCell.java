package com.nodes.chatclient.ui.cells;

import com.nodes.chatclient.store.model.ConversationUi;
import com.nodes.chatclient.util.TimeUtils;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public final class ConversationCell extends ListCell<ConversationUi> {

    private final ContextMenu contextMenu = new ContextMenu();
    private final Consumer<ConversationUi> onConversationDeleteRequested;

    public ConversationCell(Consumer<ConversationUi> onConversationDeleteRequested) {
        this.onConversationDeleteRequested = onConversationDeleteRequested;

        contextMenu.getStyleClass().add("msg-context-menu");
        setContextMenu(contextMenu);
    }

    @Override
    protected void updateItem(ConversationUi item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            return;
        } else {
            contextMenu.getItems().clear();
            setContextMenu(contextMenu);
        }

        MenuItem markRead = new MenuItem("Mark as read");
        MenuItem delete = new MenuItem("Delete contact and chat history");
        delete.setOnAction(e -> onConversationDeleteRequested.accept(item));
        contextMenu.getItems().addAll(markRead, delete);

        boolean unread = item.unreadCount() > 0;

        Label name = new Label(item.peerId());
        name.getStyleClass().add("conversation-name");
        if (unread) name.getStyleClass().add("unread");

        Region statusDot = new Region();
        statusDot.getStyleClass().addAll(
                "status-dot",
                item.isOnline() ? "status-online" : "status-offline"
        );

        HBox nameRow = new HBox(6, name, statusDot);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label preview = new Label(item.lastMessage() != null ? item.lastMessage() : "");
        preview.getStyleClass().add("conversation-preview");
        if (unread) preview.getStyleClass().add("unread");

        preview.setTextOverrun(OverrunStyle.ELLIPSIS);
        preview.setWrapText(false);

        Label time = new Label(TimeUtils.longToFormatted(item.lastTimestamp(), false));
        time.getStyleClass().add("conversation-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox messageRow = new HBox(6, preview, spacer, time);
        messageRow.setAlignment(Pos.CENTER_LEFT);

        VBox leftBox = new VBox(4, nameRow, messageRow);

        VBox rightBox = new VBox(6);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        if (unread) {
            Label unreadLabel = new Label(String.valueOf(item.unreadCount()));
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
