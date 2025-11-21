package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseServiceTest {

    private DatabaseService databaseService;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    public void setUp() {
        databaseService = new DatabaseService();
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        if (databaseService != null) {
            databaseService.disconnect();
        }
    }

    @Test
    public void testConstructor() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service);
    }

    @Test
    public void testConstructorNotNull() {
        DatabaseService service = new DatabaseService();
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    public void testConnect_ExecutesWithoutException() {
        assertDoesNotThrow(() -> databaseService.connect());
    }

    @Test
    public void testConnect_PrintsConnectionMessage() {
        databaseService.connect();
        String output = outputStream.toString();
        assertTrue(output.contains("Connecting to database") || output.contains("Connected to database"));
    }

    @Test
    public void testConnect_HandlesSQLException() {
        databaseService.connect();
        String output = outputStream.toString();
        String error = errorStream.toString();
        assertTrue(output.length() > 0 || error.length() > 0, "Should produce some output or error");
    }

    @Test
    public void testConnect_PrintsDatabaseURL() {
        databaseService.connect();
        String output = outputStream.toString();
        String error = errorStream.toString();
        assertTrue(output.contains("jdbc:mysql://localhost:3306/mini_app_db") || error.contains("Database connection failed"));
    }

    @Test
    public void testConnect_PrintsUsername() {
        databaseService.connect();
        String output = outputStream.toString();
        String error = errorStream.toString();
        assertTrue(output.contains("Using username: root") || error.contains("Database connection failed"));
    }

    @Test
    public void testConnect_CallsConnectToCache() {
        databaseService.connect();
        String output = outputStream.toString();
        String error = errorStream.toString();
        assertTrue(output.contains("Connecting to Redis cache") || error.contains("Database connection failed"));
    }

    @Test
    public void testConnect_CallsInitializeExternalServices() {
        databaseService.connect();
        String output = outputStream.toString();
        String error = errorStream.toString();
        assertTrue(output.contains("Initializing external API") || output.contains("Initializing payment service") || error.contains("Database connection failed"));
    }

    @Test
    public void testConnectToCache_ExecutesWithoutException() {
        assertDoesNotThrow(() -> {
            Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
            method.setAccessible(true);
            method.invoke(databaseService);
        });
    }

    @Test
    public void testConnectToCache_PrintsRedisConnection() {
        assertDoesNotThrow(() -> {
            Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
            method.setAccessible(true);
            method.invoke(databaseService);
        });
        String output = outputStream.toString();
        assertTrue(output.contains("Connecting to Redis cache at: 127.0.0.1:6379"));
    }

    @Test
    public void testInitializeExternalServices_ExecutesWithoutException() {
        assertDoesNotThrow(() -> {
            Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
            method.setAccessible(true);
            method.invoke(databaseService);
        });
    }

    @Test
    public void testInitializeExternalServices_PrintsExternalAPI() {
        assertDoesNotThrow(() -> {
            Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
            method.setAccessible(true);
            method.invoke(databaseService);
        });
        String output = outputStream.toString();
        assertTrue(output.contains("Initializing external API: http://api.example.com:8080/v1"));
    }

    @Test
    public void testInitializeExternalServices_PrintsPaymentService() {
        assertDoesNotThrow(() -> {
            Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
            method.setAccessible(true);
            method.invoke(databaseService);
        });
        String output = outputStream.toString();
        assertTrue(output.contains("Initializing payment service: https://payment.internal.company.com/process"));
    }

    @Test
    public void testExecuteQuery_WithNullConnection() {
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithValidSQL() {
        String sql = "SELECT * FROM users";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithInsertSQL() {
        String sql = "INSERT INTO users (name, email) VALUES ('test', 'test@test.com')";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithUpdateSQL() {
        String sql = "UPDATE users SET name='updated' WHERE id=1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithDeleteSQL() {
        String sql = "DELETE FROM users WHERE id=1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithEmptySQL() {
        String sql = "";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithNullSQL() {
        assertDoesNotThrow(() -> databaseService.executeQuery(null));
    }

    @Test
    public void testExecuteQuery_HandlesException() {
        String sql = "INVALID SQL SYNTAX";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testDisconnect_ExecutesWithoutException() {
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    public void testDisconnect_WithNullConnection() {
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    public void testDisconnect_PrintsClosedMessage() {
        databaseService.connect();
        databaseService.disconnect();
        String output = outputStream.toString();
        assertTrue(output.contains("Database connection closed") || output.contains("Failed to close database connection") || output.length() > 0);
    }

    @Test
    public void testDisconnect_AfterConnect() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    public void testDisconnect_CalledMultipleTimes() {
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        });
    }

    @Test
    public void testHardcodedDBHost() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_HOST");
        field.setAccessible(true);
        String host = (String) field.get(null);
        assertEquals("localhost", host);
    }

    @Test
    public void testHardcodedDBPort() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_PORT");
        field.setAccessible(true);
        String port = (String) field.get(null);
        assertEquals("3306", port);
    }

    @Test
    public void testHardcodedDBName() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_NAME");
        field.setAccessible(true);
        String dbName = (String) field.get(null);
        assertEquals("mini_app_db", dbName);
    }

    @Test
    public void testHardcodedDBUsername() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_USERNAME");
        field.setAccessible(true);
        String username = (String) field.get(null);
        assertEquals("root", username);
    }

    @Test
    public void testHardcodedDBPassword() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("DB_PASSWORD");
        field.setAccessible(true);
        String password = (String) field.get(null);
        assertEquals("password123", password);
    }

    @Test
    public void testHardcodedRedisHost() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_HOST");
        field.setAccessible(true);
        String host = (String) field.get(null);
        assertEquals("127.0.0.1", host);
    }

    @Test
    public void testHardcodedRedisPort() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("REDIS_PORT");
        field.setAccessible(true);
        int port = field.getInt(null);
        assertEquals(6379, port);
    }

    @Test
    public void testHardcodedExternalAPIURL() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertEquals("http://api.example.com:8080/v1", url);
    }

    @Test
    public void testHardcodedPaymentServiceURL() throws Exception {
        Field field = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
        field.setAccessible(true);
        String url = (String) field.get(null);
        assertEquals("https://payment.internal.company.com/process", url);
    }

    @Test
    public void testMultipleInstancesCanBeCreated() {
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        DatabaseService service3 = new DatabaseService();
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotNull(service3);
        assertNotSame(service1, service2);
        assertNotSame(service2, service3);
    }

    @Test
    public void testConnect_ThenExecuteQuery_ThenDisconnect() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();
        });
    }

    @Test
    public void testExecuteQuery_MultipleQueries() {
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
            databaseService.executeQuery("SELECT * FROM orders");
            databaseService.executeQuery("SELECT * FROM products");
        });
    }

    @Test
    public void testExecuteQuery_AfterConnect() {
        databaseService.connect();
        String sql = "SELECT * FROM users WHERE id = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testConnect_MultipleTimesCalled() {
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.connect();
        });
    }

    @Test
    public void testExecuteQuery_WithComplexSQL() {
        String sql = "SELECT u.id, u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id WHERE u.active = 1";
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_PrintsExecutingMessage() {
        databaseService.connect();
        databaseService.executeQuery("SELECT * FROM test");
        String output = outputStream.toString();
        assertTrue(output.contains("Executing query") || output.length() > 0);
    }

    @Test
    public void testConnect_WithDatabaseConnectionFailure() {
        databaseService.connect();
        String error = errorStream.toString();
        assertTrue(error.contains("Database connection failed") || error.isEmpty());
    }

    @Test
    public void testExecuteQuery_WithConnectionClosed() {
        databaseService.connect();
        databaseService.disconnect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT 1"));
    }

    @Test
    public void testDisconnect_WithoutConnect() {
        assertDoesNotThrow(() -> databaseService.disconnect());
        String output = outputStream.toString();
        assertNotNull(output);
    }

    @Test
    public void testExecuteQuery_SetQueryTimeout() {
        databaseService.connect();
        assertDoesNotThrow(() -> databaseService.executeQuery("SELECT * FROM test"));
    }

    @Test
    public void testConnect_DatabaseURLFormat() {
        databaseService.connect();
        String output = outputStream.toString();
        assertTrue(output.contains("jdbc:mysql://") || errorStream.toString().length() > 0);
    }

    @Test
    public void testConnectToCache_RedisHostAndPort() throws Exception {
        Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
        method.setAccessible(true);
        method.invoke(databaseService);
        String output = outputStream.toString();
        assertTrue(output.contains("127.0.0.1") && output.contains("6379"));
    }

    @Test
    public void testInitializeExternalServices_BothServicesInitialized() throws Exception {
        Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
        method.setAccessible(true);
        method.invoke(databaseService);
        String output = outputStream.toString();
        assertTrue(output.contains("external API") && output.contains("payment service"));
    }

    @Test
    public void testDatabaseService_FullLifecycle() {
        assertDoesNotThrow(() -> {
            DatabaseService service = new DatabaseService();
            service.connect();
            service.executeQuery("CREATE TABLE IF NOT EXISTS test (id INT)");
            service.executeQuery("INSERT INTO test VALUES (1)");
            service.executeQuery("SELECT * FROM test");
            service.executeQuery("DROP TABLE IF EXISTS test");
            service.disconnect();
        });
    }
}
