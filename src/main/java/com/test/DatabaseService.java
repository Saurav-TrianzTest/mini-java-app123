package com.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Database service - Cloud-Ready Version
 * Fixed to use HikariCP connection pooling, AWS Secrets Manager for credentials,
 * AWS Parameter Store for configuration, and proper timeout management
 */
public class DatabaseService {
    
    // Cloud-ready: Configuration from environment variables
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    private static final String DB_SECRET_NAME = System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/database/credentials");
    private static final String DB_HOST_PARAM = System.getenv().getOrDefault("DB_HOST_PARAM", "/mini-app/database/host");
    private static final String DB_PORT_PARAM = System.getenv().getOrDefault("DB_PORT_PARAM", "/mini-app/database/port");
    private static final String DB_NAME_PARAM = System.getenv().getOrDefault("DB_NAME_PARAM", "/mini-app/database/name");
    
    // Cloud-ready: Redis configuration from environment variables
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "redis.example.com");
    private static final String REDIS_PORT = System.getenv().getOrDefault("REDIS_PORT", "6379");
    
    // Cloud-ready: External service URLs from environment variables
    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.example.com/process");
    
    // Cloud-ready: Connection timeout from environment variable (in seconds)
    private static final int CONNECTION_TIMEOUT = Integer.parseInt(
        System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT", "30")
    );
    
    // Cloud-ready: HikariCP DataSource for connection pooling
    private HikariDataSource dataSource;
    private final SecretsManagerClient secretsManagerClient;
    private final SsmClient ssmClient;
    private final ObjectMapper objectMapper;
    
    public DatabaseService() {
        // Initialize AWS clients
        this.secretsManagerClient = SecretsManagerClient.builder()
            .region(Region.of(AWS_REGION))
            .build();
        
        this.ssmClient = SsmClient.builder()
            .region(Region.of(AWS_REGION))
            .build();
        
        this.objectMapper = new ObjectMapper();
    }
    
    public void connect() {
        try {
            System.out.println("Initializing cloud-ready database connection with HikariCP...");
            
            // Cloud-ready: Retrieve database credentials from AWS Secrets Manager
            DatabaseCredentials credentials = retrieveDatabaseCredentials();
            
            // Cloud-ready: Retrieve database configuration from AWS Parameter Store
            DatabaseConfig config = retrieveDatabaseConfig();
            
            // Cloud-ready: Initialize HikariCP connection pool
            initializeConnectionPool(credentials, config);
            
            System.out.println("Connected to database using HikariCP connection pool");
            System.out.println("Database host: " + config.host + " (from Parameter Store)");
            
            // Cloud-ready: Initialize cache connection with environment variables
            connectToCache();
            
            // Cloud-ready: Initialize external services with environment variables
            initializeExternalServices();
            
        } catch (Exception e) {
            System.err.println("Database connection initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cloud-ready: Retrieve database credentials from AWS Secrets Manager
     */
    private DatabaseCredentials retrieveDatabaseCredentials() {
        try {
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(DB_SECRET_NAME)
                .build();
            
            GetSecretValueResponse getSecretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
            String secretString = getSecretValueResponse.secretString();
            
            // Parse JSON secret
            JsonNode secretJson = objectMapper.readTree(secretString);
            
            DatabaseCredentials credentials = new DatabaseCredentials();
            credentials.username = secretJson.get("username").asText();
            credentials.password = secretJson.get("password").asText();
            
            System.out.println("Database credentials retrieved from AWS Secrets Manager: " + DB_SECRET_NAME);
            
            return credentials;
            
        } catch (Exception e) {
            System.err.println("Failed to retrieve credentials from Secrets Manager: " + e.getMessage());
            // Fallback to environment variables
            DatabaseCredentials credentials = new DatabaseCredentials();
            credentials.username = System.getenv().getOrDefault("DB_USERNAME", "app_user");
            credentials.password = System.getenv().getOrDefault("DB_PASSWORD", "");
            System.out.println("Using database credentials from environment variables (fallback)");
            return credentials;
        }
    }
    
    /**
     * Cloud-ready: Retrieve database configuration from AWS Parameter Store
     */
    private DatabaseConfig retrieveDatabaseConfig() {
        DatabaseConfig config = new DatabaseConfig();
        
        try {
            // Retrieve host
            config.host = getParameter(DB_HOST_PARAM, "localhost");
            
            // Retrieve port
            config.port = getParameter(DB_PORT_PARAM, "3306");
            
            // Retrieve database name
            config.name = getParameter(DB_NAME_PARAM, "mini_app_db");
            
            System.out.println("Database configuration retrieved from AWS Parameter Store");
            
        } catch (Exception e) {
            System.err.println("Failed to retrieve configuration from Parameter Store: " + e.getMessage());
            // Use environment variable fallbacks
            config.host = System.getenv().getOrDefault("DB_HOST", "localhost");
            config.port = System.getenv().getOrDefault("DB_PORT", "3306");
            config.name = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
            System.out.println("Using database configuration from environment variables (fallback)");
        }
        
        return config;
    }
    
    /**
     * Helper method to retrieve parameter from Parameter Store
     */
    private String getParameter(String parameterName, String defaultValue) {
        try {
            GetParameterRequest parameterRequest = GetParameterRequest.builder()
                .name(parameterName)
                .withDecryption(true)
                .build();
            
            GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
            return parameterResponse.parameter().value();
            
        } catch (Exception e) {
            System.err.println("Failed to retrieve parameter " + parameterName + ": " + e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Cloud-ready: Initialize HikariCP connection pool with proper configuration
     */
    private void initializeConnectionPool(DatabaseCredentials credentials, DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        // Build JDBC URL
        String jdbcUrl = "jdbc:mysql://" + config.host + ":" + config.port + "/" + config.name;
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(credentials.username);
        hikariConfig.setPassword(credentials.password);
        
        // Cloud-ready: Configure connection pool settings
        hikariConfig.setMaximumPoolSize(Integer.parseInt(
            System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10")
        ));
        hikariConfig.setMinimumIdle(Integer.parseInt(
            System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "2")
        ));
        
        // Cloud-ready: Configure connection timeout (in milliseconds)
        hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT));
        
        // Cloud-ready: Configure idle timeout
        hikariConfig.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
        
        // Cloud-ready: Configure max lifetime
        hikariConfig.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));
        
        // Cloud-ready: Configure connection test query
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        // Cloud-ready: Configure pool name for monitoring
        hikariConfig.setPoolName("MiniAppHikariPool");
        
        // Initialize the data source
        this.dataSource = new HikariDataSource(hikariConfig);
        
        System.out.println("HikariCP connection pool initialized with timeout: " + CONNECTION_TIMEOUT + "s");
    }
    
    private void connectToCache() {
        // Cloud-ready: Use Redis connection details from environment variables
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT + " (from environment variables)");
        // Simulate cache connection
    }
    
    private void initializeExternalServices() {
        // Cloud-ready: Use external service URLs from environment variables
        System.out.println("Initializing external API: " + EXTERNAL_API_URL + " (from environment variables)");
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL + " (from environment variables)");
    }
    
    /**
     * Cloud-ready: Execute query using HikariCP connection pool with async pattern
     */
    public CompletableFuture<Void> executeQueryAsync(String sql) {
        return CompletableFuture.runAsync(() -> {
            Connection connection = null;
            PreparedStatement stmt = null;
            
            try {
                // Cloud-ready: Get connection from HikariCP pool
                connection = dataSource.getConnection();
                
                stmt = connection.prepareStatement(sql);
                
                // Cloud-ready: Configure query timeout from environment variable
                int queryTimeout = Integer.parseInt(
                    System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30")
                );
                stmt.setQueryTimeout(queryTimeout);
                
                System.out.println("Executing query asynchronously: " + sql);
                stmt.execute();
                
            } catch (SQLException e) {
                System.err.println("Query execution failed: " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                // Cloud-ready: Properly close resources (connection returns to pool)
                closeResources(stmt, connection);
            }
        });
    }
    
    /**
     * Synchronous query execution for backward compatibility
     */
    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            // Cloud-ready: Get connection from HikariCP pool
            connection = dataSource.getConnection();
            
            stmt = connection.prepareStatement(sql);
            
            // Cloud-ready: Configure query timeout from environment variable
            int queryTimeout = Integer.parseInt(
                System.getenv().getOrDefault("DB_QUERY_TIMEOUT", "30")
            );
            stmt.setQueryTimeout(queryTimeout);
            
            System.out.println("Executing query: " + sql);
            stmt.execute();
            
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        } finally {
            // Cloud-ready: Properly close resources (connection returns to pool)
            closeResources(stmt, connection);
        }
    }
    
    /**
     * Helper method to close resources properly
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
    
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                System.out.println("HikariCP connection pool closed");
            }
            
            // Close AWS clients
            if (secretsManagerClient != null) {
                secretsManagerClient.close();
            }
            if (ssmClient != null) {
                ssmClient.close();
            }
            
        } catch (Exception e) {
            System.err.println("Failed to close database connection pool: " + e.getMessage());
        }
    }
    
    /**
     * Inner class to hold database credentials
     */
    private static class DatabaseCredentials {
        String username;
        String password;
    }
    
    /**
     * Inner class to hold database configuration
     */
    private static class DatabaseConfig {
        String host;
        String port;
        String name;
    }
}
