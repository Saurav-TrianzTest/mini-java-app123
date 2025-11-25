package com.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * Modernized Database service using Spring JDBC Template and DataSource
 * with externalized configuration and proper resource management
 */
@Service
@Transactional
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Value("${db.query.timeout:30}")
    private int queryTimeout;

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${external.api.base-url}")
    private String externalApiUrl;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    /**
     * Initialize database connection using Spring's managed DataSource
     */
    public void connect() {
        try {
            log.info("Verifying database connection...");

            // Test connection using try-with-resources
            try (var connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    log.info("Database connection established successfully");
                    log.info("Database URL: {}", connection.getMetaData().getURL());
                } else {
                    log.error("Database connection validation failed");
                }
            }

            connectToCache();
            initializeExternalServices();

        } catch (Exception e) {
            log.error("Database connection failed", e);
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    private void connectToCache() {
        log.info("Connecting to Redis cache at: {}:{}", redisHost, redisPort);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        log.info("Initializing external API: {}", externalApiUrl);
        log.info("Initializing payment service: {}", paymentServiceUrl);
    }

    /**
     * Execute parameterized query using JdbcTemplate for SQL injection prevention
     * @param sql SQL query with placeholders
     * @param params Query parameters
     */
    @Transactional
    public void executeQuery(String sql, Object... params) {
        try {
            log.debug("Executing parameterized query: {}", sql);

            // Set query timeout
            jdbcTemplate.setQueryTimeout(queryTimeout);

            // Execute query with parameters (prevents SQL injection)
            if (params != null && params.length > 0) {
                jdbcTemplate.update(sql, params);
            } else {
                jdbcTemplate.execute(sql);
            }

            log.info("Query executed successfully");

        } catch (Exception e) {
            log.error("Query execution failed for SQL: {}", sql, e);
            throw new RuntimeException("Query execution failed", e);
        }
    }

    /**
     * No explicit disconnect needed - Spring manages connection lifecycle
     * Connection pooling (HikariCP) handles connection management automatically
     */
    public void disconnect() {
        log.info("Connection pool managed by Spring - no explicit disconnect needed");
    }
}