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
import java.util.concurrent.CompletableFuture;

/**
 * Mini Java Application - Cloud-Ready Version
 * Fixed to use AWS S3 for file storage, Parameter Store for configuration,
 * and environment variables for port configuration
 */
public class MiniApp {
    
    // Cloud-ready: Port from environment variable with fallback
    private static final int SERVER_PORT = Integer.parseInt(
        System.getenv().getOrDefault("SERVER_PORT", "8080")
    );
    
    // Cloud-ready: S3 bucket and keys from environment variables
    private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "mini-app-config-bucket");
    private static final String CONFIG_S3_KEY = System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties");
    private static final String LOG_S3_KEY_PREFIX = System.getenv().getOrDefault("LOG_S3_KEY_PREFIX", "logs/");
    
    // Cloud-ready: AWS Region from environment variable
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    
    private final S3Client s3Client;
    private final SsmClient ssmClient;
    
    public MiniApp() {
        // Initialize AWS clients with proper region configuration
        this.s3Client = S3Client.builder()
            .region(Region.of(AWS_REGION))
            .build();
        
        this.ssmClient = SsmClient.builder()
            .region(Region.of(AWS_REGION))
            .build();
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application (Cloud-Ready)...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // Cloud-ready: Load configuration from S3 and Parameter Store
        loadConfiguration();
        
        // Cloud-ready: Initialize logging to S3
        initializeLogging();
        
        // Initialize database connection with cloud-ready configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        // Cloud-ready: Load configuration from AWS Systems Manager Parameter Store (asynchronously)
        CompletableFuture.runAsync(() -> {
            try {
                // Load from Parameter Store
                String parameterName = System.getenv().getOrDefault("CONFIG_PARAMETER_NAME", "/mini-app/config");
                GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
                
                GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
                String configValue = parameterResponse.parameter().value();
                
                System.out.println("Configuration loaded from Parameter Store: " + parameterName);
                
                // Also try to load from S3 as fallback
                loadConfigurationFromS3();
                
            } catch (Exception e) {
                System.err.println("Failed to load configuration from Parameter Store: " + e.getMessage());
                // Fallback to S3
                loadConfigurationFromS3();
            }
        });
    }
    
    private void loadConfigurationFromS3() {
        try {
            // Cloud-ready: Load configuration from S3 instead of local file system
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(S3_BUCKET_NAME)
                .key(CONFIG_S3_KEY)
                .build();
            
            InputStream configStream = s3Client.getObject(getObjectRequest);
            Properties props = new Properties();
            props.load(configStream);
            
            System.out.println("Configuration loaded from S3: s3://" + S3_BUCKET_NAME + "/" + CONFIG_S3_KEY);
            
        } catch (Exception e) {
            System.err.println("Failed to load configuration from S3: " + e.getMessage());
            System.out.println("Using default configuration values from environment variables");
        }
    }
    
    private void initializeLogging() {
        try {
            // Cloud-ready: Write logs to S3 instead of local file system
            String logKey = LOG_S3_KEY_PREFIX + "app-" + LocalDateTime.now().toString() + ".log";
            String logMessage = "Application started at: " + LocalDateTime.now() + "\n";
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(S3_BUCKET_NAME)
                .key(logKey)
                .contentType("text/plain")
                .build();
            
            s3Client.putObject(putObjectRequest, 
                RequestBody.fromInputStream(
                    new ByteArrayInputStream(logMessage.getBytes(StandardCharsets.UTF_8)),
                    logMessage.length()
                )
            );
            
            System.out.println("Logging initialized to S3: s3://" + S3_BUCKET_NAME + "/" + logKey);
            
        } catch (Exception e) {
            System.err.println("Failed to initialize logging to S3: " + e.getMessage());
            System.out.println("Falling back to console logging");
        }
    }
    
    private void startServer() {
        try {
            // Cloud-ready: Use port from environment variable
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT + " (from environment variable)");
            System.out.println("Server ready to accept connections...");
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        } finally {
            // Clean up AWS clients
            cleanup();
        }
    }
    
    private void cleanup() {
        try {
            if (s3Client != null) {
                s3Client.close();
            }
            if (ssmClient != null) {
                ssmClient.close();
            }
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}
