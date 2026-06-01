package com.test;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Mini Java Application - Cloud-ready version with AWS integrations
 * Fixed cloud readiness issues:
 * - Replaced hardcoded file paths with Amazon S3 object storage
 * - Replaced hardcoded ports with AWS Parameter Store and environment variables
 * - Replaced classpath properties with AWS Systems Manager Parameter Store
 */
public class MiniApp {
    
    // Cloud-ready: Port from environment variable with fallback
    private static final int SERVER_PORT = getServerPort();
    
    // Cloud-ready: S3 bucket and keys from environment variables
    private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "mini-app-config-bucket");
    private static final String CONFIG_S3_KEY = System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties");
    private static final String LOG_S3_KEY_PREFIX = System.getenv().getOrDefault("LOG_S3_KEY_PREFIX", "logs/");
    
    // AWS Region from environment variable
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    
    private final S3Client s3Client;
    private final SsmClient ssmClient;
    
    public MiniApp() {
        // Initialize AWS clients
        Region region = Region.of(AWS_REGION);
        this.s3Client = S3Client.builder().region(region).build();
        this.ssmClient = SsmClient.builder().region(region).build();
    }
    
    /**
     * Get server port from environment variable or AWS Parameter Store
     * Fixed: cr-java-0077 - Hard-coded Ports
     */
    private static int getServerPort() {
        // Try environment variable first
        String portEnv = System.getenv("SERVER_PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid SERVER_PORT environment variable: " + portEnv);
            }
        }
        
        // Try AWS Parameter Store
        try {
            SsmClient ssmClient = SsmClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();
            
            GetParameterRequest request = GetParameterRequest.builder()
                .name("/mini-app/server/port")
                .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            return Integer.parseInt(response.parameter().value());
        } catch (Exception e) {
            System.err.println("Failed to get port from Parameter Store: " + e.getMessage());
        }
        
        // Default fallback
        return 8080;
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application (Cloud-Ready Version)...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
        
        // Cleanup AWS clients
        app.cleanup();
    }
    
    private void initializeApplication() {
        // Cloud-ready: Load configuration from S3
        loadConfiguration();
        
        // Cloud-ready: Initialize logging to S3
        initializeLogging();
        
        // Initialize database connection with cloud-ready configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    /**
     * Load configuration from Amazon S3
     * Fixed: cr-java-0061 - Hard-coded File Paths
     * Fixed: cr-java-0063 - Java.io.File Usage for Data Storage
     * Fixed: cr-java-0070 - Properties Files in Classpath
     */
    private void loadConfiguration() {
        try {
            System.out.println("Loading configuration from S3 bucket: " + S3_BUCKET_NAME + ", key: " + CONFIG_S3_KEY);
            
            // Get configuration from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(S3_BUCKET_NAME)
                .key(CONFIG_S3_KEY)
                .build();
            
            InputStream configStream = s3Client.getObject(getObjectRequest);
            Properties props = new Properties();
            props.load(configStream);
            
            System.out.println("Configuration loaded successfully from S3");
            System.out.println("Loaded " + props.size() + " configuration properties");
            
            configStream.close();
            
        } catch (Exception e) {
            System.err.println("Failed to load configuration from S3: " + e.getMessage());
            System.out.println("Attempting to load configuration from AWS Parameter Store...");
            loadConfigurationFromParameterStore();
        }
    }
    
    /**
     * Fallback: Load configuration from AWS Systems Manager Parameter Store
     * Fixed: cr-java-0070 - Properties Files in Classpath
     */
    private void loadConfigurationFromParameterStore() {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                .name("/mini-app/config/application")
                .withDecryption(true)
                .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            String configValue = response.parameter().value();
            
            Properties props = new Properties();
            props.load(new ByteArrayInputStream(configValue.getBytes(StandardCharsets.UTF_8)));
            
            System.out.println("Configuration loaded from Parameter Store");
            System.out.println("Loaded " + props.size() + " configuration properties");
            
        } catch (Exception e) {
            System.err.println("Failed to load configuration from Parameter Store: " + e.getMessage());
            System.out.println("Using default configuration values");
        }
    }
    
    /**
     * Initialize logging to Amazon S3
     * Fixed: cr-java-0061 - Hard-coded File Paths
     * Fixed: cr-java-0063 - Java.io.File Usage for Data Storage
     */
    private void initializeLogging() {
        try {
            String timestamp = LocalDateTime.now().toString().replace(":", "-");
            String logKey = LOG_S3_KEY_PREFIX + "app-" + timestamp + ".log";
            
            String initialLogMessage = "Application started at: " + LocalDateTime.now() + "\n";
            
            // Write initial log entry to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(S3_BUCKET_NAME)
                .key(logKey)
                .contentType("text/plain")
                .build();
            
            s3Client.putObject(putObjectRequest, 
                RequestBody.fromString(initialLogMessage));
            
            System.out.println("Logging initialized to S3: s3://" + S3_BUCKET_NAME + "/" + logKey);
            
        } catch (Exception e) {
            System.err.println("Failed to initialize logging to S3: " + e.getMessage());
            System.out.println("Falling back to console logging");
        }
    }
    
    /**
     * Start server with cloud-ready port configuration
     * Fixed: cr-java-0077 - Hard-coded Ports
     */
    private void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT + " (from environment/Parameter Store)");
            System.out.println("Server ready to accept connections...");
            
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
    private void cleanup() {
        try {
            if (s3Client != null) {
                s3Client.close();
            }
            if (ssmClient != null) {
                ssmClient.close();
            }
            System.out.println("AWS clients closed successfully");
        } catch (Exception e) {
            System.err.println("Error closing AWS clients: " + e.getMessage());
        }
    }
}
