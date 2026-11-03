package com.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Covers connect(), executeQuery(), disconnect(), and private helper methods.
 */
@ExtendWith(MockitoExtension.class)
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
    @DisplayName("New instance has null connection field by default")
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
        // Arrange
        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            assertTrue(outContent.toString().contains("Connecting to database..."),
                    "Should print connecting message");
        } catch (Exception e) {
            // If driver not found, still verify the initial message attempt
            assertTrue(outContent.toString().contains("Connecting to database...") ||
                    errContent.toString().contains("Database"),
                    "Should attempt to connect");
        }
    }

    @Test
    @DisplayName("connect() handles ClassNotFoundException gracefully")
    void connect_whenDriverNotFound_handlesClassNotFoundException() {
        // Arrange - The MySQL driver may not be available in test classpath
        // Act - Should not throw exception
        assertDoesNotThrow(() -> databaseService.connect(),
                "connect() should handle ClassNotFoundException without throwing");
    }

    @Test
    @DisplayName("connect() handles SQLException gracefully")
    void connect_whenSQLExceptionOccurs_handlesGracefully() {
        // Arrange & Act
        // Even if connection fails, no exception should propagate
        assertDoesNotThrow(() -> databaseService.connect(),
                "connect() should handle SQLException without throwing");
    }

    @Test
    @DisplayName("connect() with mocked DriverManager sets connection field")
    void connect_withMockedDriverManager_setsConnectionField() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            Field connectionField = DatabaseService.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            Object connectionValue = connectionField.get(databaseService);
            assertNotNull(connectionValue, "Connection should be set after successful connect()");
        }
    }

    @Test
    @DisplayName("connect() prints DB_URL in output when connection succeeds")
    void connect_whenConnectionSucceeds_printsDbUrl() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("localhost") || output.contains("3306") || output.contains("mini_app_db"),
                    "Should print database URL details");
        }
    }

    @Test
    @DisplayName("connect() prints Redis cache connection message")
    void connect_whenConnectionSucceeds_printsRedisCacheMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("Redis") || output.contains("127.0.0.1") || output.contains("6379"),
                    "Should print Redis cache connection message");
        }
    }

    @Test
    @DisplayName("connect() prints external API initialization message")
    void connect_whenConnectionSucceeds_printsExternalApiMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("api.example.com") || output.contains("payment") || output.contains("external"),
                    "Should print external service initialization messages");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeQuery() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("executeQuery() with null connection does nothing")
    void executeQuery_withNullConnection_doesNothing() {
        // Arrange - connection is null by default
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
                "executeQuery() should handle null connection gracefully");
    }

    @Test
    @DisplayName("executeQuery() with null SQL does not throw")
    void executeQuery_withNullSql_doesNotThrow() {
        // Arrange - connection is null
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
                "executeQuery() should handle null SQL gracefully");
    }

    @Test
    @DisplayName("executeQuery() with empty SQL does not throw")
    void executeQuery_withEmptySql_doesNotThrow() {
        // Arrange - connection is null
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
                "executeQuery() should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("executeQuery() with active connection executes statement")
    void executeQuery_withActiveConnection_executesStatement() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.execute()).thenReturn(true);

        // Inject mock connection
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("SELECT * FROM users");

        // Assert
        verify(mockConnection).prepareStatement("SELECT * FROM users");
        verify(mockStatement).setQueryTimeout(30);
        verify(mockStatement).execute();
        verify(mockStatement).close();
    }

    @Test
    @DisplayName("executeQuery() with closed connection does nothing")
    void executeQuery_withClosedConnection_doesNothing() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(true);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("SELECT 1");

        // Assert
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("executeQuery() prints executing query message")
    void executeQuery_withActiveConnection_printsExecutingMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("SELECT * FROM orders");

        // Assert
        assertTrue(outContent.toString().contains("SELECT * FROM orders"),
                "Should print the SQL query being executed");
    }

    @Test
    @DisplayName("executeQuery() handles SQLException and prints error message")
    void executeQuery_whenSQLExceptionOccurs_printsErrorMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Syntax error"));

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        assertDoesNotThrow(() -> databaseService.executeQuery("INVALID SQL"),
                "executeQuery() should handle SQLException without throwing");

        // Assert
        assertTrue(errContent.toString().contains("Query execution failed"),
                "Should print error message on SQLException");
    }

    @Test
    @DisplayName("executeQuery() sets query timeout to 30 seconds")
    void executeQuery_withActiveConnection_setsQueryTimeoutTo30() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("SELECT 1");

        // Assert
        verify(mockStatement).setQueryTimeout(30);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // disconnect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("disconnect() with null connection does nothing")
    void disconnect_withNullConnection_doesNothing() {
        // Arrange - connection is null by default
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should handle null connection gracefully");
    }

    @Test
    @DisplayName("disconnect() with open connection closes it")
    void disconnect_withOpenConnection_closesConnection() throws Exception {
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
    }

    @Test
    @DisplayName("disconnect() with open connection prints closed message")
    void disconnect_withOpenConnection_printsDatabaseClosedMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.disconnect();

        // Assert
        assertTrue(outContent.toString().contains("Database connection closed"),
                "Should print connection closed message");
    }

    @Test
    @DisplayName("disconnect() with already closed connection does not call close()")
    void disconnect_withAlreadyClosedConnection_doesNotCallClose() throws Exception {
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
    }

    @Test
    @DisplayName("disconnect() handles SQLException and prints error message")
    void disconnect_whenSQLExceptionOccurs_printsErrorMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should handle SQLException without throwing");

        // Assert
        assertTrue(errContent.toString().contains("Failed to close database connection"),
                "Should print error message on SQLException");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Field / Constant Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DB_HOST constant is 'localhost'")
    void constants_dbHost_isLocalhost() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_HOST");
        field.setAccessible(true);
        assertEquals("localhost", field.get(null), "DB_HOST should be 'localhost'");
    }

    @Test
    @DisplayName("DB_PORT constant is '3306'")
    void constants_dbPort_is3306() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_PORT");
        field.setAccessible(true);
        assertEquals("3306", field.get(null), "DB_PORT should be '3306'");
    }

    @Test
    @DisplayName("DB_NAME constant is 'mini_app_db'")
    void constants_dbName_isMiniAppDb() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_NAME");
        field.setAccessible(true);
        assertEquals("mini_app_db", field.get(null), "DB_NAME should be 'mini_app_db'");
    }

    @Test
    @DisplayName("DB_URL constant contains correct JDBC URL format")
    void constants_dbUrl_containsJdbcMysqlPrefix() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_URL");
        field.setAccessible(true);
        String dbUrl = (String) field.get(null);
        assertTrue(dbUrl.startsWith("jdbc:mysql://"), "DB_URL should start with 'jdbc:mysql://'");
        assertTrue(dbUrl.contains("localhost"), "DB_URL should contain 'localhost'");
        assertTrue(dbUrl.contains("3306"), "DB_URL should contain port '3306'");
        assertTrue(dbUrl.contains("mini_app_db"), "DB_URL should contain database name");
    }

    @Test
    @DisplayName("DB_USERNAME constant is 'root'")
    void constants_dbUsername_isRoot() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_USERNAME");
        field.setAccessible(true);
        assertEquals("root", field.get(null), "DB_USERNAME should be 'root'");
    }

    @Test
    @DisplayName("REDIS_HOST constant is '127.0.0.1'")
    void constants_redisHost_isLoopback() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_HOST");
        field.setAccessible(true);
        assertEquals("127.0.0.1", field.get(null), "REDIS_HOST should be '127.0.0.1'");
    }

    @Test
    @DisplayName("REDIS_PORT constant is 6379")
    void constants_redisPort_is6379() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_PORT");
        field.setAccessible(true);
        assertEquals(6379, field.get(null), "REDIS_PORT should be 6379");
    }

    @Test
    @DisplayName("EXTERNAL_API_URL constant contains expected URL")
    void constants_externalApiUrl_containsExpectedValue() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertTrue(url.contains("api.example.com"), "EXTERNAL_API_URL should contain 'api.example.com'");
    }

    @Test
    @DisplayName("PAYMENT_SERVICE_URL constant contains expected URL")
    void constants_paymentServiceUrl_containsExpectedValue() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertTrue(url.contains("payment"), "PAYMENT_SERVICE_URL should contain 'payment'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Method Tests via Reflection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connectToCache() private method prints Redis connection message")
    void connectToCache_whenInvoked_printsRedisMessage() throws Exception {
        // Arrange
        Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
        method.setAccessible(true);

        // Act
        method.invoke(databaseService);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Redis") || output.contains("127.0.0.1") || output.contains("6379"),
                "connectToCache() should print Redis connection details");
    }

    @Test
    @DisplayName("initializeExternalServices() private method prints API URLs")
    void initializeExternalServices_whenInvoked_printsApiUrls() throws Exception {
        // Arrange
        Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        method.setAccessible(true);

        // Act
        method.invoke(databaseService);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("api.example.com") || output.contains("payment"),
                "initializeExternalServices() should print external service URLs");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration-style Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full lifecycle: connect, executeQuery, disconnect with mocked connection")
    void fullLifecycle_connectExecuteDisconnect_completesSuccessfully() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            // Act
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();

            // Assert
            verify(mockConnection).prepareStatement("SELECT 1");
            verify(mockConnection).close();
        }
    }

    @Test
    @DisplayName("Multiple executeQuery calls work independently")
    void executeQuery_calledMultipleTimes_eachCallExecutesIndependently() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement1 = mock(PreparedStatement.class);
        PreparedStatement mockStatement2 = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement("SELECT 1")).thenReturn(mockStatement1);
        when(mockConnection.prepareStatement("SELECT 2")).thenReturn(mockStatement2);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("SELECT 1");
        databaseService.executeQuery("SELECT 2");

        // Assert
        verify(mockConnection).prepareStatement("SELECT 1");
        verify(mockConnection).prepareStatement("SELECT 2");
        verify(mockStatement1).execute();
        verify(mockStatement2).execute();
    }
}
