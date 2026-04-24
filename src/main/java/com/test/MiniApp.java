package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application - Cloud-ready version with S3 integration
 * Replaced hard-coded file paths with Amazon S3 object storage
 */
public class MiniApp {
    
    // BLOCKER: Hardcoded port number
    private static final int SERVER_PORT = 8080;
    
    // Cloud-native: S3 object keys instead of absolute file paths
    private static final String CONFIG_S3_KEY = System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties");
    private static final String LOG_S3_KEY = System.getenv().getOrDefault("LOG_S3_KEY", "logs/mini-app.log");
    
    private S3Service s3Service;
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // Initialize S3 service for cloud-native file operations
        s3Service = new S3Service();
        
        // Cloud-native: Reading from S3 instead of hardcoded absolute path
        loadConfiguration();
        
        // Cloud-native: Writing to S3 instead of hardcoded absolute path
        initializeLogging();
        
        // Initialize database connection with hardcoded values
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // FIXED BLOCKER-1 (Line 44): Replaced hard-coded file path with S3 object storage
            // Original: File configFile = new File(CONFIG_FILE_PATH);
            // Now using S3Service to read configuration from S3
            if (s3Service.objectExists(CONFIG_S3_KEY)) {
                Properties props = s3Service.readPropertiesFromS3(CONFIG_S3_KEY);
                System.out.println("Configuration loaded from S3: " + CONFIG_S3_KEY);
            } else {
                System.out.println("Warning: Configuration file not found in S3: " + CONFIG_S3_KEY);
            }
        } catch (Exception e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        try {
            // FIXED BLOCKER-2 (Line 60) and BLOCKER-3 (Line 65): Replaced hard-coded file paths with S3 object storage
            // Original: File logDir = new File("/var/log");
            // Original: File logFile = new File(LOG_FILE_PATH);
            // Now using S3Service to create log file in S3
            
            String initialLogMessage = "Application started at " + java.time.Instant.now();
            s3Service.createLogInS3(LOG_S3_KEY, initialLogMessage);
            
            System.out.println("Logging initialized in S3: " + LOG_S3_KEY);
        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }
    
    private void startServer() {
        try {
            // BLOCKER: Hardcoded port number
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT);
            System.out.println("Server ready to accept connections...");
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
            // Clean up S3 service
            if (s3Service != null) {
                s3Service.close();
            }
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
