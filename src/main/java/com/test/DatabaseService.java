package com.test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Database service with externalized configuration for cloud-native deployment
 */
public class DatabaseService {

    // Externalized configuration - loaded from environment variables or config service
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final String redisHost;
    private final int redisPort;
    private final String externalApiUrl;
    private final String paymentServiceUrl;
    private final int queryTimeout;
    private final int maxPoolSize;
    private final int connectionTimeout;

    private DataSource dataSource;

    /**
     * Constructor with externalized configuration from environment variables
     */
    public DatabaseService() {
        this.dbUrl = getEnv("DATABASE_URL", "jdbc:mysql://localhost:3306/mini_app_db");
        this.dbUsername = getEnv("DB_USERNAME", "root");
        this.dbPassword = getEnv("DB_PASSWORD", "");
        this.redisHost = getEnv("REDIS_HOST", "localhost");
        this.redisPort = Integer.parseInt(getEnv("REDIS_PORT", "6379"));
        this.externalApiUrl = getEnv("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
        this.paymentServiceUrl = getEnv("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");
        this.queryTimeout = Integer.parseInt(getEnv("DB_QUERY_TIMEOUT", "30"));
        this.maxPoolSize = Integer.parseInt(getEnv("DB_POOL_MAX", "20"));
        this.connectionTimeout = Integer.parseInt(getEnv("DB_POOL_TIMEOUT", "30000"));
    }

    /**
     * Helper method to get environment variables with fallback defaults
     */
    private String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Initialize connection pool using HikariCP
     */
    public void connect() {
        try {
            System.out.println("Initializing database connection pool...");

            // Configure HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("MiniAppPool");

            // Create DataSource with connection pooling
            dataSource = new HikariDataSource(config);

            System.out.println("Database connection pool initialized successfully");

            // Initialize cache and external services
            connectToCache();
            initializeExternalServices();

        } catch (Exception e) {
            System.err.println("Database connection pool initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }

    private void connectToCache() {
        // Externalized Redis connection configuration
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort);
        // Cache connection implementation with externalized configuration
    }

    private void initializeExternalServices() {
        // Externalized service URLs from environment variables
        System.out.println("Initializing external API: " + externalApiUrl);
        System.out.println("Initializing payment service: " + paymentServiceUrl);
    }

    /**
     * Execute parameterized query with connection pooling and externalized timeout
     */
    public void executeQuery(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Get connection from pool
            conn = dataSource.getConnection();

            // Prepare statement with parameterized query
            stmt = conn.prepareStatement(sql);

            // Set parameters to prevent SQL injection
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            // Use externalized query timeout
            stmt.setQueryTimeout(queryTimeout);

            System.out.println("Executing parameterized query");
            stmt.execute();

        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            // Implement proper error handling with retry logic for transient errors
            handleSQLException(e);
        } finally {
            // Ensure resources are properly closed
            closeResources(stmt, conn);
        }
    }

    /**
     * Handle SQL exceptions with proper error classification
     */
    private void handleSQLException(SQLException e) {
        String sqlState = e.getSQLState();
        // Check for transient errors that can be retried
        if (sqlState != null && (sqlState.startsWith("08") || sqlState.startsWith("40"))) {
            System.err.println("Transient database error detected: " + e.getMessage());
            // Implement retry logic with exponential backoff
        } else {
            System.err.println("Permanent database error: " + e.getMessage());
        }
    }

    /**
     * Safely close database resources
     */
    private void closeResources(PreparedStatement stmt, Connection conn) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close statement: " + e.getMessage());
        }

        try {
            if (conn != null) {
                conn.close(); // Return connection to pool
            }
        } catch (SQLException e) {
            System.err.println("Failed to close connection: " + e.getMessage());
        }
    }

    /**
     * Shutdown connection pool gracefully
     */
    public void disconnect() {
        try {
            if (dataSource != null && dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
                System.out.println("Database connection pool closed successfully");
            }
        } catch (Exception e) {
            System.err.println("Failed to close database connection pool: " + e.getMessage());
        }
    }
}
