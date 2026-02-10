package com.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.InputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.UUID;

/**
 * Mini Java Application - Cloud-Ready Version
 * Follows 12-factor app principles with externalized configuration
 */
public class MiniApp {

    private static final Logger logger = LoggerFactory.getLogger(MiniApp.class);

    // Cloud-ready: Port from environment variable with default fallback
    private static final int SERVER_PORT = Integer.parseInt(
        System.getenv().getOrDefault("SERVER_PORT", "8080")
    );

    // Cloud-ready: Configuration loaded from classpath (embedded in container)
    private static final String CONFIG_RESOURCE = "application.properties";

    public static void main(String[] args) {
        // Add correlation ID for distributed tracing
        MDC.put("correlationId", UUID.randomUUID().toString());
        logger.info("Starting Mini Java Application");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();

        MDC.clear();
    }

    private void initializeApplication() {
        try {
            // Cloud-ready: Load configuration from classpath
            loadConfiguration();

            // Cloud-ready: Console logging (no file system dependency)
            logger.info("Application logging initialized with structured format");

            // Initialize database connection with environment-based configuration
            DatabaseService dbService = new DatabaseService();
            dbService.connect();
        } catch (Exception e) {
            logger.error("Failed to initialize application", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }

    private void loadConfiguration() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (inputStream != null) {
                Properties props = new Properties();
                props.load(inputStream);
                logger.info("Configuration loaded from classpath resource: {}", CONFIG_RESOURCE);
                logger.debug("Loaded {} configuration properties", props.size());
            } else {
                logger.warn("Configuration resource not found: {}, using environment variables", CONFIG_RESOURCE);
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration from classpath", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }

    private void startServer() {
        ServerSocket serverSocket = null;
        try {
            // Cloud-ready: Port from environment variable
            serverSocket = new ServerSocket(SERVER_PORT);
            logger.info("Server started successfully",
                       "port", SERVER_PORT,
                       "environment", System.getenv().getOrDefault("ENVIRONMENT", "unknown"));
            logger.info("Server ready to accept connections on port {}", SERVER_PORT);

            // Simulate server running
            Thread.sleep(1000);

        } catch (Exception e) {
            logger.error("Failed to start server on port {}", SERVER_PORT, e);
            throw new RuntimeException("Server startup failed", e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    logger.info("Server socket closed gracefully");
                } catch (IOException e) {
                    logger.error("Failed to close server socket", e);
                }
            }
        }
    }
}
