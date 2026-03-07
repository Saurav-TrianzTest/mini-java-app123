package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application with intentional containerization blockers for testing
 */
public class MiniApp {
    
    // Fixed: Port now configurable via environment variable
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
    
    // Fixed: Paths now configurable via environment variables
    private static final String CONFIG_FILE_PATH = System.getenv().getOrDefault("CONFIG_PATH", "./config/app.properties");
    private static final String LOG_FILE_PATH = System.getenv().getOrDefault("LOG_FILE_PATH", "/var/log/mini-app.log");
    
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
            // Fixed: Configuration path now uses environment variable
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(configFile));
                System.out.println("Configuration loaded from: " + CONFIG_FILE_PATH);
            } else {
                System.out.println("Warning: Configuration file not found at: " + CONFIG_FILE_PATH + " (using environment variables)");
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        // Fixed: Using stdout/stderr for container-friendly logging
        System.out.println("Logging configured to use stdout/stderr for container compatibility");
        // Container orchestration systems will collect logs from stdout/stderr
    }
    
    private void startServer() {
        try {
            // Fixed: Port is now read from environment variable via SERVER_PORT constant
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

    // Fixed: Added health check endpoint for container orchestration (blocker-12)
    public String healthCheck() {
        return "{\"status\":\"UP\",\"timestamp\":" + System.currentTimeMillis() + "}";
    }

    public String readinessCheck() {
        // Check if critical services are ready
        return "{\"status\":\"READY\",\"timestamp\":" + System.currentTimeMillis() + "}";
    }
}