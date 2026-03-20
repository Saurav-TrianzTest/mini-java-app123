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
 * Database service - Cloud-Ready Version with HikariCP Connection Pooling
 * Fixed cloud readiness issues:
 * - Replaced direct JDBC connections with HikariCP connection pool
 * - Replaced hardcoded credentials with environment variables
 * - Replaced hardcoded URLs with externalized configuration
 * - Added proper connection lifecycle management
 * - Added cloud-native resilience patterns
 */
@Service
public class DatabaseService {
    
    // FIXED: Externalized database configuration via environment variables
    @Value("${spring.datasource.url:jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:mini_app_db}}")
    private String dbUrl;
    
    @Value("${spring.datasource.username:${DB_USERNAME:root}}")
    private String dbUsername;
    
    @Value("${spring.datasource.password:${DB_PASSWORD:}}")
    private String dbPassword;
    
    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String dbDriver;
    
    // FIXED: Externalized cache configuration
    @Value("${redis.host:${REDIS_HOST:localhost}}")
    private String redisHost;
    
    @Value("${redis.port:${REDIS_PORT:6379}}")
    private int redisPort;
    
    // FIXED: Externalized external service URLs
    @Value("${external.api.url:${EXTERNAL_API_URL:http://api.example.com/v1}}")
    private String externalApiUrl;
    
    @Value("${payment.service.url:${PAYMENT_SERVICE_URL:https://payment.example.com/process}}")
    private String paymentServiceUrl;
    
    // FIXED: HikariCP connection pool configuration
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
    
    // HikariCP DataSource for connection pooling
    private HikariDataSource dataSource;
    
    /**
     * FIXED: Initialize HikariCP connection pool instead of direct JDBC connection
     * Connection pooling provides:
     * - Connection reuse and efficient resource utilization
     * - Automatic connection leak detection
     * - Connection validation and health checks
     * - Optimal performance for cloud environments
     */
    @PostConstruct
    public void connect() {
        try {
            System.out.println("Initializing HikariCP connection pool...");
            
            // Configure HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            config.setDriverClassName(dbDriver);
            
            // Connection pool settings optimized for cloud environments
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);
            
            // Cloud-native resilience settings
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);
            config.setLeakDetectionThreshold(60000);
            
            // Pool name for monitoring
            config.setPoolName("MiniAppHikariPool");
            
            // Additional cloud-optimized settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            // Initialize the connection pool
            dataSource = new HikariDataSource(config);
            
            System.out.println("HikariCP connection pool initialized successfully");
            System.out.println("Database URL: " + maskSensitiveInfo(dbUrl));
            System.out.println("Pool size: " + minIdle + " (min) - " + maxPoolSize + " (max)");
            
            // Test connection
            testConnection();
            
            // Initialize external services with externalized configuration
            connectToCache();
            initializeExternalServices();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize connection pool: " + e.getMessage());
            throw new RuntimeException("Database connection pool initialization failed", e);
        }
    }
    
    /**
     * Test database connection from the pool
     */
    private void testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Database connection test successful");
            }
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
        }
    }
    
    /**
     * FIXED: Cache connection using externalized configuration
     */
    private void connectToCache() {
        System.out.println("Cache configuration:");
        System.out.println("  Redis Host: " + redisHost + " (from environment)");
        System.out.println("  Redis Port: " + redisPort + " (from environment)");
        // Actual Redis connection would use Spring Data Redis with connection pooling
    }
    
    /**
     * FIXED: External services using externalized configuration
     */
    private void initializeExternalServices() {
        System.out.println("External services configuration:");
        System.out.println("  External API: " + externalApiUrl + " (from environment)");
        System.out.println("  Payment Service: " + maskSensitiveInfo(paymentServiceUrl) + " (from environment)");
    }
    
    /**
     * Execute query using connection from the pool
     */
    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            // Get connection from pool
            connection = dataSource.getConnection();
            
            if (connection != null && !connection.isClosed()) {
                stmt = connection.prepareStatement(sql);
                
                // Query timeout from environment variable
                int queryTimeout = Integer.parseInt(
                    System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30")
                );
                stmt.setQueryTimeout(queryTimeout);
                
                System.out.println("Executing query: " + sql);
                stmt.execute();
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        } finally {
            // Proper resource cleanup
            closeResources(stmt, connection);
        }
    }
    
    /**
     * Get a connection from the pool for external use
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Connection pool not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Get the DataSource for Spring integration
     */
    public DataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Proper resource cleanup
     */
    private void closeResources(PreparedStatement stmt, Connection conn) {
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close statement: " + e.getMessage());
        }
        
        try {
            if (conn != null && !conn.isClosed()) {
                // Return connection to pool (not actually closing it)
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to return connection to pool: " + e.getMessage());
        }
    }
    
    /**
     * FIXED: Proper connection pool shutdown
     */
    @PreDestroy
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                System.out.println("Shutting down HikariCP connection pool...");
                dataSource.close();
                System.out.println("Connection pool closed successfully");
            }
        } catch (Exception e) {
            System.err.println("Failed to close connection pool: " + e.getMessage());
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
