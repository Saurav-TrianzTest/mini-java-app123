package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service with HikariCP connection pooling - Cloud-Ready
 * 
 * FIXED ISSUES:
 * - Replaced direct JDBC connections with HikariCP connection pool
 * - Externalized all hardcoded database credentials to environment variables
 * - Externalized cache and external service URLs to environment variables
 * - Added proper connection lifecycle management
 * - Implemented connection pooling for optimal cloud performance
 */
@Service
public class DatabaseService {
    
    // FIXED: All configuration now comes from environment variables or application.properties
    // No hardcoded values - follows 12-factor app principles
    
    @Value("${spring.datasource.url:jdbc:mysql://${DATABASE_HOST:localhost}:${DATABASE_PORT:3306}/${DATABASE_NAME:mini_app_db}}")
    private String dbUrl;
    
    @Value("${spring.datasource.username:${DATABASE_USERNAME:root}}")
    private String dbUsername;
    
    @Value("${spring.datasource.password:${DATABASE_PASSWORD:}}")
    private String dbPassword;
    
    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String dbDriver;
    
    // HikariCP Configuration - FIXED: Externalized to environment variables
    @Value("${spring.datasource.hikari.maximum-pool-size:${DB_POOL_MAX_SIZE:10}}")
    private int maxPoolSize;
    
    @Value("${spring.datasource.hikari.minimum-idle:${DB_POOL_MIN_IDLE:2}}")
    private int minIdle;
    
    @Value("${spring.datasource.hikari.connection-timeout:${DB_CONNECTION_TIMEOUT:30000}}")
    private long connectionTimeout;
    
    @Value("${spring.datasource.hikari.idle-timeout:${DB_IDLE_TIMEOUT:600000}}")
    private long idleTimeout;
    
    @Value("${spring.datasource.hikari.max-lifetime:${DB_MAX_LIFETIME:1800000}}")
    private long maxLifetime;
    
    // FIXED: Redis configuration from environment variables
    @Value("${spring.redis.host:${REDIS_HOST:localhost}}")
    private String redisHost;
    
    @Value("${spring.redis.port:${REDIS_PORT:6379}}")
    private int redisPort;
    
    @Value("${spring.redis.password:${REDIS_PASSWORD:}}")
    private String redisPassword;
    
    // FIXED: External API configuration from environment variables
    @Value("${external.api.base-url:${EXTERNAL_API_URL:http://api.example.com/v1}}")
    private String externalApiUrl;
    
    @Value("${external.api.key:${EXTERNAL_API_KEY:}}")
    private String externalApiKey;
    
    @Value("${payment.service.url:${PAYMENT_SERVICE_URL:https://payment.example.com/process}}")
    private String paymentServiceUrl;
    
    @Value("${payment.service.username:${PAYMENT_SERVICE_USERNAME:}}")
    private String paymentServiceUsername;
    
    @Value("${payment.service.password:${PAYMENT_SERVICE_PASSWORD:}}")
    private String paymentServicePassword;
    
    // FIXED: HikariCP DataSource for connection pooling
    private HikariDataSource dataSource;
    
    /**
     * FIXED: Initialize HikariCP connection pool on startup
     * Replaces direct JDBC connection management with enterprise-grade connection pooling
     */
    @PostConstruct
    public void connect() {
        try {
            System.out.println("Initializing HikariCP connection pool...");
            
            // FIXED: Configure HikariCP with externalized settings
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            config.setDriverClassName(dbDriver);
            
            // Connection pool settings
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);
            
            // Performance and reliability settings for cloud environments
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("MiniAppHikariPool");
            
            // Leak detection for debugging (can be disabled in production)
            config.setLeakDetectionThreshold(60000); // 60 seconds
            
            // FIXED: Create HikariCP DataSource
            dataSource = new HikariDataSource(config);
            
            System.out.println("HikariCP connection pool initialized successfully");
            System.out.println("Database URL: " + maskSensitiveInfo(dbUrl));
            System.out.println("Pool size: " + maxPoolSize + " (max), " + minIdle + " (min idle)");
            
            // FIXED: Initialize other cloud services with externalized configuration
            connectToCache();
            initializeExternalServices();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize HikariCP connection pool: " + e.getMessage());
            throw new RuntimeException("Database connection pool initialization failed", e);
        }
    }
    
    /**
     * FIXED: Get connection from HikariCP pool instead of direct JDBC
     * Provides connection reuse, leak detection, and optimal performance
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * FIXED: Redis connection using externalized configuration
     */
    private void connectToCache() {
        // FIXED: Use environment variables for Redis connection
        System.out.println("Redis cache configuration:");
        System.out.println("  Host: " + redisHost);
        System.out.println("  Port: " + redisPort);
        System.out.println("  Password: " + (redisPassword != null && !redisPassword.isEmpty() ? "***" : "not set"));
        
        // In a real application, you would initialize Redis client here
        // using spring-boot-starter-data-redis with externalized configuration
    }
    
    /**
     * FIXED: External services using externalized configuration
     */
    private void initializeExternalServices() {
        // FIXED: Use environment variables for external service URLs
        System.out.println("External services configuration:");
        System.out.println("  API URL: " + externalApiUrl);
        System.out.println("  API Key: " + (externalApiKey != null && !externalApiKey.isEmpty() ? "***" : "not set"));
        System.out.println("  Payment Service URL: " + paymentServiceUrl);
        System.out.println("  Payment Service Username: " + maskSensitiveInfo(paymentServiceUsername));
    }
    
    /**
     * Execute query using connection from HikariCP pool
     */
    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            // FIXED: Get connection from pool
            connection = getConnection();
            
            if (connection != null && !connection.isClosed()) {
                stmt = connection.prepareStatement(sql);
                
                // FIXED: Query timeout now configurable via environment
                int queryTimeout = Integer.parseInt(
                    System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30")
                );
                stmt.setQueryTimeout(queryTimeout);
                
                System.out.println("Executing query: " + sql);
                stmt.execute();
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            throw new RuntimeException("Query execution failed", e);
        } finally {
            // FIXED: Proper resource cleanup
            closeResources(stmt, connection);
        }
    }
    
    /**
     * FIXED: Proper resource cleanup to prevent connection leaks
     */
    private void closeResources(PreparedStatement stmt, Connection connection) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                System.err.println("Failed to close statement: " + e.getMessage());
            }
        }
        
        if (connection != null) {
            try {
                // FIXED: Return connection to pool (not actually closing it)
                connection.close();
            } catch (SQLException e) {
                System.err.println("Failed to return connection to pool: " + e.getMessage());
            }
        }
    }
    
    /**
     * FIXED: Shutdown HikariCP connection pool gracefully
     */
    @PreDestroy
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                System.out.println("Shutting down HikariCP connection pool...");
                dataSource.close();
                System.out.println("HikariCP connection pool closed successfully");
            }
        } catch (Exception e) {
            System.err.println("Failed to close HikariCP connection pool: " + e.getMessage());
        }
    }
    
    /**
     * Utility method to mask sensitive information in logs
     */
    private String maskSensitiveInfo(String value) {
        if (value == null || value.isEmpty()) {
            return "not set";
        }
        if (value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
    
    /**
     * Get HikariCP pool statistics for monitoring
     */
    public String getPoolStats() {
        if (dataSource != null) {
            return String.format(
                "HikariCP Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        }
        return "Pool not initialized";
    }
}
