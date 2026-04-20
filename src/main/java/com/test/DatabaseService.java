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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Cloud-ready Database service with AWS Secrets Manager, Parameter Store, 
 * HikariCP connection pooling, and asynchronous operations
 */
public class DatabaseService {
    
    // Environment-driven configuration with defaults
    private final String dbSecretName;
    private final String dbHost;
    private final String dbPort;
    private final String dbName;
    private final String redisHost;
    private final int redisPort;
    private final String externalApiUrl;
    private final String paymentServiceUrl;
    
    private HikariDataSource dataSource;
    private final SecretsManagerClient secretsManagerClient;
    private final SsmClient ssmClient;
    private final ObjectMapper objectMapper;
    
    public DatabaseService() {
        // Initialize AWS clients with proper timeout configurations
        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
        
        this.objectMapper = new ObjectMapper();
        
        // Load configuration from environment variables and AWS Parameter Store
        this.dbSecretName = System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/database/credentials");
        this.dbHost = getParameterFromStore("DB_HOST", "localhost");
        this.dbPort = getParameterFromStore("DB_PORT", "3306");
        this.dbName = getParameterFromStore("DB_NAME", "mini_app_db");
        this.redisHost = getParameterFromStore("REDIS_HOST", "127.0.0.1");
        this.redisPort = Integer.parseInt(getParameterFromStore("REDIS_PORT", "6379"));
        this.externalApiUrl = getParameterFromStore("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
        this.paymentServiceUrl = getParameterFromStore("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");
    }
    
    /**
     * Retrieve parameter from AWS Systems Manager Parameter Store
     */
    private String getParameterFromStore(String parameterName, String defaultValue) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name("/mini-app/" + parameterName)
                    .withDecryption(true)
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            System.err.println("Failed to retrieve parameter " + parameterName + " from Parameter Store, using default: " + e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Retrieve database credentials from AWS Secrets Manager
     */
    private DatabaseCredentials getCredentialsFromSecretsManager() {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(dbSecretName)
                    .build();
            
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            String secretString = response.secretString();
            
            // Parse JSON secret
            JsonNode secretJson = objectMapper.readTree(secretString);
            String username = secretJson.get("username").asText();
            String password = secretJson.get("password").asText();
            
            return new DatabaseCredentials(username, password);
        } catch (Exception e) {
            System.err.println("Failed to retrieve credentials from Secrets Manager: " + e.getMessage());
            throw new RuntimeException("Unable to retrieve database credentials", e);
        }
    }
    
    /**
     * Initialize HikariCP connection pool with proper timeout configurations
     */
    public void connect() {
        try {
            System.out.println("Connecting to database with HikariCP connection pool...");
            
            // Retrieve credentials from AWS Secrets Manager
            DatabaseCredentials credentials = getCredentialsFromSecretsManager();
            
            // Configure HikariCP with cloud-optimized settings
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName);
            config.setUsername(credentials.getUsername());
            config.setPassword(credentials.getPassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Connection pool settings optimized for cloud environments
            config.setMaximumPoolSize(Integer.parseInt(getParameterFromStore("DB_POOL_MAX_SIZE", "20")));
            config.setMinimumIdle(Integer.parseInt(getParameterFromStore("DB_POOL_MIN_IDLE", "5")));
            config.setConnectionTimeout(Duration.ofSeconds(30).toMillis());
            config.setIdleTimeout(Duration.ofMinutes(10).toMillis());
            config.setMaxLifetime(Duration.ofMinutes(30).toMillis());
            config.setLeakDetectionThreshold(Duration.ofSeconds(60).toMillis());
            
            // Additional connection properties for cloud reliability
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
            
            // Initialize the data source
            this.dataSource = new HikariDataSource(config);
            
            System.out.println("Connected to database with HikariCP: " + dbHost + ":" + dbPort + "/" + dbName);
            
            // Initialize cache and external services
            connectToCache();
            initializeExternalServices();
            
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }
    
    private void connectToCache() {
        // Cloud-ready cache connection using environment variables
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort);
        // In production, use AWS ElastiCache with proper client configuration
    }
    
    private void initializeExternalServices() {
        // Cloud-ready external service URLs from Parameter Store
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
                
                // Configure query timeout from environment
                int queryTimeout = Integer.parseInt(getParameterFromStore("DB_QUERY_TIMEOUT", "30"));
                stmt.setQueryTimeout(queryTimeout);
                
                System.out.println("Executing query asynchronously: " + sql);
                stmt.execute();
                
            } catch (SQLException e) {
                System.err.println("Async query execution failed: " + e.getMessage());
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
            
            // Configure query timeout from environment
            int queryTimeout = Integer.parseInt(getParameterFromStore("DB_QUERY_TIMEOUT", "30"));
            stmt.setQueryTimeout(queryTimeout);
            
            System.out.println("Executing query: " + sql);
            stmt.execute();
            
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            throw new RuntimeException("Query execution failed", e);
        }
    }
    
    /**
     * Get DataSource for advanced usage
     */
    public DataSource getDataSource() {
        return dataSource;
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
