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
 * Cloud-ready Database Service
 * Fixed issues:
 * - Replaced hardcoded database credentials with environment variables and Secret Manager
 * - Replaced direct JDBC connections with HikariCP connection pool
 * - Added connection timeouts for cloud resilience
 * - Moved I/O operations from static blocks to @PostConstruct
 * - Externalized all configuration using Spring properties
 */
@Service
public class DatabaseService {
    
    // FIXED: Externalized database configuration using environment variables
    // These can be injected from GCP Secret Manager using ${sm://secret-name} syntax
    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/mini_app_db}")
    private String dbUrl;
    
    @Value("${spring.datasource.username:${sm://database-username}}")
    private String dbUsername;
    
    @Value("${spring.datasource.password:${sm://database-password}}")
    private String dbPassword;
    
    // FIXED: Externalized cache configuration
    @Value("${cache.redis.host:${REDIS_HOST:localhost}}")
    private String redisHost;
    
    @Value("${cache.redis.port:${REDIS_PORT:6379}}")
    private int redisPort;
    
    // FIXED: Externalized API endpoints
    @Value("${external.api.base-url:${EXTERNAL_API_URL:http://api.example.com/v1}}")
    private String externalApiUrl;
    
    @Value("${payment.service.url:${PAYMENT_SERVICE_URL:https://payment.example.com/process}}")
    private String paymentServiceUrl;
    
    // FIXED: Connection pool configuration
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
    
    // FIXED: Query timeout configuration
    @Value("${database.query.timeout:30}")
    private int queryTimeout;
    
    // FIXED: Use HikariCP connection pool instead of direct JDBC
    private HikariDataSource dataSource;
    
    /**
     * FIXED: Moved initialization from constructor/static block to @PostConstruct
     * This allows proper Spring dependency injection and error handling
     */
    @PostConstruct
    public void connect() {
        try {
            System.out.println("Initializing HikariCP connection pool for cloud environment...");
            
            // FIXED: Configure HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            
            // FIXED: Add connection timeouts for cloud resilience
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);
            
            // Additional cloud-ready configurations
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(60000);
            config.setPoolName("MiniAppHikariPool");
            
            // FIXED: Create connection pool
            dataSource = new HikariDataSource(config);
            
            System.out.println("Connected to database with HikariCP pool: " + maskUrl(dbUrl));
            System.out.println("Connection pool configured - Max: " + maxPoolSize + ", Min Idle: " + minIdle);
            
            // FIXED: Initialize external services with externalized configuration
            connectToCache();
            initializeExternalServices();
            
        } catch (Exception e) {
            System.err.println("Database connection pool initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }
    
    /**
     * FIXED: Externalized cache connection configuration
     */
    private void connectToCache() {
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort);
        System.out.println("Cache configuration loaded from environment variables");
        // Actual Redis connection would use Spring Data Redis with externalized config
    }
    
    /**
     * FIXED: Externalized external service URLs
     */
    private void initializeExternalServices() {
        System.out.println("Initializing external API: " + maskUrl(externalApiUrl));
        System.out.println("Initializing payment service: " + maskUrl(paymentServiceUrl));
        System.out.println("External service URLs loaded from environment variables");
    }
    
    /**
     * FIXED: Use connection pool instead of direct JDBC
     * Added proper timeout configuration
     */
    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            // FIXED: Get connection from pool
            connection = dataSource.getConnection();
            
            if (connection != null && !connection.isClosed()) {
                stmt = connection.prepareStatement(sql);
                
                // FIXED: Externalized query timeout
                stmt.setQueryTimeout(queryTimeout);
                
                System.out.println("Executing query with timeout: " + queryTimeout + "s");
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
     * FIXED: Proper resource cleanup
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
     * FIXED: Proper connection pool shutdown
     */
    @PreDestroy
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                System.out.println("HikariCP connection pool closed");
            }
        } catch (Exception e) {
            System.err.println("Failed to close connection pool: " + e.getMessage());
        }
    }
    
    /**
     * Utility method to mask sensitive URLs for logging
     */
    private String maskUrl(String url) {
        if (url == null) return "null";
        // Mask credentials in URL if present
        return url.replaceAll("://([^:]+):([^@]+)@", "://*****:*****@");
    }
    
    /**
     * Get the underlying DataSource for Spring integration
     */
    public DataSource getDataSource() {
        return dataSource;
    }
}
