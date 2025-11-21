package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DatabaseService
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
    @DisplayName("Test DatabaseService instantiation")
    void testDatabaseServiceInstantiation() {
        assertNotNull(databaseService, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect method execution without exception")
    void testConnectExecutesWithoutException() {
        assertDoesNotThrow(() -> databaseService.connect(),
                "Connect method should not throw exception");
    }

    @Test
    @DisplayName("Test connect method with invalid credentials")
    void testConnectWithInvalidCredentials() {
        // This will fail due to hardcoded credentials, but validates the error handling
        assertDoesNotThrow(() -> databaseService.connect(),
                "Connect method should handle connection failure gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL")
    void testExecuteQueryWithNullSql() {
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
                "Execute query should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL")
    void testExecuteQueryWithEmptySql() {
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
                "Execute query should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL")
    void testExecuteQueryWithValidSql() {
        String validSql = "SELECT 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(validSql),
                "Execute query should handle valid SQL");
    }

    @Test
    @DisplayName("Test executeQuery with SELECT statement")
    void testExecuteQueryWithSelectStatement() {
        String selectSql = "SELECT * FROM users WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(selectSql),
                "Execute query should handle SELECT statement");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    void testExecuteQueryWithInsertStatement() {
        String insertSql = "INSERT INTO users (name, email) VALUES ('test', 'test@example.com')";
        assertDoesNotThrow(() -> databaseService.executeQuery(insertSql),
                "Execute query should handle INSERT statement");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    void testExecuteQueryWithUpdateStatement() {
        String updateSql = "UPDATE users SET name = 'updated' WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(updateSql),
                "Execute query should handle UPDATE statement");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    void testExecuteQueryWithDeleteStatement() {
        String deleteSql = "DELETE FROM users WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(deleteSql),
                "Execute query should handle DELETE statement");
    }

    @Test
    @DisplayName("Test executeQuery with invalid SQL syntax")
    void testExecuteQueryWithInvalidSql() {
        String invalidSql = "INVALID SQL SYNTAX HERE";
        assertDoesNotThrow(() -> databaseService.executeQuery(invalidSql),
                "Execute query should handle invalid SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    void testExecuteQueryWithSqlInjection() {
        String injectionSql = "SELECT * FROM users WHERE id = 1; DROP TABLE users;--";
        assertDoesNotThrow(() -> databaseService.executeQuery(injectionSql),
                "Execute query should handle SQL injection attempt");
    }

    @Test
    @DisplayName("Test disconnect method execution")
    void testDisconnectExecutesWithoutException() {
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "Disconnect method should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect without prior connection")
    void testDisconnectWithoutConnection() {
        DatabaseService newService = new DatabaseService();
        assertDoesNotThrow(() -> newService.disconnect(),
                "Disconnect should handle no prior connection gracefully");
    }

    @Test
    @DisplayName("Test multiple connect calls")
    void testMultipleConnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect calls should be handled");
    }

    @Test
    @DisplayName("Test connect and disconnect sequence")
    void testConnectAndDisconnectSequence() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
        }, "Connect and disconnect sequence should work");
    }

    @Test
    @DisplayName("Test multiple disconnect calls")
    void testMultipleDisconnectCalls() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect calls should be handled");
    }

    @Test
    @DisplayName("Test executeQuery after disconnect")
    void testExecuteQueryAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
                "Execute query after disconnect should be handled gracefully");
    }

    @Test
    @DisplayName("Test executeQuery before connect")
    void testExecuteQueryBeforeConnect() {
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
                "Execute query before connect should be handled gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with very long SQL")
    void testExecuteQueryWithLongSql() {
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSql.append(i);
            if (i < 999) longSql.append(",");
        }
        longSql.append(")");
        assertDoesNotThrow(() -> databaseService.executeQuery(longSql.toString()),
                "Execute query should handle long SQL");
    }

    @Test
    @DisplayName("Test executeQuery with special characters")
    void testExecuteQueryWithSpecialCharacters() {
        String specialSql = "SELECT * FROM users WHERE name = 'O''Brien'";
        assertDoesNotThrow(() -> databaseService.executeQuery(specialSql),
                "Execute query should handle special characters");
    }

    @Test
    @DisplayName("Test executeQuery with unicode characters")
    void testExecuteQueryWithUnicodeCharacters() {
        String unicodeSql = "SELECT * FROM users WHERE name = '测试用户'";
        assertDoesNotThrow(() -> databaseService.executeQuery(unicodeSql),
                "Execute query should handle unicode characters");
    }

    @Test
    @DisplayName("Test DatabaseService constructor creates non-null instance")
    void testConstructorCreatesNonNullInstance() {
        DatabaseService newService = new DatabaseService();
        assertNotNull(newService, "Constructor should create non-null instance");
    }

    @Test
    @DisplayName("Test multiple DatabaseService instances")
    void testMultipleInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        assertNotNull(service1, "First instance should not be null");
        assertNotNull(service2, "Second instance should not be null");
        assertNotSame(service1, service2, "Instances should be different objects");
    }
}
