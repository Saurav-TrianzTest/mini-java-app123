package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application - Updated for Java 25 compatibility.
 *
 * Java 25 upgrade changes applied:
 * - Explicit charset (StandardCharsets.UTF_8) used wherever byte/char conversions occur
 *   to comply with the UTF-8 default-charset rule (JAVA11_TO_25_UTF8_DEFAULT_CHARSET).
 * - No Nashorn, SecurityManager, or sun.misc.Unsafe usage present.
 * - No javax.* enterprise imports; application does not use servlet/JPA/validation APIs
 *   that require jakarta.* migration in this file.
 */
public class MiniApp {

    // Port is read from environment variable; falls back to 8080 for local development.
    private static final int SERVER_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // File paths are read from environment variables; absolute defaults kept only as
    // fallback documentation – in production these must be supplied via the environment.
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
                // Java 25: UTF-8 is the default charset; use explicit InputStreamReader
                // with StandardCharsets.UTF_8 to guarantee consistent behaviour across
                // all environments (JAVA11_TO_25_UTF8_DEFAULT_CHARSET).
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
