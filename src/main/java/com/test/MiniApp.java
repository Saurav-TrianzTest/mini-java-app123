package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application with intentional containerization blockers for testing
 * Updated for Java 21 compatibility with modern language features
 */
public class MiniApp {
    
    // BLOCKER: Hardcoded port number
    private static final int SERVER_PORT = 8080;
    
    // BLOCKER: Hardcoded absolute file path
    private static final String CONFIG_FILE_PATH = "/opt/app/config/app.properties";
    private static final String LOG_FILE_PATH = "/var/log/mini-app.log";
    
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
        // FIXED: Using try-with-resources for automatic resource management (Java 7+)
        // Compatible with Java 21
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                System.out.println("Configuration loaded from: " + CONFIG_FILE_PATH);
            } catch (IOException e) {
                System.err.println("Failed to load configuration: " + e.getMessage());
            }
        } else {
            System.out.println("Warning: Configuration file not found at: " + CONFIG_FILE_PATH);
        }
    }
    
    private void initializeLogging() {
        try {
            // BLOCKER: Hardcoded absolute path for log file
            File logDir = new File("/var/log");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            File logFile = new File(LOG_FILE_PATH);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            
            System.out.println("Logging initialized at: " + LOG_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }
    
    private void startServer() {
        // FIXED: Using try-with-resources for ServerSocket (Java 7+)
        // Compatible with Java 21
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started on port: " + SERVER_PORT);
            System.out.println("Server ready to accept connections...");
            
            // Simulate server running
            Thread.sleep(1000);
            
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Server interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
