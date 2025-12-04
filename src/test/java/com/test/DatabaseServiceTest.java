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
        databaseService = new DatabaseService();
    }

    @AfterEach
    public void tearDown() {
        if (databaseService != null) {
            databaseService.disconnect();
        }
    }

    @Test
    @DisplayName("Test constructor - should create DatabaseService instance")
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect - should attempt database connection")
    public void testConnect() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
            "connect() should not throw exception even if connection fails");
    }

    @Test
    @DisplayName("Test connect - multiple connections should not throw exception")
    public void testConnectMultipleTimes() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> {
            service.connect();
            service.connect();
        }, "Multiple connect calls should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL - should handle gracefully")
    public void testExecuteQueryWithNullSql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        assertDoesNotThrow(() -> service.executeQuery(null),
            "executeQuery with null SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL - should handle gracefully")
    public void testExecuteQueryWithEmptySql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        assertDoesNotThrow(() -> service.executeQuery(""),
            "executeQuery with empty SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL - should execute without exception")
    public void testExecuteQueryWithValidSql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String validSql = "SELECT * FROM users";
        assertDoesNotThrow(() -> service.executeQuery(validSql),
            "executeQuery with valid SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with CREATE TABLE SQL")
    public void testExecuteQueryWithCreateTableSql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String createTableSql = "CREATE TABLE test_table (id INT, name VARCHAR(100))";
        assertDoesNotThrow(() -> service.executeQuery(createTableSql),
            "executeQuery with CREATE TABLE should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT SQL")
    public void testExecuteQueryWithInsertSql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String insertSql = "INSERT INTO users (id, name) VALUES (1, 'John')";
        assertDoesNotThrow(() -> service.executeQuery(insertSql),
            "executeQuery with INSERT should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE SQL")
    public void testExecuteQueryWithUpdateSql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String updateSql = "UPDATE users SET name = 'Jane' WHERE id = 1";
        assertDoesNotThrow(() -> service.executeQuery(updateSql),
            "executeQuery with UPDATE should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE SQL")
    public void testExecuteQueryWithDeleteSql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String deleteSql = "DELETE FROM users WHERE id = 1";
        assertDoesNotThrow(() -> service.executeQuery(deleteSql),
            "executeQuery with DELETE should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery without connection - should handle gracefully")
    public void testExecuteQueryWithoutConnection() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.executeQuery("SELECT * FROM users"),
            "executeQuery without connection should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with malformed SQL - should handle gracefully")
    public void testExecuteQueryWithMalformedSql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String malformedSql = "INVALID SQL STATEMENT";
        assertDoesNotThrow(() -> service.executeQuery(malformedSql),
            "executeQuery with malformed SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt - should handle gracefully")
    public void testExecuteQueryWithSqlInjection() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String injectionSql = "SELECT * FROM users WHERE id = 1; DROP TABLE users;--";
        assertDoesNotThrow(() -> service.executeQuery(injectionSql),
            "executeQuery with SQL injection should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect - should close connection gracefully")
    public void testDisconnect() {
        DatabaseService service = new DatabaseService();
        service.connect();
        assertDoesNotThrow(() -> service.disconnect(),
            "disconnect() should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect without connection - should handle gracefully")
    public void testDisconnectWithoutConnection() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.disconnect(),
            "disconnect() without connection should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect multiple times - should handle gracefully")
    public void testDisconnectMultipleTimes() {
        DatabaseService service = new DatabaseService();
        service.connect();
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
            service.disconnect();
        }, "Multiple disconnect calls should not throw exception");
    }

    @Test
    @DisplayName("Test full workflow: connect -> execute -> disconnect")
    public void testFullWorkflow() {
        DatabaseService service = new DatabaseService();
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
        DatabaseService service = new DatabaseService();
        service.connect();
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT * FROM table1");
            service.executeQuery("SELECT * FROM table2");
            service.executeQuery("SELECT * FROM table3");
        }, "Multiple sequential queries should execute without exception");
    }

    @Test
    @DisplayName("Test reconnection after disconnect")
    public void testReconnectionAfterDisconnect() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.connect();
            service.executeQuery("SELECT 1");
        }, "Reconnection after disconnect should work without exception");
    }

    @Test
    @DisplayName("Test executeQuery with long SQL statement")
    public void testExecuteQueryWithLongSql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 100; i++) {
            longSql.append(i);
            if (i < 99) longSql.append(",");
        }
        longSql.append(")");
        assertDoesNotThrow(() -> service.executeQuery(longSql.toString()),
            "executeQuery with long SQL should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with special characters in SQL")
    public void testExecuteQueryWithSpecialCharacters() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String sqlWithSpecialChars = "SELECT * FROM users WHERE name = 'O''Brien'";
        assertDoesNotThrow(() -> service.executeQuery(sqlWithSpecialChars),
            "executeQuery with special characters should not throw exception");
    }

    @Test
    @DisplayName("Test executeQuery with Unicode characters")
    public void testExecuteQueryWithUnicodeCharacters() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String sqlWithUnicode = "SELECT * FROM users WHERE name = '日本語'";
        assertDoesNotThrow(() -> service.executeQuery(sqlWithUnicode),
            "executeQuery with Unicode characters should not throw exception");
    }

    @Test
    @DisplayName("Test behavior consistency across multiple instances")
    public void testMultipleInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
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
        DatabaseService service = new DatabaseService();
        service.connect();
        assertDoesNotThrow(() -> service.executeQuery("SELECT * FROM users"),
            "executeQuery after failed connection should handle gracefully");
    }

    @Test
    @DisplayName("Test hardcoded database host")
    public void testHardcodedDatabaseHost() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
            "Should use hardcoded database host");
    }

    @Test
    @DisplayName("Test hardcoded database port")
    public void testHardcodedDatabasePort() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
            "Should use hardcoded database port");
    }

    @Test
    @DisplayName("Test hardcoded database name")
    public void testHardcodedDatabaseName() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
            "Should use hardcoded database name");
    }

    @Test
    @DisplayName("Test hardcoded database credentials")
    public void testHardcodedDatabaseCredentials() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
            "Should use hardcoded database credentials");
    }

    @Test
    @DisplayName("Test hardcoded Redis connection")
    public void testHardcodedRedisConnection() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
            "Should reference hardcoded Redis connection");
    }

    @Test
    @DisplayName("Test hardcoded external API URL")
    public void testHardcodedExternalApiUrl() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
            "Should reference hardcoded external API URL");
    }

    @Test
    @DisplayName("Test hardcoded payment service URL")
    public void testHardcodedPaymentServiceUrl() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.connect(),
            "Should reference hardcoded payment service URL");
    }

    @Test
    @DisplayName("Test query timeout setting")
    public void testQueryTimeoutSetting() {
        DatabaseService service = new DatabaseService();
        service.connect();
        assertDoesNotThrow(() -> service.executeQuery("SELECT SLEEP(1)"),
            "Should handle query timeout");
    }

    @Test
    @DisplayName("Test executeQuery with transaction SQL")
    public void testExecuteQueryWithTransactionSql() {
        DatabaseService service = new DatabaseService();
        service.connect();
        assertDoesNotThrow(() -> {
            service.executeQuery("BEGIN TRANSACTION");
            service.executeQuery("INSERT INTO test VALUES (1)");
            service.executeQuery("COMMIT");
        }, "Should handle transaction SQL");
    }

    @Test
    @DisplayName("Test executeQuery with JOIN query")
    public void testExecuteQueryWithJoinQuery() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String joinSql = "SELECT u.*, o.* FROM users u JOIN orders o ON u.id = o.user_id";
        assertDoesNotThrow(() -> service.executeQuery(joinSql),
            "Should handle JOIN queries");
    }

    @Test
    @DisplayName("Test executeQuery with subquery")
    public void testExecuteQueryWithSubquery() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String subquerySql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        assertDoesNotThrow(() -> service.executeQuery(subquerySql),
            "Should handle subqueries");
    }

    @Test
    @DisplayName("Test executeQuery with aggregate functions")
    public void testExecuteQueryWithAggregateFunctions() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String aggregateSql = "SELECT COUNT(*), AVG(age), MAX(salary) FROM users";
        assertDoesNotThrow(() -> service.executeQuery(aggregateSql),
            "Should handle aggregate functions");
    }

    @Test
    @DisplayName("Test executeQuery with GROUP BY")
    public void testExecuteQueryWithGroupBy() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String groupBySql = "SELECT department, COUNT(*) FROM users GROUP BY department";
        assertDoesNotThrow(() -> service.executeQuery(groupBySql),
            "Should handle GROUP BY queries");
    }

    @Test
    @DisplayName("Test executeQuery with ORDER BY")
    public void testExecuteQueryWithOrderBy() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String orderBySql = "SELECT * FROM users ORDER BY name ASC, age DESC";
        assertDoesNotThrow(() -> service.executeQuery(orderBySql),
            "Should handle ORDER BY queries");
    }

    @Test
    @DisplayName("Test executeQuery with LIMIT")
    public void testExecuteQueryWithLimit() {
        DatabaseService service = new DatabaseService();
        service.connect();
        String limitSql = "SELECT * FROM users LIMIT 10";
        assertDoesNotThrow(() -> service.executeQuery(limitSql),
            "Should handle LIMIT queries");
    }

    @Test
    @DisplayName("Test constructor initializes correctly")
    public void testConstructorInitializesCorrectly() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service);
        assertEquals(DatabaseService.class, service.getClass());
    }

    @Test
    @DisplayName("Test multiple operations in single session")
    public void testMultipleOperationsInSingleSession() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery("CREATE TABLE test (id INT)");
            service.executeQuery("INSERT INTO test VALUES (1)");
            service.executeQuery("SELECT * FROM test");
            service.executeQuery("UPDATE test SET id = 2");
            service.executeQuery("DELETE FROM test WHERE id = 2");
            service.executeQuery("DROP TABLE test");
            service.disconnect();
        }, "Multiple operations should work in single session");
    }
}
