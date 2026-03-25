package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application - Cloud-Ready Version
 * Fixed: Removed hardcoded file paths and ports, using environment variables and classpath resources
 */
public class MiniApp {
    
    // FIXED: Port from environment variable with default fallback
    private static final int SERVER_PORT = Integer.parseInt(
        System.getenv().getOrDefault("SERVER_PORT", "8080")
    );
    
    // FIXED: Removed hardcoded absolute file paths
    // Configuration is now loaded from classpath or environment variables
    private static final String CONFIG_FILE_NAME = System.getenv().getOrDefault(
        "CONFIG_FILE_NAME", "application.properties"
    );
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application (Cloud-Ready)...");
        System.out.println("Environment: " + System.getenv().getOrDefault("ENVIRONMENT", "development"));
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // FIXED: Load configuration from classpath resources
        loadConfiguration();
        
        // FIXED: Use console logging for cloud environments (no file system dependency)
        initializeLogging();
        
        // Initialize database connection with environment-based configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // FIXED: Load from classpath instead of absolute file path
            InputStream configStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
            
            if (configStream != null) {
                Properties props = new Properties();
                props.load(configStream);
                System.out.println("Configuration loaded from classpath: " + CONFIG_FILE_NAME);
                
                // Log configuration source (environment variables take precedence)
                System.out.println("Configuration can be overridden via environment variables");
                configStream.close();
            } else {
                System.out.println("Warning: Configuration file not found in classpath: " + CONFIG_FILE_NAME);
                System.out.println("Using environment variables for configuration");
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            System.out.println("Falling back to environment variables");
        }
    }
    
    private void initializeLogging() {
        // FIXED: Use console logging for cloud environments
        // Cloud platforms (AWS CloudWatch, Azure Monitor, GCP Cloud Logging) capture stdout/stderr
        System.out.println("Logging initialized - using console output for cloud compatibility");
        System.out.println("Log Level: " + System.getenv().getOrDefault("LOG_LEVEL", "INFO"));
        System.out.println("Logs will be captured by cloud logging service (CloudWatch/Azure Monitor/GCP Logging)");
    }
    
    private void startServer() {
        try {
            // FIXED: Use port from environment variable
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT + " (from environment variable SERVER_PORT)");
            System.out.println("Server ready to accept connections...");
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
