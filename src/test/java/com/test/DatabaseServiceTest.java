package com.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit 5 tests for DatabaseService.
 *
 * Strategy: Static-final fields in DatabaseService are initialised from
 * environment variables at class-load time. Java 17 forbids reflective
 * mutation of static-final fields, so tests that need a specific connection
 * state inject a mock directly into the non-final instance field 'connection'
 * via reflection.
 *
 * Coverage targets:
 *  - DatabaseService constructor
 *  - connect() – unreachable-DB path (SQLException caught internally)
 *  - connectToCache() – private method via reflection
 *  - initializeExternalServices() – private method via reflection
 *  - executeQuery() – open connection, closed connection, null connection, SQLException
 *  - disconnect() – open connection, closed connection, null connection, SQLException
 *  - Static field value assertions (non-null, non-blank, valid format)
 */
@DisplayName("DatabaseService Tests")
class DatabaseServiceTest {

    private final ByteArrayOutputStream outContent  = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent  = new ByteArrayOutputStream();
    private final PrintStream           originalOut = System.out;
    private final PrintStream           originalErr = System.err;

    @BeforeEach
    void redirectStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DatabaseService can be instantiated with default constructor")
    void constructor_defaultInstantiation_succeeds() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Multiple DatabaseService instances can be created independently")
    void constructor_multipleInstances_areIndependent() {
        DatabaseService s1 = new DatabaseService();
        DatabaseService s2 = new DatabaseService();
        assertNotNull(s1);
        assertNotNull(s2);
        assertNotSame(s1, s2);
    }

    // -----------------------------------------------------------------------
    // Static field defaults (via reflection – read-only)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DB_HOST static field is non-null and non-empty")
    void dbHost_staticField_isNonEmpty() throws Exception {
        String host = getStaticStringField("DB_HOST");
        assertNotNull(host, "DB_HOST must not be null");
        assertFalse(host.isBlank(), "DB_HOST must not be blank");
    }

    @Test
    @DisplayName("DB_PORT static field is a valid integer string")
    void dbPort_staticField_isValidInteger() throws Exception {
        String port = getStaticStringField("DB_PORT");
        assertNotNull(port, "DB_PORT must not be null");
        assertDoesNotThrow(() -> Integer.parseInt(port),
                "DB_PORT must be parseable as an integer, got: " + port);
        int portInt = Integer.parseInt(port);
        assertTrue(portInt > 0 && portInt <= 65535,
                "DB_PORT must be a valid port number, got: " + portInt);
    }

    @Test
    @DisplayName("DB_NAME static field is non-null and non-empty")
    void dbName_staticField_isNonEmpty() throws Exception {
        String name = getStaticStringField("DB_NAME");
        assertNotNull(name, "DB_NAME must not be null");
        assertFalse(name.isBlank(), "DB_NAME must not be blank");
    }

    @Test
    @DisplayName("DB_URL contains 'postgresql' JDBC scheme")
    void dbUrl_staticField_containsPostgresqlScheme() throws Exception {
        String url = getStaticStringField("DB_URL");
        assertNotNull(url, "DB_URL must not be null");
        assertTrue(url.contains("postgresql"),
                "DB_URL should reference the postgresql JDBC scheme, got: " + url);
    }

    @Test
    @DisplayName("DB_USERNAME static field is non-null and non-empty")
    void dbUsername_staticField_isNonEmpty() throws Exception {
        String username = getStaticStringField("DB_USERNAME");
        assertNotNull(username, "DB_USERNAME must not be null");
        assertFalse(username.isBlank(), "DB_USERNAME must not be blank");
    }

    @Test
    @DisplayName("DB_PASSWORD static field is non-null (may be empty)")
    void dbPassword_staticField_isNonNull() throws Exception {
        String password = getStaticStringField("DB_PASSWORD");
        assertNotNull(password, "DB_PASSWORD must not be null");
    }

    @Test
    @DisplayName("REDIS_HOST static field is non-null and non-empty")
    void redisHost_staticField_isNonEmpty() throws Exception {
        String host = getStaticStringField("REDIS_HOST");
        assertNotNull(host, "REDIS_HOST must not be null");
        assertFalse(host.isBlank(), "REDIS_HOST must not be blank");
    }

    @Test
    @DisplayName("REDIS_PORT static field is a valid port number")
    void redisPort_staticField_isValidPort() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_PORT");
        field.setAccessible(true);
        int port = (int) field.get(null);
        assertTrue(port > 0 && port <= 65535,
                "REDIS_PORT must be a valid port number, got: " + port);
    }

    @Test
    @DisplayName("EXTERNAL_API_URL static field is non-null and non-empty")
    void externalApiUrl_staticField_isNonEmpty() throws Exception {
        String url = getStaticStringField("EXTERNAL_API_URL");
        assertNotNull(url, "EXTERNAL_API_URL must not be null");
        assertFalse(url.isBlank(), "EXTERNAL_API_URL must not be blank");
    }

    @Test
    @DisplayName("PAYMENT_SERVICE_URL static field is non-null and non-empty")
    void paymentServiceUrl_staticField_isNonEmpty() throws Exception {
        String url = getStaticStringField("PAYMENT_SERVICE_URL");
        assertNotNull(url, "PAYMENT_SERVICE_URL must not be null");
        assertFalse(url.isBlank(), "PAYMENT_SERVICE_URL must not be blank");
    }

    // -----------------------------------------------------------------------
    // connect() – real DB not available; verify error handling
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("connect() does not throw when database is unreachable")
    void connect_databaseUnreachable_doesNotThrow() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(service::connect,
                "connect() must not propagate SQLException");
    }

    @Test
    @DisplayName("connect() prints 'Connecting to PostgreSQL database' before attempting connection")
    void connect_alwaysPrintsConnectingMessage() {
        DatabaseService service = new DatabaseService();
        service.connect();

        String output = outContent.toString();
        assertTrue(output.contains("Connecting to PostgreSQL database"),
                "Expected connecting message. Got: " + output);
    }

    @Test
    @DisplayName("connect() prints error message when database is unreachable")
    void connect_databaseUnreachable_printsErrorMessage() {
        DatabaseService service = new DatabaseService();
        service.connect();

        // Either connected (unlikely in CI) or printed an error
        String combined = outContent.toString() + errContent.toString();
        assertTrue(
            combined.contains("Connected to PostgreSQL database")
                || combined.contains("Database connection failed"),
            "Expected connection status message. Got: " + combined
        );
    }

    @Test
    @DisplayName("connect() can be called multiple times without throwing")
    void connect_calledMultipleTimes_noException() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> {
            service.connect();
            service.connect();
        });
    }

    // -----------------------------------------------------------------------
    // connectToCache() – private method via reflection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("connectToCache prints Redis connection message")
    void connectToCache_invoked_printsRedisMessage() throws Exception {
        DatabaseService service = new DatabaseService();
        invokePrivate(service, "connectToCache");

        String output = outContent.toString();
        assertTrue(output.contains("Connecting to Redis cache"),
                "Expected Redis connection message. Got: " + output);
    }

    @Test
    @DisplayName("connectToCache does not throw any exception")
    void connectToCache_neverThrows() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> invokePrivate(service, "connectToCache"));
    }

    // -----------------------------------------------------------------------
    // initializeExternalServices() – private method via reflection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("initializeExternalServices prints external API message")
    void initializeExternalServices_invoked_printsApiMessage() throws Exception {
        DatabaseService service = new DatabaseService();
        invokePrivate(service, "initializeExternalServices");

        String output = outContent.toString();
        assertTrue(output.contains("Initializing external API"),
                "Expected external API message. Got: " + output);
    }

    @Test
    @DisplayName("initializeExternalServices prints payment service message")
    void initializeExternalServices_invoked_printsPaymentMessage() throws Exception {
        DatabaseService service = new DatabaseService();
        invokePrivate(service, "initializeExternalServices");

        String output = outContent.toString();
        assertTrue(output.contains("Initializing payment service"),
                "Expected payment service message. Got: " + output);
    }

    @Test
    @DisplayName("initializeExternalServices does not throw any exception")
    void initializeExternalServices_neverThrows() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> invokePrivate(service, "initializeExternalServices"));
    }

    // -----------------------------------------------------------------------
    // executeQuery() – with mocked Connection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("executeQuery executes SQL when connection is open")
    void executeQuery_openConnection_executesStatement() throws Exception {
        // Arrange
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConn.isClosed()).thenReturn(false);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        // Act
        service.executeQuery("SELECT 1");

        // Assert
        verify(mockConn).prepareStatement("SELECT 1");
        verify(mockStmt).setQueryTimeout(30);
        verify(mockStmt).execute();
        verify(mockStmt).close();
    }

    @Test
    @DisplayName("executeQuery prints 'Executing query' message for valid SQL")
    void executeQuery_openConnection_printsExecutingMessage() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConn.isClosed()).thenReturn(false);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        service.executeQuery("SELECT 1");

        assertTrue(outContent.toString().contains("Executing query: SELECT 1"),
                "Expected 'Executing query' message");
    }

    @Test
    @DisplayName("executeQuery does nothing when connection is null")
    void executeQuery_nullConnection_doesNothing() throws Exception {
        DatabaseService service = new DatabaseService();
        injectConnection(service, null);

        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"));
        assertEquals("", outContent.toString().trim(),
                "No output expected when connection is null");
    }

    @Test
    @DisplayName("executeQuery does nothing when connection is closed")
    void executeQuery_closedConnection_doesNothing() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.isClosed()).thenReturn(true);

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"));
        verify(mockConn, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("executeQuery handles SQLException from prepareStatement gracefully")
    void executeQuery_prepareStatementThrows_printsError() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.isClosed()).thenReturn(false);
        when(mockConn.prepareStatement(anyString()))
                .thenThrow(new SQLException("Simulated SQL error"));

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        assertDoesNotThrow(() -> service.executeQuery("BAD SQL"));

        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Query execution failed"),
                "Expected 'Query execution failed' in stderr. Got: " + errOutput);
    }

    @Test
    @DisplayName("executeQuery handles isClosed() throwing SQLException")
    void executeQuery_isClosedThrows_handlesGracefully() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.isClosed()).thenThrow(new SQLException("isClosed failed"));

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"));
    }

    @Test
    @DisplayName("executeQuery with empty SQL string does not throw")
    void executeQuery_emptySql_doesNotThrow() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConn.isClosed()).thenReturn(false);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        assertDoesNotThrow(() -> service.executeQuery(""));
    }

    // -----------------------------------------------------------------------
    // disconnect() – with mocked Connection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("disconnect closes open connection and prints confirmation")
    void disconnect_openConnection_closesAndPrints() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.isClosed()).thenReturn(false);

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        service.disconnect();

        verify(mockConn).close();
        assertTrue(outContent.toString().contains("Database connection closed"),
                "Expected 'Database connection closed' message");
    }

    @Test
    @DisplayName("disconnect does nothing when connection is null")
    void disconnect_nullConnection_doesNothing() throws Exception {
        DatabaseService service = new DatabaseService();
        injectConnection(service, null);

        assertDoesNotThrow(service::disconnect);
        assertEquals("", outContent.toString().trim(),
                "No output expected when connection is null");
    }

    @Test
    @DisplayName("disconnect does nothing when connection is already closed")
    void disconnect_alreadyClosedConnection_doesNothing() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.isClosed()).thenReturn(true);

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        assertDoesNotThrow(service::disconnect);
        verify(mockConn, never()).close();
    }

    @Test
    @DisplayName("disconnect handles SQLException from close() gracefully")
    void disconnect_closeThrowsSQLException_printsError() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.isClosed()).thenReturn(false);
        doThrow(new SQLException("close failed")).when(mockConn).close();

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        assertDoesNotThrow(service::disconnect);

        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Failed to close database connection"),
                "Expected 'Failed to close database connection' in stderr. Got: " + errOutput);
    }

    @Test
    @DisplayName("disconnect handles isClosed() throwing SQLException")
    void disconnect_isClosedThrows_handlesGracefully() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.isClosed()).thenThrow(new SQLException("isClosed failed"));

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        assertDoesNotThrow(service::disconnect);
    }

    @Test
    @DisplayName("disconnect can be called multiple times without error")
    void disconnect_calledMultipleTimes_noError() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(mockConn.isClosed()).thenReturn(false);

        DatabaseService service = new DatabaseService();
        injectConnection(service, mockConn);

        assertDoesNotThrow(() -> {
            service.disconnect();
            // After first disconnect, connection is closed; second call should be safe
            when(mockConn.isClosed()).thenReturn(true);
            service.disconnect();
        });
    }

    // -----------------------------------------------------------------------
    // connect() + disconnect() integration
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("disconnect after failed connect does not throw")
    void disconnect_afterFailedConnect_doesNotThrow() {
        DatabaseService service = new DatabaseService();
        service.connect();   // will fail – no real DB
        assertDoesNotThrow(service::disconnect,
                "disconnect() after a failed connect() must not throw");
    }

    @Test
    @DisplayName("Full lifecycle: connect (fails), executeQuery (null conn), disconnect")
    void fullLifecycle_failedConnect_safeQueryAndDisconnect() {
        DatabaseService service = new DatabaseService();
        service.connect();                    // fails silently
        service.executeQuery("SELECT 1");     // connection is null – no-op
        assertDoesNotThrow(service::disconnect);
    }

    // -----------------------------------------------------------------------
    // Helper utilities
    // -----------------------------------------------------------------------

    /** Reads a static String field from DatabaseService via reflection (read-only). */
    private static String getStaticStringField(String fieldName) throws Exception {
        Field field = DatabaseService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    /** Injects a Connection into the private instance field 'connection'. */
    private static void injectConnection(DatabaseService service, Connection conn)
            throws Exception {
        Field field = DatabaseService.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(service, conn);
    }

    /** Invokes a private no-arg instance method by name. */
    private static void invokePrivate(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        try {
            method.invoke(target);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw ite;
        }
    }
}
