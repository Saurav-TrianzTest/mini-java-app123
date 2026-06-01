package com.test;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Mini Java Application — cloud-ready version.
 *
 * Fixes applied:
 *  - blocker-1,2,3   : Hard-coded file paths replaced with Amazon S3 object keys
 *  - blocker-4,5,6,7 : java.io.File operations replaced with Amazon S3 SDK v2 calls
 *  - blocker-14,15   : Hard-coded SERVER_PORT replaced with environment variable / SSM
 *  - blocker-20      : Classpath properties file replaced with AWS SSM Parameter Store
 */
public class MiniApp {

    // -----------------------------------------------------------------------
    // AWS region — injected via environment variable (12-factor)
    // -----------------------------------------------------------------------
    private static final Region AWS_REGION =
            Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

    // -----------------------------------------------------------------------
    // blocker-14, blocker-15 : Hard-coded port replaced with env var / SSM
    // -----------------------------------------------------------------------
    /**
     * Server port resolved at runtime from the environment variable SERVER_PORT,
     * falling back to the SSM Parameter Store path /mini-app/server/port,
     * and ultimately defaulting to 8080.
     */
    private static final int SERVER_PORT = resolveServerPort();

    // -----------------------------------------------------------------------
    // blocker-1,2,3 : S3 bucket / object-key constants replace absolute paths
    // -----------------------------------------------------------------------
    /**
     * S3 bucket name injected via environment variable APP_S3_BUCKET.
     * Replaces the hard-coded host-level directory /opt/app/config.
     */
    private static final String APP_S3_BUCKET =
            System.getenv().getOrDefault("APP_S3_BUCKET", "mini-app-storage");

    /**
     * S3 object key for the application configuration file.
     * Replaces the hard-coded path /opt/app/config/app.properties (blocker-1).
     */
    private static final String CONFIG_S3_KEY =
            System.getenv().getOrDefault("CONFIG_S3_KEY", "config/app.properties");

    /**
     * S3 object key prefix for log entries.
     * Replaces the hard-coded path /var/log/mini-app.log (blocker-2, blocker-3).
     */
    private static final String LOG_S3_KEY_PREFIX =
            System.getenv().getOrDefault("LOG_S3_KEY_PREFIX", "logs/mini-app");

    // -----------------------------------------------------------------------
    // blocker-20 : SSM Parameter Store path prefix for app configuration
    // -----------------------------------------------------------------------
    private static final String SSM_CONFIG_PATH =
            System.getenv().getOrDefault("SSM_CONFIG_PATH", "/mini-app/config");

    // -----------------------------------------------------------------------
    // AWS clients
    // -----------------------------------------------------------------------
    private final S3Client s3Client;
    private final SsmClient ssmClient;

    public MiniApp() {
        // blocker-1..7 : S3 client for file operations
        this.s3Client = S3Client.builder()
                .region(AWS_REGION)
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .apiCallAttemptTimeout(Duration.ofSeconds(10)))
                .build();

        // blocker-20 : SSM client for externalised configuration
        this.ssmClient = SsmClient.builder()
                .region(AWS_REGION)
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5)))
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

    /**
     * blocker-1, blocker-4, blocker-20:
     * Load application configuration from AWS SSM Parameter Store (primary)
     * and fall back to reading the properties file from Amazon S3.
     * Replaces the previous java.io.File read from /opt/app/config/app.properties.
     */
    private void loadConfiguration() {
        // Primary: load from SSM Parameter Store (blocker-20)
        try {
            GetParametersByPathRequest ssmRequest = GetParametersByPathRequest.builder()
                    .path(SSM_CONFIG_PATH)
                    .recursive(true)
                    .withDecryption(true)
                    .build();

            GetParametersByPathResponse ssmResponse = ssmClient.getParametersByPath(ssmRequest);
            Properties props = new Properties();
            for (Parameter param : ssmResponse.parameters()) {
                // Strip the path prefix to get the property key
                String key = param.name().replace(SSM_CONFIG_PATH + "/", "");
                props.setProperty(key, param.value());
            }

            if (!props.isEmpty()) {
                System.out.println("Configuration loaded from AWS SSM Parameter Store path: "
                        + SSM_CONFIG_PATH);
                return;
            }
        } catch (Exception e) {
            System.err.println("SSM Parameter Store unavailable, falling back to S3: "
                    + e.getMessage());
        }

        // Fallback: load properties file from Amazon S3 (blocker-1, blocker-4)
        // Replaces: new File(CONFIG_FILE_PATH) / new FileInputStream(configFile)
        try {
            GetObjectRequest s3Request = GetObjectRequest.builder()
                    .bucket(APP_S3_BUCKET)
                    .key(CONFIG_S3_KEY)
                    .build();

            try (ResponseInputStream<GetObjectResponse> s3Object =
                         s3Client.getObject(s3Request)) {

                Properties props = new Properties();
                props.load(s3Object);
                System.out.println("Configuration loaded from S3: s3://"
                        + APP_S3_BUCKET + "/" + CONFIG_S3_KEY);
            }

        } catch (NoSuchKeyException e) {
            System.out.println("Warning: Configuration object not found in S3 at: s3://"
                    + APP_S3_BUCKET + "/" + CONFIG_S3_KEY);
        } catch (IOException e) {
            System.err.println("Failed to load configuration from S3: " + e.getMessage());
        }
    }

    /**
     * blocker-2, blocker-3, blocker-5, blocker-6, blocker-7:
     * Initialise logging by writing a log entry to Amazon S3.
     * Replaces the previous java.io.File operations on /var/log/mini-app.log.
     */
    private void initializeLogging() {
        // Replaces: new File("/var/log").mkdirs() and new File(LOG_FILE_PATH).createNewFile()
        // Instead, write an initialisation marker object to S3 (blocker-2,3,5,6,7)
        try {
            String logKey = LOG_S3_KEY_PREFIX + "/init.log";
            String logContent = "Logging initialised at: "
                    + java.time.Instant.now().toString() + System.lineSeparator();

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(APP_S3_BUCKET)
                    .key(logKey)
                    .contentType("text/plain")
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromBytes(logContent.getBytes(StandardCharsets.UTF_8)));

            System.out.println("Logging initialised. Log object written to S3: s3://"
                    + APP_S3_BUCKET + "/" + logKey);

        } catch (Exception e) {
            System.err.println("Failed to initialise logging in S3: " + e.getMessage());
        }
    }

    /**
     * blocker-14, blocker-15:
     * Start the server on a port resolved from the environment / SSM Parameter Store.
     * Replaces the previous hard-coded SERVER_PORT = 8080.
     */
    private void startServer() {
        try {
            // SERVER_PORT is already resolved via resolveServerPort() at class load time
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

    // -----------------------------------------------------------------------
    // Static helpers
    // -----------------------------------------------------------------------

    /**
     * blocker-14, blocker-15:
     * Resolve the server port at startup using the following priority:
     *  1. Environment variable SERVER_PORT
     *  2. AWS SSM Parameter Store /mini-app/server/port
     *  3. Default value 8080
     */
    private static int resolveServerPort() {
        // 1. Environment variable (highest priority — 12-factor)
        String envPort = System.getenv("SERVER_PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException ignored) {
                System.err.println("Invalid SERVER_PORT env var value: " + envPort);
            }
        }

        // 2. AWS SSM Parameter Store
        try {
            SsmClient ssm = SsmClient.builder()
                    .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                    .build();
            GetParameterRequest req = GetParameterRequest.builder()
                    .name("/mini-app/server/port")
                    .withDecryption(false)
                    .build();
            GetParameterResponse resp = ssm.getParameter(req);
            ssm.close();
            return Integer.parseInt(resp.parameter().value());
        } catch (Exception e) {
            System.err.println("Could not fetch server port from SSM, using default 8080: "
                    + e.getMessage());
        }

        // 3. Default
        return 8080;
    }
}
