package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service - Cloud-Ready Version
 * FIXED: All hardcoded credentials replaced with environment variables
 * FIXED: Configuration externalized for AWS Secrets Manager/Parameter Store integration
 * FIXED: Connection details now retrieved from environment variables
 */
public class DatabaseService {
    
    // FIXED: Database connection details from environment variables
    private static final String DB_HOST = getEnvOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = getEnvOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = getEnvOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    
    // FIXED: Credentials from environment variables (AWS Secrets Manager integration)
    private static final String DB_USERNAME = getEnvOrDefault("DB_USERNAME", "app_user");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD"); // No default for security
    
    // FIXED: Cache server details from environment variables
    private static final String REDIS_HOST = getEnvOrDefault("REDIS_HOST", "localhost");
    private static final int REDIS_PORT = getEnvOrDefaultInt("REDIS_PORT", 6379);
    
    // FIXED: API endpoints from environment variables
    private static final String EXTERNAL_API_URL = getEnvOrDefault("EXTERNAL_API_URL", "http://api.example.com/v1");
    private static final String PAYMENT_SERVICE_URL = getEnvOrDefault("PAYMENT_SERVICE_URL", "https://payment.example.com/process");
    
    // FIXED: API keys from environment variables (AWS Secrets Manager)
    private static final String EXTERNAL_API_KEY = System.getenv("EXTERNAL_API_KEY");
    private static final String PAYMENT_SERVICE_TOKEN = System.getenv("PAYMENT_SERVICE_TOKEN");
    
    private Connection connection;
    
    /**
     * Helper method to get environment variable with default value
     */
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
    
    /**
     * Helper method to get integer environment variable with default value
     */
    private static int getEnvOrDefaultInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for " + key + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    public void connect() {
        try {
            System.out.println("Connecting to database...");
            
            // Validate required credentials are provided
            if (DB_PASSWORD == null || DB_PASSWORD.isEmpty()) {
                System.err.println("ERROR: DB_PASSWORD environment variable is required");
                System.err.println("Please configure AWS Secrets Manager or set DB_PASSWORD environment variable");
                return;
            }
            
            // FIXED: JDBC driver loaded dynamically
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // FIXED: Connection using environment-configured credentials
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            
            System.out.println("Connected to database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);
            System.out.println("Database connection successful - credentials loaded from environment");
            
            // FIXED: Cache connection using environment variables
            connectToCache();
            
            // FIXED: External services using environment variables
            initializeExternalServices();
            
        } catch (ClassNotFoundException e) {
            System.err.println("Database driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            System.err.println("Please verify DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, and DB_PASSWORD environment variables");
        }
    }
    
    /**
     * FIXED: Redis connection using environment variables
     */
    private void connectToCache() {
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        
        String redisPassword = System.getenv("REDIS_PASSWORD");
        if (redisPassword != null && !redisPassword.isEmpty()) {
            System.out.println("Redis authentication enabled - using password from environment");
        }
        
        // Simulate cache connection
        System.out.println("Cache connection configured from environment variables");
    }
    
    /**
     * FIXED: External services using environment variables
     */
    private void initializeExternalServices() {
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        
        if (EXTERNAL_API_KEY != null && !EXTERNAL_API_KEY.isEmpty()) {
            System.out.println("External API authentication configured from environment");
        } else {
            System.err.println("WARNING: EXTERNAL_API_KEY not set - API calls may fail");
        }
        
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
        
        if (PAYMENT_SERVICE_TOKEN != null && !PAYMENT_SERVICE_TOKEN.isEmpty()) {
            System.out.println("Payment service authentication configured from environment");
        } else {
            System.err.println("WARNING: PAYMENT_SERVICE_TOKEN not set - payment calls may fail");
        }
    }
    
    public void executeQuery(String sql) {
        try {
            if (connection != null && !connection.isClosed()) {
                PreparedStatement stmt = connection.prepareStatement(sql);
                
                // FIXED: Query timeout from environment variable
                int queryTimeout = getEnvOrDefaultInt("DB_QUERY_TIMEOUT", 30);
                stmt.setQueryTimeout(queryTimeout);
                
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