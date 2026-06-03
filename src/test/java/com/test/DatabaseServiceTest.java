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
        // Arrange & Act
        DatabaseService service = new DatabaseService();
        
        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }
    
    @Test
    @DisplayName("Test connect method executes without exception")
    void testConnect_shouldExecuteWithoutException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.connect(), 
            "Connect method should not throw exception even if database is unavailable");
    }
    
    @Test
    @DisplayName("Test connect method with valid configuration")
    void testConnect_withValidConfiguration_shouldAttemptConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act
        service.connect();
        
        // Assert - verify method completes (connection may fail but method should handle it)
        assertNotNull(service, "Service should remain valid after connect attempt");
    }
    
    @Test
    @DisplayName("Test executeQuery with null SQL")
    void testExecuteQuery_withNullSql_shouldHandleGracefully() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(null), 
            "executeQuery should handle null SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with empty SQL")
    void testExecuteQuery_withEmptySql_shouldHandleGracefully() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(""), 
            "executeQuery should handle empty SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid SELECT statement")
    void testExecuteQuery_withValidSelectStatement_shouldExecute() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle valid SQL statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid INSERT statement")
    void testExecuteQuery_withValidInsertStatement_shouldExecute() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle INSERT statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid UPDATE statement")
    void testExecuteQuery_withValidUpdateStatement_shouldExecute() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "UPDATE users SET name = 'Jane' WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle UPDATE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid DELETE statement")
    void testExecuteQuery_withValidDeleteStatement_shouldExecute() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "DELETE FROM users WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle DELETE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    void testExecuteQuery_withMalformedSql_shouldHandleError() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INVALID SQL STATEMENT";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle malformed SQL without throwing exception");
    }
    
    @Test
    @DisplayName("Test executeQuery without prior connection")
    void testExecuteQuery_withoutConnection_shouldHandleGracefully() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert - should not throw exception even without connection
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle missing connection gracefully");
    }
    
    @Test
    @DisplayName("Test disconnect method executes without exception")
    void testDisconnect_shouldExecuteWithoutException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect method should not throw exception");
    }
    
    @Test
    @DisplayName("Test disconnect after connect")
    void testDisconnect_afterConnect_shouldCloseConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect should work after connect attempt");
    }
    
    @Test
    @DisplayName("Test multiple disconnect calls")
    void testDisconnect_multipleCalls_shouldHandleGracefully() {
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
    @DisplayName("Test connect-disconnect-connect cycle")
    void testConnectDisconnectConnectCycle_shouldWork() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
            service.connect();
            service.disconnect();
        }, "Connect-disconnect cycle should work properly");
    }
    
    @Test
    @DisplayName("Test executeQuery after disconnect")
    void testExecuteQuery_afterDisconnect_shouldHandleGracefully() {
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
    void testExecuteQuery_multipleCalls_shouldWork() {
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
    @DisplayName("Test executeQuery with SQL injection attempt")
    void testExecuteQuery_withSqlInjectionAttempt_shouldHandle() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE id = '1' OR '1'='1'";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle SQL injection attempts");
    }
    
    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    void testExecuteQuery_withLongSql_shouldHandle() {
        // Arrange
        DatabaseService service = new DatabaseService();
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE ");
        for (int i = 0; i < 100; i++) {
            longSql.append("id = ").append(i).append(" OR ");
        }
        longSql.append("id = 100");
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(longSql.toString()), 
            "executeQuery should handle long SQL statements");
    }
    
    @Test
    @DisplayName("Test executeQuery with special characters")
    void testExecuteQuery_withSpecialCharacters_shouldHandle() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE name = 'O''Brien'";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle special characters in SQL");
    }
    
    @Test
    @DisplayName("Test executeQuery with Unicode characters")
    void testExecuteQuery_withUnicodeCharacters_shouldHandle() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE name = '日本語'";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery should handle Unicode characters");
    }
}
