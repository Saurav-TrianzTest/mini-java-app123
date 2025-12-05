package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseService class
 * Tests all methods, constructors, and code paths including error scenarios
 */
public class DatabaseServiceTest {

    private DatabaseService dbService;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    public void setUp() {
        outContent.reset();
        errContent.reset();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        dbService = new DatabaseService();
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        if (dbService != null) {
            try {
                dbService.disconnect();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    @DisplayName("Test DatabaseService constructor - creates instance successfully")
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    @DisplayName("Test DatabaseService constructor - multiple instances")
    public void testConstructorMultipleInstances() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        assertNotNull(service1, "First DatabaseService instance should not be null");
        assertNotNull(service2, "Second DatabaseService instance should not be null");
        assertNotSame(service1, service2, "Two instances should be different objects");
    }

    @Test
    @DisplayName("Test connect method - attempts database connection")
    public void testConnect() {
        assertDoesNotThrow(() -> {
            dbService.connect();
        }, "Connect method should not throw exceptions");
    }

    @Test
    @DisplayName("Test connect method - prints connection information")
    public void testConnectPrintsInformation() {
        dbService.connect();

        String output = outContent.toString();
        String errors = errContent.toString();
        String combinedOutput = output + errors;

        assertTrue(combinedOutput.contains("Connecting") || combinedOutput.contains("database") ||
                  combinedOutput.contains("connection") || combinedOutput.contains("failed"),
                  "Should print connection information or error");
    }

    @Test
    @DisplayName("Test connect method - initializes cache connection")
    public void testConnectInitializesCache() {
        dbService.connect();

        String output = outContent.toString();
        String errors = errContent.toString();
        String combined = output + errors;
        assertTrue(combined.contains("Redis") || combined.contains("cache") ||
                  combined.contains("127.0.0.1") || combined.contains("Connecting"),
                  "Should attempt to connect to cache");
    }

    @Test
    @DisplayName("Test connect method - initializes external services")
    public void testConnectInitializesExternalServices() {
        dbService.connect();

        String output = outContent.toString();
        String errors = errContent.toString();
        String combined = output + errors;
        assertTrue(combined.contains("external") || combined.contains("API") ||
                  combined.contains("payment") || combined.contains("service") ||
                  combined.contains("Connecting"),
                  "Should initialize external services");
    }

    @Test
    @DisplayName("Test connectToCache method - handles Redis connection")
    public void testConnectToCache() throws Exception {
        java.lang.reflect.Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
        method.setAccessible(true);

        assertDoesNotThrow(() -> {
            method.invoke(dbService);
        }, "connectToCache should not throw exceptions");

        String output = outContent.toString();
        assertTrue(output.contains("Redis") && output.contains("127.0.0.1:6379"),
                  "Should print Redis connection information");
    }

    @Test
    @DisplayName("Test initializeExternalServices method - prints service URLs")
    public void testInitializeExternalServices() throws Exception {
        java.lang.reflect.Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        method.setAccessible(true);

        assertDoesNotThrow(() -> {
            method.invoke(dbService);
        }, "initializeExternalServices should not throw exceptions");

        String output = outContent.toString();
        assertTrue(output.contains("http://api.example.com:8080/v1") &&
                  output.contains("https://payment.internal.company.com/process"),
                  "Should print external service URLs");
    }

    @Test
    @DisplayName("Test executeQuery method - with null connection")
    public void testExecuteQueryNullConnection() {
        assertDoesNotThrow(() -> {
            dbService.executeQuery("SELECT * FROM users");
        }, "Should handle null connection gracefully");
    }

    @Test
    @DisplayName("Test executeQuery method - with valid SQL")
    public void testExecuteQueryValidSQL() {
        dbService.connect();

        assertDoesNotThrow(() -> {
            dbService.executeQuery("SELECT * FROM users WHERE id = 1");
        }, "ExecuteQuery should not throw exceptions");
    }

    @Test
    @DisplayName("Test executeQuery method - with empty SQL")
    public void testExecuteQueryEmptySQL() {
        dbService.connect();

        assertDoesNotThrow(() -> {
            dbService.executeQuery("");
        }, "Should handle empty SQL string");
    }

    @Test
    @DisplayName("Test executeQuery method - with null SQL")
    public void testExecuteQueryNullSQL() {
        dbService.connect();

        assertDoesNotThrow(() -> {
            dbService.executeQuery(null);
        }, "Should handle null SQL gracefully");
    }

    @Test
    @DisplayName("Test executeQuery method - with invalid SQL")
    public void testExecuteQueryInvalidSQL() {
        dbService.connect();

        assertDoesNotThrow(() -> {
            dbService.executeQuery("INVALID SQL SYNTAX HERE");
        }, "Should handle invalid SQL without throwing");
    }

    @Test
    @DisplayName("Test executeQuery method - with multiple queries")
    public void testExecuteQueryMultiple() {
        dbService.connect();

        assertDoesNotThrow(() -> {
            dbService.executeQuery("SELECT * FROM users");
            dbService.executeQuery("SELECT * FROM products");
            dbService.executeQuery("INSERT INTO logs VALUES (1, 'test')");
        }, "Should handle multiple query executions");
    }

    @Test
    @DisplayName("Test disconnect method - with null connection")
    public void testDisconnectNullConnection() {
        assertDoesNotThrow(() -> {
            dbService.disconnect();
        }, "Should handle disconnect with null connection");
    }

    @Test
    @DisplayName("Test disconnect method - after connect attempt")
    public void testDisconnectAfterConnect() {
        dbService.connect();

        assertDoesNotThrow(() -> {
            dbService.disconnect();
        }, "Should disconnect without throwing exceptions");
    }

    @Test
    @DisplayName("Test disconnect method - called multiple times")
    public void testDisconnectMultipleTimes() {
        dbService.connect();

        assertDoesNotThrow(() -> {
            dbService.disconnect();
            dbService.disconnect();
            dbService.disconnect();
        }, "Should handle multiple disconnect calls gracefully");
    }

    @Test
    @DisplayName("Test hardcoded constants - DB_HOST")
    public void testDbHostConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_HOST");
        field.setAccessible(true);
        String host = (String) field.get(null);
        assertEquals("localhost", host, "DB_HOST should be localhost");
    }

    @Test
    @DisplayName("Test hardcoded constants - DB_PORT")
    public void testDbPortConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_PORT");
        field.setAccessible(true);
        String port = (String) field.get(null);
        assertEquals("3306", port, "DB_PORT should be 3306");
    }

    @Test
    @DisplayName("Test hardcoded constants - DB_NAME")
    public void testDbNameConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_NAME");
        field.setAccessible(true);
        String dbName = (String) field.get(null);
        assertEquals("mini_app_db", dbName, "DB_NAME should be mini_app_db");
    }

    @Test
    @DisplayName("Test hardcoded constants - DB_USERNAME")
    public void testDbUsernameConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_USERNAME");
        field.setAccessible(true);
        String username = (String) field.get(null);
        assertEquals("root", username, "DB_USERNAME should be root");
    }

    @Test
    @DisplayName("Test hardcoded constants - DB_PASSWORD")
    public void testDbPasswordConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_PASSWORD");
        field.setAccessible(true);
        String password = (String) field.get(null);
        assertEquals("password123", password, "DB_PASSWORD should be password123");
    }

    @Test
    @DisplayName("Test hardcoded constants - REDIS_HOST")
    public void testRedisHostConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("REDIS_HOST");
        field.setAccessible(true);
        String host = (String) field.get(null);
        assertEquals("127.0.0.1", host, "REDIS_HOST should be 127.0.0.1");
    }

    @Test
    @DisplayName("Test hardcoded constants - REDIS_PORT")
    public void testRedisPortConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("REDIS_PORT");
        field.setAccessible(true);
        int port = (int) field.get(null);
        assertEquals(6379, port, "REDIS_PORT should be 6379");
    }

    @Test
    @DisplayName("Test hardcoded constants - EXTERNAL_API_URL")
    public void testExternalApiUrlConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertEquals("http://api.example.com:8080/v1", url,
                    "EXTERNAL_API_URL should be http://api.example.com:8080/v1");
    }

    @Test
    @DisplayName("Test hardcoded constants - PAYMENT_SERVICE_URL")
    public void testPaymentServiceUrlConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertEquals("https://payment.internal.company.com/process", url,
                    "PAYMENT_SERVICE_URL should be https://payment.internal.company.com/process");
    }

    @Test
    @DisplayName("Test hardcoded constants - DB_URL format")
    public void testDbUrlConstant() throws Exception {
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertEquals("jdbc:mysql://localhost:3306/mini_app_db", url,
                    "DB_URL should be jdbc:mysql://localhost:3306/mini_app_db");
    }

    @Test
    @DisplayName("Test connection field initialization")
    public void testConnectionFieldInitialization() throws Exception {
        DatabaseService newService = new DatabaseService();
        java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("connection");
        field.setAccessible(true);
        Connection conn = (Connection) field.get(newService);
        assertNull(conn, "Connection should be null before connect() is called");
    }

    @Test
    @DisplayName("Test connect and disconnect lifecycle")
    public void testConnectDisconnectLifecycle() {
        assertDoesNotThrow(() -> {
            dbService.connect();
            dbService.disconnect();
            dbService.connect();
            dbService.disconnect();
        }, "Should handle multiple connect/disconnect cycles");
    }

    @Test
    @DisplayName("Test executeQuery with special characters")
    public void testExecuteQuerySpecialCharacters() {
        dbService.connect();

        assertDoesNotThrow(() -> {
            dbService.executeQuery("SELECT * FROM users WHERE name = 'O''Brien'");
            dbService.executeQuery("INSERT INTO logs VALUES (1, 'test; DROP TABLE users;')");
        }, "Should handle SQL with special characters");
    }

    @Test
    @DisplayName("Test class instantiation and type verification")
    public void testClassTypeVerification() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service);
        assertEquals("com.test.DatabaseService", service.getClass().getName(),
                    "Should be correct class type");
    }

    @Test
    @DisplayName("Test all private methods are accessible via reflection")
    public void testPrivateMethodsAccessibility() throws Exception {
        java.lang.reflect.Method connectCache = DatabaseService.class.getDeclaredMethod("connectToCache");
        connectCache.setAccessible(true);
        assertNotNull(connectCache, "connectToCache method should exist");

        java.lang.reflect.Method initExternal = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        initExternal.setAccessible(true);
        assertNotNull(initExternal, "initializeExternalServices method should exist");
    }

    @Test
    @DisplayName("Test executeQuery prints execution information")
    public void testExecuteQueryPrintsInformation() {
        dbService.connect();
        outContent.reset();

        dbService.executeQuery("SELECT * FROM test_table");

        String output = outContent.toString();
        String errors = errContent.toString();
        assertTrue(output.length() > 0 || errors.length() > 0,
                  "Should produce output during query execution");
    }
}
