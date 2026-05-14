package com.nodes.chatclient.e2ee.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class DatabaseManager {

    private static Connection connection;

    private DatabaseManager() {}

    public static void init() throws Exception {
        Path dbPath = resolveDbPath();
        Files.createDirectories(dbPath.getParent());

        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connection = DriverManager.getConnection(url);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL");
        }

        SchemaManager.applyMigrations(connection);
    }

    public static Connection get() {
        if (connection == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return connection;
    }

    private static Path resolveDbPath() {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Paths.get(appData, "Nodes", "client.db");
        } else if (os.contains("mac")) {
            return Paths.get(home, "Library", "Application Support", "Nodes", "client.db");
        } else {
            return Paths.get(home, ".local", "share", "Nodes", "client.db");
        }
    }
}
