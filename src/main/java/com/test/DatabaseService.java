package com.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database service - Cloud Ready Version with Connection Pooling
 */
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    // Cloud-ready configuration using environment variables
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL = String.format("jdbc:mysql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "password123");

    // Cache server configuration from environment variables
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // External service URLs from environment variables
    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");

    private DataSource dataSource;

    public void connect() {
        try {
            logger.info("Initializing database connection pool...");

            // Use HikariCP for connection pooling - cloud-ready pattern
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USERNAME);
            config.setPassword(DB_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Cloud-ready connection pool settings
            config.setMaximumPoolSize(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "20")));
            config.setMinimumIdle(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "5")));
            config.setConnectionTimeout(Long.parseLong(System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT", "30000")));
            config.setIdleTimeout(Long.parseLong(System.getenv().getOrDefault("DB_IDLE_TIMEOUT", "600000")));
            config.setMaxLifetime(Long.parseLong(System.getenv().getOrDefault("DB_MAX_LIFETIME", "1800000")));

            // Health check query for cloud environments
            config.setConnectionTestQuery("SELECT 1");

            this.dataSource = new HikariDataSource(config);

            logger.info("Database connection pool initialized successfully");
            logger.info("Connected to database: {}", DB_URL);

            // Initialize cache connection using environment variables
            connectToCache();

            // Initialize external services using environment variables
            initializeExternalServices();

        } catch (Exception e) {
            logger.error("Database connection failed: {}", e.getMessage());
        }
    }
    
    private void connectToCache() {
        // Use environment variables for cache connection
        logger.info("Connecting to Redis cache at: {}:{}", REDIS_HOST, REDIS_PORT);
        // Cache connection should be implemented with proper connection pooling
        // and retry mechanisms for cloud environments
        logger.info("Cache connection configuration loaded from environment variables");
    }

    private void initializeExternalServices() {
        // Use environment variables for external service URLs
        logger.info("Initializing external API: {}", EXTERNAL_API_URL);
        logger.info("Initializing payment service: {}", PAYMENT_SERVICE_URL);
        // External service connections should include circuit breakers
        // and proper timeout configurations for cloud resilience
        logger.info("External services configured with environment variables");
    }
    
    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            if (dataSource != null) {
                connection = dataSource.getConnection();
                stmt = connection.prepareStatement(sql);

                // Use environment variable for query timeout
                int queryTimeout = Integer.parseInt(System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30"));
                stmt.setQueryTimeout(queryTimeout);

                logger.info("Executing query: {}", sql);
                stmt.execute();
            }
        } catch (SQLException e) {
            logger.error("Query execution failed: {}", e.getMessage());
        } finally {
            // Ensure proper resource cleanup
            try {
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                logger.error("Failed to close database resources: {}", e.getMessage());
            }
        }
    }

    public void disconnect() {
        try {
            if (dataSource != null && dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
                logger.info("Database connection pool closed");
            }
        } catch (Exception e) {
            logger.error("Failed to close database connection pool: {}", e.getMessage());
        }
    }
}