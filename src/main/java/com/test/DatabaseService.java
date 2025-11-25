package com.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Modernized database service using Spring DataSource with connection pooling,
 * externalized configuration, retry logic, and transaction management.
 */
@Service
public class DatabaseService {

    private final DataSource dataSource;

    @Value("${cache.redis.host}")
    private String redisHost;

    @Value("${cache.redis.port}")
    private int redisPort;

    @Value("${external.api.base-url}")
    private String externalApiUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${spring.jpa.properties.hibernate.query.query_timeout:30}")
    private int queryTimeout;

    public DatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Gets a connection from the connection pool with retry logic for resilience.
     * Uses exponential backoff strategy for transient failures.
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Connection getConnection() throws SQLException {
        System.out.println("Getting connection from DataSource pool...");
        Connection connection = dataSource.getConnection();
        System.out.println("Connection acquired from pool");
        return connection;
    }

    private void connectToCache() {
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort);
        // Redis connection logic using externalized configuration
    }

    private void initializeExternalServices() {
        System.out.println("Initializing external API: " + externalApiUrl);
        System.out.println("Initializing payment service: " + paymentServiceUrl);
    }

    /**
     * Executes a parameterized query with proper resource management.
     * Uses try-with-resources for automatic connection and statement cleanup.
     *
     * @param sql SQL query with parameter placeholders (?)
     * @param params Parameters to bind to the query
     */
    @Transactional
    public void executeQuery(String sql, Object... params) {
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setQueryTimeout(queryTimeout);

            // Bind parameters to prevent SQL injection vulnerabilities
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            System.out.println("Executing parameterized query with " + params.length + " parameters");
            stmt.execute();

        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
            throw new RuntimeException("Database query execution failed", e);
        }
    }

    /**
     * Initializes external service connections (cache, APIs, etc.)
     * using externalized configuration values.
     */
    public void initialize() {
        connectToCache();
        initializeExternalServices();
    }
}