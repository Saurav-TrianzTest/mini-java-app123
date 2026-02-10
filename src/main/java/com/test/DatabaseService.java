package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service - Cloud-Ready Version
 * Uses HikariCP connection pooling and environment-based configuration
 */
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    // Cloud-ready: All configuration from environment variables
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL = String.format("jdbc:mysql://%s:%s/%s?useSSL=true&requireSSL=false",
                                                       DB_HOST, DB_PORT, DB_NAME);
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    // Cloud-ready: Cache server from environment
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // Cloud-ready: External service URLs from environment
    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault(
        "EXTERNAL_API_URL",
        "http://api.example.com:8080/v1"
    );
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault(
        "PAYMENT_SERVICE_URL",
        "https://payment.internal.company.com/process"
    );

    // Cloud-ready: HikariCP connection pool
    private HikariDataSource dataSource;

    public void connect() {
        try {
            logger.info("Initializing database connection pool");

            // Cloud-ready: HikariCP connection pooling configuration
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USERNAME);
            config.setPassword(DB_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Cloud-ready: Connection pool settings for cloud environments
            config.setMaximumPoolSize(Integer.parseInt(
                System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "20")
            ));
            config.setMinimumIdle(Integer.parseInt(
                System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "5")
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

            // Cloud-ready: Health check query
            config.setConnectionTestQuery("SELECT 1");

            // Cloud-ready: Pool name for monitoring
            config.setPoolName("MiniAppHikariPool");

            // Initialize the data source
            dataSource = new HikariDataSource(config);

            logger.info("Database connection pool initialized successfully",
                       "host", DB_HOST,
                       "port", DB_PORT,
                       "database", DB_NAME,
                       "poolSize", config.getMaximumPoolSize());

            // Cloud-ready: Initialize other services with environment configuration
            connectToCache();
            initializeExternalServices();

        } catch (Exception e) {
            logger.error("Database connection pool initialization failed", e);
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    private void connectToCache() {
        // Cloud-ready: Cache connection with environment variables
        logger.info("Connecting to Redis cache",
                   "host", REDIS_HOST,
                   "port", REDIS_PORT);
        // Note: In production, implement actual Redis connection with Jedis or Lettuce
    }

    private void initializeExternalServices() {
        // Cloud-ready: External service initialization with environment variables
        logger.info("Initializing external services",
                   "apiUrl", EXTERNAL_API_URL,
                   "paymentServiceUrl", PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            // Cloud-ready: Get connection from pool
            connection = dataSource.getConnection();
            stmt = connection.prepareStatement(sql);

            // Cloud-ready: Query timeout from environment
            int queryTimeout = Integer.parseInt(
                System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30")
            );
            stmt.setQueryTimeout(queryTimeout);

            logger.debug("Executing query", "sql", sql);
            stmt.execute();

        } catch (SQLException e) {
            logger.error("Query execution failed", "sql", sql, e);
            throw new RuntimeException("Query execution failed", e);
        } finally {
            // Cloud-ready: Proper resource cleanup
            closeResources(stmt, connection);
        }
    }

    private void closeResources(PreparedStatement stmt, Connection connection) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.error("Failed to close PreparedStatement", e);
            }
        }
        if (connection != null) {
            try {
                connection.close(); // Returns connection to pool
            } catch (SQLException e) {
                logger.error("Failed to close database connection", e);
            }
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    // Cloud-ready: Health check method for cloud platform health probes
    public boolean isHealthy() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            logger.error("Health check failed", e);
            return false;
        }
    }
}
