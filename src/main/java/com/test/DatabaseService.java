package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service - updated for Java 25 compatibility.
 *
 * Changes applied:
 *  - FIX JAVA11_TO_25_DEPENDENCY_UPDATES: Uses com.mysql:mysql-connector-j 8.3.0
 *    driver class (com.mysql.cj.jdbc.Driver is still valid; explicit Class.forName
 *    is no longer required with JDBC 4.0+ auto-loading, so it has been removed to
 *    avoid potential module-access issues on Java 25).
 *  - FIX JAVA11_TO_25_STRONG_ENCAPSULATION: Removed explicit Class.forName() for
 *    the JDBC driver; JDBC 4.0 SPI auto-loading is the correct approach and avoids
 *    reflective access warnings under the Java 25 strong-encapsulation model.
 *  - NOTE: No Nashorn, SecurityManager, sun.misc.Unsafe, finalize(), or
 *    javax.* enterprise imports were present, so those critical rules do not apply.
 */
public class DatabaseService {

    // Connection details are read from environment variables so that the
    // application is container/cloud friendly and credentials are not
    // embedded in source code.
    private static final String DB_HOST =
            System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT =
            System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME =
            System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME =
            System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD =
            System.getenv().getOrDefault("DB_PASSWORD", "");

    private static final String REDIS_HOST =
            System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT =
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    private static final String EXTERNAL_API_URL =
            System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL =
            System.getenv().getOrDefault("PAYMENT_SERVICE_URL",
                    "https://payment.internal.company.com/process");

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to database...");

            // FIX JAVA11_TO_25_STRONG_ENCAPSULATION:
            // Removed explicit Class.forName("com.mysql.cj.jdbc.Driver").
            // JDBC 4.0+ (Java 6+) uses the ServiceLoader SPI to auto-discover
            // drivers from the classpath/module-path, which is the correct and
            // module-safe approach on Java 25.
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Connected to database: " + DB_URL);
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
