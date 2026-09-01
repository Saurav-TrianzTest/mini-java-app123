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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Database service with credentials sourced from AWS Secrets Manager and
 * connection pooling provided by HikariCP (combined with Amazon RDS Proxy
 * for optimized cloud database connections).
 *
 * REMEDIATION (cr-java-0073 — Direct JDBC Connections):
 * Raw JDBC connection management via {@code DriverManager.getConnection} that
 * previously appeared at lines 17 and 39 has been replaced with a HikariCP
 * {@link HikariDataSource} connection pool.  HikariCP provides:
 *  - Efficient connection reuse and lifecycle management.
 *  - Configurable pool sizing, timeouts, and health checks.
 *  - Seamless integration with Amazon RDS Proxy for cloud-optimized
 *    connection multiplexing and automatic failover.
 *
 * REMEDIATION (cr-java-0113 — Lack of Externalized Secrets):
 * Hard-coded database credentials have been removed entirely.  All sensitive
 * credentials are retrieved at runtime from AWS Secrets Manager via
 * {@link #fetchDbCredentialsFromSecretsManager()}.
 *
 * REMEDIATION (cr-java-0097 — Missing Connection Timeouts):
 * Explicit connection, socket, and API call timeouts have been added to:
 *  - The AWS Secrets Manager SDK client via {@link ClientOverrideConfiguration}
 *    (apiCallTimeout, apiCallAttemptTimeout).
 *  - The JDBC URL via MySQL connectTimeout and socketTimeout parameters.
 *  - The HikariCP pool via connectionTimeout, idleTimeout, keepaliveTime,
 *    and validationTimeout settings.
 * This prevents indefinite hangs and resource exhaustion in cloud environments
 * with variable network latency or transient service failures.
 *
 * Non-sensitive connection parameters (host, port, database name) and
 * previously hard-coded port numbers (REDIS_PORT, port embedded in
 * EXTERNAL_API_URL) are externalised via environment variables so that
 * container orchestration platforms (ECS, EKS, Elastic Beanstalk) can supply
 * the correct values at runtime via AWS Systems Manager Parameter Store or
 * task/pod environment variable overrides.
 */
public class DatabaseService {

    // -----------------------------------------------------------------------
    // AWS Secrets Manager configuration — sourced from environment variables
    // -----------------------------------------------------------------------

    /**
     * Name or ARN of the AWS Secrets Manager secret that stores the database
     * credentials as a JSON object with keys {@code username} and
     * {@code password}.
     *
     * <p>Set the {@code DB_SECRET_NAME} environment variable in the ECS task
     * definition, EKS pod spec, or Elastic Beanstalk environment properties.
     * The default value {@code "mini-app/db-credentials"} is used only when
     * the variable is absent (e.g., local development).
     */
    private static final String DB_SECRET_NAME =
            System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/db-credentials");

    /**
     * AWS region where the Secrets Manager secret is stored.
     *
     * <p>Set the {@code AWS_REGION} environment variable in the ECS task
     * definition, EKS pod spec, or Elastic Beanstalk environment properties.
     */
    private static final String AWS_REGION =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // -----------------------------------------------------------------------
    // Non-sensitive connection parameters — externalised via env vars
    // -----------------------------------------------------------------------

    /** Database host — set DB_HOST in the deployment environment.
     *  When using Amazon RDS Proxy, set this to the RDS Proxy endpoint. */
    private static final String DB_HOST =
            System.getenv().getOrDefault("DB_HOST", "localhost");

    /** Database port — set DB_PORT in the deployment environment. */
    private static final String DB_PORT =
            System.getenv().getOrDefault("DB_PORT", "3306");

    /** Database name — set DB_NAME in the deployment environment. */
    private static final String DB_NAME =
            System.getenv().getOrDefault("DB_NAME", "mini_app_db");

    // -----------------------------------------------------------------------
    // REMEDIATION (cr-java-0097 — Missing Connection Timeouts):
    // JDBC URL includes explicit connectTimeout and socketTimeout parameters
    // to prevent the MySQL driver from hanging indefinitely when the database
    // host is unreachable or a query takes too long.
    //
    //   connectTimeout — milliseconds to wait while establishing the TCP
    //                    connection to the MySQL server (default: 10 000 ms).
    //                    Set DB_CONNECT_TIMEOUT_MS in the deployment environment.
    //
    //   socketTimeout  — milliseconds to wait for data on an established
    //                    socket before raising a SocketTimeoutException
    //                    (default: 30 000 ms).
    //                    Set DB_SOCKET_TIMEOUT_MS in the deployment environment.
    // -----------------------------------------------------------------------

    /** JDBC connect timeout in milliseconds — set DB_CONNECT_TIMEOUT_MS in the deployment environment. */
    private static final int DB_CONNECT_TIMEOUT_MS =
            Integer.parseInt(System.getenv().getOrDefault("DB_CONNECT_TIMEOUT_MS", "10000"));

    /** JDBC socket timeout in milliseconds — set DB_SOCKET_TIMEOUT_MS in the deployment environment. */
    private static final int DB_SOCKET_TIMEOUT_MS =
            Integer.parseInt(System.getenv().getOrDefault("DB_SOCKET_TIMEOUT_MS", "30000"));

    /** Constructed JDBC URL — no credentials embedded.
     *  Includes connectTimeout and socketTimeout to prevent indefinite hangs
     *  (cr-java-0097).  Point DB_HOST to the Amazon RDS Proxy endpoint for
     *  cloud deployments. */
    private static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?useSSL=true&requireSSL=false&serverTimezone=UTC"
            + "&connectTimeout=" + DB_CONNECT_TIMEOUT_MS
            + "&socketTimeout=" + DB_SOCKET_TIMEOUT_MS;

    // -----------------------------------------------------------------------
    // HikariCP pool sizing — externalised via environment variables
    // -----------------------------------------------------------------------

    /** Maximum number of connections in the HikariCP pool.
     *  Set DB_POOL_MAX_SIZE in the deployment environment. */
    private static final int DB_POOL_MAX_SIZE =
            Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10"));

    /** Minimum number of idle connections maintained by HikariCP.
     *  Set DB_POOL_MIN_IDLE in the deployment environment. */
    private static final int DB_POOL_MIN_IDLE =
            Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "2"));

    /** Maximum lifetime (ms) of a connection in the pool.
     *  Set DB_POOL_MAX_LIFETIME_MS in the deployment environment. */
    private static final long DB_POOL_MAX_LIFETIME_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_MAX_LIFETIME_MS", "1800000"));

    /**
     * Connection timeout (ms) — how long to wait for a pool connection.
     * REMEDIATION (cr-java-0097): Explicit pool-level connection timeout
     * prevents callers from blocking indefinitely when all pool connections
     * are in use.
     * Set DB_POOL_CONNECTION_TIMEOUT_MS in the deployment environment.
     */
    private static final long DB_POOL_CONNECTION_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_CONNECTION_TIMEOUT_MS", "30000"));

    /** Idle timeout (ms) — how long a connection may sit idle before removal.
     *  Set DB_POOL_IDLE_TIMEOUT_MS in the deployment environment. */
    private static final long DB_POOL_IDLE_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_IDLE_TIMEOUT_MS", "600000"));

    /**
     * Validation timeout (ms) — maximum time HikariCP will wait for a
     * connection to be validated as alive before returning it to the caller.
     * REMEDIATION (cr-java-0097): Prevents validation from hanging indefinitely
     * on a broken connection.
     * Set DB_POOL_VALIDATION_TIMEOUT_MS in the deployment environment.
     */
    private static final long DB_POOL_VALIDATION_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_VALIDATION_TIMEOUT_MS", "5000"));

    /**
     * Keepalive time (ms) — interval at which HikariCP sends a keepalive
     * ping to idle connections to prevent them from being silently dropped
     * by network infrastructure (e.g., AWS NAT Gateway idle timeout).
     * REMEDIATION (cr-java-0097): Ensures idle connections remain usable in
     * cloud environments where NAT/firewall devices drop long-idle TCP sessions.
     * Set DB_POOL_KEEPALIVE_TIME_MS in the deployment environment.
     */
    private static final long DB_POOL_KEEPALIVE_TIME_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_KEEPALIVE_TIME_MS", "300000"));

    // -----------------------------------------------------------------------
    // AWS SDK client timeouts — externalised via environment variables
    // REMEDIATION (cr-java-0097 — Missing Connection Timeouts):
    // All AWS SDK v2 clients must be configured with apiCallTimeout and
    // apiCallAttemptTimeout to prevent indefinite blocking on slow or
    // unresponsive AWS service endpoints.
    // -----------------------------------------------------------------------

    /**
     * Maximum total time (ms) allowed for a single AWS SDK API call, including
     * all retry attempts.
     * Set AWS_API_CALL_TIMEOUT_MS in the deployment environment.
     */
    private static final long AWS_API_CALL_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("AWS_API_CALL_TIMEOUT_MS", "10000"));

    /**
     * Maximum time (ms) allowed for a single AWS SDK API call attempt
     * (one HTTP request, excluding retries).
     * Set AWS_API_CALL_ATTEMPT_TIMEOUT_MS in the deployment environment.
     */
    private static final long AWS_API_CALL_ATTEMPT_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("AWS_API_CALL_ATTEMPT_TIMEOUT_MS", "5000"));

    // -----------------------------------------------------------------------
    // Cache server configuration — ports externalised via environment variables
    // (previously hard-coded: REDIS_HOST = "127.0.0.1", REDIS_PORT = 6379)
    // Set REDIS_HOST and REDIS_PORT in the ECS task definition / EKS pod spec
    // or inject from AWS Systems Manager Parameter Store at deployment time.
    // -----------------------------------------------------------------------

    /** Redis host — set REDIS_HOST in the deployment environment. */
    private static final String REDIS_HOST =
            System.getenv().getOrDefault("REDIS_HOST", "localhost");

    /** Redis port — set REDIS_PORT in the deployment environment. */
    private static final int REDIS_PORT =
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // -----------------------------------------------------------------------
    // External service URLs — ports externalised via environment variables
    // (previously hard-coded: "http://api.example.com:8080/v1")
    // Set EXTERNAL_API_URL and PAYMENT_SERVICE_URL in the ECS task definition /
    // EKS pod spec or inject from AWS Systems Manager Parameter Store at
    // deployment time so that port numbers can be changed without code changes.
    // -----------------------------------------------------------------------

    /** External API base URL — set EXTERNAL_API_URL in the deployment environment. */
    private static final String EXTERNAL_API_URL =
            System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");

    /** Payment service URL — set PAYMENT_SERVICE_URL in the deployment environment. */
    private static final String PAYMENT_SERVICE_URL =
            System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");

    // -----------------------------------------------------------------------
    // HikariCP DataSource — replaces raw DriverManager.getConnection (cr-java-0073)
    // -----------------------------------------------------------------------

    /**
     * HikariCP connection pool.
     *
     * <p>REMEDIATION (cr-java-0073): This field replaces the raw
     * {@code Connection connection} field that previously relied on
     * {@code DriverManager.getConnection} (original lines 17 and 39).
     * HikariCP manages the full connection lifecycle — creation, validation,
     * eviction, and reuse — eliminating the need for manual connection
     * management.  In cloud deployments, {@code DB_HOST} should be set to the
     * Amazon RDS Proxy endpoint so that HikariCP connections are multiplexed
     * through the proxy for improved scalability and automatic failover.
     */
    private HikariDataSource dataSource;

    // -----------------------------------------------------------------------
    // AWS Secrets Manager helper
    // -----------------------------------------------------------------------

    /**
     * Retrieves the database credentials JSON from AWS Secrets Manager and
     * returns a two-element array: {@code [username, password]}.
     *
     * <p>The secret must be stored as a JSON object with at least the keys
     * {@code username} and {@code password}, for example:
     * <pre>
     * {
     *   "username": "app_user",
     *   "password": "s3cr3t!"
     * }
     * </pre>
     *
     * <p>REMEDIATION (cr-java-0097 — Missing Connection Timeouts):
     * The {@link SecretsManagerClient} is now built with an explicit
     * {@link ClientOverrideConfiguration} that sets:
     * <ul>
     *   <li>{@code apiCallTimeout} — maximum total time for the entire call
     *       (including retries), sourced from {@code AWS_API_CALL_TIMEOUT_MS}.</li>
     *   <li>{@code apiCallAttemptTimeout} — maximum time for a single HTTP
     *       attempt, sourced from {@code AWS_API_CALL_ATTEMPT_TIMEOUT_MS}.</li>
     * </ul>
     * Without these timeouts the SDK would block indefinitely if the Secrets
     * Manager VPC endpoint or the public endpoint is unreachable, exhausting
     * the calling thread and potentially the entire thread pool.
     *
     * <p>This method also replaces the previously hard-coded constants
     * {@code DB_USERNAME = "root"} and {@code DB_PASSWORD = "password123"}
     * (original source lines 18–19) with a runtime lookup, ensuring that
     * credentials are never stored in source code, build artifacts, or
     * container image layers.
     *
     * @return String array where index 0 is the username and index 1 is the
     *         password retrieved from AWS Secrets Manager.
     * @throws RuntimeException if the secret cannot be retrieved or parsed.
     */
    private static String[] fetchDbCredentialsFromSecretsManager() {
        // REMEDIATION (cr-java-0097): Build the SecretsManagerClient with
        // explicit apiCallTimeout and apiCallAttemptTimeout so that the SDK
        // never blocks indefinitely waiting for a response from the Secrets
        // Manager endpoint.
        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMillis(AWS_API_CALL_TIMEOUT_MS))
                .apiCallAttemptTimeout(Duration.ofMillis(AWS_API_CALL_ATTEMPT_TIMEOUT_MS))
                .retryPolicy(RetryPolicy.defaultRetryPolicy())
                .build();

        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(AWS_REGION))
                .overrideConfiguration(overrideConfig)
                .build()) {

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(DB_SECRET_NAME)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretJson = response.secretString();

            // Parse the JSON payload to extract username and password
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(secretJson);

            String username = root.path("username").asText();
            String password = root.path("password").asText();

            return new String[]{username, password};

        } catch (SecretsManagerException e) {
            throw new RuntimeException(
                    "Failed to retrieve database credentials from AWS Secrets Manager "
                    + "(secret: " + DB_SECRET_NAME + "): " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unexpected error while fetching database credentials from AWS Secrets Manager: "
                    + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Initialises the HikariCP connection pool pointed at the database.
     *
     * <p>REMEDIATION (cr-java-0073 — Direct JDBC Connections):
     * The previous implementation called {@code DriverManager.getConnection}
     * directly (original lines 17 and 39), which created a single, unmanaged
     * JDBC connection with no pooling, health-checking, or automatic recovery.
     * This method now builds a {@link HikariDataSource} that:
     * <ul>
     *   <li>Maintains a configurable pool of reusable connections.</li>
     *   <li>Validates connections before handing them to callers.</li>
     *   <li>Evicts stale or broken connections automatically.</li>
     *   <li>Works transparently with Amazon RDS Proxy when {@code DB_HOST}
     *       is set to the RDS Proxy endpoint.</li>
     * </ul>
     *
     * <p>REMEDIATION (cr-java-0097 — Missing Connection Timeouts):
     * The HikariCP pool is configured with:
     * <ul>
     *   <li>{@code connectionTimeout} — max wait for a pool connection
     *       (prevents callers from blocking indefinitely).</li>
     *   <li>{@code validationTimeout} — max time to validate a connection
     *       as alive before returning it to the caller.</li>
     *   <li>{@code keepaliveTime} — periodic ping to idle connections to
     *       prevent silent drops by AWS NAT Gateway or firewall devices.</li>
     *   <li>{@code idleTimeout} and {@code maxLifetime} — evict stale
     *       connections before they are silently closed by the network.</li>
     * </ul>
     * The JDBC URL also includes {@code connectTimeout} and
     * {@code socketTimeout} MySQL driver parameters (see {@link #DB_URL}).
     *
     * <p>Credentials are fetched at runtime from AWS Secrets Manager via
     * {@link #fetchDbCredentialsFromSecretsManager()}.  No sensitive values
     * appear anywhere in this source file.
     */
    public void connect() {
        try {
            System.out.println("Initializing HikariCP connection pool...");

            // Retrieve credentials at runtime from AWS Secrets Manager.
            // Previously hard-coded DB_USERNAME ("root") and DB_PASSWORD
            // ("password123") at original source lines 18–19 are now resolved
            // dynamically so that credential rotation in Secrets Manager takes
            // effect without any code change or redeployment.
            String[] credentials = fetchDbCredentialsFromSecretsManager();
            String dbUsername = credentials[0];
            String dbPassword = credentials[1];

            // ----------------------------------------------------------------
            // REMEDIATION (cr-java-0073): Build HikariCP DataSource
            // Replaces: connection = DriverManager.getConnection(DB_URL, dbUsername, dbPassword)
            // (original lines 17 — import java.sql.DriverManager, and 39 — DriverManager.getConnection)
            //
            // REMEDIATION (cr-java-0097): All timeout values are explicitly
            // configured to prevent indefinite hangs in cloud environments.
            // ----------------------------------------------------------------
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(DB_URL);
            hikariConfig.setUsername(dbUsername);
            hikariConfig.setPassword(dbPassword);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Pool sizing — values sourced from environment variables
            hikariConfig.setMaximumPoolSize(DB_POOL_MAX_SIZE);
            hikariConfig.setMinimumIdle(DB_POOL_MIN_IDLE);
            hikariConfig.setMaxLifetime(DB_POOL_MAX_LIFETIME_MS);

            // REMEDIATION (cr-java-0097 — Missing Connection Timeouts):
            // connectionTimeout: max ms to wait for a connection from the pool
            //   before throwing an exception (prevents indefinite blocking).
            hikariConfig.setConnectionTimeout(DB_POOL_CONNECTION_TIMEOUT_MS);

            // idleTimeout: max ms a connection may remain idle in the pool
            //   before being evicted (prevents stale connections).
            hikariConfig.setIdleTimeout(DB_POOL_IDLE_TIMEOUT_MS);

            // validationTimeout: max ms HikariCP waits to validate a connection
            //   as alive before returning it to the caller.
            hikariConfig.setValidationTimeout(DB_POOL_VALIDATION_TIMEOUT_MS);

            // keepaliveTime: interval (ms) at which HikariCP sends a keepalive
            //   ping to idle connections to prevent silent drops by AWS NAT
            //   Gateway or firewall devices with idle-connection timeouts.
            hikariConfig.setKeepaliveTime(DB_POOL_KEEPALIVE_TIME_MS);

            // Pool name for JMX / logging identification
            hikariConfig.setPoolName("MiniAppHikariPool");

            // Connection validation query
            hikariConfig.setConnectionTestQuery("SELECT 1");

            // Auto-commit behaviour
            hikariConfig.setAutoCommit(true);

            dataSource = new HikariDataSource(hikariConfig);

            System.out.println("HikariCP connection pool initialized. JDBC URL: " + DB_URL);
            System.out.println("Pool size: min-idle=" + DB_POOL_MIN_IDLE + ", max=" + DB_POOL_MAX_SIZE);
            System.out.println("Timeouts: connectionTimeout=" + DB_POOL_CONNECTION_TIMEOUT_MS
                    + "ms, validationTimeout=" + DB_POOL_VALIDATION_TIMEOUT_MS
                    + "ms, keepaliveTime=" + DB_POOL_KEEPALIVE_TIME_MS + "ms");
            System.out.println("Using username: " + dbUsername);

            // Cache connection using externalised host/port
            connectToCache();

            // External service URLs using externalised configuration
            initializeExternalServices();

        } catch (Exception e) {
            System.err.println("Failed to initialize HikariCP connection pool: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // REDIS_HOST and REDIS_PORT are sourced from environment variables
        // (previously hard-coded to "127.0.0.1" and 6379 respectively).
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // EXTERNAL_API_URL and PAYMENT_SERVICE_URL are sourced from environment
        // variables (previously hard-coded with embedded port numbers).
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    /**
     * Executes a SQL statement using a connection borrowed from the HikariCP pool.
     *
     * <p>REMEDIATION (cr-java-0073): Connections are now obtained from the
     * {@link HikariDataSource} pool via {@code dataSource.getConnection()}
     * and returned to the pool automatically via try-with-resources, replacing
     * the previous pattern of holding a single long-lived {@code Connection}
     * instance.
     *
     * <p>REMEDIATION (cr-java-0097): The {@code PreparedStatement} query
     * timeout is explicitly set to prevent long-running queries from holding
     * pool connections indefinitely.
     */
    public void executeQuery(String sql) {
        if (dataSource == null || dataSource.isClosed()) {
            System.err.println("DataSource is not initialized or has been closed.");
            return;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            // REMEDIATION (cr-java-0097): Explicit query timeout prevents
            // long-running statements from holding pool connections indefinitely.
            stmt.setQueryTimeout(30);
            System.out.println("Executing query: " + sql);
            stmt.execute();

        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    /**
     * Shuts down the HikariCP connection pool, releasing all pooled connections.
     *
     * <p>REMEDIATION (cr-java-0073): Replaces the previous single-connection
     * {@code connection.close()} call with a proper pool shutdown via
     * {@link HikariDataSource#close()}.
     */
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool closed.");
        }
    }
}
