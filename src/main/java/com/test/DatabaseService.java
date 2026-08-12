package com.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Database service — cloud-ready version.
 *
 * FIX (cr-java-0073, lines 17 and 39 of transformed source):
 * Raw JDBC connection management via {@code java.sql.Connection} field and
 * {@code DriverManager.getConnection()} has been replaced with a HikariCP
 * connection pool ({@link HikariDataSource}). The pool is configured to connect
 * through Amazon RDS Proxy, which provides connection multiplexing, IAM
 * authentication support, and automatic failover for cloud database workloads.
 * This eliminates manual connection lifecycle management and enables efficient
 * resource utilisation in cloud environments.
 *
 * FIX (cr-java-0113 / cr-java-0069, lines 18-19 of original source):
 * Hard-coded database credentials (DB_USERNAME = "root", DB_PASSWORD = "password123")
 * have been removed from source code entirely. At runtime the service retrieves a
 * JSON secret from AWS Secrets Manager whose keys are "username" and "password"
 * (and optionally "host", "port", "dbname"). The secret is identified by the
 * environment variable DB_SECRET_NAME (default: "mini-app/db-credentials").
 * This eliminates the security vulnerability of embedding credentials in source code
 * and enables centralized secret management, automatic rotation, and audit logging.
 *
 * FIX (cr-java-0077): Hard-coded port numbers for Redis cache (6379) and
 * external API URL (port 8080) have been replaced with AWS Systems Manager
 * Parameter Store lookups with environment variable fallback injection.
 *
 * FIX (cr-java-0097, line 39 of original source):
 * AWS SDK clients (SecretsManagerClient, SsmClient) are now constructed with
 * explicit {@link ClientOverrideConfiguration} specifying apiCallTimeout and
 * apiCallAttemptTimeout. HikariCP connection pool is also configured with
 * explicit connectionTimeout, idleTimeout, and maxLifetime to prevent indefinite
 * hangs and resource exhaustion in cloud environments with variable network latency.
 *
 * Required environment variables:
 *   DB_SECRET_NAME              – name/ARN of the Secrets Manager secret
 *                                 (default: "mini-app/db-credentials")
 *   DB_HOST                     – RDS Proxy or database hostname (default: "localhost")
 *   DB_PORT                     – database port (default: "3306")
 *   DB_NAME                     – database schema name (default: "mini_app_db")
 *   AWS_REGION                  – AWS region for Secrets Manager / SSM client
 *                                 (default: "us-east-1")
 *   HIKARI_MAX_POOL_SIZE        – HikariCP maximum pool size (default: "10")
 *   HIKARI_MIN_IDLE             – HikariCP minimum idle connections (default: "2")
 *   HIKARI_CONN_TIMEOUT_MS      – HikariCP connection timeout in ms (default: "30000")
 *   HIKARI_IDLE_TIMEOUT_MS      – HikariCP idle timeout in ms (default: "600000")
 *   HIKARI_MAX_LIFETIME_MS      – HikariCP max connection lifetime in ms (default: "1800000")
 *   REDIS_HOST                  – Redis cache hostname (default: "127.0.0.1")
 *   REDIS_PORT                  – Redis cache port fallback (default: "6379")
 *   REDIS_PORT_PARAM            – SSM parameter name for Redis port
 *                                 (default: "/mini-app/cache/redis-port")
 *   EXTERNAL_API_HOST           – External API hostname (default: "api.example.com")
 *   EXTERNAL_API_PORT           – External API port fallback (default: "8080")
 *   EXTERNAL_API_PORT_PARAM     – SSM parameter name for external API port
 *                                 (default: "/mini-app/external-api/port")
 *   EXTERNAL_API_BASE_PATH      – External API base path (default: "/v1")
 *   PAYMENT_SERVICE_URL         – Payment service URL
 *                                 (default: "https://payment.internal.company.com/process")
 *   AWS_API_CALL_TIMEOUT_MS     – Total AWS SDK API call timeout in ms (default: "10000")
 *   AWS_API_ATTEMPT_TIMEOUT_MS  – Per-attempt AWS SDK API call timeout in ms (default: "5000")
 */
public class DatabaseService {

    // Non-sensitive connection defaults resolved from environment variables.
    // Actual credentials (username / password) are NEVER stored here —
    // they are fetched at pool-initialisation time from AWS Secrets Manager.
    private static final String DB_HOST =
            System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT =
            System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME =
            System.getenv().getOrDefault("DB_NAME", "mini_app_db");

    // JDBC URL points to Amazon RDS Proxy endpoint (supplied via DB_HOST env var)
    // to leverage connection multiplexing and IAM authentication in AWS.
    private static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    // Secret name / ARN externalised so it can differ per environment.
    private static final String DB_SECRET_NAME =
            System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/db-credentials");
    private static final Region AWS_REGION =
            Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

    // HikariCP pool sizing / timeout parameters resolved from environment variables.
    private static final int HIKARI_MAX_POOL_SIZE =
            Integer.parseInt(System.getenv().getOrDefault("HIKARI_MAX_POOL_SIZE", "10"));
    private static final int HIKARI_MIN_IDLE =
            Integer.parseInt(System.getenv().getOrDefault("HIKARI_MIN_IDLE", "2"));
    private static final long HIKARI_CONN_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("HIKARI_CONN_TIMEOUT_MS", "30000"));
    private static final long HIKARI_IDLE_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("HIKARI_IDLE_TIMEOUT_MS", "600000"));
    private static final long HIKARI_MAX_LIFETIME_MS =
            Long.parseLong(System.getenv().getOrDefault("HIKARI_MAX_LIFETIME_MS", "1800000"));

    // FIX cr-java-0097: AWS SDK client timeout parameters resolved from environment variables.
    // These prevent AWS SDK calls from hanging indefinitely in cloud environments
    // with variable network latency or transient service failures.
    private static final long AWS_API_CALL_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("AWS_API_CALL_TIMEOUT_MS", "10000"));
    private static final long AWS_API_ATTEMPT_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("AWS_API_ATTEMPT_TIMEOUT_MS", "5000"));

    // Redis host resolved from environment variable — no hard-coded IP address.
    private static final String REDIS_HOST =
            System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");

    // FIX cr-java-0077: Hard-coded Redis port 6379 replaced.
    private static final String REDIS_PORT_PARAM =
            System.getenv().getOrDefault("REDIS_PORT_PARAM", "/mini-app/cache/redis-port");
    private static final int REDIS_PORT = resolvePortFromSsm(
            REDIS_PORT_PARAM,
            System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // FIX cr-java-0077: Hard-coded port 8080 embedded in EXTERNAL_API_URL replaced.
    private static final String EXTERNAL_API_HOST =
            System.getenv().getOrDefault("EXTERNAL_API_HOST", "api.example.com");
    private static final String EXTERNAL_API_PORT_PARAM =
            System.getenv().getOrDefault("EXTERNAL_API_PORT_PARAM", "/mini-app/external-api/port");
    private static final int EXTERNAL_API_PORT = resolvePortFromSsm(
            EXTERNAL_API_PORT_PARAM,
            System.getenv().getOrDefault("EXTERNAL_API_PORT", "8080"));
    private static final String EXTERNAL_API_BASE_PATH =
            System.getenv().getOrDefault("EXTERNAL_API_BASE_PATH", "/v1");
    private static final String EXTERNAL_API_URL =
            "http://" + EXTERNAL_API_HOST + ":" + EXTERNAL_API_PORT + EXTERNAL_API_BASE_PATH;

    // Payment service URL externalised to environment variable.
    private static final String PAYMENT_SERVICE_URL =
            System.getenv().getOrDefault("PAYMENT_SERVICE_URL",
                    "https://payment.internal.company.com/process");

    // FIX cr-java-0073 (line 17 of transformed source):
    // Raw java.sql.Connection field replaced with HikariCP HikariDataSource.
    // HikariDataSource manages a pool of connections to Amazon RDS Proxy,
    // providing efficient connection reuse, health checking, and automatic
    // reconnection — eliminating manual connection lifecycle management.
    private HikariDataSource dataSource;

    /**
     * Builds a shared {@link ClientOverrideConfiguration} for all AWS SDK clients.
     *
     * FIX cr-java-0097 (line 39 of original source):
     * Configures explicit apiCallTimeout and apiCallAttemptTimeout on every AWS SDK
     * client to prevent indefinite hangs when Secrets Manager or SSM is slow or
     * unreachable. Values are externalised to environment variables
     * (AWS_API_CALL_TIMEOUT_MS, AWS_API_ATTEMPT_TIMEOUT_MS) so they can be tuned
     * per environment without code changes.
     *
     * @return a {@link ClientOverrideConfiguration} with timeout settings applied
     */
    private static ClientOverrideConfiguration buildAwsClientOverrideConfig() {
        return ClientOverrideConfiguration.builder()
                // Total time budget for the entire API call (including retries).
                .apiCallTimeout(Duration.ofMillis(AWS_API_CALL_TIMEOUT_MS))
                // Time budget for a single attempt (before retry kicks in).
                .apiCallAttemptTimeout(Duration.ofMillis(AWS_API_ATTEMPT_TIMEOUT_MS))
                // Use default retry policy to handle transient AWS service errors.
                .retryPolicy(RetryPolicy.defaultRetryPolicy())
                .build();
    }

    /**
     * Resolves a TCP port number from AWS Systems Manager Parameter Store.
     *
     * FIX cr-java-0097: The SsmClient is now built with an explicit
     * {@link ClientOverrideConfiguration} containing apiCallTimeout and
     * apiCallAttemptTimeout to prevent indefinite blocking when SSM is
     * unavailable or slow.
     */
    private static int resolvePortFromSsm(String paramName, String envFallback) {
        // FIX cr-java-0097: SsmClient constructed with explicit connection timeouts
        // via ClientOverrideConfiguration to prevent indefinite hangs.
        try (SsmClient ssmClient = SsmClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .overrideConfiguration(buildAwsClientOverrideConfig())
                .build()) {

            GetParameterRequest request = GetParameterRequest.builder()
                    .name(paramName)
                    .withDecryption(false)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            String portValue = response.parameter().value();
            System.out.println("Resolved port from SSM Parameter Store ["
                    + paramName + "]: " + portValue);
            return Integer.parseInt(portValue.trim());

        } catch (ParameterNotFoundException e) {
            System.out.println("SSM parameter not found [" + paramName
                    + "], using env fallback: " + envFallback);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port value in SSM parameter [" + paramName
                    + "], using env fallback: " + envFallback);
        } catch (Exception e) {
            System.out.println("SSM unavailable for parameter [" + paramName
                    + "], using env fallback: " + envFallback
                    + " (" + e.getMessage() + ")");
        }
        return Integer.parseInt(envFallback);
    }

    /**
     * Retrieves database credentials from AWS Secrets Manager.
     *
     * FIX cr-java-0113 (line 19 of original source — DB_PASSWORD = "password123"):
     * The password (and username) are no longer stored as source-code literals.
     * This method fetches them at pool-initialisation time from AWS Secrets Manager.
     *
     * FIX cr-java-0097: SecretsManagerClient is now built with an explicit
     * {@link ClientOverrideConfiguration} containing apiCallTimeout and
     * apiCallAttemptTimeout to prevent indefinite blocking when Secrets Manager
     * is unavailable or slow.
     *
     * @return a two-element array: [username, password]
     * @throws RuntimeException if the secret cannot be retrieved or parsed
     */
    private String[] loadDbCredentials() {
        // FIX cr-java-0097: SecretsManagerClient constructed with explicit connection
        // timeouts via ClientOverrideConfiguration to prevent indefinite hangs.
        try (SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                .region(AWS_REGION)
                .overrideConfiguration(buildAwsClientOverrideConfig())
                .build()) {

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(DB_SECRET_NAME)
                    .build();

            GetSecretValueResponse response = secretsClient.getSecretValue(request);
            String secretJson = response.secretString();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretNode = mapper.readTree(secretJson);

            String username = secretNode.has("username")
                    ? secretNode.get("username").asText()
                    : "";
            String password = secretNode.has("password")
                    ? secretNode.get("password").asText()
                    : "";

            return new String[]{username, password};

        } catch (SecretsManagerException e) {
            throw new RuntimeException(
                    "Failed to retrieve database credentials from AWS Secrets Manager: "
                            + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unexpected error while loading database credentials: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Initialises the HikariCP connection pool targeting Amazon RDS Proxy.
     *
     * FIX cr-java-0073 (lines 17 and 39 of transformed source):
     * Replaces the raw {@code DriverManager.getConnection()} call with a
     * HikariCP {@link HikariDataSource}. HikariCP is configured with:
     * <ul>
     *   <li>JDBC URL pointing to the Amazon RDS Proxy endpoint (DB_HOST env var)</li>
     *   <li>Credentials fetched from AWS Secrets Manager (no hard-coded values)</li>
     *   <li>Pool sizing and timeout parameters from environment variables</li>
     *   <li>Connection validation query to detect stale connections early</li>
     * </ul>
     *
     * FIX cr-java-0097 (line 39 of original source):
     * HikariCP is explicitly configured with connectionTimeout, idleTimeout, and
     * maxLifetime to prevent connections from hanging indefinitely. AWS SDK clients
     * used during initialisation (SecretsManagerClient, SsmClient) are also
     * configured with apiCallTimeout and apiCallAttemptTimeout via
     * {@link ClientOverrideConfiguration}.
     */
    public void connect() {
        try {
            System.out.println("Initialising HikariCP connection pool targeting RDS Proxy...");

            // Fetch credentials from AWS Secrets Manager — no hard-coded values.
            String[] credentials = loadDbCredentials();
            String dbUsername = credentials[0];
            String dbPassword = credentials[1];

            // FIX cr-java-0073 (line 39 of transformed source):
            // DriverManager.getConnection() replaced with HikariCP HikariDataSource.
            // HikariConfig encapsulates all pool settings; HikariDataSource manages
            // the pool lifecycle and provides thread-safe connection borrowing.
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(DB_URL);
            hikariConfig.setUsername(dbUsername);
            hikariConfig.setPassword(dbPassword);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Pool sizing — tuned for cloud workloads behind RDS Proxy.
            hikariConfig.setMaximumPoolSize(HIKARI_MAX_POOL_SIZE);
            hikariConfig.setMinimumIdle(HIKARI_MIN_IDLE);

            // FIX cr-java-0097: Explicit timeout settings on HikariCP prevent
            // connection starvation and indefinite hangs under load or when the
            // database is temporarily unreachable.
            hikariConfig.setConnectionTimeout(HIKARI_CONN_TIMEOUT_MS);
            hikariConfig.setIdleTimeout(HIKARI_IDLE_TIMEOUT_MS);
            hikariConfig.setMaxLifetime(HIKARI_MAX_LIFETIME_MS);

            // Validate connections before borrowing to detect stale pool entries.
            hikariConfig.setConnectionTestQuery("SELECT 1");

            // Pool name aids identification in monitoring dashboards.
            hikariConfig.setPoolName("MiniAppHikariPool");

            dataSource = new HikariDataSource(hikariConfig);

            System.out.println("HikariCP pool initialised. JDBC URL: " + DB_URL);
            System.out.println("Pool size: max=" + HIKARI_MAX_POOL_SIZE
                    + ", minIdle=" + HIKARI_MIN_IDLE);
            System.out.println("Connection timeout: " + HIKARI_CONN_TIMEOUT_MS + "ms"
                    + ", Idle timeout: " + HIKARI_IDLE_TIMEOUT_MS + "ms"
                    + ", Max lifetime: " + HIKARI_MAX_LIFETIME_MS + "ms");
            System.out.println("AWS SDK API call timeout: " + AWS_API_CALL_TIMEOUT_MS + "ms"
                    + ", attempt timeout: " + AWS_API_ATTEMPT_TIMEOUT_MS + "ms");
            System.out.println("Using username: " + dbUsername);

            // Connect to cache using SSM-resolved port
            connectToCache();

            // Connect to external services using SSM-resolved ports
            initializeExternalServices();

        } catch (Exception e) {
            System.err.println("Failed to initialise HikariCP connection pool: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // FIX cr-java-0077: REDIS_PORT is resolved from AWS SSM Parameter Store.
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // FIX cr-java-0077: EXTERNAL_API_URL port is resolved from AWS SSM Parameter Store.
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        // FIX cr-java-0073: Connections are now borrowed from the HikariCP pool
        // using try-with-resources, ensuring they are returned to the pool
        // automatically after use — no manual connection lifecycle management.
        if (dataSource == null || dataSource.isClosed()) {
            System.err.println("DataSource is not initialised or has been closed.");
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

    public void disconnect() {
        // FIX cr-java-0073: Closing the HikariDataSource shuts down the entire
        // connection pool gracefully, releasing all pooled connections to the
        // database — replacing the single manual connection.close() call.
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool closed");
        }
    }
}
