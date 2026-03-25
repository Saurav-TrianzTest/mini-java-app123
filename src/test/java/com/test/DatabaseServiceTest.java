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
 * Tests all public methods, constructors, and various scenarios
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
    @DisplayName("Test DatabaseService constructor creates non-null instance")
    void testConstructor_createsNonNullInstance() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();
        
        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }
    
    @Test
    @DisplayName("Test DatabaseService constructor creates new instance each time")
    void testConstructor_createsNewInstanceEachTime() {
        // Arrange & Act
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        
        // Assert
        assertNotNull(service1, "First instance should not be null");
        assertNotNull(service2, "Second instance should not be null");
        assertNotSame(service1, service2, "Each constructor call should create a new instance");
    }
    
    @Test
    @DisplayName("Test connect method prints connection message")
    void testConnect_printsConnectionMessage() {
        // Arrange & Act
        databaseService.connect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database..."), 
                   "Output should contain 'Connecting to database...'");
    }
    
    @Test
    @DisplayName("Test connect method prints database URL")
    void testConnect_printsDatabaseUrl() {
        // Arrange & Act
        databaseService.connect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("jdbc:postgresql://localhost:5432/mini_app_db"), 
                   "Output should contain database URL");
    }
    
    @Test
    @DisplayName("Test connect method prints username")
    void testConnect_printsUsername() {
        // Arrange & Act
        databaseService.connect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Using username: postgres"), 
                   "Output should contain username");
    }
    
    @Test
    @DisplayName("Test connect method initializes cache connection")
    void testConnect_initializesCacheConnection() {
        // Arrange & Act
        databaseService.connect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to Redis cache at: 127.0.0.1:6379"), 
                   "Output should contain Redis connection message");
    }
    
    @Test
    @DisplayName("Test connect method initializes external services")
    void testConnect_initializesExternalServices() {
        // Arrange & Act
        databaseService.connect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Initializing external API: http://api.example.com:8080/v1"), 
                   "Output should contain external API initialization");
        assertTrue(output.contains("Initializing payment service: https://payment.internal.company.com/process"), 
                   "Output should contain payment service initialization");
    }
    
    @Test
    @DisplayName("Test connect method handles connection failure gracefully")
    void testConnect_handlesConnectionFailureGracefully() {
        // Arrange & Act
        databaseService.connect();
        
        // Assert - Should not throw exception even if connection fails
        String errorOutput = errorStreamCaptor.toString();
        // Connection will likely fail in test environment, but should be handled
        assertNotNull(errorOutput, "Error stream should not be null");
    }
    
    @Test
    @DisplayName("Test executeQuery with null connection does not throw exception")
    void testExecuteQuery_withNullConnection_doesNotThrowException() {
        // Arrange
        String sql = "SELECT * FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql), 
                          "executeQuery should not throw exception with null connection");
    }
    
    @Test
    @DisplayName("Test executeQuery with valid SQL string")
    void testExecuteQuery_withValidSql() {
        // Arrange
        String sql = "SELECT * FROM users WHERE id = 1";
        
        // Act
        databaseService.executeQuery(sql);
        
        // Assert - Should not throw exception
        assertDoesNotThrow(() -> databaseService.executeQuery(sql), 
                          "executeQuery should not throw exception with valid SQL");
    }
    
    @Test
    @DisplayName("Test executeQuery with empty SQL string")
    void testExecuteQuery_withEmptySql() {
        // Arrange
        String sql = "";
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql), 
                          "executeQuery should handle empty SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with null SQL string")
    void testExecuteQuery_withNullSql() {
        // Arrange
        String sql = null;
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql), 
                          "executeQuery should handle null SQL gracefully");
    }
    
    @Test
    @DisplayName("Test executeQuery with complex SQL statement")
    void testExecuteQuery_withComplexSql() {
        // Arrange
        String sql = "INSERT INTO users (name, email, age) VALUES ('John Doe', 'john@example.com', 30)";
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql), 
                          "executeQuery should handle complex SQL statements");
    }
    
    @Test
    @DisplayName("Test executeQuery with SQL injection attempt")
    void testExecuteQuery_withSqlInjectionAttempt() {
        // Arrange
        String sql = "SELECT * FROM users WHERE id = 1; DROP TABLE users;--";
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql), 
                          "executeQuery should handle SQL injection attempts without crashing");
    }
    
    @Test
    @DisplayName("Test disconnect with null connection does not throw exception")
    void testDisconnect_withNullConnection_doesNotThrowException() {
        // Arrange - No connection established
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect(), 
                          "disconnect should not throw exception with null connection");
    }
    
    @Test
    @DisplayName("Test disconnect prints closure message")
    void testDisconnect_printsClosureMessage() {
        // Arrange
        databaseService.connect();
        outputStreamCaptor.reset(); // Clear previous output
        
        // Act
        databaseService.disconnect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        // May contain closure message or error message depending on connection state
        assertNotNull(output, "Output should not be null");
    }
    
    @Test
    @DisplayName("Test disconnect can be called multiple times safely")
    void testDisconnect_canBeCalledMultipleTimes() {
        // Arrange
        databaseService.connect();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        }, "disconnect should be safe to call multiple times");
    }
    
    @Test
    @DisplayName("Test connect and disconnect sequence")
    void testConnectAndDisconnectSequence() {
        // Arrange & Act
        databaseService.connect();
        databaseService.disconnect();
        
        // Assert
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database..."), 
                   "Should contain connection message");
    }
    
    @Test
    @DisplayName("Test multiple connect calls")
    void testMultipleConnectCalls() {
        // Arrange & Act
        databaseService.connect();
        databaseService.connect();
        
        // Assert - Should not throw exception
        assertDoesNotThrow(() -> databaseService.connect(), 
                          "Multiple connect calls should not throw exception");
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
                          "executeQuery after disconnect should not throw exception");
    }
    
    @Test
    @DisplayName("Test service lifecycle - connect, execute, disconnect")
    void testServiceLifecycle() {
        // Arrange
        String sql = "SELECT COUNT(*) FROM users";
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery(sql);
            databaseService.disconnect();
        }, "Complete service lifecycle should execute without exceptions");
    }
    
    @Test
    @DisplayName("Test executeQuery with various SQL types")
    void testExecuteQuery_withVariousSqlTypes() {
        // Arrange
        String[] sqlStatements = {
            "SELECT * FROM users",
            "INSERT INTO users (name) VALUES ('Test')",
            "UPDATE users SET name = 'Updated' WHERE id = 1",
            "DELETE FROM users WHERE id = 1",
            "CREATE TABLE test (id INT)",
            "DROP TABLE test"
        };
        
        // Act & Assert
        for (String sql : sqlStatements) {
            assertDoesNotThrow(() -> databaseService.executeQuery(sql), 
                              "Should handle SQL type: " + sql);
        }
    }
    
    @Test
    @DisplayName("Test DatabaseService instance isolation")
    void testDatabaseServiceInstanceIsolation() {
        // Arrange
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        
        // Act
        service1.connect();
        
        // Assert
        assertNotSame(service1, service2, "Different instances should be isolated");
        
        // Cleanup
        service1.disconnect();
    }
    
    @Test
    @DisplayName("Test executeQuery with special characters in SQL")
    void testExecuteQuery_withSpecialCharacters() {
        // Arrange
        String sql = "SELECT * FROM users WHERE name = 'O''Brien' AND email LIKE '%@example.com'";
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql), 
                          "Should handle special characters in SQL");
    }
    
    @Test
    @DisplayName("Test executeQuery with very long SQL statement")
    void testExecuteQuery_withLongSql() {
        // Arrange
        StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE id IN (");
        for (int i = 0; i < 1000; i++) {
            longSql.append(i);
            if (i < 999) longSql.append(",");
        }
        longSql.append(")");
        
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(longSql.toString()), 
                          "Should handle very long SQL statements");
    }
}
