package com.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

/**
 * Mini Java Application - Cloud-ready version with AWS integration
 * Fixed: Hardcoded file paths replaced with S3, hardcoded ports with environment variables
 */
public class MiniApp {
    
    // FIXED: Port now retrieved from environment variable or AWS Parameter Store
    private static final int DEFAULT_PORT = 8080;
    
    // FIXED: S3 bucket and keys for configuration and logging
    private static final String S3_BUCKET_ENV = "APP_S3_BUCKET";
    private static final String CONFIG_S3_KEY_ENV = "APP_CONFIG_S3_KEY";
    private static final String LOG_S3_KEY_ENV = "APP_LOG_S3_KEY";
    private static final String SERVER_PORT_ENV = "SERVER_PORT";
    private static final String AWS_REGION_ENV = "AWS_REGION";
    
    private final S3Client s3Client;
    private final SsmClient ssmClient;
    private final String s3Bucket;
    private final String configS3Key;
    private final String logS3Key;
    
    public MiniApp() {
        // Initialize AWS clients with region from environment
        String region = System.getenv(AWS_REGION_ENV);
        Region awsRegion = (region != null) ? Region.of(region) : Region.US_EAST_1;
        
        this.s3Client = S3Client.builder()
                .region(awsRegion)
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(awsRegion)
                .build();
        
        // Get S3 bucket and keys from environment variables
        this.s3Bucket = System.getenv(S3_BUCKET_ENV);
        this.configS3Key = getEnvOrDefault(CONFIG_S3_KEY_ENV, "config/app.properties");
        this.logS3Key = getEnvOrDefault(LOG_S3_KEY_ENV, "logs/mini-app.log");
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application (Cloud-Ready)...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // FIXED: Reading from S3 instead of hardcoded absolute path
        loadConfiguration();
        
        // FIXED: Writing to S3 instead of hardcoded absolute path
        initializeLogging();
        
        // Initialize database connection with cloud-native configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // FIXED: Load configuration from S3 instead of hardcoded file path
            if (s3Bucket == null || s3Bucket.isEmpty()) {
                System.out.println("Warning: S3 bucket not configured. Set " + S3_BUCKET_ENV + " environment variable.");
                return;
            }
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(configS3Key)
                    .build();
            
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            Properties props = new Properties();
            props.load(s3Object);
            
            System.out.println("Configuration loaded from S3: s3://" + s3Bucket + "/" + configS3Key);
            
        } catch (S3Exception e) {
            System.err.println("Failed to load configuration from S3: " + e.awsErrorDetails().errorMessage());
        } catch (IOException e) {
            System.err.println("Failed to parse configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        try {
            // FIXED: Initialize logging to S3 instead of hardcoded file path
            if (s3Bucket == null || s3Bucket.isEmpty()) {
                System.out.println("Warning: S3 bucket not configured for logging.");
                return;
            }
            
            String initialLogContent = "Application started at: " + System.currentTimeMillis() + "\n";
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(logS3Key)
                    .build();
            
            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(
                            new ByteArrayInputStream(initialLogContent.getBytes()), 
                            initialLogContent.length()));
            
            System.out.println("Logging initialized to S3: s3://" + s3Bucket + "/" + logS3Key);
            
        } catch (S3Exception e) {
            System.err.println("Failed to initialize logging to S3: " + e.awsErrorDetails().errorMessage());
        }
    }
    
    private void startServer() {
        try {
            // FIXED: Port retrieved from environment variable or Parameter Store
            int serverPort = getServerPort();
            
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Server started on port: " + serverPort + " (from environment/Parameter Store)");
            System.out.println("Server ready to accept connections...");
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
    
    /**
     * FIXED: Get server port from environment variable or AWS Parameter Store
     */
    private int getServerPort() {
        // First try environment variable
        String portEnv = System.getenv(SERVER_PORT_ENV);
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port in environment variable: " + portEnv);
            }
        }
        
        // Try AWS Parameter Store
        try {
            GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name("/mini-app/server/port")
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(parameterRequest);
            return Integer.parseInt(response.parameter().value());
            
        } catch (Exception e) {
            System.out.println("Using default port " + DEFAULT_PORT + " (Parameter Store not available)");
        }
        
        return DEFAULT_PORT;
    }
    
    private String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
