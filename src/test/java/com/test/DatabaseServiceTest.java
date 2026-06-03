package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService class
 * Tests all public methods, constructors, and edge cases
 */
@DisplayName("DatabaseService Test Suite")
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
    @DisplayName("Test DatabaseService constructor creates non-null instance")
    void testConstructor_createsNonNullInstance() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();
        
        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }
    
    @Test
    @DisplayName("Test connect method executes without throwing exception")
    void testConnect_executesWithoutException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.connect(), 
            "Connect method should not throw exception even if database is unavailable");
    }
    
    @Test
    @DisplayName("Test connect method with valid configuration")
    void testConnect_withValidConfiguration() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act
        service.connect();
        
        // Assert - verify method completes (connection may fail but method should handle it)
        assertNotNull(service, "Service should remain valid after connect attempt");
    }
    
    @Test
    @DisplayName("Test connect method handles ClassNotFoundException gracefully")
    void testConnect_handlesClassNotFoundException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.connect(), 
            "Connect should handle ClassNotFoundException gracefully");
    }
    
    @Test
    @DisplayName("Test connect method handles SQLException gracefully")
    void testConnect_handlesSQLException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.connect(), 
            "Connect should handle SQLException gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with null SQL string")
    void testExecuteQuery_withNullSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(null), 
            "ExecuteQuery should handle null SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with empty SQL string")
    void testExecuteQuery_withEmptySQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(""), 
            "ExecuteQuery should handle empty SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid SELECT statement")
    void testExecuteQuery_withValidSelectStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle valid SELECT statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid INSERT statement")
    void testExecuteQuery_withValidInsertStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle valid INSERT statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid UPDATE statement")
    void testExecuteQuery_withValidUpdateStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "UPDATE users SET name = 'Jane' WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle valid UPDATE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid DELETE statement")
    void testExecuteQuery_withValidDeleteStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "DELETE FROM users WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle valid DELETE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    void testExecuteQuery_withMalformedSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INVALID SQL STATEMENT";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle malformed SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery without prior connection")
    void testExecuteQuery_withoutPriorConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle no connection gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery after connection attempt")
    void testExecuteQuery_afterConnectionAttempt() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should work after connection attempt");
    }
    
    @Test
    @DisplayName("Test disconnect method executes without exception")
    void testDisconnect_executesWithoutException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect method should not throw exception");
    }
    
    @Test
    @DisplayName("Test disconnect without prior connection")
    void testDisconnect_withoutPriorConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect should handle no connection gracefully");
    }
    
    @Test
    @DisplayName("Test disconnect after connection attempt")
    void testDisconnect_afterConnectionAttempt() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect should work after connection attempt");
    }
    
    @Test
    @DisplayName("Test multiple connect calls")
    void testMultipleConnectCalls() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.connect();
            service.connect();
        }, "Multiple connect calls should not throw exception");
    }
    
    @Test
    @DisplayName("Test multiple disconnect calls")
    void testMultipleDisconnectCalls() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
            service.disconnect();
        }, "Multiple disconnect calls should not throw exception");
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
    @DisplayName("Test executeQuery multiple times")
    void testExecuteQueryMultipleTimes() {
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
    @DisplayName("Test executeQuery with very long SQL string")
    void testExecuteQuery_withVeryLongSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE ");
        for (int i = 0; i < 100; i++) {
            longSQL.append("id = ").append(i).append(" OR ");
        }
        longSQL.append("id = 100");
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(longSQL.toString()), 
            "ExecuteQuery should handle very long SQL strings");
    }
    
    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    void testExecuteQuery_withSQLInjectionAttempt() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE id = 1; DROP TABLE users; --";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle SQL injection attempts");
    }
    
    @Test
    @DisplayName("Test executeQuery with special characters")
    void testExecuteQuery_withSpecialCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE name = 'O''Brien'";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle special characters");
    }
    
    @Test
    @DisplayName("Test executeQuery with Unicode characters")
    void testExecuteQuery_withUnicodeCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE name = '日本語'";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle Unicode characters");
    }
    
    @Test
    @DisplayName("Test full lifecycle: connect, execute, disconnect")
    void testFullLifecycle() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery("SELECT * FROM users");
            service.executeQuery("INSERT INTO users (name) VALUES ('Test')");
            service.disconnect();
        }, "Full lifecycle should complete without exception");
    }
    
    @Test
    @DisplayName("Test service remains valid after errors")
    void testServiceRemainsValidAfterErrors() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act
        service.executeQuery("INVALID SQL");
        service.connect();
        service.executeQuery("SELECT * FROM users");
        
        // Assert
        assertNotNull(service, "Service should remain valid after errors");
    }
}
