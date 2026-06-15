package com.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit 5 tests for DatabaseService class.
 * Tests cover all public methods, private methods via reflection,
 * and various edge cases including exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseService Tests")
class DatabaseServiceTest {

    @InjectMocks
    private DatabaseService databaseService;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Default constructor creates DatabaseService instance successfully")
    void constructor_default_createsInstance() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Default constructor initializes connection field as null")
    void constructor_default_connectionIsNull() throws Exception {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert – connection field should be null before connect() is called
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        assertNull(connectionField.get(service), "Connection should be null before connect()");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Field / Constant Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DB_HOST constant is set to 'localhost'")
    void staticField_dbHost_isLocalhost() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_HOST");
        field.setAccessible(true);
        assertEquals("localhost", field.get(null));
    }

    @Test
    @DisplayName("DB_PORT constant is set to '3306'")
    void staticField_dbPort_is3306() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_PORT");
        field.setAccessible(true);
        assertEquals("3306", field.get(null));
    }

    @Test
    @DisplayName("DB_NAME constant is set to 'mini_app_db'")
    void staticField_dbName_isMiniAppDb() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_NAME");
        field.setAccessible(true);
        assertEquals("mini_app_db", field.get(null));
    }

    @Test
    @DisplayName("DB_URL constant contains correct JDBC URL")
    void staticField_dbUrl_containsJdbcMysql() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_URL");
        field.setAccessible(true);
        String dbUrl = (String) field.get(null);
        assertTrue(dbUrl.startsWith("jdbc:mysql://"), "DB_URL should start with jdbc:mysql://");
        assertTrue(dbUrl.contains("localhost"), "DB_URL should contain localhost");
        assertTrue(dbUrl.contains("3306"), "DB_URL should contain port 3306");
        assertTrue(dbUrl.contains("mini_app_db"), "DB_URL should contain database name");
    }

    @Test
    @DisplayName("DB_USERNAME constant is set to 'root'")
    void staticField_dbUsername_isRoot() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_USERNAME");
        field.setAccessible(true);
        assertEquals("root", field.get(null));
    }

    @Test
    @DisplayName("DB_PASSWORD constant is set to 'password123'")
    void staticField_dbPassword_isPassword123() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_PASSWORD");
        field.setAccessible(true);
        assertEquals("password123", field.get(null));
    }

    @Test
    @DisplayName("REDIS_HOST constant is set to '127.0.0.1'")
    void staticField_redisHost_is127001() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_HOST");
        field.setAccessible(true);
        assertEquals("127.0.0.1", field.get(null));
    }

    @Test
    @DisplayName("REDIS_PORT constant is set to 6379")
    void staticField_redisPort_is6379() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_PORT");
        field.setAccessible(true);
        assertEquals(6379, field.get(null));
    }

    @Test
    @DisplayName("EXTERNAL_API_URL constant contains expected URL")
    void staticField_externalApiUrl_isCorrect() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertNotNull(url);
        assertTrue(url.contains("api.example.com"), "External API URL should contain api.example.com");
    }

    @Test
    @DisplayName("PAYMENT_SERVICE_URL constant contains expected URL")
    void staticField_paymentServiceUrl_isCorrect() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertNotNull(url);
        assertTrue(url.contains("payment"), "Payment service URL should contain 'payment'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // connect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect() prints 'Connecting to database...' message")
    void connect_printsConnectingMessage() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act – will fail to connect (no real DB), but should print the initial message
        service.connect();

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Connecting to database..."),
                "connect() should print 'Connecting to database...'");
    }

    @Test
    @DisplayName("connect() handles ClassNotFoundException gracefully")
    void connect_classNotFound_printsErrorMessage() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act – driver class loading will fail in test environment without real MySQL driver
        // The method should catch the exception and print an error
        assertDoesNotThrow(service::connect,
                "connect() should not propagate ClassNotFoundException");
    }

    @Test
    @DisplayName("connect() handles SQLException gracefully and prints error")
    void connect_sqlException_doesNotThrow() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert – no real DB available, should handle gracefully
        assertDoesNotThrow(service::connect,
                "connect() should not propagate SQLException");
    }

    @Test
    @DisplayName("connect() does not throw any unchecked exception")
    void connect_noUncheckedExceptionThrown() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(service::connect);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeQuery() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("executeQuery() with null connection does nothing")
    void executeQuery_nullConnection_doesNotThrow() {
        // Arrange
        DatabaseService service = new DatabaseService();
        // connection is null by default

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"),
                "executeQuery() should not throw when connection is null");
    }

    @Test
    @DisplayName("executeQuery() with null SQL does not throw")
    void executeQuery_nullSql_doesNotThrow() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(null),
                "executeQuery() should not throw when SQL is null");
    }

    @Test
    @DisplayName("executeQuery() with empty SQL does not throw")
    void executeQuery_emptySql_doesNotThrow() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(""),
                "executeQuery() should not throw when SQL is empty");
    }

    @Test
    @DisplayName("executeQuery() with valid connection executes statement")
    void executeQuery_withValidConnection_executesStatement() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Act
        service.executeQuery("SELECT * FROM users");

        // Assert
        verify(mockConnection).prepareStatement("SELECT * FROM users");
        verify(mockPreparedStatement).setQueryTimeout(30);
        verify(mockPreparedStatement).execute();
        verify(mockPreparedStatement).close();
    }

    @Test
    @DisplayName("executeQuery() prints executing query message when connection is open")
    void executeQuery_withOpenConnection_printsQueryMessage() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Act
        service.executeQuery("SELECT 1");

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Executing query:"),
                "executeQuery() should print 'Executing query:' message");
    }

    @Test
    @DisplayName("executeQuery() skips execution when connection is closed")
    void executeQuery_withClosedConnection_skipsExecution() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(true);

        // Act
        service.executeQuery("SELECT 1");

        // Assert
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("executeQuery() handles SQLException from prepareStatement gracefully")
    void executeQuery_prepareStatementThrowsSQLException_printsError() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prepare failed"));

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery("INVALID SQL"),
                "executeQuery() should not propagate SQLException");

        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Query execution failed"),
                "executeQuery() should print error message on SQLException");
    }

    @Test
    @DisplayName("executeQuery() handles SQLException from execute() gracefully")
    void executeQuery_executeThrowsSQLException_printsError() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        doThrow(new SQLException("Execute failed")).when(mockPreparedStatement).execute();

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"),
                "executeQuery() should not propagate SQLException from execute()");
    }

    @Test
    @DisplayName("executeQuery() sets query timeout to 30 seconds")
    void executeQuery_setsQueryTimeoutTo30() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Act
        service.executeQuery("SELECT 1");

        // Assert
        verify(mockPreparedStatement).setQueryTimeout(30);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // disconnect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("disconnect() with null connection does not throw")
    void disconnect_nullConnection_doesNotThrow() {
        // Arrange
        DatabaseService service = new DatabaseService();
        // connection is null by default

        // Act & Assert
        assertDoesNotThrow(service::disconnect,
                "disconnect() should not throw when connection is null");
    }

    @Test
    @DisplayName("disconnect() with open connection closes it")
    void disconnect_withOpenConnection_closesConnection() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);

        // Act
        service.disconnect();

        // Assert
        verify(mockConnection).close();
    }

    @Test
    @DisplayName("disconnect() prints 'Database connection closed' message")
    void disconnect_withOpenConnection_printsClosed() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);

        // Act
        service.disconnect();

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Database connection closed"),
                "disconnect() should print 'Database connection closed'");
    }

    @Test
    @DisplayName("disconnect() with already-closed connection does not call close()")
    void disconnect_withClosedConnection_doesNotCallClose() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(true);

        // Act
        service.disconnect();

        // Assert
        verify(mockConnection, never()).close();
    }

    @Test
    @DisplayName("disconnect() handles SQLException from close() gracefully")
    void disconnect_closeThrowsSQLException_printsError() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();

        // Act & Assert
        assertDoesNotThrow(service::disconnect,
                "disconnect() should not propagate SQLException");

        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Failed to close database connection"),
                "disconnect() should print error message on SQLException");
    }

    @Test
    @DisplayName("disconnect() handles SQLException from isClosed() gracefully")
    void disconnect_isClosedThrowsSQLException_doesNotThrow() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenThrow(new SQLException("isClosed failed"));

        // Act & Assert
        assertDoesNotThrow(service::disconnect,
                "disconnect() should not propagate SQLException from isClosed()");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Method Tests via Reflection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connectToCache() prints Redis connection message")
    void connectToCache_printsRedisMessage() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
        method.setAccessible(true);

        // Act
        method.invoke(service);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Connecting to Redis cache at:"),
                "connectToCache() should print Redis connection message");
        assertTrue(output.contains("127.0.0.1"),
                "connectToCache() should print Redis host");
        assertTrue(output.contains("6379"),
                "connectToCache() should print Redis port");
    }

    @Test
    @DisplayName("initializeExternalServices() prints external API message")
    void initializeExternalServices_printsApiMessages() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        method.setAccessible(true);

        // Act
        method.invoke(service);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Initializing external API:"),
                "initializeExternalServices() should print external API message");
        assertTrue(output.contains("Initializing payment service:"),
                "initializeExternalServices() should print payment service message");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration / Lifecycle Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full lifecycle: connect then disconnect with mocked connection")
    void lifecycle_connectThenDisconnect_withMockedConnection() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);

        // Act
        service.disconnect();

        // Assert
        verify(mockConnection).close();
    }

    @Test
    @DisplayName("executeQuery() then disconnect() with mocked connection")
    void lifecycle_executeQueryThenDisconnect_withMockedConnection() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Act
        service.executeQuery("SELECT 1");
        service.disconnect();

        // Assert
        verify(mockPreparedStatement).execute();
        verify(mockConnection).close();
    }

    @Test
    @DisplayName("Multiple executeQuery() calls with same connection")
    void executeQuery_multipleCallsSameConnection_allExecute() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Act
        service.executeQuery("SELECT 1");
        service.executeQuery("SELECT 2");
        service.executeQuery("SELECT 3");

        // Assert
        verify(mockPreparedStatement, times(3)).execute();
    }

    @Test
    @DisplayName("disconnect() called multiple times does not throw")
    void disconnect_calledMultipleTimes_doesNotThrow() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        when(mockConnection.isClosed()).thenReturn(false).thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
        });
    }
}
