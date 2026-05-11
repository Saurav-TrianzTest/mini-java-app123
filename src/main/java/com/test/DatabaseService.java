package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Database service with cloud-native configuration using HikariCP connection pooling,
 * AWS Secrets Manager for credentials, and AWS Parameter Store for configuration
 */
public class DatabaseService {
    
    private HikariDataSource dataSource;
    private AwsConfigurationManager configManager;
    
    // Cloud-native configuration using environment variables and AWS services
    private String dbHost;
    private String dbPort;
    private String dbName;
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    
    // External service URLs from Parameter Store
    private String redisHost;
    private int redisPort;
    private String externalApiUrl;
    private String paymentServiceUrl;
    
    public DatabaseService() {
        this.configManager = new AwsConfigurationManager();
        loadConfiguration();
    }
    
    /**
     * Load configuration from AWS Secrets Manager and Parameter Store
     */
    private void loadConfiguration() {
        // Retrieve database credentials from AWS Secrets Manager
        // Secret name from environment variable or default
        String dbSecretName = System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/database");
        Map<String, String> dbSecret = configManager.getDatabaseSecret(dbSecretName);
        
        this.dbUsername = dbSecret.get("username");
        this.dbPassword = dbSecret.get("password");
        this.dbHost = dbSecret.get("host");
        this.dbPort = dbSecret.get("port");
        this.dbName = dbSecret.get("dbname");
        this.dbUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
        
        // Retrieve other configuration from AWS Parameter Store
        this.redisHost = configManager.getParameter("/mini-app/redis/host", "localhost");
        this.redisPort = Integer.parseInt(configManager.getParameter("/mini-app/redis/port", "6379"));
        this.externalApiUrl = configManager.getParameter("/mini-app/external-api/url", "http://api.example.com:8080/v1");
        this.paymentServiceUrl = configManager.getParameter("/mini-app/payment-service/url", "https://payment.internal.company.com/process");
    }
    
    /**
     * Initialize HikariCP connection pool with cloud-optimized settings
     */
    public void connect() {
        try {
            System.out.println("Initializing HikariCP connection pool...");
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Cloud-optimized connection pool settings
            config.setMaximumPoolSize(Integer.parseInt(
                    configManager.getParameter("/mini-app/db/pool-size", "20")));
            config.setMinimumIdle(Integer.parseInt(
                    configManager.getParameter("/mini-app/db/min-idle", "5")));
            config.setConnectionTimeout(Long.parseLong(
                    configManager.getParameter("/mini-app/db/connection-timeout", "30000")));
            config.setIdleTimeout(Long.parseLong(
                    configManager.getParameter("/mini-app/db/idle-timeout", "600000")));
            config.setMaxLifetime(Long.parseLong(
                    configManager.getParameter("/mini-app/db/max-lifetime", "1800000")));
            
            // Connection test query
            config.setConnectionTestQuery("SELECT 1");
            
            // Pool name for monitoring
            config.setPoolName("MiniAppHikariPool");
            
            // Initialize the data source
            this.dataSource = new HikariDataSource(config);
            
            System.out.println("Connected to database using HikariCP: " + dbUrl);
            System.out.println("Connection pool initialized with max size: " + config.getMaximumPoolSize());
            
            // Initialize other cloud services
            connectToCache();
            initializeExternalServices();
            
        } catch (Exception e) {
            System.err.println("Database connection pool initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void connectToCache() {
        // Cloud-native cache connection using environment variables
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort);
        // In production, use AWS ElastiCache for Redis with proper client library
    }
    
    private void initializeExternalServices() {
        // Cloud-native external service URLs from Parameter Store
        System.out.println("Initializing external API: " + externalApiUrl);
        System.out.println("Initializing payment service: " + paymentServiceUrl);
    }
    
    /**
     * Execute query with connection from pool and proper timeout configuration
     */
    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            // Get connection from HikariCP pool
            connection = dataSource.getConnection();
            
            if (connection != null && !connection.isClosed()) {
                stmt = connection.prepareStatement(sql);
                
                // Query timeout from Parameter Store or environment variable
                int queryTimeout = Integer.parseInt(
                        configManager.getParameter("/mini-app/db/query-timeout", "30"));
                stmt.setQueryTimeout(queryTimeout);
                
                System.out.println("Executing query: " + sql);
                stmt.execute();
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Properly close resources
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (connection != null) {
                    connection.close(); // Returns connection to pool
                }
            } catch (SQLException e) {
                System.err.println("Failed to close resources: " + e.getMessage());
            }
        }
    }
    
    /**
     * Shutdown the connection pool
     */
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                System.out.println("HikariCP connection pool closed");
            }
            
            if (configManager != null) {
                configManager.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to close connection pool: " + e.getMessage());
        }
    }
}
