package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service updated for PostgreSQL 16 compatibility.
 * Replaced MySQL JDBC driver and connection URL with PostgreSQL equivalents.
 * JDBC 4.0+ auto-loads drivers via ServiceLoader, no explicit Class.forName needed.
 */
public class DatabaseService {

    // Updated: PostgreSQL connection details replacing MySQL hardcoded values
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "5432");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    // Updated: jdbc:mysql:// replaced with jdbc:postgresql:// for PostgreSQL 16
    private static final String DB_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "postgres");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "password123");

    // Cache server details
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // External API endpoints
    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault("EXTERNAL_API_BASE_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to PostgreSQL database...");

            // Java 17 compatible: JDBC 4.0+ auto-loads the PostgreSQL driver (org.postgresql.Driver)
            // via ServiceLoader mechanism. Explicit Class.forName() is not required.

            // Updated: Using PostgreSQL JDBC URL (jdbc:postgresql://) with PostgreSQL credentials
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Connected to PostgreSQL database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);

            // Cache connection
            connectToCache();

            // External service initialization
            initializeExternalServices();

        } catch (SQLException e) {
            System.err.println("PostgreSQL database connection failed: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // Redis connection details sourced from environment variables
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // External service URLs sourced from environment variables
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        try {
            if (connection != null && !connection.isClosed()) {
                PreparedStatement stmt = connection.prepareStatement(sql);
                // Query timeout in seconds
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
                System.out.println("PostgreSQL database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
}
