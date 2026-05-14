package com.nodes.chatclient.e2ee.stores;

import com.nodes.chatclient.e2ee.mappers.SessionMapper;
import com.nodes.chatclient.e2ee.protos.ProtoSession;
import com.nodes.chatclient.e2ee.types.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class SessionStore {

    private final Connection conn;

    public SessionStore(Connection conn) {
        this.conn = conn;
    }

    // save
    private static final String UPSERT_SQL = """
        INSERT INTO sessions (
            sessionId,
            remoteUserId,
            remoteDeviceId,
            sessionBlob,
            updatedAt
        ) VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (sessionId) DO UPDATE SET
            sessionBlob = excluded.sessionBlob,
            updatedAt = excluded.updatedAt;
    """;

    public void save(String remoteUserId, String remoteDeviceId, Session session) throws Exception {
        String sessionId = key(remoteUserId, remoteDeviceId);

        byte[] blob = SessionMapper.serialize(session).toByteArray();

        try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, sessionId);
            ps.setString(2, remoteUserId);
            ps.setString(3, remoteDeviceId);
            ps.setBytes(4, blob);
            ps.setLong(5, System.currentTimeMillis());

            ps.executeUpdate();
        }
    }

    // load
    private static final String LOAD_SQL = """
        SELECT sessionBlob
        FROM sessions
        WHERE sessionId = ?;
    """;

    public Optional<Session> load(String remoteUserId, String remoteDeviceId) throws Exception {
        String sessionId = key(remoteUserId, remoteDeviceId);

        try (PreparedStatement ps = conn.prepareStatement(LOAD_SQL)) {
            ps.setString(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                byte[] blob = rs.getBytes("sessionBlob");
                return Optional.of(
                        SessionMapper.deserialize(
                                ProtoSession.SessionProto.parseFrom(blob)
                        )
                );
            }
        }
    }

    // delete
    private static final String DELETE_SQL = """
        DELETE FROM sessions
        WHERE sessionId = ?;
    """;

    public void delete(String remoteUserId, String remoteDeviceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setString(1, key(remoteUserId, remoteDeviceId));
            ps.executeUpdate();
        }
    }

    // exists check
    private static final String EXISTS_SQL = """
        SELECT 1
        FROM sessions
        WHERE sessionId = ?
        LIMIT 1;
    """;

    public boolean exists(String remoteUserId, String remoteDeviceId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_SQL)) {
            ps.setString(1, key(remoteUserId, remoteDeviceId));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String key(String userId, String deviceId) {
        return userId + ":" + deviceId;
    }
}
