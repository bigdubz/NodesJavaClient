package com.nodes.chatclient.ui.cells;

import com.nodes.chatclient.store.model.ChatMessageUi;
import com.nodes.chatclient.util.EmojiRegistry;
import com.nodes.chatclient.util.Helper;
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


public class MessageCell extends ListCell<ChatMessageUi> {
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

        boolean fromMe = m.fromUserId.equals(selfId);

        MenuItem reply = new MenuItem("Reply");
        reply.setOnAction(e -> this.onReplyRequested.accept(m));

        boolean hasReactionFromMe = m.reactions.get(selfId) != null;
        MenuItem react = new MenuItem(hasReactionFromMe ? "Remove reaction" : "Add reaction");
        if (hasReactionFromMe) {
            react.setOnAction(e -> onRemoveReactionRequested.accept(m));
        } else {
            react.setOnAction(e -> onReactionRequested.accept(m, "❤️"));
        }

        MenuItem copy = new MenuItem("Copy text");
        copy.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(m.text);
            Clipboard.getSystemClipboard().setContent(cc);
        });

        contextMenu.getItems().addAll(reply, react, copy);

        Label username = new Label(m.fromUserId);
        username.getStyleClass().add("msg-username");

        TextFlow flow = Helper.textWithEmojiTextFlow(m.text, "msg-text");
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("msg-bubble");
        bubble.setMaxWidth(Region.USE_PREF_SIZE);
        bubble.getChildren().add(username);

        ChatMessageUi replied;
        if (m.replyingTo != null) {
            replied = getListView()
                    .getItems()
                    .stream()
                    .filter(repliedMsg -> repliedMsg.messageId.equals(m.replyingTo))
                    .findFirst()
                    .orElse(null);
            if (replied != null) {
                VBox replyBox = createReplyPreview(replied, fromMe);
                bubble.getChildren().add(replyBox);
            }
        }
        bubble.getChildren().add(flow);
        if (!m.reactions.isEmpty()) {
            HBox reactions = createReactions(m);
            bubble.getChildren().add(reactions);
        }

        HBox.setHgrow(bubble, Priority.NEVER);

        HBox row = new HBox(bubble);
        row.getStyleClass().add("msg-row");
        row.setFillHeight(false);
        row.setMaxWidth(Double.MAX_VALUE);

        if (fromMe) {
            flow.setTextAlignment(TextAlignment.RIGHT);
            bubble.setAlignment(Pos.CENTER_RIGHT);
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            flow.setTextAlignment(TextAlignment.LEFT);
            bubble.setAlignment(Pos.CENTER_LEFT);
            row.setAlignment(Pos.CENTER_LEFT);
        }

        setGraphic(row);
    }

    private VBox createReplyPreview(ChatMessageUi replied, boolean fromMe) {
        Label replyUser = new Label(replied.fromUserId);
        replyUser.getStyleClass().add("msg-reply-username");

        TextFlow flow = Helper.textWithEmojiTextFlow(replied.text, "msg-text");
        flow.getStyleClass().add("msg-reply-text");
        flow.setMaxWidth(500);
        flow.setMaxHeight(100);

        VBox box = new VBox(1, replyUser, flow);
        box.getStyleClass().add("msg-reply-box");
        if (fromMe) box.getStyleClass().add("me");
        return box;
    }

    private HBox createReactions(ChatMessageUi m) {
        HBox row = new HBox(6);
        row.getStyleClass().add("msg-reactions");

        m.reactions.forEach((userId, emoji) -> {
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
}
