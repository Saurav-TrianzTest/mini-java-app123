package com.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;

/**
 * JUnit 5 test class for DatabaseService
 */
public class DatabaseServiceTest {

    private DatabaseService databaseService;

    @BeforeEach
    public void setUp() {
        try {
            databaseService = new DatabaseService();
        } catch (Exception e) {
            // Handle any initialization errors
        }
    }

    @AfterEach
    public void tearDown() {
        try {
            if (databaseService != null) {
                databaseService.disconnect();
            }
        } catch (Exception e) {
            // Handle cleanup errors
        }
    }

    @Test
    public void testDatabaseServiceConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    public void testConnectMethodExists() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> {
            // Testing that connect method can be called without throwing unexpected exceptions
            // Note: This will fail to connect to actual database in test environment
            service.connect();
        }, "Connect method should exist and be callable");
    }

    @Test
    public void testConnectWithNullConnection() {
        DatabaseService service = new DatabaseService();
        // Test connect without actual database - should handle gracefully
        assertDoesNotThrow(() -> service.connect());
    }

    @Test
    public void testExecuteQueryWithNullConnection() {
        DatabaseService service = new DatabaseService();
        // Test executeQuery before connection is established
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT * FROM test_table");
        }, "executeQuery should handle null connection gracefully");
    }

    @Test
    public void testExecuteQueryWithValidSQL() {
        DatabaseService service = new DatabaseService();
        // Test with valid SQL statement
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT 1");
        });
    }

    @Test
    public void testExecuteQueryWithEmptySQL() {
        DatabaseService service = new DatabaseService();
        // Test with empty SQL string
        assertDoesNotThrow(() -> {
            service.executeQuery("");
        });
    }

    @Test
    public void testExecuteQueryWithNullSQL() {
        DatabaseService service = new DatabaseService();
        // Test with null SQL statement
        assertDoesNotThrow(() -> {
            service.executeQuery(null);
        });
    }

    @Test
    public void testExecuteQueryWithInvalidSQL() {
        DatabaseService service = new DatabaseService();
        // Test with invalid SQL syntax
        assertDoesNotThrow(() -> {
            service.executeQuery("INVALID SQL STATEMENT");
        });
    }

    @Test
    public void testDisconnectWithoutConnection() {
        DatabaseService service = new DatabaseService();
        // Test disconnect before connect
        assertDoesNotThrow(() -> {
            service.disconnect();
        }, "Disconnect should handle null connection gracefully");
    }

    @Test
    public void testDisconnectAfterConnect() {
        DatabaseService service = new DatabaseService();
        // Test disconnect after attempting connection
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
        });
    }

    @Test
    public void testMultipleDisconnectCalls() {
        DatabaseService service = new DatabaseService();
        // Test calling disconnect multiple times
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
        }, "Multiple disconnect calls should be handled gracefully");
    }

    @Test
    public void testConnectDisconnectCycle() {
        DatabaseService service = new DatabaseService();
        // Test multiple connect-disconnect cycles
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.connect();
            service.disconnect();
        });
    }

    @Test
    public void testExecuteQueryAfterDisconnect() {
        DatabaseService service = new DatabaseService();
        // Test query execution after disconnect
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.executeQuery("SELECT 1");
        });
    }

    @Test
    public void testMultipleQueryExecutions() {
        DatabaseService service = new DatabaseService();
        // Test executing multiple queries in sequence
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT 1");
            service.executeQuery("SELECT 2");
            service.executeQuery("SELECT 3");
        });
    }

    @Test
    public void testExecuteQueryWithSpecialCharacters() {
        DatabaseService service = new DatabaseService();
        // Test query with special characters
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT * FROM table WHERE name = 'O''Brien'");
        });
    }

    @Test
    public void testExecuteQueryWithLongSQL() {
        DatabaseService service = new DatabaseService();
        // Test with very long SQL statement
        StringBuilder longSQL = new StringBuilder("SELECT * FROM table WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSQL.append(i).append(",");
        }
        longSQL.append("1001)");

        final String finalSQL = longSQL.toString();
        assertDoesNotThrow(() -> {
            service.executeQuery(finalSQL);
        });
    }
}
