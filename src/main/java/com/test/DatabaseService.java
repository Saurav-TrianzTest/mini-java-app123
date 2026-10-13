package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service - Updated for Java 25 compatibility and migrated to PostgreSQL 16.
 *
 * Java 25 upgrade changes applied:
 * - Removed explicit Class.forName("org.postgresql.Driver") call; the JDBC 4.0+
 *   service-loader mechanism (available since Java 6) auto-registers drivers, and
 *   the explicit call is unnecessary and can cause issues with the module system
 *   (JAVA11_TO_25_MODULE_SYSTEM / JAVA11_TO_25_STRONG_ENCAPSULATION).
 * - Connection parameters are now read from environment variables so that credentials
 *   are not hard-coded in source (security best-practice; also avoids issues with
 *   JAVA11_TO_25_SECURITY_CHANGES).
 * - No javax.* enterprise imports; no SecurityManager, Nashorn, or sun.misc.Unsafe usage.
 *
 * PostgreSQL 16 migration changes applied:
 * - Replaced MySQL JDBC driver (com.mysql:mysql-connector-j) with PostgreSQL JDBC driver
 *   (org.postgresql:postgresql 42.7.3).
 * - Updated JDBC URL format from jdbc:mysql:// to jdbc:postgresql://.
 * - Updated default port from 3306 (MySQL) to 5432 (PostgreSQL).
 * - Updated default username from "root" (MySQL convention) to "postgres" (PostgreSQL convention).
 * - DB_URL environment variable default updated to use PostgreSQL connection string format.
 */
public class DatabaseService {

    // Connection details resolved from environment variables at runtime.
    // Defaults are provided only for local development convenience.
    private static final String DB_HOST =
            System.getenv().getOrDefault("DB_HOST", "localhost");
    // PostgreSQL default port is 5432 (changed from MySQL default 3306)
    private static final String DB_PORT =
            System.getenv().getOrDefault("DB_PORT", "5432");
    private static final String DB_NAME =
            System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    // PostgreSQL JDBC URL format: jdbc:postgresql://<host>:<port>/<database>
    private static final String DB_URL =
            System.getenv().getOrDefault("DB_URL",
                    "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);
    // PostgreSQL default superuser is "postgres" (changed from MySQL "root")
    private static final String DB_USERNAME =
            System.getenv().getOrDefault("DB_USERNAME", "postgres");
    private static final String DB_PASSWORD =
            System.getenv().getOrDefault("DB_PASSWORD", "");

    // Cache server details resolved from environment variables.
    private static final String REDIS_HOST =
            System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT =
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // External service URLs resolved from environment variables.
    private static final String EXTERNAL_API_URL =
            System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL =
            System.getenv().getOrDefault("PAYMENT_SERVICE_URL",
                    "https://payment.internal.company.com/process");

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to PostgreSQL database...");

            // Java 25 / JDBC 4.0+: Driver auto-registration via ServiceLoader.
            // Explicit Class.forName() is no longer needed and has been removed
            // (JAVA11_TO_25_MODULE_SYSTEM – avoids reflective access into driver internals).
            // PostgreSQL JDBC driver (org.postgresql:postgresql 42.7.3) supports
            // JDBC 4.2 and is fully compatible with PostgreSQL 16.
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Connected to PostgreSQL database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);

            connectToCache();
            initializeExternalServices();

        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    private void connectToCache() {
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        try {
            if (connection != null && !connection.isClosed()) {
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setQueryTimeout(30);

                System.out.println("Executing query: " + sql);
                stmt.execute();
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
}
