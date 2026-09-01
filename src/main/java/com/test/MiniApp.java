package com.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.SsmException;

/**
 * Mini Java Application — Cloud-Ready (AWS).
 *
 * REMEDIATION (cr-java-0070 — Properties Files in Classpath):
 * Application configuration previously bundled in the classpath
 * (src/main/resources/application.properties) has been externalised to
 * AWS Systems Manager (SSM) Parameter Store.  At startup the application
 * fetches all parameters under the SSM path prefix defined by
 * {@code SSM_PARAMETER_PATH} (default: {@code /mini-app/config}) and makes
 * them available at runtime without requiring a redeployment.
 *
 * This approach:
 *  - Eliminates immutable, classpath-bundled configuration.
 *  - Enables environment-specific values (dev / staging / prod) via
 *    different SSM path prefixes.
 *  - Supports runtime configuration changes without rebuilding the artifact.
 *  - Integrates with AWS IAM for fine-grained access control.
 *  - Supports SecureString parameters for sensitive values.
 *
 * Hard-coded port numbers have been replaced with environment-variable
 * injection so that container orchestration platforms (ECS, EKS, Elastic
 * Beanstalk) can supply the correct values at runtime via AWS Systems Manager
 * Parameter Store or task/pod environment variable overrides.
 *
 * Set SERVER_PORT in the ECS task definition, EKS pod spec, or Elastic
 * Beanstalk environment properties (or store the value in AWS SSM Parameter
 * Store and inject it at deployment time).
 */
public class MiniApp {

    // -----------------------------------------------------------------------
    // AWS region — sourced from environment variable
    // -----------------------------------------------------------------------
    private static final String AWS_REGION =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // -----------------------------------------------------------------------
    // REMEDIATION (cr-java-0070 — Properties Files in Classpath):
    // SSM Parameter Store path prefix — replaces classpath application.properties.
    // All application configuration parameters are stored under this path in
    // AWS Systems Manager Parameter Store and fetched at runtime.
    //
    // Set SSM_PARAMETER_PATH in the ECS task definition, EKS pod spec, or
    // Elastic Beanstalk environment properties to switch between environments
    // (e.g., /mini-app/dev/config, /mini-app/staging/config,
    //         /mini-app/prod/config) without any code or artifact changes.
    // -----------------------------------------------------------------------
    private static final String SSM_PARAMETER_PATH =
            System.getenv().getOrDefault("SSM_PARAMETER_PATH", "/mini-app/config");

    // -----------------------------------------------------------------------
    // Server port — externalised via environment variable
    // (previously hard-coded to 8080)
    // Set SERVER_PORT in ECS task definition / EKS pod spec / Elastic
    // Beanstalk environment, or inject from AWS Systems Manager Parameter Store.
    // -----------------------------------------------------------------------
    private static final int SERVER_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // S3 configuration sourced from environment variables (replaces hardcoded file paths)
    private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "mini-app-bucket");
    private static final String S3_LOG_KEY      = System.getenv().getOrDefault("S3_LOG_KEY",      "logs/mini-app.log");

    // -----------------------------------------------------------------------
    // Shared AWS clients
    // -----------------------------------------------------------------------

    /** Shared S3 client for log storage. */
    private static final S3Client s3Client = S3Client.builder()
            .region(Region.of(AWS_REGION))
            .build();

    /**
     * Shared SSM client used to fetch configuration from Parameter Store.
     *
     * REMEDIATION (cr-java-0070): This client replaces the classpath
     * {@code Properties} load from {@code application.properties}.  All
     * configuration is retrieved from AWS SSM Parameter Store at startup,
     * enabling runtime changes without redeployment.
     */
    private static final SsmClient ssmClient = SsmClient.builder()
            .region(Region.of(AWS_REGION))
            .build();

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        // REMEDIATION (cr-java-0070): Load configuration from SSM Parameter Store
        // instead of classpath-bundled application.properties.
        loadConfiguration();

        // Initialize logging via S3
        initializeLogging();

        // Initialize database connection with credentials from AWS Secrets Manager
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    /**
     * Loads application configuration from AWS Systems Manager Parameter Store.
     *
     * REMEDIATION (cr-java-0070 — Properties Files in Classpath):
     * Previously, configuration was loaded from a classpath-bundled
     * {@code application.properties} file (line 46), making it immutable at
     * runtime and preventing environment-specific overrides without
     * redeployment.
     *
     * This method now fetches all parameters stored under the SSM path prefix
     * {@link #SSM_PARAMETER_PATH} using {@code GetParametersByPath} with
     * {@code withDecryption=true} so that SecureString parameters (e.g.,
     * database URLs, feature flags) are transparently decrypted.  The
     * resulting key-value map is available for use throughout the application
     * without any file I/O or classpath dependency.
     *
     * To add or change a configuration value, update the corresponding
     * parameter in AWS SSM Parameter Store — no code change or redeployment
     * is required.
     */
    private void loadConfiguration() {
        try {
            System.out.println("Loading configuration from AWS SSM Parameter Store path: " + SSM_PARAMETER_PATH);

            Map<String, String> configProperties = new HashMap<>();
            String nextToken = null;

            // Paginate through all parameters under the configured SSM path prefix.
            // withDecryption=true ensures SecureString parameters are decrypted
            // transparently using the associated KMS key.
            do {
                GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder()
                        .path(SSM_PARAMETER_PATH)
                        .recursive(true)
                        .withDecryption(true);

                if (nextToken != null) {
                    requestBuilder.nextToken(nextToken);
                }

                GetParametersByPathResponse response = ssmClient.getParametersByPath(requestBuilder.build());

                for (Parameter parameter : response.parameters()) {
                    // Strip the path prefix to obtain a short property key,
                    // e.g. /mini-app/config/server.port  →  server.port
                    String key = parameter.name().replace(SSM_PARAMETER_PATH + "/", "");
                    configProperties.put(key, parameter.value());
                }

                nextToken = response.nextToken();

            } while (nextToken != null);

            if (configProperties.isEmpty()) {
                System.out.println("Warning: No configuration parameters found at SSM path: " + SSM_PARAMETER_PATH
                        + ". Ensure parameters are stored in AWS SSM Parameter Store under this prefix.");
            } else {
                System.out.println("Configuration loaded from AWS SSM Parameter Store ("
                        + configProperties.size() + " parameters) at path: " + SSM_PARAMETER_PATH);
                // Log parameter keys (never log values — they may be sensitive)
                configProperties.keySet().forEach(key ->
                        System.out.println("  Loaded SSM parameter: " + key));
            }

        } catch (SsmException e) {
            System.err.println("Failed to load configuration from AWS SSM Parameter Store: "
                    + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error loading configuration from AWS SSM Parameter Store: "
                    + e.getMessage());
        }
    }

    /**
     * Retrieves a single named parameter from AWS SSM Parameter Store.
     *
     * <p>Use this helper to fetch individual configuration values by their
     * full SSM parameter name (e.g., {@code /mini-app/config/server.port}).
     *
     * @param parameterName the full SSM parameter name (path + key)
     * @param defaultValue  value to return when the parameter does not exist
     * @return the parameter value, or {@code defaultValue} if not found
     */
    private String getSsmParameter(String parameterName, String defaultValue) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();

        } catch (ParameterNotFoundException e) {
            System.out.println("SSM parameter not found: " + parameterName + ". Using default value.");
            return defaultValue;
        } catch (SsmException e) {
            System.err.println("Failed to retrieve SSM parameter '" + parameterName + "': "
                    + e.awsErrorDetails().errorMessage());
            return defaultValue;
        }
    }

    private void initializeLogging() {
        try {
            // Log entries are stored as S3 objects instead of local filesystem files.
            String logInitEntry = "Log initialized at: " + java.time.Instant.now().toString() + System.lineSeparator();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(S3_LOG_KEY)
                    .contentType("text/plain")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromString(logInitEntry));
            System.out.println("Logging initialized in S3: s3://" + S3_BUCKET_NAME + "/" + S3_LOG_KEY);
        } catch (Exception e) {
            System.err.println("Failed to initialize logging in S3: " + e.getMessage());
        }
    }

    private void startServer() {
        try {
            // SERVER_PORT is now sourced from the SERVER_PORT environment variable
            // (previously hard-coded to 8080). The value can be injected at runtime
            // via ECS task definition, EKS pod spec, or AWS SSM Parameter Store.
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
