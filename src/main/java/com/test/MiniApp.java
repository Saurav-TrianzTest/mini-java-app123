package com.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

/**
 * Mini Java Application with cloud-native AWS SSM Parameter Store-based
 * configuration, S3-based logging, and dynamic port resolution.
 *
 * FIXED (cr-java-0070, line 46): Application configuration is no longer loaded
 * from a classpath-bundled properties file (application.properties). All
 * configuration parameters are externalised to AWS Systems Manager Parameter
 * Store and loaded at runtime via {@link #loadConfigurationFromSsm()}. This
 * enables runtime configuration changes without redeployment and satisfies the
 * 12-factor app principle of strict separation of config from code.
 *
 * The SSM parameter path prefix is controlled by the SSM_CONFIG_PATH_PREFIX
 * environment variable (default: /mini-app/config). All parameters under that
 * prefix are fetched in a single GetParametersByPath call and stored in an
 * in-memory {@link Properties} map for use throughout the application lifecycle.
 *
 * Hard-coded port numbers (cr-java-0077) have been eliminated by externalizing
 * the server port to AWS Systems Manager Parameter Store. At runtime the
 * application first queries Parameter Store for the port value; if the
 * parameter is absent it falls back to the SERVER_PORT environment variable,
 * and finally to 8080 for local development. This enables dynamic port
 * assignment required by ECS, EKS, and Elastic Beanstalk without any code
 * changes or redeployment.
 *
 * Hard-coded absolute file paths have been replaced with Amazon S3 object
 * storage operations using AWS SDK for Java v2.
 */
public class MiniApp {

    private static final Region AWS_REGION =
            Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

    /**
     * SSM path prefix under which all application configuration parameters are
     * stored. Controlled by the SSM_CONFIG_PATH_PREFIX environment variable.
     * Example parameters stored under this prefix:
     *   /mini-app/config/server.port
     *   /mini-app/config/server.host
     *   /mini-app/config/database.url
     *   /mini-app/config/cache.redis.host
     *   /mini-app/config/cache.redis.port
     *   /mini-app/config/external.api.base-url
     *   /mini-app/config/environment
     *   /mini-app/config/logging.level
     */
    private static final String SSM_CONFIG_PATH_PREFIX =
            System.getenv().getOrDefault("SSM_CONFIG_PATH_PREFIX", "/mini-app/config");

    // Shared SSM client used for Parameter Store lookups.
    // FIXED (cr-java-0097): Built with explicit connection, socket, and API-call
    // timeouts via ClientOverrideConfiguration and ApacheHttpClient to prevent
    // indefinite hangs in cloud environments.
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

    // FIXED (cr-java-0077, line 15): SERVER_PORT is no longer hard-coded to 8080.
    // The port is resolved at runtime from AWS SSM Parameter Store
    // (/mini-app/server/port), falling back to the SERVER_PORT environment
    // variable, and finally to "8080" for local development.
    private static final int SERVER_PORT = Integer.parseInt(resolvePortFromSsm(
            System.getenv().getOrDefault("SSM_SERVER_PORT_PARAM", "/mini-app/server/port"),
            System.getenv().getOrDefault("SERVER_PORT", "8080")));

    // S3 bucket and object keys are supplied via environment variables so that
    // no absolute host-file-system paths are embedded in the source code.
    private static final String S3_BUCKET =
            System.getenv().getOrDefault("APP_S3_BUCKET", "my-app-bucket");
    private static final String S3_CONFIG_KEY =
            System.getenv().getOrDefault("APP_S3_CONFIG_KEY", "config/app.properties");
    private static final String S3_LOG_KEY_PREFIX =
            System.getenv().getOrDefault("APP_S3_LOG_KEY_PREFIX", "logs/mini-app");

    // Shared S3 client – created once and reused across operations
    private final S3Client s3Client;

    /**
     * In-memory configuration properties loaded from AWS SSM Parameter Store.
     * Replaces the classpath-bundled application.properties file (cr-java-0070).
     */
    private final Properties appConfig = new Properties();

    public MiniApp() {
        this.s3Client = S3Client.builder()
                .region(AWS_REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(Duration.ofMillis(Long.parseLong(
                                System.getenv().getOrDefault("AWS_SDK_CONNECT_TIMEOUT_MS", "3000"))))
                        .socketTimeout(Duration.ofMillis(Long.parseLong(
                                System.getenv().getOrDefault("AWS_SDK_SOCKET_TIMEOUT_MS", "5000")))))
                .build();
    }

    /**
     * Resolves a port value from AWS SSM Parameter Store.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>AWS SSM Parameter Store – parameter identified by {@code ssmParamName}</li>
     *   <li>Environment variable fallback – {@code envFallback}</li>
     * </ol>
     *
     * @param ssmParamName the SSM parameter name (e.g. {@code /mini-app/server/port})
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
     * Loads all application configuration parameters from AWS SSM Parameter Store.
     *
     * FIXED (cr-java-0070, line 46): This method replaces the previous pattern of
     * loading configuration from a classpath-bundled {@code application.properties}
     * file (i.e., {@code props.load(new FileInputStream(configFile))} or
     * {@code getClass().getResourceAsStream("/application.properties")}). Instead,
     * all parameters stored under the SSM path prefix
     * ({@value #SSM_CONFIG_PATH_PREFIX} by default, overridable via
     * SSM_CONFIG_PATH_PREFIX env var) are fetched at runtime using
     * {@code GetParametersByPath} with recursive traversal and decryption enabled.
     *
     * <p>This approach:
     * <ul>
     *   <li>Externalises configuration from the application artifact</li>
     *   <li>Enables runtime configuration changes without redeployment</li>
     *   <li>Supports environment-specific configuration (dev/staging/prod) via
     *       different SSM path prefixes</li>
     *   <li>Integrates with AWS IAM for fine-grained access control</li>
     *   <li>Supports SecureString parameters for sensitive non-secret config</li>
     * </ul>
     *
     * <p>Parameter names are mapped to property keys by stripping the path prefix.
     * For example, the SSM parameter {@code /mini-app/config/server.port} is stored
     * in {@link #appConfig} as the key {@code server.port}.
     *
     * <p>If SSM is unavailable (e.g., local development without AWS credentials),
     * the method falls back gracefully to environment variables and built-in defaults
     * so that the application can still start.
     */
    private void loadConfigurationFromSsm() {
        System.out.println("Loading configuration from AWS SSM Parameter Store path: "
                + SSM_CONFIG_PATH_PREFIX);

        Map<String, String> ssmParams = new HashMap<>();
        String nextToken = null;

        try {
            // Paginate through all parameters under the config path prefix
            do {
                GetParametersByPathRequest.Builder requestBuilder =
                        GetParametersByPathRequest.builder()
                                .path(SSM_CONFIG_PATH_PREFIX)
                                .recursive(true)
                                .withDecryption(true)
                                .maxResults(10);

                if (nextToken != null) {
                    requestBuilder.nextToken(nextToken);
                }

                GetParametersByPathResponse response =
                        SSM_CLIENT.getParametersByPath(requestBuilder.build());

                for (Parameter param : response.parameters()) {
                    // Strip the path prefix to derive the property key.
                    // e.g. /mini-app/config/server.port  ->  server.port
                    String key = param.name().substring(SSM_CONFIG_PATH_PREFIX.length());
                    if (key.startsWith("/")) {
                        key = key.substring(1);
                    }
                    ssmParams.put(key, param.value());
                }

                nextToken = response.nextToken();

            } while (nextToken != null);

            // Populate the in-memory Properties object from SSM parameters
            appConfig.putAll(ssmParams);

            System.out.println("Configuration loaded from AWS SSM Parameter Store: "
                    + ssmParams.size() + " parameter(s) retrieved from path: "
                    + SSM_CONFIG_PATH_PREFIX);

        } catch (Exception e) {
            System.err.println("Warning: Could not load configuration from SSM Parameter Store ["
                    + SSM_CONFIG_PATH_PREFIX + "]: " + e.getMessage()
                    + ". Falling back to environment variables and built-in defaults.");

            // Graceful fallback: populate appConfig from environment variables
            // so the application can still start in environments without SSM access.
            applyEnvironmentVariableFallbacks();
        }
    }

    /**
     * Applies environment variable fallbacks to {@link #appConfig} when SSM
     * Parameter Store is unavailable (e.g., local development).
     *
     * This ensures the application can start without AWS credentials while still
     * honouring the same configuration keys that SSM would provide.
     */
    private void applyEnvironmentVariableFallbacks() {
        appConfig.setProperty("server.port",
                System.getenv().getOrDefault("SERVER_PORT", "8080"));
        appConfig.setProperty("server.host",
                System.getenv().getOrDefault("SERVER_HOST", "localhost"));
        appConfig.setProperty("database.url",
                System.getenv().getOrDefault("DATABASE_URL",
                        "jdbc:mysql://localhost:3306/mini_app_db"));
        appConfig.setProperty("cache.redis.host",
                System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1"));
        appConfig.setProperty("cache.redis.port",
                System.getenv().getOrDefault("REDIS_PORT", "6379"));
        appConfig.setProperty("external.api.base-url",
                System.getenv().getOrDefault("EXTERNAL_API_URL",
                        "http://api.example.com:8080/v1"));
        appConfig.setProperty("environment",
                System.getenv().getOrDefault("APP_ENVIRONMENT", "production"));
        appConfig.setProperty("logging.level",
                System.getenv().getOrDefault("LOGGING_LEVEL", "INFO"));

        System.out.println("Applied environment variable fallbacks for application configuration.");
    }

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        // FIXED (cr-java-0070, line 46): Load configuration from AWS SSM Parameter
        // Store instead of from the classpath-bundled application.properties file.
        // This externalises all configuration from the application artifact and
        // enables runtime changes without redeployment.
        loadConfigurationFromSsm();

        // Replaced hard-coded file path reads with S3 object reads
        loadConfiguration();

        // Replaced hard-coded file path writes with S3 object puts
        initializeLogging();

        // Initialize database connection
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    /**
     * Loads application configuration from Amazon S3 (supplementary runtime config).
     *
     * Previously read from the hard-coded absolute path /opt/app/config/app.properties
     * (line 44 in the original source). Now retrieves the object identified by
     * APP_S3_BUCKET / APP_S3_CONFIG_KEY using the AWS SDK v2 S3Client.
     *
     * Note: Primary configuration is loaded from SSM Parameter Store via
     * {@link #loadConfigurationFromSsm()}. This S3-based load provides an optional
     * supplementary configuration layer (e.g., feature flags, large config blobs).
     */
    private void loadConfiguration() {
        // FIXED (line 44): replaced new File(CONFIG_FILE_PATH) / FileInputStream
        //   with S3Client.getObject() – eliminates the /opt/app/config absolute path dependency.
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(S3_CONFIG_KEY)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object =
                    s3Client.getObject(getObjectRequest);

            Properties s3Props = new Properties();
            s3Props.load(s3Object);

            // Merge S3 properties into appConfig (SSM values take precedence)
            s3Props.forEach((key, value) -> appConfig.putIfAbsent(key.toString(), value.toString()));

            System.out.println("Supplementary configuration loaded from S3: s3://"
                    + S3_BUCKET + "/" + S3_CONFIG_KEY);

        } catch (NoSuchKeyException e) {
            System.out.println("Info: Supplementary S3 configuration object not found at: s3://"
                    + S3_BUCKET + "/" + S3_CONFIG_KEY + " (non-fatal, SSM config is primary)");
        } catch (IOException e) {
            System.err.println("Failed to load supplementary configuration from S3: "
                    + e.getMessage());
        }
    }

    /**
     * Initialises application logging by writing a log-init marker object to Amazon S3.
     *
     * Previously created the directory /var/log and the file /var/log/mini-app.log
     * on the local file system (lines 60 and 65 in the original source). Both
     * operations are now replaced with a single S3 PutObject call so that no
     * ephemeral host-file-system paths are required.
     */
    private void initializeLogging() {
        // FIXED (line 60): replaced new File("/var/log").mkdirs()
        //   with S3 key-prefix convention – no local directory creation needed.
        // FIXED (line 65): replaced new File(LOG_FILE_PATH).createNewFile()
        //   with S3Client.putObject() – eliminates the /var/log/mini-app.log absolute path.
        try {
            String logInitKey = S3_LOG_KEY_PREFIX + "/app-init.log";
            String logInitContent = "Logging initialised at: "
                    + java.time.Instant.now().toString() + "\n"
                    + "Environment: " + appConfig.getProperty("environment", "unknown") + "\n"
                    + "Log level: " + appConfig.getProperty("logging.level", "INFO") + "\n";

            byte[] contentBytes = logInitContent.getBytes(StandardCharsets.UTF_8);
            InputStream contentStream = new ByteArrayInputStream(contentBytes);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(logInitKey)
                    .contentType("text/plain")
                    .contentLength((long) contentBytes.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                    contentStream, contentBytes.length));

            System.out.println("Logging initialised in S3: s3://"
                    + S3_BUCKET + "/" + logInitKey);

        } catch (Exception e) {
            System.err.println("Failed to initialise logging in S3: " + e.getMessage());
        }
    }

    // FIXED (cr-java-0077, line 79): ServerSocket now uses SERVER_PORT which is
    // resolved from AWS SSM Parameter Store at startup instead of a hard-coded
    // literal, enabling dynamic port assignment by the container orchestrator.
    private void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port: " + SERVER_PORT);
            System.out.println("Server ready to accept connections...");

            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
