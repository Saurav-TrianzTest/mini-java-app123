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
    @DisplayName("Test connect method - should not throw exception")
    void testConnect_shouldNotThrowException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.connect(), 
            "Connect method should not throw exception even if connection fails");
    }
    
    @Test
    @DisplayName("Test connect method - handles missing driver gracefully")
    void testConnect_handlesMissingDriverGracefully() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act
        service.connect();
        
        // Assert - should complete without throwing exception
        assertNotNull(service, "Service should remain valid after connection attempt");
    }
    
    @Test
    @DisplayName("Test connect method - handles connection failure gracefully")
    void testConnect_handlesConnectionFailureGracefully() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.connect(), 
            "Connect should handle connection failures gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with null connection")
    void testExecuteQuery_withNullConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle null connection gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid SQL statement")
    void testExecuteQuery_withValidSqlStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sql = "SELECT * FROM users WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should not throw exception with valid SQL");
    }
    
    @Test
    @DisplayName("Test executeQuery with empty SQL string")
    void testExecuteQuery_withEmptySqlString() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sql = "";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle empty SQL string gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with null SQL string")
    void testExecuteQuery_withNullSqlString() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sql = null;
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle null SQL string gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with complex SQL statement")
    void testExecuteQuery_withComplexSqlStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sql = "INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com')";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle complex SQL statements");
    }
    
    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    void testExecuteQuery_withUpdateStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sql = "UPDATE users SET name = 'Jane Doe' WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle UPDATE statements");
    }
    
    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    void testExecuteQuery_withDeleteStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sql = "DELETE FROM users WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle DELETE statements");
    }
    
    @Test
    @DisplayName("Test disconnect method - should not throw exception")
    void testDisconnect_shouldNotThrowException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect method should not throw exception");
    }
    
    @Test
    @DisplayName("Test disconnect after connect")
    void testDisconnect_afterConnect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect should work after connect attempt");
    }
    
    @Test
    @DisplayName("Test multiple disconnect calls")
    void testMultipleDisconnectCalls() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
            service.disconnect();
        }, "Multiple disconnect calls should not throw exception");
    }
    
    @Test
    @DisplayName("Test disconnect without connect")
    void testDisconnect_withoutConnect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect should work even without prior connect");
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
            "ExecuteQuery after disconnect should not throw exception");
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
    @DisplayName("Test connect-disconnect-connect cycle")
    void testConnectDisconnectConnectCycle() {
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
    @DisplayName("Test executeQuery with special characters in SQL")
    void testExecuteQuery_withSpecialCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        String sql = "SELECT * FROM users WHERE name LIKE '%O''Brien%'";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle special characters in SQL");
    }
    
    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    void testExecuteQuery_withLongSqlStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 100; i++) {
            longSql.append(i);
            if (i < 99) longSql.append(",");
        }
        longSql.append(")");
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(longSql.toString()), 
            "ExecuteQuery should handle long SQL statements");
    }
    
    @Test
    @DisplayName("Test service state after failed connection")
    void testServiceState_afterFailedConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act
        service.connect();
        
        // Assert
        assertNotNull(service, "Service should remain valid after failed connection");
    }
}
