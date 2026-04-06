package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Cloud-ready Database service with externalized configuration and connection pooling
 * 
 * FIXES APPLIED:
 * - cr-java-0069: Replaced hardcoded database credentials with environment variables
 * - cr-java-0077: Replaced hardcoded ports with environment variable configuration
 * - cr-java-0073: Replaced direct JDBC connections with HikariCP connection pool
 * - cr-java-0097: Added connection and read timeouts to all database connections
 * - cr-java-0105: Moved I/O operations from static blocks to @PostConstruct
 * - cr-java-0113: Externalized all secrets to Azure Key Vault via environment variables
 */
@Service
public class DatabaseService {
    
    // Externalized database configuration using environment variables
    @Value("${spring.datasource.url:jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:mini_app_db}}")
    private String dbUrl;
    
    @Value("${spring.datasource.username:${DB_USERNAME:root}}")
    private String dbUsername;
    
    @Value("${spring.datasource.password:${DB_PASSWORD:}}")
    private String dbPassword;
    
    // Externalized cache configuration
    @Value("${cache.redis.host:${REDIS_HOST:localhost}}")
    private String redisHost;
    
    @Value("${cache.redis.port:${REDIS_PORT:6379}}")
    private int redisPort;
    
    // Externalized API endpoints
    @Value("${external.api.base-url:${EXTERNAL_API_URL:http://api.example.com/v1}}")
    private String externalApiUrl;
    
    @Value("${payment.service.url:${PAYMENT_SERVICE_URL:https://payment.example.com/process}}")
    private String paymentServiceUrl;
    
    // Connection pool configuration
    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;
    
    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;
    
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;
    
    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;
    
    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;
    
    private HikariDataSource dataSource;
    
    /**
     * Initialize database connection pool using @PostConstruct instead of static initializer
     * This allows proper error handling and Spring dependency injection
     */
    @PostConstruct
    public void connect() {
        try {
            System.out.println("Initializing database connection pool...");
            
            // Configure HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            
            // Connection pool settings with proper timeouts
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);
            
            // Additional cloud-ready settings
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("MiniAppHikariPool");
            
            // Leak detection for debugging
            config.setLeakDetectionThreshold(60000);
            
            // Initialize the data source
            dataSource = new HikariDataSource(config);
            
            System.out.println("Database connection pool initialized successfully");
            System.out.println("Database URL: " + maskSensitiveInfo(dbUrl));
            System.out.println("Pool size: " + maxPoolSize);
            
            // Initialize external services
            initializeExternalServices();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize database connection pool: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Initialize connections to external services with proper configuration
     */
    private void initializeExternalServices() {
        System.out.println("Initializing external services...");
        System.out.println("External API: " + maskSensitiveInfo(externalApiUrl));
        System.out.println("Payment Service: " + maskSensitiveInfo(paymentServiceUrl));
        System.out.println("Redis Cache: " + redisHost + ":" + redisPort);
    }
    
    /**
     * Execute SQL query using connection pool
     * Includes proper timeout configuration
     */
    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            // Get connection from pool
            connection = dataSource.getConnection();
            
            if (connection != null && !connection.isClosed()) {
                stmt = connection.prepareStatement(sql);
                
                // Set query timeout from environment variable
                int queryTimeout = Integer.parseInt(
                    System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30")
                );
                stmt.setQueryTimeout(queryTimeout);
                
                System.out.println("Executing query with timeout: " + queryTimeout + "s");
                stmt.execute();
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            throw new RuntimeException("Database query failed", e);
        } finally {
            // Proper resource cleanup
            closeResources(stmt, connection);
        }
    }
    
    /**
     * Get a connection from the pool
     * @return Database connection
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Close database resources properly
     */
    private void closeResources(PreparedStatement stmt, Connection connection) {
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close statement: " + e.getMessage());
        }
        
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close(); // Returns connection to pool
            }
        } catch (SQLException e) {
            System.err.println("Failed to close connection: " + e.getMessage());
        }
    }
    
    /**
     * Shutdown connection pool gracefully
     */
    @PreDestroy
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                System.out.println("Shutting down database connection pool...");
                dataSource.close();
                System.out.println("Database connection pool closed successfully");
            }
        } catch (Exception e) {
            System.err.println("Failed to close database connection pool: " + e.getMessage());
        }
    }
    
    /**
     * Mask sensitive information in URLs for logging
     */
    private String maskSensitiveInfo(String url) {
        if (url == null) {
            return "null";
        }
        // Mask passwords in connection strings
        return url.replaceAll("password=[^&;]*", "password=***");
    }
    
    /**
     * Health check method for monitoring
     */
    public boolean isHealthy() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                try (Connection conn = dataSource.getConnection()) {
                    return conn.isValid(5);
                }
            }
        } catch (SQLException e) {
            System.err.println("Health check failed: " + e.getMessage());
        }
        return false;
    }
}
