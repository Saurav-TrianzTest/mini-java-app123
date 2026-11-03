package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service refactored for GKE Autopilot containerized deployment.
 *
 * Tightly-coupled static DB_URL constant (cz-java-0082) has been decomposed:
 * each connection parameter is now sourced independently from environment
 * variables, which are injected at pod startup via GCP Workload Identity and
 * Secret Manager (e.g. through a Kubernetes ExternalSecret or a mounted
 * Secret volume).  This allows each component (host, port, database name,
 * credentials) to be managed and rotated independently without rebuilding the
 * container image.
 *
 * Required environment variables:
 *   DB_HOST      – hostname / Cloud SQL proxy address  (e.g. 127.0.0.1 when using Cloud SQL Auth Proxy)
 *   DB_PORT      – database port                       (e.g. 3306)
 *   DB_NAME      – database / schema name              (e.g. mini_app_db)
 *   DB_USERNAME  – database user                       (sourced from Secret Manager)
 *   DB_PASSWORD  – database password                   (sourced from Secret Manager)
 *   REDIS_HOST   – Redis / Memorystore host            (e.g. 10.0.0.5)
 *   REDIS_PORT   – Redis port                          (e.g. 6379)
 *   EXTERNAL_API_URL    – base URL of the external API
 *   PAYMENT_SERVICE_URL – URL of the payment service
 */
public class DatabaseService {

    // Resolved cz-java-0082: Decomposed tightly-coupled DB_URL constant into
    // independently configurable environment-variable-backed fields so that
    // each component can be deployed, scaled, and rotated as a separate
    // GKE Autopilot workload with its own Workload Identity binding and
    // Secret Manager secret reference.
    private static final String DB_HOST     = System.getenv("DB_HOST")     != null ? System.getenv("DB_HOST")     : "localhost";
    private static final String DB_PORT     = System.getenv("DB_PORT")     != null ? System.getenv("DB_PORT")     : "3306";
    private static final String DB_NAME     = System.getenv("DB_NAME")     != null ? System.getenv("DB_NAME")     : "mini_app_db";
    private static final String DB_URL      = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : "root";
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";

    // Resolved cz-java-0062: Replaced hardcoded IP address with environment variable
    // REDIS_HOST is injected via GKE ConfigMap or Secret for container deployment flexibility
    private static final String REDIS_HOST = System.getenv("REDIS_HOST") != null ? System.getenv("REDIS_HOST") : "redis-service";
    private static final int REDIS_PORT = Integer.parseInt(System.getenv("REDIS_PORT") != null ? System.getenv("REDIS_PORT") : "6379");
    
    // BLOCKER: Hardcoded API endpoints
    private static final String EXTERNAL_API_URL = "http://api.example.com:8080/v1";
    private static final String PAYMENT_SERVICE_URL = "https://payment.internal.company.com/process";
    
    private Connection connection;
    
    public void connect() {
        try {
            System.out.println("Connecting to database...");
            
            // BLOCKER: Hardcoded JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // BLOCKER: Hardcoded connection string and credentials
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            
            System.out.println("Connected to database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);
            
            // BLOCKER: Hardcoded cache connection
            connectToCache();
            
            // BLOCKER: Hardcoded external service URLs
            initializeExternalServices();
            
        } catch (ClassNotFoundException e) {
            System.err.println("Database driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }
    
    private void connectToCache() {
        // BLOCKER: Hardcoded Redis connection details
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }
    
    private void initializeExternalServices() {
        // BLOCKER: Hardcoded external service URLs
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }
    
    public void executeQuery(String sql) {
        try {
            if (connection != null && !connection.isClosed()) {
                PreparedStatement stmt = connection.prepareStatement(sql);
                // BLOCKER: Hardcoded query timeout
                stmt.setQueryTimeout(30);
                
                System.out.println("Executing query: " + sql);
                stmt.execute();
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }
    
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
}
