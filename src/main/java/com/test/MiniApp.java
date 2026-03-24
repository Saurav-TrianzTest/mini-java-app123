package com.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Mini Java Application - Cloud-Ready with externalized configuration
 * 
 * FIXED ISSUES:
 * - Replaced hardcoded file paths with environment variables and classpath resources
 * - Replaced hardcoded port with environment variable configuration
 * - Externalized configuration to use Spring Boot's configuration management
 * - Removed direct file system dependencies
 */
@SpringBootApplication
public class MiniApp {
    
    // FIXED: Port now configured via environment variable or application.properties
    // Default value provided as fallback
    private static final String SERVER_PORT_ENV = "SERVER_PORT";
    private static final String DEFAULT_PORT = "8080";
    
    // FIXED: Configuration loaded from classpath or environment variables
    private static final String CONFIG_FILE_CLASSPATH = "application.properties";
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application (Cloud-Ready)...");
        
        // FIXED: Use Spring Boot's externalized configuration
        // Configuration can be provided via:
        // 1. Environment variables
        // 2. Command-line arguments
        // 3. application.properties in classpath
        // 4. External configuration files via --spring.config.location
        SpringApplication.run(MiniApp.class, args);
        
        System.out.println("Application started successfully!");
    }
    
    /**
     * FIXED: Load configuration from classpath resources instead of absolute file paths
     * This allows the application to run in any environment without file system dependencies
     */
    @Bean
    public Properties applicationProperties() {
        Properties props = new Properties();
        
        try {
            // FIXED: Load from classpath resource (packaged with application)
            Resource resource = new ClassPathResource(CONFIG_FILE_CLASSPATH);
            
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    props.load(inputStream);
                    System.out.println("Configuration loaded from classpath: " + CONFIG_FILE_CLASSPATH);
                }
            } else {
                System.out.println("Using default configuration - no classpath resource found");
            }
            
            // FIXED: Override with environment variables if present
            overrideWithEnvironmentVariables(props);
            
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            System.out.println("Using default configuration values");
        }
        
        return props;
    }
    
    /**
     * FIXED: Allow environment variables to override configuration
     * This follows 12-factor app principles for cloud-native applications
     */
    private void overrideWithEnvironmentVariables(Properties props) {
        // Database configuration from environment
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null && !dbUrl.isEmpty()) {
            props.setProperty("spring.datasource.url", dbUrl);
            System.out.println("Database URL overridden from environment variable");
        }
        
        String dbUsername = System.getenv("DATABASE_USERNAME");
        if (dbUsername != null && !dbUsername.isEmpty()) {
            props.setProperty("spring.datasource.username", dbUsername);
        }
        
        String dbPassword = System.getenv("DATABASE_PASSWORD");
        if (dbPassword != null && !dbPassword.isEmpty()) {
            props.setProperty("spring.datasource.password", dbPassword);
        }
        
        // Server port from environment
        String serverPort = System.getenv(SERVER_PORT_ENV);
        if (serverPort != null && !serverPort.isEmpty()) {
            props.setProperty("server.port", serverPort);
            System.out.println("Server port overridden from environment variable: " + serverPort);
        }
        
        // Redis configuration from environment
        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost != null && !redisHost.isEmpty()) {
            props.setProperty("spring.redis.host", redisHost);
        }
        
        String redisPort = System.getenv("REDIS_PORT");
        if (redisPort != null && !redisPort.isEmpty()) {
            props.setProperty("spring.redis.port", redisPort);
        }
        
        // External API configuration from environment
        String externalApiUrl = System.getenv("EXTERNAL_API_URL");
        if (externalApiUrl != null && !externalApiUrl.isEmpty()) {
            props.setProperty("external.api.base-url", externalApiUrl);
        }
        
        String apiKey = System.getenv("EXTERNAL_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            props.setProperty("external.api.key", apiKey);
        }
        
        System.out.println("Configuration loaded with environment variable overrides");
    }
    
    /**
     * FIXED: Logging now uses console output for cloud environments
     * Cloud platforms capture stdout/stderr and forward to centralized logging
     * No file system dependencies required
     */
    private void initializeLogging() {
        // FIXED: Use console logging instead of file-based logging
        // Cloud platforms (AWS CloudWatch, Azure Monitor, GCP Cloud Logging) 
        // automatically capture console output
        System.out.println("Logging initialized - using console output for cloud compatibility");
        System.out.println("Logs will be captured by cloud platform logging service");
    }
}
