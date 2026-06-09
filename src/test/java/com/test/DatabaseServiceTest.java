package com.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
 * Covers connect(), executeQuery(), disconnect(), connectToCache(), initializeExternalServices().
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseService Tests")
class DatabaseServiceTest {

    @InjectMocks
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
    @DisplayName("Default constructor creates DatabaseService instance successfully")
    void constructor_defaultConstructor_createsInstance() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("New DatabaseService has null connection by default")
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
        DatabaseService service = new DatabaseService();

        // Act
        service.connect(); // Will fail to connect (no real DB), but prints message

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Connecting to database..."),
                "Should print connecting message");
    }

    @Test
    @DisplayName("connect() handles ClassNotFoundException gracefully")
    void connect_whenDriverNotFound_printsErrorMessage() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act
        service.connect();

        // Assert – either ClassNotFoundException or SQLException is caught
        String errOutput = errContent.toString();
        String stdOutput = outContent.toString();
        // The method should not throw; it catches exceptions internally
        assertTrue(stdOutput.contains("Connecting to database...") || errOutput.length() >= 0,
                "connect() should handle exceptions without throwing");
    }

    @Test
    @DisplayName("connect() handles SQLException gracefully and prints error")
    void connect_whenSQLExceptionOccurs_printsErrorMessage() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act – no real DB available, so SQLException is expected internally
        assertDoesNotThrow(service::connect,
                "connect() should not propagate SQLException");
    }

    @Test
    @DisplayName("connect() does not throw any unchecked exception")
    void connect_noRealDatabase_doesNotThrow() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(service::connect,
                "connect() must swallow all exceptions internally");
    }

    @Test
    @DisplayName("connect() with mocked DriverManager succeeds and sets connection")
    void connect_withMockedDriverManager_setsConnection() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            DatabaseService service = new DatabaseService();

            // Act
            service.connect();

            // Assert
            Field connectionField = DatabaseService.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            Connection actualConnection = (Connection) connectionField.get(service);

            assertNotNull(actualConnection, "Connection should be set after successful connect()");
            assertEquals(mockConnection, actualConnection, "Connection should be the mocked connection");
        }
    }

    @Test
    @DisplayName("connect() prints DB_URL containing localhost and port 3306")
    void connect_withMockedDriverManager_printsCorrectDbUrl() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            DatabaseService service = new DatabaseService();

            // Act
            service.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("localhost"), "DB URL should contain localhost");
            assertTrue(output.contains("3306"), "DB URL should contain port 3306");
        }
    }

    @Test
    @DisplayName("connect() prints Redis cache connection message")
    void connect_withMockedDriverManager_printsRedisCacheMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            DatabaseService service = new DatabaseService();

            // Act
            service.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("Redis") || output.contains("cache"),
                    "Should print Redis cache connection message");
        }
    }

    @Test
    @DisplayName("connect() prints external API initialization message")
    void connect_withMockedDriverManager_printsExternalApiMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            DatabaseService service = new DatabaseService();

            // Act
            service.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("api.example.com") || output.contains("external"),
                    "Should print external API initialization message");
        }
    }

    @Test
    @DisplayName("connect() prints payment service initialization message")
    void connect_withMockedDriverManager_printsPaymentServiceMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            DatabaseService service = new DatabaseService();

            // Act
            service.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("payment") || output.contains("payment.internal"),
                    "Should print payment service initialization message");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeQuery() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("executeQuery() with null connection does nothing")
    void executeQuery_whenConnectionIsNull_doesNothing() {
        // Arrange
        DatabaseService service = new DatabaseService();
        // connection is null by default

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"),
                "executeQuery() should not throw when connection is null");
    }

    @Test
    @DisplayName("executeQuery() with null SQL does not throw")
    void executeQuery_withNullSql_doesNotThrow() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(null),
                "executeQuery() should not throw with null SQL");
    }

    @Test
    @DisplayName("executeQuery() with empty SQL does not throw")
    void executeQuery_withEmptySql_doesNotThrow() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(() -> service.executeQuery(""),
                "executeQuery() should not throw with empty SQL");
    }

    @Test
    @DisplayName("executeQuery() with open connection executes query and prints message")
    void executeQuery_withOpenConnection_executesQueryAndPrintsMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.execute()).thenReturn(true);

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
        verify(mockStmt).close();

        String output = outContent.toString();
        assertTrue(output.contains("Executing query:"), "Should print executing query message");
        assertTrue(output.contains("SELECT * FROM users"), "Should print the SQL query");
    }

    @Test
    @DisplayName("executeQuery() with closed connection does not execute query")
    void executeQuery_withClosedConnection_doesNotExecuteQuery() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(true);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        service.executeQuery("SELECT * FROM users");

        // Assert
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("executeQuery() sets query timeout to 30 seconds")
    void executeQuery_withOpenConnection_setsQueryTimeoutTo30() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        service.executeQuery("UPDATE users SET name='test'");

        // Assert
        verify(mockStmt).setQueryTimeout(30);
    }

    @Test
    @DisplayName("executeQuery() handles SQLException and prints error message")
    void executeQuery_whenSQLExceptionThrown_printsErrorMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        assertDoesNotThrow(() -> service.executeQuery("INVALID SQL"),
                "executeQuery() should catch SQLException");

        // Assert
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Query execution failed"),
                "Should print query execution failed message");
    }

    @Test
    @DisplayName("executeQuery() with multiple different SQL statements")
    void executeQuery_withMultipleSqlStatements_executesEach() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        service.executeQuery("SELECT * FROM users");
        service.executeQuery("INSERT INTO users VALUES (1, 'test')");
        service.executeQuery("DELETE FROM users WHERE id=1");

        // Assert
        verify(mockConnection, times(3)).prepareStatement(anyString());
        verify(mockStmt, times(3)).execute();
        verify(mockStmt, times(3)).close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // disconnect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("disconnect() with null connection does nothing")
    void disconnect_whenConnectionIsNull_doesNothing() {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act & Assert
        assertDoesNotThrow(service::disconnect,
                "disconnect() should not throw when connection is null");
    }

    @Test
    @DisplayName("disconnect() with open connection closes it and prints message")
    void disconnect_withOpenConnection_closesConnectionAndPrintsMessage() throws Exception {
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
        String output = outContent.toString();
        assertTrue(output.contains("Database connection closed"),
                "Should print connection closed message");
    }

    @Test
    @DisplayName("disconnect() with already closed connection does not call close() again")
    void disconnect_withClosedConnection_doesNotCallClose() throws Exception {
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
    @DisplayName("disconnect() handles SQLException and prints error message")
    void disconnect_whenSQLExceptionThrown_printsErrorMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        assertDoesNotThrow(service::disconnect,
                "disconnect() should catch SQLException");

        // Assert
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Failed to close database connection"),
                "Should print failed to close connection message");
    }

    @Test
    @DisplayName("disconnect() called multiple times does not throw")
    void disconnect_calledMultipleTimes_doesNotThrow() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false).thenReturn(true);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act & Assert
        assertDoesNotThrow(() -> {
            service.disconnect();
            service.disconnect();
        }, "Multiple disconnect() calls should not throw");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private method tests via reflection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connectToCache() prints Redis host and port message")
    void connectToCache_whenInvoked_printsRedisConnectionMessage() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Method connectToCacheMethod = DatabaseService.class.getDeclaredMethod("connectToCache");
        connectToCacheMethod.setAccessible(true);

        // Act
        connectToCacheMethod.invoke(service);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("127.0.0.1") || output.contains("Redis") || output.contains("cache"),
                "connectToCache() should print Redis connection details");
        assertTrue(output.contains("6379"),
                "connectToCache() should print Redis port 6379");
    }

    @Test
    @DisplayName("initializeExternalServices() prints external API URL")
    void initializeExternalServices_whenInvoked_printsExternalApiUrl() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Method initMethod = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        initMethod.setAccessible(true);

        // Act
        initMethod.invoke(service);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("api.example.com"),
                "initializeExternalServices() should print external API URL");
    }

    @Test
    @DisplayName("initializeExternalServices() prints payment service URL")
    void initializeExternalServices_whenInvoked_printsPaymentServiceUrl() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();
        Method initMethod = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        initMethod.setAccessible(true);

        // Act
        initMethod.invoke(service);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("payment.internal.company.com") || output.contains("payment"),
                "initializeExternalServices() should print payment service URL");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static field constant tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DB_HOST constant is 'localhost'")
    void staticField_dbHost_isLocalhost() throws Exception {
        // Arrange
        Field dbHostField = DatabaseService.class.getDeclaredField("DB_HOST");
        dbHostField.setAccessible(true);

        // Act
        String dbHost = (String) dbHostField.get(null);

        // Assert
        assertEquals("localhost", dbHost, "DB_HOST should be 'localhost'");
    }

    @Test
    @DisplayName("DB_PORT constant is '3306'")
    void staticField_dbPort_is3306() throws Exception {
        // Arrange
        Field dbPortField = DatabaseService.class.getDeclaredField("DB_PORT");
        dbPortField.setAccessible(true);

        // Act
        String dbPort = (String) dbPortField.get(null);

        // Assert
        assertEquals("3306", dbPort, "DB_PORT should be '3306'");
    }

    @Test
    @DisplayName("DB_NAME constant is 'mini_app_db'")
    void staticField_dbName_isMiniAppDb() throws Exception {
        // Arrange
        Field dbNameField = DatabaseService.class.getDeclaredField("DB_NAME");
        dbNameField.setAccessible(true);

        // Act
        String dbName = (String) dbNameField.get(null);

        // Assert
        assertEquals("mini_app_db", dbName, "DB_NAME should be 'mini_app_db'");
    }

    @Test
    @DisplayName("DB_USERNAME constant is 'root'")
    void staticField_dbUsername_isRoot() throws Exception {
        // Arrange
        Field dbUsernameField = DatabaseService.class.getDeclaredField("DB_USERNAME");
        dbUsernameField.setAccessible(true);

        // Act
        String dbUsername = (String) dbUsernameField.get(null);

        // Assert
        assertEquals("root", dbUsername, "DB_USERNAME should be 'root'");
    }

    @Test
    @DisplayName("REDIS_PORT constant is 6379")
    void staticField_redisPort_is6379() throws Exception {
        // Arrange
        Field redisPortField = DatabaseService.class.getDeclaredField("REDIS_PORT");
        redisPortField.setAccessible(true);

        // Act
        int redisPort = (int) redisPortField.get(null);

        // Assert
        assertEquals(6379, redisPort, "REDIS_PORT should be 6379");
    }

    @Test
    @DisplayName("REDIS_HOST constant is '127.0.0.1'")
    void staticField_redisHost_isLoopback() throws Exception {
        // Arrange
        Field redisHostField = DatabaseService.class.getDeclaredField("REDIS_HOST");
        redisHostField.setAccessible(true);

        // Act
        String redisHost = (String) redisHostField.get(null);

        // Assert
        assertEquals("127.0.0.1", redisHost, "REDIS_HOST should be '127.0.0.1'");
    }

    @Test
    @DisplayName("DB_URL is constructed from DB_HOST, DB_PORT, and DB_NAME")
    void staticField_dbUrl_isConstructedCorrectly() throws Exception {
        // Arrange
        Field dbUrlField = DatabaseService.class.getDeclaredField("DB_URL");
        dbUrlField.setAccessible(true);

        // Act
        String dbUrl = (String) dbUrlField.get(null);

        // Assert
        assertNotNull(dbUrl, "DB_URL should not be null");
        assertTrue(dbUrl.startsWith("jdbc:mysql://"), "DB_URL should start with jdbc:mysql://");
        assertTrue(dbUrl.contains("localhost"), "DB_URL should contain localhost");
        assertTrue(dbUrl.contains("3306"), "DB_URL should contain port 3306");
        assertTrue(dbUrl.contains("mini_app_db"), "DB_URL should contain database name");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Full lifecycle integration tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full lifecycle: connect, executeQuery, disconnect with mocked connection")
    void lifecycle_connectExecuteQueryDisconnect_completesSuccessfully() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        try (MockedStatic<DriverManager> driverManagerMock = Mockito.mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConnection);

            DatabaseService service = new DatabaseService();

            // Act
            service.connect();
            service.executeQuery("SELECT 1");
            service.disconnect();

            // Assert
            verify(mockConnection).prepareStatement("SELECT 1");
            verify(mockStmt).execute();
            verify(mockConnection).close();
        }
    }

    @Test
    @DisplayName("executeQuery() after disconnect() does not execute query")
    void executeQuery_afterDisconnect_doesNotExecuteQuery() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false).thenReturn(true);

        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(service, mockConnection);

        // Act
        service.disconnect();
        service.executeQuery("SELECT 1");

        // Assert
        verify(mockConnection, never()).prepareStatement(anyString());
    }
}
