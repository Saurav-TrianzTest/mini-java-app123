package com.test;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application with containerization fixes applied
 */
public class MiniApp {
    
    // FIXED: Externalized port configuration using environment variable
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
    
    // FIXED: Replaced absolute file paths with Azure Blob Storage paths
    private static final String CONFIG_BLOB_NAME = System.getenv().getOrDefault("CONFIG_BLOB_NAME", "config/app.properties");
    private static final String LOG_BLOB_NAME = System.getenv().getOrDefault("LOG_BLOB_NAME", "logs/mini-app.log");
    
    // Azure Blob Storage configuration
    private static final String AZURE_STORAGE_CONNECTION_STRING = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
    private static final String AZURE_STORAGE_CONTAINER = System.getenv().getOrDefault("AZURE_STORAGE_CONTAINER", "mini-app-storage");
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // FIXED: Reading from Azure Blob Storage instead of hardcoded absolute path
        loadConfiguration();
        
        // FIXED: Writing to Azure Blob Storage instead of hardcoded absolute path
        initializeLogging();
        
        // Initialize database connection with environment variables
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // FIXED: Using Azure Blob Storage instead of hardcoded absolute file path
            if (AZURE_STORAGE_CONNECTION_STRING != null && !AZURE_STORAGE_CONNECTION_STRING.isEmpty()) {
                BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(AZURE_STORAGE_CONNECTION_STRING)
                    .buildClient();
                
                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(AZURE_STORAGE_CONTAINER);
                BlobClient blobClient = containerClient.getBlobClient(CONFIG_BLOB_NAME);
                
                if (blobClient.exists()) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    blobClient.download(outputStream);
                    
                    Properties props = new Properties();
                    props.load(new ByteArrayInputStream(outputStream.toByteArray()));
                    System.out.println("Configuration loaded from Azure Blob Storage: " + CONFIG_BLOB_NAME);
                } else {
                    System.out.println("Warning: Configuration blob not found: " + CONFIG_BLOB_NAME);
                }
            } else {
                System.out.println("Warning: Azure Storage connection string not configured");
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        try {
            // FIXED: Using Azure Blob Storage instead of hardcoded absolute path for log file
            if (AZURE_STORAGE_CONNECTION_STRING != null && !AZURE_STORAGE_CONNECTION_STRING.isEmpty()) {
                BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(AZURE_STORAGE_CONNECTION_STRING)
                    .buildClient();
                
                BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(AZURE_STORAGE_CONTAINER);
                
                // Create container if it doesn't exist
                if (!containerClient.exists()) {
                    containerClient.create();
                }
                
                BlobClient blobClient = containerClient.getBlobClient(LOG_BLOB_NAME);
                
                // Initialize log blob with empty content if it doesn't exist
                if (!blobClient.exists()) {
                    String initialLogContent = "Log initialized at: " + java.time.Instant.now() + "\n";
                    blobClient.upload(new ByteArrayInputStream(initialLogContent.getBytes()), initialLogContent.length());
                }
                
                System.out.println("Logging initialized in Azure Blob Storage: " + LOG_BLOB_NAME);
            } else {
                System.out.println("Warning: Azure Storage connection string not configured for logging");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }
    
    private void startServer() {
        try {
            // FIXED: Using externalized port configuration from environment variable
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT);
            System.out.println("Server ready to accept connections...");
            
            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
