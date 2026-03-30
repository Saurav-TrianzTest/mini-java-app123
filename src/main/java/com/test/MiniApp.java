package com.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Mini Java Application with intentional containerization blockers for testing
 */
public class MiniApp {
    
    // BLOCKER: Hardcoded port number
    private static final int SERVER_PORT = 8080;
    
    // FIXED blocker-1 (cz-java-0057): Removed hardcoded absolute file paths
    // Configuration is now loaded from classpath
    private static final String CONFIG_FILE_NAME = "application.properties";
    
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
            // FIXED blocker-1 (cz-java-0057): Load resources via classpath instead of filesystem
            // Using getClass().getResourceAsStream() to load from classpath
            InputStream configStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
            
            if (configStream != null) {
                Properties props = new Properties();
                props.load(configStream);
                System.out.println("Configuration loaded from classpath: " + CONFIG_FILE_NAME);
                configStream.close();
            } else {
                System.out.println("Warning: Configuration file not found in classpath: " + CONFIG_FILE_NAME);
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        try {
            // FIXED blocker-2 (cz-java-0058): Use Java Temp Directory APIs for ephemeral paths
            // Using Files.createTempDirectory() and System.getProperty("java.io.tmpdir")
            String tempDir = System.getProperty("java.io.tmpdir");
            Path logDir = Files.createTempDirectory("mini-app-logs");
            
            Path logFile = logDir.resolve("mini-app.log");
            if (!Files.exists(logFile)) {
                Files.createFile(logFile);
            }
            
            System.out.println("Logging initialized at: " + logFile.toAbsolutePath());
            System.out.println("Using system temp directory: " + tempDir);
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
