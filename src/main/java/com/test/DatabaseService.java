package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Database service - Cloud Ready Version with connection pooling
 */
public class DatabaseService {

    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());

    // Cloud-ready: Use environment variables for all configuration
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?useSSL=true&serverTimezone=UTC";
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    // Cloud-ready: Use environment variables for cache configuration
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // Cloud-ready: Use environment variables for external services
    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");

    // Cloud-ready: Use connection pooling instead of direct connections
    private HikariDataSource dataSource;
    
    public void connect() {
        try {
            logger.info("Initializing database connection pool...");

            // Cloud-ready: Configure HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USERNAME);
            config.setPassword(DB_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Cloud-ready: Connection pool settings with timeouts
            config.setMaximumPoolSize(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "20")));
            config.setMinimumIdle(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "5")));
            config.setConnectionTimeout(Integer.parseInt(System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT", "30000")));
            config.setIdleTimeout(Integer.parseInt(System.getenv().getOrDefault("DB_IDLE_TIMEOUT", "600000")));
            config.setMaxLifetime(Integer.parseInt(System.getenv().getOrDefault("DB_MAX_LIFETIME", "1800000")));

            // Cloud-ready: Health check and validation
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);

            this.dataSource = new HikariDataSource(config);

            logger.info("Database connection pool initialized successfully");
            logger.info("Database URL: " + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);

            // Cloud-ready: Initialize external services with environment variables
            connectToCache();
            initializeExternalServices();

        } catch (Exception e) {
            logger.severe("Database connection pool initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }
    
    private void connectToCache() {
        // Cloud-ready: Cache connection using environment variables
        logger.info("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // In a real implementation, this would use Redis client with connection pooling
        // For now, just log the cloud-ready configuration
        logger.info("Redis connection configured with environment variables");
    }

    private void initializeExternalServices() {
        // Cloud-ready: External service URLs from environment variables
        logger.info("Initializing external API: " + EXTERNAL_API_URL);
        logger.info("Initializing payment service: " + PAYMENT_SERVICE_URL);
        logger.info("External services configured with environment variables");
    }
    
    public void executeQuery(String sql) {
        // Cloud-ready: Use connection pool instead of direct connection
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            // Cloud-ready: Configurable query timeout from environment variables
            int queryTimeout = Integer.parseInt(System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30"));
            stmt.setQueryTimeout(queryTimeout);

            logger.info("Executing query: " + sql);
            stmt.execute();

        } catch (SQLException e) {
            logger.severe("Query execution failed: " + e.getMessage());
            throw new RuntimeException("Database query failed", e);
        }
    }
    
    public void disconnect() {
        // Cloud-ready: Properly close connection pool
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}