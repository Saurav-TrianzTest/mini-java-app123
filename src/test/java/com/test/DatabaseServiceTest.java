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
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit 5 tests for DatabaseService class.
 * Tests cover: connect(), executeQuery(), disconnect(), and private helper methods.
 * Target: 80%+ code coverage.
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
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("New instance has null connection by default")
    void constructor_newInstance_connectionIsNull() throws Exception {
        DatabaseService service = new DatabaseService();
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        Object connectionValue = connectionField.get(service);
        assertNull(connectionValue, "Connection should be null before connect() is called");
    }

    @Test
    @DisplayName("Multiple instances are independent objects")
    void constructor_multipleInstances_areIndependent() {
        DatabaseService s1 = new DatabaseService();
        DatabaseService s2 = new DatabaseService();
        assertNotSame(s1, s2, "Each instantiation should produce a distinct object");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Field / Constant Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DB_HOST constant is 'localhost'")
    void dbHost_constant_isLocalhost() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_HOST");
        field.setAccessible(true);
        assertEquals("localhost", field.get(null));
    }

    @Test
    @DisplayName("DB_PORT constant is '5432' (PostgreSQL default)")
    void dbPort_constant_isPostgresDefault() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_PORT");
        field.setAccessible(true);
        assertEquals("5432", field.get(null));
    }

    @Test
    @DisplayName("DB_NAME constant is 'mini_app_db'")
    void dbName_constant_isMiniAppDb() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_NAME");
        field.setAccessible(true);
        assertEquals("mini_app_db", field.get(null));
    }

    @Test
    @DisplayName("DB_URL constant uses PostgreSQL JDBC format")
    void dbUrl_constant_usesPostgresJdbcFormat() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertTrue(url.startsWith("jdbc:postgresql://"), "DB_URL should use PostgreSQL JDBC format");
        assertTrue(url.contains("5432"), "DB_URL should contain PostgreSQL port 5432");
        assertTrue(url.contains("mini_app_db"), "DB_URL should contain database name");
    }

    @Test
    @DisplayName("DB_USERNAME constant is 'postgres'")
    void dbUsername_constant_isPostgres() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_USERNAME");
        field.setAccessible(true);
        assertEquals("postgres", field.get(null));
    }

    @Test
    @DisplayName("REDIS_HOST constant is '127.0.0.1'")
    void redisHost_constant_isLoopback() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_HOST");
        field.setAccessible(true);
        assertEquals("127.0.0.1", field.get(null));
    }

    @Test
    @DisplayName("REDIS_PORT constant is 6379")
    void redisPort_constant_is6379() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_PORT");
        field.setAccessible(true);
        assertEquals(6379, field.get(null));
    }

    @Test
    @DisplayName("EXTERNAL_API_URL constant contains expected host")
    void externalApiUrl_constant_containsExpectedHost() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertTrue(url.contains("api.example.com"), "EXTERNAL_API_URL should contain api.example.com");
    }

    @Test
    @DisplayName("PAYMENT_SERVICE_URL constant uses HTTPS")
    void paymentServiceUrl_constant_usesHttps() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertTrue(url.startsWith("https://"), "PAYMENT_SERVICE_URL should use HTTPS");
    }

    @Test
    @DisplayName("All static fields are private and final")
    void staticFields_arePrivateAndFinal() {
        String[] fieldNames = {"DB_HOST", "DB_PORT", "DB_NAME", "DB_URL",
                               "DB_USERNAME", "DB_PASSWORD", "REDIS_HOST",
                               "REDIS_PORT", "EXTERNAL_API_URL", "PAYMENT_SERVICE_URL"};
        for (String name : fieldNames) {
            try {
                Field f = DatabaseService.class.getDeclaredField(name);
                assertTrue(Modifier.isPrivate(f.getModifiers()), name + " should be private");
                assertTrue(Modifier.isStatic(f.getModifiers()), name + " should be static");
                assertTrue(Modifier.isFinal(f.getModifiers()), name + " should be final");
            } catch (NoSuchFieldException e) {
                fail("Field " + name + " not found: " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // connect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect() prints 'Connecting to PostgreSQL database' message")
    void connect_whenCalled_printsConnectingMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            assertTrue(outContent.toString().contains("Connecting to PostgreSQL database"),
                    "Should print connecting message");
        }
    }

    @Test
    @DisplayName("connect() prints 'Connected to PostgreSQL database' on success")
    void connect_onSuccess_printsConnectedMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            assertTrue(outContent.toString().contains("Connected to PostgreSQL database"),
                    "Should print connected message on success");
        }
    }

    @Test
    @DisplayName("connect() prints Redis cache connection message")
    void connect_onSuccess_printsRedisCacheMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            assertTrue(outContent.toString().contains("Connecting to Redis cache"),
                    "Should print Redis cache connection message");
        }
    }

    @Test
    @DisplayName("connect() prints Redis host and port in cache message")
    void connect_onSuccess_printsRedisHostAndPort() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            String output = outContent.toString();
            assertTrue(output.contains("127.0.0.1"), "Should print Redis host");
            assertTrue(output.contains("6379"), "Should print Redis port");
        }
    }

    @Test
    @DisplayName("connect() prints external API initialization message")
    void connect_onSuccess_printsExternalApiMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            assertTrue(outContent.toString().contains("Initializing external API"),
                    "Should print external API initialization message");
        }
    }

    @Test
    @DisplayName("connect() prints payment service initialization message")
    void connect_onSuccess_printsPaymentServiceMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            assertTrue(outContent.toString().contains("Initializing payment service"),
                    "Should print payment service initialization message");
        }
    }

    @Test
    @DisplayName("connect() prints username in output")
    void connect_onSuccess_printsUsername() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            assertTrue(outContent.toString().contains("postgres"),
                    "Should print username in output");
        }
    }

    @Test
    @DisplayName("connect() sets connection field on successful connection")
    void connect_onSuccess_setsConnectionField() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            Field connectionField = DatabaseService.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            assertNotNull(connectionField.get(databaseService),
                    "Connection field should be set after successful connect()");
        }
    }

    @Test
    @DisplayName("connect() sets connection to the mock connection object")
    void connect_onSuccess_setsConnectionToMockObject() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            Field connectionField = DatabaseService.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            assertSame(mockConnection, connectionField.get(databaseService),
                    "Connection field should be the mock connection");
        }
    }

    @Test
    @DisplayName("connect() handles SQLException and prints error to stderr")
    void connect_onSQLException_printsErrorMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenAnswer(inv -> { throw new SQLException("Connection refused"); });
            databaseService.connect();
            assertTrue(errContent.toString().contains("PostgreSQL database connection failed"),
                    "Should print error message on SQLException");
        }
    }

    @Test
    @DisplayName("connect() includes exception message in stderr on SQLException")
    void connect_onSQLException_includesExceptionMessageInStderr() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenAnswer(inv -> { throw new SQLException("Connection refused"); });
            databaseService.connect();
            assertTrue(errContent.toString().contains("Connection refused"),
                    "Should include exception message in stderr");
        }
    }

    @Test
    @DisplayName("connect() does not throw exception when SQLException occurs")
    void connect_onSQLException_doesNotThrow() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenAnswer(inv -> { throw new SQLException("Connection refused"); });
            assertDoesNotThrow(() -> databaseService.connect(),
                    "connect() should not propagate SQLException");
        }
    }

    @Test
    @DisplayName("connect() leaves connection null when SQLException occurs")
    void connect_onSQLException_connectionRemainsNull() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenAnswer(inv -> { throw new SQLException("Connection refused"); });
            databaseService.connect();
            Field connectionField = DatabaseService.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            assertNull(connectionField.get(databaseService),
                    "Connection should remain null after failed connect()");
        }
    }

    @Test
    @DisplayName("connect() uses PostgreSQL JDBC URL format in output")
    void connect_usesPostgreSQLJdbcUrl() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            assertTrue(outContent.toString().contains("jdbc:postgresql://"),
                    "Should use PostgreSQL JDBC URL format");
        }
    }

    @Test
    @DisplayName("connect() calls DriverManager.getConnection with correct URL")
    void connect_callsDriverManagerWithCorrectUrl() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            databaseService.connect();
            dmMock.verify(() -> DriverManager.getConnection(
                    contains("jdbc:postgresql://localhost:5432/mini_app_db"),
                    eq("postgres"),
                    anyString()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeQuery() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("executeQuery() executes SQL when connection is open")
    void executeQuery_withOpenConnection_executesQuery() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.execute()).thenReturn(true);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.executeQuery("SELECT * FROM users");

        verify(mockConnection).prepareStatement("SELECT * FROM users");
        verify(mockStmt).execute();
    }

    @Test
    @DisplayName("executeQuery() sets query timeout to 30 seconds")
    void executeQuery_withOpenConnection_setsQueryTimeout() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.executeQuery("SELECT 1");

        verify(mockStmt).setQueryTimeout(30);
    }

    @Test
    @DisplayName("executeQuery() prints executing query message with SQL")
    void executeQuery_withOpenConnection_printsExecutingMessage() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        String sql = "SELECT * FROM orders";
        databaseService.executeQuery(sql);

        assertTrue(outContent.toString().contains("Executing query: " + sql),
                "Should print executing query message with SQL");
    }

    @Test
    @DisplayName("executeQuery() does nothing when connection is null")
    void executeQuery_withNullConnection_doesNothing() {
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
                "executeQuery() should not throw when connection is null");
    }

    @Test
    @DisplayName("executeQuery() produces no output when connection is null")
    void executeQuery_withNullConnection_producesNoOutput() {
        databaseService.executeQuery("SELECT 1");
        assertTrue(outContent.toString().isEmpty(),
                "executeQuery() should produce no output when connection is null");
    }

    @Test
    @DisplayName("executeQuery() does nothing when connection is closed")
    void executeQuery_withClosedConnection_doesNothing() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(true);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.executeQuery("SELECT 1");

        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("executeQuery() handles SQLException and prints error to stderr")
    void executeQuery_onSQLException_printsErrorMessage() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString()))
                .thenThrow(new SQLException("Query failed"));

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.executeQuery("INVALID SQL");

        assertTrue(errContent.toString().contains("Query execution failed"),
                "Should print error message on SQLException");
    }

    @Test
    @DisplayName("executeQuery() does not throw exception when SQLException occurs")
    void executeQuery_onSQLException_doesNotThrow() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString()))
                .thenThrow(new SQLException("Query failed"));

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        assertDoesNotThrow(() -> databaseService.executeQuery("INVALID SQL"),
                "executeQuery() should not propagate SQLException");
    }

    @Test
    @DisplayName("executeQuery() with empty SQL string does not throw")
    void executeQuery_withEmptySql_doesNotThrow() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        assertDoesNotThrow(() -> databaseService.executeQuery(""),
                "executeQuery() should handle empty SQL string");
    }

    @Test
    @DisplayName("executeQuery() with INSERT SQL executes successfully")
    void executeQuery_withInsertSql_executesSuccessfully() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        String insertSql = "INSERT INTO users (name) VALUES ('test')";
        databaseService.executeQuery(insertSql);

        verify(mockConnection).prepareStatement(insertSql);
    }

    @Test
    @DisplayName("executeQuery() with UPDATE SQL executes successfully")
    void executeQuery_withUpdateSql_executesSuccessfully() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        String updateSql = "UPDATE users SET name='updated' WHERE id=1";
        databaseService.executeQuery(updateSql);

        verify(mockConnection).prepareStatement(updateSql);
    }

    @Test
    @DisplayName("executeQuery() with DELETE SQL executes successfully")
    void executeQuery_withDeleteSql_executesSuccessfully() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        String deleteSql = "DELETE FROM users WHERE id=1";
        databaseService.executeQuery(deleteSql);

        verify(mockConnection).prepareStatement(deleteSql);
    }

    @Test
    @DisplayName("executeQuery() includes exception message in stderr on SQLException")
    void executeQuery_onSQLException_includesExceptionMessageInStderr() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString()))
                .thenThrow(new SQLException("Syntax error near SELECT"));

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.executeQuery("INVALID SQL");

        assertTrue(errContent.toString().contains("Syntax error near SELECT"),
                "Should include exception message in stderr");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // disconnect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("disconnect() closes open connection successfully")
    void disconnect_withOpenConnection_closesConnection() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.disconnect();

        verify(mockConnection).close();
    }

    @Test
    @DisplayName("disconnect() prints 'PostgreSQL database connection closed' on success")
    void disconnect_withOpenConnection_printsClosed() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.disconnect();

        assertTrue(outContent.toString().contains("PostgreSQL database connection closed"),
                "Should print closed message");
    }

    @Test
    @DisplayName("disconnect() does nothing when connection is null")
    void disconnect_withNullConnection_doesNothing() {
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should not throw when connection is null");
    }

    @Test
    @DisplayName("disconnect() produces no output when connection is null")
    void disconnect_withNullConnection_producesNoOutput() {
        databaseService.disconnect();
        assertTrue(outContent.toString().isEmpty(),
                "disconnect() should produce no output when connection is null");
    }

    @Test
    @DisplayName("disconnect() does nothing when connection is already closed")
    void disconnect_withAlreadyClosedConnection_doesNothing() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(true);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.disconnect();

        verify(mockConnection, never()).close();
    }

    @Test
    @DisplayName("disconnect() handles SQLException and prints error to stderr")
    void disconnect_onSQLException_printsErrorMessage() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.disconnect();

        assertTrue(errContent.toString().contains("Failed to close database connection"),
                "Should print error message on SQLException");
    }

    @Test
    @DisplayName("disconnect() does not throw exception when SQLException occurs")
    void disconnect_onSQLException_doesNotThrow() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect() should not propagate SQLException");
    }

    @Test
    @DisplayName("disconnect() includes exception message in stderr on SQLException")
    void disconnect_onSQLException_includesExceptionMessageInStderr() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed - permission denied")).when(mockConnection).close();

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.disconnect();

        assertTrue(errContent.toString().contains("Close failed - permission denied"),
                "Should include exception message in stderr");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Method Structure Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DatabaseService has connectToCache private method")
    void databaseService_hasConnectToCacheMethod() {
        Set<String> methodNames = new HashSet<>();
        for (Method m : DatabaseService.class.getDeclaredMethods()) {
            methodNames.add(m.getName());
        }
        assertTrue(methodNames.contains("connectToCache"),
                "DatabaseService should have connectToCache method");
    }

    @Test
    @DisplayName("DatabaseService has initializeExternalServices private method")
    void databaseService_hasInitializeExternalServicesMethod() {
        Set<String> methodNames = new HashSet<>();
        for (Method m : DatabaseService.class.getDeclaredMethods()) {
            methodNames.add(m.getName());
        }
        assertTrue(methodNames.contains("initializeExternalServices"),
                "DatabaseService should have initializeExternalServices method");
    }

    @Test
    @DisplayName("connectToCache() private method is accessible via reflection")
    void connectToCache_isAccessibleViaReflection() throws Exception {
        Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(databaseService),
                "connectToCache() should be invocable via reflection");
    }

    @Test
    @DisplayName("connectToCache() prints Redis connection message")
    void connectToCache_printsRedisConnectionMessage() throws Exception {
        Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
        method.setAccessible(true);
        method.invoke(databaseService);
        assertTrue(outContent.toString().contains("Connecting to Redis cache"),
                "connectToCache() should print Redis connection message");
    }

    @Test
    @DisplayName("initializeExternalServices() prints external API message")
    void initializeExternalServices_printsExternalApiMessage() throws Exception {
        Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        method.setAccessible(true);
        method.invoke(databaseService);
        assertTrue(outContent.toString().contains("Initializing external API"),
                "initializeExternalServices() should print external API message");
    }

    @Test
    @DisplayName("initializeExternalServices() prints payment service message")
    void initializeExternalServices_printsPaymentServiceMessage() throws Exception {
        Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        method.setAccessible(true);
        method.invoke(databaseService);
        assertTrue(outContent.toString().contains("Initializing payment service"),
                "initializeExternalServices() should print payment service message");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration-style Tests (connect → executeQuery → disconnect)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full lifecycle: connect, executeQuery, disconnect")
    void fullLifecycle_connectExecuteDisconnect_succeeds() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            when(mockConnection.isClosed()).thenReturn(false);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();

            verify(mockConnection).prepareStatement("SELECT 1");
            verify(mockConnection).close();
        }
    }

    @Test
    @DisplayName("executeQuery() after failed connect() does nothing (null connection)")
    void executeQuery_afterFailedConnect_doesNothing() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenAnswer(inv -> { throw new SQLException("Connection refused"); });

            databaseService.connect(); // fails silently

            assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
                    "executeQuery() should handle null connection gracefully after failed connect");
        }
    }

    @Test
    @DisplayName("disconnect() after failed connect() does nothing (null connection)")
    void disconnect_afterFailedConnect_doesNothing() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenAnswer(inv -> { throw new SQLException("Connection refused"); });

            databaseService.connect(); // fails silently

            assertDoesNotThrow(() -> databaseService.disconnect(),
                    "disconnect() should handle null connection gracefully after failed connect");
        }
    }

    @Test
    @DisplayName("Multiple executeQuery() calls on same connection all succeed")
    void executeQuery_multipleCallsSameConnection_allSucceed() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        databaseService.executeQuery("SELECT 1");
        databaseService.executeQuery("SELECT 2");
        databaseService.executeQuery("SELECT 3");

        verify(mockConnection, times(3)).prepareStatement(anyString());
        verify(mockStmt, times(3)).execute();
    }

    @Test
    @DisplayName("connect() can be called multiple times without error")
    void connect_calledMultipleTimes_doesNotThrow() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConnection = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);
            assertDoesNotThrow(() -> {
                databaseService.connect();
                databaseService.connect();
            }, "connect() should not throw when called multiple times");
        }
    }
}
