package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service with hardcoded connection details - intentional containerization blockers.
 *
 * Java 25 upgrade notes:
 *  - Removed explicit Class.forName("com.mysql.cj.jdbc.Driver") call.
 *    Since Java 6 / JDBC 4.0 the driver is auto-loaded via the ServiceLoader
 *    mechanism (META-INF/services/java.sql.Driver).  Keeping the manual
 *    Class.forName call is harmless but unnecessary and can mask classpath
 *    issues on newer runtimes.  The updated mysql-connector-j 9.x artifact
 *    (com.mysql:mysql-connector-j) fully supports Java 25 and registers its
 *    driver automatically.
 *  - No javax.* → jakarta.* migration is required in this file because it
 *    only uses core java.sql APIs which remain in the JDK.
 */
public class DatabaseService {

    // BLOCKER: Hardcoded database connection details
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "mini_app_db";
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "password123";

    // BLOCKER: Hardcoded cache server details
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;

    // BLOCKER: Hardcoded API endpoints
    private static final String EXTERNAL_API_URL = "http://api.example.com:8080/v1";
    private static final String PAYMENT_SERVICE_URL = "https://payment.internal.company.com/process";

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to database...");

            // Java 25 fix: removed explicit Class.forName("com.mysql.cj.jdbc.Driver").
            // JDBC 4.0+ (Java 6+) auto-discovers drivers via ServiceLoader.
            // The updated com.mysql:mysql-connector-j:9.1.0 dependency registers
            // its driver automatically, so the manual load is no longer needed
            // and was removed to avoid potential issues on the module path.

            // BLOCKER: Hardcoded connection string and credentials
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Connected to database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);

            // BLOCKER: Hardcoded cache connection
            connectToCache();

            // BLOCKER: Hardcoded external service URLs
            initializeExternalServices();

        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // BLOCKER: Hardcoded Redis connection details
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // BLOCKER: Hardcoded external service URLs
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        try {
            if (connection != null && !connection.isClosed()) {
                PreparedStatement stmt = connection.prepareStatement(sql);
                // BLOCKER: Hardcoded query timeout
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
