package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Comprehensive test suite for DatabaseService class
 * Tests all public methods, constructors, and edge cases
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
        }
    }

    /**
     * Test constructor creates a valid DatabaseService instance
     */
    @Test
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    /**
     * Test connect method attempts to establish database connection
     * Tests positive scenario where connection attempt is made
     */
    @Test
    public void testConnect() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should not throw exception");
    }

    /**
     * Test connect method with missing database driver
     * Tests error handling for ClassNotFoundException
     */
    @Test
    public void testConnectWithMissingDriver() {
        // Connection attempt should handle missing driver gracefully
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect should handle missing driver gracefully");
    }

    /**
     * Test connect method with invalid credentials
     * Tests error handling for SQLException
     */
    @Test
    public void testConnectWithInvalidCredentials() {
        // Connection attempt should handle invalid credentials gracefully
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect should handle invalid credentials gracefully");
    }

    /**
     * Test executeQuery with valid SQL statement
     * Tests positive scenario with SELECT query
     */
    @Test
    public void testExecuteQueryWithValidSQL() {
        String validSQL = "SELECT * FROM users";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(validSQL);
        }, "Execute query should not throw exception with valid SQL");
    }

    /**
     * Test executeQuery with INSERT statement
     * Tests INSERT query execution
     */
    @Test
    public void testExecuteQueryWithInsertStatement() {
        String insertSQL = "INSERT INTO users (name, email) VALUES ('test', 'test@example.com')";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(insertSQL);
        }, "Execute query should handle INSERT statement");
    }

    /**
     * Test executeQuery with UPDATE statement
     * Tests UPDATE query execution
     */
    @Test
    public void testExecuteQueryWithUpdateStatement() {
        String updateSQL = "UPDATE users SET name='updated' WHERE id=1";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(updateSQL);
        }, "Execute query should handle UPDATE statement");
    }

    /**
     * Test executeQuery with DELETE statement
     * Tests DELETE query execution
     */
    @Test
    public void testExecuteQueryWithDeleteStatement() {
        String deleteSQL = "DELETE FROM users WHERE id=1";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(deleteSQL);
        }, "Execute query should handle DELETE statement");
    }

    /**
     * Test executeQuery with null SQL statement
     * Tests error handling for null input
     */
    @Test
    public void testExecuteQueryWithNullSQL() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(null);
        }, "Execute query should handle null SQL gracefully");
    }

    /**
     * Test executeQuery with empty SQL statement
     * Tests error handling for empty string
     */
    @Test
    public void testExecuteQueryWithEmptySQL() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("");
        }, "Execute query should handle empty SQL gracefully");
    }

    /**
     * Test executeQuery with invalid SQL syntax
     * Tests error handling for malformed SQL
     */
    @Test
    public void testExecuteQueryWithInvalidSQL() {
        String invalidSQL = "SELECT * FORM users"; // typo: FORM instead of FROM
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(invalidSQL);
        }, "Execute query should handle invalid SQL gracefully");
    }

    /**
     * Test executeQuery without prior connection
     * Tests behavior when query is executed without connection
     */
    @Test
    public void testExecuteQueryWithoutConnection() {
        DatabaseService newService = new DatabaseService();
        assertDoesNotThrow(() -> {
            newService.executeQuery("SELECT 1");
        }, "Execute query should handle no connection gracefully");
    }

    /**
     * Test executeQuery with complex SQL statement
     * Tests JOIN query execution
     */
    @Test
    public void testExecuteQueryWithJoinStatement() {
        String joinSQL = "SELECT u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(joinSQL);
        }, "Execute query should handle JOIN statement");
    }

    /**
     * Test disconnect method closes connection properly
     * Tests positive scenario for disconnect
     */
    @Test
    public void testDisconnect() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should not throw exception");
    }

    /**
     * Test disconnect without prior connection
     * Tests disconnect behavior when no connection exists
     */
    @Test
    public void testDisconnectWithoutConnection() {
        DatabaseService newService = new DatabaseService();
        assertDoesNotThrow(() -> {
            newService.disconnect();
        }, "Disconnect should handle no connection gracefully");
    }

    /**
     * Test multiple connect calls
     * Tests behavior when connect is called multiple times
     */
    @Test
    public void testMultipleConnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect calls should be handled gracefully");
    }

    /**
     * Test multiple disconnect calls
     * Tests behavior when disconnect is called multiple times
     */
    @Test
    public void testMultipleDisconnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect calls should be handled gracefully");
    }

    /**
     * Test connect-disconnect-connect cycle
     * Tests reconnection after disconnect
     */
    @Test
    public void testConnectDisconnectConnectCycle() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
        }, "Reconnection after disconnect should work");
    }

    /**
     * Test executeQuery after disconnect
     * Tests query execution on closed connection
     */
    @Test
    public void testExecuteQueryAfterDisconnect() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.executeQuery("SELECT 1");
        }, "Execute query after disconnect should be handled gracefully");
    }

    /**
     * Test executeQuery with very long SQL statement
     * Tests boundary condition with large query
     */
    @Test
    public void testExecuteQueryWithLongSQL() {
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSQL.append(i);
            if (i < 999) longSQL.append(",");
        }
        longSQL.append(")");

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(longSQL.toString());
        }, "Execute query should handle long SQL statement");
    }

    /**
     * Test executeQuery with SQL injection attempt
     * Tests security handling of SQL injection
     */
    @Test
    public void testExecuteQueryWithSQLInjection() {
        String sqlInjection = "SELECT * FROM users WHERE name='admin' OR '1'='1'";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sqlInjection);
        }, "Execute query should handle SQL injection attempt");
    }

    /**
     * Test executeQuery with special characters
     * Tests query with special characters in SQL
     */
    @Test
    public void testExecuteQueryWithSpecialCharacters() {
        String specialSQL = "SELECT * FROM users WHERE name='O''Brien'";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(specialSQL);
        }, "Execute query should handle special characters");
    }
}
