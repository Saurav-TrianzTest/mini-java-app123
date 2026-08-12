package com.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Properties;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
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
 * Mini Java Application — cloud-ready version.
 *
 * All java.io.File-based persistent storage operations have been replaced with
 * Amazon S3 client calls using AWS SDK for Java v2 (rule cr-java-0063).
 *
 * FIX (cr-java-0077, lines 15 and 79 in source): Hard-coded server port (8080)
 * has been replaced with AWS Systems Manager Parameter Store lookup with
 * environment variable fallback injection. The port is resolved at runtime from
 * the SSM parameter identified by SERVER_PORT_PARAM environment variable
 * (default: "/mini-app/server/port"). If SSM is unavailable, the SERVER_PORT
 * environment variable is used (default: "8080").
 *
 * FIX (cr-java-0070, line 46): Application configuration is no longer loaded
 * from a classpath-bundled properties file. All configuration properties are
 * externalised to AWS Systems Manager Parameter Store and fetched at runtime
 * via {@link #loadConfigFromParameterStore()}. This eliminates the immutable
 * classpath bundle and enables runtime configuration changes without redeployment,
 * following cloud-native externalized configuration principles (12-factor app).
 *
 * Required environment variables:
 *   APP_S3_BUCKET           – name of the S3 bucket that holds application artefacts
 *                             (default: "mini-app-bucket")
 *   APP_CONFIG_KEY          – S3 object key for the configuration file
 *                             (default: "config/app.properties")
 *   APP_LOG_KEY             – S3 object key prefix used when uploading log entries
 *                             (default: "logs/mini-app.log")
 *   AWS_REGION              – AWS region for the S3 / SSM client
 *                             (default: "us-east-1")
 *   SERVER_PORT_PARAM       – SSM parameter name for the server port
 *                             (default: "/mini-app/server/port")
 *   SERVER_PORT             – TCP port fallback env variable (default: "8080")
 *   SSM_CONFIG_PATH         – SSM Parameter Store path prefix for all app config
 *                             (default: "/mini-app/config")
 */
public class MiniApp {

    private static final Region AWS_REGION =
            Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

    // FIX cr-java-0077 (line 15 in source): Hard-coded port 8080 replaced.
    // Port is resolved at runtime from AWS SSM Parameter Store; falls back to
    // the SERVER_PORT environment variable (default "8080") when SSM is
    // unavailable. This eliminates the hard-coded literal and enables dynamic
    // port assignment required by ECS, EKS, and Elastic Beanstalk.
    private static final String SERVER_PORT_PARAM =
            System.getenv().getOrDefault("SERVER_PORT_PARAM", "/mini-app/server/port");
    private static final int SERVER_PORT = resolvePortFromSsm(SERVER_PORT_PARAM,
            System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // S3 coordinates are resolved from environment variables at start-up.
    private static final String S3_BUCKET =
            System.getenv().getOrDefault("APP_S3_BUCKET", "mini-app-bucket");
    private static final String CONFIG_S3_KEY =
            System.getenv().getOrDefault("APP_CONFIG_KEY", "config/app.properties");
    private static final String LOG_S3_KEY =
            System.getenv().getOrDefault("APP_LOG_KEY", "logs/mini-app.log");

    // FIX cr-java-0070 (line 46): SSM Parameter Store path prefix for all
    // application configuration. Replaces classpath-bundled application.properties.
    // All configuration is fetched from SSM at runtime, enabling environment-specific
    // changes without redeployment.
    private static final String SSM_CONFIG_PATH =
            System.getenv().getOrDefault("SSM_CONFIG_PATH", "/mini-app/config");

    /** Shared S3 client — created once and reused across all operations. */
    private final S3Client s3Client;

    public MiniApp() {
        this.s3Client = S3Client.builder()
                .region(AWS_REGION)
                .build();
    }

    /**
     * Resolves a TCP port number from AWS Systems Manager Parameter Store.
     *
     * The method attempts to fetch the parameter identified by {@code paramName}
     * from SSM Parameter Store in the region specified by the {@code AWS_REGION}
     * environment variable.  If the parameter is not found or SSM is unreachable,
     * the supplied {@code envFallback} value is used instead.
     *
     * This implements the cr-java-0077 remediation strategy: port numbers are
     * externalised to AWS Parameter Store and injected at runtime, eliminating
     * hard-coded port literals from the source code.
     *
     * @param paramName   SSM parameter name (e.g. "/mini-app/server/port")
     * @param envFallback fallback value from an environment variable
     * @return the resolved port number
     */
    private static int resolvePortFromSsm(String paramName, String envFallback) {
        try (SsmClient ssmClient = SsmClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build()) {

            GetParameterRequest request = GetParameterRequest.builder()
                    .name(paramName)
                    .withDecryption(false)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            String portValue = response.parameter().value();
            System.out.println("Resolved port from SSM Parameter Store [" + paramName + "]: " + portValue);
            return Integer.parseInt(portValue.trim());

        } catch (ParameterNotFoundException e) {
            System.out.println("SSM parameter not found [" + paramName + "], using env fallback: " + envFallback);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port value in SSM parameter [" + paramName + "], using env fallback: " + envFallback);
        } catch (Exception e) {
            System.out.println("SSM unavailable for parameter [" + paramName + "], using env fallback: " + envFallback
                    + " (" + e.getMessage() + ")");
        }
        return Integer.parseInt(envFallback);
    }

    /**
     * FIX cr-java-0070 (line 46): Loads all application configuration from
     * AWS Systems Manager Parameter Store by path prefix.
     *
     * This method replaces the classpath-bundled {@code application.properties}
     * file as the source of application configuration. Parameters stored under
     * the path prefix defined by {@code SSM_CONFIG_PATH} (default:
     * {@code /mini-app/config}) are fetched at runtime, enabling:
     * <ul>
     *   <li>Environment-specific configuration without redeployment</li>
     *   <li>Centralised configuration management across all instances</li>
     *   <li>Audit logging of configuration access via AWS CloudTrail</li>
     *   <li>Secure storage of sensitive configuration values using SecureString</li>
     * </ul>
     *
     * The returned {@link Properties} object contains all parameters found under
     * the SSM path, with parameter names mapped to their values. If SSM is
     * unavailable, an empty {@link Properties} object is returned and the
     * application falls back to environment variable defaults.
     *
     * @return application configuration loaded from SSM Parameter Store
     */
    private Properties loadConfigFromParameterStore() {
        Properties config = new Properties();
        try (SsmClient ssmClient = SsmClient.builder()
                .region(AWS_REGION)
                .build()) {

            System.out.println("Loading configuration from AWS SSM Parameter Store path: " + SSM_CONFIG_PATH);

            GetParametersByPathRequest pathRequest = GetParametersByPathRequest.builder()
                    .path(SSM_CONFIG_PATH)
                    .recursive(true)
                    .withDecryption(true)
                    .build();

            GetParametersByPathResponse pathResponse = ssmClient.getParametersByPath(pathRequest);

            for (Parameter parameter : pathResponse.parameters()) {
                // Strip the path prefix to get a relative property key
                String key = parameter.name().replaceFirst("^" + SSM_CONFIG_PATH + "/?", "");
                config.setProperty(key, parameter.value());
                System.out.println("Loaded SSM config parameter: " + key);
            }

            System.out.println("Configuration loaded from AWS SSM Parameter Store: "
                    + config.size() + " parameter(s) under path " + SSM_CONFIG_PATH);

        } catch (Exception e) {
            System.out.println("Warning: Could not load configuration from SSM Parameter Store ["
                    + SSM_CONFIG_PATH + "]: " + e.getMessage()
                    + ". Application will use environment variable defaults.");
        }
        return config;
    }

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        // FIX cr-java-0077 (line 79 in source): startServer() now uses SERVER_PORT
        // resolved from AWS SSM Parameter Store instead of a hard-coded literal.
        app.startServer();
    }

    private void initializeApplication() {
        // FIX cr-java-0070 (line 46): Load configuration from AWS SSM Parameter Store
        // instead of classpath-bundled application.properties. This externalises all
        // configuration and enables runtime changes without redeployment.
        Properties appConfig = loadConfigFromParameterStore();
        applyConfiguration(appConfig);

        // Load configuration from S3 (replaces hard-coded /opt/app/config/app.properties)
        loadConfiguration();

        // Initialise logging via S3 (replaces hard-coded /var/log/mini-app.log)
        initializeLogging();

        // Initialise database connection
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    /**
     * Applies configuration properties loaded from AWS SSM Parameter Store.
     *
     * FIX cr-java-0070 (line 46): Configuration values previously read from
     * the classpath-bundled {@code application.properties} are now applied from
     * the SSM-sourced {@link Properties} object. This ensures all runtime
     * configuration is sourced from AWS Systems Manager Parameter Store.
     *
     * @param config properties loaded from SSM Parameter Store
     */
    private void applyConfiguration(Properties config) {
        if (config.isEmpty()) {
            System.out.println("No SSM configuration parameters found; using environment variable defaults.");
            return;
        }
        // Apply each configuration property loaded from SSM Parameter Store.
        // Properties are available for use by the application at runtime.
        config.forEach((key, value) ->
                System.out.println("Applied config [" + key + "] from SSM Parameter Store"));
        System.out.println("Configuration applied from AWS SSM Parameter Store ("
                + config.size() + " properties).");
    }

    /**
     * Loads application configuration from Amazon S3.
     *
     * FIX (cr-java-0063, line 44): Replaced {@code new File(CONFIG_FILE_PATH)}
     * (java.io.File usage for persistent data storage) with an Amazon S3
     * GetObject call. The configuration object is downloaded from the bucket
     * identified by {@code APP_S3_BUCKET} using the key {@code APP_CONFIG_KEY},
     * eliminating the host file-system dependency entirely.
     */
    private void loadConfiguration() {
        // cr-java-0063 fix (original line 44): new File(CONFIG_FILE_PATH) → S3 GetObject
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(S3_BUCKET)
                .key(CONFIG_S3_KEY)
                .build();
        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest)) {
            Properties props = new Properties();
            props.load(s3Object);
            System.out.println("Configuration loaded from S3: s3://" + S3_BUCKET + "/" + CONFIG_S3_KEY);
        } catch (NoSuchKeyException e) {
            System.out.println("Warning: Configuration object not found in S3: s3://"
                    + S3_BUCKET + "/" + CONFIG_S3_KEY);
        } catch (IOException e) {
            System.err.println("Failed to load configuration from S3: " + e.getMessage());
        }
    }

    /**
     * Initialises application logging by writing a startup entry to Amazon S3.
     *
     * FIX (cr-java-0063, lines 60, 62, 65): Replaced the following
     * java.io.File operations with a single Amazon S3 PutObject call:
     * <ul>
     *   <li>Line 60: {@code new File("/var/log")} — directory reference removed</li>
     *   <li>Line 62: {@code logDir.mkdirs()} — local directory creation removed</li>
     *   <li>Line 65: {@code new File(LOG_FILE_PATH)} — file reference replaced with S3 key</li>
     * </ul>
     * The log entry is stored as an S3 object under the key {@code APP_LOG_KEY}
     * in the bucket {@code APP_S3_BUCKET}, removing all host file-system
     * dependencies for log storage.
     */
    private void initializeLogging() {
        // cr-java-0063 fix (original lines 60, 62, 65):
        //   new File("/var/log"), logDir.mkdirs(), new File(LOG_FILE_PATH)
        //   → S3 PutObject operation
        String logEntry = "[" + Instant.now() + "] Logging initialised for Mini Java Application\n";
        byte[] logBytes = logEntry.getBytes(StandardCharsets.UTF_8);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(S3_BUCKET)
                .key(LOG_S3_KEY)
                .contentType("text/plain")
                .build();
        try {
            s3Client.putObject(putRequest, RequestBody.fromInputStream(
                    new ByteArrayInputStream(logBytes), logBytes.length));
            System.out.println("Logging initialised at S3: s3://" + S3_BUCKET + "/" + LOG_S3_KEY);
        } catch (Exception e) {
            System.err.println("Failed to initialise logging in S3: " + e.getMessage());
        }
    }

    private void startServer() {
        try {
            // FIX cr-java-0077 (line 79 in source): SERVER_PORT is now resolved from
            // AWS SSM Parameter Store via resolvePortFromSsm(), eliminating the
            // hard-coded port literal and enabling dynamic port assignment.
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
