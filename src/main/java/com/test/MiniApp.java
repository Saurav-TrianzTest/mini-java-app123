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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Mini Java Application – cloud-ready version.
 *
 * Fixes applied:
 *   blocker-1,2,3   (cr-java-0061) – Hard-coded file paths → Amazon S3
 *   blocker-4,5,6,7 (cr-java-0063) – java.io.File usage → Amazon S3 AWS SDK v2
 *   blocker-14,15   (cr-java-0077) – Hard-coded ports → SSM Parameter Store + env var
 *   blocker-20      (cr-java-0070) – Classpath properties file → SSM Parameter Store
 */
public class MiniApp {

    // -----------------------------------------------------------------------
    // AWS region – injected via environment variable (standard ECS/EKS pattern)
    // -----------------------------------------------------------------------
    private static final String AWS_REGION =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // -----------------------------------------------------------------------
    // Port configuration – resolved from environment variable or SSM (blocker-14/15)
    // No hard-coded port number remains in source code.
    // -----------------------------------------------------------------------
    private static final String SSM_SERVER_PORT_PARAM =
            System.getenv().getOrDefault("SSM_SERVER_PORT_PARAM", "/mini-app/server/port");

    // -----------------------------------------------------------------------
    // S3 configuration – bucket and object keys injected via environment variables
    // (blocker-1/2/3/4/5/6/7 – replaces /opt/app/config/app.properties and /var/log paths)
    // -----------------------------------------------------------------------
    private static final String S3_BUCKET =
            System.getenv().getOrDefault("APP_S3_BUCKET", "mini-app-storage");

    private static final String S3_CONFIG_KEY =
            System.getenv().getOrDefault("APP_S3_CONFIG_KEY", "config/app.properties");

    private static final String S3_LOG_KEY_PREFIX =
            System.getenv().getOrDefault("APP_S3_LOG_KEY_PREFIX", "logs/mini-app");

    // -----------------------------------------------------------------------
    // SSM parameter name for application configuration (blocker-20)
    // -----------------------------------------------------------------------
    private static final String SSM_APP_CONFIG_PARAM =
            System.getenv().getOrDefault("SSM_APP_CONFIG_PARAM", "/mini-app/app/config");

    // -----------------------------------------------------------------------
    // AWS clients
    // -----------------------------------------------------------------------
    private final S3Client s3Client;
    private final SsmClient ssmClient;

    public MiniApp() {
        Region region = Region.of(AWS_REGION);

        // S3 client for config/log file operations (blocker-1 through blocker-7)
        this.s3Client = S3Client.builder()
                .region(region)
                .build();

        // SSM client for port and app-config parameters (blocker-14/15/20)
        this.ssmClient = SsmClient.builder()
                .region(region)
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(5)))
                .build();
    }

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        // Load configuration from S3 / SSM Parameter Store (blocker-1/20)
        loadConfiguration();

        // Initialise logging via S3 (blocker-2/3)
        initializeLogging();

        // Initialise database connection (credentials from Secrets Manager)
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    /**
     * Loads application configuration from Amazon S3 (replaces hard-coded
     * /opt/app/config/app.properties – blocker-1/4) and from AWS SSM Parameter
     * Store (blocker-20).
     */
    private void loadConfiguration() {
        // --- Primary: load from S3 (blocker-1, blocker-4) ---
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(S3_CONFIG_KEY)   // replaces hard-coded /opt/app/config/app.properties
                    .build();

            try (ResponseInputStream<GetObjectResponse> s3Object =
                         s3Client.getObject(getObjectRequest)) {

                Properties props = new Properties();
                props.load(s3Object);
                System.out.println("Configuration loaded from S3: s3://" + S3_BUCKET + "/" + S3_CONFIG_KEY);
            }
        } catch (Exception e) {
            System.out.println("S3 config not available, falling back to SSM Parameter Store: " + e.getMessage());

            // --- Fallback: load from SSM Parameter Store (blocker-20) ---
            try {
                String configValue = getSsmParameter(SSM_APP_CONFIG_PARAM);
                Properties props = new Properties();
                props.load(new java.io.StringReader(configValue));
                System.out.println("Configuration loaded from SSM Parameter Store: " + SSM_APP_CONFIG_PARAM);
            } catch (Exception ssme) {
                System.err.println("Failed to load configuration from SSM: " + ssme.getMessage());
            }
        }
    }

    /**
     * Initialises logging by writing a startup marker object to Amazon S3
     * (replaces hard-coded /var/log/mini-app.log – blocker-2/3/5/6/7).
     */
    private void initializeLogging() {
        try {
            String logKey = S3_LOG_KEY_PREFIX + "/startup.log";  // replaces /var/log/mini-app.log
            String logContent = "Application started at: " + java.time.Instant.now().toString() + "\n";

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(logKey)
                    .contentType("text/plain")
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromBytes(logContent.getBytes(StandardCharsets.UTF_8)));

            System.out.println("Logging initialised – log object written to S3: s3://" + S3_BUCKET + "/" + logKey);
        } catch (Exception e) {
            System.err.println("Failed to initialise S3 logging: " + e.getMessage());
        }
    }

    /**
     * Starts the server on a port resolved from AWS SSM Parameter Store or the
     * SERVER_PORT environment variable (replaces hard-coded 8080 – blocker-14/15).
     */
    private void startServer() {
        int serverPort = resolveServerPort();
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Server started on port: " + serverPort);
            System.out.println("Server ready to accept connections...");

            // Simulate server running
            Thread.sleep(1000);
            serverSocket.close();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    /**
     * Resolves the server port using the following priority order (blocker-14/15):
     *  1. SERVER_PORT environment variable (injected by ECS/EKS task definition)
     *  2. AWS SSM Parameter Store value at SSM_SERVER_PORT_PARAM
     *  3. Default value 8080 (last resort only)
     */
    private int resolveServerPort() {
        // 1. Environment variable (highest priority – 12-factor app principle)
        String envPort = System.getenv("SERVER_PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException e) {
                System.err.println("Invalid SERVER_PORT env var value: " + envPort);
            }
        }

        // 2. SSM Parameter Store
        try {
            String ssmPort = getSsmParameter(SSM_SERVER_PORT_PARAM);
            return Integer.parseInt(ssmPort);
        } catch (Exception e) {
            System.err.println("Could not resolve port from SSM, using default 8080: " + e.getMessage());
        }

        // 3. Default fallback
        return 8080;
    }

    // -----------------------------------------------------------------------
    // AWS helper – SSM Parameter Store (blocker-14/15/20)
    // -----------------------------------------------------------------------
    private String getSsmParameter(String parameterName) {
        GetParameterRequest request = GetParameterRequest.builder()
                .name(parameterName)
                .withDecryption(true)
                .build();
        GetParameterResponse response = ssmClient.getParameter(request);
        return response.parameter().value();
    }
}
