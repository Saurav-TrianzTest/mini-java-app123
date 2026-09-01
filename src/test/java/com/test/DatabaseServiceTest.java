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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit 5 tests for DatabaseService class.
 * Tests cover: constructor, connect(), connectToCache(), initializeExternalServices(),
 * executeQuery(), disconnect() — including happy paths, error paths, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseService Tests")
class DatabaseServiceTest {

    private DatabaseService databaseService;

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
    // Constructor tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Constructor: DatabaseService instance is created successfully")
    void constructor_createsInstance_notNull() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Constructor: Multiple instances are independent")
    void constructor_multipleInstances_areIndependent() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotSame(service1, service2, "Each instance should be a distinct object");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // connect() — success path tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect(): prints connecting message before attempting connection")
    void connect_printsConnectingMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);

            databaseService.connect();

            assertTrue(outContent.toString().contains("Connecting to PostgreSQL database"),
                    "Should print connecting message");
        }
    }

    @Test
    @DisplayName("connect(): prints connected message on successful connection")
    void connect_successfulConnection_printsConnectedMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);

            databaseService.connect();

            assertTrue(outContent.toString().contains("Connected to PostgreSQL database"),
                    "Should print connected message after successful connection");
        }
    }

    @Test
    @DisplayName("connect(): prints Redis cache connection message")
    void connect_successfulConnection_printsRedisCacheMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);

            databaseService.connect();

            assertTrue(outContent.toString().contains("Connecting to Redis cache"),
                    "Should print Redis cache connection message");
        }
    }

    @Test
    @DisplayName("connect(): prints external API initialization message")
    void connect_successfulConnection_printsExternalApiMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);

            databaseService.connect();

            assertTrue(outContent.toString().contains("Initializing external API"),
                    "Should print external API initialization message");
        }
    }

    @Test
    @DisplayName("connect(): prints payment service initialization message")
    void connect_successfulConnection_printsPaymentServiceMessage() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);

            databaseService.connect();

            assertTrue(outContent.toString().contains("Initializing payment service"),
                    "Should print payment service initialization message");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // connect() — error path tests
    // NOTE: SQLException is pre-created BEFORE entering MockedStatic scope to
    //       avoid the UnfinishedStubbingException caused by SQLException's
    //       constructor calling DriverManager.getLogWriter() internally.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect(): handles SQLException gracefully and prints error message")
    void connect_sqlException_printsErrorMessage() {
        // Pre-create exception BEFORE opening MockedStatic scope
        SQLException preCreatedException = new SQLException("Connection refused");

        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenThrow(preCreatedException);

            databaseService.connect();   // must NOT throw

            assertTrue(errContent.toString().contains("PostgreSQL database connection failed"),
                    "Should print error message on SQLException");
        }
    }

    @Test
    @DisplayName("connect(): error message contains original exception message")
    void connect_sqlException_errorMessageContainsExceptionDetail() {
        // Pre-create exception BEFORE opening MockedStatic scope
        SQLException preCreatedException = new SQLException("Connection refused to host");

        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenThrow(preCreatedException);

            databaseService.connect();

            assertTrue(errContent.toString().contains("Connection refused to host"),
                    "Error message should contain the original exception detail");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeQuery() tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("executeQuery(): does nothing when connection is null (no exception thrown)")
    void executeQuery_nullConnection_doesNotThrow() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"),
                "executeQuery should not throw when connection is null");
    }

    @Test
    @DisplayName("executeQuery(): does nothing when connection is null (no output)")
    void executeQuery_nullConnection_producesNoOutput() {
        DatabaseService service = new DatabaseService();
        service.executeQuery("SELECT 1");
        assertFalse(outContent.toString().contains("Executing query"),
                "Should not print executing query when connection is null");
    }

    @Test
    @DisplayName("executeQuery(): executes query when connection is open")
    void executeQuery_openConnection_executesQuery() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn        = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(false);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

            databaseService.connect();
            outContent.reset();

            databaseService.executeQuery("SELECT * FROM users");

            String output = outContent.toString();
            assertTrue(output.contains("Executing query"), "Should print executing query message");
            assertTrue(output.contains("SELECT * FROM users"), "Should include the SQL in the output");
        }
    }

    @Test
    @DisplayName("executeQuery(): sets query timeout to 30 seconds")
    void executeQuery_openConnection_setsQueryTimeout() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn        = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(false);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

            databaseService.connect();
            databaseService.executeQuery("SELECT 1");

            verify(mockStmt).setQueryTimeout(30);
        }
    }

    @Test
    @DisplayName("executeQuery(): closes PreparedStatement after execution")
    void executeQuery_openConnection_closesStatement() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn        = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(false);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

            databaseService.connect();
            databaseService.executeQuery("DELETE FROM logs");

            verify(mockStmt).close();
        }
    }

    @Test
    @DisplayName("executeQuery(): handles SQLException during execution gracefully")
    void executeQuery_sqlExceptionDuringExecution_printsError() throws Exception {
        // Pre-create exception BEFORE opening MockedStatic scope
        SQLException preCreatedException = new SQLException("Table not found");

        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(false);
            when(mockConn.prepareStatement(anyString())).thenThrow(preCreatedException);

            databaseService.connect();
            errContent.reset();

            databaseService.executeQuery("SELECT * FROM nonexistent");

            assertTrue(errContent.toString().contains("Query execution failed"),
                    "Should print query execution failed message");
        }
    }

    @Test
    @DisplayName("executeQuery(): skips execution when connection is closed")
    void executeQuery_closedConnection_doesNotExecute() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(true);

            databaseService.connect();
            outContent.reset();

            databaseService.executeQuery("SELECT 1");

            assertFalse(outContent.toString().contains("Executing query"),
                    "Should not execute query when connection is closed");
        }
    }

    @Test
    @DisplayName("executeQuery(): handles empty SQL string without throwing")
    void executeQuery_emptySql_doesNotThrow() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn        = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(false);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

            databaseService.connect();

            assertDoesNotThrow(() -> databaseService.executeQuery(""),
                    "executeQuery should not throw for empty SQL");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // disconnect() tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("disconnect(): does nothing when connection is null (no exception)")
    void disconnect_nullConnection_doesNotThrow() {
        DatabaseService service = new DatabaseService();
        assertDoesNotThrow(service::disconnect,
                "disconnect should not throw when connection is null");
    }

    @Test
    @DisplayName("disconnect(): closes open connection and prints confirmation")
    void disconnect_openConnection_closesAndPrintsMessage() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(false);

            databaseService.connect();
            outContent.reset();

            databaseService.disconnect();

            verify(mockConn).close();
            assertTrue(outContent.toString().contains("PostgreSQL database connection closed"),
                    "Should print connection closed message");
        }
    }

    @Test
    @DisplayName("disconnect(): does not close already-closed connection")
    void disconnect_alreadyClosedConnection_doesNotCallClose() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(true);

            databaseService.connect();
            databaseService.disconnect();

            verify(mockConn, never()).close();
        }
    }

    @Test
    @DisplayName("disconnect(): handles SQLException during close gracefully")
    void disconnect_sqlExceptionOnClose_printsError() throws Exception {
        // Pre-create exception BEFORE opening MockedStatic scope
        SQLException preCreatedException = new SQLException("Close failed");

        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(false);
            doThrow(preCreatedException).when(mockConn).close();

            databaseService.connect();
            errContent.reset();

            databaseService.disconnect();   // must NOT propagate exception

            assertTrue(errContent.toString().contains("Failed to close database connection"),
                    "Should print error message when close fails");
        }
    }

    @Test
    @DisplayName("disconnect(): can be called multiple times without error")
    void disconnect_calledMultipleTimes_doesNotThrow() throws Exception {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);

            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);
            when(mockConn.isClosed()).thenReturn(false).thenReturn(true);

            databaseService.connect();

            assertDoesNotThrow(() -> {
                databaseService.disconnect();
                databaseService.disconnect();
            }, "Multiple disconnect calls should not throw");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Environment variable / static field tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect(): DB_URL contains 'jdbc:postgresql://' prefix")
    void connect_dbUrl_containsPostgresqlPrefix() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);

            databaseService.connect();

            dmMock.verify(() -> DriverManager.getConnection(
                    argThat(url -> url.startsWith("jdbc:postgresql://")),
                    anyString(), anyString()));
        }
    }

    @Test
    @DisplayName("connect(): uses default port 5432 when DB_PORT env var is absent")
    void connect_defaultDbPort_is5432() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);

            databaseService.connect();

            dmMock.verify(() -> DriverManager.getConnection(
                    argThat(url -> url.contains("5432")),
                    anyString(), anyString()));
        }
    }

    @Test
    @DisplayName("connect(): Redis output contains host and port")
    void connect_redisOutput_containsHostAndPort() {
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConn);

            databaseService.connect();

            String output = outContent.toString();
            assertTrue(output.contains("127.0.0.1") || output.contains("localhost"),
                    "Redis output should contain host");
            assertTrue(output.contains("6379"),
                    "Redis output should contain default port 6379");
        }
    }
}
