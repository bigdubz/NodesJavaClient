package com.nodes.chatclient.e2ee.provisioning;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.e2ee.protos.ProtoEncryptedPayload.EncryptedPayload;
import com.nodes.chatclient.e2ee.utils.ThrowingRunnable;

import java.util.List;

public final class MessageTransportService {

    public static void sendRemoveReaction(AppContext ctx,
                                          String peerId,
                                          String messageId,
                                          String referencedMessageId,
                                          long createdAt) {
        withProvisionedSession(ctx, peerId, () ->
                sendEncrypted(
                        ctx,
                        ctx.messageEncryptionService.encryptReactionForUser(
                                peerId,
                                messageId,
                                createdAt,
                                referencedMessageId,
                                "del",
                                true
                        )
                )
        );
    }

    public static void sendAddReaction(AppContext ctx,
                                       String peerId,
                                       String messageId,
                                       String referencedMessageId,
                                       String emoji,
                                       long createdAt) {
        withProvisionedSession(ctx, peerId, () ->
                sendEncrypted(
                        ctx,
                        ctx.messageEncryptionService.encryptReactionForUser(
                                peerId,
                                messageId,
                                createdAt,
                                referencedMessageId,
                                emoji,
                                false
                        )
                )
        );
    }

    public static void sendText(AppContext ctx,
                                String peerId,
                                String clientId,
                                String text,
                                String replyingTo,
                                long createdAt) {

        withProvisionedSession(ctx, peerId, () ->
                sendEncrypted(
                        ctx,
                        ctx.messageEncryptionService.encryptTextForUser(
                                peerId,
                                clientId,
                                createdAt,
                                text,
                                replyingTo
                        )
                )
        );
    }

    public static void sendDeliveredReceipt(AppContext ctx,
                                            String peerId,
                                            String messageId,
                                            String referencedMessageId,
                                            long createdAt) {
        sendControl(ctx, peerId, messageId, createdAt, EncryptedPayload.ControlMessage.Type.ACK, referencedMessageId);
    }

    public static void sendReadReceipt(AppContext ctx,
                                       String peerId,
                                       String messageId,
                                       String referencedMessageId,
                                       long createdAt) {
        sendControl(ctx, peerId, messageId, createdAt, EncryptedPayload.ControlMessage.Type.READ_RECEIPT, referencedMessageId);
    }

    public static void sendTyping(AppContext ctx,
                                  String peerId,
                                  String messageId,
                                  long createdAt) {
        sendControl(ctx, peerId, messageId, createdAt, EncryptedPayload.ControlMessage.Type.TYPING, null);
    }

    private static void sendControl(AppContext ctx,
                                    String peerId,
                                    String messageId,
                                    long createdAt,
                                    EncryptedPayload.ControlMessage.Type controlType,
                                    String referencedMessageId) {
        withProvisionedSession(ctx, peerId, () ->
                sendEncrypted(
                        ctx,
                        ctx.messageEncryptionService.encryptControlForUser(
                                peerId,
                                messageId,
                                createdAt,
                                controlType,
                                referencedMessageId
                        )
                )
        );
    }

    private static void sendEncrypted(
            AppContext ctx,
            List<MessageEncryptionService.EncryptedSend> encryptedMessages
    ) {
        encryptedMessages.forEach(encrypted -> ctx.wsService.sendEncryptedAsync(
                encrypted.toUserId(),
                encrypted.toDeviceId(),
                encrypted.blob()
        ));
    }

    private static void withProvisionedSession(AppContext ctx, String peerId, ThrowingRunnable task) {
        ctx.sessionProvisioningService.ensureSessionsAsync(ctx.jwt, peerId)
                .thenAccept(ready -> {
                    if (!ready) {
                        System.err.println("Unable to establish session with " + peerId);
                        return;
                    }

                    try {
                        task.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .exceptionally(err -> {
                    System.err.println(
                            "Failed to send to " + peerId + ": " + err.getMessage()
                    );
                    return null;
                });
    }
}
