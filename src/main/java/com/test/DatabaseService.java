package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Database service - Cloud-ready version with connection pooling for AWS deployment
 */
public class DatabaseService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String CORRELATION_ID = UUID.randomUUID().toString();

    // Cloud-ready: Database connection from environment variables
    private static final String DB_URL = System.getenv()
        .getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/mini_app_db");
    private static final String DB_USERNAME = System.getenv()
        .getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getenv()
        .getOrDefault("DB_PASSWORD", "");

    // Cloud-ready: Cache configuration from environment variables
    private static final String REDIS_HOST = System.getenv()
        .getOrDefault("REDIS_HOST", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(
        System.getenv().getOrDefault("REDIS_PORT", "6379")
    );

    // Cloud-ready: External API endpoints from environment variables
    private static final String EXTERNAL_API_URL = System.getenv()
        .getOrDefault("EXTERNAL_API_URL", "http://api.example.com/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv()
        .getOrDefault("PAYMENT_SERVICE_URL", "https://payment.example.com/process");

    // Cloud-ready: Connection pooling with HikariCP
    private HikariDataSource dataSource;
    private Connection connection;
    
    public void connect() {
        try {
            logStructured("INFO", "Initializing database connection pool", "Setting up HikariCP");

            // Cloud-ready: Configure HikariCP connection pooling
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USERNAME);
            config.setPassword(DB_PASSWORD);

            // Cloud-ready: Connection pool configuration from environment
            config.setMaximumPoolSize(Integer.parseInt(
                System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10")
            ));
            config.setMinimumIdle(Integer.parseInt(
                System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "2")
            ));
            config.setConnectionTimeout(Long.parseLong(
                System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT", "30000")
            ));
            config.setIdleTimeout(Long.parseLong(
                System.getenv().getOrDefault("DB_IDLE_TIMEOUT", "600000")
            ));
            config.setMaxLifetime(Long.parseLong(
                System.getenv().getOrDefault("DB_MAX_LIFETIME", "1800000")
            ));

            // Additional cloud-ready settings
            config.setConnectionTestQuery("SELECT 1");
            config.setAutoCommit(true);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            connection = dataSource.getConnection();

            logStructured("INFO", "Database connection pool initialized",
                "Connected to: " + maskUrl(DB_URL) + " with user: " + maskUsername(DB_USERNAME));

            // Cloud-ready: Initialize cache and external services
            connectToCache();
            initializeExternalServices();

        } catch (SQLException e) {
            logStructured("ERROR", "Database connection failed",
                "Error: " + e.getMessage());
        }
    }
    
    private void connectToCache() {
        // Cloud-ready: Redis connection from environment variables
        logStructured("INFO", "Connecting to Redis cache",
            "Cache endpoint: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // Cloud-ready: External service URLs from environment variables
        logStructured("INFO", "Initializing external API",
            "API endpoint: " + EXTERNAL_API_URL);
        logStructured("INFO", "Initializing payment service",
            "Payment endpoint: " + maskUrl(PAYMENT_SERVICE_URL));
    }
    
    public void executeQuery(String sql) {
        try {
            // Cloud-ready: Get connection from pool
            Connection conn = dataSource.getConnection();

            if (conn != null && !conn.isClosed()) {
                PreparedStatement stmt = conn.prepareStatement(sql);

                // Cloud-ready: Query timeout from environment variable
                int queryTimeout = Integer.parseInt(
                    System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30")
                );
                stmt.setQueryTimeout(queryTimeout);

                logStructured("INFO", "Executing database query",
                    "Query timeout: " + queryTimeout + "s");
                stmt.execute();
                stmt.close();
            }

            conn.close(); // Return connection to pool
        } catch (SQLException e) {
            logStructured("ERROR", "Query execution failed",
                "Error: " + e.getMessage());
        }
    }
    
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logStructured("INFO", "Database connection pool closed",
                    "HikariCP datasource shutdown completed");
            }
        } catch (SQLException e) {
            logStructured("ERROR", "Failed to close database connection pool",
                "Error: " + e.getMessage());
        }
    }

    /**
     * Cloud-ready structured logging with JSON format and correlation IDs
     */
    private static void logStructured(String level, String message, String details) {
        try {
            ObjectNode logEntry = JSON_MAPPER.createObjectNode();
            logEntry.put("timestamp", Instant.now().toString());
            logEntry.put("level", level);
            logEntry.put("message", message);
            logEntry.put("details", details);
            logEntry.put("correlationId", CORRELATION_ID);
            logEntry.put("service", "database-service");
            logEntry.put("environment", System.getenv().getOrDefault("ENVIRONMENT", "unknown"));

            System.out.println(JSON_MAPPER.writeValueAsString(logEntry));
        } catch (Exception e) {
            // Fallback to simple logging if JSON serialization fails
            System.out.println(String.format("[%s] %s - %s - %s",
                Instant.now(), level, message, details));
        }
    }

    /**
     * Mask sensitive URLs for logging
     */
    private static String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll("(https?://)[^/]+", "$1***");
    }

    /**
     * Mask sensitive usernames for logging
     */
    private static String maskUsername(String username) {
        if (username == null || username.length() <= 2) return "***";
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }
}