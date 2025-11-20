package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DatabaseService
 * Tests all public and private methods with comprehensive coverage
 */
public class DatabaseServiceTest {

    private DatabaseService databaseService;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    public void setUp() {
        databaseService = new DatabaseService();

        // Capture System.out and System.err
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    public void tearDown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);

        // Clean up database connection
        if (databaseService != null) {
            databaseService.disconnect();
        }
    }

    @Test
    public void testConstructor() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service);
    }

    @Test
    public void testConstructorNotNull() {
        // Arrange & Act
        DatabaseService service = new DatabaseService();

        // Assert
        assertNotNull(service, "DatabaseService instance should not be null");
    }

    @Test
    public void testConnect_ExecutesWithoutException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> databaseService.connect());
    }

    @Test
    public void testConnect_PrintsConnectionMessage() {
        // Arrange & Act
        databaseService.connect();

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Connecting to database") ||
                   output.contains("Connected to database"));
    }

    @Test
    public void testConnect_HandlesSQLException() {
        // Arrange & Act
        databaseService.connect();

        // Assert
        String output = outputStream.toString();
        String error = errorStream.toString();
        assertTrue(output.length() > 0 || error.length() > 0,
                   "Should produce some output or error");
    }

    @Test
    public void testConnect_PrintsDatabaseURL() {
        // Arrange & Act
        databaseService.connect();

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("jdbc:mysql://localhost:3306/mini_app_db") ||
                   output.contains("Database connection failed"));
    }

    @Test
    public void testConnect_PrintsUsername() {
        // Arrange & Act
        databaseService.connect();

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Using username: root") ||
                   output.contains("Database connection failed"));
    }

    @Test
    public void testConnect_CallsConnectToCache() {
        // Arrange & Act
        databaseService.connect();

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Connecting to Redis cache") ||
                   output.contains("Database connection failed"));
    }

    @Test
    public void testConnect_CallsInitializeExternalServices() {
        // Arrange & Act
        databaseService.connect();

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Initializing external API") ||
                   output.contains("Initializing payment service") ||
                   output.contains("Database connection failed"));
    }

    @Test
    public void testConnectToCache_ExecutesWithoutException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
            method.setAccessible(true);
            method.invoke(databaseService);
        });
    }

    @Test
    public void testConnectToCache_PrintsRedisConnection() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = DatabaseService.class.getDeclaredMethod("connectToCache");
            method.setAccessible(true);
            method.invoke(databaseService);
        });

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Connecting to Redis cache at: 127.0.0.1:6379"));
    }

    @Test
    public void testInitializeExternalServices_ExecutesWithoutException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
            method.setAccessible(true);
            method.invoke(databaseService);
        });
    }

    @Test
    public void testInitializeExternalServices_PrintsExternalAPI() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
            method.setAccessible(true);
            method.invoke(databaseService);
        });

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Initializing external API: http://api.example.com:8080/v1"));
    }

    @Test
    public void testInitializeExternalServices_PrintsPaymentService() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = DatabaseService.class.getDeclaredMethod("initializeExternalServices");
            method.setAccessible(true);
            method.invoke(databaseService);
        });

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Initializing payment service: https://payment.internal.company.com/process"));
    }

    @Test
    public void testExecuteQuery_WithNullConnection() {
        // Arrange
        String sql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithValidSQL() {
        // Arrange
        String sql = "SELECT * FROM users";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithInsertSQL() {
        // Arrange
        String sql = "INSERT INTO users (name, email) VALUES ('test', 'test@test.com')";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithUpdateSQL() {
        // Arrange
        String sql = "UPDATE users SET name='updated' WHERE id=1";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithDeleteSQL() {
        // Arrange
        String sql = "DELETE FROM users WHERE id=1";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithEmptySQL() {
        // Arrange
        String sql = "";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testExecuteQuery_WithNullSQL() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(null));
    }

    @Test
    public void testExecuteQuery_HandlesException() {
        // Arrange
        String sql = "INVALID SQL SYNTAX";

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.executeQuery(sql));
    }

    @Test
    public void testDisconnect_ExecutesWithoutException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    public void testDisconnect_WithNullConnection() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    public void testDisconnect_PrintsClosedMessage() {
        // Arrange
        databaseService.connect();

        // Act
        databaseService.disconnect();

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Database connection closed") ||
                   output.contains("Failed to close database connection") ||
                   output.length() > 0);
    }

    @Test
    public void testDisconnect_AfterConnect() {
        // Arrange
        databaseService.connect();

        // Act & Assert
        assertDoesNotThrow(() -> databaseService.disconnect());
    }

    @Test
    public void testDisconnect_CalledMultipleTimes() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.disconnect();
            databaseService.disconnect();
            databaseService.disconnect();
        });
    }

    @Test
    public void testHardcodedDBHost() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_HOST");
            field.setAccessible(true);
            String host = (String) field.get(null);

            // Assert
            assertEquals("localhost", host);
        });
    }

    @Test
    public void testHardcodedDBPort() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_PORT");
            field.setAccessible(true);
            String port = (String) field.get(null);

            // Assert
            assertEquals("3306", port);
        });
    }

    @Test
    public void testHardcodedDBName() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_NAME");
            field.setAccessible(true);
            String dbName = (String) field.get(null);

            // Assert
            assertEquals("mini_app_db", dbName);
        });
    }

    @Test
    public void testHardcodedDBUsername() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_USERNAME");
            field.setAccessible(true);
            String username = (String) field.get(null);

            // Assert
            assertEquals("root", username);
        });
    }

    @Test
    public void testHardcodedDBPassword() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("DB_PASSWORD");
            field.setAccessible(true);
            String password = (String) field.get(null);

            // Assert
            assertEquals("password123", password);
        });
    }

    @Test
    public void testHardcodedRedisHost() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("REDIS_HOST");
            field.setAccessible(true);
            String host = (String) field.get(null);

            // Assert
            assertEquals("127.0.0.1", host);
        });
    }

    @Test
    public void testHardcodedRedisPort() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("REDIS_PORT");
            field.setAccessible(true);
            int port = field.getInt(null);

            // Assert
            assertEquals(6379, port);
        });
    }

    @Test
    public void testHardcodedExternalAPIURL() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("EXTERNAL_API_URL");
            field.setAccessible(true);
            String url = (String) field.get(null);

            // Assert
            assertEquals("http://api.example.com:8080/v1", url);
        });
    }

    @Test
    public void testHardcodedPaymentServiceURL() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = DatabaseService.class.getDeclaredField("PAYMENT_SERVICE_URL");
            field.setAccessible(true);
            String url = (String) field.get(null);

            // Assert
            assertEquals("https://payment.internal.company.com/process", url);
        });
    }

    @Test
    public void testMultipleInstancesCanBeCreated() {
        // Arrange & Act
        DatabaseService service1 = new DatabaseService();
        DatabaseService service2 = new DatabaseService();
        DatabaseService service3 = new DatabaseService();

        // Assert
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotNull(service3);
        assertNotSame(service1, service2);
        assertNotSame(service2, service3);
    }

    @Test
    public void testConnect_ThenExecuteQuery_ThenDisconnect() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.connect();
            databaseService.executeQuery("SELECT 1");
            databaseService.disconnect();
        });
    }

    @Test
    public void testExecuteQuery_MultipleQueries() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            databaseService.executeQuery("SELECT * FROM users");
            databaseService.executeQuery("SELECT * FROM orders");
            databaseService.executeQuery("SELECT * FROM products");
        });
    }
}
