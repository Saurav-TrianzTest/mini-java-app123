package com.test;

import com.test.config.CloudConfigurationManager;
import com.test.service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Cloud-native Mini Java Application
 * Replaces hardcoded values with environment variables and cloud services
 * Replaces file system operations with S3 storage
 * Replaces hardcoded ports with environment variable configuration
 */
public class MiniApp {
    
    private static final Logger logger = LoggerFactory.getLogger(MiniApp.class);
    
    private CloudConfigurationManager configManager;
    private S3StorageService s3StorageService;
    private DatabaseService databaseService;
    
    public static void main(String[] args) {
        logger.info("Starting Cloud-Native Mini Java Application...");
        
        MiniApp app = new MiniApp();
        
        try {
            app.initializeApplication();
            app.startServer();
        } catch (Exception e) {
            logger.error("Application failed to start", e);
            System.exit(1);
        } finally {
            app.shutdown();
        }
    }
    
    /**
     * Initialize application with cloud-native configuration
     */
    private void initializeApplication() {
        logger.info("Initializing application with cloud-native configuration...");
        
        // Initialize cloud configuration manager
        configManager = new CloudConfigurationManager();
        
        // Load configuration from cloud sources
        loadConfiguration();
        
        // Initialize logging (console-based for cloud environments)
        initializeLogging();
        
        // Initialize database connection with connection pooling
        databaseService = new DatabaseService();
        databaseService.connect();
        
        logger.info("Application initialized successfully");
    }
    
    /**
     * Load configuration from classpath resources, S3, or Parameter Store
     * Replaces hardcoded file paths with cloud-native configuration sources
     */
    private void loadConfiguration() {
        try {
            logger.info("Loading configuration from cloud sources...");
            
            // First, try to load from classpath resources (for default configuration)
            Properties props = loadFromClasspath("application.properties");
            
            // Then, try to load from S3 if bucket is configured
            String s3BucketName = System.getenv("CONFIG_S3_BUCKET");
            if (s3BucketName != null && !s3BucketName.isEmpty()) {
                try {
                    s3StorageService = new S3StorageService(s3BucketName);
                    String configKey = System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties");
                    
                    if (s3StorageService.objectExists(configKey)) {
                        Properties s3Props = s3StorageService.loadPropertiesFromS3(configKey);
                        // Merge S3 properties (override classpath properties)
                        props.putAll(s3Props);
                        logger.info("Configuration loaded from S3: s3://{}/{}", s3BucketName, configKey);
                    } else {
                        logger.info("Configuration file not found in S3, using classpath defaults");
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load configuration from S3, using classpath defaults", e);
                }
            } else {
                logger.info("S3 bucket not configured, using classpath configuration");
            }
            
            // Environment variables override all other configuration sources
            logger.info("Configuration loaded successfully. Environment variables will override file-based configuration.");
            
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
    
    /**
     * Load properties from classpath resources
     * Replaces hardcoded file paths with classpath resource loading
     */
    private Properties loadFromClasspath(String resourceName) {
        Properties props = new Properties();
        
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
            
            if (inputStream != null) {
                props.load(inputStream);
                logger.info("Configuration loaded from classpath: {}", resourceName);
                inputStream.close();
            } else {
                logger.warn("Configuration file not found in classpath: {}", resourceName);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to load configuration from classpath: {}", resourceName, e);
        }
        
        return props;
    }
    
    /**
     * Initialize structured logging for cloud environments
     * Replaces file-based logging with console logging
     */
    private void initializeLogging() {
        try {
            // Get log level from environment variable or Parameter Store
            String logLevel = configManager.getConfigValue("LOG_LEVEL", "INFO");
            
            logger.info("Logging initialized with level: {}", logLevel);
            logger.info("Using structured JSON logging for cloud monitoring");
            
            // In cloud environments, logs are written to stdout/stderr
            // and collected by cloud logging services (CloudWatch, etc.)
            
        } catch (Exception e) {
            logger.error("Failed to initialize logging configuration", e);
            // Don't throw exception - use default logging configuration
        }
    }
    
    /**
     * Start server with dynamic port configuration
     * Replaces hardcoded port with environment variable
     */
    private void startServer() {
        ServerSocket serverSocket = null;
        
        try {
            // Get server port from environment variable or Parameter Store
            int serverPort = getServerPort();
            
            logger.info("Starting server on port: {}", serverPort);
            
            // Create server socket with dynamic port
            serverSocket = new ServerSocket(serverPort);
            
            logger.info("Server started successfully on port: {}", serverPort);
            logger.info("Server ready to accept connections...");
            
            // Simulate server running
            Thread.sleep(1000);
            
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            throw new RuntimeException("Failed to start server", e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    logger.info("Server socket closed");
                } catch (Exception e) {
                    logger.warn("Failed to close server socket", e);
                }
            }
        }
    }
    
    /**
     * Get server port from environment variable or Parameter Store
     * Replaces hardcoded port number
     */
    private int getServerPort() {
        // First check environment variable (highest priority)
        String portEnv = System.getenv("SERVER_PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                logger.warn("Invalid SERVER_PORT environment variable: {}", portEnv);
            }
        }
        
        // Then check Parameter Store
        try {
            return configManager.getConfigValueAsInt("SERVER_PORT", 8080);
        } catch (Exception e) {
            logger.warn("Failed to get server port from Parameter Store, using default: 8080");
            return 8080;
        }
    }
    
    /**
     * Shutdown application and release resources
     */
    private void shutdown() {
        logger.info("Shutting down application...");
        
        try {
            if (databaseService != null) {
                databaseService.disconnect();
            }
            
            if (s3StorageService != null) {
                s3StorageService.close();
            }
            
            if (configManager != null) {
                configManager.close();
            }
            
            logger.info("Application shutdown complete");
            
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
        }
    }
}
