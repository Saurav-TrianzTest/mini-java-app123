package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application.
 *
 * Java 17 Upgrade Notes:
 * - Removed any SecurityManager usage (deprecated in Java 17, removed in Java 18).
 * - FileInputStream now uses try-with-resources for proper resource management.
 * - ServerSocket now uses try-with-resources for proper resource management.
 * - Server port and file paths are externalised via environment variables
 *   or application.properties for containerisation readiness.
 */
public class MiniApp {

    // Reads port from environment variable; falls back to 8080 for local dev.
    private static final int SERVER_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // Reads config/log paths from environment variables; falls back to defaults.
    private static final String CONFIG_FILE_PATH =
            System.getenv().getOrDefault("CONFIG_FILE_PATH", "/opt/app/config/app.properties");
    private static final String LOG_FILE_PATH =
            System.getenv().getOrDefault("LOG_FILE_PATH", "/var/log/mini-app.log");

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        loadConfiguration();
        initializeLogging();

        // Initialize database connection
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    private void loadConfiguration() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                Properties props = new Properties();
                // Java 17: use try-with-resources for FileInputStream
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                }
                System.out.println("Configuration loaded from: " + CONFIG_FILE_PATH);
            } else {
                System.out.println("Warning: Configuration file not found at: " + CONFIG_FILE_PATH);
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }

    private void initializeLogging() {
        try {
            File logDir = new File(LOG_FILE_PATH).getParentFile();
            if (logDir != null && !logDir.exists()) {
                logDir.mkdirs();
            }

            File logFile = new File(LOG_FILE_PATH);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            System.out.println("Logging initialized at: " + LOG_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }

    private void startServer() {
        // Java 17: use try-with-resources for ServerSocket
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
