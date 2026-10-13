package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service - Updated for Java 25 compatibility.
 *
 * Changes applied:
 * - JAVA11_TO_25_DEPENDENCY_UPDATES: Class compiles under Java 25 (source/target/release 25).
 * - JAVA11_TO_25_MISSING_DEPENDENCY_VERSIONS: Uses updated com.mysql:mysql-connector-j:8.3.0
 *   driver; com.mysql.cj.jdbc.Driver class name remains valid in 8.3.0.
 * - JAVA11_TO_25_STRONG_ENCAPSULATION: No reflective setAccessible usage present;
 *   Class.forName is a standard public API and remains safe in Java 25.
 * - JAVA11_TO_25_UTF8_DEFAULT_CHARSET: No byte/char conversions present; no change needed.
 * - JAVA11_TO_25_JAKARTA_EE_MIGRATION: No javax.* imports present; no migration needed.
 */
public class DatabaseService {

    // NOTE: Hardcoded database connection details - consider externalising to application.properties
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "mini_app_db";
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "password123";

    // NOTE: Hardcoded cache server details - consider externalising to application.properties
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;

    // NOTE: Hardcoded API endpoints - consider externalising to application.properties
    private static final String EXTERNAL_API_URL = "http://api.example.com:8080/v1";
    private static final String PAYMENT_SERVICE_URL = "https://payment.internal.company.com/process";

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to database...");

            // JAVA11_TO_25_DEPENDENCY_UPDATES: Driver class is still valid with
            // com.mysql:mysql-connector-j:8.3.0 (updated groupId/artifactId in pom.xml).
            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Connected to database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);

            connectToCache();
            initializeExternalServices();

        } catch (ClassNotFoundException e) {
            System.err.println("Database driver not found: " + e.getMessage());
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
