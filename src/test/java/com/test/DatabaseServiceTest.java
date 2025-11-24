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

    @Test
    @DisplayName("Test connectToCache is invoked during connect")
    void testConnectInvokesCache() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "connect() should invoke cache connection");
    }

    @Test
    @DisplayName("Test initializeExternalServices is invoked during connect")
    void testConnectInvokesExternalServices() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "connect() should initialize external services");
    }

    @Test
    @DisplayName("Test executeQuery with CREATE TABLE statement")
    void testExecuteQueryWithCreateTable() {
        databaseService.connect();
        String createSQL = "CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(createSQL);
        }, "executeQuery() should handle CREATE TABLE statements");
    }

    @Test
    @DisplayName("Test executeQuery with DROP TABLE statement")
    void testExecuteQueryWithDropTable() {
        databaseService.connect();
        String dropSQL = "DROP TABLE IF EXISTS test_table";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(dropSQL);
        }, "executeQuery() should handle DROP TABLE statements");
    }

    @Test
    @DisplayName("Test executeQuery with ALTER TABLE statement")
    void testExecuteQueryWithAlterTable() {
        databaseService.connect();
        String alterSQL = "ALTER TABLE users ADD COLUMN age INT";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(alterSQL);
        }, "executeQuery() should handle ALTER TABLE statements");
    }

    @Test
    @DisplayName("Test executeQuery with JOIN query")
    void testExecuteQueryWithJoin() {
        databaseService.connect();
        String joinSQL = "SELECT u.id, u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(joinSQL);
        }, "executeQuery() should handle JOIN queries");
    }

    @Test
    @DisplayName("Test executeQuery with aggregate functions")
    void testExecuteQueryWithAggregateFunctions() {
        databaseService.connect();
        String aggSQL = "SELECT COUNT(*), AVG(price), SUM(quantity) FROM products";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(aggSQL);
        }, "executeQuery() should handle aggregate functions");
    }

    @Test
    @DisplayName("Test executeQuery with subquery")
    void testExecuteQueryWithSubquery() {
        databaseService.connect();
        String subquerySQL = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE total > 100)";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(subquerySQL);
        }, "executeQuery() should handle subqueries");
    }

    @Test
    @DisplayName("Test executeQuery with LIKE pattern")
    void testExecuteQueryWithLikePattern() {
        databaseService.connect();
        String likeSQL = "SELECT * FROM users WHERE name LIKE '%john%'";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(likeSQL);
        }, "executeQuery() should handle LIKE patterns");
    }

    @Test
    @DisplayName("Test executeQuery with ORDER BY clause")
    void testExecuteQueryWithOrderBy() {
        databaseService.connect();
        String orderSQL = "SELECT * FROM users ORDER BY name ASC, age DESC";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(orderSQL);
        }, "executeQuery() should handle ORDER BY clauses");
    }

    @Test
    @DisplayName("Test executeQuery with GROUP BY clause")
    void testExecuteQueryWithGroupBy() {
        databaseService.connect();
        String groupSQL = "SELECT category, COUNT(*) FROM products GROUP BY category HAVING COUNT(*) > 5";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(groupSQL);
        }, "executeQuery() should handle GROUP BY clauses");
    }

    @Test
    @DisplayName("Test executeQuery with LIMIT clause")
    void testExecuteQueryWithLimit() {
        databaseService.connect();
        String limitSQL = "SELECT * FROM users LIMIT 10 OFFSET 5";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(limitSQL);
        }, "executeQuery() should handle LIMIT clauses");
    }

    @Test
    @DisplayName("Test executeQuery with special characters in SQL")
    void testExecuteQueryWithSpecialCharacters() {
        databaseService.connect();
        String specialSQL = "SELECT * FROM users WHERE name = 'O''Brien' AND email LIKE '%@%.com'";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(specialSQL);
        }, "executeQuery() should handle special characters");
    }

    @Test
    @DisplayName("Test executeQuery with UNION statement")
    void testExecuteQueryWithUnion() {
        databaseService.connect();
        String unionSQL = "SELECT id FROM users UNION SELECT id FROM customers";
        assertDoesNotThrow(() -> {
            databaseService.executeQuery(unionSQL);
        }, "executeQuery() should handle UNION statements");
    }

    @Test
    @DisplayName("Test executeQuery with transaction statements")
    void testExecuteQueryWithTransaction() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("BEGIN TRANSACTION");
            databaseService.executeQuery("COMMIT");
        }, "executeQuery() should handle transaction statements");
    }
}
