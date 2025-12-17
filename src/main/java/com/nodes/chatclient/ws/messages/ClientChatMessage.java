package com.nodes.chatclient.ws.messages;

public class ClientChatMessage implements ClientMessage {
    public final String type = "CHAT_MESSAGE";
    public final Payload payload;

    public ClientChatMessage(
            String toUserId,
            String text,
            String clientId,
            String replyingTo
    ) {
        this.payload = new ClientChatMessage.Payload(toUserId, text, clientId, replyingTo);
    }

    @Override
    public String type() {
        return "";
    }

    public static final class Payload {
        public final String toUserId;
        public final String text;
        public final String clientId;
        public final String replyingTo;

        public Payload(String toUserId, String text, String clientId, String replyingTo) {
            this.toUserId = toUserId;
            this.text = text;
            this.clientId = clientId;
            this.replyingTo = replyingTo;
        }
    }
}
