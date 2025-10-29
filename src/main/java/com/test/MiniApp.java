package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Mini Java Application - Cloud Ready Version
 */
public class MiniApp {

    private static final Logger logger = Logger.getLogger(MiniApp.class.getName());

    // Cloud-ready: Use environment variables for configuration
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // Cloud-ready: Use classpath resources instead of absolute file paths
    private static final String CONFIG_FILE_PATH = System.getenv().getOrDefault("CONFIG_FILE_PATH", "application.properties");
    
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
            // Cloud-ready: Use classpath resources or environment variables
            Properties props = new Properties();
            InputStream configStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);

            if (configStream != null) {
                props.load(configStream);
                logger.info("Configuration loaded from classpath: " + CONFIG_FILE_PATH);
                configStream.close();
            } else {
                logger.warning("Configuration file not found in classpath: " + CONFIG_FILE_PATH + ", using defaults");
            }
        } catch (IOException e) {
            logger.severe("Failed to load configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        // Cloud-ready: Use structured logging to stdout for cloud environments
        // Cloud platforms handle log aggregation and storage automatically
        String logLevel = System.getenv().getOrDefault("LOG_LEVEL", "INFO");
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "{\"timestamp\":\"%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS\",\"level\":\"%4$s\",\"logger\":\"%3$s\",\"message\":\"%5$s\"}%n");

        logger.info("Structured logging initialized with level: " + logLevel);
    }
    
    private void startServer() {
        ServerSocket serverSocket = null;
        try {
            // Cloud-ready: Use environment variable for port configuration
            serverSocket = new ServerSocket(SERVER_PORT);
            logger.info("Server started on port: " + SERVER_PORT);
            logger.info("Server ready to accept connections...");

            // Add connection timeout for cloud environments
            serverSocket.setSoTimeout(Integer.parseInt(System.getenv().getOrDefault("SERVER_TIMEOUT", "30000")));

            // Simulate server running
            Thread.sleep(1000);

        } catch (Exception e) {
            logger.severe("Failed to start server: " + e.getMessage());
        } finally {
            // Cloud-ready: Ensure resources are properly cleaned up
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    logger.warning("Failed to close server socket: " + e.getMessage());
                }
            }
        }
    }
}