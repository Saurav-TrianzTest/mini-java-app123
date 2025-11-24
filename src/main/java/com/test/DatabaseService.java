package com.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Modernized database service using Spring JDBC and connection pooling
 * Replaces hardcoded connection details with externalized configuration
 */
@Service
public class DatabaseService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    // Externalized configuration values
    @Value("${cache.redis.host}")
    private String redisHost;

    @Value("${cache.redis.port}")
    private int redisPort;

    @Value("${external.api.base-url}")
    private String externalApiUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    private boolean initialized = false;

    /**
     * Constructor with dependency injection
     * Modern approach using DataSource and JdbcTemplate instead of DriverManager
     */
    @Autowired
    public DatabaseService(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /**
     * Initialize connection pool and services after bean construction
     * Replaces manual connect() method with automatic initialization
     */
    @PostConstruct
    public void connect() {
        try {
            System.out.println("Connecting to database...");

            // Test connection from pool
            try (Connection connection = dataSource.getConnection()) {
                System.out.println("Connected to database: " + dbUrl);
                System.out.println("Using username: " + dbUsername);
                initialized = true;
            }

            // Initialize external services
            connectToCache();
            initializeExternalServices();

        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    /**
     * Connect to cache using externalized configuration
     */
    private void connectToCache() {
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort);
        // Simulate cache connection
    }

    /**
     * Initialize external services using externalized configuration
     */
    private void initializeExternalServices() {
        System.out.println("Initializing external API: " + externalApiUrl);
        System.out.println("Initializing payment service: " + paymentServiceUrl);
    }

    /**
     * Execute SQL query using JdbcTemplate
     * Modern approach with automatic resource management and exception handling
     */
    public void executeQuery(String sql) {
        try {
            if (sql == null || sql.trim().isEmpty()) {
                return;
            }

            System.out.println("Executing query: " + sql);
            jdbcTemplate.execute(sql);

        } catch (DataAccessException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    /**
     * Cleanup method called before bean destruction
     * Connection pool is managed by Spring, no manual close needed
     */
    @PreDestroy
    public void disconnect() {
        try {
            // Connection pool cleanup is handled by Spring
            // This method maintained for backward compatibility
            if (initialized) {
                System.out.println("Database connection closed");
                initialized = false;
            }
        } catch (Exception e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }

    /**
     * Get DataSource for advanced use cases
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Get JdbcTemplate for advanced database operations
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}