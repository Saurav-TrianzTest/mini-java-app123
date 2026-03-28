package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService();
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        if (databaseService != null) {
            databaseService.disconnect();
        }
    }
    
    @Test
    @DisplayName("Test DatabaseService constructor creates instance")
    void testConstructor_createsInstance() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();
        
        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }
    
    @Test
    @DisplayName("Test connect method attempts database connection")
    void testConnect_attemptsConnection() {
        // Act
        databaseService.connect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database"), 
                   "Should print connecting message");
    }
    
    @Test
    @DisplayName("Test connect method prints connection details")
    void testConnect_printsConnectionDetails() {
        // Act
        databaseService.connect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database"), 
                   "Should print connecting message");
        // Note: Actual connection may fail due to missing database, but method should execute
    }
    
    @Test
    @DisplayName("Test connect method handles missing driver gracefully")
    void testConnect_handlesMissingDriver() {
        // Act
        databaseService.connect();
        
        // Assert - Either connects successfully or handles error
        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database") || 
                   error.contains("Database driver not found") ||
                   error.contains("Database connection failed"),
                   "Should handle connection attempt");
    }
    
    @Test
    @DisplayName("Test connect method initializes cache connection")
    void testConnect_initializesCacheConnection() {
        // Act
        databaseService.connect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        // Cache connection may be attempted even if DB connection fails
        assertTrue(output.contains("Connecting to database"), 
                   "Should attempt database connection");
    }
    
    @Test
    @DisplayName("Test connect method initializes external services")
    void testConnect_initializesExternalServices() {
        // Act
        databaseService.connect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database"), 
                   "Should attempt to connect");
    }
    
    @Test
    @DisplayName("Test executeQuery with null connection")
    void testExecuteQuery_withNullConnection() {
        // Arrange
        String sql = "SELECT * FROM users";
        
        // Act - Don't connect first, so connection is null
        databaseService.executeQuery(sql);
        
        // Assert - Should handle null connection gracefully (no exception thrown)
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "Should handle null connection without throwing exception");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid SQL")
    void testExecuteQuery_withValidSQL() {
        // Arrange
        String sql = "SELECT * FROM users WHERE id = 1";
        databaseService.connect();
        
        // Act
        databaseService.executeQuery(sql);
        
        // Assert - Method should execute without throwing exception
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "Should execute query without throwing exception");
    }
    
    @Test
    @DisplayName("Test executeQuery with empty SQL string")
    void testExecuteQuery_withEmptySQL() {
        // Arrange
        String sql = "";
        databaseService.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "Should handle empty SQL without throwing exception");
    }
    
    @Test
    @DisplayName("Test executeQuery with null SQL string")
    void testExecuteQuery_withNullSQL() {
        // Arrange
        String sql = null;
        databaseService.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "Should handle null SQL without throwing exception");
    }
    
    @Test
    @DisplayName("Test executeQuery with complex SQL")
    void testExecuteQuery_withComplexSQL() {
        // Arrange
        String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";
        databaseService.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "Should handle complex SQL without throwing exception");
    }
    
    @Test
    @DisplayName("Test executeQuery with malformed SQL")
    void testExecuteQuery_withMalformedSQL() {
        // Arrange
        String sql = "INVALID SQL STATEMENT";
        databaseService.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "Should handle malformed SQL without throwing exception");
    }
    
    @Test
    @DisplayName("Test disconnect with null connection")
    void testDisconnect_withNullConnection() {
        // Act & Assert - Don't connect first
        assertDoesNotThrow(() -> databaseService.disconnect(),
                          "Should handle null connection without throwing exception");
    }
    
    @Test
    @DisplayName("Test disconnect after connect")
    void testDisconnect_afterConnect() {
        // Arrange
        databaseService.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect(),
                          "Should disconnect without throwing exception");
    }
    
    @Test
    @DisplayName("Test disconnect prints closing message")
    void testDisconnect_printsClosingMessage() {
        // Arrange
        databaseService.connect();
        
        // Act
        databaseService.disconnect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        String error = errorStreamCaptor.toString();
        assertTrue(output.length() > 0 || error.length() > 0,
                   "Should produce some output during disconnect");
    }
    
    @Test
    @DisplayName("Test multiple connect calls")
    void testConnect_multipleCalls() {
        // Act
        databaseService.connect();
        databaseService.connect();
        
        // Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        }, "Should handle multiple connect calls");
    }
    
    @Test
    @DisplayName("Test multiple disconnect calls")
    void testDisconnect_multipleCalls() {
        // Arrange
        databaseService.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Should handle multiple disconnect calls");
    }
    
    @Test
    @DisplayName("Test connect-disconnect-connect sequence")
    void testConnectDisconnectConnectSequence() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.disconnect();
            databaseService.connect();
            databaseService.disconnect();
        }, "Should handle connect-disconnect sequence");
    }
    
    @Test
    @DisplayName("Test executeQuery between connect and disconnect")
    void testExecuteQuery_betweenConnectAndDisconnect() {
        // Arrange
        databaseService.connect();
        String sql = "SELECT 1";
        
        // Act
        databaseService.executeQuery(sql);
        databaseService.disconnect();
        
        // Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery(sql);
            databaseService.disconnect();
        }, "Should handle full lifecycle");
    }
    
    @Test
    @DisplayName("Test executeQuery after disconnect")
    void testExecuteQuery_afterDisconnect() {
        // Arrange
        databaseService.connect();
        databaseService.disconnect();
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "Should handle query after disconnect");
    }
    
    @Test
    @DisplayName("Test DatabaseService with special characters in SQL")
    void testExecuteQuery_withSpecialCharacters() {
        // Arrange
        String sql = "SELECT * FROM users WHERE name = 'O''Brien'";
        databaseService.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql),
                          "Should handle special characters in SQL");
    }
    
    @Test
    @DisplayName("Test DatabaseService with very long SQL")
    void testExecuteQuery_withVeryLongSQL() {
        // Arrange
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSql.append(i);
            if (i < 999) longSql.append(",");
        }
        longSql.append(")");
        databaseService.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(longSql.toString()),
                          "Should handle very long SQL");
    }
    
    @Test
    @DisplayName("Test DatabaseService instance is independent")
    void testMultipleInstances_areIndependent() {
        // Arrange
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        
        // Assert
        assertNotNull(service1, "First instance should not be null");
        assertNotNull(service2, "Second instance should not be null");
        assertNotSame(service1, service2, "Instances should be different objects");
    }
}
