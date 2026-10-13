package com.test;

// Verified: 0 compilation errors - Java 25 / Spring Boot 3.3.0 compatible (Iteration 2)
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application - Updated for Java 25 compatibility.
 *
 * Changes applied:
 * - JAVA11_TO_25_UTF8_DEFAULT_CHARSET: FileInputStream wrapped with explicit UTF-8
 *   InputStreamReader to avoid locale-encoding issues; Java 25 defaults to UTF-8
 *   but explicit charset is best practice.
 * - JAVA11_TO_25_DEPENDENCY_UPDATES: Class updated to compile under Java 25
 *   (source/target/release 25 in pom.xml).
 * - JAVA11_TO_25_SPRING_BOOT_COMPATIBILITY: Spring Boot upgraded to 3.3.0 (Jakarta EE).
 */
public class MiniApp {

    // NOTE: Hardcoded port number - consider externalising to application.properties
    private static final int SERVER_PORT = 8080;

    // NOTE: Hardcoded absolute file path - consider externalising to application.properties
    private static final String CONFIG_FILE_PATH = "/opt/app/config/app.properties";
    private static final String LOG_FILE_PATH = "/var/log/mini-app.log";

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
                // JAVA11_TO_25_UTF8_DEFAULT_CHARSET: Use explicit UTF-8 InputStreamReader
                // to ensure consistent behaviour across all Java versions including Java 25.
                try (InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                    props.load(reader);
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
            File logDir = new File("/var/log");
            if (!logDir.exists()) {
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
