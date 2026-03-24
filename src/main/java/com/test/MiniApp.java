package com.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Mini Java Application - Cloud-Ready Version
 * Fixed cloud readiness issues:
 * - Replaced hardcoded file paths with environment variables
 * - Replaced hardcoded ports with environment variable configuration
 * - Using classpath resources instead of absolute file paths
 * - Removed direct file system dependencies
 */
@SpringBootApplication
public class MiniApp {
    
    // FIXED: Port now comes from environment variable or application.properties
    // No hardcoded port - Spring Boot manages this via ${SERVER_PORT:8080}
    
    // FIXED: Configuration file loaded from classpath instead of absolute path
    private static final String CONFIG_FILE_CLASSPATH = "application.properties";
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application (Cloud-Ready)...");
        
        // Start Spring Boot application - handles port configuration automatically
        ConfigurableApplicationContext context = SpringApplication.run(MiniApp.class, args);
        
        MiniApp app = new MiniApp();
        app.initializeApplication(context);
    }
    
    private void initializeApplication(ConfigurableApplicationContext context) {
        Environment env = context.getEnvironment();
        
        // FIXED: Load configuration from classpath or environment variables
        loadConfiguration(env);
        
        // FIXED: Use console logging instead of file-based logging (cloud-native)
        initializeLogging();
        
        // Initialize database connection with connection pooling
        DatabaseService dbService = context.getBean(DatabaseService.class);
        dbService.testConnection();
        
        // Display server configuration from environment
        displayServerConfiguration(env);
    }
    
    private void loadConfiguration(Environment env) {
        try {
            // FIXED: Load from classpath resources instead of absolute file path
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_CLASSPATH);
            
            if (inputStream != null) {
                Properties props = new Properties();
                props.load(inputStream);
                System.out.println("Configuration loaded from classpath: " + CONFIG_FILE_CLASSPATH);
                inputStream.close();
            } else {
                System.out.println("Using environment-based configuration (no classpath properties file)");
            }
            
            // FIXED: Configuration can be overridden by environment variables
            String configDir = env.getProperty("APP_CONFIG_DIRECTORY", "classpath:/config");
            System.out.println("Configuration directory: " + configDir);
            
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        // FIXED: Use console logging for cloud environments (stdout/stderr)
        // Cloud platforms (AWS CloudWatch, Azure Monitor, GCP Cloud Logging) capture console output
        System.out.println("Logging initialized - using console output for cloud compatibility");
        System.out.println("Logs will be captured by cloud logging services (CloudWatch, Stackdriver, etc.)");
        
        // FIXED: Log directory now comes from environment variable if needed
        String logDir = System.getenv("APP_LOG_DIRECTORY");
        if (logDir != null) {
            System.out.println("Optional log directory from environment: " + logDir);
        } else {
            System.out.println("Using cloud-native console logging (recommended for containers)");
        }
    }
    
    private void displayServerConfiguration(Environment env) {
        // FIXED: Port comes from environment variable or application.properties
        String serverPort = env.getProperty("server.port", "8080");
        String serverHost = env.getProperty("server.host", "0.0.0.0");
        
        System.out.println("=== Server Configuration (Cloud-Ready) ===");
        System.out.println("Server Host: " + serverHost);
        System.out.println("Server Port: " + serverPort + " (from environment/config)");
        System.out.println("Server ready to accept connections...");
        System.out.println("=========================================");
    }
}
