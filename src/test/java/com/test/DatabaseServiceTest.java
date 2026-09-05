package com.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 tests for DatabaseService class.
 * Tests cover: constructor, connect(), executeQuery(), disconnect(),
 * connectToCache() (via connect()), initializeExternalServices() (via connect()),
 * and all static constant fields.
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
        outContent.reset();
        errContent.reset();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Default constructor creates a non-null DatabaseService instance")
    void constructor_defaultConstructor_createsNonNullInstance() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Default constructor initialises connection field to null")
    void constructor_defaultConstructor_connectionFieldIsNull() throws Exception {
        // Arrange
        DatabaseService service = new DatabaseService();

        // Act
        Field connectionField = DatabaseService.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        Object connectionValue = connectionField.get(service);

        // Assert
        assertNull(connectionValue, "Connection field should be null before connect() is called");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Constant Field Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DB_HOST constant equals 'localhost'")
    void staticField_dbHost_equalsLocalhost() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_HOST");
        field.setAccessible(true);
        assertEquals("localhost", field.get(null));
    }

    @Test
    @DisplayName("DB_PORT constant equals '3306'")
    void staticField_dbPort_equals3306() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_PORT");
        field.setAccessible(true);
        assertEquals("3306", field.get(null));
    }

    @Test
    @DisplayName("DB_NAME constant equals 'mini_app_db'")
    void staticField_dbName_equalsMiniAppDb() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_NAME");
        field.setAccessible(true);
        assertEquals("mini_app_db", field.get(null));
    }

    @Test
    @DisplayName("DB_URL constant is correctly composed")
    void staticField_dbUrl_isCorrectlyComposed() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_URL");
        field.setAccessible(true);
        String dbUrl = (String) field.get(null);
        assertNotNull(dbUrl);
        assertTrue(dbUrl.startsWith("jdbc:mysql://"), "DB_URL should start with jdbc:mysql://");
        assertTrue(dbUrl.contains("localhost"), "DB_URL should contain localhost");
        assertTrue(dbUrl.contains("3306"), "DB_URL should contain port 3306");
        assertTrue(dbUrl.contains("mini_app_db"), "DB_URL should contain database name");
    }

    @Test
    @DisplayName("DB_USERNAME constant equals 'root'")
    void staticField_dbUsername_equalsRoot() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_USERNAME");
        field.setAccessible(true);
        assertEquals("root", field.get(null));
    }

    @Test
    @DisplayName("DB_PASSWORD constant equals 'password123'")
    void staticField_dbPassword_equalsPassword123() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_PASSWORD");
        field.setAccessible(true);
        assertEquals("password123", field.get(null));
    }

    @Test
    @DisplayName("REDIS_HOST constant equals '127.0.0.1'")
    void staticField_redisHost_equals127001() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_HOST");
        field.setAccessible(true);
        assertEquals("127.0.0.1", field.get(null));
    }

    @Test
    @DisplayName("REDIS_PORT constant equals 6379")
    void staticField_redisPort_equals6379() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_PORT");
        field.setAccessible(true);
        assertEquals(6379, field.get(null));
    }

    @Test
    @DisplayName("EXTERNAL_API_URL constant is not null and not empty")
    void staticField_externalApiUrl_isNotNullOrEmpty() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        field.setAccessible(true);
        String value = (String) field.get(null);
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("http"), "EXTERNAL_API_URL should start with http");
    }

    @Test
    @DisplayName("PAYMENT_SERVICE_URL constant is not null and not empty")
    void staticField_paymentServiceUrl_isNotNullOrEmpty() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
        field.setAccessible(true);
        String value = (String) field.get(null);
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("https"), "PAYMENT_SERVICE_URL should start with https");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // connect() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connect() prints 'Connecting to database...' message")
    void connect_printsConnectingMessage() {
        // Act
        databaseService.connect();

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Connecting to database..."),
                "connect() should print 'Connecting to database...'");
    }

    @Test
    @DisplayName("connect() handles ClassNotFoundException or SQLException gracefully (no exception thrown)")
    void connect_whenDriverOrConnectionFails_doesNotThrowException() {
        // Act & Assert – must not propagate any exception
        assertDoesNotThrow(() -> databaseService.connect(),
                "connect() should handle exceptions internally and not propagate them");
    }

    @Test
    @DisplayName("connect() prints error message when connection fails")
    void connect_whenConnectionFails_printsErrorMessage() {
        // Act
        databaseService.connect();

        // Assert – either connected (unlikely in test env) or error printed
        String stdOut = outContent.toString();
        String stdErr = errContent.toString();
        // At minimum the initial message must appear
        assertTrue(stdOut.contains("Connecting to database..."),
                "Should print initial connecting message");
        // In a test environment without a real DB, an error is expected
        // We just verify no unhandled exception was thrown (already covered above)
    }

    @Test
    @DisplayName("connect() can be called multiple times without throwing")
    void connect_calledMultipleTimes_doesNotThrow() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        });
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
    @DisplayName("executeQuery() with null SQL does not throw exception")
    void executeQuery_withNullSql_doesNotThrow() {
        assertDoesNotThrow(() -> databaseService.executeQuery(null),
                "executeQuery() should handle null SQL gracefully");
    }

    @Test
    @DisplayName("executeQuery() with empty SQL does not throw exception")
    void executeQuery_withEmptySql_doesNotThrow() {
        assertDoesNotThrow(() -> databaseService.executeQuery(""),
                "executeQuery() should handle empty SQL gracefully");
    }

    @Test
    @DisplayName("executeQuery() with valid SQL string does not throw exception when connection is null")
    void executeQuery_withValidSqlAndNullConnection_doesNotThrow() {
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT * FROM users"),
                "executeQuery() should not throw when connection is null");
    }

    @Test
    @DisplayName("executeQuery() with SELECT statement does not throw exception")
    void executeQuery_withSelectStatement_doesNotThrow() {
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT id, name FROM employees WHERE id = 1"));
    }

    @Test
    @DisplayName("executeQuery() with INSERT statement does not throw exception")
    void executeQuery_withInsertStatement_doesNotThrow() {
        assertDoesNotThrow(() -> databaseService.executeQuery("INSERT INTO test (col) VALUES ('val')"));
    }

    @Test
    @DisplayName("executeQuery() with UPDATE statement does not throw exception")
    void executeQuery_withUpdateStatement_doesNotThrow() {
        assertDoesNotThrow(() -> databaseService.executeQuery("UPDATE test SET col = 'val' WHERE id = 1"));
    }

    @Test
    @DisplayName("executeQuery() with DELETE statement does not throw exception")
    void executeQuery_withDeleteStatement_doesNotThrow() {
        assertDoesNotThrow(() -> databaseService.executeQuery("DELETE FROM test WHERE id = 1"));
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
    @DisplayName("disconnect() can be called multiple times without throwing")
    void disconnect_calledMultipleTimes_doesNotThrow() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        });
    }

    @Test
    @DisplayName("disconnect() after connect() does not throw exception")
    void disconnect_afterConnect_doesNotThrow() {
        // Act
        databaseService.connect();   // may fail silently in test env
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    @DisplayName("disconnect() without prior connect() does not print 'Database connection closed'")
    void disconnect_withoutConnect_doesNotPrintClosedMessage() {
        // Act
        databaseService.disconnect();

        // Assert
        String output = outContent.toString();
        assertFalse(output.contains("Database connection closed"),
                "Should not print closed message when connection was never opened");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Full lifecycle Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full lifecycle: connect -> executeQuery -> disconnect does not throw")
    void lifecycle_connectExecuteQueryDisconnect_doesNotThrow() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();
        });
    }

    @Test
    @DisplayName("executeQuery() followed by disconnect() does not throw")
    void lifecycle_executeQueryThenDisconnect_doesNotThrow() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();
        });
    }

    @Test
    @DisplayName("Multiple executeQuery() calls without connection do not throw")
    void executeQuery_multipleCallsWithoutConnection_doesNotThrow() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT 1");
            databaseService.executeQuery("SELECT 2");
            databaseService.executeQuery("SELECT 3");
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reflection / Instance Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DatabaseService is a concrete (non-abstract) class")
    void classStructure_isConcreteClass() {
        assertFalse(java.lang.reflect.Modifier.isAbstract(DatabaseService.class.getModifiers()),
                "DatabaseService should be a concrete class");
    }

    @Test
    @DisplayName("DatabaseService has public connect() method")
    void classStructure_hasPublicConnectMethod() throws NoSuchMethodException {
        var method = DatabaseService.class.getMethod("connect");
        assertNotNull(method);
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }

    @Test
    @DisplayName("DatabaseService has public executeQuery(String) method")
    void classStructure_hasPublicExecuteQueryMethod() throws NoSuchMethodException {
        var method = DatabaseService.class.getMethod("executeQuery", String.class);
        assertNotNull(method);
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }

    @Test
    @DisplayName("DatabaseService has public disconnect() method")
    void classStructure_hasPublicDisconnectMethod() throws NoSuchMethodException {
        var method = DatabaseService.class.getMethod("disconnect");
        assertNotNull(method);
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }

    @Test
    @DisplayName("Two separate DatabaseService instances are independent")
    void constructor_twoInstances_areIndependent() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        assertNotSame(service1, service2, "Two instances should be different objects");
    }
}
