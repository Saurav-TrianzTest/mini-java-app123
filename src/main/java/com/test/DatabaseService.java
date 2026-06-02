package com.test;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service updated for Azure cloud readiness.
 * - Uses Azure Key Vault for secrets
 * - Uses environment variables / Azure App Configuration for host/port
 * - Uses HikariCP for connection pooling
 */
public class DatabaseService {

    // Azure Key Vault configuration (externalized)
    private static final String KEY_VAULT_URL = System.getenv().getOrDefault(
            "AZURE_KEY_VAULT_URL",
            "https://your-key-vault-name.vault.azure.net/"
    );

    // Secret names in Key Vault
    private static final String SECRET_DB_URL = System.getenv().getOrDefault(
            "DB_URL_SECRET_NAME",
            "mini-app-db-url"
    );
    private static final String SECRET_DB_USERNAME = System.getenv().getOrDefault(
            "DB_USERNAME_SECRET_NAME",
            "mini-app-db-username"
    );
    private static final String SECRET_DB_PASSWORD = System.getenv().getOrDefault(
            "DB_PASSWORD_SECRET_NAME",
            "mini-app-db-password"
    );

    // Cache and external service configuration via environment variables / Azure App Configuration
    private static final String REDIS_HOST = System.getenv().getOrDefault("CACHE_REDIS_HOST", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("CACHE_REDIS_PORT", "6379"));

    private static final String EXTERNAL_API_URL = System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com/v1");
    private static final String PAYMENT_SERVICE_URL = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "https://payment.service/process");

    // HikariCP configuration via environment variables / Azure App Configuration
    private static final int MAX_POOL_SIZE = Integer.parseInt(System.getenv().getOrDefault("DB_MAX_POOL_SIZE", "10"));
    private static final long CONNECTION_TIMEOUT_MS = Long.parseLong(System.getenv().getOrDefault("DB_CONNECTION_TIMEOUT_MS", "30000"));

    private final HikariDataSource dataSource;
    private final SecretClient secretClient;

    public DatabaseService() {
        // Initialize Azure Key Vault client using Managed Identity / DefaultAzureCredential
        this.secretClient = new SecretClientBuilder()
                .vaultUrl(KEY_VAULT_URL)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        // Resolve secrets from Key Vault
        String dbUrl = secretClient.getSecret(SECRET_DB_URL).getValue();
        String dbUsername = secretClient.getSecret(SECRET_DB_USERNAME).getValue();
        String dbPassword = secretClient.getSecret(SECRET_DB_PASSWORD).getValue();

        // Configure HikariCP connection pool
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);

        this.dataSource = new HikariDataSource(config);
    }

    public void connect() {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("Obtained connection from HikariCP pool");

            // Simulate cache and external service initialization using externalized configuration
            connectToCache();
            initializeExternalServices();

        } catch (SQLException e) {
            System.err.println("Failed to obtain database connection from pool: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // Cache connection details are externalized via environment variables / Azure App Configuration
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

            // Use query timeout from environment / configuration, defaulting to 30 seconds
            int queryTimeoutSeconds = Integer.parseInt(System.getenv().getOrDefault("DB_QUERY_TIMEOUT_SECONDS", "30"));
            stmt.setQueryTimeout(queryTimeoutSeconds);

            System.out.println("Executing query: " + sql);
            stmt.execute();
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP data source closed");
        }
    }
}