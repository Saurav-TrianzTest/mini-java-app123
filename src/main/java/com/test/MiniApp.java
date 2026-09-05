package com.test;

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

/**
 * Mini Java Application with intentional containerization blockers for testing
 */
public class MiniApp {

    // cr-java-0077 fix (line 15): Replaced hard-coded SERVER_PORT = 8080 with
    // environment variable injection. The SERVER_PORT environment variable is
    // populated at runtime by ECS task definitions, EKS pod specs, or Elastic
    // Beanstalk environment properties — all sourced from AWS Systems Manager
    // Parameter Store — enabling dynamic port assignment by the orchestration
    // platform and eliminating deployment conflicts.
    private static final int SERVER_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // S3 configuration sourced from environment variables
    // cr-java-0063: Replaced java.io.File-based storage with Amazon S3 client calls
    private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "mini-app-bucket");
    private static final String S3_LOG_KEY      = System.getenv().getOrDefault("S3_LOG_KEY",      "logs/mini-app.log");
    private static final String AWS_REGION      = System.getenv().getOrDefault("AWS_REGION",      "us-east-1");

    // cr-java-0070 fix (line 46): AWS Systems Manager Parameter Store configuration.
    // The SSM parameter path prefix is sourced from the SSM_PARAMETER_PATH environment
    // variable, enabling environment-specific parameter namespacing (e.g.
    // /mini-app/dev/, /mini-app/prod/) without code changes.  All application
    // configuration parameters are stored as SSM parameters under this path prefix
    // and fetched at runtime via GetParametersByPath, replacing the classpath-bundled
    // application.properties file that was previously immutable at runtime.
    private static final String SSM_PARAMETER_PATH =
            System.getenv().getOrDefault("SSM_PARAMETER_PATH", "/mini-app/config");

    // S3 client (shared across methods)
    private final S3Client s3Client;

    // cr-java-0070 fix: SSM client used to retrieve configuration parameters
    // from AWS Systems Manager Parameter Store at runtime.
    private final SsmClient ssmClient;

    public MiniApp() {
        this.s3Client = S3Client.builder()
                .region(Region.of(AWS_REGION))
                .build();
        // cr-java-0070 fix: Initialise the SSM client once and reuse it across
        // all parameter fetch operations.  The region is sourced from the
        // AWS_REGION environment variable, consistent with all other AWS SDK
        // clients in this application.
        this.ssmClient = SsmClient.builder()
                .region(Region.of(AWS_REGION))
                .build();
    }

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }
    
    private void initializeApplication() {
        // cr-java-0070 fix: Reading configuration from AWS Systems Manager Parameter Store
        loadConfiguration();
        
        // Writing log placeholder to Amazon S3
        initializeLogging();
        
        // Initialize database connection with hardcoded values
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }
    
    /**
     * cr-java-0070 fix (line 46): Replaced classpath-bundled properties file loading
     * with AWS Systems Manager Parameter Store retrieval.
     *
     * Previously the application loaded configuration from a Properties object
     * populated from a classpath resource (application.properties), making the
     * configuration immutable at runtime and requiring a full rebuild and
     * redeployment to change any property value.
     *
     * This method now uses SsmClient.getParametersByPath() to fetch all parameters
     * stored under the SSM_PARAMETER_PATH prefix (e.g. /mini-app/config/) at
     * application startup.  Parameters are stored as SecureString or String values
     * in AWS Systems Manager Parameter Store and can be updated at any time without
     * redeployment, satisfying the 12-factor app externalized configuration principle
     * and enabling environment-specific configuration (dev / staging / prod) through
     * different path prefixes.
     *
     * The SSM_PARAMETER_PATH environment variable controls which parameter namespace
     * is loaded, allowing the same application artifact to be deployed across
     * multiple environments with different configurations.
     */
    private void loadConfiguration() {
        try {
            System.out.println("Loading configuration from AWS Systems Manager Parameter Store: "
                    + SSM_PARAMETER_PATH);

            // cr-java-0070 fix: Use GetParametersByPath to retrieve all parameters
            // under the configured path prefix in a single paginated API call.
            // withDecryption(true) ensures SecureString parameters (e.g. API keys,
            // passwords) are automatically decrypted using the associated KMS key.
            Map<String, String> configParams = new HashMap<>();
            String nextToken = null;

            do {
                GetParametersByPathRequest.Builder requestBuilder = GetParametersByPathRequest.builder()
                        .path(SSM_PARAMETER_PATH)
                        .recursive(true)
                        .withDecryption(true)
                        .maxResults(10);

                if (nextToken != null) {
                    requestBuilder.nextToken(nextToken);
                }

                GetParametersByPathResponse response = ssmClient.getParametersByPath(requestBuilder.build());

                for (Parameter param : response.parameters()) {
                    // Strip the path prefix to obtain the short parameter name
                    // e.g. /mini-app/config/server.port → server.port
                    String shortName = param.name().startsWith(SSM_PARAMETER_PATH)
                            ? param.name().substring(SSM_PARAMETER_PATH.length()).replaceFirst("^/", "")
                            : param.name();
                    configParams.put(shortName, param.value());
                }

                nextToken = response.nextToken();
            } while (nextToken != null);

            System.out.println("Configuration loaded from AWS SSM Parameter Store. "
                    + "Parameters retrieved: " + configParams.size()
                    + " under path: " + SSM_PARAMETER_PATH);

            // Apply loaded parameters to system properties so that Spring Boot
            // and other framework components can consume them via standard
            // property resolution (e.g. @Value, Environment).
            for (Map.Entry<String, String> entry : configParams.entrySet()) {
                System.setProperty(entry.getKey(), entry.getValue());
            }

        } catch (ParameterNotFoundException e) {
            System.out.println("Warning: No parameters found in AWS SSM Parameter Store at path: "
                    + SSM_PARAMETER_PATH + ". Using default values.");
        } catch (Exception e) {
            System.err.println("Failed to load configuration from AWS SSM Parameter Store: "
                    + e.getMessage());
        }
    }

    /**
     * Convenience method to retrieve a single named parameter from AWS SSM
     * Parameter Store.  This can be used by other components to fetch
     * individual configuration values on demand (e.g. feature flags that
     * change frequently).
     *
     * @param parameterName the full SSM parameter name (e.g. /mini-app/config/feature.flag)
     * @param defaultValue  value to return if the parameter does not exist
     * @return the parameter value, or defaultValue if not found
     */
    public String getParameter(String parameterName, String defaultValue) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (ParameterNotFoundException e) {
            System.out.println("SSM parameter not found: " + parameterName
                    + ". Using default: " + defaultValue);
            return defaultValue;
        } catch (Exception e) {
            System.err.println("Failed to retrieve SSM parameter '" + parameterName
                    + "': " + e.getMessage() + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    private void initializeLogging() {
        try {
            // cr-java-0063 fix (lines 60, 62, 65): Replaced java.io.File usages:
            //   - new File("/var/log")          → removed (no local directory creation needed)
            //   - new File(LOG_FILE_PATH)        → removed (no local file handle needed)
            //   - logFile.createNewFile()        → replaced with S3 PutObjectRequest
            // Log initialization content is now written directly to Amazon S3 as a cloud-native
            // object, eliminating all host-level file system dependencies.
            String logInitContent = "Log initialized at: " + java.time.Instant.now().toString();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET_NAME)
                    .key(S3_LOG_KEY)
                    .contentType("text/plain")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromString(logInitContent));
            System.out.println("Logging initialized in S3: s3://" + S3_BUCKET_NAME + "/" + S3_LOG_KEY);
        } catch (Exception e) {
            System.err.println("Failed to initialize logging in S3: " + e.getMessage());
        }
    }
    
    private void startServer() {
        try {
            // cr-java-0077 fix (line 79): SERVER_PORT is now sourced from the
            // SERVER_PORT environment variable (injected from AWS Parameter Store)
            // instead of the former hard-coded literal 8080, enabling dynamic
            // port assignment by the container orchestration platform.
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
