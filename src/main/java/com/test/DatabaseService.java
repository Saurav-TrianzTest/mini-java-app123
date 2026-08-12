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
import software.amazon.awssdk.http.apache.ApacheHttpClient;
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
 * Database service with credentials retrieved from AWS Secrets Manager and
 * port configuration resolved from AWS Systems Manager Parameter Store.
 *
 * FIXED (cr-java-0073): Raw JDBC DriverManager connections have been replaced
 * with HikariCP connection pooling backed by Amazon RDS Proxy. HikariCP manages
 * the connection lifecycle, enforces pool limits, and integrates seamlessly with
 * RDS Proxy for optimized database connections in cloud environments.
 *
 * Hard-coded port numbers (cr-java-0077) have been eliminated by externalizing
 * all port values to AWS SSM Parameter Store. At runtime the application first
 * queries Parameter Store; if the parameter is absent it falls back to the
 * corresponding environment variable, and finally to a safe default value.
 * This enables dynamic port assignment required by ECS, EKS, and Elastic
 * Beanstalk without any code changes or redeployment.
 *
 * FIXED (cr-java-0097): All AWS SDK clients (SsmClient, SecretsManagerClient)
 * are now built with explicit ClientOverrideConfiguration specifying
 * apiCallTimeout and apiCallAttemptTimeout, and with an ApacheHttpClient
 * specifying connectionTimeout and socketTimeout. This prevents connections
 * from hanging indefinitely in cloud environments with variable network
 * latency or transient service failures.
 */
public class DatabaseService {

    private static final Region AWS_REGION =
            Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

    // FIXED (cr-java-0097, line 39): SSM_CLIENT is now built with explicit
    // connection, socket, and API-call timeouts via ClientOverrideConfiguration
    // and ApacheHttpClient to prevent indefinite hangs in cloud environments.
    private static final SsmClient SSM_CLIENT = SsmClient.builder()
            .region(AWS_REGION)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClientBuilder(ApacheHttpClient.builder()
                    .connectionTimeout(Duration.ofMillis(Long.parseLong(
                            System.getenv().getOrDefault("AWS_SDK_CONNECT_TIMEOUT_MS", "3000"))))
                    .socketTimeout(Duration.ofMillis(Long.parseLong(
                            System.getenv().getOrDefault("AWS_SDK_SOCKET_TIMEOUT_MS", "5000")))))
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .apiCallTimeout(Duration.ofMillis(Long.parseLong(
                            System.getenv().getOrDefault("AWS_SDK_API_CALL_TIMEOUT_MS", "10000"))))
                    .apiCallAttemptTimeout(Duration.ofMillis(Long.parseLong(
                            System.getenv().getOrDefault("AWS_SDK_API_ATTEMPT_TIMEOUT_MS", "5000"))))
                    .retryPolicy(RetryPolicy.defaultRetryPolicy())
                    .build())
            .build();

    // Database host and name remain configurable via environment variables.
    // When Amazon RDS Proxy is used, DB_HOST should point to the RDS Proxy endpoint.
    private static final String DB_HOST =
            System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_NAME =
            System.getenv().getOrDefault("DB_NAME", "mini_app_db");

    // FIXED (cr-java-0077, line 17): DB_PORT is no longer hard-coded to "3306".
    // The port is resolved at runtime from AWS SSM Parameter Store
    // (/mini-app/db/port), falling back to the DB_PORT environment variable,
    // and finally to "3306" for local development.
    private static final String DB_PORT = resolvePortFromSsm(
            System.getenv().getOrDefault("SSM_DB_PORT_PARAM", "/mini-app/db/port"),
            System.getenv().getOrDefault("DB_PORT", "3306"));

    // FIXED (cr-java-0073): JDBC URL now targets the Amazon RDS Proxy endpoint
    // (supplied via DB_HOST env var) so that all connections are routed through
    // the proxy for connection multiplexing and IAM authentication support.
    private static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    // FIXED (lines 17-19): Removed hard-coded DB_USERNAME / DB_PASSWORD constants.
    // Credentials are now fetched at runtime from AWS Secrets Manager using the
    // secret name supplied via the DB_SECRET_NAME environment variable.
    private static final String DB_SECRET_NAME =
            System.getenv().getOrDefault("DB_SECRET_NAME", "mini-app/db-credentials");

    // FIXED (cr-java-0077, line 23): REDIS_PORT is no longer hard-coded to 6379.
    // The port is resolved at runtime from AWS SSM Parameter Store
    // (/mini-app/redis/port), falling back to the REDIS_PORT environment variable,
    // and finally to "6379" for local development.
    private static final String REDIS_HOST =
            System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT = Integer.parseInt(resolvePortFromSsm(
            System.getenv().getOrDefault("SSM_REDIS_PORT_PARAM", "/mini-app/redis/port"),
            System.getenv().getOrDefault("REDIS_PORT", "6379")));

    // FIXED (cr-java-0077, line 59): The port embedded in EXTERNAL_API_URL (8080)
    // is no longer hard-coded. The external API port is resolved from AWS SSM
    // Parameter Store (/mini-app/external-api/port), falling back to the
    // EXTERNAL_API_PORT environment variable, and finally to "8080".
    private static final String EXTERNAL_API_HOST =
            System.getenv().getOrDefault("EXTERNAL_API_HOST", "api.example.com");
    private static final String EXTERNAL_API_PORT = resolvePortFromSsm(
            System.getenv().getOrDefault("SSM_EXTERNAL_API_PORT_PARAM", "/mini-app/external-api/port"),
            System.getenv().getOrDefault("EXTERNAL_API_PORT", "8080"));
    private static final String EXTERNAL_API_PATH =
            System.getenv().getOrDefault("EXTERNAL_API_PATH", "/v1");
    private static final String EXTERNAL_API_URL =
            System.getenv().getOrDefault("EXTERNAL_API_URL",
                    "http://" + EXTERNAL_API_HOST + ":" + EXTERNAL_API_PORT + EXTERNAL_API_PATH);

    private static final String PAYMENT_SERVICE_URL =
            System.getenv().getOrDefault("PAYMENT_SERVICE_URL",
                    "https://payment.internal.company.com/process");

    // FIXED (cr-java-0073, lines 17 & 39): Replaced the raw java.sql.Connection
    // field (managed via DriverManager) with a HikariCP HikariDataSource.
    // HikariCP maintains a pool of pre-established connections and integrates
    // with Amazon RDS Proxy for efficient connection multiplexing in AWS.
    private HikariDataSource dataSource;

    /**
     * Resolves a port value from AWS SSM Parameter Store.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>AWS SSM Parameter Store – parameter identified by {@code ssmParamName}</li>
     *   <li>Environment variable fallback – {@code envFallback}</li>
     * </ol>
     *
     * @param ssmParamName the SSM parameter name (e.g. {@code /mini-app/db/port})
     * @param envFallback  the value to use when the SSM parameter is not found
     * @return the resolved port as a {@link String}
     */
    static String resolvePortFromSsm(String ssmParamName, String envFallback) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(ssmParamName)
                    .withDecryption(false)
                    .build();
            GetParameterResponse response = SSM_CLIENT.getParameter(request);
            String value = response.parameter().value();
            System.out.println("Resolved port from SSM Parameter Store ["
                    + ssmParamName + "]: " + value);
            return value;
        } catch (ParameterNotFoundException e) {
            System.out.println("SSM parameter not found [" + ssmParamName
                    + "], using fallback value: " + envFallback);
            return envFallback;
        } catch (Exception e) {
            System.err.println("Failed to retrieve SSM parameter [" + ssmParamName
                    + "]: " + e.getMessage() + ". Using fallback value: " + envFallback);
            return envFallback;
        }
    }

    /**
     * Retrieves database credentials from AWS Secrets Manager.
     *
     * The secret is expected to be stored as a JSON string with the keys
     * "username" and "password", for example:
     * <pre>{"username":"dbuser","password":"s3cr3t"}</pre>
     *
     * FIXED (cr-java-0097): SecretsManagerClient is built with explicit
     * connection, socket, and API-call timeouts to prevent indefinite hangs
     * when the Secrets Manager endpoint is unreachable or slow to respond.
     *
     * @return a two-element array where [0] is the username and [1] is the password
     * @throws RuntimeException if the secret cannot be retrieved or parsed
     */
    private String[] fetchDbCredentialsFromSecretsManager() {
        // FIXED (cr-java-0097, line 39): SecretsManagerClient is now built with
        // explicit ApacheHttpClient connectionTimeout / socketTimeout and
        // ClientOverrideConfiguration apiCallTimeout / apiCallAttemptTimeout.
        // Timeout values are externalised to environment variables so they can be
        // tuned per environment without code changes.
        try (SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                .region(AWS_REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(Duration.ofMillis(Long.parseLong(
                                System.getenv().getOrDefault("AWS_SDK_CONNECT_TIMEOUT_MS", "3000"))))
                        .socketTimeout(Duration.ofMillis(Long.parseLong(
                                System.getenv().getOrDefault("AWS_SDK_SOCKET_TIMEOUT_MS", "5000")))))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMillis(Long.parseLong(
                                System.getenv().getOrDefault("AWS_SDK_API_CALL_TIMEOUT_MS", "10000"))))
                        .apiCallAttemptTimeout(Duration.ofMillis(Long.parseLong(
                                System.getenv().getOrDefault("AWS_SDK_API_ATTEMPT_TIMEOUT_MS", "5000"))))
                        .retryPolicy(RetryPolicy.defaultRetryPolicy())
                        .build())
                .build()) {

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(DB_SECRET_NAME)
                    .build();

            GetSecretValueResponse response = secretsClient.getSecretValue(request);
            String secretJson = response.secretString();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretNode = mapper.readTree(secretJson);

            String username = secretNode.get("username").asText();
            String password = secretNode.get("password").asText();

            return new String[]{username, password};

        } catch (SecretsManagerException e) {
            throw new RuntimeException(
                    "Failed to retrieve database credentials from AWS Secrets Manager "
                            + "(secret: " + DB_SECRET_NAME + "): " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse database credentials from AWS Secrets Manager: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Initialises the HikariCP connection pool targeting the Amazon RDS Proxy endpoint.
     *
     * FIXED (cr-java-0073, lines 17 & 39):
     *   - Line 17: The raw {@code Connection connection} field has been replaced by a
     *     {@code HikariDataSource dataSource} that manages a pool of connections.
     *   - Line 39: The {@code DriverManager.getConnection()} call has been replaced by
     *     {@code HikariDataSource.getConnection()}, which borrows a connection from the
     *     pool rather than opening a new physical connection on every invocation.
     *
     * HikariCP pool settings are externalised to environment variables so they can be
     * tuned per environment without code changes:
     * <ul>
     *   <li>DB_POOL_MAX_SIZE       – maximum pool size (default: 10)</li>
     *   <li>DB_POOL_MIN_IDLE       – minimum idle connections (default: 2)</li>
     *   <li>DB_POOL_CONN_TIMEOUT   – connection timeout in ms (default: 30000)</li>
     *   <li>DB_POOL_IDLE_TIMEOUT   – idle timeout in ms (default: 600000)</li>
     *   <li>DB_POOL_MAX_LIFETIME   – max connection lifetime in ms (default: 1800000)</li>
     * </ul>
     */
    public void connect() {
        try {
            System.out.println("Initialising HikariCP connection pool...");

            // Fetch credentials from AWS Secrets Manager
            String[] credentials = fetchDbCredentialsFromSecretsManager();
            String dbUsername = credentials[0];
            String dbPassword = credentials[1];

            // FIXED (cr-java-0073, line 17 & 39): Build HikariCP configuration.
            // DB_HOST should be set to the Amazon RDS Proxy endpoint so that all
            // pooled connections are routed through the proxy.
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(DB_URL);
            hikariConfig.setUsername(dbUsername);
            hikariConfig.setPassword(dbPassword);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Pool sizing – externalised to environment variables
            hikariConfig.setMaximumPoolSize(Integer.parseInt(
                    System.getenv().getOrDefault("DB_POOL_MAX_SIZE", "10")));
            hikariConfig.setMinimumIdle(Integer.parseInt(
                    System.getenv().getOrDefault("DB_POOL_MIN_IDLE", "2")));

            // Timeout settings
            hikariConfig.setConnectionTimeout(Long.parseLong(
                    System.getenv().getOrDefault("DB_POOL_CONN_TIMEOUT", "30000")));
            hikariConfig.setIdleTimeout(Long.parseLong(
                    System.getenv().getOrDefault("DB_POOL_IDLE_TIMEOUT", "600000")));
            hikariConfig.setMaxLifetime(Long.parseLong(
                    System.getenv().getOrDefault("DB_POOL_MAX_LIFETIME", "1800000")));

            // Pool name for monitoring / JMX
            hikariConfig.setPoolName("MiniAppHikariPool");

            // FIXED (cr-java-0073, line 39): HikariDataSource replaces the single
            // DriverManager.getConnection() call. The pool is created once here and
            // connections are borrowed/returned on each executeQuery() invocation.
            dataSource = new HikariDataSource(hikariConfig);

            System.out.println("HikariCP connection pool initialised for: " + DB_URL);
            System.out.println("Using username retrieved from AWS Secrets Manager");

            connectToCache();
            initializeExternalServices();

        } catch (Exception e) {
            System.err.println("Failed to initialise HikariCP connection pool: " + e.getMessage());
        }
    }

    private void connectToCache() {
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        // FIXED (cr-java-0073): Connections are now borrowed from the HikariCP pool
        // (dataSource.getConnection()) and returned automatically via try-with-resources,
        // ensuring proper connection lifecycle management in cloud environments.
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
        // FIXED (cr-java-0073): Close the entire HikariCP pool gracefully instead
        // of closing a single raw JDBC connection.
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool closed");
        }
    }
}
