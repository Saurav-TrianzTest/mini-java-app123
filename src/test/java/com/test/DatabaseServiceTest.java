package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService
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
    public void testDatabaseServiceConstructor() {
        assertNotNull(databaseService, "DatabaseService instance should not be null");
    }

    @Test
    public void testConnectWithDefaultEnvironmentVariables() {
        assertDoesNotThrow(() -> databaseService.connect(),
            "Connect should not throw exception with default environment variables");
    }

    @Test
    public void testConnectMultipleTimes() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect calls should not throw exception");
    }

    @Test
    public void testExecuteQueryWithNullConnection() {
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
            "executeQuery should handle null connection gracefully");
    }

    @Test
    public void testExecuteQueryWithValidSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
            "executeQuery should handle valid SQL without throwing exception");
    }

    @Test
    public void testExecuteQueryWithInvalidSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("INVALID SQL QUERY"),
            "executeQuery should handle invalid SQL gracefully");
    }

    @Test
    public void testExecuteQueryWithEmptyString() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
            "executeQuery should handle empty SQL string gracefully");
    }

    @Test
    public void testExecuteQueryWithNullSQL() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
            "executeQuery should handle null SQL gracefully");
    }

    @Test
    public void testExecuteQueryWithLongSQL() {
        databaseService.connect();
        String longSQL = "SELECT * FROM table WHERE " + "condition = 'value' AND ".repeat(100) + "1=1";
        assertDoesNotThrow(() -> databaseService.executeQuery(longSQL),
            "executeQuery should handle long SQL statements");
    }

    @Test
    public void testDisconnectWithoutConnection() {
        assertDoesNotThrow(() -> databaseService.disconnect(),
            "disconnect should handle case when no connection exists");
    }

    @Test
    public void testDisconnectAfterConnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect(),
            "disconnect should work after connect");
    }

    @Test
    public void testDisconnectMultipleTimes() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect calls should not throw exception");
    }

    @Test
    public void testConnectDisconnectCycle() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
            databaseService.disconnect();
        }, "Connect-disconnect cycle should work correctly");
    }

    @Test
    public void testExecuteQueryAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
            "executeQuery should handle disconnected state gracefully");
    }

    @Test
    public void testMultipleQueriesInSequence() {
        databaseService.connect();
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.executeQuery("SELECT 3");
        }, "Multiple queries in sequence should execute without error");
    }

    @Test
    public void testExecuteQueryWithSpecialCharacters() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 'test''data'"),
            "executeQuery should handle special characters in SQL");
    }

    @Test
    public void testExecuteQueryWithUnicodeCharacters() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT '测试数据'"),
            "executeQuery should handle unicode characters");
    }

    @Test
    public void testDatabaseServiceLifecycle() {
        assertDoesNotThrow(() -> {
            DatabaseService service = new DatabaseService();
            service.connect();
            service.executeQuery("SELECT 1");
            service.disconnect();
        }, "Complete lifecycle should work without errors");
    }

    @Test
    public void testExecuteQueryWithMultilineSQL() {
        databaseService.connect();
        String multilineSQL = "SELECT *\nFROM users\nWHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(multilineSQL),
            "executeQuery should handle multiline SQL");
    }

    @Test
    public void testExecuteQueryWithComments() {
        databaseService.connect();
        String sqlWithComments = "SELECT 1 -- This is a comment";
        assertDoesNotThrow(() -> databaseService.executeQuery(sqlWithComments),
            "executeQuery should handle SQL with comments");
    }
}
