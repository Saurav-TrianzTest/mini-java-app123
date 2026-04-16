package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Cloud-ready Database service using HikariCP connection pool, AWS Secrets Manager,
 * and AWS Systems Manager Parameter Store for configuration
 */
public class DatabaseService {
    
    // Environment variables for AWS configuration
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    private static final String DB_SECRET_NAME = System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/database");
    private static final String DB_HOST_PARAM = System.getenv().getOrDefault("DB_HOST_PARAM", "/mini-app/database/host");
    private static final String DB_PORT_PARAM = System.getenv().getOrDefault("DB_PORT_PARAM", "/mini-app/database/port");
    private static final String DB_NAME_PARAM = System.getenv().getOrDefault("DB_NAME_PARAM", "/mini-app/database/name");
    private static final String REDIS_HOST_PARAM = System.getenv().getOrDefault("REDIS_HOST_PARAM", "/mini-app/redis/host");
    private static final String REDIS_PORT_PARAM = System.getenv().getOrDefault("REDIS_PORT_PARAM", "/mini-app/redis/port");
    
    private HikariDataSource dataSource;
    private SecretsManagerClient secretsManagerClient;
    private SsmClient ssmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public DatabaseService() {
        // Initialize AWS clients
        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
    
    public void connect() {
        try {
            System.out.println("Connecting to database using AWS Secrets Manager and Parameter Store...");
            
            // Retrieve database credentials from AWS Secrets Manager
            DatabaseCredentials credentials = retrieveDatabaseCredentials();
            
            // Retrieve database connection parameters from AWS Systems Manager Parameter Store
            String dbHost = getParameter(DB_HOST_PARAM);
            String dbPort = getParameter(DB_PORT_PARAM);
            String dbName = getParameter(DB_NAME_PARAM);
            
            // Build connection URL
            String dbUrl = String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, dbName);
            
            // Configure HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(credentials.getUsername());
            config.setPassword(credentials.getPassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Connection pool settings for cloud environments
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30));
            config.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
            config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));
            config.setConnectionTestQuery("SELECT 1");
            config.setAutoCommit(true);
            
            // Additional cloud-ready settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            // Initialize HikariCP data source
            dataSource = new HikariDataSource(config);
            
            System.out.println("Connected to database using HikariCP connection pool");
            System.out.println("Database URL: " + dbUrl);
            
            // Connect to cache using externalized configuration
            connectToCache();
            
            // Initialize external services using externalized configuration
            initializeExternalServices();
            
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }
    
    /**
     * Retrieve database credentials from AWS Secrets Manager
     */
    private DatabaseCredentials retrieveDatabaseCredentials() {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(DB_SECRET_NAME)
                    .build();
            
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            String secretString = response.secretString();
            
            // Parse JSON secret
            JsonNode secretJson = objectMapper.readTree(secretString);
            String username = secretJson.get("username").asText();
            String password = secretJson.get("password").asText();
            
            return new DatabaseCredentials(username, password);
            
        } catch (Exception e) {
            System.err.println("Failed to retrieve database credentials from Secrets Manager: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve database credentials", e);
        }
    }
    
    /**
     * Retrieve parameter from AWS Systems Manager Parameter Store
     */
    private String getParameter(String parameterName) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
            
        } catch (Exception e) {
            System.err.println("Failed to retrieve parameter " + parameterName + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve parameter: " + parameterName, e);
        }
    }
    
    private void connectToCache() {
        // Retrieve Redis configuration from Parameter Store
        String redisHost = getParameter(REDIS_HOST_PARAM);
        String redisPort = getParameter(REDIS_PORT_PARAM);
        
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort);
        // Simulate cache connection with externalized configuration
    }
    
    private void initializeExternalServices() {
        // Retrieve external service URLs from Parameter Store
        String externalApiUrl = getParameter("/mini-app/external-api/url");
        String paymentServiceUrl = getParameter("/mini-app/payment-service/url");
        
        System.out.println("Initializing external API: " + externalApiUrl);
        System.out.println("Initializing payment service: " + paymentServiceUrl);
    }
    
    /**
     * Execute query asynchronously using CompletableFuture
     */
    public CompletableFuture<Void> executeQueryAsync(String sql) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                // Set query timeout for cloud environments
                stmt.setQueryTimeout(30);
                
                System.out.println("Executing query asynchronously: " + sql);
                stmt.execute();
                
            } catch (SQLException e) {
                System.err.println("Query execution failed: " + e.getMessage());
                throw new RuntimeException("Query execution failed", e);
            }
        });
    }
    
    /**
     * Execute query synchronously (for backward compatibility)
     */
    public void executeQuery(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            // Set query timeout for cloud environments
            stmt.setQueryTimeout(30);
            
            System.out.println("Executing query: " + sql);
            stmt.execute();
            
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            throw new RuntimeException("Query execution failed", e);
        }
    }
    
    public void disconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                System.out.println("Database connection pool closed");
            }
            
            if (secretsManagerClient != null) {
                secretsManagerClient.close();
            }
            
            if (ssmClient != null) {
                ssmClient.close();
            }
            
        } catch (Exception e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
    
    /**
     * Inner class to hold database credentials
     */
    private static class DatabaseCredentials {
        private final String username;
        private final String password;
        
        public DatabaseCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
    }
}
