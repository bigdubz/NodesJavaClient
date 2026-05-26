package com.nodes.chatclient.e2ee.provisioning;

import com.nodes.chatclient.e2ee.crypto.DoubleRatchet;
import com.nodes.chatclient.e2ee.db.stores.ContactStore;
import com.nodes.chatclient.e2ee.db.stores.SessionStore;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;
import com.nodes.chatclient.e2ee.types.InternalMessage;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.types.Session;

import java.util.Objects;

public final class MessageDecryptionService {

    private final LocalIdentity localIdentity;
    private final ContactStore contactStore;
    private final SessionStore sessionStore;

    public MessageDecryptionService(
            LocalIdentity localIdentity,
            ContactStore contactStore,
            SessionStore sessionStore
    ) {
        this.localIdentity = Objects.requireNonNull(localIdentity, "localIdentity");
        this.contactStore = Objects.requireNonNull(contactStore, "contactStore");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    }

    public InternalMessage decryptMessage(EncryptedMessage encryptedMessage) {
        try {
            if (!localIdentity.userId().equals(encryptedMessage.toUserId)
                    || !localIdentity.deviceId().equals(encryptedMessage.toDeviceId)) {
                throw new IllegalArgumentException("Encrypted message was not addressed to this device");
            }

            if (contactStore.get(encryptedMessage.fromUserId, encryptedMessage.fromDeviceId).isEmpty()) {
                throw new IllegalArgumentException("Encrypted message sender is not a known contact device");
            }

            Session session = sessionStore.load(encryptedMessage.fromUserId, encryptedMessage.fromDeviceId)
                    .orElseThrow(() -> new IllegalStateException(
                            "No session found for " + encryptedMessage.fromUserId + ":" + encryptedMessage.fromDeviceId
                    ));

            InternalMessage decryptedMessage = DoubleRatchet.decrypt(session, encryptedMessage);
            sessionStore.save(encryptedMessage.fromUserId, encryptedMessage.fromDeviceId, session);

            return decryptedMessage;

        } catch (Exception e) {
            System.err.println("Failed to decrypt message: " + e.getMessage());
            return null;
        }
    }
}
