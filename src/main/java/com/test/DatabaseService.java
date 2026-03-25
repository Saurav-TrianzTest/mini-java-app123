package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service - Cloud-Ready Version
 * Fixed: Using HikariCP connection pooling instead of direct JDBC connections
 * Fixed: All configuration externalized to environment variables
 */
public class DatabaseService {
    
    // FIXED: All hardcoded values replaced with environment variables
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL = System.getenv().getOrDefault(
        "DB_URL", 
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
    );
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");
    
    // FIXED: Cache configuration from environment variables
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    
    // FIXED: API endpoints from environment variables
    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault(
        "EXTERNAL_API_URL", 
        "http://api.example.com:8080/v1"
    );
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault(
        "PAYMENT_SERVICE_URL", 
        "https://payment.internal.company.com/process"
    );
    
    // FIXED: Using HikariCP DataSource instead of direct Connection
    private HikariDataSource dataSource;
    
    public void connect() {
        try {
            System.out.println("Initializing HikariCP connection pool...");
            
            // FIXED: Configure HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USERNAME);
            config.setPassword(DB_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Connection pool settings from environment variables
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
            
            // Connection pool optimizations for cloud environments
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("MiniAppHikariPool");
            
            // Additional cloud-ready settings
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
            
            // FIXED: Create HikariCP DataSource
            dataSource = new HikariDataSource(config);
            
            System.out.println("HikariCP connection pool initialized successfully");
            System.out.println("Database URL: " + DB_URL);
            System.out.println("Pool Max Size: " + config.getMaximumPoolSize());
            System.out.println("Pool Min Idle: " + config.getMinimumIdle());
            
            // Test connection
            try (Connection testConnection = dataSource.getConnection()) {
                System.out.println("Database connection test successful");
            }
            
            // FIXED: Cache connection using environment variables
            connectToCache();
            
            // FIXED: External services using environment variables
            initializeExternalServices();
            
        } catch (SQLException e) {
            System.err.println("Database connection pool initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }
    
    private void connectToCache() {
        // FIXED: Redis connection details from environment variables
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        System.out.println("Redis configuration loaded from environment variables");
        // In production, use a proper Redis client library with connection pooling
    }
    
    private void initializeExternalServices() {
        // FIXED: External service URLs from environment variables
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
        System.out.println("External service URLs loaded from environment variables");
    }
    
    public void executeQuery(String sql) {
        // FIXED: Get connection from pool instead of using direct connection
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            // FIXED: Query timeout from environment variable
            int queryTimeout = Integer.parseInt(
                System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30")
            );
            stmt.setQueryTimeout(queryTimeout);
            
            System.out.println("Executing query: " + sql);
            stmt.execute();
            
            System.out.println("Query executed successfully (connection returned to pool)");
            
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            throw new RuntimeException("Query execution failed", e);
        }
    }
    
    public void disconnect() {
        // FIXED: Close HikariCP DataSource (closes all pooled connections)
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool closed");
        }
    }
    
    /**
     * Get a connection from the pool for advanced usage
     * @return Connection from the pool
     * @throws SQLException if connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not initialized or has been closed");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Get connection pool statistics
     */
    public void printPoolStats() {
        if (dataSource != null) {
            System.out.println("=== HikariCP Pool Statistics ===");
            System.out.println("Active Connections: " + dataSource.getHikariPoolMXBean().getActiveConnections());
            System.out.println("Idle Connections: " + dataSource.getHikariPoolMXBean().getIdleConnections());
            System.out.println("Total Connections: " + dataSource.getHikariPoolMXBean().getTotalConnections());
            System.out.println("Threads Awaiting Connection: " + dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
    }
}
