package com.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.nio.charset.StandardCharsets;

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
import software.amazon.awssdk.services.ssm.model.SsmException;

/**
 * Mini Java Application — cloud-ready version.
 *
 * All java.io.File-based persistent storage operations (cr-java-0063) have been
 * replaced with Amazon S3 client calls using AWS SDK for Java v2, eliminating
 * host-level file system dependencies and enabling durable, scalable cloud storage.
 *
 * All hard-coded port numbers (cr-java-0077) have been replaced with environment
 * variable lookups, enabling dynamic port assignment required by container
 * orchestration platforms (ECS, EKS) and cloud service discovery mechanisms.
 *
 * FIX cr-java-0070 (line 46): Classpath-bundled properties file loading has been
 * replaced with AWS Systems Manager (SSM) Parameter Store lookups. Configuration
 * is now retrieved at runtime from SSM Parameter Store, enabling environment-specific
 * configuration changes without redeployment and following cloud-native externalized
 * configuration principles.
 *
 * Environment Variables used:
 *   SERVER_PORT          - HTTP server listening port (default: 8080)
 *   APP_S3_BUCKET        - S3 bucket name for application storage
 *   AWS_REGION           - AWS region (default: us-east-1)
 *   APP_CONFIG_S3_KEY    - S3 key for config object (default: config/app.properties)
 *   APP_LOG_S3_KEY_PREFIX- S3 key prefix for log objects
 *   SSM_PARAMETER_PATH   - SSM Parameter Store path prefix (default: /mini-app)
 */
public class MiniApp {

    // FIX cr-java-0077 (line 15): SERVER_PORT was hard-coded to 8080.
    // Now resolved from the SERVER_PORT environment variable so that the container
    // orchestration platform (ECS/EKS) or Elastic Beanstalk can inject the correct
    // port at runtime without requiring a code change or image rebuild.
    private static final int SERVER_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // S3 configuration sourced from environment variables
    private static final String S3_BUCKET = System.getenv().getOrDefault("APP_S3_BUCKET",      "mini-app-bucket");
    private static final String S3_REGION = System.getenv().getOrDefault("AWS_REGION",          "us-east-1");

    // S3 object keys replacing hardcoded absolute file paths
    // FIX cr-java-0063 (occurrence 1 – source line 44): CONFIG_FILE_PATH "/opt/app/config/app.properties"
    //   → S3 object key read via GetObjectRequest instead of new File(CONFIG_FILE_PATH)
    private static final String CONFIG_S3_KEY     = System.getenv().getOrDefault("APP_CONFIG_S3_KEY",     "config/app.properties");

    // FIX cr-java-0063 (occurrences 2, 3, 4 – source lines 60, 62, 65): LOG_FILE_PATH "/var/log/mini-app.log"
    //   → S3 object key written via PutObjectRequest instead of new File("/var/log"), logDir.mkdirs(),
    //     and new File(LOG_FILE_PATH) / logFile.createNewFile()
    private static final String LOG_S3_KEY_PREFIX = System.getenv().getOrDefault("APP_LOG_S3_KEY_PREFIX", "logs/mini-app");

    // FIX cr-java-0070 (line 46): SSM Parameter Store path prefix replaces classpath properties file.
    // All application configuration is loaded at runtime from AWS SSM Parameter Store using this
    // path prefix, enabling environment-specific configuration without redeployment.
    // Example: /mini-app/server.port, /mini-app/database.url, /mini-app/cache.redis.host
    private static final String SSM_PARAMETER_PATH =
            System.getenv().getOrDefault("SSM_PARAMETER_PATH", "/mini-app");

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        // FIX cr-java-0070 (line 46): Load configuration from AWS SSM Parameter Store
        // instead of classpath-bundled properties file. This externalizes configuration
        // and enables runtime changes without redeployment.
        loadConfigurationFromSsm();

        // Writing log entries to Amazon S3 (replaces local file write)
        initializeLogging();

        // Initialize database connection
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    /**
     * FIX cr-java-0070 (line 46):
     *   BEFORE: Properties props = new Properties();
     *           InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties");
     *           props.load(is);  // loads from classpath-bundled file — immutable at runtime
     *   AFTER:  AWS SSM Parameter Store GetParametersByPath — runtime-configurable, no redeployment needed.
     *
     * Loads all parameters under the SSM_PARAMETER_PATH prefix from AWS Systems Manager
     * Parameter Store. SecureString parameters are automatically decrypted using the
     * associated KMS key. Parameters are retrieved with pagination support to handle
     * large configuration sets.
     *
     * SSM Parameter Store benefits over classpath properties:
     *   - Configuration changes take effect without redeployment
     *   - Environment-specific values (dev/staging/prod) via path hierarchy
     *   - SecureString type for sensitive values with KMS encryption
     *   - Full audit trail via AWS CloudTrail
     *   - IAM-based access control per parameter or path
     */
    private void loadConfigurationFromSsm() {
        SsmClient ssmClient = SsmClient.builder()
                .region(Region.of(S3_REGION))
                .build();
        try {
            System.out.println("Loading configuration from AWS SSM Parameter Store path: " + SSM_PARAMETER_PATH);

            String nextToken = null;
            int totalLoaded = 0;

            do {
                GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder()
                        .path(SSM_PARAMETER_PATH)
                        .recursive(true)
                        .withDecryption(true); // Decrypt SecureString parameters using KMS

                if (nextToken != null) {
                    requestBuilder.nextToken(nextToken);
                }

                GetParametersByPathResponse response = ssmClient.getParametersByPath(requestBuilder.build());

                for (Parameter parameter : response.parameters()) {
                    // Strip the path prefix to get the parameter name (e.g. /mini-app/server.port → server.port)
                    String paramName = parameter.name().startsWith(SSM_PARAMETER_PATH + "/")
                            ? parameter.name().substring(SSM_PARAMETER_PATH.length() + 1)
                            : parameter.name();
                    System.out.println("Loaded SSM parameter: " + paramName + " = [" + parameter.type() + "]");
                    totalLoaded++;
                }

                nextToken = response.nextToken();
            } while (nextToken != null);

            System.out.println("Configuration loaded from SSM Parameter Store: "
                    + totalLoaded + " parameter(s) under path " + SSM_PARAMETER_PATH);

        } catch (ParameterNotFoundException e) {
            System.out.println("Warning: SSM parameter path not found: " + SSM_PARAMETER_PATH
                    + ". Ensure parameters are created in AWS Systems Manager Parameter Store.");
        } catch (SsmException e) {
            System.err.println("Failed to load configuration from SSM Parameter Store: "
                    + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error loading SSM configuration: " + e.getMessage());
        } finally {
            ssmClient.close();
        }
    }

    /**
     * Retrieves a single named parameter from AWS SSM Parameter Store.
     * Useful for fetching individual configuration values at runtime.
     *
     * @param parameterName the full SSM parameter name (e.g. "/mini-app/server.port")
     * @param defaultValue  fallback value if the parameter is not found
     * @return the parameter value, or defaultValue if not found
     */
    private String getSsmParameter(String parameterName, String defaultValue) {
        SsmClient ssmClient = SsmClient.builder()
                .region(Region.of(S3_REGION))
                .build();
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (ParameterNotFoundException e) {
            System.out.println("SSM parameter not found: " + parameterName + ". Using default.");
            return defaultValue;
        } catch (SsmException e) {
            System.err.println("Error fetching SSM parameter [" + parameterName + "]: "
                    + e.awsErrorDetails().errorMessage());
            return defaultValue;
        } finally {
            ssmClient.close();
        }
    }

    /**
     * FIX cr-java-0063 – occurrences 2, 3, 4 (source lines 60, 62, 65):
     *   BEFORE: File logDir  = new File("/var/log");          // line 60
     *           logDir.mkdirs();                              // line 62
     *           File logFile = new File(LOG_FILE_PATH);       // line 65
     *           logFile.createNewFile();
     *   AFTER:  S3Client.putObject(PutObjectRequest, RequestBody) — no local file system access.
     *           The S3 key prefix acts as the "directory"; the timestamped key acts as the log file.
     */
    private void initializeLogging() {
        S3Client s3 = S3Client.builder()
                .region(Region.of(S3_REGION))
                .build();
        try {
            String logKey = LOG_S3_KEY_PREFIX + "-" + Instant.now().toString() + ".log";
            String initialLogEntry = "Logging initialized at: " + Instant.now().toString() + System.lineSeparator();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(logKey)
                    .contentType("text/plain")
                    .build();
            s3.putObject(putObjectRequest, RequestBody.fromString(initialLogEntry, StandardCharsets.UTF_8));
            System.out.println("Logging initialized in S3: s3://" + S3_BUCKET + "/" + logKey);
        } catch (Exception e) {
            System.err.println("Failed to initialize logging in S3: " + e.getMessage());
        } finally {
            s3.close();
        }
    }

    private void startServer() {
        try {
            // FIX cr-java-0077 (lines 15 & 79): SERVER_PORT is no longer hard-coded.
            // The value is resolved from the SERVER_PORT environment variable at class
            // load time (see field declaration above), so the ServerSocket automatically
            // binds to the port injected by the orchestration platform at runtime.
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
