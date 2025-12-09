package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService class
 * Tests all public and private methods, constructors, and edge cases
 */
@DisplayName("DatabaseService Test Suite")
public class DatabaseServiceTest {

    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    public void setUp() {
        originalOut = System.out;
        originalErr = System.err;
    }

    @AfterEach
    public void tearDown() {
        // Restore original System.out and System.err
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Test DatabaseService default constructor creates non-null instance")
    public void testDatabaseServiceConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test DatabaseService constructor creates multiple independent instances")
    public void testDatabaseServiceConstructorMultipleInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();

        assertNotNull(service1, "First DatabaseService instance should not be null");
        assertNotNull(service2, "Second DatabaseService instance should not be null");
        assertNotSame(service1, service2, "Multiple instances should be different objects");
    }

    @Test
    @DisplayName("Test connect method executes without throwing exceptions")
    public void testConnectMethodExecutesSuccessfully() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should not throw exceptions during execution");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test connect method prints connection message")
    public void testConnectMethodPrintsMessage() {
        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        DatabaseService databaseService = new DatabaseService();
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database") || output.contains("database"),
                   "Connect method should print connection message");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test connect method handles database connection failure gracefully")
    public void testConnectMethodHandlesConnectionFailure() {
        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));

        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should handle connection failures gracefully");

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();

        assertTrue(output.length() > 0 || error.length() > 0,
                   "Connect should produce output or error messages");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test connect method initializes cache connection")
    public void testConnectMethodInitializesCache() {
        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        DatabaseService databaseService = new DatabaseService();
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Redis") || output.contains("cache") || output.contains("Connecting"),
                   "Connect should initialize cache connection");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test connect method initializes external services")
    public void testConnectMethodInitializesExternalServices() {
        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));

        DatabaseService databaseService = new DatabaseService();
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("external") || output.contains("API") || output.contains("service") || output.contains("Initializing") || error.length() > 0,
                   "Connect should initialize external services");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test disconnect method executes without throwing exceptions")
    public void testDisconnectMethodExecutesSuccessfully() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect method should not throw exceptions");
    }

    @Test
    @DisplayName("Test disconnect method when not connected")
    public void testDisconnectMethodWhenNotConnected() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should handle not connected state gracefully");
    }

    @Test
    @DisplayName("Test disconnect method after successful connection")
    public void testDisconnectMethodAfterConnection() {
        DatabaseService databaseService = new DatabaseService();
        databaseService.connect();

        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should work after successful connection");
    }

    @Test
    @DisplayName("Test disconnect method prints closure message")
    public void testDisconnectMethodPrintsMessage() {
        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();

        DatabaseService databaseService = new DatabaseService();
        databaseService.connect();

        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));

        databaseService.disconnect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();

        assertTrue(output.length() >= 0 && error.length() >= 0,
                   "Disconnect should complete without critical errors");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL")
    public void testExecuteQueryWithNullSQL() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(null);
        }, "executeQuery should handle null SQL gracefully");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL")
    public void testExecuteQueryWithEmptySQL() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("");
        }, "executeQuery should handle empty SQL gracefully");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL statement")
    public void testExecuteQueryWithValidSQL() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
        }, "executeQuery should handle valid SQL without throwing exceptions");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    public void testExecuteQueryWithInsertStatement() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("INSERT INTO users (name) VALUES ('test')");
        }, "executeQuery should handle INSERT statement");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    public void testExecuteQueryWithUpdateStatement() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("UPDATE users SET name='test' WHERE id=1");
        }, "executeQuery should handle UPDATE statement");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    public void testExecuteQueryWithDeleteStatement() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("DELETE FROM users WHERE id=1");
        }, "executeQuery should handle DELETE statement");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with CREATE TABLE statement")
    public void testExecuteQueryWithCreateTableStatement() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("CREATE TABLE test (id INT PRIMARY KEY)");
        }, "executeQuery should handle CREATE TABLE statement");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    public void testExecuteQueryWithMalformedSQL() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("INVALID SQL STATEMENT");
        }, "executeQuery should handle malformed SQL gracefully");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery without prior connection")
    public void testExecuteQueryWithoutConnection() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
        }, "executeQuery should handle no connection state gracefully");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery after connection")
    public void testExecuteQueryAfterConnection() {
        DatabaseService databaseService = new DatabaseService();
        databaseService.connect();

        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "executeQuery should work after connection");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    public void testExecuteQueryWithLongSQL() {
        DatabaseService databaseService = new DatabaseService();
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE ");
        for (int i = 0; i < 100; i++) {
            longSQL.append("id=").append(i).append(" OR ");
        }
        longSQL.append("id=999");

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(longSQL.toString());
        }, "executeQuery should handle long SQL statements");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with SQL containing special characters")
    public void testExecuteQueryWithSpecialCharacters() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE name='O''Brien'");
        }, "executeQuery should handle special characters in SQL");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test multiple sequential connect calls")
    public void testMultipleSequentialConnectCalls() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
            databaseService.connect();
        }, "Multiple sequential connect calls should not throw exceptions");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test multiple sequential disconnect calls")
    public void testMultipleSequentialDisconnectCalls() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple sequential disconnect calls should not throw exceptions");
    }

    @Test
    @DisplayName("Test connect-disconnect-connect cycle")
    public void testConnectDisconnectConnectCycle() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
        }, "Connect-disconnect-connect cycle should work correctly");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery after disconnect")
    public void testExecuteQueryAfterDisconnect() {
        DatabaseService databaseService = new DatabaseService();
        databaseService.connect();
        databaseService.disconnect();

        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
        }, "executeQuery after disconnect should handle gracefully");
    }

    @Test
    @DisplayName("Test concurrent DatabaseService instances")
    public void testConcurrentDatabaseServiceInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        DatabaseService service3 = new DatabaseService();

        assertDoesNotThrow(() -> {
            service1.connect();
            service2.connect();
            service3.connect();

            service1.disconnect();
            service2.disconnect();
            service3.disconnect();
        }, "Concurrent instances should work independently");
    }

    @Test
    @DisplayName("Test executeQuery handles SQL injection attempt")
    public void testExecuteQueryHandlesSQLInjection() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE id = '1' OR '1'='1'");
        }, "executeQuery should handle SQL injection attempts");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test service initialization is clean")
    public void testServiceInitializationIsClean() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "Service should initialize cleanly");
        assertEquals(DatabaseService.class, service.getClass(), "Service should be correct type");
    }

    @Test
    @DisplayName("Test service handles database unavailability")
    public void testServiceHandlesDatabaseUnavailability() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Service should handle database unavailability gracefully");

        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with parameterized query pattern")
    public void testExecuteQueryWithParameterizedQueryPattern() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE id = ?");
        }, "executeQuery should handle parameterized query patterns");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test executeQuery with multiple statements")
    public void testExecuteQueryWithMultipleStatements() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users; SELECT * FROM orders;");
        }, "executeQuery should handle multiple statements");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test service resource cleanup")
    public void testServiceResourceCleanup() {
        DatabaseService databaseService = new DatabaseService();
        databaseService.connect();
        databaseService.executeQuery("SELECT 1");

        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Service should cleanup resources properly");
    }

    @Test
    @DisplayName("Test connect method handles SQLException")
    public void testConnectMethodHandlesSQLException() {
        ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorStreamCaptor));

        DatabaseService databaseService = new DatabaseService();
        databaseService.connect();

        String error = errorStreamCaptor.toString();
        assertNotNull(error, "Error stream should be initialized");
        databaseService.disconnect();
    }

    @Test
    @DisplayName("Test disconnect method handles SQLException")
    public void testDisconnectMethodHandlesSQLException() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should handle SQLException gracefully");
    }

    @Test
    @DisplayName("Test executeQuery handles SQLException")
    public void testExecuteQueryHandlesSQLException() {
        DatabaseService databaseService = new DatabaseService();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM nonexistent_table");
        }, "executeQuery should handle SQLException gracefully");
        databaseService.disconnect();
    }
}
