package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService class
 * Tests all methods including connection handling, query execution, and disconnection
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
     * Test constructor - verify DatabaseService instance can be created
     */
    @Test
    public void testConstructor() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should be created");
    }

    /**
     * Test constructor multiple times - verify multiple instances can be created
     */
    @Test
    public void testConstructorMultipleInstances() {
        // Arrange & Act
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();

        // Assert
        assertNotNull(service1, "First DatabaseService instance should be created");
        assertNotNull(service2, "Second DatabaseService instance should be created");
        assertNotEquals(service1, service2, "Instances should be different objects");
    }

    /**
     * Test connect method - verify connection attempt is made without throwing exception
     */
    @Test
    public void testConnect() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should not throw exception even if connection fails");
    }

    /**
     * Test connect method multiple times - verify multiple connection attempts
     */
    @Test
    public void testConnectMultipleTimes() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect calls should not throw exception");
    }

    /**
     * Test executeQuery with valid SQL - verify query execution doesn't throw exception
     */
    @Test
    public void testExecuteQueryWithValidSQL() {
        // Arrange
        String sql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle SQL gracefully");
    }

    /**
     * Test executeQuery with null SQL - verify null handling
     */
    @Test
    public void testExecuteQueryWithNullSQL() {
        // Arrange
        String sql = null;

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle null SQL gracefully");
    }

    /**
     * Test executeQuery with empty SQL - verify empty string handling
     */
    @Test
    public void testExecuteQueryWithEmptySQL() {
        // Arrange
        String sql = "";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle empty SQL gracefully");
    }

    /**
     * Test executeQuery with SELECT statement
     */
    @Test
    public void testExecuteQueryWithSelectStatement() {
        // Arrange
        String sql = "SELECT id, name, email FROM users WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle SELECT statements");
    }

    /**
     * Test executeQuery with INSERT statement
     */
    @Test
    public void testExecuteQueryWithInsertStatement() {
        // Arrange
        String sql = "INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com')";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle INSERT statements");
    }

    /**
     * Test executeQuery with UPDATE statement
     */
    @Test
    public void testExecuteQueryWithUpdateStatement() {
        // Arrange
        String sql = "UPDATE users SET name = 'Jane Doe' WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle UPDATE statements");
    }

    /**
     * Test executeQuery with DELETE statement
     */
    @Test
    public void testExecuteQueryWithDeleteStatement() {
        // Arrange
        String sql = "DELETE FROM users WHERE id = 1";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle DELETE statements");
    }

    /**
     * Test executeQuery with complex SQL
     */
    @Test
    public void testExecuteQueryWithComplexSQL() {
        // Arrange
        String sql = "SELECT u.id, u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id WHERE u.active = true";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle complex SQL statements");
    }

    /**
     * Test executeQuery with malformed SQL
     */
    @Test
    public void testExecuteQueryWithMalformedSQL() {
        // Arrange
        String sql = "INVALID SQL STATEMENT;;;";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle malformed SQL gracefully");
    }

    /**
     * Test executeQuery with very long SQL
     */
    @Test
    public void testExecuteQueryWithLongSQL() {
        // Arrange
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSql.append(i);
            if (i < 999) longSql.append(",");
        }
        longSql.append(")");

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(longSql.toString());
        }, "Execute query should handle long SQL statements");
    }

    /**
     * Test executeQuery before connect - verify behavior when connection is not established
     */
    @Test
    public void testExecuteQueryBeforeConnect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.executeQuery(sql);
        }, "Execute query before connect should not throw exception");
    }

    /**
     * Test disconnect method - verify disconnect doesn't throw exception
     */
    @Test
    public void testDisconnect() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect method should not throw exception");
    }

    /**
     * Test disconnect without connect - verify disconnecting without prior connection
     */
    @Test
    public void testDisconnectWithoutConnect() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
        }, "Disconnect without connect should not throw exception");
    }

    /**
     * Test disconnect multiple times - verify multiple disconnect calls
     */
    @Test
    public void testDisconnectMultipleTimes() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect calls should not throw exception");
    }

    /**
     * Test full lifecycle: connect, execute, disconnect
     */
    @Test
    public void testFullLifecycle() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery(sql);
            service.disconnect();
        }, "Full lifecycle should execute without exception");
    }

    /**
     * Test multiple queries in sequence
     */
    @Test
    public void testMultipleQueriesInSequence() {
        // Arrange
        String sql1 = "SELECT * FROM users";
        String sql2 = "SELECT * FROM orders";
        String sql3 = "SELECT * FROM products";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql1);
            databaseService.executeQuery(sql2);
            databaseService.executeQuery(sql3);
        }, "Multiple queries should execute in sequence");
    }

    /**
     * Test executeQuery with SQL containing special characters
     */
    @Test
    public void testExecuteQueryWithSpecialCharacters() {
        // Arrange
        String sql = "SELECT * FROM users WHERE name LIKE '%O''Reilly%'";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle special characters in SQL");
    }

    /**
     * Test executeQuery with SQL containing unicode characters
     */
    @Test
    public void testExecuteQueryWithUnicodeCharacters() {
        // Arrange
        String sql = "SELECT * FROM users WHERE name = '中文測試'";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle unicode characters in SQL");
    }

    /**
     * Test connect and disconnect cycle multiple times
     */
    @Test
    public void testConnectDisconnectCycle() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.connect();
            service.disconnect();
        }, "Connect-disconnect cycle should work multiple times");
    }

    /**
     * Test executeQuery with CREATE TABLE statement
     */
    @Test
    public void testExecuteQueryWithCreateTable() {
        // Arrange
        String sql = "CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(100))";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle CREATE TABLE statements");
    }

    /**
     * Test executeQuery with DROP TABLE statement
     */
    @Test
    public void testExecuteQueryWithDropTable() {
        // Arrange
        String sql = "DROP TABLE IF EXISTS test_table";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle DROP TABLE statements");
    }

    /**
     * Test executeQuery with ALTER TABLE statement
     */
    @Test
    public void testExecuteQueryWithAlterTable() {
        // Arrange
        String sql = "ALTER TABLE users ADD COLUMN age INT";

        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle ALTER TABLE statements");
    }
}
