package com.test;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service updated for cloud readiness with Azure Key Vault, environment-based configuration,
 * and HikariCP connection pooling.
 */
public class DatabaseService {

    // Azure Key Vault configuration - vault URL should come from environment or Azure App Configuration
    private static final String KEY_VAULT_URL = System.getenv().getOrDefault(
            "KEY_VAULT_URL",
            "https://your-key-vault-name.vault.azure.net/");

    // Secret names in Key Vault for DB connection details
    private static final String SECRET_DB_URL = System.getenv().getOrDefault(
            "DB_URL_SECRET_NAME",
            "db-connection-url");
    private static final String SECRET_DB_USERNAME = System.getenv().getOrDefault(
            "DB_USERNAME_SECRET_NAME",
            "db-username");
    private static final String SECRET_DB_PASSWORD = System.getenv().getOrDefault(
            "DB_PASSWORD_SECRET_NAME",
            "db-password");

    // Environment-based configuration for cache and external services (can be sourced from Azure App Configuration)
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault(
            "EXTERNAL_API_URL",
            "http://api.example.com/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault(
            "PAYMENT_SERVICE_URL",
            "https://payment.internal.company.com/process");

    // HikariCP DataSource for connection pooling
    private DataSource dataSource;

    public DatabaseService() {
        initializeDataSource();
    }

    private void initializeDataSource() {
        // Build SecretClient using DefaultAzureCredential (supports Managed Identity in Azure)
        SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(KEY_VAULT_URL)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        // Retrieve secrets from Azure Key Vault
        String dbUrl = secretClient.getSecret(SECRET_DB_URL).getValue();
        String dbUsername = secretClient.getSecret(SECRET_DB_USERNAME).getValue();
        String dbPassword = secretClient.getSecret(SECRET_DB_PASSWORD).getValue();

        // Configure HikariCP with Azure SQL / MySQL JDBC URL and credentials
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);

        // Reasonable defaults for cloud environments
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000);      // 10 minutes
        config.setMaxLifetime(1800000);     // 30 minutes

        this.dataSource = new HikariDataSource(config);

        System.out.println("HikariCP DataSource initialized for DB URL from Key Vault secret: " + SECRET_DB_URL);

        // Initialize other external dependencies using environment-based configuration
        connectToCache();
        initializeExternalServices();
    }

    public void connect() {
        // Connection acquisition is handled via HikariCP; this method is kept for backward compatibility
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("Successfully obtained a connection from HikariCP pool.");
        } catch (SQLException e) {
            System.err.println("Failed to obtain database connection from pool: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // Cache connection details are externalized via environment variables
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // External service URLs are externalized via environment variables / Azure App Configuration
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            // Use a configurable query timeout; default to 30 seconds if not provided
            int queryTimeoutSeconds = Integer.parseInt(
                    System.getenv().getOrDefault("DB_QUERY_TIMEOUT_SECONDS", "30"));
            stmt.setQueryTimeout(queryTimeoutSeconds);

            System.out.println("Executing query with timeout " + queryTimeoutSeconds + " seconds: " + sql);
            stmt.execute();
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        // With HikariCP and try-with-resources, individual connections are returned to the pool automatically.
        // If the DataSource needs to be shut down (e.g., on application shutdown), it can be closed if it's a HikariDataSource.
        if (this.dataSource instanceof HikariDataSource) {
            ((HikariDataSource) this.dataSource).close();
            System.out.println("HikariCP DataSource closed");
        }
    }
}