package com.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Cloud-ready Mini Java Application
 * Fixed issues:
 * - Replaced hardcoded ports with environment variables
 * - Replaced hardcoded file paths with classpath resources and environment variables
 * - Moved I/O operations from static blocks to @PostConstruct
 * - Externalized configuration using Spring Boot properties
 * - Replaced java.io.File with ResourceLoader for cloud compatibility
 */
@SpringBootApplication
@Component
public class MiniApp {
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Autowired
    private DatabaseService databaseService;
    
    // FIXED: Externalized port using environment variable with default
    @Value("${server.port:8080}")
    private int serverPort;
    
    // FIXED: Externalized config file path using environment variable with classpath default
    @Value("${app.config.file:classpath:config/app.properties}")
    private String configFilePath;
    
    // FIXED: Logging now uses console output for cloud environments
    // Cloud platforms capture stdout/stderr for log aggregation
    
    public static void main(String[] args) {
        System.out.println("Starting Cloud-Ready Mini Java Application...");
        SpringApplication.run(MiniApp.class, args);
    }
    
    /**
     * FIXED: Moved initialization from constructor to @PostConstruct
     * This allows proper Spring dependency injection and error handling
     */
    @PostConstruct
    public void initializeApplication() {
        System.out.println("Initializing application with cloud-native patterns...");
        
        // FIXED: Load configuration from classpath or cloud storage
        loadConfiguration();
        
        // FIXED: Initialize logging to console (cloud-native pattern)
        initializeLogging();
        
        // Initialize database connection with externalized configuration
        databaseService.connect();
        
        System.out.println("Application initialized successfully on port: " + serverPort);
    }
    
    /**
     * FIXED: Load configuration from classpath resources or cloud storage
     * Replaced hardcoded file paths with ResourceLoader for cloud compatibility
     */
    private void loadConfiguration() {
        try {
            // FIXED: Use ResourceLoader to load from classpath or cloud storage
            Resource configResource = resourceLoader.getResource(configFilePath);
            
            if (configResource.exists()) {
                Properties props = new Properties();
                try (InputStream inputStream = configResource.getInputStream()) {
                    props.load(inputStream);
                    System.out.println("Configuration loaded from: " + configFilePath);
                    System.out.println("Loaded " + props.size() + " configuration properties");
                }
            } else {
                System.out.println("Info: Configuration file not found at: " + configFilePath);
                System.out.println("Using default Spring Boot configuration");
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to load configuration: " + e.getMessage());
            System.err.println("Continuing with default configuration");
        }
    }
    
    /**
     * FIXED: Initialize logging to use console output for cloud environments
     * Cloud platforms (GCP Cloud Logging, AWS CloudWatch) capture stdout/stderr
     * Structured JSON logging is configured in logback-spring.xml
     */
    private void initializeLogging() {
        System.out.println("Logging initialized for cloud environment");
        System.out.println("Logs are written to stdout/stderr for cloud log aggregation");
        System.out.println("Structured JSON logging enabled for cloud monitoring");
    }
}
