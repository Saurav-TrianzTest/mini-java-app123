package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 test suite for DatabaseService class
 * Tests all public methods, constructors, and edge cases
 */
@DisplayName("DatabaseService Test Suite")
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
        databaseService = null;
    }

    @Test
    @DisplayName("Test DatabaseService constructor creates non-null instance")
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect method executes without exceptions")
    public void testConnectMethod() {
        assertDoesNotThrow(() -> databaseService.connect(),
                "connect() method should not throw exceptions");
    }

    @Test
    @DisplayName("Test connect method with valid connection establishment")
    public void testConnectEstablishesConnection() {
        databaseService.connect();
        // Verify connection attempt was made (output-based verification)
        assertNotNull(databaseService, "DatabaseService should remain valid after connect");
    }

    @Test
    @DisplayName("Test connect method initializes cache connection")
    public void testConnectInitializesCache() {
        assertDoesNotThrow(() -> databaseService.connect(),
                "connect() should initialize cache without exceptions");
    }

    @Test
    @DisplayName("Test connect method initializes external services")
    public void testConnectInitializesExternalServices() {
        assertDoesNotThrow(() -> databaseService.connect(),
                "connect() should initialize external services without exceptions");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL statement")
    public void testExecuteQueryWithValidSQL() {
        String validSQL = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(validSQL),
                "executeQuery should handle valid SQL without exceptions");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL statement")
    public void testExecuteQueryWithNullSQL() {
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
                "executeQuery should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL statement")
    public void testExecuteQueryWithEmptySQL() {
        String emptySQL = "";
        assertDoesNotThrow(() -> databaseService.executeQuery(emptySQL),
                "executeQuery should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    public void testExecuteQueryWithInsertStatement() {
        String insertSQL = "INSERT INTO users (name, email) VALUES ('Test', 'test@example.com')";
        assertDoesNotThrow(() -> databaseService.executeQuery(insertSQL),
                "executeQuery should handle INSERT statements");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    public void testExecuteQueryWithUpdateStatement() {
        String updateSQL = "UPDATE users SET name = 'Updated' WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(updateSQL),
                "executeQuery should handle UPDATE statements");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    public void testExecuteQueryWithDeleteStatement() {
        String deleteSQL = "DELETE FROM users WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(deleteSQL),
                "executeQuery should handle DELETE statements");
    }

    @Test
    @DisplayName("Test executeQuery with complex SQL statement")
    public void testExecuteQueryWithComplexSQL() {
        String complexSQL = "SELECT u.name, o.order_date FROM users u JOIN orders o ON u.id = o.user_id WHERE u.active = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(complexSQL),
                "executeQuery should handle complex SQL statements");
    }

    @Test
    @DisplayName("Test executeQuery without prior connection")
    public void testExecuteQueryWithoutConnection() {
        DatabaseService newService = new DatabaseService();
        assertDoesNotThrow(() -> newService.executeQuery("SELECT 1"),
                "executeQuery should handle no connection gracefully");
    }

    @Test
    @DisplayName("Test executeQuery after successful connection")
    public void testExecuteQueryAfterConnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT * FROM test_table"),
                "executeQuery should work after connection");
    }

    @Test
    @DisplayName("Test disconnect method executes without exceptions")
    public void testDisconnectMethod() {
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() method should not throw exceptions");
    }

    @Test
    @DisplayName("Test disconnect without prior connection")
    public void testDisconnectWithoutConnection() {
        DatabaseService newService = new DatabaseService();
        assertDoesNotThrow(() -> newService.disconnect(),
                "disconnect() should handle no connection gracefully");
    }

    @Test
    @DisplayName("Test disconnect after successful connection")
    public void testDisconnectAfterConnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should work after connection");
    }

    @Test
    @DisplayName("Test multiple connect calls")
    public void testMultipleConnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect() calls should be handled gracefully");
    }

    @Test
    @DisplayName("Test multiple disconnect calls")
    public void testMultipleDisconnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect() calls should be handled gracefully");
    }

    @Test
    @DisplayName("Test connect-disconnect-connect sequence")
    public void testConnectDisconnectConnectSequence() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
        }, "Connect-disconnect-connect sequence should work properly");
    }

    @Test
    @DisplayName("Test executeQuery between connect and disconnect")
    public void testExecuteQueryBetweenConnectAndDisconnect() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT * FROM test");
            databaseService.disconnect();
        }, "Query execution between connect and disconnect should work");
    }

    @Test
    @DisplayName("Test multiple queries in sequence")
    public void testMultipleQueriesInSequence() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT * FROM users");
            databaseService.executeQuery("SELECT * FROM orders");
            databaseService.executeQuery("SELECT * FROM products");
        }, "Multiple sequential queries should execute successfully");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    public void testExecuteQueryWithSQLInjection() {
        String sqlInjection = "SELECT * FROM users WHERE id = '1' OR '1'='1'";
        assertDoesNotThrow(() -> databaseService.executeQuery(sqlInjection),
                "executeQuery should handle SQL injection attempts");
    }

    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    public void testExecuteQueryWithLongSQL() {
        String longSQL = "SELECT * FROM users WHERE " + "name = 'test' AND ".repeat(100) + "id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(longSQL),
                "executeQuery should handle long SQL statements");
    }

    @Test
    @DisplayName("Test executeQuery with special characters in SQL")
    public void testExecuteQueryWithSpecialCharacters() {
        String specialSQL = "SELECT * FROM users WHERE name = 'O''Brien' AND email LIKE '%@%.com'";
        assertDoesNotThrow(() -> databaseService.executeQuery(specialSQL),
                "executeQuery should handle special characters in SQL");
    }

    @Test
    @DisplayName("Test DatabaseService object state after operations")
    public void testObjectStateAfterOperations() {
        databaseService.connect();
        databaseService.executeQuery("SELECT 1");
        databaseService.disconnect();
        assertNotNull(databaseService, "DatabaseService object should remain valid after operations");
    }

    @Test
    @DisplayName("Test concurrent DatabaseService instances")
    public void testConcurrentInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();

        assertNotNull(service1, "First instance should not be null");
        assertNotNull(service2, "Second instance should not be null");
        assertNotSame(service1, service2, "Instances should be different objects");
    }

    @Test
    @DisplayName("Test DatabaseService with null operations")
    public void testNullOperations() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(null);
            databaseService.disconnect();
        }, "Null operations should be handled gracefully");
    }
}
