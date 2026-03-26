package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Mini Java Application with intentional containerization blockers for testing
 */
public class MiniApp {
    
    // BLOCKER: Hardcoded port number
    private static final int SERVER_PORT = 8080;
    
    // FIXED: Externalized base path via environment variable (blocker-2: cz-java-0058)
    // FIXED: Using relative paths with java.nio.Paths (blocker-1: cz-java-0057)
    private static final String BASE_PATH = System.getenv().getOrDefault("APP_BASE_PATH", ".");
    private static final Path CONFIG_FILE_PATH = Paths.get(BASE_PATH, "config", "app.properties");
    private static final Path LOG_FILE_PATH = Paths.get(BASE_PATH, "logs", "mini-app.log");
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // BLOCKER: Reading from hardcoded absolute path
        loadConfiguration();
        
        // BLOCKER: Writing to hardcoded absolute path
        initializeLogging();
        
        // Initialize database connection with hardcoded values
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // FIXED: Using relative path resolved from configurable base directory (blocker-1: cz-java-0057)
            File configFile = CONFIG_FILE_PATH.toFile();
            if (configFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(configFile));
                System.out.println("Configuration loaded from: " + CONFIG_FILE_PATH.toAbsolutePath());
            } else {
                System.out.println("Warning: Configuration file not found at: " + CONFIG_FILE_PATH.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        try {
            // FIXED: Using relative path resolved from configurable base directory (blocker-1: cz-java-0057)
            File logDir = LOG_FILE_PATH.getParent().toFile();
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            File logFile = LOG_FILE_PATH.toFile();
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            
            System.out.println("Logging initialized at: " + LOG_FILE_PATH.toAbsolutePath());
        } catch (IOException e) {
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
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}