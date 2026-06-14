package com.nodes.chatclient.e2ee.identity;

import com.nodes.chatclient.e2ee.crypto.KeyMaterial;
import com.nodes.chatclient.e2ee.db.stores.LocalIdentityStore;
import com.nodes.chatclient.e2ee.types.LocalIdentity;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class LocalIdentityService {

    private static final int MAX_REGISTRATION_ID = 0x7fffffff;

    private final LocalIdentityStore store;
    private final Clock clock;
    private final SecureRandom secureRandom;

    public LocalIdentityService(LocalIdentityStore store) {
        this(store, Clock.systemUTC(), new SecureRandom());
    }

    LocalIdentityService(LocalIdentityStore store, Clock clock, SecureRandom secureRandom) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    public static LocalIdentityService from(Connection conn) {
        return new LocalIdentityService(new LocalIdentityStore(conn));
    }

    public Optional<LocalIdentity> load() throws SQLException {
        return store.get();
    }

    public LocalIdentity getOrCreate(String userId) throws SQLException {
        return getOrCreate(userId, newDeviceId());
    }

    public LocalIdentity getOrCreate(String userId, String deviceId) throws SQLException {
        Optional<LocalIdentity> existing = store.get();
        if (existing.isPresent()) {
            return existing.get();
        }

        LocalIdentity identity = build(userId, deviceId);
        persist(identity);
        return identity;
    }

    public LocalIdentity build(String userId, String deviceId) {
        requireText(userId, "userId");
        requireText(deviceId, "deviceId");

        byte[][] identityKeyPair = KeyMaterial.generateX25519KeyPair();
        byte[][] signingKeyPair = KeyMaterial.generateEd25519KeyPair();

        return new LocalIdentity(
                userId,
                deviceId,
                newRegistrationId(),
                identityKeyPair[0],
                identityKeyPair[1],
                signingKeyPair[0],
                signingKeyPair[1],
                clock.millis()
        );
    }

    public void persist(LocalIdentity identity) throws SQLException {
        store.save(Objects.requireNonNull(identity, "identity"));
    }

    private int newRegistrationId() {
        return secureRandom.nextInt(MAX_REGISTRATION_ID - 1) + 1;
    }

    private static String newDeviceId() {
        return UUID.randomUUID().toString();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
