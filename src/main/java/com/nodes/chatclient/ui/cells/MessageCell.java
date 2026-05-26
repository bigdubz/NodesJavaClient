package com.nodes.chatclient.ui.cells;

import com.nodes.chatclient.store.model.ChatMessageUi;
import com.nodes.chatclient.util.EmojiRegistry;
import com.nodes.chatclient.util.Helper;
import com.nodes.chatclient.util.TimeUtils;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.function.BiConsumer;
import java.util.function.Consumer;


public final class MessageCell extends ListCell<ChatMessageUi> {
    private final String selfId;
    private final ContextMenu contextMenu = new ContextMenu();
    private final Consumer<ChatMessageUi> onReplyRequested;
    private final BiConsumer<ChatMessageUi, String> onReactionRequested;
    private final Consumer<ChatMessageUi> onRemoveReactionRequested;


    public MessageCell(
            String selfId,
            Consumer<ChatMessageUi> onReplyRequested,
            BiConsumer<ChatMessageUi, String> onReactionRequested,
            Consumer<ChatMessageUi> onRemoveReactionRequested
    ) {
        this.selfId = selfId;
        this.onReplyRequested = onReplyRequested;
        this.onReactionRequested = onReactionRequested;
        this.onRemoveReactionRequested = onRemoveReactionRequested;
        getStyleClass().add("msg-cell");
        setPrefWidth(0);
        setMaxWidth(Double.MAX_VALUE);

        contextMenu.getStyleClass().add("msg-context-menu");
        setContextMenu(contextMenu);

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && getItem() != null) {
                onReplyRequested.accept(getItem());
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
            setContextMenu(null);
            return;
        } else {
            contextMenu.getItems().clear();
            setContextMenu(contextMenu);
        }

        boolean fromMe = m.fromUserId().equals(selfId);
        boolean shouldBundle = false;
        boolean shouldShowDateSeparator = false;
        boolean hasReactionFromMe = m.reactions().get(selfId) != null;

        if (getIndex() - 1 >= 0) {
            ChatMessageUi mOld = getListView().getItems().get(getIndex() - 1);
            shouldBundle = TimeUtils.getShouldBundle(mOld, m);
            shouldShowDateSeparator = TimeUtils.getShouldShowDateSeparator(mOld, m);
        }

        // context menu
        MenuItem reply = new MenuItem("Reply");
        reply.setOnAction(e -> this.onReplyRequested.accept(m));

        MenuItem react = new MenuItem(hasReactionFromMe ? "Remove reaction" : "Add reaction");
        if (hasReactionFromMe) {
            react.setOnAction(e -> onRemoveReactionRequested.accept(m));
        } else {
            react.setOnAction(e -> onReactionRequested.accept(m, "❤️"));
        }

        MenuItem copy = new MenuItem("Copy text");
        copy.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(m.text());
            Clipboard.getSystemClipboard().setContent(cc);
        });

        contextMenu.getItems().addAll(reply, react, copy);

        VBox row = new VBox(10);

        // message header
        HBox bubbleHeader = new HBox(5);
        Label username = new Label(m.fromUserId());
        username.getStyleClass().add("msg-username");
        Label timestamp = new Label(TimeUtils.longToFormatted(m.createdAt(), true));
        timestamp.getStyleClass().add("msg-time");

        // message content
        TextFlow flow = Helper.textWithEmojiTextFlow(m.text(), "msg-text");
        VBox bubble = new VBox(1);
        bubble.getStyleClass().add("msg-bubble");
        bubble.setMaxWidth(Region.USE_PREF_SIZE);

        if (shouldShowDateSeparator) {
            HBox separator = createDateSeparator(TimeUtils.getDate(m.createdAt()));
            row.getChildren().add(separator);
        }

        if (!shouldBundle) {
            bubble.getChildren().add(bubbleHeader);
        }

        // reply box
        ChatMessageUi replied;
        if (m.replyingTo() != null) {
            replied = getListView()
                    .getItems()
                    .stream()
                    .filter(repliedMsg -> repliedMsg.messageId().equals(m.replyingTo()))
                    .findFirst()
                    .orElse(null);
            VBox replyBox;
            if (replied != null) {
                replyBox = createReplyPreview(replied.text(), replied.fromUserId(), fromMe);
            } else {
                replyBox = createReplyPreview("Message not loaded", "Unknown", fromMe);
            }
            bubble.getChildren().add(replyBox);
        }
        bubble.getChildren().add(flow);

        // reaction box
        if (!m.reactions().isEmpty()) {
            HBox reactions = createReactions(m);
            bubble.getChildren().add(reactions);
        }

        HBox.setHgrow(bubble, Priority.NEVER);
        row.getChildren().add(bubble);
        row.getStyleClass().add("msg-row");
        row.setMaxWidth(Double.MAX_VALUE);

        if (fromMe) {
            bubbleHeader.getChildren().add(timestamp);
            bubbleHeader.getChildren().add(username);
            flow.setTextAlignment(TextAlignment.RIGHT);
            bubble.setAlignment(Pos.CENTER_RIGHT);
            row.setAlignment(Pos.CENTER_RIGHT);
            bubbleHeader.setAlignment(Pos.CENTER_RIGHT);
            // if is last message, from me, and is read, show read receipt
            if (getIndex() == getListView().getItems().size() - 1 && m.read()) {
                Label seen = new Label("Seen");
                seen.getStyleClass().add("msg-seen");
                bubble.getChildren().add(seen);
            }
        } else {
            bubbleHeader.getChildren().add(username);
            bubbleHeader.getChildren().add(timestamp);
            flow.setTextAlignment(TextAlignment.LEFT);
            bubble.setAlignment(Pos.CENTER_LEFT);
            row.setAlignment(Pos.CENTER_LEFT);
        }

        setGraphic(row);
    }

    private VBox createReplyPreview(String text, String fromUserId, boolean fromMe) {
        Label replyUser = new Label(fromUserId);
        replyUser.getStyleClass().add("msg-reply-username");

        TextFlow flow = Helper.textWithEmojiTextFlow(text, "msg-text");
        flow.getStyleClass().add("msg-reply-text");
        flow.setMaxWidth(500);
        flow.setMaxHeight(100);

        VBox box = new VBox(1, replyUser, flow);
        box.getStyleClass().add("msg-reply-box");
        box.setMinWidth(100);
        box.setMaxWidth(500);
        if (fromMe) box.getStyleClass().add("me");
        return box;
    }

    private HBox createReactions(ChatMessageUi m) {
        HBox row = new HBox(6);
        row.getStyleClass().add("msg-reactions");

        m.reactions().forEach((userId, emoji) -> {
            String key = EmojiRegistry.emojiToKey(emoji);
            double size = 13;
            ImageView emojiImage = EmojiRegistry.createEmojiView(key, size);
            Label userLabel = new Label(userId);
            userLabel.getStyleClass().add("msg-reaction-count");

            HBox pill = new HBox(4, userLabel, emojiImage);
            pill.getStyleClass().add("msg-reaction");

            row.getChildren().add(pill);
        });

        return row;
    }

    private HBox createDateSeparator(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("msg-date-separator");

        Region leftLine = new Region();
        leftLine.getStyleClass().add("msg-date-line");
        HBox.setHgrow(leftLine, Priority.ALWAYS);

        Region rightLine = new Region();
        rightLine.getStyleClass().add("msg-date-line");
        HBox.setHgrow(rightLine, Priority.ALWAYS);

        HBox box = new HBox(10, leftLine, label, rightLine);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
}
