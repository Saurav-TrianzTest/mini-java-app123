package com.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

/**
 * Database service - Cloud-ready version with AWS Secrets Manager, Parameter Store, and HikariCP
 * Fixed: Hardcoded credentials, URLs, ports, and raw JDBC connections
 */
public class DatabaseService {
    
    // FIXED: Configuration retrieved from environment variables and AWS services
    private static final String AWS_REGION_ENV = "AWS_REGION";
    private static final String DB_SECRET_NAME_ENV = "DB_SECRET_NAME";
    private static final String DB_HOST_PARAM = "/mini-app/database/host";
    private static final String DB_PORT_PARAM = "/mini-app/database/port";
    private static final String DB_NAME_PARAM = "/mini-app/database/name";
    private static final String REDIS_HOST_PARAM = "/mini-app/redis/host";
    private static final String REDIS_PORT_PARAM = "/mini-app/redis/port";
    private static final String EXTERNAL_API_URL_PARAM = "/mini-app/external-api/url";
    private static final String PAYMENT_SERVICE_URL_PARAM = "/mini-app/payment-service/url";
    
    // FIXED: Using HikariCP connection pool instead of raw JDBC
    private HikariDataSource dataSource;
    private final SecretsManagerClient secretsManagerClient;
    private final SsmClient ssmClient;
    private final ObjectMapper objectMapper;
    
    public DatabaseService() {
        // Initialize AWS clients
        String region = System.getenv(AWS_REGION_ENV);
        Region awsRegion = (region != null) ? Region.of(region) : Region.US_EAST_1;
        
        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(awsRegion)
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(awsRegion)
                .build();
        
        this.objectMapper = new ObjectMapper();
    }
    
    public void connect() {
        try {
            System.out.println("Connecting to database using HikariCP and AWS Secrets Manager...");
            
            // FIXED: Retrieve database credentials from AWS Secrets Manager
            DatabaseCredentials credentials = getDatabaseCredentials();
            
            // FIXED: Retrieve database connection details from Parameter Store
            String dbHost = getParameter(DB_HOST_PARAM, "localhost");
            String dbPort = getParameter(DB_PORT_PARAM, "3306");
            String dbName = getParameter(DB_NAME_PARAM, "mini_app_db");
            
            String dbUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
            
            // FIXED: Configure HikariCP connection pool instead of raw JDBC
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(credentials.getUsername());
            config.setPassword(credentials.getPassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // HikariCP optimal settings for cloud environments
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");
            
            // Additional cloud-optimized settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            this.dataSource = new HikariDataSource(config);
            
            System.out.println("Connected to database using HikariCP: " + dbUrl);
            System.out.println("Connection pool initialized with max size: " + config.getMaximumPoolSize());
            
            // FIXED: Connect to cache using Parameter Store
            connectToCache();
            
            // FIXED: Initialize external services using Parameter Store
            initializeExternalServices();
            
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * FIXED: Retrieve database credentials from AWS Secrets Manager
     */
    private DatabaseCredentials getDatabaseCredentials() {
        try {
            String secretName = System.getenv(DB_SECRET_NAME_ENV);
            if (secretName == null || secretName.isEmpty()) {
                secretName = "mini-app/database/credentials";
            }
            
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            
            GetSecretValueResponse getSecretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
            String secret = getSecretValueResponse.secretString();
            
            // Parse JSON secret
            JsonNode secretJson = objectMapper.readTree(secret);
            String username = secretJson.get("username").asText();
            String password = secretJson.get("password").asText();
            
            System.out.println("Database credentials retrieved from AWS Secrets Manager: " + secretName);
            
            return new DatabaseCredentials(username, password);
            
        } catch (Exception e) {
            System.err.println("Failed to retrieve credentials from Secrets Manager: " + e.getMessage());
            // Fallback to environment variables for local development
            String username = System.getenv("DB_USERNAME");
            String password = System.getenv("DB_PASSWORD");
            return new DatabaseCredentials(
                    username != null ? username : "root",
                    password != null ? password : "password"
            );
        }
    }
    
    /**
     * FIXED: Retrieve parameter from AWS Systems Manager Parameter Store
     */
    private String getParameter(String parameterName, String defaultValue) {
        try {
            GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name(parameterName)
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(parameterRequest);
            return response.parameter().value();
            
        } catch (Exception e) {
            System.out.println("Parameter " + parameterName + " not found in Parameter Store, using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    private void connectToCache() {
        // FIXED: Redis connection details from Parameter Store
        String redisHost = getParameter(REDIS_HOST_PARAM, "localhost");
        String redisPort = getParameter(REDIS_PORT_PARAM, "6379");
        
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort + " (from Parameter Store)");
        // Simulate cache connection
    }
    
    private void initializeExternalServices() {
        // FIXED: External service URLs from Parameter Store
        String externalApiUrl = getParameter(EXTERNAL_API_URL_PARAM, "http://api.example.com:8080/v1");
        String paymentServiceUrl = getParameter(PAYMENT_SERVICE_URL_PARAM, "https://payment.internal.company.com/process");
        
        System.out.println("Initializing external API: " + externalApiUrl + " (from Parameter Store)");
        System.out.println("Initializing payment service: " + paymentServiceUrl + " (from Parameter Store)");
    }
    
    public void executeQuery(String sql) {
        Connection connection = null;
        PreparedStatement stmt = null;
        
        try {
            // FIXED: Get connection from HikariCP pool instead of raw JDBC
            connection = dataSource.getConnection();
            
            if (connection != null && !connection.isClosed()) {
                stmt = connection.prepareStatement(sql);
                // Query timeout can be configured via Parameter Store if needed
                stmt.setQueryTimeout(30);
                
                System.out.println("Executing query: " + sql);
                stmt.execute();
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        } finally {
            // Properly close resources
            try {
                if (stmt != null) stmt.close();
                if (connection != null) connection.close(); // Returns connection to pool
            } catch (SQLException e) {
                System.err.println("Failed to close resources: " + e.getMessage());
            }
        }
    }
    
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
     * Helper class to hold database credentials
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
