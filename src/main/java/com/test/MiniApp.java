package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;

/**
 * Mini Java Application - Cloud Ready Version
 */
public class MiniApp {

    private static final Logger logger = Logger.getLogger(MiniApp.class.getName());

    // Cloud-ready: Use environment variable for port with fallback
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // Cloud-ready: Use classpath resources instead of absolute file paths
    private static final String CONFIG_FILE_PATH = "application.properties";

    static {
        // Initialize structured logging for cloud environments
        setupCloudLogging();
    }
    
    public static void main(String[] args) {
        logger.info("Starting Mini Java Application in cloud environment...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private static void setupCloudLogging() {
        // Cloud-ready: Console logging for cloud environments with structured format
        Logger rootLogger = Logger.getLogger("");
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        rootLogger.addHandler(consoleHandler);
        rootLogger.setUseParentHandlers(false);
    }

    private void initializeApplication() {
        // Cloud-ready: Load configuration from classpath
        loadConfiguration();

        // Cloud-ready: Console logging initialized in static block
        logger.info("Application logging initialized for cloud environment");

        // Initialize database connection with cloud-ready configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // Cloud-ready: Load configuration from classpath resources
            InputStream configStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);
            if (configStream != null) {
                Properties props = new Properties();
                props.load(configStream);
                logger.info("Configuration loaded from classpath: " + CONFIG_FILE_PATH);
                configStream.close();
            } else {
                logger.warning("Configuration file not found in classpath: " + CONFIG_FILE_PATH);
            }
        } catch (IOException e) {
            logger.severe("Failed to load configuration: " + e.getMessage());
        }
    }
    
    // Removed initializeLogging method - logging is now handled in setupCloudLogging static block
    
    private void startServer() {
        try {
            // Cloud-ready: Use environment variable for port configuration
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            logger.info("Server started on port: " + SERVER_PORT + " (from environment variable SERVER_PORT)");
            logger.info("Server ready to accept connections...");

            // Add connection timeout for cloud resilience
            serverSocket.setSoTimeout(Integer.parseInt(System.getenv().getOrDefault("SERVER_TIMEOUT", "30000")));

            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();

        } catch (Exception e) {
            logger.severe("Failed to start server: " + e.getMessage());
        }
    }
}