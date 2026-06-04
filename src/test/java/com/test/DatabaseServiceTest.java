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
    @DisplayName("Test executeQuery with null SQL")
    void testExecuteQuery_withNullSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(null), 
            "ExecuteQuery should handle null SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with empty SQL")
    void testExecuteQuery_withEmptySql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(""), 
            "ExecuteQuery should handle empty SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid SQL statement")
    void testExecuteQuery_withValidSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String validSql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(validSql), 
            "ExecuteQuery should handle valid SQL without throwing exception");
    }
    
    @Test
    @DisplayName("Test executeQuery with CREATE TABLE statement")
    void testExecuteQuery_withCreateTableStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String createTableSql = "CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(100))";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(createTableSql), 
            "ExecuteQuery should handle CREATE TABLE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with INSERT statement")
    void testExecuteQuery_withInsertStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String insertSql = "INSERT INTO users (id, name) VALUES (1, 'Test User')";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(insertSql), 
            "ExecuteQuery should handle INSERT statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with UPDATE statement")
    void testExecuteQuery_withUpdateStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String updateSql = "UPDATE users SET name = 'Updated Name' WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(updateSql), 
            "ExecuteQuery should handle UPDATE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with DELETE statement")
    void testExecuteQuery_withDeleteStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String deleteSql = "DELETE FROM users WHERE id = 1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(deleteSql), 
            "ExecuteQuery should handle DELETE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery without connection")
    void testExecuteQuery_withoutConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert - should not throw exception even without connection
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "ExecuteQuery should handle missing connection gracefully");
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
    @DisplayName("Test disconnect without prior connection")
    void testDisconnect_withoutPriorConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect should handle case when no connection was established");
    }
    
    @Test
    @DisplayName("Test disconnect after connect attempt")
    void testDisconnect_afterConnectAttempt() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "Disconnect should work after connect attempt");
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
            service.disconnect();
        }, "Connect-disconnect sequence should work properly");
    }
    
    @Test
    @DisplayName("Test executeQuery with special characters in SQL")
    void testExecuteQuery_withSpecialCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sqlWithSpecialChars = "SELECT * FROM users WHERE name = 'O''Brien'";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sqlWithSpecialChars), 
            "ExecuteQuery should handle SQL with special characters");
    }
    
    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    void testExecuteQuery_withLongSqlStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
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
    @DisplayName("Test executeQuery with malformed SQL")
    void testExecuteQuery_withMalformedSql() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String malformedSql = "SELECT * FROM WHERE";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(malformedSql), 
            "ExecuteQuery should handle malformed SQL gracefully");
    }
    
    @Test
    @DisplayName("Test service lifecycle - full workflow")
    void testServiceLifecycle_fullWorkflow() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery("SELECT 1");
            service.executeQuery("SELECT * FROM users");
            service.disconnect();
        }, "Full service lifecycle should complete without exceptions");
    }
    
    @Test
    @DisplayName("Test executeQuery multiple times with same connection")
    void testExecuteQuery_multipleTimesWithSameConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT 1");
            service.executeQuery("SELECT 2");
            service.executeQuery("SELECT 3");
        }, "Multiple queries should execute without exception");
        
        service.disconnect();
    }
    
    @Test
    @DisplayName("Test DatabaseService with different SQL types")
    void testDatabaseService_withDifferentSqlTypes() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.executeQuery("SELECT * FROM users");
            service.executeQuery("INSERT INTO users VALUES (1, 'test')");
            service.executeQuery("UPDATE users SET name = 'updated'");
            service.executeQuery("DELETE FROM users WHERE id = 1");
            service.executeQuery("CREATE TABLE test (id INT)");
            service.executeQuery("DROP TABLE test");
        }, "Service should handle all SQL statement types");
    }
}
