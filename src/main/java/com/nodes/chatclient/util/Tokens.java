package com.nodes.chatclient.util;


public class Tokens {
    public sealed interface Token permits TextToken, EmojiToken {}
    public record TextToken(String text) implements Token {}
    public record EmojiToken(String key) implements Token {}
}
