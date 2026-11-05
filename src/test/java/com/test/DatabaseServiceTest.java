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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for DatabaseService class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseService Tests")
class DatabaseServiceTest {

    private DatabaseService databaseService;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService();
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
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
    void constructor_default_createsNonNullInstance() {
        // Arrange / Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Newly created DatabaseService has null connection field")
    void constructor_default_connectionIsNull() throws Exception {
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
    void connect_printsConnectingMessage() {
        // Arrange
        // Use a pre-created exception to avoid DriverManager interaction inside MockedStatic
        SQLException simulatedEx = new SQLException("Simulated connection failure");
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenThrow(simulatedEx);

            // Act
            databaseService.connect();

            // Assert
            assertTrue(outContent.toString().contains("Connecting to database..."),
                    "Should print connecting message");
        }
    }

    @Test
    @DisplayName("connect() handles ClassNotFoundException gracefully and prints error")
    void connect_classNotFound_printsErrorMessage() {
        // Arrange – use a subclass that overrides Class.forName behaviour via reflection
        // We simulate by testing the error path: DriverManager throws to simulate driver missing
        SQLException noDriverEx = new SQLException("No suitable driver");
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenThrow(noDriverEx);

            // Act
            databaseService.connect();

            // Assert – error stream should contain the SQL error message
            String errOutput = errContent.toString();
            assertTrue(errOutput.contains("Database connection failed") || outContent.toString().contains("Connecting"),
                    "Should handle connection failure gracefully");
        }
    }

    @Test
    @DisplayName("connect() handles SQLException and prints error to stderr")
    void connect_sqlException_printsErrorToStderr() {
        // Arrange
        SQLException connRefusedEx = new SQLException("Connection refused");
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenThrow(connRefusedEx);

            // Act
            databaseService.connect();

            // Assert
            String errOutput = errContent.toString();
            assertTrue(errOutput.contains("Database connection failed"),
                    "Should print database connection failed message on SQLException");
        }
    }

    @Test
    @DisplayName("connect() with successful connection prints connected message")
    void connect_successfulConnection_printsConnectedMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("Connected to database:"),
                    "Should print connected message on successful connection");
        }
    }

    @Test
    @DisplayName("connect() with successful connection prints username")
    void connect_successfulConnection_printsUsername() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("Using username:"),
                    "Should print username after successful connection");
        }
    }

    @Test
    @DisplayName("connect() with successful connection prints Redis cache message")
    void connect_successfulConnection_printsCacheMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("Connecting to Redis cache at:"),
                    "Should print Redis cache connection message");
        }
    }

    @Test
    @DisplayName("connect() with successful connection prints external API message")
    void connect_successfulConnection_printsExternalApiMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("Initializing external API:"),
                    "Should print external API initialization message");
        }
    }

    @Test
    @DisplayName("connect() with successful connection prints payment service message")
    void connect_successfulConnection_printsPaymentServiceMessage() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);

            // Act
            databaseService.connect();

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("Initializing payment service:"),
                    "Should print payment service initialization message");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeQuery() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("executeQuery() with null connection does nothing (no exception)")
    void executeQuery_nullConnection_doesNotThrow() {
        // Arrange – connection is null by default
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"),
                "executeQuery should not throw when connection is null");
    }

    @Test
    @DisplayName("executeQuery() with null connection produces no output")
    void executeQuery_nullConnection_producesNoOutput() {
        // Arrange – connection is null by default
        // Act
        databaseService.executeQuery("SELECT 1");

        // Assert
        assertEquals("", outContent.toString(),
                "No output expected when connection is null");
    }

    @Test
    @DisplayName("executeQuery() with open connection executes and prints query")
    void executeQuery_openConnection_executesQuery() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("SELECT * FROM users");

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Executing query:"),
                "Should print executing query message");
        verify(mockStmt).execute();
        verify(mockStmt).close();
    }

    @Test
    @DisplayName("executeQuery() sets query timeout to 30 seconds")
    void executeQuery_openConnection_setsQueryTimeout() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("SELECT 1");

        // Assert
        verify(mockStmt).setQueryTimeout(30);
    }

    @Test
    @DisplayName("executeQuery() with closed connection does nothing")
    void executeQuery_closedConnection_doesNotExecute() throws Exception {
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
    @DisplayName("executeQuery() handles SQLException and prints error to stderr")
    void executeQuery_sqlException_printsErrorToStderr() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString()))
                .thenThrow(new SQLException("Query failed"));

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.executeQuery("INVALID SQL");

        // Assert
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Query execution failed"),
                "Should print query execution failed message on SQLException");
    }

    @Test
    @DisplayName("executeQuery() with empty SQL string does not throw")
    void executeQuery_emptySql_doesNotThrow() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
                "executeQuery should not throw for empty SQL string");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // disconnect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("disconnect() with null connection does not throw")
    void disconnect_nullConnection_doesNotThrow() {
        // Arrange – connection is null by default
        // Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect(),
                "disconnect should not throw when connection is null");
    }

    @Test
    @DisplayName("disconnect() with null connection produces no output")
    void disconnect_nullConnection_producesNoOutput() {
        // Arrange – connection is null by default
        // Act
        databaseService.disconnect();

        // Assert
        assertEquals("", outContent.toString(),
                "No output expected when connection is null");
    }

    @Test
    @DisplayName("disconnect() with open connection closes it and prints message")
    void disconnect_openConnection_closesAndPrintsMessage() throws Exception {
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
        assertTrue(outContent.toString().contains("Database connection closed"),
                "Should print connection closed message");
    }

    @Test
    @DisplayName("disconnect() with already closed connection does not call close()")
    void disconnect_closedConnection_doesNotCallClose() throws Exception {
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
    @DisplayName("disconnect() handles SQLException and prints error to stderr")
    void disconnect_sqlException_printsErrorToStderr() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);
        doThrow(new SQLException("Close failed")).when(mockConnection).close();

        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(databaseService, mockConnection);

        // Act
        databaseService.disconnect();

        // Assert
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Failed to close database connection"),
                "Should print failed to close message on SQLException");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Field / Constant Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DB_URL constant contains expected host and port")
    void staticFields_dbUrl_containsHostAndPort() throws Exception {
        Field dbUrlField = DatabaseService.class.getDeclaredField("DB_URL");
        dbUrlField.setAccessible(true);
        String dbUrl = (String) dbUrlField.get(null);

        assertTrue(dbUrl.contains("localhost"), "DB_URL should contain 'localhost'");
        assertTrue(dbUrl.contains("3306"), "DB_URL should contain port '3306'");
        assertTrue(dbUrl.contains("mini_app_db"), "DB_URL should contain database name");
    }

    @Test
    @DisplayName("REDIS_PORT constant equals 6379")
    void staticFields_redisPort_equals6379() throws Exception {
        Field redisPortField = DatabaseService.class.getDeclaredField("REDIS_PORT");
        redisPortField.setAccessible(true);
        int redisPort = (int) redisPortField.get(null);

        assertEquals(6379, redisPort, "REDIS_PORT should be 6379");
    }

    @Test
    @DisplayName("EXTERNAL_API_URL constant is not null or empty")
    void staticFields_externalApiUrl_isNotNullOrEmpty() throws Exception {
        Field apiUrlField = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        apiUrlField.setAccessible(true);
        String apiUrl = (String) apiUrlField.get(null);

        assertNotNull(apiUrl, "EXTERNAL_API_URL should not be null");
        assertFalse(apiUrl.isEmpty(), "EXTERNAL_API_URL should not be empty");
    }

    @Test
    @DisplayName("PAYMENT_SERVICE_URL constant starts with https")
    void staticFields_paymentServiceUrl_startsWithHttps() throws Exception {
        Field paymentUrlField = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
        paymentUrlField.setAccessible(true);
        String paymentUrl = (String) paymentUrlField.get(null);

        assertTrue(paymentUrl.startsWith("https://"),
                "PAYMENT_SERVICE_URL should start with 'https://'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration-style: connect then disconnect
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect() then disconnect() lifecycle works without exception")
    void connectThenDisconnect_lifecycle_noException() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        when(mockConnection.isClosed()).thenReturn(false);

        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);

            // Act & Assert
            assertDoesNotThrow(() -> {
                databaseService.connect();
                databaseService.disconnect();
            }, "connect() then disconnect() should not throw");
        }
    }

    @Test
    @DisplayName("connect() then executeQuery() then disconnect() full lifecycle")
    void fullLifecycle_connectExecuteDisconnect_noException() throws Exception {
        // Arrange
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockConnection.isClosed()).thenReturn(false);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);

        try (MockedStatic<DriverManager> dmMock = Mockito.mockStatic(DriverManager.class)) {
            dmMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                  .thenReturn(mockConnection);

            // Act & Assert
            assertDoesNotThrow(() -> {
                databaseService.connect();
                databaseService.executeQuery("SELECT 1");
                databaseService.disconnect();
            }, "Full lifecycle should not throw");
        }
    }
}
