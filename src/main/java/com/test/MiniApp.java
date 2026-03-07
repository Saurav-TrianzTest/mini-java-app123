package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Mini Java Application - Cloud-ready version for AWS deployment
 */
public class MiniApp {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String CORRELATION_ID = UUID.randomUUID().toString();

    // Cloud-ready: Port from environment variable
    private static final int SERVER_PORT = Integer.parseInt(
        System.getenv().getOrDefault("SERVER_PORT", "8080")
    );

    // Cloud-ready: Use classpath resources instead of absolute file paths
    private static final String CONFIG_FILE_PATH = System.getenv()
        .getOrDefault("CONFIG_FILE_PATH", "application.properties");
    private static final String LOG_FILE_PATH = System.getenv()
        .getOrDefault("LOG_FILE_PATH", "stdout");
    
    public static void main(String[] args) {
        logStructured("INFO", "Starting Mini Java Application", "Application startup initiated");

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
            // Cloud-ready: Load from classpath resources
            InputStream configStream = getClass().getClassLoader()
                .getResourceAsStream(CONFIG_FILE_PATH);

            if (configStream != null) {
                Properties props = new Properties();
                props.load(configStream);
                logStructured("INFO", "Configuration loaded from classpath",
                    "Configuration loaded successfully from: " + CONFIG_FILE_PATH);
                configStream.close();
            } else {
                logStructured("WARN", "Configuration file not found in classpath",
                    "Configuration file not found: " + CONFIG_FILE_PATH);
            }
        } catch (IOException e) {
            logStructured("ERROR", "Failed to load configuration",
                "Error: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        // Cloud-ready: Use console/stdout logging for cloud environments
        // Cloud platforms (AWS CloudWatch, etc.) will capture stdout/stderr
        logStructured("INFO", "Logging initialized",
            "Using structured JSON logging to stdout for cloud monitoring");
    }
    
    private void startServer() {
        try {
            // Cloud-ready: Port from environment variable
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            logStructured("INFO", "Server started",
                "Server listening on port: " + SERVER_PORT);
            logStructured("INFO", "Server ready",
                "Server ready to accept connections");

            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();

        } catch (Exception e) {
            logStructured("ERROR", "Failed to start server",
                "Error: " + e.getMessage());
        }
    }

    /**
     * Cloud-ready structured logging with JSON format for cloud monitoring
     */
    private static void logStructured(String level, String message, String details) {
        try {
            ObjectNode logEntry = JSON_MAPPER.createObjectNode();
            logEntry.put("timestamp", Instant.now().toString());
            logEntry.put("level", level);
            logEntry.put("message", message);
            logEntry.put("details", details);
            logEntry.put("correlationId", CORRELATION_ID);
            logEntry.put("application", "mini-app");
            logEntry.put("environment", System.getenv().getOrDefault("ENVIRONMENT", "unknown"));

            System.out.println(JSON_MAPPER.writeValueAsString(logEntry));
        } catch (Exception e) {
            // Fallback to simple logging if JSON serialization fails
            System.out.println(String.format("[%s] %s - %s - %s",
                Instant.now(), level, message, details));
        }
    }
}