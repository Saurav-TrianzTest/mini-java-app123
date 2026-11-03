package com.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Database service — cloud-ready version.
 *
 * cr-java-0069 fix: Hard-coded database credentials (DB_USERNAME, DB_PASSWORD, and the
 * credential-bearing DB_URL) have been removed and replaced with a call to
 * AWS Secrets Manager.  The secret is expected to be a JSON object with the keys
 * "username", "password", "host", "port", and "dbname".  The secret ARN / name is
 * resolved from the environment variable DB_SECRET_NAME at runtime, so no credentials
 * are ever stored in source code or compiled binaries.
 *
 * cr-java-0073 fix: Raw JDBC DriverManager connection management has been replaced with
 * HikariCP connection pooling combined with Amazon RDS Proxy for optimized database
 * connections in cloud environments.  HikariCP provides efficient connection lifecycle
 * management, automatic connection validation, and pool-level health monitoring.
 * Amazon RDS Proxy sits between the application and the RDS instance to multiplex
 * connections, improve scalability, and enable seamless failover.
 *
 * cr-java-0077 fix: All hard-coded port numbers have been replaced with environment
 * variable lookups injected at runtime via ECS/EKS task definitions or Elastic Beanstalk
 * environment configuration, backed by AWS Systems Manager Parameter Store.
 *
 * cr-java-0097 fix: Explicit connection, socket, and API call timeouts have been added
 * to all network clients to prevent indefinite hangs and resource exhaustion in cloud
 * environments with variable network latency:
 *   - JDBC / HikariCP: connectTimeout and socketTimeout added as datasource properties
 *     so that TCP-level connection establishment and read operations are bounded.
 *   - AWS Secrets Manager SDK client: ClientOverrideConfiguration with apiCallTimeout
 *     and apiCallAttemptTimeout added so that secret retrieval calls cannot hang
 *     indefinitely when the Secrets Manager endpoint is unreachable or slow.
 *
 * cr-java-0113 fix: All embedded secrets (API keys, authentication tokens, service
 * credentials, and encryption keys) have been removed from source code and replaced
 * with AWS Secrets Manager integration.  Specifically:
 *   - Line 19 (src): private static final String DB_PASSWORD = "password123"
 *     → Removed entirely; password is now retrieved at runtime from AWS Secrets Manager
 *       using the secret name defined by the DB_SECRET_NAME environment variable
 *       (default: "mini-app/db-credentials").  The secret is a JSON object containing
 *       "username" and "password" fields, enabling centralized secret management,
 *       automatic rotation, and full audit logging without any credentials in source code.
 *
 * Original violations replaced:
 *   Line 17 (src): private static final String DB_PORT  = "3306"  → System.getenv("DB_PORT")
 *   Line 17 (src): DriverManager (raw JDBC)                       → HikariCP DataSource + RDS Proxy
 *   Line 19 (src): private static final String DB_PASSWORD = "password123" → AWS Secrets Manager
 *   Line 23 (src): private static final int REDIS_PORT  = 6379    → System.getenv("REDIS_PORT")
 *   Line 39 (src): DriverManager.getConnection(...)               → HikariCP DataSource.getConnection()
 *   Line 59 (src): EXTERNAL_API_URL containing port 8080          → System.getenv("EXTERNAL_API_URL")
 */
public class DatabaseService {

    // -----------------------------------------------------------------------
    // cr-java-0077: Non-sensitive configuration — resolved from environment
    // variables so that port numbers are never hard-coded in source code.
    // Values should be stored in AWS Systems Manager Parameter Store and
    // injected at runtime through ECS task definitions, EKS pod specs, or
    // Elastic Beanstalk environment properties.
    // -----------------------------------------------------------------------
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");

    // cr-java-0077 (Line 17): DB_PORT was hard-coded as "3306".
    // Now resolved from the DB_PORT environment variable (SSM Parameter Store backed).
    // cr-java-0073: When using Amazon RDS Proxy, set DB_HOST to the RDS Proxy endpoint
    // and DB_PORT to the proxy listener port (typically 3306 for MySQL).
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");

    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "mini_app_db");

    // -----------------------------------------------------------------------
    // cr-java-0069 / cr-java-0113: Secret name / ARN resolved from the environment.
    //
    // The DB_PASSWORD field that was previously hard-coded as "password123" at
    // line 19 of the original source has been completely removed.  Database
    // credentials are now retrieved exclusively from AWS Secrets Manager at
    // runtime, providing:
    //   • Centralized secret management across all environments
    //   • Automatic credential rotation without code changes or redeployment
    //   • Full audit logging of every secret access via AWS CloudTrail
    //   • Elimination of security vulnerabilities from source code exposure
    //
    // The secret stored in AWS Secrets Manager must be a JSON object with the
    // following keys: username, password, host (optional), port (optional),
    // dbname (optional).  Example secret value:
    //   { "username": "appuser", "password": "s3cr3t!", "host": "db.example.com",
    //     "port": "3306", "dbname": "mini_app_db" }
    // -----------------------------------------------------------------------
    private static final String DB_SECRET_NAME =
            System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/db-credentials");
    private static final String AWS_REGION =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // cr-java-0077 (Line 23): REDIS_PORT was hard-coded as 6379.
    // Now resolved from the REDIS_PORT environment variable (SSM Parameter Store backed).
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT =
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // cr-java-0077 (Line 59): EXTERNAL_API_URL contained a hard-coded port (8080).
    // Now resolved from the EXTERNAL_API_URL environment variable so the port is
    // configurable at runtime without code changes.
    private static final String EXTERNAL_API_URL =
            System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL =
            System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.internal.company.com/process");

    // -----------------------------------------------------------------------
    // cr-java-0073: HikariCP connection pool configuration constants.
    //
    // HikariCP pool sizing and timeout values are resolved from environment
    // variables so they can be tuned per environment without code changes.
    // When using Amazon RDS Proxy, set DB_HOST to the RDS Proxy endpoint —
    // the proxy handles connection multiplexing and failover transparently.
    //
    // Recommended RDS Proxy settings:
    //   DB_HOST          → <rds-proxy-endpoint>.proxy-<id>.<region>.rds.amazonaws.com
    //   DB_POOL_MAX_SIZE → 10–20 (RDS Proxy multiplexes these to the DB)
    //   DB_POOL_MIN_IDLE → 2–5
    // -----------------------------------------------------------------------
    private static final int DB_POOL_MAX_SIZE =
            Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10"));
    private static final int DB_POOL_MIN_IDLE =
            Integer.parseInt(System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "2"));
    private static final long DB_POOL_CONNECTION_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_CONNECTION_TIMEOUT_MS", "30000"));
    private static final long DB_POOL_IDLE_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_IDLE_TIMEOUT_MS", "600000"));
    private static final long DB_POOL_MAX_LIFETIME_MS =
            Long.parseLong(System.getenv().getOrDefault("DB_POOL_MAX_LIFETIME_MS", "1800000"));

    // -----------------------------------------------------------------------
    // cr-java-0097: Network-level timeout constants for JDBC connections.
    //
    // These values bound the TCP-level connection establishment (connectTimeout)
    // and individual socket read operations (socketTimeout) so that a slow or
    // unresponsive database host cannot cause threads to hang indefinitely.
    // Both values are resolved from environment variables so they can be tuned
    // per environment without code changes.
    //
    //   DB_CONNECT_TIMEOUT_MS  — maximum time (ms) to wait for the TCP connection
    //                            to the database host to be established.
    //                            Default: 10 000 ms (10 seconds)
    //   DB_SOCKET_TIMEOUT_MS   — maximum time (ms) to wait for data to be received
    //                            on an established socket (read timeout).
    //                            Default: 30 000 ms (30 seconds)
    // -----------------------------------------------------------------------
    private static final int DB_CONNECT_TIMEOUT_MS =
            Integer.parseInt(System.getenv().getOrDefault("DB_CONNECT_TIMEOUT_MS", "10000"));
    private static final int DB_SOCKET_TIMEOUT_MS =
            Integer.parseInt(System.getenv().getOrDefault("DB_SOCKET_TIMEOUT_MS", "30000"));

    // -----------------------------------------------------------------------
    // cr-java-0097: AWS SDK API call timeout constants for Secrets Manager.
    //
    // These values bound the total time allowed for a Secrets Manager API call
    // (apiCallTimeout) and each individual attempt within that call
    // (apiCallAttemptTimeout), preventing the application from hanging
    // indefinitely when the Secrets Manager endpoint is unreachable or slow.
    //
    //   SM_API_CALL_TIMEOUT_MS         — total time budget for the entire API call
    //                                    including all retry attempts.
    //                                    Default: 10 000 ms (10 seconds)
    //   SM_API_CALL_ATTEMPT_TIMEOUT_MS — time budget for each individual attempt.
    //                                    Default: 5 000 ms (5 seconds)
    // -----------------------------------------------------------------------
    private static final long SM_API_CALL_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("SM_API_CALL_TIMEOUT_MS", "10000"));
    private static final long SM_API_CALL_ATTEMPT_TIMEOUT_MS =
            Long.parseLong(System.getenv().getOrDefault("SM_API_CALL_ATTEMPT_TIMEOUT_MS", "5000"));

    // -----------------------------------------------------------------------
    // cr-java-0073: HikariCP DataSource replaces the raw java.sql.Connection field.
    //
    // Line 17 (original): raw JDBC DriverManager usage
    // Line 39 (original): DriverManager.getConnection(...) call
    //
    // Both occurrences are eliminated by introducing a HikariDataSource that
    // manages the connection pool lifecycle.  Individual connections are obtained
    // from the pool via dataSource.getConnection() and returned automatically
    // when the try-with-resources block closes them.
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

    // -----------------------------------------------------------------------
    // cr-java-0069 / cr-java-0113: Retrieve database credentials from AWS Secrets Manager.
    //
    // This method replaces the original hard-coded DB_PASSWORD = "password123"
    // (source line 19) and DB_USERNAME = "root" (source line 18).  Credentials
    // are fetched at connection time from AWS Secrets Manager, ensuring they are
    // never embedded in source code, configuration files, or compiled binaries.
    // The secret value is a JSON string; Jackson is used to parse it.
    //
    // cr-java-0097: The SecretsManagerClient is built with a ClientOverrideConfiguration
    // that sets explicit apiCallTimeout and apiCallAttemptTimeout values.  This ensures
    // that secret retrieval calls cannot hang indefinitely in cloud environments with
    // variable network latency or transient Secrets Manager endpoint unavailability.
    // -----------------------------------------------------------------------
    private DbCredentials fetchCredentialsFromSecretsManager() {
        // cr-java-0097: Build ClientOverrideConfiguration with explicit API call timeouts.
        // apiCallTimeout     — total time budget for the entire call (all retry attempts).
        // apiCallAttemptTimeout — time budget for each individual attempt.
        // Both values are resolved from environment variables (SM_API_CALL_TIMEOUT_MS and
        // SM_API_CALL_ATTEMPT_TIMEOUT_MS) so they can be tuned per environment.
        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMillis(SM_API_CALL_TIMEOUT_MS))
                .apiCallAttemptTimeout(Duration.ofMillis(SM_API_CALL_ATTEMPT_TIMEOUT_MS))
                .retryPolicy(RetryPolicy.defaultRetryPolicy())
                .build();

        SecretsManagerClient smClient = SecretsManagerClient.builder()
                .region(Region.of(AWS_REGION))
                .overrideConfiguration(overrideConfig)
                .build();

        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(DB_SECRET_NAME)
                    .build();

            GetSecretValueResponse response = smClient.getSecretValue(request);
            String secretJson = response.secretString();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode secret = mapper.readTree(secretJson);

            // Allow the secret to override host/port/dbname as well, falling back
            // to the environment-variable values set above.
            String host   = secret.has("host")   ? secret.get("host").asText()   : DB_HOST;
            String port   = secret.has("port")   ? secret.get("port").asText()   : DB_PORT;
            String dbname = secret.has("dbname") ? secret.get("dbname").asText() : DB_NAME;

            String resolvedUrl      = "jdbc:mysql://" + host + ":" + port + "/" + dbname;
            String resolvedUsername = secret.get("username").asText();
            String resolvedPassword = secret.get("password").asText();

            return new DbCredentials(resolvedUrl, resolvedUsername, resolvedPassword);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to retrieve database credentials from AWS Secrets Manager (secret: "
                    + DB_SECRET_NAME + "): " + e.getMessage(), e);
        } finally {
            smClient.close();
        }
    }

    // -----------------------------------------------------------------------
    // cr-java-0073: Build and initialise the HikariCP connection pool.
    //
    // This method replaces the raw DriverManager.getConnection() call that
    // previously appeared at line 17 and line 39 of the original source file.
    // HikariCP is configured with:
    //   • jdbcUrl pointing to the Amazon RDS Proxy endpoint (DB_HOST env var)
    //   • Credentials fetched from AWS Secrets Manager (no hard-coded values)
    //   • Pool sizing and timeout parameters from environment variables
    //   • Connection test query to validate pool health
    //
    // cr-java-0097: Network-level JDBC timeouts (connectTimeout, socketTimeout)
    // are added as datasource properties so that TCP-level connection establishment
    // and socket read operations are bounded.  This prevents threads from hanging
    // indefinitely when the database host is slow or unreachable.
    //
    // Amazon RDS Proxy benefits when DB_HOST is set to the proxy endpoint:
    //   • Connection multiplexing reduces load on the RDS instance
    //   • Automatic failover with minimal connection disruption
    //   • IAM authentication support for enhanced security
    //   • Seamless integration with AWS Secrets Manager for credential rotation
    // -----------------------------------------------------------------------
    private HikariDataSource buildConnectionPool(DbCredentials creds) {
        HikariConfig config = new HikariConfig();

        // cr-java-0073 (Line 17): jdbcUrl replaces the raw DriverManager JDBC URL.
        // Set DB_HOST to the Amazon RDS Proxy endpoint for cloud-optimized pooling.
        config.setJdbcUrl(creds.url);

        // cr-java-0073 (Line 39): Username and password from Secrets Manager replace
        // the hard-coded credentials previously passed to DriverManager.getConnection().
        config.setUsername(creds.username);
        config.setPassword(creds.password);

        // Driver class name — HikariCP can auto-detect from the JDBC URL, but
        // setting it explicitly avoids classpath scanning overhead.
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool sizing — tuned for Amazon RDS Proxy which multiplexes connections.
        config.setMaximumPoolSize(DB_POOL_MAX_SIZE);
        config.setMinimumIdle(DB_POOL_MIN_IDLE);

        // HikariCP-level timeout configuration for cloud environments with variable latency.
        config.setConnectionTimeout(DB_POOL_CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(DB_POOL_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(DB_POOL_MAX_LIFETIME_MS);

        // Pool name for monitoring and CloudWatch metrics identification.
        config.setPoolName("MiniAppHikariPool");

        // Connection validation — ensures stale connections are detected and replaced.
        config.setConnectionTestQuery("SELECT 1");

        // cr-java-0097 (Line 39): Add network-level JDBC timeout properties.
        //
        // connectTimeout — maximum time (ms) to wait for the TCP connection to the
        //   database host to be established.  Prevents indefinite hangs when the
        //   database host is unreachable or the network path is congested.
        //   Resolved from DB_CONNECT_TIMEOUT_MS env var (default: 10 000 ms).
        //
        // socketTimeout — maximum time (ms) to wait for data to be received on an
        //   established socket (read timeout).  Prevents threads from blocking
        //   indefinitely on slow or stalled database queries.
        //   Resolved from DB_SOCKET_TIMEOUT_MS env var (default: 30 000 ms).
        config.addDataSourceProperty("connectTimeout", String.valueOf(DB_CONNECT_TIMEOUT_MS));
        config.addDataSourceProperty("socketTimeout", String.valueOf(DB_SOCKET_TIMEOUT_MS));

        // Additional MySQL/RDS optimizations for cloud environments.
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(config);
    }

    public void connect() {
        try {
            System.out.println("Connecting to database via HikariCP connection pool...");

            // cr-java-0069 / cr-java-0113: Credentials are now fetched from AWS Secrets Manager —
            // no username or password is hard-coded in source code.
            DbCredentials creds = fetchCredentialsFromSecretsManager();

            // cr-java-0073 (Line 17 + Line 39): Raw DriverManager replaced with HikariCP pool.
            // The dataSource manages the connection lifecycle; individual connections are
            // obtained via dataSource.getConnection() and returned to the pool on close().
            // Set DB_HOST to the Amazon RDS Proxy endpoint for cloud-optimized connection
            // multiplexing and automatic failover.
            dataSource = buildConnectionPool(creds);

            System.out.println("HikariCP connection pool initialized: " + creds.url);
            System.out.println("Pool size: " + DB_POOL_MAX_SIZE + " max / " + DB_POOL_MIN_IDLE + " min idle");
            System.out.println("Using username: " + creds.username);
            System.out.println("JDBC connect timeout: " + DB_CONNECT_TIMEOUT_MS + " ms");
            System.out.println("JDBC socket timeout:  " + DB_SOCKET_TIMEOUT_MS + " ms");

            // Cache connection using externalized port
            connectToCache();

            // External service URLs resolved from environment
            initializeExternalServices();

        } catch (Exception e) {
            System.err.println("Database connection pool initialization failed: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // cr-java-0077: REDIS_PORT is now resolved from the REDIS_PORT environment variable.
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // cr-java-0077: EXTERNAL_API_URL (including its port) is now resolved from
        // the EXTERNAL_API_URL environment variable.
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        // cr-java-0073: Connections are obtained from the HikariCP pool using
        // try-with-resources, ensuring they are always returned to the pool after use.
        // This replaces the previous pattern of holding a single raw Connection field.
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            // Query-level timeout — bounds individual statement execution time.
            stmt.setQueryTimeout(30);

            System.out.println("Executing query: " + sql);
            stmt.execute();

        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        // cr-java-0073: Closing the HikariDataSource shuts down the entire connection pool
        // gracefully, releasing all pooled connections back to the database.
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool closed");
        }
    }
}
