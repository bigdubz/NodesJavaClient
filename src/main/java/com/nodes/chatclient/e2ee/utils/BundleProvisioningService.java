package com.nodes.chatclient.e2ee.utils;

import com.nodes.chatclient.e2ee.records.ContactRecord;
import com.nodes.chatclient.e2ee.records.OneTimePrekeyRecord;
import com.nodes.chatclient.e2ee.records.SignedPrekeyRecord;
import com.nodes.chatclient.e2ee.stores.ContactStore;
import com.nodes.chatclient.e2ee.stores.OneTimePrekeyStore;
import com.nodes.chatclient.e2ee.stores.SignedPrekeyStore;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.types.LocalUserBundle;
import com.nodes.chatclient.e2ee.types.RemoteUserBundle;
import com.nodes.chatclient.http.api.BundlesApi;
import com.nodes.chatclient.http.dto.BundleStatusResponse;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class BundleProvisioningService {

    private static final int DEFAULT_ONE_TIME_PREKEY_TARGET = 100;
    private static final int DEFAULT_MAX_ONE_TIME_PREKEYS_PER_UPLOAD = 100;
    private static final int MAX_PREKEY_ID = 0x7fffffff;

    private final BundlesApi bundlesApi;
    private final LocalIdentity localIdentity;
    private final SignedPrekeyStore signedPrekeyStore;
    private final OneTimePrekeyStore oneTimePrekeyStore;
    private final ContactStore contactStore;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final int oneTimePrekeyTarget;
    private final int maxOneTimePrekeysPerUpload;

    public BundleProvisioningService(BundlesApi bundlesApi, LocalIdentity localIdentity) {
        this(
                bundlesApi,
                localIdentity,
                null,
                null,
                null,
                DEFAULT_ONE_TIME_PREKEY_TARGET,
                DEFAULT_MAX_ONE_TIME_PREKEYS_PER_UPLOAD,
                Clock.systemUTC(),
                new SecureRandom()
        );
    }

    public BundleProvisioningService(
            BundlesApi bundlesApi,
            LocalIdentity localIdentity,
            SignedPrekeyStore signedPrekeyStore,
            OneTimePrekeyStore oneTimePrekeyStore
    ) {
        this(
                bundlesApi,
                localIdentity,
                signedPrekeyStore,
                oneTimePrekeyStore,
                null
        );
    }

    public BundleProvisioningService(
            BundlesApi bundlesApi,
            LocalIdentity localIdentity,
            SignedPrekeyStore signedPrekeyStore,
            OneTimePrekeyStore oneTimePrekeyStore,
            ContactStore contactStore
    ) {
        this(
                bundlesApi,
                localIdentity,
                signedPrekeyStore,
                oneTimePrekeyStore,
                contactStore,
                DEFAULT_ONE_TIME_PREKEY_TARGET,
                DEFAULT_MAX_ONE_TIME_PREKEYS_PER_UPLOAD,
                Clock.systemUTC(),
                new SecureRandom()
        );
    }

    public BundleProvisioningService(
            BundlesApi bundlesApi,
            LocalIdentity localIdentity,
            SignedPrekeyStore signedPrekeyStore,
            OneTimePrekeyStore oneTimePrekeyStore,
            int oneTimePrekeyTarget,
            int maxOneTimePrekeysPerUpload,
            Clock clock,
            SecureRandom secureRandom
    ) {
        this(
                bundlesApi,
                localIdentity,
                signedPrekeyStore,
                oneTimePrekeyStore,
                null,
                oneTimePrekeyTarget,
                maxOneTimePrekeysPerUpload,
                clock,
                secureRandom
        );
    }

    public BundleProvisioningService(
            BundlesApi bundlesApi,
            LocalIdentity localIdentity,
            SignedPrekeyStore signedPrekeyStore,
            OneTimePrekeyStore oneTimePrekeyStore,
            ContactStore contactStore,
            int oneTimePrekeyTarget,
            int maxOneTimePrekeysPerUpload,
            Clock clock,
            SecureRandom secureRandom
    ) {
        this.bundlesApi = Objects.requireNonNull(bundlesApi, "bundlesApi");
        this.localIdentity = Objects.requireNonNull(localIdentity, "localIdentity");
        this.signedPrekeyStore = signedPrekeyStore;
        this.oneTimePrekeyStore = oneTimePrekeyStore;
        this.contactStore = contactStore;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
        this.oneTimePrekeyTarget = requirePositive(oneTimePrekeyTarget, "oneTimePrekeyTarget");
        this.maxOneTimePrekeysPerUpload = requirePositive(
                maxOneTimePrekeysPerUpload,
                "maxOneTimePrekeysPerUpload"
        );
    }

    public CompletableFuture<Void> ensureBundleUploadedAsync(String jwt) {
        return bundlesApi.getBundleStatusAsync(jwt)
                .thenCompose(status -> {
                    if (!needsUpload(status)) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return uploadBundleAsync(jwt, status);
                });
    }

    public CompletableFuture<Boolean> addContact(String jwt, String userId) {
        return bundlesApi.downloadBundleAsync(jwt, userId)
                .thenApply(bundles -> {
                    if (bundles == null || bundles.payload == null || bundles.payload.length == 0) {
                        return false;
                    }

                    return persistContacts(bundles.payload);
                })
                .exceptionally(throwable -> false);
    }

    private boolean persistContacts(RemoteUserBundle[] bundles) {
        List<ContactRecord> contacts = new ArrayList<>();
        for (RemoteUserBundle bundle : bundles) {
            if (bundle == null || bundle.userId() == null || bundle.deviceId() == null) {
                continue;
            }

            contacts.add(new ContactRecord(
                    bundle.userId(),
                    bundle.deviceId(),
                    bundle.ik(),
                    bundle.sk()
            ));
        }

        if (contacts.isEmpty()) {
            return false;
        }

        if (contactStore == null) {
            return true;
        }

        try {
            contactStore.saveAll(contacts);
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist contacts", e);
        }
    }

    private boolean needsUpload(BundleStatusResponse status) {
        if (status == null) {
            return true;
        }

        return status.bundleMissing
                || status.signedPrekeyStale
                || status.oneTimePrekeyCount < effectiveOneTimePrekeyTarget(status);
    }

    private CompletableFuture<Void> uploadBundleAsync(String jwt, BundleStatusResponse status) {
        GeneratedBundle generatedBundle;

        try {
            generatedBundle = generateBundle(oneTimePrekeysToUpload(status));
        } catch (SQLException e) {
            return CompletableFuture.failedFuture(e);
        }

        return bundlesApi.uploadBundleAsync(jwt, generatedBundle.uploadBody())
                .thenRun(() -> persistGeneratedBundle(generatedBundle));
    }

    private GeneratedBundle generateBundle(int oneTimePrekeyCount) throws SQLException {
        byte[][] signedPrekey = CryptoUtils.generateKeyPair();
        byte[] signedPrekeySignature = CryptoUtils.sign(signedPrekey[0], localIdentity.signingPrivateKey());
        SignedPrekeyRecord signedPrekeyRecord = new SignedPrekeyRecord(
                nextSignedPrekeyId(),
                signedPrekey[0],
                signedPrekey[1],
                signedPrekeySignature,
                clock.millis(),
                true
        );

        byte[][] oneTimePrekeys = new byte[oneTimePrekeyCount][];
        List<OneTimePrekeyRecord> oneTimePrekeyRecords = new ArrayList<>();

        for (int i = 0; i < oneTimePrekeyCount; i++) {
            byte[][] oneTimePrekey = CryptoUtils.generateKeyPair();
            oneTimePrekeys[i] = oneTimePrekey[0];
            oneTimePrekeyRecords.add(new OneTimePrekeyRecord(
                    nextOneTimePrekeyId(),
                    oneTimePrekey[0],
                    oneTimePrekey[1],
                    false
            ));
        }

        LocalUserBundle uploadBody = new LocalUserBundle(
                localIdentity.userId(),
                localIdentity.deviceId(),
                localIdentity.registrationId(),
                localIdentity.signingPublicKey(),
                localIdentity.identityPublicKey(),
                signedPrekeyRecord.publicKey(),
                signedPrekeyRecord.signature(),
                oneTimePrekeys
        );

        return new GeneratedBundle(uploadBody, signedPrekeyRecord, oneTimePrekeyRecords);
    }

    private void persistGeneratedBundle(GeneratedBundle generatedBundle) {
        if (signedPrekeyStore == null || oneTimePrekeyStore == null) {
            return;
        }

        try {
            signedPrekeyStore.saveActive(generatedBundle.signedPrekey());
            oneTimePrekeyStore.saveAll(generatedBundle.oneTimePrekeys());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist generated bundle keys", e);
        }
    }

    private int oneTimePrekeysToUpload(BundleStatusResponse status) {
        int target = effectiveOneTimePrekeyTarget(status);
        int currentCount = status == null ? 0 : Math.max(status.oneTimePrekeyCount, 0);
        int missingCount = Math.max(target - currentCount, 0);

        if (status != null && status.bundleMissing && missingCount == 0) {
            missingCount = target;
        }

        return Math.min(missingCount, effectiveMaxOneTimePrekeysPerUpload(status));
    }

    private int effectiveOneTimePrekeyTarget(BundleStatusResponse status) {
        if (status != null && status.oneTimePrekeyTarget > 0) {
            return status.oneTimePrekeyTarget;
        }

        return oneTimePrekeyTarget;
    }

    private int effectiveMaxOneTimePrekeysPerUpload(BundleStatusResponse status) {
        if (status != null && status.maxOneTimePrekeysPerUpload > 0) {
            return Math.min(status.maxOneTimePrekeysPerUpload, maxOneTimePrekeysPerUpload);
        }

        return maxOneTimePrekeysPerUpload;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }

        return value;
    }

    private int nextSignedPrekeyId() throws SQLException {
        int keyId = nextPrekeyId();
        while (signedPrekeyStore != null && signedPrekeyStore.exists(keyId)) {
            keyId = nextPrekeyId();
        }

        return keyId;
    }

    private int nextOneTimePrekeyId() throws SQLException {
        int keyId = nextPrekeyId();
        while (oneTimePrekeyStore != null && oneTimePrekeyStore.exists(keyId)) {
            keyId = nextPrekeyId();
        }

        return keyId;
    }

    private int nextPrekeyId() {
        return secureRandom.nextInt(MAX_PREKEY_ID - 1) + 1;
    }

    private record GeneratedBundle(
            LocalUserBundle uploadBody,
            SignedPrekeyRecord signedPrekey,
            List<OneTimePrekeyRecord> oneTimePrekeys
    ) {
    }
}
