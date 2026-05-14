package com.nodes.chatclient.e2ee.db.stores;

import com.nodes.chatclient.e2ee.records.ContactRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ContactStore {

    private final Connection conn;

    public ContactStore(Connection conn) {
        this.conn = conn;
    }

    private static final String UPSERT_SQL = """
        INSERT INTO contacts (
            userId,
            deviceId,
            identityKey,
            signingKey
        ) VALUES (?, ?, ?, ?)
        ON CONFLICT (userId, deviceId) DO UPDATE SET
            identityKey = excluded.identityKey,
            signingKey = excluded.signingKey;
    """;

    public void save(ContactRecord contact) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            bind(ps, contact);
            ps.executeUpdate();
        }
    }

    public void saveAll(List<ContactRecord> contacts) throws SQLException {
        boolean previousAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
                for (ContactRecord contact : contacts) {
                    bind(ps, contact);
                    ps.addBatch();
                }

                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private static final String GET_SQL = """
        SELECT
            userId,
            deviceId,
            identityKey,
            signingKey
        FROM contacts
        WHERE userId = ? AND deviceId = ?;
    """;

    public Optional<ContactRecord> get(String userId, String deviceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(GET_SQL)) {
            ps.setString(1, userId);
            ps.setString(2, deviceId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapRow(rs));
            }
        }
    }

    private static final String GET_FOR_USER_SQL = """
        SELECT
            userId,
            deviceId,
            identityKey,
            signingKey
        FROM contacts
        WHERE userId = ?
        ORDER BY deviceId;
    """;

    public List<ContactRecord> getForUser(String userId) throws SQLException {
        List<ContactRecord> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(GET_FOR_USER_SQL)) {
            ps.setString(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }

        return result;
    }

    private static final String GET_ALL_SQL = """
        SELECT
            userId,
            deviceId,
            identityKey,
            signingKey
        FROM contacts
        ORDER BY userId, deviceId;
    """;

    public List<ContactRecord> getAll() throws SQLException {
        List<ContactRecord> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(GET_ALL_SQL);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }

        return result;
    }

    private static final String DELETE_SQL = """
        DELETE FROM contacts
        WHERE userId = ? AND deviceId = ?;
    """;

    public void delete(String userId, String deviceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setString(1, userId);
            ps.setString(2, deviceId);
            ps.executeUpdate();
        }
    }

    private static final String DELETE_USER_SQL = """
        DELETE FROM contacts
        WHERE userId = ?;
    """;

    public void deleteUser(String userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_USER_SQL)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    private static final String EXISTS_SQL = """
        SELECT 1
        FROM contacts
        WHERE userId = ? AND deviceId = ?
        LIMIT 1;
    """;

    public boolean exists(String userId, String deviceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_SQL)) {
            ps.setString(1, userId);
            ps.setString(2, deviceId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void bind(PreparedStatement ps, ContactRecord contact) throws SQLException {
        ps.setString(1, contact.userId());
        ps.setString(2, contact.deviceId());
        ps.setBytes(3, contact.identityKey());
        ps.setBytes(4, contact.signingKey());
    }

    private ContactRecord mapRow(ResultSet rs) throws SQLException {
        return new ContactRecord(
                rs.getString("userId"),
                rs.getString("deviceId"),
                rs.getBytes("identityKey"),
                rs.getBytes("signingKey")
        );
    }
}
