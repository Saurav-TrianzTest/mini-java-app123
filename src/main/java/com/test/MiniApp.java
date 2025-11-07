package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mini Java Application - Cloud Ready Version
 */
public class MiniApp {

    private static final Logger logger = LoggerFactory.getLogger(MiniApp.class);

    // Cloud-ready configuration using environment variables
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // Use classpath resources instead of absolute file paths
    private static final String CONFIG_FILE_PATH = "application.properties";
    private static final String LOG_FILE_PATH = System.getenv().getOrDefault("LOG_FILE_PATH", "/app/logs/mini-app.log");
    
    public static void main(String[] args) {
        logger.info("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // Load configuration from classpath resources
        loadConfiguration();

        // Initialize logging using structured logging
        initializeLogging();

        // Initialize database connection using environment variables
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // Load configuration from classpath resources
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);
            if (inputStream != null) {
                Properties props = new Properties();
                props.load(inputStream);
                logger.info("Configuration loaded from classpath: {}", CONFIG_FILE_PATH);
                inputStream.close();
            } else {
                logger.warn("Configuration file not found in classpath: {}", CONFIG_FILE_PATH);
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration: {}", e.getMessage());
        }
    }
    
    private void initializeLogging() {
        // Use structured logging for cloud environments
        // Logging configuration should be handled by logback.xml or application.properties
        // This eliminates the need for file system operations
        logger.info("Structured logging initialized for cloud environment");
        logger.info("Log output will be directed to stdout for container log aggregation");
    }
    
    private void startServer() {
        ServerSocket serverSocket = null;
        try {
            // Use environment variable for port configuration
            serverSocket = new ServerSocket(SERVER_PORT);
            logger.info("Server started on port: {}", SERVER_PORT);
            logger.info("Server ready to accept connections...");

            // Add connection timeout and proper resource management
            serverSocket.setSoTimeout(30000); // 30 second timeout

            // Simulate server running
            Thread.sleep(1000);

        } catch (Exception e) {
            logger.error("Failed to start server: {}", e.getMessage());
        } finally {
            // Ensure proper resource cleanup
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    logger.info("Server socket closed successfully");
                } catch (IOException e) {
                    logger.error("Failed to close server socket: {}", e.getMessage());
                }
            }
        }
    }
}