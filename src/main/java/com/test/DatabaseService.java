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
 * Database service with HikariCP connection pooling - Cloud-Ready Version
 * Fixed cloud readiness issues:
 * - Replaced direct JDBC connections with HikariCP connection pool
 * - Replaced hardcoded database credentials with environment variables
 * - Replaced hardcoded API URLs with environment variable configuration
 * - Added proper connection lifecycle management
 * - Added connection timeout and pool configurations
 */
@Service
public class DatabaseService {
    
    // FIXED: Database configuration from environment variables
    @Value("${spring.datasource.url:jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:mini_app_db}}")
    private String dbUrl;
    
    @Value("${spring.datasource.username:${DB_USERNAME:root}}")
    private String dbUsername;
    
    @Value("${spring.datasource.password:${DB_PASSWORD:password}}")
    private String dbPassword;
    
    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String dbDriver;
    
    // FIXED: Connection pool configuration from environment variables
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
    
    // FIXED: Cache configuration from environment variables
    @Value("${cache.redis.host:${REDIS_HOST:localhost}}")
    private String redisHost;
    
    @Value("${cache.redis.port:${REDIS_PORT:6379}}")
    private int redisPort;
    
    // FIXED: External API URLs from environment variables
    @Value("${external.api.base-url:${EXTERNAL_API_URL:http://api.example.com/v1}}")
    private String externalApiUrl;
    
    @Value("${payment.service.url:${PAYMENT_SERVICE_URL:https://payment.example.com/process}}")
    private String paymentServiceUrl;
    
    // FIXED: HikariCP connection pool instead of direct JDBC
    private HikariDataSource dataSource;
    
    @PostConstruct
    public void initialize() {
        System.out.println("Initializing database service with HikariCP connection pool...");
        initializeConnectionPool();
        connectToCache();
        initializeExternalServices();
    }
    
    /**
     * FIXED: Initialize HikariCP connection pool with environment-based configuration
     */
    private void initializeConnectionPool() {
        try {
            HikariConfig config = new HikariConfig();
            
            // Database connection settings
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
            
            // Pool name for monitoring
            config.setPoolName("MiniAppHikariPool");
            
            // Connection test query
            config.setConnectionTestQuery("SELECT 1");
            
            // Leak detection threshold (10 seconds)
            config.setLeakDetectionThreshold(10000);
            
            // Auto-commit
            config.setAutoCommit(true);
            
            // Initialize the data source
            dataSource = new HikariDataSource(config);
            
            System.out.println("HikariCP connection pool initialized successfully");
            System.out.println("Database URL: " + maskSensitiveInfo(dbUrl));
            System.out.println("Max Pool Size: " + maxPoolSize);
            System.out.println("Connection Timeout: " + connectionTimeout + "ms");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize connection pool: " + e.getMessage());
            throw new RuntimeException("Database connection pool initialization failed", e);
        }
    }
    
    /**
     * FIXED: Cache connection using environment variables
     */
    private void connectToCache() {
        System.out.println("Cache configuration:");
        System.out.println("Redis Host: " + redisHost);
        System.out.println("Redis Port: " + redisPort);
        System.out.println("Note: Use AWS ElastiCache, Azure Cache for Redis, or GCP Memorystore in production");
    }
    
    /**
     * FIXED: External service URLs from environment variables
     */
    private void initializeExternalServices() {
        System.out.println("External service configuration:");
        System.out.println("External API URL: " + externalApiUrl);
        System.out.println("Payment Service URL: " + maskSensitiveInfo(paymentServiceUrl));
    }
    
    /**
     * Get a connection from the HikariCP pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Connection pool not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Test database connection
     */
    public void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Database connection test successful");
                System.out.println("Connection pool active connections: " + dataSource.getHikariPoolMXBean().getActiveConnections());
                System.out.println("Connection pool idle connections: " + dataSource.getHikariPoolMXBean().getIdleConnections());
            }
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
        }
    }
    
    /**
     * Execute a query using connection from pool
     */
    public void executeQuery(String sql) {
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            // FIXED: Query timeout from environment variable
            int queryTimeout = Integer.parseInt(System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30"));
            stmt.setQueryTimeout(queryTimeout);
            
            System.out.println("Executing query: " + sql);
            stmt.execute();
            
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup connection pool on shutdown
     */
    @PreDestroy
    public void cleanup() {
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("Closing HikariCP connection pool...");
            dataSource.close();
            System.out.println("Connection pool closed successfully");
        }
    }
    
    /**
     * Mask sensitive information in URLs for logging
     */
    private String maskSensitiveInfo(String url) {
        if (url == null) return "null";
        // Mask passwords in JDBC URLs
        return url.replaceAll("password=[^&;]*", "password=***");
    }
}
