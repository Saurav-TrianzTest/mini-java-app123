package com.test.config;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud-native configuration manager for AWS Secrets Manager and Parameter Store
 * Replaces hardcoded credentials and configuration values
 */
public class CloudConfigurationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CloudConfigurationManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final SecretsManagerClient secretsManagerClient;
    private final SsmClient ssmClient;
    private final Region region;
    
    // Cache for configuration values to reduce API calls
    private final Map<String, String> configCache = new HashMap<>();
    
    public CloudConfigurationManager() {
        // Get AWS region from environment variable or default to us-east-1
        String regionName = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        this.region = Region.of(regionName);
        
        // Initialize AWS SDK clients with proper timeouts
        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(region)
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5)))
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(region)
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5)))
                .build();
        
        logger.info("CloudConfigurationManager initialized for region: {}", regionName);
    }
    
    /**
     * Retrieve secret from AWS Secrets Manager
     * @param secretName The name of the secret in Secrets Manager
     * @return The secret value as a string
     */
    public String getSecret(String secretName) {
        try {
            // Check cache first
            if (configCache.containsKey(secretName)) {
                return configCache.get(secretName);
            }
            
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            String secretValue = response.secretString();
            
            // Cache the value
            configCache.put(secretName, secretValue);
            
            logger.info("Successfully retrieved secret: {}", secretName);
            return secretValue;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve secret: {}", secretName, e);
            throw new RuntimeException("Failed to retrieve secret: " + secretName, e);
        }
    }
    
    /**
     * Retrieve database credentials from AWS Secrets Manager
     * Expected JSON format: {"username": "...", "password": "...", "host": "...", "port": "...", "dbname": "..."}
     */
    public DatabaseCredentials getDatabaseCredentials(String secretName) {
        try {
            String secretJson = getSecret(secretName);
            JsonNode jsonNode = objectMapper.readTree(secretJson);
            
            return new DatabaseCredentials(
                    jsonNode.get("username").asText(),
                    jsonNode.get("password").asText(),
                    jsonNode.get("host").asText(),
                    jsonNode.get("port").asInt(),
                    jsonNode.get("dbname").asText()
            );
            
        } catch (Exception e) {
            logger.error("Failed to parse database credentials from secret: {}", secretName, e);
            throw new RuntimeException("Failed to parse database credentials", e);
        }
    }
    
    /**
     * Retrieve parameter from AWS Systems Manager Parameter Store
     * @param parameterName The name of the parameter
     * @return The parameter value
     */
    public String getParameter(String parameterName) {
        try {
            // Check cache first
            if (configCache.containsKey(parameterName)) {
                return configCache.get(parameterName);
            }
            
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            String parameterValue = response.parameter().value();
            
            // Cache the value
            configCache.put(parameterName, parameterValue);
            
            logger.info("Successfully retrieved parameter: {}", parameterName);
            return parameterValue;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve parameter: {}", parameterName, e);
            throw new RuntimeException("Failed to retrieve parameter: " + parameterName, e);
        }
    }
    
    /**
     * Get configuration value with fallback to environment variable
     */
    public String getConfigValue(String key, String defaultValue) {
        // First try environment variable
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        
        // Then try Parameter Store (with /mini-app/ prefix)
        try {
            return getParameter("/mini-app/" + key);
        } catch (Exception e) {
            logger.warn("Failed to retrieve parameter for key: {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get integer configuration value with fallback
     */
    public int getConfigValueAsInt(String key, int defaultValue) {
        try {
            String value = getConfigValue(key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse integer for key: {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Close AWS SDK clients
     */
    public void close() {
        if (secretsManagerClient != null) {
            secretsManagerClient.close();
        }
        if (ssmClient != null) {
            ssmClient.close();
        }
        logger.info("CloudConfigurationManager closed");
    }
    
    /**
     * Database credentials holder
     */
    public static class DatabaseCredentials {
        private final String username;
        private final String password;
        private final String host;
        private final int port;
        private final String dbname;
        
        public DatabaseCredentials(String username, String password, String host, int port, String dbname) {
            this.username = username;
            this.password = password;
            this.host = host;
            this.port = port;
            this.dbname = dbname;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public String getHost() {
            return host;
        }
        
        public int getPort() {
            return port;
        }
        
        public String getDbname() {
            return dbname;
        }
        
        public String getJdbcUrl() {
            return String.format("jdbc:mysql://%s:%d/%s", host, port, dbname);
        }
    }
}
