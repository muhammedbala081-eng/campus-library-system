package app.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.stream.Collectors;

public class DatabaseManager {

    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties = new Properties();
    private static String dbUrl;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                properties.load(in);
                dbUrl = properties.getProperty("db.url", "jdbc:sqlite:clms.db");
            } else {
                System.err.println("Warning: config.properties not found on classpath, defaulting to jdbc:sqlite:clms.db");
                dbUrl = "jdbc:sqlite:clms.db";
            }
        } catch (Exception e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            dbUrl = "jdbc:sqlite:clms.db";
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }
        return conn;
    }

    public static synchronized void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection()) {
            executeSqlScript(conn, "schema.sql");
            System.out.println("Database schema verified at: " + dbUrl);

            // Seed with 1000 books, 150 students, 30 faculty, and active loans
            DatabaseSeeder.seedIfEmpty(conn);
        }
    }

    private static void executeSqlScript(Connection conn, String scriptPath) throws SQLException {
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(scriptPath)) {
            if (in == null) {
                throw new SQLException("SQL script not found on classpath: " + scriptPath);
            }
            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            // Split on semicolons while skipping empty statements
            String[] statements = sql.split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String raw : statements) {
                    String clean = raw.trim();
                    if (!clean.isEmpty()) {
                        stmt.execute(clean);
                    }
                }
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Failed reading SQL script " + scriptPath + ": " + e.getMessage(), e);
        }
    }

    public static Properties getProperties() {
        return properties;
    }

    public static String getDbUrl() {
        return dbUrl;
    }
}
