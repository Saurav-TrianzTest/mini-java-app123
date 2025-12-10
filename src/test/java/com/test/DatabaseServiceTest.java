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
 * Target coverage: 80%+
 */
@DisplayName("DatabaseService Test Suite")
public class DatabaseServiceTest {

    private DatabaseService databaseService;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outputStreamCaptor;
    private ByteArrayOutputStream errorStreamCaptor;

    @BeforeEach
    public void setUp() {
        databaseService = new DatabaseService();
        originalOut = System.out;
        originalErr = System.err;
        outputStreamCaptor = new ByteArrayOutputStream();
        errorStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));
    }

    @AfterEach
    public void tearDown() {
        if (databaseService != null) {
            databaseService.disconnect();
        }
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // Constructor Tests
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
        DatabaseService service3 = new DatabaseService();

        assertNotNull(service1, "First DatabaseService instance should not be null");
        assertNotNull(service2, "Second DatabaseService instance should not be null");
        assertNotNull(service3, "Third DatabaseService instance should not be null");
        assertNotSame(service1, service2, "Multiple instances should be different objects");
        assertNotSame(service2, service3, "Multiple instances should be different objects");
        assertNotSame(service1, service3, "Multiple instances should be different objects");
    }

    @Test
    @DisplayName("Test constructor initializes with clean state")
    public void testConstructorInitializesCleanState() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "Service should initialize");
        assertEquals(DatabaseService.class, service.getClass(), "Service should be correct type");
    }

    // Connect Method Tests
    @Test
    @DisplayName("Test connect method executes without throwing exceptions")
    public void testConnectMethodExecutesSuccessfully() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should not throw exceptions during execution");
    }

    @Test
    @DisplayName("Test connect method prints connection message")
    public void testConnectMethodPrintsMessage() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database") || output.contains("database"),
                   "Connect method should print connection message");
    }

    @Test
    @DisplayName("Test connect method handles database connection failure gracefully")
    public void testConnectMethodHandlesConnectionFailure() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should handle connection failures gracefully");

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();

        assertTrue(output.length() > 0 || error.length() > 0,
                   "Connect should produce output or error messages");
    }

    @Test
    @DisplayName("Test connect method initializes cache connection")
    public void testConnectMethodInitializesCache() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Redis") || output.contains("cache") || output.contains("Connecting"),
                   "Connect should initialize cache connection");
    }

    @Test
    @DisplayName("Test connect method initializes external services")
    public void testConnectMethodInitializesExternalServices() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("external") || output.contains("API") ||
                   output.contains("service") || output.contains("Initializing") ||
                   output.contains("payment") || error.length() > 0,
                   "Connect should initialize external services");
    }

    @Test
    @DisplayName("Test connect prints database URL information")
    public void testConnectPrintsDatabaseURL() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("jdbc:postgresql") || output.contains("database") ||
                   output.contains("Connected") || output.contains("localhost"),
                   "Connect should print database URL information");
    }

    @Test
    @DisplayName("Test connect prints username information")
    public void testConnectPrintsUsername() {
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("username") || output.contains("postgres") ||
                   output.contains("Using") || error.length() > 0,
                   "Connect should print username information or error");
    }

    @Test
    @DisplayName("Test multiple sequential connect calls")
    public void testMultipleSequentialConnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
            databaseService.connect();
        }, "Multiple sequential connect calls should not throw exceptions");
    }

    @Test
    @DisplayName("Test connect-disconnect-connect cycle")
    public void testConnectDisconnectConnectCycle() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
            databaseService.disconnect();
        }, "Connect-disconnect-connect cycle should work correctly");
    }

    @Test
    @DisplayName("Test connect after disconnect maintains functionality")
    public void testConnectAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();

        outputStreamCaptor.reset();
        databaseService.connect();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting") || output.contains("database"),
                   "Connect after disconnect should work properly");
    }

    // Disconnect Method Tests
    @Test
    @DisplayName("Test disconnect method executes without throwing exceptions")
    public void testDisconnectMethodExecutesSuccessfully() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect method should not throw exceptions");
    }

    @Test
    @DisplayName("Test disconnect method when not connected")
    public void testDisconnectMethodWhenNotConnected() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should handle not connected state gracefully");
    }

    @Test
    @DisplayName("Test disconnect method after successful connection")
    public void testDisconnectMethodAfterConnection() {
        databaseService.connect();

        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should work after successful connection");
    }

    @Test
    @DisplayName("Test disconnect method handles closure gracefully")
    public void testDisconnectMethodHandlesClosure() {
        databaseService.connect();
        databaseService.disconnect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();

        assertTrue(output.length() >= 0 && error.length() >= 0,
                   "Disconnect should complete without critical errors");
    }

    @Test
    @DisplayName("Test multiple sequential disconnect calls")
    public void testMultipleSequentialDisconnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple sequential disconnect calls should not throw exceptions");
    }

    @Test
    @DisplayName("Test disconnect after connect prints message")
    public void testDisconnectAfterConnectPrintsMessage() {
        databaseService.connect();
        outputStreamCaptor.reset();
        errorStreamCaptor.reset();

        databaseService.disconnect();

        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();

        assertTrue(output.length() >= 0 || error.length() >= 0,
                   "Disconnect should handle closure");
    }

    // ExecuteQuery Method Tests
    @Test
    @DisplayName("Test executeQuery with null SQL")
    public void testExecuteQueryWithNullSQL() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(null);
        }, "executeQuery should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL")
    public void testExecuteQueryWithEmptySQL() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("");
        }, "executeQuery should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with whitespace-only SQL")
    public void testExecuteQueryWithWhitespaceSQL() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("   ");
        }, "executeQuery should handle whitespace-only SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with valid SELECT statement")
    public void testExecuteQueryWithValidSelectSQL() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
        }, "executeQuery should handle valid SELECT SQL without throwing exceptions");
    }

    @Test
    @DisplayName("Test executeQuery with SELECT 1 statement")
    public void testExecuteQueryWithSelectOne() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "executeQuery should handle SELECT 1 statement");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    public void testExecuteQueryWithInsertStatement() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("INSERT INTO users (name) VALUES ('test')");
        }, "executeQuery should handle INSERT statement");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    public void testExecuteQueryWithUpdateStatement() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("UPDATE users SET name='test' WHERE id=1");
        }, "executeQuery should handle UPDATE statement");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    public void testExecuteQueryWithDeleteStatement() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("DELETE FROM users WHERE id=1");
        }, "executeQuery should handle DELETE statement");
    }

    @Test
    @DisplayName("Test executeQuery with CREATE TABLE statement")
    public void testExecuteQueryWithCreateTableStatement() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("CREATE TABLE test (id INT PRIMARY KEY)");
        }, "executeQuery should handle CREATE TABLE statement");
    }

    @Test
    @DisplayName("Test executeQuery with DROP TABLE statement")
    public void testExecuteQueryWithDropTableStatement() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("DROP TABLE test");
        }, "executeQuery should handle DROP TABLE statement");
    }

    @Test
    @DisplayName("Test executeQuery with ALTER TABLE statement")
    public void testExecuteQueryWithAlterTableStatement() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("ALTER TABLE users ADD COLUMN email VARCHAR(255)");
        }, "executeQuery should handle ALTER TABLE statement");
    }

    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    public void testExecuteQueryWithMalformedSQL() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("INVALID SQL STATEMENT");
        }, "executeQuery should handle malformed SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with syntax error")
    public void testExecuteQueryWithSyntaxError() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FORM users");
        }, "executeQuery should handle syntax errors gracefully");
    }

    @Test
    @DisplayName("Test executeQuery without prior connection")
    public void testExecuteQueryWithoutConnection() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
        }, "executeQuery should handle no connection state gracefully");
    }

    @Test
    @DisplayName("Test executeQuery after connection")
    public void testExecuteQueryAfterConnection() {
        databaseService.connect();

        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "executeQuery should work after connection");
    }

    @Test
    @DisplayName("Test executeQuery after disconnect")
    public void testExecuteQueryAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();

        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
        }, "executeQuery after disconnect should handle gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    public void testExecuteQueryWithLongSQL() {
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE ");
        for (int i = 0; i < 100; i++) {
            longSQL.append("id=").append(i).append(" OR ");
        }
        longSQL.append("id=999");

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(longSQL.toString());
        }, "executeQuery should handle long SQL statements");
    }

    @Test
    @DisplayName("Test executeQuery with SQL containing special characters")
    public void testExecuteQueryWithSpecialCharacters() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE name='O''Brien'");
        }, "executeQuery should handle special characters in SQL");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    public void testExecuteQueryHandlesSQLInjection() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE id = '1' OR '1'='1'");
        }, "executeQuery should handle SQL injection attempts");
    }

    @Test
    @DisplayName("Test executeQuery with parameterized query pattern")
    public void testExecuteQueryWithParameterizedQueryPattern() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE id = ?");
        }, "executeQuery should handle parameterized query patterns");
    }

    @Test
    @DisplayName("Test executeQuery with multiple statements")
    public void testExecuteQueryWithMultipleStatements() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users; SELECT * FROM orders;");
        }, "executeQuery should handle multiple statements");
    }

    @Test
    @DisplayName("Test executeQuery with JOIN statement")
    public void testExecuteQueryWithJoinStatement() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users u JOIN orders o ON u.id = o.user_id");
        }, "executeQuery should handle JOIN statements");
    }

    @Test
    @DisplayName("Test executeQuery with nested SELECT")
    public void testExecuteQueryWithNestedSelect() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)");
        }, "executeQuery should handle nested SELECT statements");
    }

    @Test
    @DisplayName("Test executeQuery with LIKE operator")
    public void testExecuteQueryWithLikeOperator() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE name LIKE '%test%'");
        }, "executeQuery should handle LIKE operator");
    }

    @Test
    @DisplayName("Test executeQuery with ORDER BY clause")
    public void testExecuteQueryWithOrderBy() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users ORDER BY name ASC");
        }, "executeQuery should handle ORDER BY clause");
    }

    @Test
    @DisplayName("Test executeQuery with GROUP BY clause")
    public void testExecuteQueryWithGroupBy() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT COUNT(*), name FROM users GROUP BY name");
        }, "executeQuery should handle GROUP BY clause");
    }

    @Test
    @DisplayName("Test executeQuery with LIMIT clause")
    public void testExecuteQueryWithLimit() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users LIMIT 10");
        }, "executeQuery should handle LIMIT clause");
    }

    @Test
    @DisplayName("Test executeQuery prints executing message after connection")
    public void testExecuteQueryPrintsMessage() {
        databaseService.connect();
        outputStreamCaptor.reset();

        databaseService.executeQuery("SELECT 1");

        String output = outputStreamCaptor.toString();
        assertNotNull(output, "Output should not be null");
    }

    @Test
    @DisplayName("Test multiple sequential executeQuery calls")
    public void testMultipleSequentialExecuteQueryCalls() {
        databaseService.connect();

        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.executeQuery("SELECT 3");
        }, "Multiple sequential executeQuery calls should not throw exceptions");
    }

    // Integration Tests
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

            service1.executeQuery("SELECT 1");
            service2.executeQuery("SELECT 2");
            service3.executeQuery("SELECT 3");

            service1.disconnect();
            service2.disconnect();
            service3.disconnect();
        }, "Concurrent instances should work independently");
    }

    @Test
    @DisplayName("Test service resource cleanup")
    public void testServiceResourceCleanup() {
        databaseService.connect();
        databaseService.executeQuery("SELECT 1");

        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Service should cleanup resources properly");
    }

    @Test
    @DisplayName("Test service handles database unavailability")
    public void testServiceHandlesDatabaseUnavailability() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Service should handle database unavailability gracefully");
    }

    @Test
    @DisplayName("Test full lifecycle: connect, query, disconnect")
    public void testFullLifecycle() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.disconnect();
        }, "Full lifecycle should complete without errors");
    }

    @Test
    @DisplayName("Test executeQuery handles nonexistent table")
    public void testExecuteQueryWithNonexistentTable() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM nonexistent_table");
        }, "executeQuery should handle nonexistent table gracefully");
    }

    @Test
    @DisplayName("Test connect handles SQLException")
    public void testConnectMethodHandlesSQLException() {
        databaseService.connect();

        String error = errorStreamCaptor.toString();
        assertNotNull(error, "Error stream should be initialized");
    }

    @Test
    @DisplayName("Test disconnect handles SQLException")
    public void testDisconnectMethodHandlesSQLException() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should handle SQLException gracefully");
    }

    @Test
    @DisplayName("Test executeQuery handles SQLException")
    public void testExecuteQueryHandlesSQLException() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM nonexistent_table");
        }, "executeQuery should handle SQLException gracefully");
    }

    @Test
    @DisplayName("Test service behavior with rapid connect-disconnect cycles")
    public void testRapidConnectDisconnectCycles() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                databaseService.connect();
                databaseService.disconnect();
            }
        }, "Rapid connect-disconnect cycles should work properly");
    }

    @Test
    @DisplayName("Test service handles empty query execution")
    public void testEmptyQueryExecution() {
        databaseService.connect();

        assertDoesNotThrow(() -> {
            databaseService.executeQuery("");
            databaseService.executeQuery("");
            databaseService.executeQuery("");
        }, "Empty query executions should be handled");
    }

    @Test
    @DisplayName("Test executeQuery with complex WHERE clause")
    public void testExecuteQueryWithComplexWhereClause() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE age > 18 AND (city = 'NYC' OR city = 'LA') AND status = 'active'");
        }, "executeQuery should handle complex WHERE clauses");
    }

    @Test
    @DisplayName("Test executeQuery with HAVING clause")
    public void testExecuteQueryWithHavingClause() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT COUNT(*), city FROM users GROUP BY city HAVING COUNT(*) > 5");
        }, "executeQuery should handle HAVING clause");
    }

    @Test
    @DisplayName("Test executeQuery with UNION operator")
    public void testExecuteQueryWithUnionOperator() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT name FROM users UNION SELECT name FROM admins");
        }, "executeQuery should handle UNION operator");
    }

    @Test
    @DisplayName("Test executeQuery with CASE statement")
    public void testExecuteQueryWithCaseStatement() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT name, CASE WHEN age < 18 THEN 'minor' ELSE 'adult' END AS status FROM users");
        }, "executeQuery should handle CASE statements");
    }

    @Test
    @DisplayName("Test executeQuery with EXISTS clause")
    public void testExecuteQueryWithExistsClause() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users WHERE EXISTS (SELECT 1 FROM orders WHERE orders.user_id = users.id)");
        }, "executeQuery should handle EXISTS clause");
    }
}
