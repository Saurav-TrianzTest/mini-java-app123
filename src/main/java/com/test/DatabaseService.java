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
 * Database service — cloud-ready version.
 *
 * Blockers addressed:
 *  - cr-java-0069 (blocker-8,9,10)  : Hard-coded DB credentials → AWS Secrets Manager
 *  - cr-java-0113 (blocker-16)       : Lack of Externalized Secrets → AWS Secrets Manager
 *  - cr-java-0077 (blocker-11,12,13) : Hard-coded ports → AWS SSM Parameter Store / env vars
 *  - cr-java-0073 (blocker-17,18)    : Direct JDBC connections → HikariCP connection pool
 *  - cr-java-0097 (blocker-19)       : Missing connection timeouts → HikariCP timeout config
 */
public class DatabaseService {

    // -----------------------------------------------------------------------
    // Configuration keys — values are resolved at runtime from AWS services
    // or environment variables; nothing is hard-coded in source.
    // -----------------------------------------------------------------------

    /** AWS Secrets Manager secret name that stores DB credentials as JSON:
     *  { "username": "...", "password": "...", "host": "...", "dbname": "..." } */
    private static final String DB_SECRET_NAME =
            System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/db-credentials");

    /** AWS SSM Parameter Store path for the DB port (blocker-11, blocker-12, blocker-13) */
    private static final String DB_PORT_PARAM =
            System.getenv().getOrDefault("DB_PORT_PARAM", "/mini-app/db/port");

    /** AWS SSM Parameter Store path for the Redis port */
    private static final String REDIS_PORT_PARAM =
            System.getenv().getOrDefault("REDIS_PORT_PARAM", "/mini-app/cache/redis/port");

    /** AWS region — injected via environment variable */
    private static final String AWS_REGION =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // -----------------------------------------------------------------------
    // HikariCP DataSource — replaces raw DriverManager (blocker-17, blocker-18)
    // -----------------------------------------------------------------------
    private HikariDataSource dataSource;

    // -----------------------------------------------------------------------
    // AWS clients
    // -----------------------------------------------------------------------
    private final SecretsManagerClient secretsClient;
    private final SsmClient ssmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DatabaseService() {
        Region region = Region.of(AWS_REGION);
        this.secretsClient = SecretsManagerClient.builder()
                .region(region)
                .build();
        this.ssmClient = SsmClient.builder()
                .region(region)
                .build();
    }

    // -----------------------------------------------------------------------
    // Secret / parameter helpers
    // -----------------------------------------------------------------------

    /**
     * Retrieves the DB credentials JSON from AWS Secrets Manager.
     * Addresses blocker-8, blocker-9, blocker-10, blocker-16.
     */
    private JsonNode getDbCredentials() {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(DB_SECRET_NAME)
                    .build();
            GetSecretValueResponse response = secretsClient.getSecretValue(request);
            return objectMapper.readTree(response.secretString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve DB credentials from AWS Secrets Manager: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a port number from AWS SSM Parameter Store.
     * Addresses blocker-11, blocker-12, blocker-13.
     */
    private String getSsmParameter(String paramName) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(paramName)
                    .withDecryption(true)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            System.err.println("Warning: Could not retrieve SSM parameter '" + paramName
                    + "': " + e.getMessage() + ". Falling back to environment variable.");
            // Fallback: derive env-var name from param path, e.g. /mini-app/db/port → DB_PORT
            String envKey = paramName.replaceAll("^/", "")
                                     .replace("/", "_")
                                     .replace("-", "_")
                                     .toUpperCase();
            return System.getenv(envKey);
        }
    }

    // -----------------------------------------------------------------------
    // Connection pool initialisation (blocker-17, blocker-18, blocker-19)
    // -----------------------------------------------------------------------

    public void connect() {
        System.out.println("Retrieving database credentials from AWS Secrets Manager...");

        // Fetch credentials — no hard-coded values (blocker-8, blocker-9, blocker-10, blocker-16)
        JsonNode creds = getDbCredentials();
        String dbUsername = creds.path("username").asText();
        String dbPassword = creds.path("password").asText();
        String dbHost     = creds.path("host").asText();
        String dbName     = creds.path("dbname").asText();

        // Fetch port from SSM Parameter Store (blocker-11, blocker-12, blocker-13)
        String dbPort = getSsmParameter(DB_PORT_PARAM);
        if (dbPort == null || dbPort.isEmpty()) {
            dbPort = System.getenv().getOrDefault("DB_PORT", "3306");
        }

        String jdbcUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;

        // Build HikariCP pool — replaces raw DriverManager (blocker-17, blocker-18)
        // Explicit timeouts prevent indefinite hangs in cloud environments (blocker-19)
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Connection pool sizing
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);

        // Timeout configuration (blocker-19: Missing Connection Timeouts)
        config.setConnectionTimeout(Duration.ofSeconds(30).toMillis());   // max wait for pool connection
        config.setIdleTimeout(Duration.ofMinutes(10).toMillis());          // idle connection eviction
        config.setMaxLifetime(Duration.ofMinutes(30).toMillis());          // max connection lifetime
        config.setKeepaliveTime(Duration.ofMinutes(5).toMillis());         // keepalive ping interval
        config.setInitializationFailTimeout(Duration.ofSeconds(60).toMillis());

        // JDBC-level socket/connect timeouts passed via connection properties
        config.addDataSourceProperty("connectTimeout", "10000");   // 10 s TCP connect
        config.addDataSourceProperty("socketTimeout",  "30000");   // 30 s socket read

        config.setPoolName("MiniAppHikariPool");

        this.dataSource = new HikariDataSource(config);
        System.out.println("HikariCP connection pool initialised for: " + jdbcUrl);

        // Initialise supporting services
        connectToCache();
        initializeExternalServices();
    }

    private void connectToCache() {
        // Redis host and port are resolved from environment variables / SSM Parameter Store
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "");
        String redisPort = getSsmParameter(REDIS_PORT_PARAM);
        if (redisPort == null || redisPort.isEmpty()) {
            redisPort = System.getenv().getOrDefault("REDIS_PORT", "6379");
        }
        System.out.println("Connecting to Redis cache at: " + redisHost + ":" + redisPort);
        // Actual Redis client initialisation would go here
    }

    private void initializeExternalServices() {
        // External service URLs are resolved from environment variables — no hard-coded values
        String externalApiUrl    = System.getenv().getOrDefault("EXTERNAL_API_URL", "");
        String paymentServiceUrl = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "");
        System.out.println("Initializing external API: " + externalApiUrl);
        System.out.println("Initializing payment service: " + paymentServiceUrl);
    }

    // -----------------------------------------------------------------------
    // Query execution
    // -----------------------------------------------------------------------

    public void executeQuery(String sql) {
        if (dataSource == null) {
            System.err.println("DataSource not initialised. Call connect() first.");
            return;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setQueryTimeout(30);
            System.out.println("Executing query: " + sql);
            stmt.execute();
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Shutdown
    // -----------------------------------------------------------------------

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool closed.");
        }
        if (secretsClient != null) {
            secretsClient.close();
        }
        if (ssmClient != null) {
            ssmClient.close();
        }
    }
}
