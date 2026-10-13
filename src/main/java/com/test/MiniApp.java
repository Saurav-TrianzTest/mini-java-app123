package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application
 *
 * Java 25 upgrade notes:
 * - Explicit charset (StandardCharsets.UTF_8) used wherever bytes/chars are converted,
 *   because Java 25 mandates UTF-8 as the default charset (JAVA11_TO_25_UTF8_DEFAULT_CHARSET).
 * - No SecurityManager usage (removed in Java 24+).
 * - No Nashorn / jdk.nashorn references.
 * - No sun.misc.Unsafe usage.
 */
public class MiniApp {

    // Port read from environment variable; falls back to 8080 for local development
    private static final int SERVER_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // File paths read from environment variables; fall back to local-dev defaults
    private static final String CONFIG_FILE_PATH =
            System.getenv().getOrDefault("APP_CONFIG_FILE", "/opt/app/config/app.properties");
    private static final String LOG_FILE_PATH =
            System.getenv().getOrDefault("APP_LOG_FILE", "/var/log/mini-app.log");

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
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                Properties props = new Properties();
                // Java 25: UTF-8 is the default charset; use explicit InputStreamReader for clarity
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(new java.io.InputStreamReader(fis, StandardCharsets.UTF_8));
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
        try {
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
