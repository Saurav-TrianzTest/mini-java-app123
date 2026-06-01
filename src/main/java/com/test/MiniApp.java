package com.test;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Properties;

/**
 * Mini Java Application — cloud-ready version.
 *
 * Blockers addressed:
 *  - cr-java-0061 (blocker-1,2,3)  : Hard-coded file paths → Amazon S3
 *  - cr-java-0063 (blocker-4,5,6,7): java.io.File usage → Amazon S3 via AWS SDK v2
 *  - cr-java-0077 (blocker-14,15)  : Hard-coded ports → AWS SSM Parameter Store / env vars
 *  - cr-java-0070 (blocker-20)     : Properties files in classpath → AWS SSM Parameter Store
 */
public class MiniApp {

    // -----------------------------------------------------------------------
    // AWS region — injected via environment variable
    // -----------------------------------------------------------------------
    private static final String AWS_REGION =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // -----------------------------------------------------------------------
    // S3 configuration — replaces hard-coded absolute file paths
    // (blocker-1, blocker-2, blocker-3, blocker-4, blocker-5, blocker-6, blocker-7)
    // -----------------------------------------------------------------------
    private static final String S3_BUCKET =
            System.getenv().getOrDefault("APP_S3_BUCKET", "mini-app-storage");

    /** S3 key for the application configuration object (was /opt/app/config/app.properties) */
    private static final String CONFIG_S3_KEY =
            System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties");

    /** S3 key prefix for log objects (was /var/log/mini-app.log) */
    private static final String LOG_S3_KEY =
            System.getenv().getOrDefault("LOG_S3_KEY", "logs/mini-app.log");

    // -----------------------------------------------------------------------
    // SSM Parameter Store key for the server port
    // (blocker-14, blocker-15 — hard-coded port 8080)
    // -----------------------------------------------------------------------
    private static final String SERVER_PORT_PARAM =
            System.getenv().getOrDefault("SERVER_PORT_PARAM", "/mini-app/server/port");

    // -----------------------------------------------------------------------
    // SSM Parameter Store key for the application configuration path
    // (blocker-20 — properties file in classpath)
    // -----------------------------------------------------------------------
    private static final String APP_CONFIG_PARAM_PATH =
            System.getenv().getOrDefault("APP_CONFIG_PARAM_PATH", "/mini-app/config/");

    // -----------------------------------------------------------------------
    // AWS clients
    // -----------------------------------------------------------------------
    private final S3Client s3Client;
    private final SsmClient ssmClient;

    public MiniApp() {
        Region region = Region.of(AWS_REGION);
        this.s3Client = S3Client.builder()
                .region(region)
                .build();
        this.ssmClient = SsmClient.builder()
                .region(region)
                .build();
    }

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");
        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    // -----------------------------------------------------------------------
    // Initialisation
    // -----------------------------------------------------------------------

    private void initializeApplication() {
        loadConfiguration();
        initializeLogging();

        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    // -----------------------------------------------------------------------
    // Configuration loading from Amazon S3 + AWS SSM Parameter Store
    // (blocker-1, blocker-4, blocker-20)
    // -----------------------------------------------------------------------

    private void loadConfiguration() {
        // Primary: load configuration from Amazon S3 (replaces hard-coded /opt/app/config/app.properties)
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(CONFIG_S3_KEY)
                    .build();

            try (ResponseInputStream<GetObjectResponse> s3Object =
                         s3Client.getObject(getObjectRequest);
                 InputStream inputStream = s3Object) {

                Properties props = new Properties();
                props.load(inputStream);
                System.out.println("Configuration loaded from S3: s3://" + S3_BUCKET + "/" + CONFIG_S3_KEY);

                // Supplement / override with AWS SSM Parameter Store values (blocker-20)
                loadSsmParameters(props);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load configuration from S3 ("
                    + e.getMessage() + "). Falling back to SSM Parameter Store.");
            // Fallback: load entirely from SSM Parameter Store
            Properties props = new Properties();
            loadSsmParameters(props);
        }
    }

    /**
     * Loads application configuration from AWS SSM Parameter Store.
     * Replaces classpath-bundled properties file (blocker-20).
     */
    private void loadSsmParameters(Properties existingProps) {
        String[] paramKeys = {
            APP_CONFIG_PARAM_PATH + "server.port",
            APP_CONFIG_PARAM_PATH + "database.url",
            APP_CONFIG_PARAM_PATH + "external.api.base-url"
        };
        for (String paramKey : paramKeys) {
            try {
                GetParameterRequest request = GetParameterRequest.builder()
                        .name(paramKey)
                        .withDecryption(true)
                        .build();
                GetParameterResponse response = ssmClient.getParameter(request);
                String shortKey = paramKey.replace(APP_CONFIG_PARAM_PATH, "");
                existingProps.setProperty(shortKey, response.parameter().value());
                System.out.println("Loaded SSM parameter: " + paramKey);
            } catch (Exception e) {
                System.err.println("Warning: SSM parameter not found: " + paramKey);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Logging initialisation — writes log marker to Amazon S3
    // (blocker-2, blocker-3, blocker-5, blocker-6, blocker-7)
    // Replaces java.io.File operations on /var/log/mini-app.log
    // -----------------------------------------------------------------------

    private void initializeLogging() {
        try {
            String logContent = "Mini-app logging initialised at: "
                    + java.time.Instant.now().toString() + System.lineSeparator();

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(LOG_S3_KEY)
                    .contentType("text/plain")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromString(logContent));
            System.out.println("Logging initialised. Log marker written to S3: s3://"
                    + S3_BUCKET + "/" + LOG_S3_KEY);
        } catch (Exception e) {
            System.err.println("Warning: Could not write log marker to S3: " + e.getMessage());
            // Non-fatal — application continues
        }
    }

    // -----------------------------------------------------------------------
    // Server startup — port resolved from SSM Parameter Store / env var
    // (blocker-14, blocker-15 — replaces hard-coded SERVER_PORT = 8080)
    // -----------------------------------------------------------------------

    private void startServer() {
        int serverPort = resolveServerPort();
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("Server started on port: " + serverPort);
            System.out.println("Server ready to accept connections...");

            // Simulate server running
            Thread.sleep(1000);
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    /**
     * Resolves the server port from AWS SSM Parameter Store, falling back to
     * the SERVER_PORT environment variable, and finally to 8080.
     * Addresses blocker-14 and blocker-15.
     */
    private int resolveServerPort() {
        // 1. Try SSM Parameter Store
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(SERVER_PORT_PARAM)
                    .withDecryption(false)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            int port = Integer.parseInt(response.parameter().value().trim());
            System.out.println("Server port resolved from SSM Parameter Store: " + port);
            return port;
        } catch (Exception e) {
            System.err.println("Warning: Could not retrieve server port from SSM ("
                    + e.getMessage() + "). Checking environment variable.");
        }

        // 2. Fall back to SERVER_PORT environment variable
        String envPort = System.getenv("SERVER_PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                int port = Integer.parseInt(envPort.trim());
                System.out.println("Server port resolved from environment variable: " + port);
                return port;
            } catch (NumberFormatException nfe) {
                System.err.println("Warning: Invalid SERVER_PORT env var value: " + envPort);
            }
        }

        // 3. Default
        System.out.println("Using default server port: 8080");
        return 8080;
    }
}
