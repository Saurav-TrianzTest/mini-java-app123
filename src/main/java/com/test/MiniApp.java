package com.test;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application updated for cloud readiness.
 * - Replaces hardcoded file paths with Azure Blob Storage
 * - Externalizes ports and configuration via environment variables
 */
public class MiniApp {

    // Server port is externalized via environment variable (e.g., from Azure App Configuration)
    private static final int SERVER_PORT = Integer.parseInt(
            System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // Azure Storage configuration for configuration and log blobs
    private static final String STORAGE_ACCOUNT_URL = System.getenv().getOrDefault(
            "AZURE_STORAGE_ACCOUNT_URL",
            "https://your-storage-account.blob.core.windows.net");
    private static final String CONFIG_CONTAINER = System.getenv().getOrDefault(
            "CONFIG_CONTAINER_NAME",
            "app-config");
    private static final String CONFIG_BLOB_NAME = System.getenv().getOrDefault(
            "CONFIG_BLOB_NAME",
            "app.properties");

    private static final String LOG_CONTAINER = System.getenv().getOrDefault(
            "LOG_CONTAINER_NAME",
            "app-logs");
    private static final String LOG_BLOB_NAME = System.getenv().getOrDefault(
            "LOG_BLOB_NAME",
            "mini-app.log");

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        // Load configuration from Azure Blob Storage
        loadConfiguration();

        // Initialize logging to Azure Blob Storage
        initializeLogging();

        // Initialize database connection with externalized values
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    private void loadConfiguration() {
        try {
            BlobClient configBlobClient = new BlobClientBuilder()
                    .endpoint(STORAGE_ACCOUNT_URL)
                    .containerName(CONFIG_CONTAINER)
                    .blobName(CONFIG_BLOB_NAME)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();

            if (configBlobClient.exists()) {
                Properties props = new Properties();
                props.load(configBlobClient.openInputStream());
                System.out.println("Configuration loaded from Azure Blob: " + CONFIG_CONTAINER + "/" + CONFIG_BLOB_NAME);
            } else {
                System.out.println("Warning: Configuration blob not found: " + CONFIG_CONTAINER + "/" + CONFIG_BLOB_NAME);
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration from Azure Blob Storage: " + e.getMessage());
        }
    }

    private void initializeLogging() {
        try {
            BlobClient logBlobClient = new BlobClientBuilder()
                    .endpoint(STORAGE_ACCOUNT_URL)
                    .containerName(LOG_CONTAINER)
                    .blobName(LOG_BLOB_NAME)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();

            if (!logBlobClient.exists()) {
                String initialLogContent = "MiniApp log initialized\n";
                ByteArrayInputStream dataStream = new ByteArrayInputStream(
                        initialLogContent.getBytes(StandardCharsets.UTF_8));
                logBlobClient.upload(dataStream, initialLogContent.length(), true);
            }

            System.out.println("Logging initialized in Azure Blob: " + LOG_CONTAINER + "/" + LOG_BLOB_NAME);
        } catch (Exception e) {
            System.err.println("Failed to initialize logging in Azure Blob Storage: " + e.getMessage());
        }
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started on port: " + SERVER_PORT);
            System.out.println("Server ready to accept connections...");

            // Simulate server running
            Thread.sleep(1000);
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}