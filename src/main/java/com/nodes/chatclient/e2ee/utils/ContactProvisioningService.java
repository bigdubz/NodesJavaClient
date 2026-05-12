package com.nodes.chatclient.e2ee.utils;

import com.nodes.chatclient.e2ee.records.ContactRecord;
import com.nodes.chatclient.e2ee.stores.ContactStore;
import com.nodes.chatclient.http.api.ContactsApi;
import com.nodes.chatclient.http.dto.Contact;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


public class ContactProvisioningService {

    private final ContactsApi contactsApi;
    private final ContactStore contactStore;

    public ContactProvisioningService(
            ContactsApi contactsApi,
            ContactStore contactStore
    ) {
        this.contactsApi = Objects.requireNonNull(contactsApi, "contactsApi");
        this.contactStore = Objects.requireNonNull(contactStore, "contactStore");
    }

    public CompletableFuture<Boolean> addContact(String jwt, String userId) {
        return contactsApi.downloadContactAsync(jwt, userId)
                .thenApply(bundles -> {
                    if (bundles == null || bundles.payload() == null || bundles.payload().length == 0) {
                        return false;
                    }

                    return persistContacts(bundles.payload());
                })
                .exceptionally(throwable -> {
                    System.out.println("Failed to download contact: " + throwable.getMessage());
                    return false;
                });
    }

    public boolean deleteContact(String conversationId) {
        try {
            contactStore.deleteUser(conversationId);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean persistContacts(Contact[] bundles) {
        List<ContactRecord> contacts = new ArrayList<>();
        for (Contact contact : bundles) {
            if (contact == null || contact.userId() == null || contact.deviceId() == null) {
                continue;
            }

            contacts.add(ContactRecord.from(contact));
        }

        if (contacts.isEmpty()) {
            return false;
        }

        try {
            contactStore.saveAll(contacts);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist contacts", e);
        }
    }
}
