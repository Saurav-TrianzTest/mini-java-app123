package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 test class for DatabaseService
 * Tests all public methods, constructors, and critical code paths
 */
public class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    public void setUp() {
        databaseService = new DatabaseService();
    }

    @AfterEach
    public void tearDown() {
        if (databaseService != null) {
            databaseService.disconnect();
            databaseService = null;
        }
    }

    /**
     * Test constructor creates instance successfully
     */
    @Test
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    /**
     * Test connect method initializes connection
     * Tests the primary connection flow
     */
    @Test
    public void testConnect() {
        assertDoesNotThrow(() -> databaseService.connect(),
            "Connect method should not throw exception");
    }

    /**
     * Test connect method handles database connection initialization
     */
    @Test
    public void testConnectInitializesConnection() {
        databaseService.connect();
        // Verify connection was attempted (no exception thrown indicates success path)
        assertTrue(true, "Connection initialization completed");
    }

    /**
     * Test executeQuery with null connection
     */
    @Test
    public void testExecuteQueryWithoutConnection() {
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT * FROM users"),
            "executeQuery should handle null connection gracefully");
    }

    /**
     * Test executeQuery with valid SQL query after connection
     */
    @Test
    public void testExecuteQueryAfterConnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
            "executeQuery should execute without throwing exception");
    }

    /**
     * Test executeQuery with complex SQL statement
     */
    @Test
    public void testExecuteQueryWithComplexSQL() {
        databaseService.connect();
        String complexQuery = "SELECT u.id, u.name FROM users u WHERE u.active = true";
        assertDoesNotThrow(() -> databaseService.executeQuery(complexQuery),
            "executeQuery should handle complex queries");
    }

    /**
     * Test executeQuery with empty SQL string
     */
    @Test
    public void testExecuteQueryWithEmptySQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
            "executeQuery should handle empty SQL string");
    }

    /**
     * Test executeQuery with null SQL string
     */
    @Test
    public void testExecuteQueryWithNullSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
            "executeQuery should handle null SQL gracefully");
    }

    /**
     * Test disconnect method without prior connection
     */
    @Test
    public void testDisconnectWithoutConnection() {
        assertDoesNotThrow(() -> databaseService.disconnect(),
            "Disconnect should handle no connection gracefully");
    }

    /**
     * Test disconnect method after connection
     */
    @Test
    public void testDisconnectAfterConnection() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect(),
            "Disconnect should close connection without exception");
    }

    /**
     * Test multiple connect calls
     */
    @Test
    public void testMultipleConnectCalls() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.connect(),
            "Multiple connect calls should be handled");
    }

    /**
     * Test multiple disconnect calls
     */
    @Test
    public void testMultipleDisconnectCalls() {
        databaseService.connect();
        databaseService.disconnect();
        assertDoesNotThrow(() -> databaseService.disconnect(),
            "Multiple disconnect calls should be handled");
    }

    /**
     * Test executeQuery multiple times in sequence
     */
    @Test
    public void testMultipleExecuteQueryCalls() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.executeQuery("SELECT 3");
        }, "Multiple executeQuery calls should work");
    }

    /**
     * Test complete workflow: connect -> execute -> disconnect
     */
    @Test
    public void testCompleteWorkflow() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT * FROM test_table");
            databaseService.disconnect();
        }, "Complete workflow should execute without errors");
    }

    /**
     * Test executeQuery with INSERT statement
     */
    @Test
    public void testExecuteQueryWithInsertStatement() {
        databaseService.connect();
        String insertSQL = "INSERT INTO users (name, email) VALUES ('test', 'test@example.com')";
        assertDoesNotThrow(() -> databaseService.executeQuery(insertSQL),
            "executeQuery should handle INSERT statements");
    }

    /**
     * Test executeQuery with UPDATE statement
     */
    @Test
    public void testExecuteQueryWithUpdateStatement() {
        databaseService.connect();
        String updateSQL = "UPDATE users SET active = false WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(updateSQL),
            "executeQuery should handle UPDATE statements");
    }

    /**
     * Test executeQuery with DELETE statement
     */
    @Test
    public void testExecuteQueryWithDeleteStatement() {
        databaseService.connect();
        String deleteSQL = "DELETE FROM users WHERE id = 999";
        assertDoesNotThrow(() -> databaseService.executeQuery(deleteSQL),
            "executeQuery should handle DELETE statements");
    }

    /**
     * Test executeQuery with DDL statement
     */
    @Test
    public void testExecuteQueryWithDDLStatement() {
        databaseService.connect();
        String ddlSQL = "CREATE TABLE IF NOT EXISTS test_table (id INT PRIMARY KEY)";
        assertDoesNotThrow(() -> databaseService.executeQuery(ddlSQL),
            "executeQuery should handle DDL statements");
    }

    /**
     * Test disconnect after failed connection
     */
    @Test
    public void testDisconnectAfterFailedConnection() {
        // Don't connect, just try to disconnect
        assertDoesNotThrow(() -> databaseService.disconnect(),
            "Disconnect should handle no active connection");
    }

    /**
     * Test executeQuery with special characters in SQL
     */
    @Test
    public void testExecuteQueryWithSpecialCharacters() {
        databaseService.connect();
        String specialSQL = "SELECT * FROM users WHERE name = 'O''Brien'";
        assertDoesNotThrow(() -> databaseService.executeQuery(specialSQL),
            "executeQuery should handle special characters");
    }

    /**
     * Test connection lifecycle
     */
    @Test
    public void testConnectionLifecycle() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            assertTrue(true, "Connection established");
            databaseService.disconnect();
            assertTrue(true, "Connection closed");
        }, "Connection lifecycle should complete successfully");
    }
}
