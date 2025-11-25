package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit test for DatabaseService class
 * Tests all public methods, constructors, and edge cases
 */
@DisplayName("DatabaseService Test Suite")
public class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    public void setUp() {
        // Arrange: Initialize DatabaseService before each test
        databaseService = new DatabaseService();
    }

    @AfterEach
    public void tearDown() {
        // Clean up: Disconnect after each test
        if (databaseService != null) {
            databaseService.disconnect();
        }
    }

    @Test
    @DisplayName("Test constructor - should create DatabaseService instance")
    public void testConstructor() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect - should attempt database connection")
    public void testConnect() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> service.connect(),
            "connect() should not throw exception even if connection fails");
    }

    @Test
    @DisplayName("Test connect - multiple connections should not throw exception")
    public void testConnectMultipleTimes() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.connect(); // Second connection attempt
        }, "Multiple connect calls should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL - should handle gracefully")
    public void testExecuteQueryWithNullSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(null),
            "executeQuery with null SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL - should handle gracefully")
    public void testExecuteQueryWithEmptySql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(""),
            "executeQuery with empty SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL - should execute without exception")
    public void testExecuteQueryWithValidSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String validSql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(validSql),
            "executeQuery with valid SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with CREATE TABLE SQL")
    public void testExecuteQueryWithCreateTableSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String createTableSql = "CREATE TABLE test_table (id INT, name VARCHAR(100))";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(createTableSql),
            "executeQuery with CREATE TABLE should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT SQL")
    public void testExecuteQueryWithInsertSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String insertSql = "INSERT INTO users (id, name) VALUES (1, 'John')";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(insertSql),
            "executeQuery with INSERT should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE SQL")
    public void testExecuteQueryWithUpdateSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String updateSql = "UPDATE users SET name = 'Jane' WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(updateSql),
            "executeQuery with UPDATE should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE SQL")
    public void testExecuteQueryWithDeleteSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String deleteSql = "DELETE FROM users WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(deleteSql),
            "executeQuery with DELETE should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery without connection - should handle gracefully")
    public void testExecuteQueryWithoutConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        // Not calling connect()

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery("SELECT * FROM users"),
            "executeQuery without connection should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with malformed SQL - should handle gracefully")
    public void testExecuteQueryWithMalformedSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String malformedSql = "INVALID SQL STATEMENT";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(malformedSql),
            "executeQuery with malformed SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt - should handle gracefully")
    public void testExecuteQueryWithSqlInjection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String injectionSql = "SELECT * FROM users WHERE id = 1; DROP TABLE users;--";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(injectionSql),
            "executeQuery with SQL injection should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect - should close connection gracefully")
    public void testDisconnect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();

        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(),
            "disconnect() should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect without connection - should handle gracefully")
    public void testDisconnectWithoutConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        // Not calling connect()

        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(),
            "disconnect() without connection should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect multiple times - should handle gracefully")
    public void testDisconnectMultipleTimes() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect(); // Second disconnect
            service.disconnect(); // Third disconnect
        }, "Multiple disconnect calls should not throw exception");
    }

    @Test
    @DisplayName("Test full workflow: connect -> execute -> disconnect")
    public void testFullWorkflow() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery("SELECT * FROM users");
            service.executeQuery("INSERT INTO users VALUES (1, 'Test')");
            service.disconnect();
        }, "Full workflow should execute without exception");
    }

    @Test
    @DisplayName("Test multiple queries in sequence")
    public void testMultipleQueriesInSequence() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT * FROM table1");
            service.executeQuery("SELECT * FROM table2");
            service.executeQuery("SELECT * FROM table3");
        }, "Multiple sequential queries should execute without exception");
    }

    @Test
    @DisplayName("Test reconnection after disconnect")
    public void testReconnectionAfterDisconnect() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.connect(); // Reconnect
            service.executeQuery("SELECT 1");
        }, "Reconnection after disconnect should work without exception");
    }

    @Test
    @DisplayName("Test executeQuery with long SQL statement")
    public void testExecuteQueryWithLongSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 100; i++) {
            longSql.append(i);
            if (i < 99) longSql.append(",");
        }
        longSql.append(")");

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(longSql.toString()),
            "executeQuery with long SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with special characters in SQL")
    public void testExecuteQueryWithSpecialCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sqlWithSpecialChars = "SELECT * FROM users WHERE name = 'O''Brien'";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sqlWithSpecialChars),
            "executeQuery with special characters should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with Unicode characters")
    public void testExecuteQueryWithUnicodeCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sqlWithUnicode = "SELECT * FROM users WHERE name = '日本語'";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sqlWithUnicode),
            "executeQuery with Unicode characters should not throw exception");
    }

    @Test
    @DisplayName("Test behavior consistency across multiple instances")
    public void testMultipleInstances() {
        // Arrange
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service1.connect();
            service2.connect();
            service1.executeQuery("SELECT 1");
            service2.executeQuery("SELECT 2");
            service1.disconnect();
            service2.disconnect();
        }, "Multiple instances should work independently");
    }

    @Test
    @DisplayName("Test executeQuery after failed connection")
    public void testExecuteQueryAfterFailedConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect(); // This will fail to connect to actual database

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery("SELECT * FROM users"),
            "executeQuery after failed connection should handle gracefully");
    }
}
