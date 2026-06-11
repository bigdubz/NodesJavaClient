package com.nodes.chatclient.util;

import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public final class Helper {

    public static TextFlow textWithEmojiTextFlow(String raw, String textClass) {
        TextFlow flow = new TextFlow();
        flow.setLineSpacing(2);

        StringBuilder buffer = new StringBuilder();

        for (Tokens.Token token : EmojiRegistry.textToTokens(raw)) {
            if (token instanceof Tokens.TextToken(String text)) {
                buffer.append(text);
            } else {
                if (!buffer.isEmpty()) {
                    Text text = new Text(buffer.toString());
                    text.getStyleClass().add(textClass);
                    flow.getChildren().add(text);
                    buffer.setLength(0);
                }
                double fontSize = 20;
                ImageView emoji = EmojiRegistry.createEmojiView(((Tokens.EmojiToken) token).key(), fontSize);
                flow.getChildren().add(emoji);
            }
        }

        if (!buffer.isEmpty()) {
            Text text = new Text(buffer.toString());
            text.getStyleClass().add(textClass);
            flow.getChildren().add(text);
            buffer.setLength(0);
        }
        return flow;
    }

    public static String controlMessageId() {
        return "java-control-" + System.nanoTime();
    }

    public static String mainMessageId() {
        return "java-main-" + System.nanoTime();
    }
}
