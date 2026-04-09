package com.test;

import com.test.config.CloudConfigurationManager;
import com.test.config.CloudConfigurationManager.DatabaseCredentials;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Cloud-native database service with connection pooling and externalized configuration
 * Replaces hardcoded credentials with AWS Secrets Manager
 * Replaces direct JDBC connections with HikariCP connection pool
 */
public class DatabaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    
    private HikariDataSource dataSource;
    private CloudConfigurationManager configManager;
    
    // Lazy initialization - moved from static initializer to avoid startup failures
    private volatile boolean initialized = false;
    
    /**
     * Initialize database connection pool with cloud-native configuration
     */
    public void connect() {
        if (initialized) {
            logger.info("Database service already initialized");
            return;
        }
        
        synchronized (this) {
            if (initialized) {
                return;
            }
            
            try {
                logger.info("Initializing database service with cloud-native configuration...");
                
                // Initialize cloud configuration manager
                configManager = new CloudConfigurationManager();
                
                // Get database credentials from AWS Secrets Manager
                String secretName = System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/database");
                DatabaseCredentials dbCredentials = getDatabaseCredentialsFromSecretsManager(secretName);
                
                // Configure HikariCP connection pool
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(dbCredentials.getJdbcUrl());
                config.setUsername(dbCredentials.getUsername());
                config.setPassword(dbCredentials.getPassword());
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                
                // Cloud-optimized connection pool settings
                config.setMaximumPoolSize(getMaxPoolSize());
                config.setMinimumIdle(getMinIdleConnections());
                config.setConnectionTimeout(getConnectionTimeout());
                config.setIdleTimeout(600000); // 10 minutes
                config.setMaxLifetime(1800000); // 30 minutes
                config.setLeakDetectionThreshold(60000); // 1 minute
                
                // Connection validation
                config.setConnectionTestQuery("SELECT 1");
                config.setValidationTimeout(3000);
                
                // Pool name for monitoring
                config.setPoolName("MiniAppHikariPool");
                
                // Initialize data source
                dataSource = new HikariDataSource(config);
                
                initialized = true;
                logger.info("Database service initialized successfully with connection pool");
                
                // Initialize external services with cloud-native configuration
                initializeExternalServices();
                
            } catch (Exception e) {
                logger.error("Failed to initialize database service", e);
                throw new RuntimeException("Failed to initialize database service", e);
            }
        }
    }
    
    /**
     * Get database credentials from AWS Secrets Manager with fallback to environment variables
     */
    private DatabaseCredentials getDatabaseCredentialsFromSecretsManager(String secretName) {
        try {
            // Try to get credentials from AWS Secrets Manager
            return configManager.getDatabaseCredentials(secretName);
        } catch (Exception e) {
            logger.warn("Failed to retrieve credentials from Secrets Manager, falling back to environment variables", e);
            
            // Fallback to environment variables for local development
            String host = System.getenv().getOrDefault("DB_HOST", "localhost");
            String portStr = System.getenv().getOrDefault("DB_PORT", "3306");
            String dbname = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
            String username = System.getenv().getOrDefault("DB_USERNAME", "root");
            String password = System.getenv().getOrDefault("DB_PASSWORD", "password");
            
            int port = Integer.parseInt(portStr);
            
            return new DatabaseCredentials(username, password, host, port, dbname);
        }
    }
    
    /**
     * Get maximum pool size from environment variable or Parameter Store
     */
    private int getMaxPoolSize() {
        return configManager.getConfigValueAsInt("DB_POOL_MAX_SIZE", 20);
    }
    
    /**
     * Get minimum idle connections from environment variable or Parameter Store
     */
    private int getMinIdleConnections() {
        return configManager.getConfigValueAsInt("DB_POOL_MIN_IDLE", 5);
    }
    
    /**
     * Get connection timeout from environment variable or Parameter Store
     */
    private long getConnectionTimeout() {
        return configManager.getConfigValueAsInt("DB_CONNECTION_TIMEOUT_MS", 30000);
    }
    
    /**
     * Initialize external services with cloud-native configuration
     */
    private void initializeExternalServices() {
        try {
            // Get external service URLs from Parameter Store or environment variables
            String externalApiUrl = configManager.getConfigValue("EXTERNAL_API_URL", 
                    "http://api.example.com/v1");
            String paymentServiceUrl = configManager.getConfigValue("PAYMENT_SERVICE_URL", 
                    "https://payment.internal.company.com/process");
            
            // Get cache configuration from Parameter Store or environment variables
            String redisHost = configManager.getConfigValue("REDIS_HOST", "localhost");
            int redisPort = configManager.getConfigValueAsInt("REDIS_PORT", 6379);
            
            logger.info("External API URL configured: {}", externalApiUrl);
            logger.info("Payment service URL configured: {}", paymentServiceUrl);
            logger.info("Redis cache configured: {}:{}", redisHost, redisPort);
            
        } catch (Exception e) {
            logger.error("Failed to initialize external services", e);
            // Don't throw exception - allow application to start even if external services are not configured
        }
    }
    
    /**
     * Execute SQL query using connection pool
     * @param sql The SQL query to execute
     */
    public void executeQuery(String sql) {
        if (!initialized) {
            logger.error("Database service not initialized. Call connect() first.");
            throw new IllegalStateException("Database service not initialized");
        }
        
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            // Get connection from pool
            connection = dataSource.getConnection();
            
            // Prepare statement with timeout
            stmt = connection.prepareStatement(sql);
            stmt.setQueryTimeout(30); // 30 seconds timeout
            
            logger.info("Executing query: {}", sql);
            stmt.execute();
            
            logger.info("Query executed successfully");
            
        } catch (SQLException e) {
            logger.error("Query execution failed: {}", sql, e);
            throw new RuntimeException("Query execution failed", e);
        } finally {
            // Close resources (connection returns to pool)
            closeQuietly(stmt);
            closeQuietly(connection);
        }
    }
    
    /**
     * Get a connection from the pool
     * @return Database connection
     */
    public Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new IllegalStateException("Database service not initialized");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Get the data source
     * @return HikariDataSource
     */
    public DataSource getDataSource() {
        if (!initialized) {
            throw new IllegalStateException("Database service not initialized");
        }
        return dataSource;
    }
    
    /**
     * Close statement quietly without throwing exception
     */
    private void closeQuietly(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.warn("Failed to close statement", e);
            }
        }
    }
    
    /**
     * Close connection quietly (returns to pool)
     */
    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warn("Failed to close connection", e);
            }
        }
    }
    
    /**
     * Shutdown database service and close connection pool
     */
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("Database connection pool closed");
            }
            
            if (configManager != null) {
                configManager.close();
                logger.info("Cloud configuration manager closed");
            }
            
            initialized = false;
            
        } catch (Exception e) {
            logger.error("Failed to close database service", e);
        }
    }
}
