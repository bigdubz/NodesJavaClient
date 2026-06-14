package com.nodes.chatclient.e2ee.provisioning;

import com.nodes.chatclient.e2ee.crypto.DoubleRatchet;
import com.nodes.chatclient.e2ee.mappers.OuterPayloadMapper;
import com.nodes.chatclient.e2ee.protos.ProtoEncryptedPayload.EncryptedPayload;
import com.nodes.chatclient.e2ee.db.records.ContactRecord;
import com.nodes.chatclient.e2ee.db.stores.ContactStore;
import com.nodes.chatclient.e2ee.db.stores.SessionStore;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;
import com.nodes.chatclient.e2ee.types.InternalMessage;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.types.Session;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public final class MessageEncryptionService {

    private final LocalIdentity localIdentity;
    private final ContactStore contactStore;
    private final SessionStore sessionStore;

    public MessageEncryptionService(
            LocalIdentity localIdentity,
            ContactStore contactStore,
            SessionStore sessionStore
    ) {
        this.localIdentity = Objects.requireNonNull(localIdentity, "localIdentity");
        this.contactStore = Objects.requireNonNull(contactStore, "contactStore");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    }

    public List<EncryptedSend> encryptReactionForUser(
            String toUserId,
            String messageId,
            long createdAt,
            String referencedMessageId,
            String emoji,
            boolean isRemoved
    ) throws Exception {
        InternalMessage internalMessage = InternalMessage.reaction(
                messageId,
                createdAt,
                referencedMessageId,
                emoji,
                isRemoved
        );

        return encryptForUser(toUserId, internalMessage);
    }

    public List<EncryptedSend> encryptTextForUser(
            String toUserId,
            String messageId,
            long createdAt,
            String body,
            String referencedMessageId
    ) throws Exception {
        InternalMessage internalMessage = InternalMessage.text(
                messageId,
                createdAt,
                body,
                referencedMessageId
        );

        return encryptForUser(toUserId, internalMessage);
    }

    public List<EncryptedSend> encryptControlForUser(
            String toUserId,
            String messageId,
            long createdAt,
            EncryptedPayload.ControlMessage.Type controlType,
            String referencedMessageId
    ) throws Exception {
        InternalMessage internalMessage = InternalMessage.control(
                messageId,
                createdAt,
                controlType,
                referencedMessageId
        );

        return encryptForUser(toUserId, internalMessage);
    }

    private List<EncryptedSend> encryptForUser(String toUserId, InternalMessage internalMessage) throws Exception {
        List<ContactRecord> contacts = contactStore.getForUser(toUserId);
        if (contacts.isEmpty()) {
            throw new SQLException("No local contact devices for " + toUserId);
        }

        List<EncryptedSend> result = new ArrayList<>();

        for (ContactRecord contact : contacts) {
            Session session = sessionStore.load(contact.userId(), contact.deviceId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No session for " + contact.userId() + ":" + contact.deviceId()
                    ));
            boolean needsPrekeyMetadata = session.receivingChainKey == null;

            EncryptedMessage encrypted = DoubleRatchet.encrypt(
                    session,
                    internalMessage,
                    localIdentity.userId(),
                    contact.userId()
            );
            if (needsPrekeyMetadata) {
                encrypted.attachPrekeyMetadata(
                        localIdentity.identityPublicKey(),
                        localIdentity.signingPublicKey(),
                        session.oneTimePrekeyId
                );
                encrypted.sign(localIdentity.signingPrivateKey());
            }

            sessionStore.save(contact.userId(), contact.deviceId(), session);

            byte[] blob = OuterPayloadMapper.serialize(encrypted);
            result.add(new EncryptedSend(
                    contact.userId(),
                    contact.deviceId(),
                    Base64.getEncoder().encodeToString(blob)
            ));
        }

        return result;
    }

    public record EncryptedSend(String toUserId, String toDeviceId, String blob) {
    }
}
