package com.nodes.chatclient.e2ee.db.stores;

import com.nodes.chatclient.e2ee.db.records.SignedPrekeyRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class SignedPrekeyStore {

    private final Connection conn;

    public SignedPrekeyStore(Connection conn) {
        this.conn = conn;
    }

    private static final String UPSERT_SQL = """
        INSERT INTO signed_prekeys (
            keyId,
            publicKey,
            privateKey,
            signature,
            createdAt,
            isActive
        ) VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (keyId) DO UPDATE SET
            publicKey = excluded.publicKey,
            privateKey = excluded.privateKey,
            signature = excluded.signature,
            createdAt = excluded.createdAt,
            isActive = excluded.isActive;
    """;

    public void save(SignedPrekeyRecord prekey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            bind(ps, prekey);
            ps.executeUpdate();
        }
    }

    public void saveActive(SignedPrekeyRecord prekey) throws SQLException {
        boolean previousAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);
            deactivateAll();
            save(new SignedPrekeyRecord(
                    prekey.keyId(),
                    prekey.publicKey(),
                    prekey.privateKey(),
                    prekey.signature(),
                    prekey.createdAt(),
                    true
            ));
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private static final String GET_ACTIVE_SQL = """
        SELECT
            keyId,
            publicKey,
            privateKey,
            signature,
            createdAt,
            isActive
        FROM signed_prekeys
        WHERE isActive = 1
        ORDER BY createdAt DESC
        LIMIT 1;
    """;

    public Optional<SignedPrekeyRecord> getActive() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(GET_ACTIVE_SQL);
                ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                return Optional.empty();
            }

            return Optional.of(mapRow(rs));
        }
    }

    private static final String GET_BY_ID_SQL = """
        SELECT
            keyId,
            publicKey,
            privateKey,
            signature,
            createdAt,
            isActive
        FROM signed_prekeys
        WHERE keyId = ?;
    """;

    public Optional<SignedPrekeyRecord> getById(int keyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(GET_BY_ID_SQL)) {
            ps.setInt(1, keyId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapRow(rs));
            }
        }
    }

    private static final String DEACTIVATE_ALL_SQL = """
        UPDATE signed_prekeys
        SET isActive = 0;
    """;

    public void deactivateAll() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DEACTIVATE_ALL_SQL)) {
            ps.executeUpdate();
        }
    }

    private static final String DELETE_SQL = """
        DELETE FROM signed_prekeys
        WHERE keyId = ?;
    """;

    public void delete(int keyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setInt(1, keyId);
            ps.executeUpdate();
        }
    }

    private static final String EXISTS_SQL = """
        SELECT 1
        FROM signed_prekeys
        WHERE keyId = ?
        LIMIT 1;
    """;

    public boolean exists(int keyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_SQL)) {
            ps.setInt(1, keyId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void bind(PreparedStatement ps, SignedPrekeyRecord prekey) throws SQLException {
        ps.setInt(1, prekey.keyId());
        ps.setBytes(2, prekey.publicKey());
        ps.setBytes(3, prekey.privateKey());
        ps.setBytes(4, prekey.signature());
        ps.setLong(5, prekey.createdAt());
        ps.setInt(6, prekey.isActive() ? 1 : 0);
    }

    private SignedPrekeyRecord mapRow(ResultSet rs) throws SQLException {
        return new SignedPrekeyRecord(
                rs.getInt("keyId"),
                rs.getBytes("publicKey"),
                rs.getBytes("privateKey"),
                rs.getBytes("signature"),
                rs.getLong("createdAt"),
                rs.getInt("isActive") == 1
        );
    }
}
