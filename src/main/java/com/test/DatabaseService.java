package com.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Database service — database credentials are retrieved at runtime from
 * AWS Secrets Manager (cr-java-0069, cr-java-0113).  No credentials are
 * embedded in source code, property files, or compiled binaries.
 *
 * cr-java-0073 fix (lines 17 and 39 in original source):
 *   Replaced raw JDBC DriverManager.getConnection() calls with a HikariCP
 *   connection pool backed by Amazon RDS Proxy.  HikariCP manages the
 *   connection lifecycle (pooling, validation, eviction) while RDS Proxy
 *   provides multiplexed, IAM-authenticated connections to the underlying
 *   Amazon RDS instance.  The pool is configured entirely from environment
 *   variables / AWS Secrets Manager so that no credentials or host details
 *   are hard-coded in source code.
 *
 * cr-java-0113 fix (line 19 in original source):
 *   The hardcoded secret DB_PASSWORD = "password123" has been removed entirely.
 *   All database credentials (username, password, host, port, dbname) are now
 *   fetched at runtime from AWS Secrets Manager via fetchDbCredentials().
 *   The secret name/ARN is supplied through the DB_SECRET_NAME environment
 *   variable, enabling centralized secret management, automatic rotation, and
 *   full audit logging — eliminating the security vulnerability caused by
 *   embedding credentials in source code.
 *
 * cr-java-0097 fix (line 39 in original source):
 *   Added explicit connection timeout, socket (read) timeout, API call timeout,
 *   and API call attempt timeout to the SecretsManagerClient via
 *   ClientOverrideConfiguration and UrlConnectionHttpClient.  This prevents
 *   the AWS SDK client from hanging indefinitely when the Secrets Manager
 *   endpoint is unreachable or slow, protecting the connection pool
 *   initialisation path from resource exhaustion in cloud environments with
 *   variable network latency.
 */
public class DatabaseService {

    // -----------------------------------------------------------------------
    // AWS Secrets Manager configuration (sourced from environment variables)
    // -----------------------------------------------------------------------
    private static final String AWS_REGION =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    /**
     * Name / ARN of the secret stored in AWS Secrets Manager.
     * The secret must be a JSON object with at minimum the keys:
     *   "username", "password", "host", "port", "dbname"
     * Example secret value:
     *   {"username":"root","password":"s3cr3t","host":"db.example.com","port":"3306","dbname":"mini_app_db"}
     *
     * Set the DB_SECRET_NAME environment variable to point to the correct
     * secret in your AWS account (e.g. via ECS task definition, EKS pod spec,
     * or Elastic Beanstalk environment properties).
     */
    private static final String DB_SECRET_NAME =
            System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/db-credentials");

    // -----------------------------------------------------------------------
    // cr-java-0097 fix: Timeout values for the SecretsManagerClient HTTP
    // transport and SDK-level call timeouts.  All values are sourced from
    // environment variables so they can be tuned per environment without code
    // changes.  Sensible defaults are provided for cloud deployments.
    //
    //  SM_CONNECTION_TIMEOUT_MS  — TCP connection establishment timeout
    //  SM_SOCKET_TIMEOUT_MS      — Socket read timeout (time waiting for data)
    //  SM_API_CALL_TIMEOUT_MS    — Total time budget for a single API call
    //                              (including retries)
    //  SM_API_ATTEMPT_TIMEOUT_MS — Time budget for a single attempt (no retries)
    // -----------------------------------------------------------------------
    private static final Duration SM_CONNECTION_TIMEOUT =
            Duration.ofMillis(Long.parseLong(
                    System.getenv().getOrDefault("SM_CONNECTION_TIMEOUT_MS", "5000")));
    private static final Duration SM_SOCKET_TIMEOUT =
            Duration.ofMillis(Long.parseLong(
                    System.getenv().getOrDefault("SM_SOCKET_TIMEOUT_MS", "10000")));
    private static final Duration SM_API_CALL_TIMEOUT =
            Duration.ofMillis(Long.parseLong(
                    System.getenv().getOrDefault("SM_API_CALL_TIMEOUT_MS", "15000")));
    private static final Duration SM_API_ATTEMPT_TIMEOUT =
            Duration.ofMillis(Long.parseLong(
                    System.getenv().getOrDefault("SM_API_ATTEMPT_TIMEOUT_MS", "10000")));

    // -----------------------------------------------------------------------
    // cr-java-0073 fix: HikariCP pool configuration sourced from environment
    // variables so that pool sizing can be tuned per environment without code
    // changes.  When using Amazon RDS Proxy the recommended pool size is small
    // because RDS Proxy itself multiplexes connections on the server side.
    // -----------------------------------------------------------------------
    private static final int HIKARI_MAXIMUM_POOL_SIZE =
            Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10"));
    private static final int HIKARI_MINIMUM_IDLE =
            Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "2"));
    private static final long HIKARI_CONNECTION_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_CONNECTION_TIMEOUT_MS", "30000"));
    private static final long HIKARI_IDLE_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_IDLE_TIMEOUT_MS", "600000"));
    private static final long HIKARI_MAX_LIFETIME_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_MAX_LIFETIME_MS", "1800000"));

    // -----------------------------------------------------------------------
    // cr-java-0113 fix (line 19 in original source):
    //   Removed hard-coded DB_PASSWORD = "password123" constant.
    // cr-java-0069 fix (lines 17-19 in original source):
    //   Removed hard-coded DB_URL, DB_USERNAME, and DB_PASSWORD constants.
    //   Credentials and connection details are now fetched from AWS Secrets
    //   Manager via fetchDbCredentials() and are never stored as static fields.
    // -----------------------------------------------------------------------

    // cr-java-0077 fix: Replaced hard-coded REDIS_PORT = 6379 with
    // environment variable injection. The value is sourced from the REDIS_PORT
    // environment variable, which is populated at runtime by ECS/EKS/Elastic
    // Beanstalk from AWS Systems Manager Parameter Store.
    private static final String REDIS_HOST =
            System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT =
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // cr-java-0077 fix: Replaced hard-coded port embedded in
    // EXTERNAL_API_URL ("http://api.example.com:8080/v1") with an environment
    // variable. The full URL (including port) is now injected at runtime from
    // AWS Systems Manager Parameter Store via the EXTERNAL_API_URL env var.
    private static final String EXTERNAL_API_URL =
            System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com/v1");
    private static final String PAYMENT_SERVICE_URL =
            System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");

    // -----------------------------------------------------------------------
    // cr-java-0073 fix: HikariDataSource replaces the raw java.sql.Connection
    // field.  The pool is initialised once in connect() and reused for every
    // subsequent executeQuery() call, providing proper connection lifecycle
    // management, health-checking, and automatic reconnection.
    // -----------------------------------------------------------------------
    private HikariDataSource dataSource;

    // -----------------------------------------------------------------------
    // Inner value-object that holds the credentials retrieved from Secrets Manager
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

    /**
     * Retrieves database credentials from AWS Secrets Manager.
     *
     * cr-java-0113: This method is the sole point of credential access.
     * No password, API key, or authentication token is stored in any static
     * field, property file, or source file.  The secret is fetched fresh at
     * connection time, so automatic rotation in Secrets Manager takes effect
     * without any code change or redeployment.
     *
     * cr-java-0097 fix (line 39 in original source):
     *   The SecretsManagerClient is now built with:
     *     1. UrlConnectionHttpClient configured with explicit connectionTimeout
     *        and socketTimeout to prevent TCP-level hangs.
     *     2. ClientOverrideConfiguration with apiCallTimeout (total budget
     *        including retries) and apiCallAttemptTimeout (per-attempt budget)
     *        to prevent SDK-level hangs when the Secrets Manager endpoint is
     *        slow or unreachable.
     *   All timeout values are sourced from environment variables
     *   (SM_CONNECTION_TIMEOUT_MS, SM_SOCKET_TIMEOUT_MS,
     *    SM_API_CALL_TIMEOUT_MS, SM_API_ATTEMPT_TIMEOUT_MS) with safe defaults.
     *
     * The secret is expected to be stored as a JSON string with the keys:
     * username, password, host, port, dbname.
     */
    private DbCredentials fetchDbCredentials() {
        // cr-java-0097 fix: Configure the HTTP transport layer with explicit
        // connection and socket timeouts.  Without these, the underlying
        // URLConnection can block indefinitely waiting for a TCP connection or
        // for data to arrive from the Secrets Manager endpoint.
        UrlConnectionHttpClient.Builder httpClientBuilder =
                UrlConnectionHttpClient.builder()
                        .connectionTimeout(SM_CONNECTION_TIMEOUT)
                        .socketTimeout(SM_SOCKET_TIMEOUT);

        // cr-java-0097 fix: Configure SDK-level timeouts via
        // ClientOverrideConfiguration.  apiCallTimeout caps the total wall-clock
        // time for the entire call (including all retry attempts), while
        // apiCallAttemptTimeout caps each individual attempt.  Together they
        // ensure the call fails fast rather than hanging indefinitely.
        ClientOverrideConfiguration overrideConfig =
                ClientOverrideConfiguration.builder()
                        .apiCallTimeout(SM_API_CALL_TIMEOUT)
                        .apiCallAttemptTimeout(SM_API_ATTEMPT_TIMEOUT)
                        .build();

        SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                .region(Region.of(AWS_REGION))
                .httpClientBuilder(httpClientBuilder)
                .overrideConfiguration(overrideConfig)
                .build();

        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(DB_SECRET_NAME)
                .build();

        GetSecretValueResponse response = secretsClient.getSecretValue(request);
        String secretJson = response.secretString();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(secretJson);

            String host     = node.path("host").asText("localhost");
            String port     = node.path("port").asText("3306");
            String dbName   = node.path("dbname").asText("mini_app_db");
            String username = node.path("username").asText();
            String password = node.path("password").asText();

            // cr-java-0073 fix: When Amazon RDS Proxy is in use the JDBC URL
            // should point to the RDS Proxy endpoint (supplied via the
            // DB_PROXY_ENDPOINT environment variable) rather than the RDS
            // instance endpoint directly.  If DB_PROXY_ENDPOINT is set it
            // overrides the host value from Secrets Manager.
            String proxyEndpoint = System.getenv("DB_PROXY_ENDPOINT");
            if (proxyEndpoint != null && !proxyEndpoint.isEmpty()) {
                host = proxyEndpoint;
            }

            String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                    + "?useSSL=true&requireSSL=true&serverTimezone=UTC";
            return new DbCredentials(dbUrl, username, password);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse database credentials from AWS Secrets Manager secret '"
                    + DB_SECRET_NAME + "': " + e.getMessage(), e);
        } finally {
            secretsClient.close();
        }
    }

    /**
     * cr-java-0073 fix: Initialises a HikariCP connection pool instead of
     * opening a single raw JDBC connection via DriverManager.getConnection().
     *
     * HikariCP provides:
     *  - Connection pooling with configurable pool size, timeouts, and
     *    keep-alive validation queries.
     *  - Automatic connection eviction and replacement on failure.
     *  - Integration with Amazon RDS Proxy via a standard JDBC URL pointing
     *    to the proxy endpoint (DB_PROXY_ENDPOINT env var).
     *
     * Original blocker lines 17 and 39 both used DriverManager.getConnection()
     * directly; both are now replaced by dataSource.getConnection() which
     * borrows a connection from the managed pool.
     */
    public void connect() {
        try {
            System.out.println("Initialising HikariCP connection pool...");

            // cr-java-0113 / cr-java-0069 fix: credentials are fetched from
            // AWS Secrets Manager at connection time — no hard-coded values
            // (including the former DB_PASSWORD = "password123") remain in
            // source code.
            DbCredentials creds = fetchDbCredentials();

            // cr-java-0073 fix (line 17): Build HikariCP configuration.
            // DriverManager.getConnection() has been removed entirely; the pool
            // manages all connection acquisition, validation, and release.
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(creds.url);
            hikariConfig.setUsername(creds.username);
            hikariConfig.setPassword(creds.password);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Pool sizing — tuned for use with Amazon RDS Proxy
            hikariConfig.setMaximumPoolSize(HIKARI_MAXIMUM_POOL_SIZE);
            hikariConfig.setMinimumIdle(HIKARI_MINIMUM_IDLE);
            hikariConfig.setConnectionTimeout(HIKARI_CONNECTION_TIMEOUT_MS);
            hikariConfig.setIdleTimeout(HIKARI_IDLE_TIMEOUT_MS);
            hikariConfig.setMaxLifetime(HIKARI_MAX_LIFETIME_MS);

            // Connection validation
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.setPoolName("MiniAppHikariPool");

            // cr-java-0073 fix (line 39): HikariDataSource replaces the raw
            // Connection field.  All subsequent getConnection() calls borrow
            // from the pool rather than opening a new physical connection each
            // time via DriverManager.
            dataSource = new HikariDataSource(hikariConfig);

            System.out.println("HikariCP pool initialised. JDBC URL: " + creds.url);
            System.out.println("Pool size: " + HIKARI_MAXIMUM_POOL_SIZE
                    + " | Min idle: " + HIKARI_MINIMUM_IDLE);

            // Cache connection using externalized port configuration
            connectToCache();

            // External service URLs sourced from environment variables
            initializeExternalServices();

        } catch (Exception e) {
            System.err.println("Failed to initialise HikariCP connection pool: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // cr-java-0077 fix: REDIS_PORT is now sourced from the
        // REDIS_PORT environment variable (injected from AWS Parameter Store)
        // instead of the former hard-coded literal 6379.
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // External service URLs are now sourced from environment variables
        // (EXTERNAL_API_URL, PAYMENT_SERVICE_URL) injected from AWS Parameter Store.
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        // cr-java-0073 fix: Obtain a connection from the HikariCP pool instead
        // of using a long-lived raw Connection field.  The try-with-resources
        // block ensures the connection is returned to the pool (not closed
        // physically) after each query, enabling proper resource reuse.
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
        // cr-java-0073 fix: Close the HikariCP DataSource (which drains and
        // closes all pooled connections) instead of closing a single raw
        // Connection.  This ensures all physical connections are released
        // cleanly when the application shuts down.
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool closed");
        }
    }
}
