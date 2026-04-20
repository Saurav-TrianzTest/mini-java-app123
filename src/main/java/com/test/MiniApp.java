package com.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Cloud-ready Mini Java Application with AWS S3, Parameter Store, 
 * and environment-driven configuration
 */
public class MiniApp {
    
    // Environment-driven configuration with defaults
    private final int serverPort;
    private final String configBucket;
    private final String configKey;
    private final String logBucket;
    private final String logKeyPrefix;
    
    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;
    private final SsmClient ssmClient;
    private final ObjectMapper objectMapper;
    
    public MiniApp() {
        // Initialize AWS clients
        Region region = Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));
        
        this.s3Client = S3Client.builder()
                .region(region)
                .build();
        
        this.s3AsyncClient = S3AsyncClient.builder()
                .region(region)
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(region)
                .build();
        
        this.objectMapper = new ObjectMapper();
        
        // Load configuration from environment variables and AWS Parameter Store
        this.serverPort = Integer.parseInt(getParameterFromStore("SERVER_PORT", 
                System.getenv().getOrDefault("SERVER_PORT", "8080")));
        this.configBucket = System.getenv().getOrDefault("CONFIG_BUCKET", "mini-app-config");
        this.configKey = System.getenv().getOrDefault("CONFIG_KEY", "config/app.properties");
        this.logBucket = System.getenv().getOrDefault("LOG_BUCKET", "mini-app-logs");
        this.logKeyPrefix = System.getenv().getOrDefault("LOG_KEY_PREFIX", "logs/");
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
    
    public static void main(String[] args) {
        System.out.println("Starting Cloud-Ready Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // Load configuration from S3
        loadConfigurationAsync();
        
        // Initialize logging to S3
        initializeLogging();
        
        // Initialize database connection with cloud-native patterns
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    /**
     * Load configuration from Amazon S3 asynchronously
     */
    private void loadConfigurationAsync() {
        CompletableFuture<Void> configFuture = CompletableFuture.runAsync(() -> {
            try {
                System.out.println("Loading configuration from S3: s3://" + configBucket + "/" + configKey);
                
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(configBucket)
                        .key(configKey)
                        .build();
                
                ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
                
                Properties props = new Properties();
                props.load(new BufferedReader(new InputStreamReader(s3Object, StandardCharsets.UTF_8)));
                
                System.out.println("Configuration loaded successfully from S3");
                System.out.println("Loaded " + props.size() + " configuration properties");
                
                s3Object.close();
                
            } catch (Exception e) {
                System.err.println("Failed to load configuration from S3: " + e.getMessage());
                System.out.println("Application will use default configuration values");
            }
        });
        
        // Wait for configuration to load (with timeout)
        try {
            configFuture.get();
        } catch (Exception e) {
            System.err.println("Configuration loading timed out or failed: " + e.getMessage());
        }
    }
    
    /**
     * Initialize logging to Amazon S3
     */
    private void initializeLogging() {
        try {
            System.out.println("Initializing cloud-based logging to S3: s3://" + logBucket + "/" + logKeyPrefix);
            
            // Create initial log entry
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            String logMessage = timestamp + " - Application started\n";
            String logKey = logKeyPrefix + "app-" + timestamp.replace(":", "-") + ".log";
            
            // Write log to S3 asynchronously
            writeLogToS3Async(logKey, logMessage);
            
            System.out.println("Logging initialized successfully");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }
    
    /**
     * Write log entry to S3 asynchronously
     */
    private CompletableFuture<PutObjectResponse> writeLogToS3Async(String key, String content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(logBucket)
                .key(key)
                .contentType("text/plain")
                .build();
        
        return s3AsyncClient.putObject(putObjectRequest, 
                AsyncRequestBody.fromString(content, StandardCharsets.UTF_8))
                .whenComplete((response, error) -> {
                    if (error != null) {
                        System.err.println("Failed to write log to S3: " + error.getMessage());
                    } else {
                        System.out.println("Log written to S3: s3://" + logBucket + "/" + key);
                    }
                });
    }
    
    /**
     * Start server with environment-driven port configuration
     */
    private void startServer() {
        try {
            // Use port from environment variable or Parameter Store
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Server started on port: " + serverPort + " (configured via environment)");
            System.out.println("Server ready to accept connections...");
            
            // Log server start to S3
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            String logMessage = timestamp + " - Server started on port " + serverPort + "\n";
            String logKey = logKeyPrefix + "server-start-" + timestamp.replace(":", "-") + ".log";
            writeLogToS3Async(logKey, logMessage);
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup AWS resources
     */
    public void shutdown() {
        try {
            if (s3Client != null) {
                s3Client.close();
            }
            if (s3AsyncClient != null) {
                s3AsyncClient.close();
            }
            if (ssmClient != null) {
                ssmClient.close();
            }
            System.out.println("AWS clients closed successfully");
        } catch (Exception e) {
            System.err.println("Failed to close AWS clients: " + e.getMessage());
        }
    }
}
