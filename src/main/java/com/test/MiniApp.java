package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application - Cloud-Ready Version
 * Fixed: Hardcoded file paths replaced with environment variables and classpath resources
 * Fixed: Hardcoded port replaced with environment variable
 */
public class MiniApp {
    
    // FIXED: Port now configurable via environment variable
    private static final int SERVER_PORT = getServerPort();
    
    // FIXED: Configuration loaded from classpath resources instead of absolute paths
    private static final String CONFIG_RESOURCE = "application.properties";
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application (Cloud-Ready)...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    /**
     * FIXED: Get server port from environment variable with fallback
     */
    private static int getServerPort() {
        String portEnv = System.getenv("SERVER_PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid SERVER_PORT environment variable, using default 8080");
            }
        }
        return 8080; // Default fallback
    }
    
    private void initializeApplication() {
        // FIXED: Load configuration from classpath resources
        loadConfiguration();
        
        // FIXED: Use console logging for cloud environments (no file system dependency)
        initializeLogging();
        
        // Initialize database connection with externalized configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    /**
     * FIXED: Load configuration from classpath resources instead of absolute file paths
     * This allows configuration to be packaged with the application or provided via ConfigMap/Parameter Store
     */
    private void loadConfiguration() {
        try {
            // FIXED: Load from classpath instead of absolute file path
            InputStream configStream = getClass().getClassLoader().getResourceAsStream(CONFIG_RESOURCE);
            
            if (configStream != null) {
                Properties props = new Properties();
                props.load(configStream);
                System.out.println("Configuration loaded from classpath resource: " + CONFIG_RESOURCE);
                configStream.close();
            } else {
                System.out.println("Warning: Configuration file not found in classpath: " + CONFIG_RESOURCE);
                System.out.println("Using environment variables for configuration");
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }
    
    /**
     * FIXED: Use console logging instead of file-based logging
     * Cloud platforms capture stdout/stderr for log aggregation
     */
    private void initializeLogging() {
        // FIXED: Console logging for cloud environments
        // Logs are captured by container runtime and sent to CloudWatch/Stackdriver/Azure Monitor
        System.out.println("Logging initialized - using console output for cloud log aggregation");
        System.out.println("Log Level: " + System.getenv().getOrDefault("LOG_LEVEL", "INFO"));
        System.out.println("Application Name: " + System.getenv().getOrDefault("APP_NAME", "mini-java-app"));
    }
    
    /**
     * FIXED: Server port now configurable via environment variable
     */
    private void startServer() {
        try {
            // FIXED: Use environment-configured port
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