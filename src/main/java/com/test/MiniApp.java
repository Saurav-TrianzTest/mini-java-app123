package com.test;

import java.io.File; // java.io.File retained - used for config/log file checks
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application - updated for Java 25 compatibility.
 *
 * Changes made for Java 11 → Java 25 upgrade:
 *  - JAVA11_TO_25_UTF8_DEFAULT_CHARSET (line 44 in original):
 *    Properties.load() was called with a raw FileInputStream, which relies on
 *    the platform default charset. Java 18+ changed the default charset to UTF-8
 *    (JEP 400), but to guarantee identical behaviour across all Java versions and
 *    platforms the FileInputStream is now wrapped in an explicit
 *    InputStreamReader(fis, StandardCharsets.UTF_8) before being passed to
 *    Properties.load(Reader).
 *  - No Nashorn, SecurityManager, or sun.misc.Unsafe usages were present.
 */
public class MiniApp {

    // Hardcoded port number (containerization concern, not a Java-version blocker)
    private static final int SERVER_PORT = 8080;

    // Hardcoded absolute file paths (containerization concern, not a Java-version blocker)
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

        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    private void loadConfiguration() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                Properties props = new Properties();
                // JAVA11_TO_25_UTF8_DEFAULT_CHARSET fix (original line 44):
                // Wrap FileInputStream in an explicit UTF-8 InputStreamReader so that
                // Properties.load(Reader) uses a well-defined charset on every JVM,
                // regardless of the platform default (which changed to UTF-8 in Java 18
                // via JEP 400 but was locale-dependent in Java 11).
                try (FileInputStream fis = new FileInputStream(configFile);
                     InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                    props.load(isr);
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
