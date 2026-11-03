package com.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

/**
 * Mini Java Application — cloud-ready version.
 *
 * cr-java-0063 fix: All java.io.File-based persistent storage operations have been
 * replaced with Amazon S3 client calls (AWS SDK for Java v2) to achieve cloud-native,
 * durable, and scalable storage without host-level file system dependencies.
 *
 * cr-java-0070 fix: Classpath-bundled properties file loading (new File(CONFIG_FILE_PATH)
 * / new FileInputStream(configFile) / Properties.load()) has been replaced with AWS
 * Systems Manager Parameter Store integration.  All application configuration parameters
 * are now stored under a hierarchical SSM path (e.g. /mini-app/config/) and retrieved
 * at runtime via the AWS SSM GetParametersByPath API.  This enables:
 *   - Runtime configuration changes without application redeployment or image rebuilds
 *   - Environment-specific configuration (dev/staging/prod) via separate SSM paths
 *   - Centralized configuration management across all services and environments
 *   - Full audit logging of every configuration access via AWS CloudTrail
 *   - Fine-grained IAM access control over individual parameters
 *
 * cr-java-0077 fix: All hard-coded port numbers have been replaced with environment
 * variable lookups injected at runtime via ECS/EKS task definitions or Elastic Beanstalk
 * environment configuration, backed by AWS Systems Manager Parameter Store.
 *
 * Original violations replaced:
 *   Line 15 (src): private static final int SERVER_PORT = 8080  → System.getenv("SERVER_PORT")
 *   Line 79 (src): new ServerSocket(SERVER_PORT)                → new ServerSocket(resolvedServerPort)
 *   Line 44 (src): new File(CONFIG_FILE_PATH)                   → SSM GetParametersByPath (loadConfiguration)
 *   Line 60 (src): new File("/var/log")                         → S3 PutObject (initializeLogging)
 *   Line 62 (src): logDir.mkdirs()                              → S3 PutObject (initializeLogging)
 *   Line 65 (src): new File(LOG_FILE_PATH)                      → S3 PutObject (initializeLogging)
 */
public class MiniApp {

    // cr-java-0077 (Line 15): SERVER_PORT was hard-coded as 8080.
    // Now resolved from the SERVER_PORT environment variable (SSM Parameter Store backed)
    // so that container orchestration platforms (ECS, EKS, Elastic Beanstalk) can assign
    // ports dynamically without requiring code changes or image rebuilds.
    private static final int SERVER_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // AWS region — resolved from environment variable at runtime.
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // cr-java-0070: SSM Parameter Store path prefix for application configuration.
    // All application configuration parameters are stored under this hierarchical path
    // in AWS Systems Manager Parameter Store.  The path is resolved from the
    // SSM_CONFIG_PATH environment variable so it can be overridden per environment
    // (e.g. /mini-app/dev/config/, /mini-app/staging/config/, /mini-app/prod/config/).
    // Set SSM_CONFIG_PATH in the container / task environment to override the default.
    private static final String SSM_CONFIG_PATH =
            System.getenv().getOrDefault("SSM_CONFIG_PATH", "/mini-app/config/");

    // AWS S3 configuration — resolved from environment variables at runtime.
    // Used for log initialisation record storage (cr-java-0063 fix).
    // Set S3_BUCKET_NAME, S3_LOG_KEY, and AWS_REGION in the container / task
    // environment to override the defaults.
    private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "mini-app-bucket");
    private static final String S3_LOG_KEY      = System.getenv().getOrDefault("S3_LOG_KEY",      "logs/mini-app.log");

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        // Reading configuration from AWS Systems Manager Parameter Store
        loadConfiguration();

        // Writing log initialisation record to Amazon S3
        initializeLogging();

        // Initialize database connection
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    /**
     * cr-java-0070 (Line 46): Replaced {@code new File(CONFIG_FILE_PATH)} /
     * {@code new FileInputStream(configFile)} / {@code Properties.load()} with an
     * AWS Systems Manager Parameter Store GetParametersByPath call.
     *
     * <p>Previously the application loaded configuration from a classpath-bundled
     * {@code /opt/app/config/app.properties} file, making configuration immutable
     * at runtime and tightly coupled to the application artifact.  This violates
     * cloud-native externalized configuration principles (12-factor app, factor III).
     *
     * <p>The new implementation:
     * <ol>
     *   <li>Builds an {@link SsmClient} using the AWS Default Credential Provider Chain
     *       (IAM role, environment variables, ~/.aws/credentials, etc.).</li>
     *   <li>Calls {@code GetParametersByPath} with the hierarchical path prefix defined
     *       by the {@code SSM_CONFIG_PATH} environment variable (default:
     *       {@code /mini-app/config/}).</li>
     *   <li>Iterates through all returned parameters, loading each name/value pair into
     *       a {@link Properties} object for use by the application.</li>
     *   <li>Handles pagination automatically so that all parameters under the path are
     *       retrieved regardless of how many there are.</li>
     * </ol>
     *
     * <p>Benefits over classpath-bundled properties files:
     * <ul>
     *   <li>Configuration can be changed at runtime without redeployment or image rebuilds.</li>
     *   <li>Environment-specific configuration is achieved by using separate SSM paths
     *       (e.g. {@code /mini-app/dev/config/}, {@code /mini-app/prod/config/}).</li>
     *   <li>Centralized configuration management across all services and environments.</li>
     *   <li>Full audit logging of every configuration access via AWS CloudTrail.</li>
     *   <li>Fine-grained IAM access control over individual parameters.</li>
     *   <li>Support for SecureString parameter type to protect sensitive non-secret values.</li>
     * </ul>
     *
     * <p>To populate configuration in SSM Parameter Store, use the AWS CLI:
     * <pre>
     *   aws ssm put-parameter \
     *     --name "/mini-app/config/server.port" \
     *     --value "8080" \
     *     --type "String"
     *
     *   aws ssm put-parameter \
     *     --name "/mini-app/config/database.url" \
     *     --value "jdbc:mysql://db.example.com:3306/mini_app_db" \
     *     --type "String"
     * </pre>
     */
    private void loadConfiguration() {
        SsmClient ssmClient = SsmClient.builder()
                .region(Region.of(AWS_REGION))
                .build();
        try {
            Properties props = new Properties();
            String nextToken = null;

            // Paginate through all parameters under the configured SSM path prefix.
            do {
                GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder()
                        .path(SSM_CONFIG_PATH)
                        .recursive(true)
                        .withDecryption(true);

                if (nextToken != null) {
                    requestBuilder.nextToken(nextToken);
                }

                GetParametersByPathResponse response = ssmClient.getParametersByPath(requestBuilder.build());

                for (Parameter parameter : response.parameters()) {
                    // Strip the path prefix to get the bare parameter name,
                    // e.g. "/mini-app/config/server.port" → "server.port"
                    String paramName = parameter.name();
                    if (paramName.startsWith(SSM_CONFIG_PATH)) {
                        paramName = paramName.substring(SSM_CONFIG_PATH.length());
                    }
                    props.setProperty(paramName, parameter.value());
                }

                nextToken = response.nextToken();
            } while (nextToken != null);

            System.out.println("Configuration loaded from AWS SSM Parameter Store path: " + SSM_CONFIG_PATH);
            System.out.println("Total parameters loaded: " + props.size());

        } catch (Exception e) {
            System.err.println("Failed to load configuration from AWS SSM Parameter Store "
                    + "(path: " + SSM_CONFIG_PATH + "): " + e.getMessage());
        } finally {
            ssmClient.close();
        }
    }

    /**
     * cr-java-0063 (Lines 60, 62, 65): Replaced the three java.io.File operations:
     *   {@code new File("/var/log")}        — directory existence check
     *   {@code logDir.mkdirs()}             — directory creation
     *   {@code new File(LOG_FILE_PATH)}     — log file creation
     * with a single Amazon S3 PutObject call that writes the initialisation log entry
     * directly to S3, removing all host-level file system dependencies.
     */
    private void initializeLogging() {
        S3Client s3 = buildS3Client();
        try {
            String logEntry = "Logging initialized at: " + Instant.now().toString() + System.lineSeparator();

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(S3_LOG_KEY)
                    .contentType("text/plain")
                    .build();

            s3.putObject(putRequest, RequestBody.fromString(logEntry, StandardCharsets.UTF_8));
            System.out.println("Logging initialized in S3: s3://" + S3_BUCKET_NAME + "/" + S3_LOG_KEY);
        } catch (Exception e) {
            System.err.println("Failed to initialize logging in S3: " + e.getMessage());
        } finally {
            s3.close();
        }
    }

    /**
     * cr-java-0077 (Lines 15 and 79): SERVER_PORT is now resolved from the SERVER_PORT
     * environment variable at class-load time.  The ServerSocket is created using this
     * runtime-resolved value, enabling dynamic port assignment by ECS, EKS, or Elastic
     * Beanstalk without any code changes or image rebuilds.
     */
    private void startServer() {
        try {
            // cr-java-0077: SERVER_PORT is resolved from the SERVER_PORT environment
            // variable (backed by AWS Systems Manager Parameter Store) — no longer
            // hard-coded as 8080 in source code.
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

    /**
     * Builds an S3Client using the AWS region resolved from the environment.
     * Credentials are automatically resolved via the AWS Default Credential Provider Chain
     * (IAM role, environment variables, ~/.aws/credentials, etc.).
     */
    private S3Client buildS3Client() {
        return S3Client.builder()
                .region(Region.of(AWS_REGION))
                .build();
    }
}
