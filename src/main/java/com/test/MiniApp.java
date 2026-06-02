package com.test;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application updated for Azure cloud readiness.
 */
public class MiniApp {

    // Server port is externalized via environment variable / Azure App Configuration
    private static final int SERVER_PORT = Integer.parseInt(
            System.getenv().getOrDefault("APP_SERVER_PORT", "8080")
    );

    // Azure Blob Storage configuration for configuration and logging
    private static final String BLOB_CONNECTION_STRING = System.getenv().getOrDefault(
            "AZURE_STORAGE_CONNECTION_STRING",
            "DefaultEndpointsProtocol=https;AccountName=youraccount;AccountKey=yourkey;EndpointSuffix=core.windows.net"
    );
    private static final String CONFIG_CONTAINER = System.getenv().getOrDefault("APP_CONFIG_CONTAINER", "app-config");
    private static final String CONFIG_BLOB_NAME = System.getenv().getOrDefault("APP_CONFIG_BLOB", "app.properties");
    private static final String LOG_CONTAINER = System.getenv().getOrDefault("APP_LOG_CONTAINER", "app-logs");
    private static final String LOG_BLOB_NAME = System.getenv().getOrDefault("APP_LOG_BLOB", "mini-app.log");

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        loadConfiguration();
        initializeLogging();

        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    private void loadConfiguration() {
        try {
            BlobClient configBlobClient = new BlobClientBuilder()
                    .connectionString(BLOB_CONNECTION_STRING)
                    .containerName(CONFIG_CONTAINER)
                    .blobName(CONFIG_BLOB_NAME)
                    .buildClient();

            if (configBlobClient.exists()) {
                Properties props = new Properties();
                try (InputStream is = configBlobClient.openInputStream()) {
                    props.load(is);
                }
                System.out.println("Configuration loaded from Azure Blob Storage: " + CONFIG_CONTAINER + "/" + CONFIG_BLOB_NAME);
            } else {
                System.out.println("Warning: Configuration blob not found in container: " + CONFIG_CONTAINER);
            }
        } catch (Exception e) {
            System.err.println("Failed to load configuration from Azure Blob Storage: " + e.getMessage());
        }
    }

    private void initializeLogging() {
        try {
            BlobClient logBlobClient = new BlobClientBuilder()
                    .connectionString(BLOB_CONNECTION_STRING)
                    .containerName(LOG_CONTAINER)
                    .blobName(LOG_BLOB_NAME)
                    .buildClient();

            if (!logBlobClient.exists()) {
                logBlobClient.uploadFromFile(createEmptyTempLogFile());
            }

            System.out.println("Logging initialized in Azure Blob Storage at: " + LOG_CONTAINER + "/" + LOG_BLOB_NAME);
        } catch (Exception e) {
            System.err.println("Failed to initialize logging in Azure Blob Storage: " + e.getMessage());
        }
    }

    private String createEmptyTempLogFile() throws IOException {
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("mini-app-log", ".log");
        return tempFile.toString();
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