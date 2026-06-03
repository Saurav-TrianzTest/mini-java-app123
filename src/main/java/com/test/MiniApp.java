package com.test;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application with containerization fixes applied
 */
public class MiniApp {
    
    // FIXED blocker-7: Externalized port configuration using environment variable
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
    
    // FIXED blocker-1, blocker-2: Replaced absolute file paths with S3 bucket and keys
    private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "mini-app-config-bucket");
    private static final String CONFIG_S3_KEY = System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties");
    private static final String LOG_S3_KEY = System.getenv().getOrDefault("LOG_S3_KEY", "logs/mini-app.log");
    
    private final S3Client s3Client;
    
    public MiniApp() {
        // Initialize S3 client for file operations
        this.s3Client = S3Client.builder().build();
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // FIXED: Reading from S3 instead of hardcoded absolute path
        loadConfiguration();
        
        // FIXED: Writing to S3 instead of hardcoded absolute path
        initializeLogging();
        
        // Initialize database connection with externalized values
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // FIXED blocker-1: Using S3 for configuration file storage
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(CONFIG_S3_KEY)
                    .build();
            
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            Properties props = new Properties();
            props.load(s3Object);
            
            System.out.println("Configuration loaded from S3: s3://" + S3_BUCKET_NAME + "/" + CONFIG_S3_KEY);
            s3Object.close();
        } catch (Exception e) {
            System.out.println("Warning: Configuration file not found in S3: " + e.getMessage());
            System.out.println("Using default configuration");
        }
    }
    
    private void initializeLogging() {
        try {
            // FIXED blocker-2, blocker-3: Using S3 for log file storage instead of local filesystem
            String logMessage = "Application initialized at: " + System.currentTimeMillis() + "\n";
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(LOG_S3_KEY)
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromString(logMessage));
            
            System.out.println("Logging initialized in S3: s3://" + S3_BUCKET_NAME + "/" + LOG_S3_KEY);
        } catch (Exception e) {
            System.err.println("Failed to initialize logging in S3: " + e.getMessage());
        }
    }
    
    private void startServer() {
        try {
            // FIXED blocker-8: Using externalized port configuration
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT);
            System.out.println("Server ready to accept connections...");
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
