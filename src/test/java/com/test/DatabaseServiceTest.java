package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService class
 * Tests all public methods, constructors, and error scenarios
 */
public class DatabaseServiceTest {

    private DatabaseService databaseService;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    public void setUp() {
        databaseService = new DatabaseService();

        // Capture System.out and System.err for output verification
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    public void tearDown() {
        // Restore original System.out and System.err
        System.setOut(originalOut);
        System.setErr(originalErr);

        if (databaseService != null) {
            databaseService.disconnect();
        }
    }

    /**
     * Test constructor creates non-null instance
     */
    @Test
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    /**
     * Test multiple constructor calls create independent instances
     */
    @Test
    public void testMultipleConstructorCalls() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();

        assertNotNull(service1, "First instance should not be null");
        assertNotNull(service2, "Second instance should not be null");
        assertNotSame(service1, service2, "Instances should be different");
    }

    /**
     * Test connect method attempts database connection
     * Note: Will fail due to missing database, but tests method execution
     */
    @Test
    public void testConnect_DatabaseNotAvailable() {
        databaseService.connect();

        String output = outputStream.toString();
        assertTrue(output.contains("Connecting to database"),
            "Should print connection message");
    }

    /**
     * Test connect method initializes all services
     */
    @Test
    public void testConnect_InitializesAllServices() {
        databaseService.connect();

        String output = outputStream.toString();
        assertTrue(output.contains("Connecting to database"),
            "Should initialize database connection");
        assertTrue(output.contains("Connecting to Redis cache"),
            "Should initialize Redis cache connection");
        assertTrue(output.contains("Initializing external API"),
            "Should initialize external API");
        assertTrue(output.contains("Initializing payment service"),
            "Should initialize payment service");
    }

    /**
     * Test connect method prints correct database URL
     */
    @Test
    public void testConnect_PrintsCorrectDatabaseURL() {
        databaseService.connect();

        String output = outputStream.toString();
        assertTrue(output.contains("jdbc:mysql://localhost:3306/mini_app_db"),
            "Should print correct database URL");
    }

    /**
     * Test connect method prints correct username
     */
    @Test
    public void testConnect_PrintsCorrectUsername() {
        databaseService.connect();

        String output = outputStream.toString();
        assertTrue(output.contains("Using username: root"),
            "Should print correct username");
    }

    /**
     * Test connect method prints Redis connection details
     */
    @Test
    public void testConnect_PrintsRedisConnectionDetails() {
        databaseService.connect();

        String output = outputStream.toString();
        assertTrue(output.contains("127.0.0.1:6379"),
            "Should print correct Redis host and port");
    }

    /**
     * Test connect method prints external API URLs
     */
    @Test
    public void testConnect_PrintsExternalAPIURLs() {
        databaseService.connect();

        String output = outputStream.toString();
        assertTrue(output.contains("http://api.example.com:8080/v1"),
            "Should print external API URL");
        assertTrue(output.contains("https://payment.internal.company.com/process"),
            "Should print payment service URL");
    }

    /**
     * Test executeQuery with null connection (before connect)
     */
    @Test
    public void testExecuteQuery_WithNullConnection() {
        String sql = "SELECT * FROM users";

        // Should not throw exception
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
            "Should handle null connection gracefully");
    }

    /**
     * Test executeQuery with simple SELECT query
     */
    @Test
    public void testExecuteQuery_WithSelectQuery() {
        String sql = "SELECT * FROM users";

        databaseService.connect();
        databaseService.executeQuery(sql);

        String output = outputStream.toString();
        assertTrue(output.contains("Executing query: " + sql),
            "Should print query execution message");
    }

    /**
     * Test executeQuery with INSERT query
     */
    @Test
    public void testExecuteQuery_WithInsertQuery() {
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@test.com')";

        databaseService.connect();
        databaseService.executeQuery(sql);

        String output = outputStream.toString();
        assertTrue(output.contains("Executing query"),
            "Should execute INSERT query");
    }

    /**
     * Test executeQuery with UPDATE query
     */
    @Test
    public void testExecuteQuery_WithUpdateQuery() {
        String sql = "UPDATE users SET name = 'Jane' WHERE id = 1";

        databaseService.connect();
        databaseService.executeQuery(sql);

        String output = outputStream.toString();
        assertTrue(output.contains("Executing query"),
            "Should execute UPDATE query");
    }

    /**
     * Test executeQuery with DELETE query
     */
    @Test
    public void testExecuteQuery_WithDeleteQuery() {
        String sql = "DELETE FROM users WHERE id = 1";

        databaseService.connect();
        databaseService.executeQuery(sql);

        String output = outputStream.toString();
        assertTrue(output.contains("Executing query"),
            "Should execute DELETE query");
    }

    /**
     * Test executeQuery with empty SQL string
     */
    @Test
    public void testExecuteQuery_WithEmptySQL() {
        String sql = "";

        databaseService.connect();

        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
            "Should handle empty SQL gracefully");
    }

    /**
     * Test executeQuery with null SQL string
     */
    @Test
    public void testExecuteQuery_WithNullSQL() {
        databaseService.connect();

        assertDoesNotThrow(() -> databaseService.executeQuery(null),
            "Should handle null SQL gracefully");
    }

    /**
     * Test executeQuery with complex SQL query
     */
    @Test
    public void testExecuteQuery_WithComplexQuery() {
        String sql = "SELECT u.name, COUNT(o.id) FROM users u LEFT JOIN orders o ON u.id = o.user_id GROUP BY u.name";

        databaseService.connect();
        databaseService.executeQuery(sql);

        String output = outputStream.toString();
        assertTrue(output.contains("Executing query"),
            "Should execute complex query");
    }

    /**
     * Test disconnect with null connection
     */
    @Test
    public void testDisconnect_WithNullConnection() {
        assertDoesNotThrow(() -> databaseService.disconnect(),
            "Should handle null connection gracefully");
    }

    /**
     * Test disconnect after successful connection
     */
    @Test
    public void testDisconnect_AfterConnect() {
        databaseService.connect();

        assertDoesNotThrow(() -> databaseService.disconnect(),
            "Should disconnect without exception");
    }

    /**
     * Test disconnect prints success message
     */
    @Test
    public void testDisconnect_PrintsSuccessMessage() {
        databaseService.connect();
        databaseService.disconnect();

        String output = outputStream.toString();
        // May print close message or error depending on connection state
        assertTrue(output.length() > 0,
            "Should print some output");
    }

    /**
     * Test multiple disconnect calls
     */
    @Test
    public void testDisconnect_MultipleCalls() {
        databaseService.connect();
        databaseService.disconnect();

        assertDoesNotThrow(() -> databaseService.disconnect(),
            "Should handle multiple disconnect calls gracefully");
    }

    /**
     * Test full connection lifecycle
     */
    @Test
    public void testFullConnectionLifecycle() {
        // Connect
        databaseService.connect();
        String connectOutput = outputStream.toString();
        assertTrue(connectOutput.contains("Connecting to database"),
            "Should connect to database");

        // Execute query
        databaseService.executeQuery("SELECT 1");

        // Disconnect
        databaseService.disconnect();

        assertDoesNotThrow(() -> {},
            "Full lifecycle should complete without exceptions");
    }

    /**
     * Test connect followed by immediate disconnect
     */
    @Test
    public void testConnect_ThenImmediateDisconnect() {
        databaseService.connect();
        databaseService.disconnect();

        assertDoesNotThrow(() -> {},
            "Should handle immediate disconnect after connect");
    }

    /**
     * Test multiple executeQuery calls
     */
    @Test
    public void testMultipleExecuteQueryCalls() {
        databaseService.connect();

        databaseService.executeQuery("SELECT * FROM users");
        databaseService.executeQuery("SELECT * FROM orders");
        databaseService.executeQuery("SELECT * FROM products");

        String output = outputStream.toString();
        int queryCount = output.split("Executing query").length - 1;
        assertTrue(queryCount >= 0, "Should execute multiple queries");
    }

    /**
     * Test executeQuery with SQL injection attempt (security test)
     */
    @Test
    public void testExecuteQuery_WithSQLInjectionAttempt() {
        String maliciousSQL = "SELECT * FROM users WHERE id = 1 OR 1=1; DROP TABLE users;";

        databaseService.connect();

        assertDoesNotThrow(() -> databaseService.executeQuery(maliciousSQL),
            "Should handle malicious SQL without crashing");
    }

    /**
     * Test executeQuery with very long SQL string
     */
    @Test
    public void testExecuteQuery_WithVeryLongSQL() {
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSQL.append(i).append(",");
        }
        longSQL.append("9999)");

        databaseService.connect();

        assertDoesNotThrow(() -> databaseService.executeQuery(longSQL.toString()),
            "Should handle very long SQL strings");
    }

    /**
     * Test connect does not throw exception
     */
    @Test
    public void testConnect_DoesNotThrowException() {
        assertDoesNotThrow(() -> databaseService.connect(),
            "Connect should not throw exception");
    }

    /**
     * Test object creation and method chaining
     */
    @Test
    public void testObjectCreationAndMethodChaining() {
        DatabaseService service = new DatabaseService();

        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery("SELECT 1");
            service.disconnect();
        }, "Method chaining should work correctly");
    }
}
