package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application - updated for Java 25 compatibility.
 *
 * Changes applied:
 *  - FIX JAVA11_TO_25_UTF8_DEFAULT_CHARSET: All byte/character conversions now
 *    explicitly specify StandardCharsets.UTF_8 instead of relying on the
 *    platform default charset (which changed to UTF-8 in Java 17+ but must be
 *    explicit for correctness).
 *  - FIX JAVA11_TO_25_DEPENDENCY_UPDATES: Class compiled with --release 25.
 *  - NOTE: No Nashorn, SecurityManager, sun.misc.Unsafe, or finalize() usage
 *    was present in this file, so those critical rules do not apply here.
 */
public class MiniApp {

    // Port is read from the environment variable SERVER_PORT; falls back to 8080.
    private static final int SERVER_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // Config/log paths are read from environment variables for container-friendliness.
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

        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    private void loadConfiguration() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                Properties props = new Properties();
                // FIX JAVA11_TO_25_UTF8_DEFAULT_CHARSET: Use explicit UTF-8 charset
                // so that locale-encoded files from Java 11 are not mis-decoded.
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
