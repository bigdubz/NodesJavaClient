package com.nodes.chatclient.e2ee.provisioning;

import com.nodes.chatclient.e2ee.records.ContactRecord;
import com.nodes.chatclient.e2ee.stores.ContactStore;
import com.nodes.chatclient.http.api.ContactsApi;
import com.nodes.chatclient.http.dto.Contact;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


public final class ContactProvisioningService {

    private final ContactsApi api;
    private final ContactStore store;

    public ContactProvisioningService(
            ContactsApi contactsApi,
            ContactStore contactStore
    ) {
        this.api = Objects.requireNonNull(contactsApi, "contactsApi");
        this.store = Objects.requireNonNull(contactStore, "contactStore");
    }

    public CompletableFuture<Boolean> addContact(String jwt, String userId) {
        return api.downloadContactAsync(jwt, userId)
                .thenApply(contacts -> {
                    if (contacts == null || contacts.payload() == null || contacts.payload().length == 0) {
                        return false;
                    }

                    return persistContacts(contacts.payload());
                })
                .exceptionally(throwable -> {
                    System.out.println("Failed to download contact: " + throwable.getMessage());
                    return false;
                });
    }

    public boolean deleteContact(String conversationId) {
        try {
            store.deleteUser(conversationId);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean persistContacts(Contact[] contacts) {
        List<ContactRecord> contactList = new ArrayList<>();
        for (Contact contact : contacts) {
            if (contact == null || contact.userId() == null || contact.deviceId() == null) {
                continue;
            }

            contactList.add(ContactRecord.from(contact));
        }

        if (contactList.isEmpty()) {
            return false;
        }

        try {
            store.saveAll(contactList);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist contacts", e);
        }
    }
}
