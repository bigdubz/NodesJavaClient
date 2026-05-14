package com.nodes.chatclient.e2ee.provisioning;

import com.nodes.chatclient.e2ee.handshake.X3DHResult;
import com.nodes.chatclient.e2ee.handshake.X3DHService;
import com.nodes.chatclient.e2ee.records.ContactRecord;
import com.nodes.chatclient.e2ee.stores.ContactStore;
import com.nodes.chatclient.e2ee.stores.SessionStore;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.http.api.BundlesApi;
import com.nodes.chatclient.http.dto.RemoteUserBundle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class SessionProvisioningService {

    private final BundlesApi bundlesApi;
    private final LocalIdentity localIdentity;
    private final ContactStore contactStore;
    private final SessionStore sessionStore;
    private final X3DHService x3dhService;

    public SessionProvisioningService(
            BundlesApi bundlesApi,
            LocalIdentity localIdentity,
            ContactStore contactStore,
            SessionStore sessionStore
    ) {
        this(
                bundlesApi,
                localIdentity,
                contactStore,
                sessionStore,
                new X3DHService()
        );
    }

    public SessionProvisioningService(
            BundlesApi bundlesApi,
            LocalIdentity localIdentity,
            ContactStore contactStore,
            SessionStore sessionStore,
            X3DHService x3dhService
    ) {
        this.bundlesApi = Objects.requireNonNull(bundlesApi, "bundlesApi");
        this.localIdentity = Objects.requireNonNull(localIdentity, "localIdentity");
        this.contactStore = Objects.requireNonNull(contactStore, "contactStore");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.x3dhService = Objects.requireNonNull(x3dhService, "x3dhService");
    }

    public CompletableFuture<Boolean> ensureSessionsAsync(String jwt, String userId) {
        try {
            if (hasSessionForEveryKnownDevice(userId)) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (SQLException e) {
            return CompletableFuture.failedFuture(e);
        }

        return bundlesApi.downloadBundleAsync(jwt, userId)
                .thenApply(response -> {
                    if (response == null || response.payload() == null || response.payload().length == 0) {
                        return false;
                    }

                    try {
                        boolean ensuredAny = false;
                        for (RemoteUserBundle bundle : response.payload()) {
                            ensuredAny |= ensureSession(bundle);
                        }

                        return ensuredAny;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initiate session with " + userId, e);
                    }
                });
    }

    private boolean hasSessionForEveryKnownDevice(String userId) throws SQLException {
        boolean hasContact = false;

        for (ContactRecord contact : contactStore.getForUser(userId)) {
            hasContact = true;
            if (!sessionStore.exists(contact.userId(), contact.deviceId())) {
                return false;
            }
        }

        return hasContact;
    }

    private boolean ensureSession(RemoteUserBundle bundle) throws Exception {
        if (bundle == null || bundle.userId() == null || bundle.deviceId() == null) {
            return false;
        }

        Optional<ContactRecord> contact = contactStore.get(bundle.userId(), bundle.deviceId());
        if (contact.isEmpty() || !matchesPinnedIdentity(contact.get(), bundle)) {
            return false;
        }

        if (sessionStore.exists(bundle.userId(), bundle.deviceId())) {
            return true;
        }

        X3DHResult result = x3dhService.initiateHandshake(localIdentity, bundle);
        sessionStore.save(bundle.userId(), bundle.deviceId(), result.session());
        return true;
    }

    private boolean matchesPinnedIdentity(ContactRecord contact, RemoteUserBundle bundle) {
        return Arrays.equals(contact.identityKey(), bundle.ik())
                && Arrays.equals(contact.signingKey(), bundle.sk());
    }
}
