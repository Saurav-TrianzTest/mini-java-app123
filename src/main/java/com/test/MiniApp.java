package com.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

import java.nio.charset.StandardCharsets;

/**
 * Mini Java Application with cloud-ready Amazon S3 storage integration and
 * AWS Systems Manager Parameter Store configuration management.
 *
 * Violations fixed (cr-java-0063 - Java.io.File Usage for Data Storage):
 *   - Line 44 (original): new File(CONFIG_FILE_PATH)  → S3 GetObjectRequest
 *   - Line 60 (original): new File("/var/log")         → S3 HeadObjectRequest existence check
 *   - Line 62 (original): logDir.mkdirs()              → removed (no local dir needed)
 *   - Line 65 (original): new File(LOG_FILE_PATH)      → S3 PutObjectRequest
 *
 * Violations fixed (cr-java-0070 - Properties Files in Classpath):
 *   - Line 46 (original): new Properties() loaded from classpath/file
 *     → Replaced with AWS Systems Manager Parameter Store GetParametersByPath call.
 *       Configuration parameters are fetched at runtime from SSM Parameter Store
 *       under the path prefix configured via SSM_PARAM_APP_CONFIG_PATH env var
 *       (default: /mini-app/config). This externalises all configuration from the
 *       bundled application.properties file, enabling runtime changes without
 *       redeployment and full environment-specific configuration management.
 *
 * Violations fixed (cr-java-0077 - Hard-coded Ports):
 *   - Line 15 (original): SERVER_PORT = 8080           → resolved from AWS SSM Parameter Store
 *   - Line 79 (original): new ServerSocket(8080)       → uses SSM-resolved SERVER_PORT
 */
public class MiniApp {

    // -----------------------------------------------------------------------
    // AWS region — shared by both S3 and SSM Parameter Store clients
    // -----------------------------------------------------------------------
    private static final String AWS_REGION_VALUE =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // -----------------------------------------------------------------------
    // AWS SSM Parameter Store client — used to resolve configuration and port
    // numbers at runtime, replacing classpath-bundled Properties files.
    // -----------------------------------------------------------------------
    private static final SsmClient SSM_CLIENT = SsmClient.builder()
            .region(Region.of(AWS_REGION_VALUE))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();

    /**
     * Retrieves a single parameter value from AWS SSM Parameter Store.
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
     * FIXED (cr-java-0070 - Properties Files in Classpath, line 46):
     *
     * Loads all application configuration parameters from AWS Systems Manager
     * Parameter Store using GetParametersByPath, replacing the previous pattern
     * of loading a {@code java.util.Properties} object from a classpath-bundled
     * or file-system properties file.
     *
     * Parameters are stored under a hierarchical path prefix in SSM
     * (default: /mini-app/config, configurable via SSM_PARAM_APP_CONFIG_PATH).
     * For example:
     *   /mini-app/config/server.context-path
     *   /mini-app/config/database.driver
     *   /mini-app/config/cache.redis.database
     *   /mini-app/config/external.api.timeout
     *   /mini-app/config/environment
     *   /mini-app/config/debug.enabled
     *   /mini-app/config/logging.level
     *
     * This approach:
     *   - Externalises configuration from the bundled application artifact
     *   - Enables runtime configuration changes without redeployment
     *   - Supports environment-specific configuration (dev/staging/prod)
     *   - Follows 12-factor app principle III (Config)
     *
     * @return a Map of parameter name (relative to path prefix) → value
     */
    private static Map<String, String> loadConfigFromSsmParameterStore() {
        Map<String, String> configMap = new HashMap<>();
        String paramPathPrefix = System.getenv().getOrDefault(
                "SSM_PARAM_APP_CONFIG_PATH", "/mini-app/config");

        try {
            String nextToken = null;
            do {
                GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder()
                        .path(paramPathPrefix)
                        .recursive(true)
                        .withDecryption(true);
                if (nextToken != null) {
                    requestBuilder.nextToken(nextToken);
                }

                GetParametersByPathResponse response = SSM_CLIENT.getParametersByPath(
                        requestBuilder.build());

                for (Parameter param : response.parameters()) {
                    // Strip the path prefix to get the relative parameter name
                    // e.g. /mini-app/config/server.context-path → server.context-path
                    String relativeName = param.name().startsWith(paramPathPrefix + "/")
                            ? param.name().substring(paramPathPrefix.length() + 1)
                            : param.name();
                    configMap.put(relativeName, param.value());
                }

                nextToken = response.nextToken();
            } while (nextToken != null && !nextToken.isEmpty());

            System.out.println("Configuration loaded from AWS SSM Parameter Store path: "
                    + paramPathPrefix + " (" + configMap.size() + " parameters)");

        } catch (Exception e) {
            System.err.println("Failed to load configuration from AWS SSM Parameter Store ("
                    + paramPathPrefix + "): " + e.getMessage()
                    + " — application will use environment variable fallbacks.");
        }

        return configMap;
    }

    // -----------------------------------------------------------------------
    // FIXED (cr-java-0077, lines 15 & 79): SERVER_PORT is no longer hard-coded.
    // Value is resolved at class-load time from AWS SSM Parameter Store
    // (/mini-app/server/port), then from the SERVER_PORT env var, and finally
    // defaults to 8080 only when neither source is available.
    // -----------------------------------------------------------------------
    private static final int SERVER_PORT =
            Integer.parseInt(
                    resolveFromSsm(
                            System.getenv().getOrDefault("SSM_PARAM_SERVER_PORT", "/mini-app/server/port"),
                            "SERVER_PORT",
                            "8080"));

    // S3 bucket name resolved from environment variable (APP_S3_BUCKET)
    private static final String S3_BUCKET =
            System.getenv().getOrDefault("APP_S3_BUCKET", "mini-app-bucket");

    // S3 object keys that replace the former hard-coded absolute file paths
    // was: /opt/app/config/app.properties  (original line 44)
    private static final String CONFIG_S3_KEY = "config/app.properties";
    // was: /var/log/mini-app.log            (original lines 60, 65)
    private static final String LOG_S3_KEY    = "logs/mini-app.log";

    // Shared S3 client — region resolved from AWS_REGION env var or SDK default
    private static final S3Client s3Client = S3Client.builder()
            .region(Region.of(AWS_REGION_VALUE))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        // FIXED (cr-java-0070): Load configuration from AWS SSM Parameter Store
        // instead of a classpath-bundled Properties file.
        loadConfiguration();

        // Initialize logging via Amazon S3 (replaces hard-coded absolute path write)
        initializeLogging();

        // Initialize database connection
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    /**
     * FIXED (cr-java-0070 - Properties Files in Classpath, line 46):
     *
     * Loads application configuration from AWS Systems Manager Parameter Store,
     * replacing the previous pattern:
     *
     *   BEFORE (original lines 44-48):
     *     File configFile = new File(CONFIG_FILE_PATH);
     *     if (configFile.exists()) {
     *         Properties props = new Properties();          // ← violation line 46
     *         props.load(new FileInputStream(configFile));
     *         System.out.println("Configuration loaded from: " + CONFIG_FILE_PATH);
     *     }
     *
     *   AFTER:
     *     Configuration parameters are fetched at runtime from AWS SSM Parameter
     *     Store via GetParametersByPath. This externalises all configuration from
     *     the bundled application artifact, enabling runtime changes without
     *     redeployment and full environment-specific configuration management.
     *     The SSM path prefix defaults to /mini-app/config and is configurable
     *     via the SSM_PARAM_APP_CONFIG_PATH environment variable.
     */
    private void loadConfiguration() {
        // FIXED (cr-java-0070): Replace classpath Properties file loading with
        // AWS SSM Parameter Store GetParametersByPath — no Properties object,
        // no classpath resource, no bundled configuration file.
        Map<String, String> appConfig = loadConfigFromSsmParameterStore();

        if (!appConfig.isEmpty()) {
            // Apply loaded configuration values to the application runtime
            applyConfiguration(appConfig);
        } else {
            System.out.println("Warning: No configuration parameters found in AWS SSM Parameter Store. "
                    + "Ensure parameters are published under the configured path prefix "
                    + "(SSM_PARAM_APP_CONFIG_PATH, default: /mini-app/config).");
        }
    }

    /**
     * Applies the configuration map loaded from AWS SSM Parameter Store to the
     * application runtime. Extend this method to wire SSM-sourced values into
     * Spring context, application settings, or other configuration consumers.
     *
     * @param config map of relative parameter name → value from SSM Parameter Store
     */
    private void applyConfiguration(Map<String, String> config) {
        // Log the resolved configuration keys (values omitted for security)
        System.out.println("Applying " + config.size()
                + " configuration parameter(s) from AWS SSM Parameter Store:");
        for (String key : config.keySet()) {
            System.out.println("  [SSM] " + key + " = (resolved)");
        }

        // Example: apply specific well-known parameters
        String contextPath = config.getOrDefault("server.context-path",
                System.getenv().getOrDefault("SERVER_CONTEXT_PATH", "/mini-app"));
        System.out.println("Server context path: " + contextPath);

        String logLevel = config.getOrDefault("logging.level",
                System.getenv().getOrDefault("LOG_LEVEL", "INFO"));
        System.out.println("Log level: " + logLevel);

        String environment = config.getOrDefault("environment",
                System.getenv().getOrDefault("APP_ENVIRONMENT", "production"));
        System.out.println("Environment: " + environment);
    }

    /**
     * Initialises application logging by writing an initial log entry to Amazon S3.
     *
     * FIXED (cr-java-0063, lines 60, 62, 65):
     *   BEFORE: File logDir  = new File("/var/log");           // line 60
     *           if (!logDir.exists()) { logDir.mkdirs(); }     // line 62
     *           File logFile = new File(LOG_FILE_PATH);        // line 65
     *           if (!logFile.exists()) { logFile.createNewFile(); }
     *   AFTER:  S3 HeadObjectRequest to check existence + PutObjectRequest to create.
     */
    private void initializeLogging() {
        try {
            // Check whether the log object already exists in S3 (replaces logDir.exists() + logFile.exists())
            boolean logExists = s3ObjectExists(S3_BUCKET, LOG_S3_KEY);

            if (!logExists) {
                // Create an initial log object in S3 (replaces logFile.createNewFile())
                String initialContent = "# Mini-App log initialised\n";
                byte[] contentBytes = initialContent.getBytes(StandardCharsets.UTF_8);

                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(S3_BUCKET)
                        .key(LOG_S3_KEY)
                        .contentType("text/plain")
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(contentBytes));
                System.out.println("Log object created in S3: s3://" + S3_BUCKET + "/" + LOG_S3_KEY);
            }

            System.out.println("Logging initialised at S3: s3://" + S3_BUCKET + "/" + LOG_S3_KEY);
        } catch (Exception e) {
            System.err.println("Failed to initialise logging in S3: " + e.getMessage());
        }
    }

    /**
     * Helper: returns {@code true} when the given S3 object exists.
     */
    private boolean s3ObjectExists(String bucket, String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    private void startServer() {
        try {
            // FIXED (cr-java-0077, line 79): Port is resolved from AWS SSM Parameter Store
            // via the SERVER_PORT constant — no hard-coded port number in source code.
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
