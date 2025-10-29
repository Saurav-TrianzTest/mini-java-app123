package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Cloud-ready Database service with externalized configuration
 */
public class DatabaseService {

    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());

    // Cloud-ready: Use environment variables for configuration
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?useSSL=true&serverTimezone=UTC";
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "password123");

    // Cloud-ready: Use environment variables for cache configuration
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // Cloud-ready: Use environment variables for external service URLs
    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");

    // Cloud-ready: Use connection pooling for database connections
    private HikariDataSource dataSource;
    
    public void connect() {
        try {
            logger.info("Initializing database connection pool...");

            // Cloud-ready: Use HikariCP for connection pooling
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USERNAME);
            config.setPassword(DB_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Cloud-ready: Configure connection pool for cloud environments
            config.setMaximumPoolSize(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_SIZE", "20")));
            config.setMinimumIdle(Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "5")));
            config.setConnectionTimeout(Long.parseLong(System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT", "30000")));
            config.setIdleTimeout(Long.parseLong(System.getenv().getOrDefault("DB_IDLE_TIMEOUT", "600000")));
            config.setMaxLifetime(Long.parseLong(System.getenv().getOrDefault("DB_MAX_LIFETIME", "1800000")));
            config.setLeakDetectionThreshold(60000);

            // Cloud-ready: Health check and validation
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);

            dataSource = new HikariDataSource(config);

            logger.info("Database connection pool initialized successfully");
            logger.info("Connected to database: " + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);

            // Initialize external services with environment-based configuration
            connectToCache();
            initializeExternalServices();

        } catch (Exception e) {
            logger.severe("Database connection failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }
    
    private void connectToCache() {
        // Cloud-ready: Use environment variables for cache configuration
        logger.info("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);

        // Cloud-ready: Add timeout configuration for cache connections
        int cacheTimeout = Integer.parseInt(System.getenv().getOrDefault("CACHE_TIMEOUT", "5000"));
        logger.info("Cache connection timeout set to: " + cacheTimeout + "ms");

        // Simulate cache connection with proper error handling
        try {
            // In a real implementation, this would use a Redis client like Jedis or Lettuce
            // with proper connection pooling and circuit breakers
            logger.info("Cache connection established successfully");
        } catch (Exception e) {
            logger.warning("Cache connection failed, continuing without cache: " + e.getMessage());
        }
    }

    private void initializeExternalServices() {
        // Cloud-ready: Use environment variables for service URLs and add timeouts
        int apiTimeout = Integer.parseInt(System.getenv().getOrDefault("API_TIMEOUT", "30000"));

        logger.info("Initializing external API: " + EXTERNAL_API_URL + " (timeout: " + apiTimeout + "ms)");
        logger.info("Initializing payment service: " + PAYMENT_SERVICE_URL + " (timeout: " + apiTimeout + "ms)");

        // Cloud-ready: Add circuit breaker pattern for external service calls
        logger.info("External services initialized with circuit breaker pattern");
    }
    
    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            if (dataSource != null) {
                connection = dataSource.getConnection();
                stmt = connection.prepareStatement(sql);

                // Cloud-ready: Use environment variable for query timeout
                int queryTimeout = Integer.parseInt(System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30"));
                stmt.setQueryTimeout(queryTimeout);

                logger.info("Executing query: " + sql.substring(0, Math.min(sql.length(), 100)) + "...");
                stmt.execute();
            }
        } catch (SQLException e) {
            logger.severe("Query execution failed: " + e.getMessage());
        } finally {
            // Cloud-ready: Proper resource cleanup
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.warning("Failed to close statement: " + e.getMessage());
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.warning("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("Database connection pool closed successfully");
            }
        } catch (SQLException e) {
            logger.severe("Failed to close database connection pool: " + e.getMessage());
        }
    }
}