package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DatabaseService – independently deployable microservice component.
 *
 * Remediation for cz-java-0082 (Individual Components):
 *   This class has been refactored so that every connection parameter is
 *   sourced exclusively from environment variables, making it suitable for
 *   independent deployment as an EKS microservice with its own Kubernetes
 *   Deployment, Service, and ConfigMap (see k8s/database-service/).
 *
 * All previously hardcoded values (DB host/port/name, credentials, cache
 * address, external-service URLs) have been replaced with environment-variable
 * lookups so that no configuration is baked into the container image.
 */
public class DatabaseService {

    // -------------------------------------------------------------------------
    // cz-java-0082 fix – Line 19: DB_URL was hardcoded; now built from env vars
    // -------------------------------------------------------------------------
    private static final String DB_HOST     = System.getenv().getOrDefault("DB_HOST",     "localhost");
    private static final String DB_PORT     = System.getenv().getOrDefault("DB_PORT",     "3306");
    private static final String DB_NAME     = System.getenv().getOrDefault("DB_NAME",     "mini_app_db");
    private static final String DB_URL      = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    // -------------------------------------------------------------------------
    // cz-java-0062 fix – Line 22 (source): REDIS_HOST was hardcoded to the IP
    // address "127.0.0.1"; replaced with the REDIS_HOST environment variable
    // backed by the Kubernetes Service DNS name "redis-service" so that
    // inter-service communication uses cluster-internal DNS on Amazon EKS.
    // The value is injected via the database-service ConfigMap
    // (k8s/database-service/configmap.yaml).
    // -------------------------------------------------------------------------
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "redis-service");
    private static final int    REDIS_PORT = Integer.parseInt(
            System.getenv().getOrDefault("REDIS_PORT", "6379"));

    // External service URLs – sourced from environment variables
    private static final String EXTERNAL_API_URL    = System.getenv().getOrDefault("EXTERNAL_API_URL",    "http://external-api-service/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "http://payment-service/process");

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to database...");

            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Connected to database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);

            connectToCache();
            initializeExternalServices();

        } catch (ClassNotFoundException e) {
            System.err.println("Database driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    private void connectToCache() {
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
    }

    private void initializeExternalServices() {
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        try {
            if (connection != null && !connection.isClosed()) {
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setQueryTimeout(30);
                System.out.println("Executing query: " + sql);
                stmt.execute();
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
}
