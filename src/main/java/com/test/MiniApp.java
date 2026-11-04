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
    
    // Port from environment variable with fallback default
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("APP_PORT", "8080"));
    
    // Config file path from environment variable with fallback default
    private static final String CONFIG_FILE_PATH = System.getenv().getOrDefault("CONFIG_PATH", "/app/config/app.properties");
    private static final String LOG_FILE_PATH = System.getenv().getOrDefault("LOG_PATH", "/app/logs/mini-app.log");
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        // Start health check endpoint first so it is available from start-up
        HealthCheckServer.start();

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();

        // Keep the process running so the health check endpoint stays available
        // The application's own work ends after start-up; the health check server
        // keeps the JVM alive for the container lifecycle.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
            // BLOCKER: Hardcoded absolute file path
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(configFile));
                System.out.println("Configuration loaded from: " + CONFIG_FILE_PATH);
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
            File logDir = new File(System.getenv().getOrDefault("LOG_DIR", "/app/logs"));
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