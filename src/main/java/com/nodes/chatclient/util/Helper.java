package com.nodes.chatclient.util;

import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    public static void loadToBuffer(ByteBuffer buffer,
                             byte[] labelBytes,
                             byte[] channelBytes,
                             byte[] senderBytes,
                             byte[] senderDeviceBytes,
                             byte[] receiverBytes,
                             byte[] receiverDeviceBytes) {
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.putInt(labelBytes.length);
        buffer.put(labelBytes);

        buffer.putInt(channelBytes.length);
        buffer.put(channelBytes);

        buffer.putInt(senderBytes.length);
        buffer.put(senderBytes);

        buffer.putInt(senderDeviceBytes.length);
        buffer.put(senderDeviceBytes);

        buffer.putInt(receiverBytes.length);
        buffer.put(receiverBytes);

        buffer.putInt(receiverDeviceBytes.length);
        buffer.put(receiverDeviceBytes);
    }
}
