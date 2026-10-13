package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Mini Java Application – updated for Java 25 compatibility.
 *
 * <p>Changes applied:
 * <ul>
 *   <li><b>FIX JAVA11_TO_25_UTF8_DEFAULT_CHARSET (charset)</b>: The original
 *       Java 11 code used {@code Properties.load(FileInputStream)} which reads
 *       bytes using the platform default charset.  On Java 17+ the default
 *       charset is UTF-8, but relying on the implicit default is fragile and
 *       was flagged as a compatibility issue.  The fix wraps the
 *       {@link FileInputStream} in an {@link InputStreamReader} with an
 *       explicit {@link StandardCharsets#UTF_8} charset so that behaviour is
 *       identical across all Java versions and all locales.</li>
 *   <li><b>FIX JAVA11_TO_25_DEPENDENCY_UPDATES</b>: Class compiled with
 *       {@code --release 25} via the updated maven-compiler-plugin 3.13.0
 *       configuration in {@code pom.xml}.</li>
 *   <li><b>NOTE</b>: No Nashorn, SecurityManager, {@code sun.misc.Unsafe}, or
 *       {@code finalize()} usage was present in this file, so those critical
 *       migration rules do not apply here.</li>
 * </ul>
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

    /**
     * Loads application configuration from the properties file specified by
     * {@code CONFIG_FILE_PATH}.
     *
     * <p><b>FIX JAVA11_TO_25_UTF8_DEFAULT_CHARSET</b>: The original code called
     * {@code props.load(new FileInputStream(configFile))} which uses the platform
     * default charset.  This has been replaced with an explicit UTF-8
     * {@link InputStreamReader} to guarantee consistent behaviour on Java 25
     * regardless of the JVM locale setting.
     */
    private void loadConfiguration() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                Properties props = new Properties();
                // FIX JAVA11_TO_25_UTF8_DEFAULT_CHARSET:
                // Wrap FileInputStream in an InputStreamReader with explicit UTF-8
                // charset instead of relying on the platform default charset.
                // This ensures consistent decoding on Java 25 across all locales.
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
                boolean created = logDir.mkdirs();
                if (!created) {
                    System.err.println("Warning: Could not create log directory: " + logDir);
                }
            }

            File logFile = new File(LOG_FILE_PATH);
            if (!logFile.exists()) {
                boolean created = logFile.createNewFile();
                if (!created) {
                    System.err.println("Warning: Could not create log file: " + LOG_FILE_PATH);
                }
            }

            System.out.println("Logging initialized at: " + LOG_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }

    private void startServer() {
        // Use try-with-resources to ensure the ServerSocket is always closed.
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
