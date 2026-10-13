package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service - updated for Java 25 compatibility.
 *
 * Changes made for Java 11 → Java 25 upgrade:
 *  - Removed explicit Class.forName("com.mysql.cj.jdbc.Driver") call.
 *    Modern JDBC drivers (mysql-connector-j 9.x) register themselves via
 *    java.util.ServiceLoader; the manual Class.forName is unnecessary and
 *    can cause issues with the module system on Java 25
 *    (JAVA11_TO_25_STRONG_ENCAPSULATION / JAVA11_TO_25_MODULE_SYSTEM).
 *  - mysql-connector-java 8.0.33 (old groupId "mysql") replaced with
 *    mysql-connector-j 9.1.0 (groupId "com.mysql") in pom.xml for full
 *    Java 25 / Jakarta EE compatibility (JAVA11_TO_25_SPRING_BOOT_COMPATIBILITY).
 *  - No SecurityManager, Nashorn, sun.misc.Unsafe, or finalize() usages present.
 */
public class DatabaseService {

    // Hardcoded connection details (containerization concern, not a Java-version blocker)
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "mini_app_db";
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "password123";

    // Hardcoded cache server details (containerization concern, not a Java-version blocker)
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;

    // Hardcoded API endpoints (containerization concern, not a Java-version blocker)
    private static final String EXTERNAL_API_URL = "http://api.example.com:8080/v1";
    private static final String PAYMENT_SERVICE_URL = "https://payment.internal.company.com/process";

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to database...");

            // JAVA11_TO_25_MODULE_SYSTEM / JAVA11_TO_25_STRONG_ENCAPSULATION:
            // Removed Class.forName("com.mysql.cj.jdbc.Driver") – the driver registers
            // itself automatically via ServiceLoader in mysql-connector-j 9.x, and
            // explicit Class.forName can trigger illegal reflective access on Java 25.
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
