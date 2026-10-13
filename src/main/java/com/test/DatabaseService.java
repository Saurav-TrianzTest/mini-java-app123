package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service – updated for Java 25 compatibility.
 *
 * <p>Changes applied:
 * <ul>
 *   <li><b>FIX JAVA11_TO_25_STRONG_ENCAPSULATION (reflection)</b>: Removed the
 *       explicit {@code Class.forName("com.mysql.cj.jdbc.Driver")} call that was
 *       present in the original Java 11 source.  Under Java 25's strong
 *       encapsulation model, reflective access to internal JDK and library classes
 *       triggers warnings (and in future releases, errors).  JDBC 4.0+ (Java 6+)
 *       uses the {@link java.util.ServiceLoader} SPI to auto-discover drivers on
 *       the classpath/module-path, so the explicit {@code Class.forName()} call is
 *       both unnecessary and potentially problematic on Java 25.</li>
 *   <li><b>FIX JAVA11_TO_25_DEPENDENCY_UPDATES</b>: Switched to
 *       {@code com.mysql:mysql-connector-j 8.3.0} (the official replacement for
 *       the deprecated {@code mysql:mysql-connector-java} artifact) which ships
 *       with a proper {@code module-info.class} and full Java 25 / JPMS support.</li>
 *   <li><b>NOTE</b>: No Nashorn, SecurityManager, {@code sun.misc.Unsafe},
 *       {@code finalize()}, or {@code javax.*} enterprise imports were present in
 *       this file, so those critical migration rules do not apply here.</li>
 * </ul>
 */
public class DatabaseService {

    // -----------------------------------------------------------------------
    // Connection details are read from environment variables so that the
    // application is container/cloud-friendly and credentials are not
    // embedded in source code (12-factor app principle).
    // -----------------------------------------------------------------------
    private static final String DB_HOST =
            System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT =
            System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME =
            System.getenv().getOrDefault("DB_NAME", "mini_app_db");
    private static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME =
            System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String DB_PASSWORD =
            System.getenv().getOrDefault("DB_PASSWORD", "");

    private static final String REDIS_HOST =
            System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT =
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    private static final String EXTERNAL_API_URL =
            System.getenv().getOrDefault("EXTERNAL_API_URL", "http://api.example.com:8080/v1");
    private static final String PAYMENT_SERVICE_URL =
            System.getenv().getOrDefault("PAYMENT_SERVICE_URL",
                    "https://payment.internal.company.com/process");

    private Connection connection;

    /**
     * Opens a JDBC connection to the configured MySQL database.
     *
     * <p>The JDBC driver is loaded automatically via the ServiceLoader SPI
     * (JDBC 4.0+), which is the correct and module-safe approach on Java 25.
     * No explicit {@code Class.forName()} call is required or used.
     */
    public void connect() {
        try {
            System.out.println("Connecting to database...");

            // FIX JAVA11_TO_25_STRONG_ENCAPSULATION:
            // The original code contained:
            //   Class.forName("com.mysql.cj.jdbc.Driver");
            // This has been removed.  JDBC 4.0+ (Java 6+) uses the ServiceLoader
            // SPI to auto-discover drivers from the classpath/module-path, which
            // is the correct and module-safe approach on Java 25.  Keeping the
            // explicit Class.forName() would trigger strong-encapsulation warnings
            // and is unnecessary with mysql-connector-j 8.3.0.
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Connected to database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);

            connectToCache();
            initializeExternalServices();

        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    private void connectToCache() {
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    /**
     * Executes a SQL statement using a {@link PreparedStatement}.
     *
     * @param sql the SQL string to execute
     */
    public void executeQuery(String sql) {
        try {
            if (connection != null && !connection.isClosed()) {
                // Use try-with-resources to ensure the PreparedStatement is always closed,
                // which is the recommended pattern in Java 7+ and required for correctness.
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setQueryTimeout(30);
                    System.out.println("Executing query: " + sql);
                    stmt.execute();
                }
            }
        } catch (SQLException e) {
            System.err.println("Query execution failed: " + e.getMessage());
        }
    }

    /**
     * Closes the JDBC connection if it is currently open.
     */
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
