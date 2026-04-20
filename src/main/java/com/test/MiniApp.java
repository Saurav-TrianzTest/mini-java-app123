package com.test;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application with containerization fixes applied
 */
public class MiniApp {
    
    // FIXED blocker-7: Externalized port configuration using environment variable
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
    
    // FIXED blocker-1, blocker-2: Replaced absolute file paths with GCS bucket/object paths
    private static final String CONFIG_BUCKET = System.getenv().getOrDefault("GCS_CONFIG_BUCKET", "app-config-bucket");
    private static final String CONFIG_OBJECT = System.getenv().getOrDefault("GCS_CONFIG_OBJECT", "app.properties");
    private static final String LOG_BUCKET = System.getenv().getOrDefault("GCS_LOG_BUCKET", "app-logs-bucket");
    private static final String LOG_OBJECT = System.getenv().getOrDefault("GCS_LOG_OBJECT", "mini-app.log");
    
    private Storage storage;
    
    public MiniApp() {
        // Initialize GCS client
        this.storage = StorageOptions.getDefaultInstance().getService();
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // Load configuration from GCS
        loadConfiguration();
        
        // Initialize logging to GCS
        initializeLogging();
        
        // Initialize database connection with externalized values
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // FIXED blocker-1: Using GCS instead of absolute file path
            BlobId blobId = BlobId.of(CONFIG_BUCKET, CONFIG_OBJECT);
            Blob blob = storage.get(blobId);
            
            if (blob != null && blob.exists()) {
                byte[] content = blob.getContent();
                Properties props = new Properties();
                props.load(new ByteArrayInputStream(content));
                System.out.println("Configuration loaded from GCS: gs://" + CONFIG_BUCKET + "/" + CONFIG_OBJECT);
            } else {
                System.out.println("Warning: Configuration file not found in GCS: gs://" + CONFIG_BUCKET + "/" + CONFIG_OBJECT);
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration from GCS: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("GCS access error: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        try {
            // FIXED blocker-2, blocker-3, blocker-4: Using GCS for log storage instead of local filesystem
            String logMessage = "Application initialized at " + System.currentTimeMillis();
            BlobId blobId = BlobId.of(LOG_BUCKET, LOG_OBJECT);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("text/plain")
                    .build();
            
            storage.create(blobInfo, logMessage.getBytes(StandardCharsets.UTF_8));
            System.out.println("Logging initialized in GCS: gs://" + LOG_BUCKET + "/" + LOG_OBJECT);
        } catch (Exception e) {
            System.err.println("Failed to initialize logging in GCS: " + e.getMessage());
        }
    }
    
    private void startServer() {
        try {
            // FIXED blocker-8: Using externalized port configuration
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
