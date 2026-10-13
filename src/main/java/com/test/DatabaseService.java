package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service.
 *
 * Java 25 upgrade notes:
 * - All connection details are now read from environment variables so that no
 *   credentials are hard-coded in source (also removes containerisation blockers).
 * - Class.forName() for the JDBC driver is no longer required; modern JDBC 4+
 *   drivers (including mysql-connector-j 8.3.x) register themselves via the
 *   ServiceLoader mechanism.  The call has been removed to avoid unnecessary
 *   ClassNotFoundException risk.
 * - No SecurityManager, Nashorn, sun.misc.Unsafe, or javax.* enterprise imports
 *   are present, so no additional migration is needed for those rules.
 */
public class DatabaseService {

    // Connection details resolved from environment variables at runtime
    private static final String DB_HOST =
            System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT =
            System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME =
            System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL =
            System.getenv().getOrDefault("DB_URL",
                    "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);
    private static final String DB_USERNAME =
            System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD =
            System.getenv().getOrDefault("DB_PASSWORD", "");

    // Cache server details resolved from environment variables
    private static final String REDIS_HOST =
            System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT =
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // External service URLs resolved from environment variables
    private static final String EXTERNAL_API_URL =
            System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL =
            System.getenv().getOrDefault("PAYMENT_SERVICE_URL",
                    "https://payment.internal.company.com/process");

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to database...");

            // JDBC 4+ drivers (mysql-connector-j 8.3.x) self-register via ServiceLoader;
            // explicit Class.forName() is no longer needed.
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
