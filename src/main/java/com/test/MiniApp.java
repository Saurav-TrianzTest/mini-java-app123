package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application
 *
 * Java 25 upgrade notes:
 * - Explicit charset (StandardCharsets.UTF_8) used in Properties.load() via
 *   InputStreamReader to avoid reliance on the platform default charset
 *   (issue-5: JAVA11_TO_25_UTF8_DEFAULT_CHARSET).  Java 25 mandates UTF-8 as
 *   the JVM default, but using an explicit InputStreamReader makes the intent
 *   clear and portable across all JVM versions.
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

    /**
     * Loads application configuration from a properties file.
     *
     * Fix (issue-5): Properties.load(InputStream) uses the platform default
     * charset in Java 11 and earlier.  In Java 25 the default is UTF-8, but
     * relying on that implicitly is fragile.  We now pass an explicit
     * InputStreamReader(fis, StandardCharsets.UTF_8) to Properties.load() so
     * the behaviour is identical on every JVM version.
     */
    private void loadConfiguration() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                Properties props = new Properties();
                // Issue-5 fix: use explicit UTF-8 InputStreamReader instead of
                // the raw InputStream overload to avoid platform-charset dependency.
                try (FileInputStream fis = new FileInputStream(configFile);
                     InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
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
