package com.nodes.chatclient.util;

import com.nodes.chatclient.App;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public final class EmojiRegistry {

    public static final Set<String> EMOJI_SEQUENCES = new HashSet<>();
    private static final Map<String, Image> EMOJI_CACHE = new HashMap<>();

    static {
        loadEmojis();
    }

    private static void loadEmojis() {
        try (InputStream is = EmojiRegistry.class
                .getResourceAsStream("/emojis/index.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))) {

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.endsWith(".png")) continue;
                EMOJI_SEQUENCES.add(line.replace(".png", ""));
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load emojis", e);
        }
    }

    public static boolean isEmoji(String text) {
        return EMOJI_SEQUENCES.contains(text);
    }


    public static ImageView createEmojiView(String key, double size) {
        Image img = EMOJI_CACHE.computeIfAbsent(key, k ->
                new Image(
                        Objects.requireNonNull(
                                App.class.getResourceAsStream("/emojis/" + k + ".png")
                        )
                )
        );

        ImageView view = new ImageView(img);
        view.setFitWidth(size);
        view.setFitHeight(size);

        view.setPreserveRatio(true);
//        view.setSmooth(true);

        return view;
    }

    public static List<Tokens.Token> textToTokens(String raw) {
        int[] cps = raw.codePoints().toArray();

        List<Tokens.Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < cps.length) {
            boolean matchedEmoji = false;

            for (int len = Math.min(8, cps.length - i); len >= 1; len--) {
                String key = toHexKey(cps, i, len);

                if (EmojiRegistry.isEmoji(key)) {
                    tokens.add(new Tokens.EmojiToken(key));
                    i += len;
                    matchedEmoji = true;
                    break;
                }
            }

            if (!matchedEmoji) {
                tokens.add(new Tokens.TextToken(new String(Character.toChars(cps[i]))));
                i++;
            }
        }
        return tokens;
    }

    private static String toHexKey(int[] cps, int start, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append("-");
            sb.append(Integer.toHexString(cps[start + i]));
        }
        return sb.toString();
    }

    public static String emojiToKey(String emoji) {
        return emoji.codePoints()
                .filter(cp -> cp != 0xFE0F)
                .mapToObj(Integer::toHexString)
                .collect(Collectors.joining("-"));
    }
}
