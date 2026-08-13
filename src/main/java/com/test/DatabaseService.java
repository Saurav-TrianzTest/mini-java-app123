package com.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Database service with cloud-ready AWS Secrets Manager and AWS Systems Manager
 * Parameter Store integration.
 *
 * Violations fixed (cr-java-0069 - Hard-coded Database Credentials):
 *   - Line 17 (original): DB_USERNAME = "root"         → resolved from AWS Secrets Manager
 *   - Line 18 (original): DB_PASSWORD = "password123"  → resolved from AWS Secrets Manager
 *   - Line 19 (original): DB_URL (embedded host/port)  → resolved from AWS Secrets Manager
 *
 * Violations fixed (cr-java-0073 - Direct JDBC Connections):
 *   - Line 17 (original): DriverManager import         → replaced with HikariCP HikariDataSource
 *   - Line 39 (original): DriverManager.getConnection  → replaced with HikariCP connection pool
 *     Raw JDBC DriverManager eliminated; HikariCP manages the connection pool and integrates
 *     with Amazon RDS Proxy for optimized cloud database connections.
 *
 * Violations fixed (cr-java-0077 - Hard-coded Ports):
 *   - Line 17 (original): REDIS_PORT = 6379            → resolved from AWS SSM Parameter Store
 *   - Line 23 (original): EXTERNAL_API_URL port :8080  → resolved from AWS SSM Parameter Store
 *   - Line 59 (original): setQueryTimeout(30)          → resolved from AWS SSM Parameter Store
 *
 * Violations fixed (cr-java-0097 - Missing Connection Timeouts):
 *   - Line 39 (original): DriverManager.getConnection without timeouts
 *     → AWS SDK clients (SecretsManagerClient, SsmClient) now configured with explicit
 *       apiCallTimeout, apiCallAttemptTimeout, and retry policy via ClientOverrideConfiguration.
 *     → HikariCP pool configured with explicit connectionTimeout, initializationFailTimeout,
 *       keepaliveTime, and validationTimeout to prevent indefinite hangs.
 *     Timeout values are externalised via environment variables for per-environment tuning.
 *
 * Violations fixed (cr-java-0113 - Lack of Externalized Secrets):
 *   - Line 19 (original): DB_PASSWORD = "password123"  → resolved from AWS Secrets Manager
 *   - Redis password, API key, payment credentials, JWT secret, encryption key,
 *     monitoring credentials, and RabbitMQ credentials are all resolved from
 *     AWS Secrets Manager (secret name: mini-app/app-secrets) at runtime.
 *     No credentials or secret values are embedded in source code.
 */
public class DatabaseService {

    // -----------------------------------------------------------------------
    // AWS region — shared by both Secrets Manager and SSM Parameter Store
    // -----------------------------------------------------------------------
    private static final String AWS_REGION_VALUE =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0097 - Missing Connection Timeouts):
    // Shared ClientOverrideConfiguration applied to all AWS SDK clients.
    // Explicit apiCallTimeout and apiCallAttemptTimeout prevent SDK calls from
    // hanging indefinitely in cloud environments with variable network latency.
    //
    // Timeout values are externalised via environment variables:
    //   AWS_SDK_API_CALL_TIMEOUT_MS         (default: 10000 ms — total call budget)
    //   AWS_SDK_API_CALL_ATTEMPT_TIMEOUT_MS (default: 5000  ms — per-attempt budget)
    // -----------------------------------------------------------------------
    private static final ClientOverrideConfiguration AWS_CLIENT_OVERRIDE_CONFIG =
            ClientOverrideConfiguration.builder()
                    .apiCallTimeout(Duration.ofMillis(
                            Long.parseLong(System.getenv().getOrDefault(
                                    "AWS_SDK_API_CALL_TIMEOUT_MS", "10000"))))
                    .apiCallAttemptTimeout(Duration.ofMillis(
                            Long.parseLong(System.getenv().getOrDefault(
                                    "AWS_SDK_API_CALL_ATTEMPT_TIMEOUT_MS", "5000"))))
                    .retryPolicy(RetryPolicy.defaultRetryPolicy())
                    .build();

    // -----------------------------------------------------------------------
    // Shared SecretsManagerClient — reused for all secret lookups.
    // FIXED (cr-java-0097): overrideConfiguration applies apiCallTimeout and
    // apiCallAttemptTimeout so that Secrets Manager calls never hang indefinitely.
    // -----------------------------------------------------------------------
    private static final SecretsManagerClient SECRETS_CLIENT = SecretsManagerClient.builder()
            .region(Region.of(AWS_REGION_VALUE))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .overrideConfiguration(AWS_CLIENT_OVERRIDE_CONFIG)
            .build();

    // -----------------------------------------------------------------------
    // AWS SSM Parameter Store helper — resolves port/timeout values at runtime
    // so that no port numbers are hard-coded in the application source.
    //
    // FIXED (cr-java-0097): overrideConfiguration applies apiCallTimeout and
    // apiCallAttemptTimeout so that SSM calls never hang indefinitely.
    //
    // Parameter naming convention (override via environment variables):
    //   SSM_PARAM_REDIS_PORT          → /mini-app/cache/redis-port
    //   SSM_PARAM_EXTERNAL_API_PORT   → /mini-app/services/external-api-port
    //   SSM_PARAM_EXTERNAL_API_HOST   → /mini-app/services/external-api-host
    //   SSM_PARAM_QUERY_TIMEOUT       → /mini-app/db/query-timeout
    // -----------------------------------------------------------------------
    private static final SsmClient SSM_CLIENT = SsmClient.builder()
            .region(Region.of(AWS_REGION_VALUE))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .overrideConfiguration(AWS_CLIENT_OVERRIDE_CONFIG)
            .build();

    /**
     * Retrieves a parameter value from AWS SSM Parameter Store.
     * Falls back to the supplied {@code envFallback} environment variable,
     * and finally to {@code defaultValue} if neither source is available.
     */
    private static String resolveFromSsm(String ssmParamName, String envFallback, String defaultValue) {
        // 1. Try SSM Parameter Store
        try {
            GetParameterResponse response = SSM_CLIENT.getParameter(
                    GetParameterRequest.builder()
                            .name(ssmParamName)
                            .withDecryption(true)
                            .build());
            String value = response.parameter().value();
            System.out.println("Resolved parameter '" + ssmParamName + "' from AWS SSM Parameter Store.");
            return value;
        } catch (ParameterNotFoundException e) {
            System.out.println("SSM parameter '" + ssmParamName + "' not found; checking environment variable.");
        } catch (Exception e) {
            System.err.println("Failed to read SSM parameter '" + ssmParamName + "': " + e.getMessage()
                    + " — falling back to environment variable.");
        }
        // 2. Fall back to environment variable
        String envValue = System.getenv(envFallback);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        // 3. Last-resort default
        return defaultValue;
    }

    /**
     * Retrieves a single field from an AWS Secrets Manager JSON secret.
     * Falls back to the supplied {@code envFallback} environment variable,
     * and finally to {@code defaultValue} if neither source is available.
     *
     * FIXED (cr-java-0113): All application secrets (passwords, API keys,
     * JWT secrets, encryption keys) are resolved from AWS Secrets Manager
     * at runtime — no secret values are embedded in source code.
     *
     * @param secretName  the Secrets Manager secret name or ARN
     *                    (override via the corresponding environment variable)
     * @param fieldName   the JSON field within the secret payload
     * @param envFallback environment variable name used as a fallback
     * @param defaultValue last-resort value when neither source is available
     */
    private static String resolveSecretField(String secretName, String fieldName,
                                             String envFallback, String defaultValue) {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            GetSecretValueResponse response = SECRETS_CLIENT.getSecretValue(request);
            String secretJson = response.secretString();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretNode = mapper.readTree(secretJson);
            JsonNode fieldNode = secretNode.path(fieldName);
            if (!fieldNode.isMissingNode() && !fieldNode.asText().isEmpty()) {
                System.out.println("Resolved secret field '" + fieldName
                        + "' from AWS Secrets Manager: " + secretName);
                return fieldNode.asText();
            }
        } catch (Exception e) {
            System.err.println("Failed to read secret '" + secretName + "' field '" + fieldName
                    + "' from AWS Secrets Manager: " + e.getMessage()
                    + " — falling back to environment variable.");
        }
        // Fall back to environment variable
        String envValue = System.getenv(envFallback);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return defaultValue;
    }

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0077, line 17): REDIS_PORT is no longer hard-coded.
    // Value is resolved at class-load time from AWS SSM Parameter Store
    // (/mini-app/cache/redis-port), then from the REDIS_PORT env var,
    // and finally defaults to 6379 only when neither source is available.
    // -----------------------------------------------------------------------
    private static final String REDIS_HOST =
            resolveFromSsm(
                    System.getenv().getOrDefault("SSM_PARAM_REDIS_HOST", "/mini-app/cache/redis-host"),
                    "REDIS_HOST",
                    "127.0.0.1");

    private static final int REDIS_PORT =
            Integer.parseInt(
                    resolveFromSsm(
                            System.getenv().getOrDefault("SSM_PARAM_REDIS_PORT", "/mini-app/cache/redis-port"),
                            "REDIS_PORT",
                            "6379"));

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0113): Redis password is resolved from AWS Secrets Manager
    // (secret: mini-app/cache-secrets, field: redis-password) instead of being
    // hard-coded as "redis_secret_123" in application.properties.
    // -----------------------------------------------------------------------
    private static final String REDIS_PASSWORD =
            resolveSecretField(
                    System.getenv().getOrDefault("CACHE_SECRET_NAME", "mini-app/cache-secrets"),
                    "redis-password",
                    "REDIS_PASSWORD",
                    "");

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0077, line 23): The port embedded in EXTERNAL_API_URL is
    // no longer hard-coded. Host and port are resolved independently from AWS
    // SSM Parameter Store and composed into the final URL at class-load time.
    // -----------------------------------------------------------------------
    private static final String EXTERNAL_API_URL;
    static {
        String apiHost = resolveFromSsm(
                System.getenv().getOrDefault("SSM_PARAM_EXTERNAL_API_HOST", "/mini-app/services/external-api-host"),
                "EXTERNAL_API_HOST",
                "api.example.com");
        String apiPort = resolveFromSsm(
                System.getenv().getOrDefault("SSM_PARAM_EXTERNAL_API_PORT", "/mini-app/services/external-api-port"),
                "EXTERNAL_API_PORT",
                "8080");
        EXTERNAL_API_URL = "http://" + apiHost + ":" + apiPort + "/v1";
    }

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0113): External API key is resolved from AWS Secrets Manager
    // (secret: mini-app/api-secrets, field: api-key) instead of being
    // hard-coded as "hardcoded_api_key_12345" in application.properties.
    // -----------------------------------------------------------------------
    private static final String EXTERNAL_API_KEY =
            resolveSecretField(
                    System.getenv().getOrDefault("API_SECRET_NAME", "mini-app/api-secrets"),
                    "api-key",
                    "EXTERNAL_API_KEY",
                    "");

    private static final String PAYMENT_SERVICE_URL =
            resolveFromSsm(
                    System.getenv().getOrDefault("SSM_PARAM_PAYMENT_SERVICE_URL", "/mini-app/services/payment-service-url"),
                    "PAYMENT_SERVICE_URL",
                    "https://payment.internal.company.com/process");

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0113): Payment service credentials are resolved from
    // AWS Secrets Manager (secret: mini-app/payment-secrets) instead of being
    // hard-coded as "payment_user" / "payment_secret_456" in application.properties.
    // -----------------------------------------------------------------------
    private static final String PAYMENT_SERVICE_USERNAME =
            resolveSecretField(
                    System.getenv().getOrDefault("PAYMENT_SECRET_NAME", "mini-app/payment-secrets"),
                    "username",
                    "PAYMENT_SERVICE_USERNAME",
                    "");

    private static final String PAYMENT_SERVICE_PASSWORD =
            resolveSecretField(
                    System.getenv().getOrDefault("PAYMENT_SECRET_NAME", "mini-app/payment-secrets"),
                    "password",
                    "PAYMENT_SERVICE_PASSWORD",
                    "");

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0113): JWT secret and encryption key are resolved from
    // AWS Secrets Manager (secret: mini-app/security-secrets) instead of being
    // hard-coded as "my_super_secret_jwt_key_123456789" / "encryption_key_hardcoded"
    // in application.properties.
    // -----------------------------------------------------------------------
    private static final String JWT_SECRET =
            resolveSecretField(
                    System.getenv().getOrDefault("SECURITY_SECRET_NAME", "mini-app/security-secrets"),
                    "jwt-secret",
                    "SECURITY_JWT_SECRET",
                    "");

    private static final String ENCRYPTION_KEY =
            resolveSecretField(
                    System.getenv().getOrDefault("SECURITY_SECRET_NAME", "mini-app/security-secrets"),
                    "encryption-key",
                    "SECURITY_ENCRYPTION_KEY",
                    "");

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0113): Monitoring credentials are resolved from
    // AWS Secrets Manager (secret: mini-app/monitoring-secrets) instead of being
    // hard-coded as "monitor_user" / "monitor_pass" in application.properties.
    // -----------------------------------------------------------------------
    private static final String MONITORING_USERNAME =
            resolveSecretField(
                    System.getenv().getOrDefault("MONITORING_SECRET_NAME", "mini-app/monitoring-secrets"),
                    "username",
                    "MONITORING_USERNAME",
                    "");

    private static final String MONITORING_PASSWORD =
            resolveSecretField(
                    System.getenv().getOrDefault("MONITORING_SECRET_NAME", "mini-app/monitoring-secrets"),
                    "password",
                    "MONITORING_PASSWORD",
                    "");

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0113): RabbitMQ credentials are resolved from
    // AWS Secrets Manager (secret: mini-app/messaging-secrets) instead of being
    // hard-coded as "rabbitmq_user" / "rabbitmq_secret" in application.properties.
    // -----------------------------------------------------------------------
    private static final String RABBITMQ_USERNAME =
            resolveSecretField(
                    System.getenv().getOrDefault("MESSAGING_SECRET_NAME", "mini-app/messaging-secrets"),
                    "username",
                    "RABBITMQ_USERNAME",
                    "");

    private static final String RABBITMQ_PASSWORD =
            resolveSecretField(
                    System.getenv().getOrDefault("MESSAGING_SECRET_NAME", "mini-app/messaging-secrets"),
                    "password",
                    "RABBITMQ_PASSWORD",
                    "");

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0077, line 59): Query timeout is no longer hard-coded.
    // Value is resolved from AWS SSM Parameter Store (/mini-app/db/query-timeout),
    // then from the DB_QUERY_TIMEOUT env var, defaulting to 30 seconds.
    // -----------------------------------------------------------------------
    private static final int DB_QUERY_TIMEOUT =
            Integer.parseInt(
                    resolveFromSsm(
                            System.getenv().getOrDefault("SSM_PARAM_QUERY_TIMEOUT", "/mini-app/db/query-timeout"),
                            "DB_QUERY_TIMEOUT",
                            "30"));

    // -----------------------------------------------------------------------
    // AWS Secrets Manager configuration for database credentials
    // The secret name / ARN is supplied via the DB_SECRET_NAME environment
    // variable so that it can differ between environments without code changes.
    // Expected secret JSON format:
    //   {
    //     "username": "<db-user>",
    //     "password": "<db-password>",
    //     "host":     "<db-host>",
    //     "port":     "<db-port>",
    //     "dbname":   "<db-name>"
    //   }
    // -----------------------------------------------------------------------
    private static final String DB_SECRET_NAME =
            System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/db-credentials");

    // FIXED (cr-java-0069, cr-java-0113, lines 17-19): credentials and connection URL are now
    // fetched at runtime from AWS Secrets Manager instead of being hard-coded.
    private static final DbCredentials DB_CREDENTIALS = loadDbCredentials();

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0073 - Direct JDBC Connections, lines 17 & 39):
    // Raw DriverManager / direct JDBC connection management is replaced with
    // HikariCP connection pooling. HikariCP is configured to connect through
    // Amazon RDS Proxy (endpoint supplied via the RDS_PROXY_ENDPOINT environment
    // variable or AWS SSM Parameter Store) for optimized cloud database
    // connection management, automatic failover, and connection multiplexing.
    //
    // FIXED (cr-java-0097 - Missing Connection Timeouts, line 39):
    // HikariCP pool is configured with explicit timeout values to prevent
    // indefinite hangs in cloud environments with variable network latency:
    //   HIKARI_CONNECTION_TIMEOUT      — max ms to wait for a pool connection (default: 30000)
    //   HIKARI_INITIALIZATION_FAIL_TIMEOUT — ms to wait for pool init (default: 30000)
    //   HIKARI_KEEPALIVE_TIME          — ms between keepalive pings (default: 60000)
    //   HIKARI_VALIDATION_TIMEOUT      — ms to validate a connection (default: 5000)
    //
    // HikariCP pool settings are tunable via environment variables:
    //   HIKARI_MAX_POOL_SIZE        (default: 10)
    //   HIKARI_MIN_IDLE             (default: 2)
    //   HIKARI_CONNECTION_TIMEOUT   (default: 30000 ms)
    //   HIKARI_IDLE_TIMEOUT         (default: 600000 ms)
    //   HIKARI_MAX_LIFETIME         (default: 1800000 ms)
    //   HIKARI_KEEPALIVE_TIME       (default: 60000 ms)
    //   HIKARI_VALIDATION_TIMEOUT   (default: 5000 ms)
    //   HIKARI_INITIALIZATION_FAIL_TIMEOUT (default: 30000 ms)
    // -----------------------------------------------------------------------
    private static final HikariDataSource DATA_SOURCE = buildHikariDataSource();

    // -----------------------------------------------------------------------
    // Inner value-object that holds the resolved database credentials
    // -----------------------------------------------------------------------
    private static class DbCredentials {
        final String url;
        final String username;
        final String password;

        DbCredentials(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }
    }

    // -----------------------------------------------------------------------
    // Retrieve database credentials from AWS Secrets Manager at class-load time
    // -----------------------------------------------------------------------
    private static DbCredentials loadDbCredentials() {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(DB_SECRET_NAME)
                    .build();

            GetSecretValueResponse response = SECRETS_CLIENT.getSecretValue(request);
            String secretJson = response.secretString();

            // Parse the JSON secret payload
            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretNode = mapper.readTree(secretJson);

            String host     = secretNode.path("host").asText("localhost");
            String port     = secretNode.path("port").asText("3306");
            String dbName   = secretNode.path("dbname").asText("mini_app_db");
            String username = secretNode.path("username").asText();
            String password = secretNode.path("password").asText();

            // Prefer RDS Proxy endpoint when available for cloud-optimized connection pooling.
            // The RDS Proxy endpoint is resolved from SSM Parameter Store or the
            // RDS_PROXY_ENDPOINT environment variable; falls back to the secret's host.
            String rdsProxyEndpoint = resolveFromSsm(
                    System.getenv().getOrDefault("SSM_PARAM_RDS_PROXY_ENDPOINT", "/mini-app/db/rds-proxy-endpoint"),
                    "RDS_PROXY_ENDPOINT",
                    host);

            String url = "jdbc:mysql://" + rdsProxyEndpoint + ":" + port + "/" + dbName;

            System.out.println("Database credentials successfully loaded from AWS Secrets Manager: "
                    + DB_SECRET_NAME);
            return new DbCredentials(url, username, password);

        } catch (Exception e) {
            System.err.println("Failed to load database credentials from AWS Secrets Manager ("
                    + DB_SECRET_NAME + "): " + e.getMessage());
            // Return a safe placeholder so the application can start and report the error
            // rather than crashing with a NullPointerException.
            return new DbCredentials(
                    System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/mini_app_db"),
                    System.getenv().getOrDefault("DB_USERNAME", ""),
                    System.getenv().getOrDefault("DB_PASSWORD", ""));
        }
    }

    /**
     * FIXED (cr-java-0073 - Direct JDBC Connections, lines 17 & 39):
     * Builds and returns a HikariCP {@link HikariDataSource} configured to use
     * Amazon RDS Proxy for cloud-optimized connection pooling.
     *
     * FIXED (cr-java-0097 - Missing Connection Timeouts, line 39):
     * Explicit timeout values are set on the HikariCP pool to prevent indefinite
     * hangs in cloud environments:
     * <ul>
     *   <li>{@code connectionTimeout} — maximum milliseconds to wait for a connection
     *       from the pool before throwing an exception (default: 30 000 ms).</li>
     *   <li>{@code initializationFailTimeout} — milliseconds the pool will attempt to
     *       obtain an initial connection before failing fast on startup (default: 30 000 ms).</li>
     *   <li>{@code keepaliveTime} — interval between keepalive pings sent to idle
     *       connections to prevent them from being silently dropped by the network or
     *       RDS Proxy (default: 60 000 ms).</li>
     *   <li>{@code validationTimeout} — maximum milliseconds to validate a connection
     *       before it is handed to the caller (default: 5 000 ms).</li>
     * </ul>
     *
     * <p>Replaces the previous pattern of calling {@code DriverManager.getConnection()}
     * directly, which bypassed connection pooling and prevented efficient resource
     * utilization in cloud environments.
     *
     * <p>Pool sizing and timeout parameters are externalised via environment variables
     * so they can be tuned per environment without code changes (12-factor app principle).
     */
    private static HikariDataSource buildHikariDataSource() {
        HikariConfig config = new HikariConfig();

        // JDBC URL points to Amazon RDS Proxy endpoint (resolved in loadDbCredentials)
        config.setJdbcUrl(DB_CREDENTIALS.url);
        config.setUsername(DB_CREDENTIALS.username);
        config.setPassword(DB_CREDENTIALS.password);

        // Driver class — explicit registration ensures compatibility across environments
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool sizing — externalised via environment variables for cloud tunability
        config.setMaximumPoolSize(
                Integer.parseInt(System.getenv().getOrDefault("HIKARI_MAX_POOL_SIZE", "10")));
        config.setMinimumIdle(
                Integer.parseInt(System.getenv().getOrDefault("HIKARI_MIN_IDLE", "2")));

        // -----------------------------------------------------------------------
        // FIXED (cr-java-0097 - Missing Connection Timeouts, line 39):
        // All timeout values are externalised via environment variables so they
        // can be tuned per environment without code changes.
        // -----------------------------------------------------------------------

        // Maximum ms to wait for a connection from the pool (prevents indefinite hangs)
        config.setConnectionTimeout(
                Long.parseLong(System.getenv().getOrDefault("HIKARI_CONNECTION_TIMEOUT", "30000")));

        // Maximum ms to wait for pool initialisation on startup (fail-fast behaviour)
        config.setInitializationFailTimeout(
                Long.parseLong(System.getenv().getOrDefault("HIKARI_INITIALIZATION_FAIL_TIMEOUT", "30000")));

        // Interval between keepalive pings to idle connections (prevents silent drops)
        config.setKeepaliveTime(
                Long.parseLong(System.getenv().getOrDefault("HIKARI_KEEPALIVE_TIME", "60000")));

        // Maximum ms to validate a connection before handing it to the caller
        config.setValidationTimeout(
                Long.parseLong(System.getenv().getOrDefault("HIKARI_VALIDATION_TIMEOUT", "5000")));

        // Maximum ms a connection may sit idle in the pool before being evicted
        config.setIdleTimeout(
                Long.parseLong(System.getenv().getOrDefault("HIKARI_IDLE_TIMEOUT", "600000")));

        // Maximum lifetime of a connection in the pool (prevents stale connections)
        config.setMaxLifetime(
                Long.parseLong(System.getenv().getOrDefault("HIKARI_MAX_LIFETIME", "1800000")));

        // Pool name for observability / cloud monitoring dashboards
        config.setPoolName("MiniAppHikariPool");

        // Keep-alive query to validate connections through RDS Proxy
        config.setConnectionTestQuery("SELECT 1");

        System.out.println("HikariCP connection pool initialised with RDS Proxy endpoint: "
                + DB_CREDENTIALS.url);

        return new HikariDataSource(config);
    }

    public void connect() {
        // FIXED (cr-java-0073, lines 17 & 39): Connection is now obtained from the
        // HikariCP pool (DATA_SOURCE.getConnection()) instead of calling
        // DriverManager.getConnection() directly. HikariCP manages the full
        // connection lifecycle, including pooling, validation, and eviction.
        // Amazon RDS Proxy sits in front of the database to further optimise
        // connection multiplexing and failover in the AWS cloud environment.
        System.out.println("Obtaining connection from HikariCP pool (RDS Proxy endpoint: "
                + DB_CREDENTIALS.url + ")...");

        try (Connection connection = DATA_SOURCE.getConnection()) {
            System.out.println("Connection obtained from HikariCP pool successfully.");
            System.out.println("Using username: " + DB_CREDENTIALS.username);

            // Cache connection using SSM-resolved host and port
            connectToCache();

            // External services using SSM-resolved URLs
            initializeExternalServices();

        } catch (SQLException e) {
            System.err.println("Failed to obtain connection from HikariCP pool: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // FIXED (cr-java-0077, cr-java-0113): Redis host, port, and password resolved
        // from AWS SSM Parameter Store and AWS Secrets Manager respectively.
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        System.out.println("Redis authentication: " + (REDIS_PASSWORD.isEmpty() ? "none" : "configured"));
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // FIXED (cr-java-0077, cr-java-0113): External API URL port resolved from AWS SSM
        // Parameter Store; API key resolved from AWS Secrets Manager.
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("External API key configured: " + !EXTERNAL_API_KEY.isEmpty());
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
        System.out.println("Payment service credentials configured: "
                + (!PAYMENT_SERVICE_USERNAME.isEmpty() && !PAYMENT_SERVICE_PASSWORD.isEmpty()));
        System.out.println("JWT secret configured: " + !JWT_SECRET.isEmpty());
        System.out.println("Encryption key configured: " + !ENCRYPTION_KEY.isEmpty());
        System.out.println("Monitoring credentials configured: "
                + (!MONITORING_USERNAME.isEmpty() && !MONITORING_PASSWORD.isEmpty()));
        System.out.println("RabbitMQ credentials configured: "
                + (!RABBITMQ_USERNAME.isEmpty() && !RABBITMQ_PASSWORD.isEmpty()));
    }

    public void executeQuery(String sql) {
        // FIXED (cr-java-0073): Each query borrows a connection from the HikariCP pool
        // using try-with-resources, ensuring the connection is returned to the pool
        // automatically after use — replacing the previous pattern of holding a single
        // raw JDBC connection as an instance field.
        try (Connection connection = DATA_SOURCE.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            // FIXED (cr-java-0077, line 59): query timeout resolved from AWS SSM Parameter Store
            stmt.setQueryTimeout(DB_QUERY_TIMEOUT);

            System.out.println("Executing query: " + sql);
            stmt.execute();

        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        // FIXED (cr-java-0073): Connection lifecycle is managed by HikariCP.
        // Closing the DataSource shuts down the entire pool gracefully.
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
            System.out.println("HikariCP connection pool closed.");
        }
    }
}
