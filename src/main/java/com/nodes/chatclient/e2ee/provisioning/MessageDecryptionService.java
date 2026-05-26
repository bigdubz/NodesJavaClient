package com.nodes.chatclient.e2ee.provisioning;

import com.nodes.chatclient.e2ee.crypto.DoubleRatchet;
import com.nodes.chatclient.e2ee.crypto.KeyMaterial;
import com.nodes.chatclient.e2ee.db.stores.ContactStore;
import com.nodes.chatclient.e2ee.db.stores.OneTimePrekeyStore;
import com.nodes.chatclient.e2ee.db.stores.SessionStore;
import com.nodes.chatclient.e2ee.db.stores.SignedPrekeyStore;
import com.nodes.chatclient.e2ee.protos.ProtoSession;
import com.nodes.chatclient.e2ee.records.ContactRecord;
import com.nodes.chatclient.e2ee.records.OneTimePrekeyRecord;
import com.nodes.chatclient.e2ee.records.SignedPrekeyRecord;
import com.nodes.chatclient.e2ee.types.EncryptedMessage;
import com.nodes.chatclient.e2ee.types.InternalMessage;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.types.Session;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class MessageDecryptionService {

    private final LocalIdentity localIdentity;
    private final ContactStore contactStore;
    private final SessionStore sessionStore;
    private final SignedPrekeyStore signedPrekeyStore;
    private final OneTimePrekeyStore oneTimePrekeyStore;

    public MessageDecryptionService(
            LocalIdentity localIdentity,
            ContactStore contactStore,
            SessionStore sessionStore,
            SignedPrekeyStore signedPrekeyStore,
            OneTimePrekeyStore oneTimePrekeyStore
    ) {
        this.localIdentity = Objects.requireNonNull(localIdentity, "localIdentity");
        this.contactStore = Objects.requireNonNull(contactStore, "contactStore");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.signedPrekeyStore = signedPrekeyStore;
        this.oneTimePrekeyStore = oneTimePrekeyStore;
    }

    public InternalMessage decryptMessage(EncryptedMessage encryptedMessage) {
        try {
            if (!localIdentity.userId().equals(encryptedMessage.toUserId)
                    || !localIdentity.deviceId().equals(encryptedMessage.toDeviceId)) {
                throw new IllegalArgumentException("Encrypted message was not addressed to this device");
            }

            Optional<ContactRecord> contactRecord = contactStore.get(
                    encryptedMessage.fromUserId,
                    encryptedMessage.fromDeviceId
            );
            ContactRecord contact = contactRecord.isPresent()
                    ? contactRecord.get()
                    : saveTestingContact(encryptedMessage);

            Session session = sessionStore.load(encryptedMessage.fromUserId, encryptedMessage.fromDeviceId)
                    .orElseGet(() -> initializeResponderSession(encryptedMessage, contact));

            InternalMessage decryptedMessage = DoubleRatchet.decrypt(session, encryptedMessage);
            sessionStore.save(encryptedMessage.fromUserId, encryptedMessage.fromDeviceId, session);
            if (encryptedMessage.oneTimePrekeyId != null && oneTimePrekeyStore != null) {
                oneTimePrekeyStore.markUsed(encryptedMessage.oneTimePrekeyId);
            }

            return decryptedMessage;

        } catch (Exception e) {
            System.err.println("Failed to decrypt message: " + e.getMessage());
            return null;
        }
    }

    private Session initializeResponderSession(EncryptedMessage encryptedMessage, ContactRecord contact) {
        try {
            if (signedPrekeyStore == null) {
                throw new IllegalStateException("No session found and signed prekey store is unavailable");
            }
            if (encryptedMessage.senderIdentityKey != null
                    && !Arrays.equals(encryptedMessage.senderIdentityKey, contact.identityKey())) {
                throw new IllegalArgumentException("Encrypted message sender identity does not match pinned contact");
            }

            SignedPrekeyRecord signedPrekey = signedPrekeyStore.getActive()
                    .orElseThrow(() -> new IllegalStateException("No active signed prekey for responder session"));

            byte[] senderIdentityKey = encryptedMessage.senderIdentityKey != null
                    ? encryptedMessage.senderIdentityKey
                    : contact.identityKey();

            byte[] dh1 = KeyMaterial.dh(signedPrekey.privateKey(), senderIdentityKey);
            byte[] dh2 = KeyMaterial.dh(localIdentity.identityPrivateKey(), encryptedMessage.dhPublicKey);
            byte[] dh3 = KeyMaterial.dh(signedPrekey.privateKey(), encryptedMessage.dhPublicKey);
            byte[] secret = KeyMaterial.concat(KeyMaterial.concat(dh1, dh2), dh3);

            if (encryptedMessage.oneTimePrekeyId != null) {
                if (oneTimePrekeyStore == null) {
                    throw new IllegalStateException("One-time prekey id present but store is unavailable");
                }
                OneTimePrekeyRecord oneTimePrekey = oneTimePrekeyStore.getById(encryptedMessage.oneTimePrekeyId)
                        .orElseThrow(() -> new IllegalStateException(
                                "No one-time prekey for id " + encryptedMessage.oneTimePrekeyId
                        ));
                if (oneTimePrekey.isUsed()) {
                    throw new IllegalStateException("One-time prekey with id " + oneTimePrekey.keyId() +
                            " is already used.");
                }
                byte[] dh4 = KeyMaterial.dh(oneTimePrekey.privateKey(), encryptedMessage.dhPublicKey);
                secret = KeyMaterial.concat(secret, dh4);
            }

            Session session = Session.createInitial(
                    KeyMaterial.hash(secret),
                    signedPrekey.privateKey(),
                    signedPrekey.publicKey(),
                    null,
                    localIdentity.deviceId(),
                    encryptedMessage.fromDeviceId,
                    false
            );
            session.state = ProtoSession.SessionProto.State.ACTIVE;
            session.sessionVersion = 1;
            session.signingPrivateKey = localIdentity.signingPrivateKey();
            session.signingPublicKey = localIdentity.signingPublicKey();
            session.remoteSigningPublicKey = contact.signingKey();

            return session;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to initialize responder session for "
                            + encryptedMessage.fromUserId + ":" + encryptedMessage.fromDeviceId,
                    e
            );
        }
    }

    private ContactRecord saveTestingContact(EncryptedMessage encryptedMessage) throws Exception {
        if (encryptedMessage.senderIdentityKey == null || encryptedMessage.senderSigningKey == null) {
            throw new IllegalArgumentException(
                    "Encrypted message sender is not a known contact device and did not include contact keys"
            );
        }

        ContactRecord contact = new ContactRecord(
                encryptedMessage.fromUserId,
                encryptedMessage.fromDeviceId,
                encryptedMessage.senderIdentityKey,
                encryptedMessage.senderSigningKey
        );
        contactStore.save(contact);
        System.err.println(
                "Auto-added unknown contact for testing: "
                        + encryptedMessage.fromUserId + ":" + encryptedMessage.fromDeviceId
        );
        return contact;
    }
}
