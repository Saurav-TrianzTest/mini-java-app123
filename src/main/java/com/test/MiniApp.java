package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Mini Java Application - Cloud Native Version
 */
public class MiniApp {

    private static final Logger logger = Logger.getLogger(MiniApp.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Cloud-native: Use environment variables with defaults
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // Cloud-native: Use classpath resources instead of hardcoded file paths
    private static final String CONFIG_FILE_PATH = System.getenv().getOrDefault("CONFIG_FILE_PATH", "application.properties");
    private static final String LOG_LEVEL = System.getenv().getOrDefault("LOG_LEVEL", "INFO");
    
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
            // Cloud-native: Load configuration from classpath resources
            InputStream configStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);
            if (configStream != null) {
                Properties props = new Properties();
                props.load(configStream);
                configStream.close();

                // Log configuration load success with structured logging
                Map<String, Object> logData = new HashMap<>();
                logData.put("event", "configuration_loaded");
                logData.put("source", CONFIG_FILE_PATH);
                logData.put("timestamp", System.currentTimeMillis());
                logger.info(objectMapper.writeValueAsString(logData));
            } else {
                // Try to load from environment variables if classpath resource not found
                Map<String, Object> logData = new HashMap<>();
                logData.put("event", "configuration_fallback");
                logData.put("message", "Configuration file not found in classpath, using environment variables");
                logData.put("source", CONFIG_FILE_PATH);
                logData.put("timestamp", System.currentTimeMillis());
                logger.warning(objectMapper.writeValueAsString(logData));
            }
        } catch (IOException e) {
            Map<String, Object> logData = new HashMap<>();
            logData.put("event", "configuration_load_error");
            logData.put("error", e.getMessage());
            logData.put("source", CONFIG_FILE_PATH);
            logData.put("timestamp", System.currentTimeMillis());
            logger.severe(objectMapper.writeValueAsString(logData));
        }
    }
    
    private void initializeLogging() {
        try {
            // Cloud-native: Initialize structured console logging
            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.parse(LOG_LEVEL));

            // Cloud-native: Log initialization with structured format
            Map<String, Object> logData = new HashMap<>();
            logData.put("event", "logging_initialized");
            logData.put("level", LOG_LEVEL);
            logData.put("format", "structured_json");
            logData.put("output", "console");
            logData.put("timestamp", System.currentTimeMillis());
            logger.info(objectMapper.writeValueAsString(logData));

        } catch (Exception e) {
            Map<String, Object> logData = new HashMap<>();
            logData.put("event", "logging_initialization_error");
            logData.put("error", e.getMessage());
            logData.put("timestamp", System.currentTimeMillis());
            logger.severe(objectMapper.writeValueAsString(logData));
        }
    }
    
    private void startServer() {
        ServerSocket serverSocket = null;
        try {
            // Cloud-native: Use environment variable for port
            serverSocket = new ServerSocket(SERVER_PORT);

            // Cloud-native: Structured logging for server startup
            Map<String, Object> logData = new HashMap<>();
            logData.put("event", "server_started");
            logData.put("port", SERVER_PORT);
            logData.put("status", "ready");
            logData.put("timestamp", System.currentTimeMillis());
            logger.info(objectMapper.writeValueAsString(logData));

            // Simulate server running
            Thread.sleep(1000);

        } catch (Exception e) {
            Map<String, Object> logData = new HashMap<>();
            logData.put("event", "server_start_error");
            logData.put("error", e.getMessage());
            logData.put("port", SERVER_PORT);
            logData.put("timestamp", System.currentTimeMillis());
            logger.severe(objectMapper.writeValueAsString(logData));
        } finally {
            // Cloud-native: Proper resource cleanup
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    Map<String, Object> logData = new HashMap<>();
                    logData.put("event", "server_stopped");
                    logData.put("port", SERVER_PORT);
                    logData.put("timestamp", System.currentTimeMillis());
                    logger.info(objectMapper.writeValueAsString(logData));
                } catch (IOException e) {
                    Map<String, Object> logData = new HashMap<>();
                    logData.put("event", "server_cleanup_error");
                    logData.put("error", e.getMessage());
                    logData.put("timestamp", System.currentTimeMillis());
                    logger.warning(objectMapper.writeValueAsString(logData));
                }
            }
        }
    }
}