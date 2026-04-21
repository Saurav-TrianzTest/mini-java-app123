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
    
    // FIXED blocker-1 & blocker-2: Replaced absolute file paths with GCS bucket paths
    private static final String CONFIG_FILE_PATH = System.getenv().getOrDefault("GCS_CONFIG_BUCKET", "gs://app-config-bucket") + "/app.properties";
    private static final String LOG_FILE_PATH = System.getenv().getOrDefault("GCS_LOG_BUCKET", "gs://app-logs-bucket") + "/mini-app.log";
    
    private Storage storage;
    
    public MiniApp() {
        // Initialize Google Cloud Storage client
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
        
        // Initialize database connection with externalized configuration
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // FIXED blocker-1: Using GCS instead of absolute file path
            String bucketName = extractBucketName(CONFIG_FILE_PATH);
            String objectName = extractObjectName(CONFIG_FILE_PATH);
            
            Blob blob = storage.get(BlobId.of(bucketName, objectName));
            if (blob != null && blob.exists()) {
                Properties props = new Properties();
                props.load(new ByteArrayInputStream(blob.getContent()));
                System.out.println("Configuration loaded from GCS: " + CONFIG_FILE_PATH);
            } else {
                System.out.println("Warning: Configuration file not found in GCS: " + CONFIG_FILE_PATH);
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration from GCS: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("GCS access error: " + e.getMessage());
        }
    }
    
    private void initializeLogging() {
        try {
            // FIXED blocker-2 & blocker-3: Using GCS for log storage instead of local filesystem
            String bucketName = extractBucketName(LOG_FILE_PATH);
            String objectName = extractObjectName(LOG_FILE_PATH);
            
            // Create initial log file in GCS
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
            
            String initialLogContent = "Application started at: " + System.currentTimeMillis();
            storage.create(blobInfo, initialLogContent.getBytes(StandardCharsets.UTF_8));
            
            System.out.println("Logging initialized in GCS: " + LOG_FILE_PATH);
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
    
    /**
     * Extract bucket name from GCS path (gs://bucket-name/object-path)
     */
    private String extractBucketName(String gcsPath) {
        if (gcsPath.startsWith("gs://")) {
            String path = gcsPath.substring(5);
            int slashIndex = path.indexOf('/');
            return slashIndex > 0 ? path.substring(0, slashIndex) : path;
        }
        return gcsPath;
    }
    
    /**
     * Extract object name from GCS path (gs://bucket-name/object-path)
     */
    private String extractObjectName(String gcsPath) {
        if (gcsPath.startsWith("gs://")) {
            String path = gcsPath.substring(5);
            int slashIndex = path.indexOf('/');
            return slashIndex > 0 ? path.substring(slashIndex + 1) : "";
        }
        return gcsPath;
    }
}
