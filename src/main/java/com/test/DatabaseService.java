package com.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Database service with hardcoded connection details – intentional containerization blockers.
 *
 * <h2>Java 25 Upgrade Notes</h2>
 * <ul>
 *   <li><strong>Removed explicit {@code Class.forName("com.mysql.cj.jdbc.Driver")} call
 *       (JDBC 3 pattern):</strong>
 *       Since Java 6 / JDBC 4.0 the JDBC driver is auto-discovered via the
 *       {@link java.util.ServiceLoader} mechanism
 *       ({@code META-INF/services/java.sql.Driver}).  Keeping the manual
 *       {@code Class.forName} call is harmless but unnecessary and can mask
 *       classpath / module-path issues on newer runtimes.  The updated
 *       {@code com.mysql:mysql-connector-j:9.1.0} artifact fully supports
 *       Java 17/25 and registers its driver automatically.
 *   </li>
 *   <li><strong>Dependency updated:</strong> The deprecated
 *       {@code mysql:mysql-connector-java} artifact (groupId {@code mysql}) has
 *       been replaced with {@code com.mysql:mysql-connector-j:9.1.0} in
 *       {@code pom.xml}.  The new artifact supports Java 17+ and JDBC 4.3.
 *   </li>
 *   <li><strong>No javax.* → jakarta.* migration required</strong> in this file
 *       because it only uses core {@code java.sql} APIs which remain in the JDK.
 *   </li>
 * </ul>
 */
public class DatabaseService {

    // BLOCKER: Hardcoded database connection details
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "mini_app_db";
    private static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "password123";

    // BLOCKER: Hardcoded cache server details
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;

    // BLOCKER: Hardcoded API endpoints
    private static final String EXTERNAL_API_URL = "http://api.example.com:8080/v1";
    private static final String PAYMENT_SERVICE_URL =
            "https://payment.internal.company.com/process";

    private Connection connection;

    public void connect() {
        try {
            System.out.println("Connecting to database...");

            /*
             * Java 25 fix: removed explicit Class.forName("com.mysql.cj.jdbc.Driver").
             *
             * JDBC 4.0 (Java 6+) introduced automatic driver discovery via
             * ServiceLoader.  The com.mysql:mysql-connector-j:9.1.0 JAR ships a
             * META-INF/services/java.sql.Driver file that registers
             * com.mysql.cj.jdbc.Driver automatically when the JAR is on the
             * classpath (or module path).  The manual Class.forName call is
             * therefore redundant and has been removed to avoid potential
             * ClassNotFoundException issues when running on the module path in
             * Java 17/25.
             */

            // BLOCKER: Hardcoded connection string and credentials
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Connected to database: " + DB_URL);
            System.out.println("Using username: " + DB_USERNAME);

            // BLOCKER: Hardcoded cache connection
            connectToCache();

            // BLOCKER: Hardcoded external service URLs
            initializeExternalServices();

        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    private void connectToCache() {
        // BLOCKER: Hardcoded Redis connection details
        System.out.println("Connecting to Redis cache at: " + REDIS_HOST + ":" + REDIS_PORT);
        // Simulate cache connection
    }

    private void initializeExternalServices() {
        // BLOCKER: Hardcoded external service URLs
        System.out.println("Initializing external API: " + EXTERNAL_API_URL);
        System.out.println("Initializing payment service: " + PAYMENT_SERVICE_URL);
    }

    public void executeQuery(String sql) {
        try {
            if (connection != null && !connection.isClosed()) {
                PreparedStatement stmt = connection.prepareStatement(sql);
                // BLOCKER: Hardcoded query timeout
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
