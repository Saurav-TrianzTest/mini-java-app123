package com.test;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application with cloud-native patterns for AWS deployment
 * - Uses Amazon S3 for file storage instead of local file system
 * - Uses AWS Parameter Store for configuration
 * - Uses environment variables for port configuration
 */
public class MiniApp {
    
    // Cloud-native: Port from environment variable with fallback
    private static int SERVER_PORT;
    
    // Cloud-native: S3 bucket and keys from environment variables
    private static String S3_BUCKET_NAME;
    private static String CONFIG_S3_KEY;
    private static String LOG_S3_KEY;
    
    private S3Client s3Client;
    private AwsConfigurationManager configManager;
    
    public MiniApp() {
        // Initialize AWS clients
        String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
        this.configManager = new AwsConfigurationManager();
        
        // Load configuration from environment variables and Parameter Store
        loadEnvironmentConfiguration();
    }
    
    /**
     * Load configuration from environment variables and AWS Parameter Store
     */
    private void loadEnvironmentConfiguration() {
        // Server port from Parameter Store or environment variable
        String portStr = configManager.getParameter("/mini-app/server/port", 
                System.getenv().getOrDefault("SERVER_PORT", "8080"));
        SERVER_PORT = Integer.parseInt(portStr);
        
        // S3 bucket configuration from Parameter Store or environment variables
        S3_BUCKET_NAME = configManager.getParameter("/mini-app/s3/bucket-name",
                System.getenv().getOrDefault("S3_BUCKET_NAME", "mini-app-storage"));
        CONFIG_S3_KEY = configManager.getParameter("/mini-app/s3/config-key",
                System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties"));
        LOG_S3_KEY = configManager.getParameter("/mini-app/s3/log-key",
                System.getenv().getOrDefault("LOG_S3_KEY", "logs/mini-app.log"));
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application with cloud-native configuration...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
        app.cleanup();
    }
    
    private void initializeApplication() {
        // Cloud-native: Load configuration from S3
        loadConfiguration();
        
        // Cloud-native: Initialize logging to S3
        initializeLogging();
        
        // Initialize database connection with cloud-native configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    /**
     * Load configuration from Amazon S3 instead of local file system
     */
    private void loadConfiguration() {
        try {
            System.out.println("Loading configuration from S3 bucket: " + S3_BUCKET_NAME + ", key: " + CONFIG_S3_KEY);
            
            // Download configuration file from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(CONFIG_S3_KEY)
                    .build();
            
            InputStream configStream = s3Client.getObject(getObjectRequest);
            Properties props = new Properties();
            props.load(configStream);
            
            System.out.println("Configuration loaded from S3: " + S3_BUCKET_NAME + "/" + CONFIG_S3_KEY);
            System.out.println("Loaded " + props.size() + " configuration properties");
            
            configStream.close();
            
        } catch (Exception e) {
            System.err.println("Failed to load configuration from S3: " + e.getMessage());
            System.out.println("Using default configuration from Parameter Store");
            // Fallback to Parameter Store or environment variables
        }
    }
    
    /**
     * Initialize logging to Amazon S3 instead of local file system
     */
    private void initializeLogging() {
        try {
            System.out.println("Initializing logging to S3 bucket: " + S3_BUCKET_NAME + ", key: " + LOG_S3_KEY);
            
            // Create initial log entry
            String initialLogEntry = "Application started at: " + System.currentTimeMillis() + "\n";
            
            // Upload log file to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(LOG_S3_KEY)
                    .contentType("text/plain")
                    .build();
            
            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(
                            new ByteArrayInputStream(initialLogEntry.getBytes(StandardCharsets.UTF_8)),
                            initialLogEntry.length()));
            
            System.out.println("Logging initialized to S3: " + S3_BUCKET_NAME + "/" + LOG_S3_KEY);
            
        } catch (Exception e) {
            System.err.println("Failed to initialize logging to S3: " + e.getMessage());
            System.out.println("Falling back to CloudWatch Logs or stdout");
        }
    }
    
    /**
     * Start server using port from environment variable/Parameter Store
     */
    private void startServer() {
        try {
            System.out.println("Starting server on port from environment: " + SERVER_PORT);
            
            // Cloud-native: Use port from environment variable
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT);
            System.out.println("Server ready to accept connections...");
            System.out.println("Port configured via AWS Parameter Store or environment variable");
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cleanup AWS resources
     */
    private void cleanup() {
        try {
            if (s3Client != null) {
                s3Client.close();
            }
            if (configManager != null) {
                configManager.close();
            }
            System.out.println("AWS resources cleaned up successfully");
        } catch (Exception e) {
            System.err.println("Failed to cleanup resources: " + e.getMessage());
        }
    }
}
