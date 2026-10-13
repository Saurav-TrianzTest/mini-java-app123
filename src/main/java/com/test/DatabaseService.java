package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service - updated for Java 25 + PostgreSQL 16 compatibility.
 *
 * DB Migration Changes (MySQL → PostgreSQL 16):
 *  - [CRITICAL] Replaced MySQL JDBC URL (jdbc:mysql://...) with PostgreSQL JDBC URL
 *               (jdbc:postgresql://...) on port 5432 (PostgreSQL default).
 *  - [CRITICAL] Replaced MySQL driver class reference with PostgreSQL driver
 *               (org.postgresql.Driver). PostgreSQL JDBC driver also auto-registers
 *               via ServiceLoader, so no explicit Class.forName() is needed.
 *  - [HIGH]     Updated DB_PORT from "3306" (MySQL) to "5432" (PostgreSQL default).
 *  - [HIGH]     Updated DB_USERNAME from "root" (MySQL default) to "postgres"
 *               (PostgreSQL default superuser).
 *  - [MEDIUM]   Added PostgreSQL connection parameters: sslmode=prefer,
 *               ApplicationName for connection identification, and
 *               connectTimeout for resilience.
 *
 * Java 25 compatibility notes (retained from previous upgrade):
 *  - Removed explicit Class.forName() driver registration – PostgreSQL JDBC driver
 *    (org.postgresql:postgresql:42.7.3) registers itself automatically via
 *    java.util.ServiceLoader, avoiding module-system violations on Java 25.
 *  - No SecurityManager, Nashorn, sun.misc.Unsafe, or finalize() usages present.
 */
public class DatabaseService {

    // ─── PostgreSQL Connection Details ────────────────────────────────────────
    // [CRITICAL] Updated from MySQL host/port/driver to PostgreSQL equivalents
    private static final String DB_HOST = "localhost";
    // [HIGH] Updated port from 3306 (MySQL) to 5432 (PostgreSQL default)
    private static final String DB_PORT = "5432";
    private static final String DB_NAME = "mini_app_db";

    // [CRITICAL] Replaced MySQL JDBC URL with PostgreSQL JDBC URL:
    //   Before: jdbc:mysql://localhost:3306/mini_app_db
    //   After:  jdbc:postgresql://localhost:5432/mini_app_db?sslmode=prefer&ApplicationName=mini-java-app&connectTimeout=10
    private static final String DB_URL =
            "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?sslmode=prefer&ApplicationName=mini-java-app&connectTimeout=10";

    // [HIGH] Updated username from MySQL "root" to PostgreSQL default superuser "postgres"
    private static final String DB_USERNAME = "postgres";
    private static final String DB_PASSWORD = "password123";

    // Hardcoded cache server details (containerization concern, not a DB-migration blocker)
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;

    // Hardcoded API endpoints (containerization concern, not a DB-migration blocker)
    private static final String EXTERNAL_API_URL = "http://api.example.com:8080/v1";
    private static final String PAYMENT_SERVICE_URL = "https://payment.internal.company.com/process";

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to PostgreSQL database...");

            // DB Migration fix: No explicit Class.forName() needed.
            // org.postgresql.Driver registers itself automatically via ServiceLoader
            // in postgresql-42.7.3, avoiding illegal reflective access on Java 25.
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Connected to PostgreSQL database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);

            connectToCache();
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
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setQueryTimeout(30);

                System.out.println("Executing query on PostgreSQL: " + sql);
                stmt.execute();
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed on PostgreSQL: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("PostgreSQL database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Failed to close PostgreSQL database connection: " + e.getMessage());
        }
    }
}
