package com.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Properties;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/**
 * Mini Java Application with intentional containerization blockers for testing
 */
public class MiniApp {
    
    // BLOCKER: Hardcoded port number
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // BLOCKER: Hardcoded absolute file path
    private static final String CONFIG_FILE_PATH = System.getenv().getOrDefault("CONFIG_PATH", "/opt/app/config/app.properties");
    private static final String LOG_FILE_PATH = System.getenv().getOrDefault("LOG_FILE_PATH", "/var/log/mini-app.log");
    
    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // BLOCKER: Reading from hardcoded absolute path
        loadConfiguration();

        // BLOCKER: Writing to hardcoded absolute path
        initializeLogging();

        // Initialize database connection with hardcoded values
        DatabaseService dbService = new DatabaseService();
        dbService.connect();

        // Start health check endpoint for container orchestration
        startHealthCheckEndpoint();
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
        // Container-friendly logging - output to stdout/stderr
        System.out.println("Logging to stdout/stderr for container compatibility");
    }
    
    private void startHealthCheckEndpoint() {
        try {
            int healthPort = Integer.parseInt(System.getenv().getOrDefault("HEALTH_CHECK_PORT", "8081"));
            HttpServer healthServer = HttpServer.create(new InetSocketAddress(healthPort), 0);

            healthServer.createContext("/health", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String response = "{\"status\":\"UP\",\"application\":\"mini-app\"}";
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            });

            healthServer.setExecutor(null);
            healthServer.start();
            System.out.println("Health check endpoint started on port: " + healthPort);
        } catch (Exception e) {
            System.err.println("Failed to start health check endpoint: " + e.getMessage());
        }
    }

    private void startServer() {
        try {
            // BLOCKER: Hardcoded port number
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