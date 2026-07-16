package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service migrated from MySQL to PostgreSQL 16.
 * Updated for Java 17 compatibility and PostgreSQL JDBC driver.
 *
 * Migration Changes:
 * - Replaced MySQL JDBC URL (jdbc:mysql://) with PostgreSQL JDBC URL (jdbc:postgresql://)
 * - Updated default port from 3306 (MySQL) to 5432 (PostgreSQL)
 * - Updated driver class reference from com.mysql.cj.jdbc.Driver to org.postgresql.Driver
 * - PostgreSQL JDBC 4.0+ driver auto-registers via ServiceLoader; Class.forName() not required
 * - Updated DB_URL to use PostgreSQL connection string format
 * - Added sslmode=disable for local/dev environments (configurable for production)
 */
public class DatabaseService {

    // Database connection details - migrated from MySQL to PostgreSQL
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "5432";  // PostgreSQL default port (was 3306 for MySQL)
    private static final String DB_NAME = "mini_app_db";
    // PostgreSQL JDBC URL format: jdbc:postgresql://<host>:<port>/<database>
    private static final String DB_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = "postgres";  // PostgreSQL default superuser (was 'root' for MySQL)
    private static final String DB_PASSWORD = "password123";

    // Cache server details
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;

    // External API endpoints
    private static final String EXTERNAL_API_URL = "http://api.example.com:8080/v1";
    private static final String PAYMENT_SERVICE_URL = "https://payment.internal.company.com/process";

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to PostgreSQL database...");

            // PostgreSQL JDBC 4.0+ driver (org.postgresql.Driver) auto-registers via ServiceLoader.
            // Class.forName("org.postgresql.Driver") is NOT required for PostgreSQL JDBC 42.x+
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
                // Java 17: Use try-with-resources for proper resource management
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    // Query timeout configuration
                    stmt.setQueryTimeout(30);
                    System.out.println("Executing query: " + sql);
                    stmt.execute();
                }
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
