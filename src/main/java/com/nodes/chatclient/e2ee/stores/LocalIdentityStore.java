package com.nodes.chatclient.e2ee.stores;

import com.nodes.chatclient.e2ee.types.LocalIdentity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class LocalIdentityStore {

    private final Connection conn;

    public LocalIdentityStore(Connection conn) {
        this.conn = conn;
    }

    private static final String GET_LOCAL_IDENTITY_SQL = """
        SELECT
            userId,
            deviceId,
            registrationId,
            signingPublicKey,
            signingPrivateKey,
            identityPublicKey,
            identityPrivateKey,
            createdAt
        FROM device_identity
        LIMIT 1;
    """;

    public Optional<LocalIdentity> get() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(GET_LOCAL_IDENTITY_SQL);
                ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                return Optional.empty();
            }

            LocalIdentity identity = new LocalIdentity(
                    rs.getString("userId"),
                    rs.getString("deviceId"),
                    rs.getInt("registrationId"),

                    rs.getBytes("identityPublicKey"),
                    rs.getBytes("identityPrivateKey"),
                    rs.getBytes("signingPublicKey"),
                    rs.getBytes("signingPrivateKey"),
                    rs.getLong("createdAt")
            );

            return Optional.of(identity);
        }
    }

    private static final String EXISTS_SQL = """
        SELECT 1
        FROM device_identity
        LIMIT 1;
    """;

    public boolean exists() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_SQL);
                ResultSet rs = ps.executeQuery()) {

            return rs.next();
        }
    }

    private static final String SAVE_SQL = """
        INSERT INTO device_identity (
            userId,
            deviceId,
            registrationId,
            signingPublicKey,
            signingPrivateKey,
            identityPublicKey,
            identityPrivateKey,
            createdAt
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
    """;

    public void save(LocalIdentity identity) throws SQLException {
        if (exists()) {
            throw new IllegalStateException("Local identity already exists");
        }

        try (PreparedStatement ps = conn.prepareStatement(SAVE_SQL)) {
            bind(ps, identity);
            ps.executeUpdate();
        }
    }

    private static final String REPLACE_SQL = """
        INSERT OR REPLACE INTO device_identity (
            userId,
            deviceId,
            registrationId,
            signingPublicKey,
            signingPrivateKey,
            identityPublicKey,
            identityPrivateKey,
            createdAt
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
    """;

    public void replace(LocalIdentity identity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(REPLACE_SQL)) {
            bind(ps, identity);
            ps.executeUpdate();
        }
    }

    private static final String DELETE_SQL = """
        DELETE FROM device_identity;
    """;

    public void delete() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.executeUpdate();
        }
    }


    private void bind(PreparedStatement ps, LocalIdentity identity) throws SQLException {
        ps.setString(1, identity.userId());
        ps.setString(2, identity.deviceId());
        ps.setInt(3, identity.registrationId());

        ps.setBytes(4, identity.signingPublicKey());
        ps.setBytes(5, identity.signingPrivateKey());

        ps.setBytes(6, identity.identityPublicKey());
        ps.setBytes(7, identity.identityPrivateKey());

        ps.setLong(8, identity.createdAt());
    }
}
