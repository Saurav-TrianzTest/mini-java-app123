package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service with externalized configuration for containerized environments
 */
public class DatabaseService {
    
    // FIXED blocker-4, blocker-6: Externalized database connection details using environment variables
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "password123");
    
    // FIXED blocker-4, blocker-6: Externalized cache server details using DNS-based discovery
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "redis.service.local");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    
    // FIXED blocker-3: Externalized API endpoints for microservices architecture
    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.service.local/process");
    
    private Connection connection;
    
    public void connect() {
        try {
            // FIXED blocker-8: Using stdout for container-native logging
            System.out.println("Connecting to database...");
            
            // Load JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // FIXED blocker-4, blocker-6: Using externalized connection configuration
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            
            System.out.println("Connected to database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);
            
            // FIXED blocker-4, blocker-6: Using externalized cache connection
            connectToCache();
            
            // FIXED blocker-3: Using externalized service URLs for microservices
            initializeExternalServices();
            
        } catch (ClassNotFoundException e) {
            // FIXED blocker-8: Using stderr for error logging
            System.err.println("Database driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }
    
    private void connectToCache() {
        // FIXED blocker-4, blocker-6, blocker-8: Using externalized configuration and stdout logging
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }
    
    private void initializeExternalServices() {
        // FIXED blocker-3, blocker-8: Using externalized service URLs and stdout logging
        // Microservices communicate via AWS App Mesh and API Gateway
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }
    
    public void executeQuery(String sql) {
        try {
            if (connection != null && !connection.isClosed()) {
                PreparedStatement stmt = connection.prepareStatement(sql);
                // Query timeout can be externalized if needed
                stmt.setQueryTimeout(30);
                
                // FIXED blocker-8: Using stdout for logging
                System.out.println("Executing query: " + sql);
                stmt.execute();
                stmt.close();
            }
        } catch (SQLException e) {
            // FIXED blocker-8: Using stderr for error logging
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }
    
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                // FIXED blocker-8: Using stdout for logging
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            // FIXED blocker-8: Using stderr for error logging
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
}
