package com.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

/**
 * Database service using HikariCP connection pooling with AWS Secrets Manager
 * for credentials and AWS Systems Manager Parameter Store for port/host configuration.
 *
 * Fixes applied:
 *  - blocker-8,9,10,16 : Hard-coded DB credentials replaced with AWS Secrets Manager
 *  - blocker-11,12,13  : Hard-coded ports replaced with AWS SSM Parameter Store / env vars
 *  - blocker-17,18     : Direct JDBC replaced with HikariCP connection pool
 *  - blocker-19        : Connection timeouts configured on HikariCP data source
 */
public class DatabaseService {

    // -----------------------------------------------------------------------
    // Configuration resolved at runtime from AWS Secrets Manager / SSM / env
    // -----------------------------------------------------------------------

    /**
     * Retrieve the AWS region from the environment variable AWS_REGION,
     * defaulting to us-east-1 when not set.
     */
    private static final Region AWS_REGION =
            Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

    /**
     * Name of the AWS Secrets Manager secret that stores DB credentials.
     * Expected secret value is a JSON object:
     *   { "username": "...", "password": "...", "host": "...", "dbname": "..." }
     * Injected via environment variable DB_SECRET_NAME (blocker-8,9,10,16).
     */
    private static final String DB_SECRET_NAME =
            System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/db-credentials");

    /**
     * SSM Parameter Store path for the database port (blocker-11,12,13).
     * Injected via environment variable DB_PORT_PARAM_NAME.
     */
    private static final String DB_PORT_PARAM_NAME =
            System.getenv().getOrDefault("DB_PORT_PARAM_NAME", "/mini-app/db/port");

    /**
     * SSM Parameter Store path for the Redis port (blocker-11,12,13).
     */
    private static final String REDIS_PORT_PARAM_NAME =
            System.getenv().getOrDefault("REDIS_PORT_PARAM_NAME", "/mini-app/redis/port");

    /**
     * SSM Parameter Store path for the external API URL (blocker-11,12,13).
     */
    private static final String EXTERNAL_API_PARAM_NAME =
            System.getenv().getOrDefault("EXTERNAL_API_PARAM_NAME", "/mini-app/external-api/url");

    // -----------------------------------------------------------------------
    // HikariCP data source (replaces raw DriverManager — blocker-17,18,19)
    // -----------------------------------------------------------------------
    private HikariDataSource dataSource;

    // -----------------------------------------------------------------------
    // AWS clients
    // -----------------------------------------------------------------------
    private final SecretsManagerClient secretsManagerClient;
    private final SsmClient ssmClient;
    private final ObjectMapper objectMapper;

    public DatabaseService() {
        // Build AWS clients with explicit connection/read timeouts (blocker-19)
        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(AWS_REGION)
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5)))
                .build();

        this.ssmClient = SsmClient.builder()
                .region(AWS_REGION)
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5)))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void connect() {
        try {
            System.out.println("Resolving database credentials from AWS Secrets Manager...");

            // Fetch credentials from AWS Secrets Manager (blocker-8,9,10,16)
            DbCredentials creds = fetchDbCredentials();

            // Fetch port from AWS SSM Parameter Store (blocker-11,12,13)
            String dbPort = fetchSsmParameter(DB_PORT_PARAM_NAME, "3306");

            String dbUrl = "jdbc:mysql://" + creds.host + ":" + dbPort + "/" + creds.dbName;

            // Build HikariCP pool (blocker-17,18,19)
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(dbUrl);
            hikariConfig.setUsername(creds.username);
            hikariConfig.setPassword(creds.password);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Connection pool sizing
            hikariConfig.setMaximumPoolSize(20);
            hikariConfig.setMinimumIdle(5);

            // Timeout configuration (blocker-19)
            hikariConfig.setConnectionTimeout(30_000);   // 30 s — max wait for a connection from pool
            hikariConfig.setIdleTimeout(600_000);        // 10 min — idle connection eviction
            hikariConfig.setMaxLifetime(1_800_000);      // 30 min — max connection lifetime
            hikariConfig.setKeepaliveTime(60_000);       // 1 min — keepalive ping
            hikariConfig.setInitializationFailTimeout(10_000); // 10 s — fail fast on startup

            // RDS-friendly validation query
            hikariConfig.setConnectionTestQuery("SELECT 1");

            dataSource = new HikariDataSource(hikariConfig);

            System.out.println("HikariCP connection pool initialised. JDBC URL: " + dbUrl);

            // Connect to cache using SSM-resolved port (blocker-11,12,13)
            connectToCache();

            // Initialise external services using SSM-resolved URLs (blocker-11,12,13)
            initializeExternalServices();

        } catch (Exception e) {
            System.err.println("Database initialisation failed: " + e.getMessage());
        }
    }

    public void executeQuery(String sql) {
        // Obtain a connection from the HikariCP pool (blocker-17,18)
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setQueryTimeout(30);
            System.out.println("Executing query: " + sql);
            stmt.execute();

        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool closed");
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Fetch database credentials from AWS Secrets Manager (blocker-8,9,10,16).
     * The secret is expected to be a JSON object with keys:
     *   username, password, host, dbname
     */
    private DbCredentials fetchDbCredentials() throws Exception {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(DB_SECRET_NAME)
                .build();

        GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
        String secretJson = response.secretString();

        JsonNode node = objectMapper.readTree(secretJson);
        DbCredentials creds = new DbCredentials();
        creds.username = node.path("username").asText("root");
        creds.password = node.path("password").asText();
        creds.host     = node.path("host").asText("localhost");
        creds.dbName   = node.path("dbname").asText("mini_app_db");
        return creds;
    }

    /**
     * Fetch a string parameter from AWS SSM Parameter Store (blocker-11,12,13).
     * Falls back to {@code defaultValue} if the parameter cannot be retrieved.
     */
    private String fetchSsmParameter(String paramName, String defaultValue) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(paramName)
                    .withDecryption(true)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            System.err.println("Could not fetch SSM parameter '" + paramName
                    + "', using default '" + defaultValue + "': " + e.getMessage());
            return defaultValue;
        }
    }

    private void connectToCache() {
        // Redis host/port resolved from SSM Parameter Store (blocker-11,12,13)
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
        String redisPort = fetchSsmParameter(REDIS_PORT_PARAM_NAME, "6379");
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort);
        // Actual Redis client initialisation would go here
    }

    private void initializeExternalServices() {
        // External API URL resolved from SSM Parameter Store (blocker-11,12,13)
        String externalApiUrl = fetchSsmParameter(EXTERNAL_API_PARAM_NAME,
                "http://api.example.com/v1");
        String paymentServiceUrl = System.getenv()
                .getOrDefault("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");

        System.out.println("Initializing external API: " + externalApiUrl);
        System.out.println("Initializing payment service: " + paymentServiceUrl);
    }

    // -----------------------------------------------------------------------
    // Inner helper class
    // -----------------------------------------------------------------------

    private static class DbCredentials {
        String username;
        String password;
        String host;
        String dbName;
    }
}
