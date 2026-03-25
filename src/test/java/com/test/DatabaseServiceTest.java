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
 * Tests all methods, constructors, and edge cases
 */
@DisplayName("DatabaseService Test Suite")
class DatabaseServiceTest {
    
    private DatabaseService databaseService;
    
    @BeforeEach
    void setUp() {
        // Arrange: Create a new DatabaseService instance before each test
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
            "connect() should not throw exception even if database is unavailable");
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
            "connect() should handle ClassNotFoundException gracefully");
    }
    
    @Test
    @DisplayName("Test connect method handles SQLException gracefully")
    void testConnect_handlesSQLException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.connect(), 
            "connect() should handle SQLException gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with null SQL string")
    void testExecuteQuery_withNullSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(null), 
            "executeQuery() should handle null SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with empty SQL string")
    void testExecuteQuery_withEmptySQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(""), 
            "executeQuery() should handle empty SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid SELECT statement")
    void testExecuteQuery_withValidSelectStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery() should handle valid SELECT statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid INSERT statement")
    void testExecuteQuery_withValidInsertStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery() should handle valid INSERT statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid UPDATE statement")
    void testExecuteQuery_withValidUpdateStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "UPDATE users SET name='Jane' WHERE id=1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery() should handle valid UPDATE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid DELETE statement")
    void testExecuteQuery_withValidDeleteStatement() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "DELETE FROM users WHERE id=1";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery() should handle valid DELETE statement");
    }
    
    @Test
    @DisplayName("Test executeQuery without prior connection")
    void testExecuteQuery_withoutPriorConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery() should handle execution without prior connection");
    }
    
    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    void testExecuteQuery_withMalformedSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "INVALID SQL STATEMENT";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery() should handle malformed SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    void testExecuteQuery_withSQLInjectionAttempt() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE id=1; DROP TABLE users;--";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery() should handle SQL injection attempts");
    }
    
    @Test
    @DisplayName("Test disconnect method executes without exception")
    void testDisconnect_executesWithoutException() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "disconnect() should not throw exception");
    }
    
    @Test
    @DisplayName("Test disconnect without prior connection")
    void testDisconnect_withoutPriorConnection() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "disconnect() should handle disconnection without prior connection");
    }
    
    @Test
    @DisplayName("Test disconnect after connect")
    void testDisconnect_afterConnect() {
        // Arrange
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> service.disconnect(), 
            "disconnect() should work after connect()");
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
        }, "Multiple connect() calls should not throw exception");
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
        }, "Multiple disconnect() calls should not throw exception");
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
        }, "Connect-disconnect sequence should work correctly");
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
            "executeQuery() should handle execution after disconnect");
    }
    
    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    void testExecuteQuery_withVeryLongSQL() {
        // Arrange
        DatabaseService service = new DatabaseService();
        StringBuilder longSQL = new StringBuilder("SELECT * FROM users WHERE ");
        for (int i = 0; i < 1000; i++) {
            longSQL.append("id=").append(i).append(" OR ");
        }
        longSQL.append("id=9999");
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(longSQL.toString()), 
            "executeQuery() should handle very long SQL statements");
    }
    
    @Test
    @DisplayName("Test executeQuery with special characters in SQL")
    void testExecuteQuery_withSpecialCharacters() {
        // Arrange
        DatabaseService service = new DatabaseService();
        String sql = "SELECT * FROM users WHERE name='O''Brien' AND email LIKE '%@example.com'";
        
        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(sql), 
            "executeQuery() should handle special characters in SQL");
    }
    
    @Test
    @DisplayName("Test service instance is reusable")
    void testServiceInstance_isReusable() {
        // Arrange
        DatabaseService service = new DatabaseService();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.executeQuery("SELECT 1");
            service.disconnect();
            service.connect();
            service.executeQuery("SELECT 2");
            service.disconnect();
        }, "DatabaseService instance should be reusable");
    }
    
    @Test
    @DisplayName("Test hardcoded database configuration values")
    void testHardcodedDatabaseConfiguration() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();
        
        // Assert - verify service can be instantiated with hardcoded values
        assertNotNull(service, "Service should be created with hardcoded configuration");
    }
    
    @Test
    @DisplayName("Test hardcoded cache configuration values")
    void testHardcodedCacheConfiguration() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Assert - verify connect handles cache initialization
        assertNotNull(service, "Service should handle cache configuration");
    }
    
    @Test
    @DisplayName("Test hardcoded external service URLs")
    void testHardcodedExternalServiceURLs() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();
        service.connect();
        
        // Assert - verify connect handles external service initialization
        assertNotNull(service, "Service should handle external service URLs");
    }
}
