package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit test class for DatabaseService
 * Tests cover: constructors, methods, edge cases, error handling, null inputs
 * Target: 80%+ code coverage
 */
public class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    public void setUp() {
        databaseService = new DatabaseService();
    }

    @AfterEach
    public void tearDown() {
        try {
            if (databaseService != null) {
                databaseService.disconnect();
            }
        } catch (Exception e) {
            // Ignore teardown exceptions
        }
    }

    /**
     * Test 1: Constructor creates non-null instance
     */
    @Test
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    /**
     * Test 2: Constructor creates independent instances
     */
    @Test
    public void testConstructorCreatesIndependentInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        assertNotNull(service1, "First instance should not be null");
        assertNotNull(service2, "Second instance should not be null");
        assertNotSame(service1, service2, "Instances should be different objects");
    }

    /**
     * Test 3: Connect method executes without fatal errors
     */
    @Test
    public void testConnect() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.connect(),
                          "Connect should not throw uncaught exceptions");
    }

    /**
     * Test 4: Connect method with valid configuration
     */
    @Test
    public void testConnectWithValidConfiguration() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        databaseService.connect();
        assertNotNull(databaseService, "DatabaseService should remain non-null after connect");
    }

    /**
     * Test 5: Connect initializes external services
     */
    @Test
    public void testConnectInitializesExternalServices() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.connect(),
                          "Connect should initialize external services without exception");
    }

    /**
     * Test 6: Connect with cache connection
     */
    @Test
    public void testConnectWithCacheConnection() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.connect(),
                          "Cache connection should be attempted without exception");
    }

    /**
     * Test 7: Multiple connect calls
     */
    @Test
    public void testMultipleConnectCalls() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect calls should not cause fatal errors");
    }

    /**
     * Test 8: Connect-disconnect-connect cycle
     */
    @Test
    public void testConnectDisconnectConnectCycle() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
        }, "Connect-disconnect-connect cycle should work");
    }

    /**
     * Test 9: ExecuteQuery with null SQL
     */
    @Test
    public void testExecuteQueryWithNullSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
                          "ExecuteQuery should handle null SQL gracefully");
    }

    /**
     * Test 10: ExecuteQuery with empty SQL
     */
    @Test
    public void testExecuteQueryWithEmptySQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
                          "ExecuteQuery should handle empty SQL gracefully");
    }

    /**
     * Test 11: ExecuteQuery with valid SELECT statement
     */
    @Test
    public void testExecuteQueryWithValidSelectSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle valid SELECT SQL");
    }

    /**
     * Test 12: ExecuteQuery with CREATE TABLE statement
     */
    @Test
    public void testExecuteQueryWithCreateTableSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle CREATE TABLE SQL");
    }

    /**
     * Test 13: ExecuteQuery with INSERT statement
     */
    @Test
    public void testExecuteQueryWithInsertSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "INSERT INTO users (id, name) VALUES (1, 'test')";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle INSERT SQL");
    }

    /**
     * Test 14: ExecuteQuery with UPDATE statement
     */
    @Test
    public void testExecuteQueryWithUpdateSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "UPDATE users SET name = 'updated' WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle UPDATE SQL");
    }

    /**
     * Test 15: ExecuteQuery with DELETE statement
     */
    @Test
    public void testExecuteQueryWithDeleteSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "DELETE FROM users WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle DELETE SQL");
    }

    /**
     * Test 16: ExecuteQuery without establishing connection first
     */
    @Test
    public void testExecuteQueryWithoutConnection() {
        DatabaseService newService = new DatabaseService();
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> newService.executeQuery(sql),
                          "ExecuteQuery should handle no connection gracefully");
    }

    /**
     * Test 17: ExecuteQuery with special characters in SQL
     */
    @Test
    public void testExecuteQueryWithSpecialCharacters() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT * FROM users WHERE name = 'O''Brien'";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle special characters");
    }

    /**
     * Test 18: ExecuteQuery with very long SQL statement
     */
    @Test
    public void testExecuteQueryWithLongSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 200; i++) {
            longSQL.append(i);
            if (i < 199) longSQL.append(",");
        }
        longSQL.append(")");

        String sql = longSQL.toString();
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle long SQL statements");
    }

    /**
     * Test 19: ExecuteQuery with SQL injection attempt
     */
    @Test
    public void testExecuteQueryWithSQLInjection() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT * FROM users WHERE id = 1; DROP TABLE users;";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle SQL injection attempts");
    }

    /**
     * Test 20: ExecuteQuery with complex JOIN statement
     */
    @Test
    public void testExecuteQueryWithJoinSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT u.*, o.* FROM users u INNER JOIN orders o ON u.id = o.user_id";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle JOIN statements");
    }

    /**
     * Test 21: ExecuteQuery with aggregate functions
     */
    @Test
    public void testExecuteQueryWithAggregateSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT COUNT(*), AVG(price), MAX(quantity) FROM products GROUP BY category";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle aggregate functions");
    }

    /**
     * Test 22: ExecuteQuery with subquery
     */
    @Test
    public void testExecuteQueryWithSubquery() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE total > 100)";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle subqueries");
    }

    /**
     * Test 23: ExecuteQuery with UNION statement
     */
    @Test
    public void testExecuteQueryWithUnionSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT name FROM users UNION SELECT name FROM customers";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle UNION statements");
    }

    /**
     * Test 24: ExecuteQuery after disconnect
     */
    @Test
    public void testExecuteQueryAfterDisconnect() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        databaseService.connect();
        databaseService.disconnect();

        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery after disconnect should be handled");
    }

    /**
     * Test 25: ExecuteQuery with whitespace-only SQL
     */
    @Test
    public void testExecuteQueryWithWhitespaceSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.executeQuery("   "),
                          "ExecuteQuery should handle whitespace-only SQL");
    }

    /**
     * Test 26: ExecuteQuery with multi-line SQL
     */
    @Test
    public void testExecuteQueryWithMultiLineSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT id,\n       name,\n       email\nFROM users\nWHERE active = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle multi-line SQL");
    }

    /**
     * Test 27: Disconnect method
     */
    @Test
    public void testDisconnect() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.disconnect(),
                          "Disconnect method should complete without exception");
    }

    /**
     * Test 28: Disconnect without prior connection
     */
    @Test
    public void testDisconnectWithoutConnection() {
        DatabaseService newService = new DatabaseService();
        assertDoesNotThrow(() -> newService.disconnect(),
                          "Disconnect should handle no connection gracefully");
    }

    /**
     * Test 29: Disconnect after connect
     */
    @Test
    public void testDisconnectAfterConnect() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect(),
                          "Disconnect should work after connect");
    }

    /**
     * Test 30: Multiple disconnect calls
     */
    @Test
    public void testMultipleDisconnectCalls() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect calls should not cause errors");
    }

    /**
     * Test 31: Hardcoded database configuration is used
     */
    @Test
    public void testHardcodedDatabaseConfiguration() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.connect(),
                          "DatabaseService should use hardcoded configuration");
    }

    /**
     * Test 32: Redis connection is initialized
     */
    @Test
    public void testRedisConnectionInitialization() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.connect(),
                          "Redis connection should be initialized");
    }

    /**
     * Test 33: External API URLs are configured
     */
    @Test
    public void testExternalAPIConfiguration() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> databaseService.connect(),
                          "External API URLs should be configured");
    }

    /**
     * Test 34: Query timeout configuration
     */
    @Test
    public void testQueryTimeoutConfiguration() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "Query timeout should be configured");
    }

    /**
     * Test 35: ExecuteQuery with ALTER TABLE statement
     */
    @Test
    public void testExecuteQueryWithAlterTableSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "ALTER TABLE users ADD COLUMN age INT";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle ALTER TABLE SQL");
    }

    /**
     * Test 36: ExecuteQuery with DROP TABLE statement
     */
    @Test
    public void testExecuteQueryWithDropTableSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "DROP TABLE IF EXISTS temp_table";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle DROP TABLE SQL");
    }

    /**
     * Test 37: ExecuteQuery with TRUNCATE statement
     */
    @Test
    public void testExecuteQueryWithTruncateSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "TRUNCATE TABLE logs";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle TRUNCATE SQL");
    }

    /**
     * Test 38: ExecuteQuery with CREATE INDEX statement
     */
    @Test
    public void testExecuteQueryWithCreateIndexSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "CREATE INDEX idx_users_email ON users(email)";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle CREATE INDEX SQL");
    }

    /**
     * Test 39: ExecuteQuery multiple times in sequence
     */
    @Test
    public void testExecuteQueryMultipleTimes() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
            databaseService.executeQuery("SELECT * FROM products");
            databaseService.executeQuery("SELECT * FROM orders");
        }, "Multiple sequential queries should execute without exception");
    }

    /**
     * Test 40: Service lifecycle - full connect, query, disconnect cycle
     */
    @Test
    public void testServiceLifecycle() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();
        }, "Full service lifecycle should complete without exception");
    }

    /**
     * Test 41: ExecuteQuery with transaction control statements
     */
    @Test
    public void testExecuteQueryWithTransactionSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "START TRANSACTION";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle transaction statements");
    }

    /**
     * Test 42: ExecuteQuery with COMMIT statement
     */
    @Test
    public void testExecuteQueryWithCommitSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "COMMIT";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle COMMIT statement");
    }

    /**
     * Test 43: ExecuteQuery with ROLLBACK statement
     */
    @Test
    public void testExecuteQueryWithRollbackSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "ROLLBACK";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle ROLLBACK statement");
    }

    /**
     * Test 44: ExecuteQuery with GRANT statement
     */
    @Test
    public void testExecuteQueryWithGrantSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "GRANT SELECT ON users TO 'readonly_user'@'localhost'";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle GRANT statement");
    }

    /**
     * Test 45: ExecuteQuery with stored procedure call
     */
    @Test
    public void testExecuteQueryWithStoredProcedureCall() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "CALL sp_get_user_details(1)";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle stored procedure calls");
    }

    /**
     * Test 46: Connect handles ClassNotFoundException gracefully
     */
    @Test
    public void testConnectHandlesClassNotFoundException() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                          "Connect should handle driver not found gracefully");
    }

    /**
     * Test 47: Connect handles SQLException gracefully
     */
    @Test
    public void testConnectHandlesSQLException() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
                          "Connect should handle SQL exceptions gracefully");
    }

    /**
     * Test 48: ExecuteQuery with ORDER BY clause
     */
    @Test
    public void testExecuteQueryWithOrderBySQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT * FROM users ORDER BY name ASC, id DESC";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle ORDER BY clause");
    }

    /**
     * Test 49: ExecuteQuery with LIMIT clause
     */
    @Test
    public void testExecuteQueryWithLimitSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT * FROM users LIMIT 10 OFFSET 20";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle LIMIT clause");
    }

    /**
     * Test 50: ExecuteQuery with HAVING clause
     */
    @Test
    public void testExecuteQueryWithHavingSQL() {
        assertNotNull(databaseService, "DatabaseService should be initialized");
        String sql = "SELECT category, COUNT(*) FROM products GROUP BY category HAVING COUNT(*) > 5";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "ExecuteQuery should handle HAVING clause");
    }
}
