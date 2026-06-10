package com.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit 5 tests for DatabaseService class.
 * Tests cover: constructor, connect(), executeQuery(), disconnect(),
 * connectToCache() (via connect()), initializeExternalServices() (via connect()).
 */
@DisplayName("DatabaseService Tests")
class DatabaseServiceTest {

    private DatabaseService databaseService;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Default constructor creates a non-null DatabaseService instance")
    void constructor_defaultConstructor_createsInstance() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Newly created DatabaseService has null connection field")
    void constructor_newInstance_connectionIsNull() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        Object connectionValue = connectionField.get(service);

        // Assert
        assertNull(connectionValue, "Connection should be null before connect() is called");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // connect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect() prints 'Connecting to database...' message")
    void connect_whenCalled_printsDatabaseConnectingMessage() {
        // Arrange & Act
        databaseService.connect();

        // Assert – even if the actual DB is unavailable the message is printed first
        String output = outContent.toString();
        assertTrue(output.contains("Connecting to database..."),
                "Should print connecting message");
    }

    @Test
    @DisplayName("connect() prints error message when database is unavailable")
    void connect_whenDatabaseUnavailable_printsErrorMessage() {
        // Arrange & Act
        databaseService.connect();

        // Assert – no real DB, so error stream should contain failure info
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Database connection failed"),
                "Should print database connection failed error");
    }

    @Test
    @DisplayName("connect() does not throw exception when database is unavailable")
    void connect_whenDatabaseUnavailable_doesNotThrowException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> databaseService.connect(),
                "connect() should handle SQLException internally and not propagate it");
    }

    @Test
    @DisplayName("connect() can be called multiple times without throwing")
    void connect_calledMultipleTimes_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
            databaseService.connect();
        }, "Multiple connect() calls should not throw exceptions");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeQuery() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("executeQuery() with null connection does not throw exception")
    void executeQuery_withNullConnection_doesNotThrow() {
        // Arrange – connection is null by default (no connect() called)
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
                "executeQuery() should handle null connection gracefully");
    }

    @Test
    @DisplayName("executeQuery() with null connection produces no output")
    void executeQuery_withNullConnection_producesNoOutput() {
        // Arrange – connection is null by default
        // Act
        databaseService.executeQuery("SELECT 1");

        // Assert
        String output = outContent.toString();
        assertFalse(output.contains("Executing query"),
                "Should not print executing query when connection is null");
    }

    @Test
    @DisplayName("executeQuery() with null SQL and null connection does not throw")
    void executeQuery_withNullSqlAndNullConnection_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
                "executeQuery() with null SQL should not throw when connection is null");
    }

    @Test
    @DisplayName("executeQuery() with empty SQL and null connection does not throw")
    void executeQuery_withEmptySqlAndNullConnection_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
                "executeQuery() with empty SQL should not throw when connection is null");
    }

    @Test
    @DisplayName("executeQuery() with mock open connection executes query and prints message")
    void executeQuery_withMockOpenConnection_executesQueryAndPrintsMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.execute()).thenReturn(true);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("SELECT * FROM users");

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Executing query: SELECT * FROM users"),
                "Should print executing query message");
        verify(mockStmt).setQueryTimeout(30);
        verify(mockStmt).execute();
        verify(mockStmt).close();
    }

    @Test
    @DisplayName("executeQuery() with mock closed connection does not execute query")
    void executeQuery_withMockClosedConnection_doesNotExecuteQuery() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(true);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("SELECT 1");

        // Assert
        String output = outContent.toString();
        assertFalse(output.contains("Executing query"),
                "Should not execute query when connection is closed");
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("executeQuery() handles SQLException from prepareStatement gracefully")
    void executeQuery_whenPrepareStatementThrowsSQLException_printsError() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString()))
                .thenThrow(new SQLException("Prepare failed"));

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery("BAD SQL"),
                "executeQuery() should handle SQLException without propagating");

        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Query execution failed"),
                "Should print query execution failed error");
    }

    @Test
    @DisplayName("executeQuery() handles SQLException from isClosed gracefully")
    void executeQuery_whenIsClosedThrowsSQLException_printsError() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenThrow(new SQLException("isClosed failed"));

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
                "executeQuery() should handle isClosed SQLException without propagating");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // disconnect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("disconnect() with null connection does not throw exception")
    void disconnect_withNullConnection_doesNotThrow() {
        // Arrange – connection is null by default
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should handle null connection gracefully");
    }

    @Test
    @DisplayName("disconnect() with null connection produces no output")
    void disconnect_withNullConnection_producesNoOutput() {
        // Arrange & Act
        databaseService.disconnect();

        // Assert
        String output = outContent.toString();
        assertFalse(output.contains("Database connection closed"),
                "Should not print closed message when connection is null");
    }

    @Test
    @DisplayName("disconnect() with mock open connection closes it and prints message")
    void disconnect_withMockOpenConnection_closesConnectionAndPrintsMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.disconnect();

        // Assert
        verify(mockConnection).close();
        String output = outContent.toString();
        assertTrue(output.contains("Database connection closed"),
                "Should print database connection closed message");
    }

    @Test
    @DisplayName("disconnect() with mock already-closed connection does not call close()")
    void disconnect_withMockAlreadyClosedConnection_doesNotCallClose() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(true);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.disconnect();

        // Assert
        verify(mockConnection, never()).close();
        String output = outContent.toString();
        assertFalse(output.contains("Database connection closed"),
                "Should not print closed message when connection is already closed");
    }

    @Test
    @DisplayName("disconnect() handles SQLException from close() gracefully")
    void disconnect_whenCloseThrowsSQLException_printsError() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should handle SQLException without propagating");

        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Failed to close database connection"),
                "Should print failed to close connection error");
    }

    @Test
    @DisplayName("disconnect() can be called multiple times without throwing")
    void disconnect_calledMultipleTimes_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
        }, "Multiple disconnect() calls should not throw exceptions");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Constants Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DB_URL constant is correctly formed")
    void staticConstants_dbUrl_isCorrectlyFormed() throws Exception {
        Field dbUrlField = DatabaseService.class.getDeclaredField("DB_URL");
        dbUrlField.setAccessible(true);
        String dbUrl = (String) dbUrlField.get(null);

        assertNotNull(dbUrl, "DB_URL should not be null");
        assertTrue(dbUrl.startsWith("jdbc:mysql://"),
                "DB_URL should start with jdbc:mysql://");
        assertTrue(dbUrl.contains("localhost"),
                "DB_URL should contain localhost");
        assertTrue(dbUrl.contains("3306"),
                "DB_URL should contain port 3306");
        assertTrue(dbUrl.contains("mini_app_db"),
                "DB_URL should contain database name");
    }

    @Test
    @DisplayName("REDIS_PORT constant has expected value")
    void staticConstants_redisPort_hasExpectedValue() throws Exception {
        Field redisPortField = DatabaseService.class.getDeclaredField("REDIS_PORT");
        redisPortField.setAccessible(true);
        int redisPort = (int) redisPortField.get(null);

        assertEquals(6379, redisPort, "REDIS_PORT should be 6379");
    }

    @Test
    @DisplayName("DB_USERNAME constant has expected value")
    void staticConstants_dbUsername_hasExpectedValue() throws Exception {
        Field dbUsernameField = DatabaseService.class.getDeclaredField("DB_USERNAME");
        dbUsernameField.setAccessible(true);
        String dbUsername = (String) dbUsernameField.get(null);

        assertEquals("root", dbUsername, "DB_USERNAME should be 'root'");
    }

    @Test
    @DisplayName("EXTERNAL_API_URL constant is not null or empty")
    void staticConstants_externalApiUrl_isNotNullOrEmpty() throws Exception {
        Field apiUrlField = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        apiUrlField.setAccessible(true);
        String apiUrl = (String) apiUrlField.get(null);

        assertNotNull(apiUrl, "EXTERNAL_API_URL should not be null");
        assertFalse(apiUrl.isEmpty(), "EXTERNAL_API_URL should not be empty");
        assertTrue(apiUrl.startsWith("http"), "EXTERNAL_API_URL should start with http");
    }
}
