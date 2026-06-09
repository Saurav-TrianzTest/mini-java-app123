package com.test;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Mini Java Application — cloud-ready version.
 *
 * Fixes applied:
 *  - blocker-1,2,3   : Hard-coded File Paths
 *                       → Absolute paths replaced; config/log objects stored in Amazon S3.
 *  - blocker-4,5,6,7 : java.io.File Usage for Data Storage
 *                       → All java.io.File operations replaced with Amazon S3 SDK v2 calls.
 *  - blocker-14,15   : Hard-coded Ports
 *                       → SERVER_PORT read from environment variable SERVER_PORT
 *                         (injected at runtime via ECS/EKS from AWS SSM Parameter Store).
 *  - blocker-20      : Properties Files in Classpath
 *                       → Configuration loaded from AWS Systems Manager Parameter Store
 *                         instead of a classpath-bundled .properties file.
 */
public class MiniApp {

    // -----------------------------------------------------------------------
    // blocker-14: Hard-coded port replaced with environment variable injection.
    // The value is set at deployment time via ECS task definition / EKS env
    // referencing an AWS SSM Parameter Store entry (e.g. /mini-app/server-port).
    // -----------------------------------------------------------------------
    private static final int SERVER_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    // -----------------------------------------------------------------------
    // S3 configuration — sourced from environment variables so no path is
    // hard-coded in the binary (blocker-1, blocker-2, blocker-3).
    // -----------------------------------------------------------------------
    private static final String S3_BUCKET =
            System.getenv().getOrDefault("APP_S3_BUCKET", "mini-app-bucket");
    private static final String S3_CONFIG_KEY =
            System.getenv().getOrDefault("APP_S3_CONFIG_KEY", "config/app.properties");
    private static final String S3_LOG_KEY_PREFIX =
            System.getenv().getOrDefault("APP_S3_LOG_KEY_PREFIX", "logs/mini-app");
    private static final String AWS_REGION =
            System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    // SSM Parameter Store path prefix for application configuration (blocker-20)
    private static final String SSM_PARAM_PREFIX =
            System.getenv().getOrDefault("SSM_PARAM_PREFIX", "/mini-app");

    // -----------------------------------------------------------------------
    // AWS clients
    // -----------------------------------------------------------------------
    private final S3Client s3Client;
    private final SsmClient ssmClient;

    public MiniApp() {
        this.s3Client = S3Client.builder()
                .region(Region.of(AWS_REGION))
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(5))
                        .socketTimeout(Duration.ofSeconds(10)))
                .build();

        this.ssmClient = SsmClient.builder()
                .region(Region.of(AWS_REGION))
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(5))
                        .socketTimeout(Duration.ofSeconds(10)))
                .build();
    }

    public static void main(String[] args) {
        System.out.println("Starting Mini Java Application...");

        MiniApp app = new MiniApp();
        app.initializeApplication();
        app.startServer();
    }

    private void initializeApplication() {
        // Load configuration from AWS SSM Parameter Store (blocker-20)
        // and fall back to S3 for file-based config (blocker-1, blocker-4)
        loadConfiguration();

        // Initialize logging via S3 (blocker-2, blocker-5)
        initializeLogging();

        // Initialize database connection
        DatabaseService dbService = new DatabaseService();
        dbService.connect();
    }

    /**
     * Loads application configuration from AWS Systems Manager Parameter Store.
     * Falls back to reading a properties object from Amazon S3 if SSM is unavailable.
     *
     * Fixes:
     *  - blocker-1  : Hard-coded file path /opt/app/config/app.properties
     *  - blocker-4  : java.io.File usage for config loading
     *  - blocker-20 : Properties file in classpath replaced with SSM Parameter Store
     */
    private void loadConfiguration() {
        System.out.println("Loading configuration from AWS SSM Parameter Store...");
        Properties props = new Properties();

        // Primary: load each known parameter from SSM Parameter Store (blocker-20)
        String[] paramNames = {
            SSM_PARAM_PREFIX + "/server-port",
            SSM_PARAM_PREFIX + "/db-host",
            SSM_PARAM_PREFIX + "/db-name",
            SSM_PARAM_PREFIX + "/redis-host"
        };

        boolean ssmSuccess = false;
        for (String paramName : paramNames) {
            try {
                GetParameterRequest request = GetParameterRequest.builder()
                        .name(paramName)
                        .withDecryption(true)
                        .build();
                GetParameterResponse response = ssmClient.getParameter(request);
                String shortKey = paramName.substring(paramName.lastIndexOf('/') + 1);
                props.setProperty(shortKey, response.parameter().value());
                ssmSuccess = true;
            } catch (Exception e) {
                System.err.println("SSM parameter not found: " + paramName + " — " + e.getMessage());
            }
        }

        if (ssmSuccess) {
            System.out.println("Configuration loaded from AWS SSM Parameter Store.");
            return;
        }

        // Fallback: load properties object from Amazon S3 (blocker-1, blocker-4)
        // replaces: new File(CONFIG_FILE_PATH) / new FileInputStream(configFile)
        System.out.println("Falling back to S3 configuration: s3://" + S3_BUCKET + "/" + S3_CONFIG_KEY);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(S3_CONFIG_KEY)
                    .build();
            try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest)) {
                props.load(s3Object);
                System.out.println("Configuration loaded from S3: s3://" + S3_BUCKET + "/" + S3_CONFIG_KEY);
            }
        } catch (NoSuchKeyException e) {
            System.out.println("Warning: Configuration object not found in S3 at key: " + S3_CONFIG_KEY);
        } catch (IOException e) {
            System.err.println("Failed to load configuration from S3: " + e.getMessage());
        }
    }

    /**
     * Initializes application logging by writing a startup marker to Amazon S3.
     *
     * Fixes:
     *  - blocker-2  : Hard-coded file path /var/log/mini-app.log
     *  - blocker-3  : Hard-coded file path /var/log directory
     *  - blocker-5  : java.io.File usage for log directory creation
     *  - blocker-6  : java.io.File usage for log file creation
     *  - blocker-7  : java.io.File usage for log file existence check
     */
    private void initializeLogging() {
        // blocker-2,3,5,6,7: replace File("/var/log") and File(LOG_FILE_PATH)
        // with an S3 PutObject call — log entries are written to S3 as objects.
        String logKey = S3_LOG_KEY_PREFIX + "/startup.log";
        System.out.println("Initializing logging — writing startup marker to S3: s3://"
                + S3_BUCKET + "/" + logKey);
        try {
            String logContent = "Application started at: " + java.time.Instant.now().toString()
                    + System.lineSeparator();
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(logKey)
                    .contentType("text/plain")
                    .build();
            s3Client.putObject(putRequest,
                    RequestBody.fromBytes(logContent.getBytes(StandardCharsets.UTF_8)));
            System.out.println("Logging initialized. Log marker written to: s3://"
                    + S3_BUCKET + "/" + logKey);
        } catch (Exception e) {
            System.err.println("Failed to initialize S3 logging: " + e.getMessage());
        }
    }

    private void startServer() {
        try {
            // blocker-15: SERVER_PORT is now read from the environment variable SERVER_PORT
            // (injected at runtime via ECS/EKS task definition backed by SSM Parameter Store).
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
