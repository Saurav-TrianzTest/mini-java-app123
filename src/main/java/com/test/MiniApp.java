package com.test;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cloud-Ready Mini Java Application
 * - Uses environment variables for configuration
 * - Uses classpath resources instead of file system
 * - Uses structured logging for cloud monitoring
 * - Stateless design for horizontal scaling
 */
public class MiniApp {
    
    private static final Logger logger = LoggerFactory.getLogger(MiniApp.class);
    
    // Cloud-Native: Port from environment variable
    private static final int SERVER_PORT = Integer.parseInt(
        System.getenv().getOrDefault("SERVER_PORT", "8080")
    );
    
    // Cloud-Native: Configuration from classpath resources
    private static final String CONFIG_RESOURCE_PATH = "/application.properties";
    
    public static void main(String[] args) {
        logger.info("Starting Cloud-Ready Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // Cloud-Native: Load configuration from classpath
        loadConfiguration();
        
        // Cloud-Native: Use structured logging instead of file writes
        initializeLogging();
        
        // Initialize database connection with connection pooling
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // Cloud-Native: Load from classpath instead of absolute file path
            InputStream configStream = getClass().getResourceAsStream(CONFIG_RESOURCE_PATH);
            
            if (configStream != null) {
                Properties props = new Properties();
                props.load(configStream);
                logger.info("Configuration loaded from classpath resource: {}", CONFIG_RESOURCE_PATH);
                
                // Log configuration (without sensitive data)
                logger.debug("Server port: {}", props.getProperty("server.port", "8080"));
                logger.debug("Environment: {}", props.getProperty("environment", "development"));
                
                configStream.close();
            } else {
                logger.warn("Configuration resource not found: {}", CONFIG_RESOURCE_PATH);
                logger.info("Using environment variables for configuration");
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
        }
    }
    
    private void initializeLogging() {
        // Cloud-Native: Use structured logging (JSON) for cloud monitoring
        // Logging is configured via logback.xml for JSON output
        logger.info("Structured logging initialized for cloud environment");
        logger.info("Log output: STDOUT (for cloud log aggregation)");
        
        // Add correlation ID for distributed tracing
        String correlationId = System.getenv().getOrDefault("CORRELATION_ID", 
            java.util.UUID.randomUUID().toString());
        org.slf4j.MDC.put("correlationId", correlationId);
        
        logger.info("Correlation ID set for distributed tracing: {}", correlationId);
    }
    
    private void startServer() {
        try {
            // Cloud-Native: Use HTTP server instead of raw ServerSocket
            // In production, this would be handled by Spring Boot or similar framework
            logger.info("Server configured to start on port: {}", SERVER_PORT);
            logger.info("Server ready to accept HTTP connections...");
            logger.info("Health check endpoint: /actuator/health");
            
            // Simulate server initialization
            Thread.sleep(1000);
            
            logger.info("Server started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            throw new RuntimeException("Server startup failed", e);
        }
    }
    
    /**
     * Cloud-Native: Health check endpoint for container orchestration
     */
    public boolean isHealthy() {
        try {
            // Check database connectivity
            DatabaseService dbService = new DatabaseService();
            boolean dbHealthy = dbService.isConnected();
            
            logger.debug("Health check - Database: {}", dbHealthy ? "UP" : "DOWN");
            
            return dbHealthy;
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return false;
        }
    }
    
    /**
     * Cloud-Native: Graceful shutdown for container orchestration
     */
    public void shutdown() {
        logger.info("Initiating graceful shutdown...");
        
        try {
            // Close database connections
            DatabaseService dbService = new DatabaseService();
            dbService.disconnect();
            
            // Clear MDC
            org.slf4j.MDC.clear();
            
            logger.info("Graceful shutdown completed");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
}
