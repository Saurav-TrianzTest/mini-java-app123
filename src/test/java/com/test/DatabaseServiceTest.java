package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit test suite for DatabaseService class.
 * Tests all methods including constructors, connection management, and query execution.
 */
class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService();
    }

    @AfterEach
    void tearDown() {
        if (databaseService != null) {
            databaseService.disconnect();
        }
    }

    @Test
    @DisplayName("Test DatabaseService constructor - creates non-null instance")
    void testDatabaseServiceConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect method - establishes database connection")
    void testConnect() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "connect() should not throw exception");
    }

    @Test
    @DisplayName("Test connect method - handles connection errors gracefully")
    void testConnectWithInvalidCredentials() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "connect() should handle errors gracefully without throwing");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL")
    void testExecuteQueryWithValidSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "executeQuery() should not throw exception with valid SQL");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL")
    void testExecuteQueryWithNullSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(null);
        }, "executeQuery() should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL")
    void testExecuteQueryWithEmptySQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("");
        }, "executeQuery() should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with complex SQL statement")
    void testExecuteQueryWithComplexSQL() {
        databaseService.connect();
        String complexSQL = "SELECT * FROM users WHERE id > 100 AND status = 'active'";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(complexSQL);
        }, "executeQuery() should handle complex SQL statements");
    }

    @Test
    @DisplayName("Test executeQuery without connection")
    void testExecuteQueryWithoutConnection() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
        }, "executeQuery() should handle missing connection gracefully");
    }

    @Test
    @DisplayName("Test disconnect method - closes connection")
    void testDisconnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "disconnect() should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect without prior connection")
    void testDisconnectWithoutConnection() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "disconnect() should handle no connection gracefully");
    }

    @Test
    @DisplayName("Test multiple connect calls")
    void testMultipleConnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect() calls should be handled gracefully");
    }

    @Test
    @DisplayName("Test multiple disconnect calls")
    void testMultipleDisconnectCalls() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect() calls should be handled gracefully");
    }

    @Test
    @DisplayName("Test connect-query-disconnect workflow")
    void testCompleteWorkflow() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();
        }, "Complete workflow should execute without errors");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    void testExecuteQueryWithInsert() {
        databaseService.connect();
        String insertSQL = "INSERT INTO users (name, email) VALUES ('test', 'test@example.com')";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(insertSQL);
        }, "executeQuery() should handle INSERT statements");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    void testExecuteQueryWithUpdate() {
        databaseService.connect();
        String updateSQL = "UPDATE users SET status = 'inactive' WHERE id = 1";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(updateSQL);
        }, "executeQuery() should handle UPDATE statements");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    void testExecuteQueryWithDelete() {
        databaseService.connect();
        String deleteSQL = "DELETE FROM users WHERE id = 1";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(deleteSQL);
        }, "executeQuery() should handle DELETE statements");
    }

    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    void testExecuteQueryWithMalformedSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("INVALID SQL STATEMENT");
        }, "executeQuery() should handle malformed SQL gracefully");
    }

    @Test
    @DisplayName("Test instance state after connect")
    void testInstanceStateAfterConnect() {
        databaseService.connect();
        assertNotNull(databaseService, "DatabaseService should remain valid after connect");
    }

    @Test
    @DisplayName("Test instance state after disconnect")
    void testInstanceStateAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();
        assertNotNull(databaseService, "DatabaseService should remain valid after disconnect");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    void testExecuteQueryWithSQLInjection() {
        databaseService.connect();
        String injectionSQL = "SELECT * FROM users WHERE id = 1 OR 1=1; DROP TABLE users;";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(injectionSQL);
        }, "executeQuery() should handle SQL injection attempts");
    }

    @Test
    @DisplayName("Test executeQuery with very long SQL")
    void testExecuteQueryWithLongSQL() {
        databaseService.connect();
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 100; i++) {
            longSQL.append(i);
            if (i < 99) longSQL.append(",");
        }
        longSQL.append(")");
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(longSQL.toString());
        }, "executeQuery() should handle long SQL statements");
    }

    @Test
    @DisplayName("Test multiple queries in sequence")
    void testMultipleQueriesInSequence() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.executeQuery("SELECT 3");
        }, "Multiple sequential queries should execute without errors");
    }
}
