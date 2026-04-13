package com.test;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application - Containerized version with S3 storage and externalized configuration
 */
public class MiniApp {
    
    // FIXED blocker-5: Externalized port configuration using environment variable
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
    
    // FIXED blocker-1, blocker-2: Replaced absolute file paths with S3 bucket and key configuration
    private static final String CONFIG_S3_BUCKET = System.getenv().getOrDefault("CONFIG_S3_BUCKET", "app-config-bucket");
    private static final String CONFIG_S3_KEY = System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties");
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    
    private final S3Client s3Client;
    
    public MiniApp() {
        // Initialize S3 client for cloud-native storage
        this.s3Client = S3Client.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
    
    public static void main(String[] args) {
        // FIXED blocker-7, blocker-9: Using stdout for container-native logging
        System.out.println("Starting Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // FIXED blocker-1: Loading configuration from S3 instead of local file system
        loadConfiguration();
        
        // FIXED blocker-7, blocker-9: Removed file-based logging, using stdout/stderr
        initializeLogging();
        
        // Initialize database connection with externalized configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // FIXED blocker-1, blocker-2: Reading from S3 instead of hardcoded absolute path
            System.out.println("Loading configuration from S3 bucket: " + CONFIG_S3_BUCKET + ", key: " + CONFIG_S3_KEY);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(CONFIG_S3_BUCKET)
                    .key(CONFIG_S3_KEY)
                    .build();
            
            try (InputStream s3InputStream = s3Client.getObject(getObjectRequest)) {
                Properties props = new Properties();
                props.load(s3InputStream);
                System.out.println("Configuration loaded successfully from S3");
            }
        } catch (Exception e) {
            // FIXED blocker-7, blocker-9: Logging to stderr for container log collection
            System.err.println("Failed to load configuration from S3: " + e.getMessage());
            System.out.println("Using default configuration values");
        }
    }
    
    private void initializeLogging() {
        // FIXED blocker-7, blocker-9: Replaced file-based logging with stdout/stderr
        // Container runtimes (ECS, EKS) automatically capture stdout/stderr and forward to CloudWatch Logs
        System.out.println("Logging initialized - using stdout/stderr for container-native log aggregation");
        System.out.println("Logs will be automatically collected by AWS container runtime and forwarded to CloudWatch Logs");
    }
    
    private void startServer() {
        try {
            // FIXED blocker-5: Using externalized port configuration
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT);
            System.out.println("Server ready to accept connections...");
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
        } catch (Exception e) {
            // FIXED blocker-7, blocker-9: Using stderr for error logging
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
