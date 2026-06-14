package com.nodes.chatclient.e2ee.provisioning;

import com.nodes.chatclient.e2ee.crypto.KeyMaterial;
import com.nodes.chatclient.e2ee.crypto.MessageAuth;
import com.nodes.chatclient.e2ee.db.records.OneTimePrekeyRecord;
import com.nodes.chatclient.e2ee.db.records.SignedPrekeyRecord;
import com.nodes.chatclient.e2ee.db.stores.OneTimePrekeyStore;
import com.nodes.chatclient.e2ee.db.stores.SignedPrekeyStore;
import com.nodes.chatclient.e2ee.types.BundleOneTimePrekey;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.types.LocalUserBundle;
import com.nodes.chatclient.http.api.BundlesApi;
import com.nodes.chatclient.http.dto.BundleStatusResponse;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class BundleProvisioningService {

    private static final int DEFAULT_ONE_TIME_PREKEY_TARGET = 100;
    private static final int DEFAULT_MAX_ONE_TIME_PREKEYS_PER_UPLOAD = 100;
    private static final int MAX_PREKEY_ID = 0x7fffffff;

    private final BundlesApi bundlesApi;
    private final LocalIdentity localIdentity;
    private final SignedPrekeyStore signedPrekeyStore;
    private final OneTimePrekeyStore oneTimePrekeyStore;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final int oneTimePrekeyTarget;
    private final int maxOneTimePrekeysPerUpload;


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
        this.bundlesApi = Objects.requireNonNull(bundlesApi, "bundlesApi");
        this.localIdentity = Objects.requireNonNull(localIdentity, "localIdentity");
        this.signedPrekeyStore = signedPrekeyStore;
        this.oneTimePrekeyStore = oneTimePrekeyStore;
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

    private boolean needsUpload(BundleStatusResponse status) {
        if (status == null) {
            return true;
        }

        return status.bundleMissing()
                || status.signedPrekeyStale()
                || status.oneTimePrekeyCount() < effectiveOneTimePrekeyTarget(status);
    }

    private CompletableFuture<Void> uploadBundleAsync(String jwt, BundleStatusResponse status) {
        GeneratedBundle generatedBundle;

        try {
            generatedBundle = generateBundlePerStatus(status);
        } catch (SQLException e) {
            return CompletableFuture.failedFuture(e);
        }

        return bundlesApi.uploadBundleAsync(jwt, generatedBundle.uploadBody())
                .thenRun(() -> persistGeneratedBundle(generatedBundle));
    }

    private GeneratedBundle generateBundlePerStatus(BundleStatusResponse status) throws SQLException {
        SignedPrekeyRecord signedPrekeyRecord;
        if (status.signedPrekeyStale()) {
            byte[][] signedPrekey = KeyMaterial.generateX25519KeyPair();
            byte[] signedPrekeySignature = MessageAuth.sign(signedPrekey[0], localIdentity.signingPrivateKey());
            signedPrekeyRecord = new SignedPrekeyRecord(
                    nextSignedPrekeyId(),
                    signedPrekey[0],
                    signedPrekey[1],
                    signedPrekeySignature,
                    clock.millis(),
                    true
            );
        } else {
            if (signedPrekeyStore != null) {
                Optional<SignedPrekeyRecord> active = signedPrekeyStore.getActive();
                if (active.isPresent()) {
                    signedPrekeyRecord = active.get();
                } else {
                    throw new IllegalStateException("No active SPK available");
                }
            } else {
                throw new IllegalStateException("No signed prekey store available");
            }
        }

        int oneTimePrekeyCount = oneTimePrekeysToUpload(status);
        BundleOneTimePrekey[] oneTimePrekeys = new BundleOneTimePrekey[oneTimePrekeyCount];
        List<OneTimePrekeyRecord> oneTimePrekeyRecords = new ArrayList<>();
        Set<Integer> generatedOneTimePrekeyIds = new HashSet<>();

        for (int i = 0; i < oneTimePrekeyCount; i++) {
            byte[][] oneTimePrekey = KeyMaterial.generateX25519KeyPair();
            int id = nextOneTimePrekeyId();
            while (!generatedOneTimePrekeyIds.add(id)) {
                id = nextOneTimePrekeyId();
            }
            oneTimePrekeys[i] = new BundleOneTimePrekey(id, oneTimePrekey[0]);
            oneTimePrekeyRecords.add(new OneTimePrekeyRecord(
                    id,
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
        int currentCount = status == null ? 0 : Math.max(status.oneTimePrekeyCount(), 0);
        int missingCount = Math.max(target - currentCount, 0);

        if (status != null && status.bundleMissing() && missingCount == 0) {
            missingCount = target;
        }

        return Math.min(missingCount, effectiveMaxOneTimePrekeysPerUpload(status));
    }

    private int effectiveOneTimePrekeyTarget(BundleStatusResponse status) {
        if (status != null && status.oneTimePrekeyTarget() > 0) {
            return status.oneTimePrekeyTarget();
        }

        return oneTimePrekeyTarget;
    }

    private int effectiveMaxOneTimePrekeysPerUpload(BundleStatusResponse status) {
        if (status != null && status.maxOneTimePrekeysPerUpload() > 0) {
            return Math.min(status.maxOneTimePrekeysPerUpload(), maxOneTimePrekeysPerUpload);
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
