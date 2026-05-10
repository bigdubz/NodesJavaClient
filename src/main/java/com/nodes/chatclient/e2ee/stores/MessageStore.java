package com.nodes.chatclient.e2ee.stores;

import com.nodes.chatclient.e2ee.records.MessageRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MessageStore {

    private final Connection conn;

    public MessageStore(Connection conn) {
        this.conn = conn;
    }

    // idempotent insert
    private static final String INSERT_SQL = """
        INSERT INTO messages (
            messageId,
            conversationId,
            senderUserId,
            senderDeviceId,
            createdAt,
            receivedAt,
            isOutgoing,
            type,
            deliveryStatus,
            controlType,
            body,
            reaction,
            reactionIsRemoved,
            referencedMessageId
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (messageId) DO NOTHING;
    """;

    public void insert(MessageRecord m) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setString(1, m.messageId);
            ps.setString(2, m.conversationId);
            ps.setString(3, m.senderUserId);
            ps.setString(4, m.senderDeviceId);

            ps.setLong(5, m.createdAt);
            ps.setLong(6, m.receivedAt);

            ps.setInt(7, m.isOutgoing ? 1 : 0);
            ps.setInt(8, m.type);
            ps.setInt(9, m.deliveryStatus);

            setNullableInt(ps, 10, m.controlType);
            ps.setString(11, m.body);
            ps.setString(12, m.reaction);
            setNullableInt(ps, 13, m.reactionIsRemoved);
            ps.setString(14, m.referencedMessageId);

            ps.executeUpdate();
        }
    }

    // update delivery status
    private static final String UPDATE_STATUS_SQL = """
        UPDATE messages
        SET deliveryStatus = ?
        WHERE messageId = ?;
    """;

    public void updateDeliveryStatus(String messageId, int status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS_SQL)) {
            ps.setInt(1, status);
            ps.setString(2, messageId);
            ps.executeUpdate();
        }
    }

    // fetch conversation
    private static final String GET_CONVO_SQL = """
        SELECT messageId,
                conversationId,
                senderUserId,
                senderDeviceId,
                createdAt,
                receivedAt,
                isOutgoing,
                type,
                deliveryStatus,
                controlType,
                body,
                reaction,
                reactionIsRemoved,
                referencedMessageId
        FROM messages
        WHERE conversationId = ?
        ORDER BY createdAt;
    """;

    public List<MessageRecord> getConversation(String conversationId) throws SQLException {
        List<MessageRecord> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(GET_CONVO_SQL)) {
            ps.setString(1, conversationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }

        return result;
    }

    // fetch single message
    private static final String GET_ONE_SQL = """
        SELECT * FROM messages
        WHERE messageId = ?;
    """;

    public Optional<MessageRecord> getById(String messageId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(GET_ONE_SQL)) {
            ps.setString(1, messageId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    // get pending outgoing messages
    private static final String GET_PENDING_SQL = """
        SELECT * FROM messages
        WHERE isOutgoing = 1 AND deliveryStatus = 0
        ORDER BY createdAt;
    """;

    public List<MessageRecord> getPendingOutgoing() throws SQLException {
        List<MessageRecord> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(GET_PENDING_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }

        return result;
    }

    // add reaction
    public void applyReaction(MessageRecord reactionMsg) throws SQLException {
        insert(reactionMsg);
    }

    private MessageRecord mapRow(ResultSet rs) throws SQLException {
        MessageRecord m = new MessageRecord();

        m.messageId = rs.getString("messageId");
        m.conversationId = rs.getString("conversationId");
        m.senderUserId = rs.getString("senderUserId");
        m.senderDeviceId = rs.getString("senderDeviceId");

        m.createdAt = rs.getLong("createdAt");
        m.receivedAt = rs.getLong("receivedAt");

        m.isOutgoing = rs.getInt("isOutgoing") == 1;
        m.type = rs.getInt("type");
        m.deliveryStatus = rs.getInt("deliveryStatus");

        m.controlType = getNullableInt(rs, "controlType");
        m.body = rs.getString("body");
        m.reaction = rs.getString("reaction");
        m.reactionIsRemoved = getNullableInt(rs, "reactionIsRemoved");
        m.referencedMessageId = rs.getString("referencedMessageId");

        return m;
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }

    private static Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int val = rs.getInt(col);
        return rs.wasNull() ? null : val;
    }
}
