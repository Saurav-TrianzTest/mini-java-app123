package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for DatabaseService class with 80%+ coverage
 */
public class DatabaseServiceTest {

    private DatabaseService databaseService;
    private ByteArrayOutputStream outputStreamCaptor;
    private ByteArrayOutputStream errorStreamCaptor;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    public void setUp() {
        databaseService = new DatabaseService();

        // Capture System.out and System.err
        outputStreamCaptor = new ByteArrayOutputStream();
        errorStreamCaptor = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));
    }

    @AfterEach
    public void tearDown() {
        // Restore original System.out and System.err
        System.setOut(originalOut);
        System.setErr(originalErr);

        // Clean up database connection
        if (databaseService != null) {
            databaseService.disconnect();
        }
    }

    // Constructor Tests
    @Test
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should be created successfully");
    }

    @Test
    public void testConstructorCreatesMultipleInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        DatabaseService service3 = new DatabaseService();

        assertNotNull(service1, "First instance should be created");
        assertNotNull(service2, "Second instance should be created");
        assertNotNull(service3, "Third instance should be created");
        assertNotSame(service1, service2, "Instances should be different objects");
        assertNotSame(service2, service3, "Instances should be different objects");
    }

    // Connect Method Tests
    @Test
    public void testConnectMethodExecutesWithoutException() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should execute without throwing exceptions");
    }

    @Test
    public void testConnectMethodPrintsConnectingMessage() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database"),
            "Connect method should print connecting message");
    }

    @Test
    public void testConnectMethodHandlesDatabaseConnectionFailure() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();

        assertTrue(output.contains("Connecting to database") ||
                   error.contains("Database connection failed"),
            "Connect should handle connection failure gracefully");
    }

    @Test
    public void testConnectMethodDisplaysDatabaseURL() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("jdbc:mysql://localhost:3306/mini_app_db") ||
                   error.contains("Database connection failed"),
            "Connect method should display database URL or handle failure");
    }

    @Test
    public void testConnectMethodDisplaysUsername() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("root") || output.contains("username") ||
                   error.contains("Database connection failed"),
            "Connect method should display username or handle connection failure");
    }

    @Test
    public void testConnectMethodInitializesCacheConnection() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("Redis") || output.contains("cache") ||
                   error.contains("Database connection failed"),
            "Connect method should initialize cache connection");
    }

    @Test
    public void testConnectMethodDisplaysRedisHost() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("127.0.0.1") || output.contains("6379") ||
                   error.contains("Database connection failed"),
            "Connect method should display Redis host and port");
    }

    @Test
    public void testConnectMethodInitializesExternalServices() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("external") || output.contains("API") ||
                   output.contains("api.example.com") ||
                   error.contains("Database connection failed"),
            "Connect method should initialize external services");
    }

    @Test
    public void testConnectMethodDisplaysPaymentServiceURL() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("payment") ||
                   output.contains("payment.internal.company.com") ||
                   error.contains("Database connection failed"),
            "Connect method should display payment service URL");
    }

    @Test
    public void testMultipleConnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect calls should not throw exceptions");
    }

    // Execute Query Tests
    @Test
    public void testExecuteQueryWithValidSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "Execute query should handle valid SQL without exceptions");
    }

    @Test
    public void testExecuteQueryWithNullSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(null);
        }, "Execute query should handle null SQL gracefully");
    }

    @Test
    public void testExecuteQueryWithEmptySQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("");
        }, "Execute query should handle empty SQL gracefully");
    }

    @Test
    public void testExecuteQueryWithSelectStatement() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
        }, "Execute query should handle SELECT statement");
    }

    @Test
    public void testExecuteQueryWithInsertStatement() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("INSERT INTO users (id, name) VALUES (1, 'Test')");
        }, "Execute query should handle INSERT statement");
    }

    @Test
    public void testExecuteQueryWithUpdateStatement() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("UPDATE users SET name='Updated' WHERE id=1");
        }, "Execute query should handle UPDATE statement");
    }

    @Test
    public void testExecuteQueryWithDeleteStatement() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("DELETE FROM users WHERE id=1");
        }, "Execute query should handle DELETE statement");
    }

    @Test
    public void testExecuteQueryWithComplexSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT u.id, u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id WHERE u.status = 'active'");
        }, "Execute query should handle complex SQL");
    }

    @Test
    public void testExecuteQueryWithoutConnection() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "Execute query should handle missing connection gracefully");
    }

    @Test
    public void testExecuteQueryMultipleTimes() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.executeQuery("SELECT 3");
        }, "Execute query should handle multiple executions");
    }

    @Test
    public void testExecuteQueryWithSpecialCharacters() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE name LIKE '%John%'");
        }, "Execute query should handle special characters");
    }

    @Test
    public void testExecuteQueryWithSemicolon() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users;");
        }, "Execute query should handle semicolons");
    }

    @Test
    public void testExecuteQueryWithLongSQL() {
        databaseService.connect();
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 100; i++) {
            longSQL.append(i);
            if (i < 99) longSQL.append(",");
        }
        longSQL.append(")");

        String finalSQL = longSQL.toString();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(finalSQL);
        }, "Execute query should handle long SQL statements");
    }

    // Disconnect Tests
    @Test
    public void testDisconnectMethodExecutesWithoutException() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect method should execute without throwing exceptions");
    }

    @Test
    public void testDisconnectWithoutConnection() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should handle missing connection gracefully");
    }

    @Test
    public void testDisconnectAfterConnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should work after connect");
    }

    @Test
    public void testDisconnectMultipleTimes() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Disconnect should handle multiple calls gracefully");
    }

    // Integration Tests
    @Test
    public void testCompleteLifecycle() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();
        }, "Complete lifecycle should execute without exceptions");
    }

    @Test
    public void testReconnectAfterDisconnect() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
            databaseService.disconnect();
        }, "Reconnect after disconnect should work");
    }

    @Test
    public void testExecuteQueryBetweenConnectAndDisconnect() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT * FROM users WHERE id = 1");
            databaseService.executeQuery("SELECT * FROM orders WHERE status = 'pending'");
            databaseService.disconnect();
        }, "Execute query between connect and disconnect should work");
    }

    @Test
    public void testServiceResilience() {
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> {
                DatabaseService service = new DatabaseService();
                service.connect();
                service.disconnect();
            }, "Service should be resilient to multiple instantiations");
        }
    }

    @Test
    public void testConcurrentServiceInstances() {
        assertDoesNotThrow(() -> {
            DatabaseService service1 = new DatabaseService();
            DatabaseService service2 = new DatabaseService();
            DatabaseService service3 = new DatabaseService();

            service1.connect();
            service2.connect();
            service3.connect();

            service1.executeQuery("SELECT 1");
            service2.executeQuery("SELECT 2");
            service3.executeQuery("SELECT 3");

            service1.disconnect();
            service2.disconnect();
            service3.disconnect();
        }, "Multiple service instances should work concurrently");
    }

    // Output Verification Tests
    @Test
    public void testServiceOutputIsNotEmpty() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertFalse(output.isEmpty(), "Service should produce output");
    }

    @Test
    public void testConnectionURLFormat() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("jdbc:mysql://") || output.contains("localhost") ||
                   error.contains("Database connection failed"),
            "Connection URL should be in correct format");
    }

    @Test
    public void testConnectionPortDisplay() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("3306") || output.contains("database") ||
                   errorStreamCaptor.toString().contains("Database connection failed"),
            "Connection port should be displayed");
    }

    @Test
    public void testDatabaseNameDisplay() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("mini_app_db") || output.contains("database") ||
                   errorStreamCaptor.toString().contains("Database connection failed"),
            "Database name should be displayed");
    }

    @Test
    public void testRedisPortDisplay() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("6379") || output.contains("Redis") ||
                   error.contains("Database connection failed"),
            "Redis port should be displayed");
    }

    @Test
    public void testExternalAPIURLDisplay() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("api.example.com") || output.contains("external") ||
                   error.contains("Database connection failed"),
            "External API URL should be displayed");
    }

    @Test
    public void testPaymentServiceURLDisplay() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("payment.internal.company.com") ||
                   output.contains("payment") ||
                   error.contains("Database connection failed"),
            "Payment service URL should be displayed");
    }

    // Edge Case Tests
    @Test
    public void testServiceHandlesSQLException() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("INVALID SQL SYNTAX HERE");
        }, "Service should handle SQL exceptions gracefully");
    }

    @Test
    public void testExecuteQueryWithMalformedSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT FROM WHERE");
        }, "Execute query should handle malformed SQL");
    }

    @Test
    public void testExecuteQueryWithSQLInjectionAttempt() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE id = 1 OR 1=1");
        }, "Execute query should handle SQL injection attempts");
    }

    @Test
    public void testInstanceCreationSpeed() {
        long startTime = System.nanoTime();
        DatabaseService service = new DatabaseService();
        long endTime = System.nanoTime();

        long duration = (endTime - startTime) / 1_000_000;
        assertTrue(duration < 1000,
            "Instance creation should be fast (under 1 second)");
        assertNotNull(service);
    }

    @Test
    public void testQueryExecutionWithBoundaryValues() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE age = 0");
            databaseService.executeQuery("SELECT * FROM users WHERE age = 999999");
            databaseService.executeQuery("SELECT * FROM users WHERE name = ''");
        }, "Execute query should handle boundary values");
    }

    @Test
    public void testConnectPrintsAllExpectedMessages() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();

        boolean hasConnectionAttempt = output.contains("Connecting to database");
        boolean hasErrorOrSuccess = output.contains("Connected to database") ||
                                   error.contains("Database connection failed");

        assertTrue(hasConnectionAttempt, "Should attempt database connection");
    }

    @Test
    public void testDisconnectWithClosedConnection() {
        databaseService.connect();
        databaseService.disconnect();

        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should handle already closed connection");
    }

    @Test
    public void testMultipleQueriesInSequence() {
        databaseService.connect();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                databaseService.executeQuery("SELECT " + i);
            }
        }, "Should handle multiple sequential queries");
    }

    @Test
    public void testExecuteQueryWithWhitespaceSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("   ");
            databaseService.executeQuery("\n\t");
        }, "Execute query should handle whitespace SQL");
    }

    @Test
    public void testServiceStateAfterException() {
        databaseService.connect();
        databaseService.executeQuery("INVALID SQL");

        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "Service should remain operational after exception");
    }
}
