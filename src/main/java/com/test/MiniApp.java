package com.test;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Cloud-ready Mini Java Application using AWS S3 for storage, 
 * AWS Systems Manager Parameter Store for configuration,
 * and environment variables for port configuration
 */
public class MiniApp {
    
    // Environment variables for cloud-native configuration
    private static final int SERVER_PORT = Integer.parseInt(
            System.getenv().getOrDefault("SERVER_PORT", "8080"));
    
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "mini-app-config-bucket");
    private static final String CONFIG_S3_KEY = System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties");
    private static final String LOG_S3_KEY_PREFIX = System.getenv().getOrDefault("LOG_S3_KEY_PREFIX", "logs/");
    
    private S3Client s3Client;
    private SsmClient ssmClient;
    
    public static void main(String[] args) {
        System.out.println("Starting Cloud-Ready Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    public MiniApp() {
        // Initialize AWS clients
        this.s3Client = S3Client.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
    
    private void initializeApplication() {
        // Load configuration from AWS S3 and Parameter Store
        loadConfiguration();
        
        // Initialize logging to AWS S3
        initializeLogging();
        
        // Initialize database connection with externalized configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    /**
     * Load configuration from AWS S3 instead of hardcoded file paths
     */
    private void loadConfiguration() {
        try {
            System.out.println("Loading configuration from AWS S3...");
            
            // Retrieve configuration file from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(CONFIG_S3_KEY)
                    .build();
            
            // Load configuration asynchronously
            CompletableFuture<Properties> configFuture = CompletableFuture.supplyAsync(() -> {
                try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
                    Properties props = new Properties();
                    props.load(inputStream);
                    return props;
                } catch (IOException e) {
                    System.err.println("Failed to load configuration from S3: " + e.getMessage());
                    return new Properties();
                }
            });
            
            // Wait for configuration to load
            Properties props = configFuture.get();
            System.out.println("Configuration loaded from S3: " + S3_BUCKET_NAME + "/" + CONFIG_S3_KEY);
            System.out.println("Loaded " + props.size() + " configuration properties");
            
            // Also load configuration from AWS Systems Manager Parameter Store
            loadParameterStoreConfiguration();
            
        } catch (Exception e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            // Fallback to default configuration
            System.out.println("Using default configuration");
        }
    }
    
    /**
     * Load configuration from AWS Systems Manager Parameter Store
     */
    private void loadParameterStoreConfiguration() {
        try {
            String serverPortParam = getParameter("/mini-app/server/port");
            System.out.println("Server port from Parameter Store: " + serverPortParam);
            
            String appEnvironment = getParameter("/mini-app/environment");
            System.out.println("Application environment: " + appEnvironment);
            
        } catch (Exception e) {
            System.err.println("Failed to load Parameter Store configuration: " + e.getMessage());
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
            return null;
        }
    }
    
    /**
     * Initialize logging to AWS S3 instead of hardcoded file paths
     */
    private void initializeLogging() {
        try {
            System.out.println("Initializing cloud-native logging to AWS S3...");
            
            // Create initial log entry
            String logEntry = "Application started at: " + java.time.Instant.now().toString() + "\n";
            String logKey = LOG_S3_KEY_PREFIX + "app-" + System.currentTimeMillis() + ".log";
            
            // Upload log to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(logKey)
                    .contentType("text/plain")
                    .build();
            
            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromString(logEntry, StandardCharsets.UTF_8));
            
            System.out.println("Logging initialized to S3: " + S3_BUCKET_NAME + "/" + logKey);
            
        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }
    
    /**
     * Start server using environment variable for port configuration
     */
    private void startServer() {
        try {
            // Use environment variable for port (cloud-native pattern)
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT + " (from environment variable)");
            System.out.println("Server ready to accept connections...");
            
            // Log server start to S3
            logToS3("Server started successfully on port " + SERVER_PORT);
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
    
    /**
     * Log message to AWS S3
     */
    private void logToS3(String message) {
        try {
            String logEntry = java.time.Instant.now().toString() + " - " + message + "\n";
            String logKey = LOG_S3_KEY_PREFIX + "app-" + java.time.LocalDate.now() + ".log";
            
            // In production, you would append to existing log or use CloudWatch Logs
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(logKey)
                    .contentType("text/plain")
                    .build();
            
            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromString(logEntry, StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            System.err.println("Failed to log to S3: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup AWS clients
     */
    public void shutdown() {
        try {
            if (s3Client != null) {
                s3Client.close();
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
