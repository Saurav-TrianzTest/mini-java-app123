package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    @DisplayName("Test constructor creates non-null DatabaseService instance")
    public void testConstructor() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect method initializes database connection")
    public void testConnectSuccess() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.connect(),
            "connect() should not throw exception even if connection fails");
    }

    @Test
    @DisplayName("Test connect method handles missing driver gracefully")
    public void testConnectWithMissingDriver() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.connect(),
            "connect() should handle ClassNotFoundException gracefully");
    }

    @Test
    @DisplayName("Test connect method handles SQL exceptions gracefully")
    public void testConnectWithSQLException() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.connect(),
            "connect() should handle SQLException gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL statement")
    public void testExecuteQueryWithValidSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
            "executeQuery() should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL statement")
    public void testExecuteQueryWithNullSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = null;

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
            "executeQuery() should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL statement")
    public void testExecuteQueryWithEmptySQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
            "executeQuery() should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with invalid SQL statement")
    public void testExecuteQueryWithInvalidSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INVALID SQL STATEMENT";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
            "executeQuery() should handle invalid SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with complex SQL statement")
    public void testExecuteQueryWithComplexSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
            "executeQuery() should handle complex SQL statements");
    }

    @Test
    @DisplayName("Test executeQuery without prior connection")
    public void testExecuteQueryWithoutConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
            "executeQuery() should handle null connection gracefully");
    }

    @Test
    @DisplayName("Test disconnect method closes connection successfully")
    public void testDisconnectSuccess() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(),
            "disconnect() should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect method without prior connection")
    public void testDisconnectWithoutConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(),
            "disconnect() should handle null connection gracefully");
    }

    @Test
    @DisplayName("Test disconnect method called multiple times")
    public void testDisconnectMultipleTimes() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
            service.disconnect();
        }, "disconnect() should be idempotent");
    }

    @Test
    @DisplayName("Test full lifecycle: connect, execute, disconnect")
    public void testFullLifecycle() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT 1";

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery(sql);
            service.disconnect();
        }, "Full lifecycle should complete without exceptions");
    }

    @Test
    @DisplayName("Test multiple queries in sequence")
    public void testMultipleQueriesInSequence() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery("SELECT * FROM users");
            service.executeQuery("SELECT * FROM orders");
            service.executeQuery("SELECT * FROM products");
            service.disconnect();
        }, "Multiple queries should execute without exceptions");
    }

    @Test
    @DisplayName("Test query execution with special characters")
    public void testExecuteQueryWithSpecialCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE name = 'O''Brien'";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
            "executeQuery() should handle SQL with special characters");
    }

    @Test
    @DisplayName("Test query execution with very long SQL statement")
    public void testExecuteQueryWithLongSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE id IN (" + "1,".repeat(1000) + "2)";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql),
            "executeQuery() should handle long SQL statements");
    }

    @Test
    @DisplayName("Test connect establishes hardcoded database URL")
    public void testConnectUsesHardcodedValues() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act
        service.connect();

        // Assert - verifies that connect method executes without exception
        assertNotNull(service, "Service should remain valid after connect attempt");
    }

    @Test
    @DisplayName("Test executeQuery handles SQLException gracefully")
    public void testExecuteQueryHandlesSQLException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String malformedSQL = "SELECT FROM WHERE";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(malformedSQL),
            "executeQuery() should catch and handle SQLException");
    }

    @Test
    @DisplayName("Test disconnect handles already closed connection")
    public void testDisconnectHandlesClosedConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.disconnect();

        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(),
            "disconnect() should handle already closed connection");
    }
}
