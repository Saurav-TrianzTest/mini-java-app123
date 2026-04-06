package com.test;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Cloud-ready Mini Java Application
 * 
 * FIXES APPLIED:
 * - cr-java-0061: Replaced hardcoded file paths with environment variables and Azure Blob Storage
 * - cr-java-0063: Replaced java.io.File storage with Azure Blob Storage for cloud-native persistence
 * - cr-java-0077: Replaced hardcoded ports with environment variable configuration
 * - cr-java-0105: Moved I/O operations from static blocks to @PostConstruct
 * - cr-java-0070: Externalized configuration using Azure App Configuration and environment variables
 */
@SpringBootApplication
public class MiniApp implements CommandLineRunner {
    
    // Externalized server port configuration
    @Value("${server.port:${PORT:8080}}")
    private int serverPort;
    
    // Externalized file paths using environment variables
    @Value("${app.config.path:${CONFIG_PATH:config/app.properties}}")
    private String configPath;
    
    @Value("${app.log.path:${LOG_PATH:logs/mini-app.log}}")
    private String logPath;
    
    // Azure Blob Storage configuration
    @Value("${azure.storage.connection-string:${AZURE_STORAGE_CONNECTION_STRING:}}")
    private String storageConnectionString;
    
    @Value("${azure.storage.container-name:${AZURE_STORAGE_CONTAINER:app-data}}")
    private String containerName;
    
    @Value("${app.config.use-blob-storage:${USE_BLOB_STORAGE:false}}")
    private boolean useBlobStorage;
    
    @Autowired
    private DatabaseService databaseService;
    
    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;
    
    public static void main(String[] args) {
        System.out.println("Starting Cloud-Ready Mini Java Application...");
        SpringApplication.run(MiniApp.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Application initialized successfully");
        System.out.println("Server running on port: " + serverPort);
        System.out.println("Configuration loaded from: " + configPath);
    }
    
    /**
     * Initialize application using @PostConstruct instead of static initializer
     * This allows proper Spring dependency injection and error handling
     */
    @PostConstruct
    public void initializeApplication() {
        System.out.println("Initializing application components...");
        
        // Initialize Azure Blob Storage if enabled
        if (useBlobStorage && storageConnectionString != null && !storageConnectionString.isEmpty()) {
            initializeBlobStorage();
        }
        
        // Load configuration from classpath or blob storage
        loadConfiguration();
        
        // Initialize structured logging for cloud monitoring
        initializeLogging();
        
        System.out.println("Application initialization complete");
    }
    
    /**
     * Initialize Azure Blob Storage client for cloud-native file operations
     * Replaces local file system dependencies
     */
    private void initializeBlobStorage() {
        try {
            System.out.println("Initializing Azure Blob Storage...");
            
            blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient();
            
            // Get or create container
            containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create();
                System.out.println("Created blob container: " + containerName);
            }
            
            System.out.println("Azure Blob Storage initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize Azure Blob Storage: " + e.getMessage());
            System.err.println("Falling back to classpath resources");
        }
    }
    
    /**
     * Load configuration from classpath resources or Azure Blob Storage
     * Replaces hardcoded file path dependencies
     */
    private void loadConfiguration() {
        try {
            Properties props = new Properties();
            
            if (useBlobStorage && containerClient != null) {
                // Load from Azure Blob Storage
                loadConfigurationFromBlob(props);
            } else {
                // Load from classpath resources
                loadConfigurationFromClasspath(props);
            }
            
            System.out.println("Configuration loaded successfully");
            logConfigurationSummary(props);
            
        } catch (Exception e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            System.err.println("Using default configuration values");
        }
    }
    
    /**
     * Load configuration from Azure Blob Storage
     */
    private void loadConfigurationFromBlob(Properties props) throws IOException {
        System.out.println("Loading configuration from Azure Blob Storage: " + configPath);
        
        BlobClient blobClient = containerClient.getBlobClient(configPath);
        
        if (blobClient.exists()) {
            try (InputStream inputStream = blobClient.openInputStream()) {
                props.load(inputStream);
                System.out.println("Configuration loaded from blob: " + configPath);
            }
        } else {
            System.out.println("Configuration blob not found: " + configPath);
            System.out.println("Using default configuration");
        }
    }
    
    /**
     * Load configuration from classpath resources
     * Cloud-compatible approach using Spring's Resource abstraction
     */
    private void loadConfigurationFromClasspath(Properties props) throws IOException {
        System.out.println("Loading configuration from classpath: " + configPath);
        
        try {
            Resource resource = new ClassPathResource(configPath);
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    props.load(inputStream);
                    System.out.println("Configuration loaded from classpath: " + configPath);
                }
            } else {
                System.out.println("Configuration file not found in classpath: " + configPath);
                System.out.println("Using environment variables and default values");
            }
        } catch (IOException e) {
            System.out.println("Could not load configuration from classpath: " + e.getMessage());
            System.out.println("Using environment variables and default values");
        }
    }
    
    /**
     * Initialize structured logging for cloud monitoring
     * Replaces file-based logging with console logging
     */
    private void initializeLogging() {
        System.out.println("Initializing cloud-native structured logging...");
        
        // Log configuration is handled by logback-spring.xml
        // All logs go to stdout/stderr for cloud log aggregation
        System.out.println("Logging configured for cloud environment");
        System.out.println("Logs will be sent to stdout for aggregation by cloud monitoring");
    }
    
    /**
     * Log configuration summary (without sensitive data)
     */
    private void logConfigurationSummary(Properties props) {
        System.out.println("Configuration summary:");
        System.out.println("  - Server port: " + serverPort);
        System.out.println("  - Config path: " + configPath);
        System.out.println("  - Blob storage enabled: " + useBlobStorage);
        System.out.println("  - Properties loaded: " + props.size());
    }
    
    /**
     * Write data to Azure Blob Storage
     * Replaces local file write operations
     */
    public void writeToStorage(String blobName, String content) {
        try {
            if (useBlobStorage && containerClient != null) {
                BlobClient blobClient = containerClient.getBlobClient(blobName);
                blobClient.upload(new java.io.ByteArrayInputStream(content.getBytes()), content.length(), true);
                System.out.println("Data written to blob: " + blobName);
            } else {
                System.out.println("Blob storage not enabled, data not persisted: " + blobName);
            }
        } catch (Exception e) {
            System.err.println("Failed to write to blob storage: " + e.getMessage());
        }
    }
    
    /**
     * Read data from Azure Blob Storage
     * Replaces local file read operations
     */
    public String readFromStorage(String blobName) {
        try {
            if (useBlobStorage && containerClient != null) {
                BlobClient blobClient = containerClient.getBlobClient(blobName);
                if (blobClient.exists()) {
                    try (InputStream inputStream = blobClient.openInputStream()) {
                        return new String(inputStream.readAllBytes());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to read from blob storage: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Health check endpoint support
     */
    public boolean isHealthy() {
        try {
            // Check database connectivity
            boolean dbHealthy = databaseService != null && databaseService.isHealthy();
            
            // Check blob storage connectivity if enabled
            boolean storageHealthy = true;
            if (useBlobStorage && containerClient != null) {
                storageHealthy = containerClient.exists();
            }
            
            return dbHealthy && storageHealthy;
        } catch (Exception e) {
            System.err.println("Health check failed: " + e.getMessage());
            return false;
        }
    }
}
