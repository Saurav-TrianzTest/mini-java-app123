package com.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS Configuration Manager for retrieving secrets and parameters from AWS cloud services
 */
public class AwsConfigurationManager {
    
    private final SecretsManagerClient secretsManagerClient;
    private final SsmClient ssmClient;
    private final Gson gson;
    private final Map<String, String> cachedSecrets;
    private final Map<String, String> cachedParameters;
    
    public AwsConfigurationManager() {
        // Initialize AWS clients with default region from environment variable
        String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        
        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(Region.of(region))
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(Region.of(region))
                .build();
        
        this.gson = new Gson();
        this.cachedSecrets = new HashMap<>();
        this.cachedParameters = new HashMap<>();
    }
    
    /**
     * Retrieve a secret from AWS Secrets Manager
     * @param secretName The name of the secret in AWS Secrets Manager
     * @return The secret value as a string
     */
    public String getSecret(String secretName) {
        if (cachedSecrets.containsKey(secretName)) {
            return cachedSecrets.get(secretName);
        }
        
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            String secretValue = response.secretString();
            
            cachedSecrets.put(secretName, secretValue);
            return secretValue;
        } catch (Exception e) {
            System.err.println("Failed to retrieve secret '" + secretName + "': " + e.getMessage());
            // Fallback to environment variable if AWS Secrets Manager is not available
            return System.getenv(secretName);
        }
    }
    
    /**
     * Retrieve a database secret from AWS Secrets Manager and parse it
     * @param secretName The name of the database secret
     * @return Map containing database connection details
     */
    public Map<String, String> getDatabaseSecret(String secretName) {
        String secretJson = getSecret(secretName);
        Map<String, String> dbConfig = new HashMap<>();
        
        try {
            JsonObject jsonObject = gson.fromJson(secretJson, JsonObject.class);
            dbConfig.put("username", jsonObject.get("username").getAsString());
            dbConfig.put("password", jsonObject.get("password").getAsString());
            dbConfig.put("host", jsonObject.get("host").getAsString());
            dbConfig.put("port", jsonObject.get("port").getAsString());
            dbConfig.put("dbname", jsonObject.get("dbname").getAsString());
        } catch (Exception e) {
            System.err.println("Failed to parse database secret: " + e.getMessage());
            // Fallback to environment variables
            dbConfig.put("username", System.getenv().getOrDefault("DB_USERNAME", "root"));
            dbConfig.put("password", System.getenv().getOrDefault("DB_PASSWORD", "password"));
            dbConfig.put("host", System.getenv().getOrDefault("DB_HOST", "localhost"));
            dbConfig.put("port", System.getenv().getOrDefault("DB_PORT", "3306"));
            dbConfig.put("dbname", System.getenv().getOrDefault("DB_NAME", "mini_app_db"));
        }
        
        return dbConfig;
    }
    
    /**
     * Retrieve a parameter from AWS Systems Manager Parameter Store
     * @param parameterName The name of the parameter
     * @return The parameter value
     */
    public String getParameter(String parameterName) {
        if (cachedParameters.containsKey(parameterName)) {
            return cachedParameters.get(parameterName);
        }
        
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            String parameterValue = response.parameter().value();
            
            cachedParameters.put(parameterName, parameterValue);
            return parameterValue;
        } catch (Exception e) {
            System.err.println("Failed to retrieve parameter '" + parameterName + "': " + e.getMessage());
            // Fallback to environment variable
            return System.getenv(parameterName);
        }
    }
    
    /**
     * Get parameter with default value
     * @param parameterName The parameter name
     * @param defaultValue Default value if parameter not found
     * @return The parameter value or default
     */
    public String getParameter(String parameterName, String defaultValue) {
        String value = getParameter(parameterName);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
    
    /**
     * Close AWS clients
     */
    public void close() {
        if (secretsManagerClient != null) {
            secretsManagerClient.close();
        }
        if (ssmClient != null) {
            ssmClient.close();
        }
    }
}
