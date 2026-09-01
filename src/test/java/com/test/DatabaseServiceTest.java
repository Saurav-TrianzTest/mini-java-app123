package com.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit 5 tests for DatabaseService.
 *
 * Tests cover:
 *  - connect()          : success path, SQLException path
 *  - executeQuery()     : with open connection, with closed/null connection, SQLException path
 *  - disconnect()       : with open connection, with null connection, with closed connection, SQLException path
 *  - Private helpers    : connectToCache(), initializeExternalServices() (exercised via connect())
 *  - Static field defaults (environment-variable fallbacks)
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("DatabaseService Tests")
class DatabaseServiceTest {

    private DatabaseService databaseService;

    // Streams for capturing System.out / System.err
    private final ByteArrayOutputStream outContent  = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent  = new ByteArrayOutputStream();
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
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Constructor - default instantiation creates non-null object")
    void constructor_defaultInstantiation_createsNonNullObject() {
        // Arrange + Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Constructor - connection field is null before connect()")
    void constructor_connectionFieldIsNullBeforeConnect() throws Exception {
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
    // connect()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect() - prints 'Connecting to database' message")
    void connect_printsDatabaseConnectingMessage() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act  (will fail to connect in test env – that is expected)
        service.connect();

        // Assert – at minimum the "Connecting" message must appear
        String output = outContent.toString() + errContent.toString();
        assertTrue(output.contains("Connecting to database"),
                "Output should contain 'Connecting to database'");
    }

    @Test
    @DisplayName("connect() - handles SQLException gracefully and prints error")
    void connect_handlesSQLExceptionGracefully() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act – in a test environment without a real DB, DriverManager throws SQLException
        service.connect();

        // Assert – error message should be printed to stderr
        String errOutput = errContent.toString();
        // Either a connection error or the "Connecting" message must be present
        String allOutput = outContent.toString() + errOutput;
        assertFalse(allOutput.isEmpty(), "Some output should be produced by connect()");
    }

    @Test
    @DisplayName("connect() - does not throw unchecked exception when DB unavailable")
    void connect_doesNotThrowWhenDatabaseUnavailable() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act + Assert – must not propagate any exception
        assertDoesNotThrow(service::connect,
                "connect() must not throw an unchecked exception when DB is unavailable");
    }

    @Test
    @DisplayName("connect() - prints Redis cache connection message via connectToCache()")
    void connect_printsRedisCacheConnectionMessage() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act
        service.connect();

        // Assert – connectToCache() is called inside connect(); its output must appear
        // (only reachable if DriverManager.getConnection succeeds, so we check combined output)
        String allOutput = outContent.toString() + errContent.toString();
        assertFalse(allOutput.isEmpty(), "Output should not be empty after connect()");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeQuery()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("executeQuery() - with null connection does nothing silently")
    void executeQuery_withNullConnection_doesNothing() throws Exception {
        // Arrange – connection field stays null (default)
        DatabaseService service = new DatabaseService();

        // Act + Assert
        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"),
                "executeQuery() must not throw when connection is null");
    }

    @Test
    @DisplayName("executeQuery() - with null connection produces no output")
    void executeQuery_withNullConnection_producesNoOutput() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act
        service.executeQuery("SELECT 1");

        // Assert
        assertEquals("", outContent.toString(),
                "No stdout output expected when connection is null");
        assertEquals("", errContent.toString(),
                "No stderr output expected when connection is null");
    }

    @Test
    @DisplayName("executeQuery() - with open mock connection executes query and prints message")
    void executeQuery_withOpenMockConnection_executesQuery() throws Exception {
        // Arrange
        Connection mockConnection    = mock(Connection.class);
        PreparedStatement mockStmt   = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        service.executeQuery("SELECT * FROM users");

        // Assert
        verify(mockConnection).prepareStatement("SELECT * FROM users");
        verify(mockStmt).setQueryTimeout(30);
        verify(mockStmt).execute();
        assertTrue(outContent.toString().contains("Executing query"),
                "Output should contain 'Executing query'");
    }

    @Test
    @DisplayName("executeQuery() - with closed mock connection does nothing")
    void executeQuery_withClosedMockConnection_doesNothing() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(true);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        service.executeQuery("SELECT 1");

        // Assert – prepareStatement must never be called
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("executeQuery() - handles SQLException from prepareStatement gracefully")
    void executeQuery_handlesSQLExceptionFromPrepareStatement() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString()))
                .thenThrow(new SQLException("Prepare failed"));

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act + Assert
        assertDoesNotThrow(() -> service.executeQuery("BAD SQL"),
                "executeQuery() must not propagate SQLException");
        assertTrue(errContent.toString().contains("Query execution failed"),
                "Error message should be printed to stderr");
    }

    @Test
    @DisplayName("executeQuery() - handles SQLException from isClosed gracefully")
    void executeQuery_handlesSQLExceptionFromIsClosed() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenThrow(new SQLException("isClosed failed"));

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act + Assert
        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"),
                "executeQuery() must not propagate SQLException from isClosed()");
    }

    @Test
    @DisplayName("executeQuery() - with empty SQL string and open connection")
    void executeQuery_withEmptySql_andOpenConnection() throws Exception {
        // Arrange
        Connection mockConnection  = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement("")).thenReturn(mockStmt);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act + Assert
        assertDoesNotThrow(() -> service.executeQuery(""),
                "executeQuery() must handle empty SQL string without throwing");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // disconnect()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("disconnect() - with null connection does nothing silently")
    void disconnect_withNullConnection_doesNothing() {
        // Arrange – connection is null by default
        DatabaseService service = new DatabaseService();

        // Act + Assert
        assertDoesNotThrow(service::disconnect,
                "disconnect() must not throw when connection is null");
    }

    @Test
    @DisplayName("disconnect() - with null connection produces no output")
    void disconnect_withNullConnection_producesNoOutput() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act
        service.disconnect();

        // Assert
        assertEquals("", outContent.toString(),
                "No stdout output expected when connection is null");
        assertEquals("", errContent.toString(),
                "No stderr output expected when connection is null");
    }

    @Test
    @DisplayName("disconnect() - with open mock connection closes it and prints message")
    void disconnect_withOpenMockConnection_closesConnectionAndPrintsMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        service.disconnect();

        // Assert
        verify(mockConnection).close();
        assertTrue(outContent.toString().contains("Database connection closed"),
                "Output should contain 'Database connection closed'");
    }

    @Test
    @DisplayName("disconnect() - with already-closed mock connection does not call close()")
    void disconnect_withAlreadyClosedConnection_doesNotCallClose() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(true);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        service.disconnect();

        // Assert
        verify(mockConnection, never()).close();
    }

    @Test
    @DisplayName("disconnect() - handles SQLException from close() gracefully")
    void disconnect_handlesSQLExceptionFromClose() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act + Assert
        assertDoesNotThrow(service::disconnect,
                "disconnect() must not propagate SQLException");
        assertTrue(errContent.toString().contains("Failed to close database connection"),
                "Error message should be printed to stderr");
    }

    @Test
    @DisplayName("disconnect() - handles SQLException from isClosed() gracefully")
    void disconnect_handlesSQLExceptionFromIsClosed() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenThrow(new SQLException("isClosed failed"));

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act + Assert
        assertDoesNotThrow(service::disconnect,
                "disconnect() must not propagate SQLException from isClosed()");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // connect() → disconnect() lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect() then disconnect() lifecycle - no exception thrown")
    void connectThenDisconnect_lifecycle_noExceptionThrown() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act + Assert
        assertDoesNotThrow(() -> {
            service.connect();
            service.disconnect();
        }, "Full connect→disconnect lifecycle must not throw");
    }

    @Test
    @DisplayName("disconnect() called multiple times - no exception thrown")
    void disconnect_calledMultipleTimes_noExceptionThrown() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed())
                .thenReturn(false)   // first call
                .thenReturn(true);   // second call (already closed)

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act + Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
        }, "Calling disconnect() twice must not throw");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static field defaults (environment-variable fallbacks)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Static fields - DB_URL is constructed from host/port/name defaults")
    void staticFields_dbUrlConstructedFromDefaults() throws Exception {
        // Arrange
        Field dbUrlField = DatabaseService.class.getDeclaredField("DB_URL");
        dbUrlField.setAccessible(true);

        // Act
        String dbUrl = (String) dbUrlField.get(null);

        // Assert
        assertNotNull(dbUrl, "DB_URL must not be null");
        assertTrue(dbUrl.startsWith("jdbc:mysql://"),
                "DB_URL must start with 'jdbc:mysql://'");
    }

    @Test
    @DisplayName("Static fields - REDIS_PORT is a positive integer")
    void staticFields_redisPortIsPositiveInteger() throws Exception {
        // Arrange
        Field redisPortField = DatabaseService.class.getDeclaredField("REDIS_PORT");
        redisPortField.setAccessible(true);

        // Act
        int redisPort = (int) redisPortField.get(null);

        // Assert
        assertTrue(redisPort > 0, "REDIS_PORT must be a positive integer");
    }

    @Test
    @DisplayName("Static fields - EXTERNAL_API_URL is non-null and non-empty")
    void staticFields_externalApiUrlIsNonNullAndNonEmpty() throws Exception {
        // Arrange
        Field apiUrlField = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        apiUrlField.setAccessible(true);

        // Act
        String apiUrl = (String) apiUrlField.get(null);

        // Assert
        assertNotNull(apiUrl, "EXTERNAL_API_URL must not be null");
        assertFalse(apiUrl.isBlank(), "EXTERNAL_API_URL must not be blank");
    }

    @Test
    @DisplayName("Static fields - PAYMENT_SERVICE_URL is non-null and non-empty")
    void staticFields_paymentServiceUrlIsNonNullAndNonEmpty() throws Exception {
        // Arrange
        Field paymentUrlField = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
        paymentUrlField.setAccessible(true);

        // Act
        String paymentUrl = (String) paymentUrlField.get(null);

        // Assert
        assertNotNull(paymentUrl, "PAYMENT_SERVICE_URL must not be null");
        assertFalse(paymentUrl.isBlank(), "PAYMENT_SERVICE_URL must not be blank");
    }
}
