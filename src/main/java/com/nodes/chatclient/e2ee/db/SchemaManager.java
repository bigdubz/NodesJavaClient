package com.nodes.chatclient.e2ee.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public final class SchemaManager {

    private static final String SCHEMA_PATH = "schema/schema_v1.sql";

    private SchemaManager() {}

    public static void applyMigrations(Connection conn) throws Exception {
        ensureSchemaVersionTable(conn);

        int currentVersion = getCurrentVersion(conn);

        if (currentVersion < 1) {
            applySchema(conn, SCHEMA_PATH);
            setVersion(conn, 1);
        }

        // for the future
//         if (currentVersion < 2) { applySchema(conn, "db/schema_v2.sql"); setVersion(conn, 2); }
    }

    private static void applySchema(Connection conn, String resourcePath) throws Exception {
        String sql = loadSql(resourcePath);

        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            executeSqlScript(conn, sql);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    private static String loadSql(String path) throws Exception {
        try (InputStream is = SchemaManager.class
                .getClassLoader()
                .getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("SQL file not found: " + path);
            }

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void executeSqlScript(Connection conn, String sql) throws SQLException {
        String[] statements = sql.split(";");

        try (Statement stmt = conn.createStatement()) {
            for (String s : statements) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static void ensureSchemaVersionTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY
                );
            """);
        }
    }

    private static int getCurrentVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(version) as v FROM schema_version")) {

            if (rs.next()) {
                return rs.getInt("v");
            }
            return 0;
        }
    }

    private static void setVersion(Connection conn, int version) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO schema_version (version) VALUES (?)")) {
            ps.setInt(1, version);
            ps.executeUpdate();
        }
    }
}
