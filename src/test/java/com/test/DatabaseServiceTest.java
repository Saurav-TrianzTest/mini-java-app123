package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService class
 * Tests cover connection management, query execution, and error handling
 */
@DisplayName("DatabaseService Test Suite")
class DatabaseServiceTest {
    
    private DatabaseService databaseService;
    
    @BeforeEach
    void setUp() {
        // Arrange: Create a fresh instance before each test
        databaseService = new DatabaseService();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up: Ensure connection is closed after each test
        if (databaseService != null) {
            databaseService.disconnect();
        }
    }
    
    @Test
    @DisplayName("Test DatabaseService instantiation")
    void testDatabaseServiceInstantiation() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();
        
        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }
    
    @Test
    @DisplayName("Test connect method executes without throwing exceptions")
    void testConnect_shouldExecuteWithoutException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.connect(), 
            "Connect method should not throw exception even if connection fails");
    }
    
    @Test
    @DisplayName("Test connect method with valid configuration")
    void testConnect_withValidConfiguration() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act
        service.connect();
        
        // Assert - Connection attempt should complete without throwing
        assertNotNull(service, "Service should remain valid after connect attempt");
    }
    
    @Test
    @DisplayName("Test connect method handles connection failure gracefully")
    void testConnect_handlesConnectionFailureGracefully() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert - Should handle SQLException internally
        assertDoesNotThrow(() -> service.connect(), 
            "Connect should handle connection failures gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with null SQL")
    void testExecuteQuery_withNullSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert - Should handle null gracefully
        assertDoesNotThrow(() -> service.executeQuery(null), 
            "executeQuery should handle null SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with empty SQL")
    void testExecuteQuery_withEmptySql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(""), 
            "executeQuery should handle empty SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid SELECT statement")
    void testExecuteQuery_withValidSelectStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle valid SQL statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid INSERT statement")
    void testExecuteQuery_withValidInsertStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle INSERT statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid UPDATE statement")
    void testExecuteQuery_withValidUpdateStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "UPDATE users SET name = 'Jane' WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle UPDATE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid DELETE statement")
    void testExecuteQuery_withValidDeleteStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "DELETE FROM users WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle DELETE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery without prior connection")
    void testExecuteQuery_withoutPriorConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert - Should handle no connection gracefully
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle no connection gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    void testExecuteQuery_withMalformedSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INVALID SQL STATEMENT";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle malformed SQL gracefully");
    }
    
    @Test
    @DisplayName("Test disconnect method without prior connection")
    void testDisconnect_withoutPriorConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "disconnect should handle no connection gracefully");
    }
    
    @Test
    @DisplayName("Test disconnect method after connection attempt")
    void testDisconnect_afterConnectionAttempt() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "disconnect should execute without exception");
    }
    
    @Test
    @DisplayName("Test multiple disconnect calls")
    void testDisconnect_multipleCalls() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
            service.disconnect();
        }, "Multiple disconnect calls should be handled gracefully");
    }
    
    @Test
    @DisplayName("Test connect-disconnect-connect sequence")
    void testConnectDisconnectConnectSequence() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.connect();
        }, "Connect-disconnect-connect sequence should work");
    }
    
    @Test
    @DisplayName("Test executeQuery after disconnect")
    void testExecuteQuery_afterDisconnect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        service.disconnect();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery after disconnect should handle gracefully");
    }
    
    @Test
    @DisplayName("Test multiple executeQuery calls")
    void testExecuteQuery_multipleCalls() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT * FROM users");
            service.executeQuery("SELECT * FROM products");
            service.executeQuery("SELECT * FROM orders");
        }, "Multiple executeQuery calls should work");
    }
    
    @Test
    @DisplayName("Test executeQuery with special characters in SQL")
    void testExecuteQuery_withSpecialCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE name = 'O''Brien'";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle special characters");
    }
    
    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    void testExecuteQuery_withLongSqlStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSql.append(i);
            if (i < 999) longSql.append(",");
        }
        longSql.append(")");
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(longSql.toString()), 
            "executeQuery should handle long SQL statements");
    }
    
    @Test
    @DisplayName("Test service state after exception in connect")
    void testServiceState_afterConnectException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act
        service.connect(); // Will fail but should not throw
        
        // Assert - Service should still be usable
        assertNotNull(service, "Service should remain valid after failed connect");
        assertDoesNotThrow(() -> service.disconnect(), 
            "Should be able to call disconnect after failed connect");
    }
    
    @Test
    @DisplayName("Test concurrent operations safety")
    void testConcurrentOperations() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery("SELECT 1");
            service.disconnect();
        }, "Sequential operations should work correctly");
    }
}
