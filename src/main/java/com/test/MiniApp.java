package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mini Java Application with intentional containerization blockers for testing
 */
@SpringBootApplication
public class MiniApp {
    
    // cz-java-0061 fix – Lines 15,79: SERVER_PORT was hardcoded to 8080; now sourced from SERVER_PORT env var
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
    
    // BLOCKER: Hardcoded absolute file path
    private static final String CONFIG_FILE_PATH = System.getenv("CONFIG_FILE_PATH") != null ? System.getenv("CONFIG_FILE_PATH") : "/opt/app/config/app.properties";
    private static final String LOG_FILE_PATH = System.getenv("LOG_FILE_PATH") != null ? System.getenv("LOG_FILE_PATH") : "/var/log/mini-app.log";
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        SpringApplication.run(MiniApp.class, args);
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
    }
    
    private void initializeApplication() {
        // BLOCKER: Reading from hardcoded absolute path
        loadConfiguration();
        
        // BLOCKER: Writing to hardcoded absolute path
        initializeLogging();
        
        // Initialize database connection with hardcoded values
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    private void loadConfiguration() {
        try {
            // BLOCKER: Hardcoded absolute file path
            File configFile = new File(CONFIG_FILE_PATH);
            if (configFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(configFile));
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
            // BLOCKER: Hardcoded absolute path for log file
            // EFS-backed PersistentVolume mount path via environment variable (cz-java-0058)
            String logDirPath = System.getenv("LOG_DIR_PATH") != null ? System.getenv("LOG_DIR_PATH") : "/mnt/efs/logs";
            File logDir = new File(logDirPath);
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
}
