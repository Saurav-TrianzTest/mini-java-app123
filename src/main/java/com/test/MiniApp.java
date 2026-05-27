package com.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Mini Java Application with intentional containerization blockers for testing
 * Updated for Java 21 compatibility with modern Java features
 */
public class MiniApp {
    
    // BLOCKER: Hardcoded port number
    private static final int SERVER_PORT = 8080;
    
    // BLOCKER: Hardcoded absolute file path
    private static final String CONFIG_FILE_PATH = "/opt/app/config/app.properties";
    private static final String LOG_FILE_PATH = "/var/log/mini-app.log";
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        
        var app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // BLOCKER: Reading from hardcoded absolute path
        loadConfiguration();
        
        // BLOCKER: Writing to hardcoded absolute path
        initializeLogging();
        
        // Initialize database connection with hardcoded values
        var dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // BLOCKER: Hardcoded absolute file path
            // Using modern Path API instead of File
            Path configPath = Paths.get(CONFIG_FILE_PATH);
            if (Files.exists(configPath)) {
                var props = new Properties();
                try (var inputStream = Files.newInputStream(configPath)) {
                    props.load(inputStream);
                    System.out.println("Configuration loaded from: " + CONFIG_FILE_PATH);
                }
            } else {
                System.out.println("Warning: Configuration file not found at: " + CONFIG_FILE_PATH);
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        try {
            // BLOCKER: Hardcoded absolute path for log file
            // Using modern Path API
            Path logDir = Paths.get("/var/log");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            Path logFile = Paths.get(LOG_FILE_PATH);
            if (!Files.exists(logFile)) {
                Files.createFile(logFile);
            }
            
            System.out.println("Logging initialized at: " + LOG_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }
    
    private void startServer() {
        try {
            // BLOCKER: Hardcoded port number
            // Using try-with-resources for automatic resource management
            try (var serverSocket = new ServerSocket(SERVER_PORT)) {
                System.out.println("Server started on port: " + SERVER_PORT);
                System.out.println("Server ready to accept connections...");
                
                // Simulate server running
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
