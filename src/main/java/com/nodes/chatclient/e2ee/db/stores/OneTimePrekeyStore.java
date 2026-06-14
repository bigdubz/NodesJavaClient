package com.nodes.chatclient.e2ee.db.stores;

import com.nodes.chatclient.e2ee.db.records.OneTimePrekeyRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OneTimePrekeyStore {

    private final Connection conn;

    public OneTimePrekeyStore(Connection conn) {
        this.conn = conn;
    }

    private static final String UPSERT_SQL = """
        INSERT INTO one_time_prekeys (
            keyId,
            publicKey,
            privateKey,
            isUsed
        ) VALUES (?, ?, ?, ?)
        ON CONFLICT (keyId) DO UPDATE SET
            publicKey = excluded.publicKey,
            privateKey = excluded.privateKey,
            isUsed = excluded.isUsed;
    """;

    public void save(OneTimePrekeyRecord prekey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            bind(ps, prekey);
            ps.executeUpdate();
        }
    }

    public void saveAll(List<OneTimePrekeyRecord> prekeys) throws SQLException {
        boolean previousAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
                for (OneTimePrekeyRecord prekey : prekeys) {
                    bind(ps, prekey);
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

    private static final String GET_UNUSED_SQL = """
        SELECT
            keyId,
            publicKey,
            privateKey,
            isUsed
        FROM one_time_prekeys
        WHERE isUsed = 0
        ORDER BY keyId
        LIMIT ?;
    """;

    public List<OneTimePrekeyRecord> getUnused(int limit) throws SQLException {
        List<OneTimePrekeyRecord> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(GET_UNUSED_SQL)) {
            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }

        return result;
    }

    private static final String GET_BY_ID_SQL = """
        SELECT
            keyId,
            publicKey,
            privateKey,
            isUsed
        FROM one_time_prekeys
        WHERE keyId = ?;
    """;

    public Optional<OneTimePrekeyRecord> getById(int keyId) throws SQLException {
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

    private static final String MARK_USED_SQL = """
        UPDATE one_time_prekeys
        SET isUsed = 1
        WHERE keyId = ?;
    """;

    public void markUsed(int keyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(MARK_USED_SQL)) {
            ps.setInt(1, keyId);
            ps.executeUpdate();
        }
    }

    private static final String UNUSED_COUNT_SQL = """
        SELECT COUNT(*) AS count
        FROM one_time_prekeys
        WHERE isUsed = 0;
    """;

    public int unusedCount() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UNUSED_COUNT_SQL);
                ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                return 0;
            }

            return rs.getInt("count");
        }
    }

    private static final String DELETE_USED_SQL = """
        DELETE FROM one_time_prekeys
        WHERE isUsed = 1;
    """;

    public void deleteUsed() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_USED_SQL)) {
            ps.executeUpdate();
        }
    }

    private static final String EXISTS_SQL = """
        SELECT 1
        FROM one_time_prekeys
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

    private void bind(PreparedStatement ps, OneTimePrekeyRecord prekey) throws SQLException {
        ps.setInt(1, prekey.keyId());
        ps.setBytes(2, prekey.publicKey());
        ps.setBytes(3, prekey.privateKey());
        ps.setInt(4, prekey.isUsed() ? 1 : 0);
    }

    private OneTimePrekeyRecord mapRow(ResultSet rs) throws SQLException {
        return new OneTimePrekeyRecord(
                rs.getInt("keyId"),
                rs.getBytes("publicKey"),
                rs.getBytes("privateKey"),
                rs.getInt("isUsed") == 1
        );
    }
}
