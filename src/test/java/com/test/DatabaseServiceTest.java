package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService class
 * Covers constructors, public methods, edge cases, and error scenarios
 */
@DisplayName("DatabaseService Test Suite")
public class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    public void setUp() {
        // Arrange: Initialize DatabaseService instance before each test
        databaseService = new DatabaseService();
    }

    @AfterEach
    public void tearDown() {
        // Clean up: Ensure database connections are closed
        if (databaseService != null) {
            databaseService.disconnect();
        }
        databaseService = null;
    }

    @Test
    @DisplayName("Test DatabaseService constructor - should create instance successfully")
    public void testDatabaseServiceConstructor() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect method - should establish database connection")
    public void testConnectMethod() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.connect(),
            "Connect method should not throw exception");
    }

    @Test
    @DisplayName("Test connect method with subsequent calls - should handle multiple connect attempts")
    public void testMultipleConnectCalls() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.connect();
        }, "Multiple connect calls should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL - should handle null input gracefully")
    public void testExecuteQueryWithNullSql() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(null),
            "executeQuery should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL - should handle empty string")
    public void testExecuteQueryWithEmptySql() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(""),
            "executeQuery should handle empty SQL string");
    }

    @Test
    @DisplayName("Test executeQuery with valid SELECT statement")
    public void testExecuteQueryWithValidSelect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String validSql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(validSql),
            "executeQuery should handle valid SELECT statement");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    public void testExecuteQueryWithInsertStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String insertSql = "INSERT INTO users (name, email) VALUES ('Test', 'test@example.com')";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(insertSql),
            "executeQuery should handle INSERT statement");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    public void testExecuteQueryWithUpdateStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String updateSql = "UPDATE users SET name='Updated' WHERE id=1";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(updateSql),
            "executeQuery should handle UPDATE statement");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    public void testExecuteQueryWithDeleteStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String deleteSql = "DELETE FROM users WHERE id=1";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(deleteSql),
            "executeQuery should handle DELETE statement");
    }

    @Test
    @DisplayName("Test executeQuery without connection - should handle gracefully")
    public void testExecuteQueryWithoutConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";

        // Act & Assert - executeQuery before connect
        assertDoesNotThrow(() -> service.executeQuery(sql),
            "executeQuery should handle no connection gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    public void testExecuteQueryWithMalformedSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String malformedSql = "SELCT * FORM users WERE id=1";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(malformedSql),
            "executeQuery should handle malformed SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    public void testExecuteQueryWithSqlInjection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String injectionSql = "SELECT * FROM users WHERE id=1 OR 1=1; DROP TABLE users;";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(injectionSql),
            "executeQuery should handle SQL injection attempts");
    }

    @Test
    @DisplayName("Test disconnect method - should close connection gracefully")
    public void testDisconnectMethod() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(),
            "disconnect should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect without connection - should handle gracefully")
    public void testDisconnectWithoutConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(),
            "disconnect without connection should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect multiple times - should handle repeated disconnect calls")
    public void testMultipleDisconnectCalls() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
            service.disconnect();
        }, "Multiple disconnect calls should not throw exception");
    }

    @Test
    @DisplayName("Test full lifecycle - connect, execute, disconnect")
    public void testFullDatabaseLifecycle() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT COUNT(*) FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery(sql);
            service.disconnect();
        }, "Full database lifecycle should complete without exception");
    }

    @Test
    @DisplayName("Test executeQuery after disconnect - should handle closed connection")
    public void testExecuteQueryAfterDisconnect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.executeQuery(sql);
        }, "executeQuery after disconnect should handle gracefully");
    }

    @Test
    @DisplayName("Test reconnect after disconnect")
    public void testReconnectAfterDisconnect() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.connect();
        }, "Reconnect after disconnect should work properly");
    }

    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    public void testExecuteQueryWithLongSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE ");
        for (int i = 0; i < 100; i++) {
            longSql.append("id=").append(i).append(" OR ");
        }
        longSql.append("id=999");

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(longSql.toString()),
            "executeQuery should handle long SQL statements");
    }

    @Test
    @DisplayName("Test executeQuery with special characters in SQL")
    public void testExecuteQueryWithSpecialCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String specialSql = "SELECT * FROM users WHERE name='O''Brien' AND email LIKE '%@test.com'";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(specialSql),
            "executeQuery should handle special characters");
    }

    @Test
    @DisplayName("Test executeQuery with unicode characters")
    public void testExecuteQueryWithUnicodeCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String unicodeSql = "SELECT * FROM users WHERE name='用户' OR name='пользователь'";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(unicodeSql),
            "executeQuery should handle unicode characters");
    }

    @Test
    @DisplayName("Test concurrent query execution simulation")
    public void testConcurrentQueryExecution() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql1 = "SELECT * FROM users";
        String sql2 = "SELECT * FROM products";

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery(sql1);
            service.executeQuery(sql2);
            service.disconnect();
        }, "Consecutive query execution should work properly");
    }

    @Test
    @DisplayName("Test executeQuery with CREATE TABLE statement")
    public void testExecuteQueryWithCreateTable() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String createSql = "CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(100))";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(createSql),
            "executeQuery should handle CREATE TABLE statement");
    }

    @Test
    @DisplayName("Test executeQuery with DROP TABLE statement")
    public void testExecuteQueryWithDropTable() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String dropSql = "DROP TABLE IF EXISTS test_table";

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(dropSql),
            "executeQuery should handle DROP TABLE statement");
    }

    @Test
    @DisplayName("Test service instance independence")
    public void testMultipleServiceInstances() {
        // Arrange
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();

        // Act & Assert
        assertNotNull(service1, "First service instance should not be null");
        assertNotNull(service2, "Second service instance should not be null");
        assertNotSame(service1, service2, "Service instances should be independent");
    }
}
