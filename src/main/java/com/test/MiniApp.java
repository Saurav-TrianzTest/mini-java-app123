package com.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Mini Java Application - Cloud-Ready Version
 * Fixed cloud readiness issues:
 * - Replaced hardcoded file paths with environment variables and classpath resources
 * - Replaced hardcoded port with environment variable configuration
 * - Externalized configuration to support cloud deployment
 * - Uses Spring Boot for cloud-native patterns
 */
@SpringBootApplication
public class MiniApp {
    
    private final Environment environment;
    
    public MiniApp(Environment environment) {
        this.environment = environment;
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application (Cloud-Ready)...");
        SpringApplication.run(MiniApp.class, args);
    }
    
    @PostConstruct
    public void initializeApplication() {
        System.out.println("Initializing cloud-ready application...");
        
        // Load configuration from classpath or environment variables
        loadConfiguration();
        
        // Initialize logging (using console for cloud environments)
        initializeLogging();
        
        // Display server configuration from environment
        displayServerConfiguration();
    }
    
    /**
     * FIXED: Replaced hardcoded file path with classpath resource loading
     * Configuration can be externalized via Spring profiles or environment variables
     */
    private void loadConfiguration() {
        try {
            // Try to load from classpath (packaged with application)
            InputStream configStream = getClass().getClassLoader().getResourceAsStream("application.properties");
            
            if (configStream != null) {
                Properties props = new Properties();
                props.load(configStream);
                System.out.println("Configuration loaded from classpath: application.properties");
                configStream.close();
            } else {
                System.out.println("Using externalized configuration from environment variables");
            }
            
            // Configuration values are now managed by Spring Boot's externalized configuration
            // Priority: Environment Variables > application.properties > defaults
            
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            System.out.println("Falling back to environment variable configuration");
        }
    }
    
    /**
     * FIXED: Replaced file-based logging with console logging for cloud environments
     * Cloud platforms capture stdout/stderr for centralized logging
     */
    private void initializeLogging() {
        // Cloud-native approach: Log to stdout/stderr
        // Cloud platforms (AWS CloudWatch, Azure Monitor, GCP Cloud Logging) capture console output
        System.out.println("Logging configured for cloud environment (console output)");
        System.out.println("Logs will be captured by cloud platform logging service");
        
        // Structured logging can be added via Logback/Log4j2 with JSON format
        // for better integration with cloud monitoring tools
    }
    
    /**
     * FIXED: Server port now configured via environment variable
     * Reads from SERVER_PORT environment variable or defaults to Spring Boot's server.port property
     */
    private void displayServerConfiguration() {
        // Get port from environment variable or Spring property
        String serverPort = environment.getProperty("server.port", 
                                                    System.getenv().getOrDefault("SERVER_PORT", "8080"));
        
        String serverHost = environment.getProperty("server.host", 
                                                    System.getenv().getOrDefault("SERVER_HOST", "0.0.0.0"));
        
        System.out.println("Server configuration:");
        System.out.println("  Host: " + serverHost + " (configured via environment)");
        System.out.println("  Port: " + serverPort + " (configured via environment)");
        System.out.println("Server ready to accept connections...");
    }
    
    /**
     * Configuration bean for accessing environment properties
     */
    @Bean
    public ConfigurationProperties configurationProperties(Environment env) {
        return new ConfigurationProperties(env);
    }
    
    /**
     * Helper class for accessing externalized configuration
     */
    public static class ConfigurationProperties {
        private final Environment environment;
        
        public ConfigurationProperties(Environment environment) {
            this.environment = environment;
        }
        
        public String getProperty(String key, String defaultValue) {
            // Priority: Environment Variable > Spring Property > Default Value
            String envKey = key.toUpperCase().replace('.', '_');
            return System.getenv().getOrDefault(envKey, 
                   environment.getProperty(key, defaultValue));
        }
    }
}
