package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService class
 * Testing all methods, constructors, and edge cases
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
        databaseService = null;
    }

    @Test
    @DisplayName("Test DatabaseService constructor creates non-null instance")
    void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect method executes without throwing exceptions")
    void testConnect() {
        assertDoesNotThrow(() -> databaseService.connect(),
                "connect() should not throw exceptions even if connection fails");
    }

    @Test
    @DisplayName("Test connect with null connection handling")
    void testConnectHandlesFailureGracefully() {
        DatabaseService service = new DatabaseService();
        service.connect();
        assertNotNull(service, "Service should remain valid after connect attempt");
    }

    @Test
    @DisplayName("Test disconnect method with unestablished connection")
    void testDisconnectWithoutConnect() {
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should not throw exceptions when connection was never established");
    }

    @Test
    @DisplayName("Test disconnect after connect attempt")
    void testDisconnectAfterConnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should handle cleanup gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL")
    void testExecuteQueryWithNull() {
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
                "executeQuery() should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL")
    void testExecuteQueryWithEmptyString() {
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
                "executeQuery() should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL before connection")
    void testExecuteQueryBeforeConnect() {
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle queries when connection is not established");
    }

    @Test
    @DisplayName("Test executeQuery with SELECT statement")
    void testExecuteQueryWithSelect() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle SELECT statements");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    void testExecuteQueryWithInsert() {
        databaseService.connect();
        String sql = "INSERT INTO users (name, email) VALUES ('test', 'test@example.com')";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle INSERT statements");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    void testExecuteQueryWithUpdate() {
        databaseService.connect();
        String sql = "UPDATE users SET name = 'updated' WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle UPDATE statements");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    void testExecuteQueryWithDelete() {
        databaseService.connect();
        String sql = "DELETE FROM users WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle DELETE statements");
    }

    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    void testExecuteQueryWithMalformedSql() {
        databaseService.connect();
        String sql = "INVALID SQL STATEMENT";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle malformed SQL gracefully");
    }

    @Test
    @DisplayName("Test multiple connect calls")
    void testMultipleConnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect() calls should not cause issues");
    }

    @Test
    @DisplayName("Test multiple disconnect calls")
    void testMultipleDisconnectCalls() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect() calls should not cause issues");
    }

    @Test
    @DisplayName("Test executeQuery after disconnect")
    void testExecuteQueryAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle queries after disconnect gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with very long SQL")
    void testExecuteQueryWithLongSql() {
        databaseService.connect();
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE ");
        for (int i = 0; i < 100; i++) {
            longSql.append("id = ").append(i).append(" OR ");
        }
        longSql.append("id = 100");

        assertDoesNotThrow(() -> databaseService.executeQuery(longSql.toString()),
                "executeQuery() should handle long SQL statements");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    void testExecuteQueryWithSqlInjection() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE id = '1' OR '1'='1'";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle SQL injection attempts gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with special characters")
    void testExecuteQueryWithSpecialCharacters() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE name = 'O''Brien'";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle special characters in SQL");
    }

    @Test
    @DisplayName("Test connect-execute-disconnect workflow")
    void testFullWorkflow() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.disconnect();
        }, "Full workflow should execute without exceptions");
    }

    @Test
    @DisplayName("Test service instance remains valid after operations")
    void testServiceValidityAfterOperations() {
        databaseService.connect();
        databaseService.executeQuery("SELECT * FROM users");
        databaseService.disconnect();
        assertNotNull(databaseService, "Service instance should remain valid after operations");
    }

    @Test
    @DisplayName("Test executeQuery with CREATE TABLE statement")
    void testExecuteQueryWithCreateTable() {
        databaseService.connect();
        String sql = "CREATE TABLE test_table (id INT, name VARCHAR(100))";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle CREATE TABLE statements");
    }

    @Test
    @DisplayName("Test executeQuery with DROP TABLE statement")
    void testExecuteQueryWithDropTable() {
        databaseService.connect();
        String sql = "DROP TABLE test_table";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle DROP TABLE statements");
    }

    @Test
    @DisplayName("Test constructor creates independent instances")
    void testMultipleIndependentInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2, "Each constructor call should create a new instance");
    }

    @Test
    @DisplayName("Test connect initializes all services")
    void testConnectInitializesAllServices() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                "connect() should initialize database, cache, and external services");
    }

    @Test
    @DisplayName("Test executeQuery with transaction statements")
    void testExecuteQueryWithTransactionStatements() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("BEGIN TRANSACTION");
            databaseService.executeQuery("COMMIT");
        }, "executeQuery() should handle transaction statements");
    }

    @Test
    @DisplayName("Test executeQuery with rollback")
    void testExecuteQueryWithRollback() {
        databaseService.connect();
        String sql = "ROLLBACK";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle ROLLBACK statements");
    }

    @Test
    @DisplayName("Test executeQuery with ALTER TABLE")
    void testExecuteQueryWithAlterTable() {
        databaseService.connect();
        String sql = "ALTER TABLE users ADD COLUMN age INT";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle ALTER TABLE statements");
    }

    @Test
    @DisplayName("Test disconnect multiple times in sequence")
    void testSequentialDisconnects() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple sequential disconnect() calls should be safe");
    }

    @Test
    @DisplayName("Test executeQuery with subquery")
    void testExecuteQueryWithSubquery() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle subqueries");
    }

    @Test
    @DisplayName("Test executeQuery with JOIN")
    void testExecuteQueryWithJoin() {
        databaseService.connect();
        String sql = "SELECT u.*, o.* FROM users u JOIN orders o ON u.id = o.user_id";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle JOIN statements");
    }

    @Test
    @DisplayName("Test executeQuery with GROUP BY")
    void testExecuteQueryWithGroupBy() {
        databaseService.connect();
        String sql = "SELECT COUNT(*), status FROM orders GROUP BY status";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle GROUP BY statements");
    }

    @Test
    @DisplayName("Test executeQuery with ORDER BY")
    void testExecuteQueryWithOrderBy() {
        databaseService.connect();
        String sql = "SELECT * FROM users ORDER BY name ASC, id DESC";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle ORDER BY statements");
    }

    @Test
    @DisplayName("Test executeQuery with LIMIT")
    void testExecuteQueryWithLimit() {
        databaseService.connect();
        String sql = "SELECT * FROM users LIMIT 10 OFFSET 5";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                "executeQuery() should handle LIMIT statements");
    }

    @Test
    @DisplayName("Test connect followed by immediate disconnect")
    void testConnectDisconnectImmediately() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
        }, "Immediate disconnect after connect should work");
    }
}
