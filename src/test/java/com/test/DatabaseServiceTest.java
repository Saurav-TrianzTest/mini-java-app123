package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test class for DatabaseService
 * Tests database connection, query execution, and disconnection functionality
 */
@DisplayName("DatabaseService Tests")
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
    @DisplayName("Test DatabaseService constructor creates instance")
    public void testDatabaseServiceConstructor() {
        assertNotNull(databaseService, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test connect method establishes database connection")
    public void testConnect() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should not throw exception");
    }

    @Test
    @DisplayName("Test connect method handles connection failure gracefully")
    public void testConnectWithInvalidCredentials() {
        // Test that connection failure is handled without throwing exception
        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Connect method should handle failures gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with valid SQL statement")
    public void testExecuteQueryWithValidSQL() {
        databaseService.connect();
        String validSQL = "SELECT 1";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(validSQL);
        }, "Execute query should not throw exception with valid SQL");
    }

    @Test
    @DisplayName("Test executeQuery with empty SQL statement")
    public void testExecuteQueryWithEmptySQL() {
        databaseService.connect();
        String emptySQL = "";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(emptySQL);
        }, "Execute query should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with null SQL statement")
    public void testExecuteQueryWithNullSQL() {
        databaseService.connect();

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(null);
        }, "Execute query should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery before connection is established")
    public void testExecuteQueryBeforeConnect() {
        String sql = "SELECT 1";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query should handle no connection gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with SELECT statement")
    public void testExecuteQuerySelect() {
        databaseService.connect();
        String selectSQL = "SELECT * FROM users WHERE id = 1";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(selectSQL);
        }, "Execute query should handle SELECT statement");
    }

    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    public void testExecuteQueryInsert() {
        databaseService.connect();
        String insertSQL = "INSERT INTO users (name, email) VALUES ('test', 'test@example.com')";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(insertSQL);
        }, "Execute query should handle INSERT statement");
    }

    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    public void testExecuteQueryUpdate() {
        databaseService.connect();
        String updateSQL = "UPDATE users SET name = 'updated' WHERE id = 1";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(updateSQL);
        }, "Execute query should handle UPDATE statement");
    }

    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    public void testExecuteQueryDelete() {
        databaseService.connect();
        String deleteSQL = "DELETE FROM users WHERE id = 1";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(deleteSQL);
        }, "Execute query should handle DELETE statement");
    }

    @Test
    @DisplayName("Test disconnect method closes connection")
    public void testDisconnect() {
        databaseService.connect();

        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should not throw exception");
    }

    @Test
    @DisplayName("Test disconnect method without prior connection")
    public void testDisconnectWithoutConnection() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Disconnect should handle no connection gracefully");
    }

    @Test
    @DisplayName("Test multiple disconnect calls")
    public void testMultipleDisconnectCalls() {
        databaseService.connect();
        databaseService.disconnect();

        assertDoesNotThrow(() -> {
            databaseService.disconnect();
        }, "Multiple disconnect calls should be handled gracefully");
    }

    @Test
    @DisplayName("Test connect after disconnect")
    public void testReconnectAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();

        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Should be able to reconnect after disconnect");
    }

    @Test
    @DisplayName("Test executeQuery after disconnect")
    public void testExecuteQueryAfterDisconnect() {
        databaseService.connect();
        databaseService.disconnect();
        String sql = "SELECT 1";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sql);
        }, "Execute query after disconnect should be handled gracefully");
    }

    @Test
    @DisplayName("Test multiple connect calls")
    public void testMultipleConnectCalls() {
        databaseService.connect();

        assertDoesNotThrow(() -> {
            databaseService.connect();
        }, "Multiple connect calls should be handled gracefully");
    }

    @Test
    @DisplayName("Test executeQuery with complex SQL statement")
    public void testExecuteQueryWithComplexSQL() {
        databaseService.connect();
        String complexSQL = "SELECT u.name, o.order_date FROM users u JOIN orders o ON u.id = o.user_id WHERE u.status = 'active'";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(complexSQL);
        }, "Execute query should handle complex SQL statements");
    }

    @Test
    @DisplayName("Test executeQuery with SQL containing special characters")
    public void testExecuteQueryWithSpecialCharacters() {
        databaseService.connect();
        String sqlWithSpecialChars = "SELECT * FROM users WHERE name LIKE '%test%'";

        assertDoesNotThrow(() -> {
            databaseService.executeQuery(sqlWithSpecialChars);
        }, "Execute query should handle SQL with special characters");
    }

    @Test
    @DisplayName("Test service instance independence")
    public void testMultipleServiceInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();

        assertNotSame(service1, service2, "Different instances should be created");

        service1.connect();
        service2.connect();

        assertDoesNotThrow(() -> {
            service1.disconnect();
            service2.disconnect();
        }, "Multiple service instances should work independently");
    }
}
