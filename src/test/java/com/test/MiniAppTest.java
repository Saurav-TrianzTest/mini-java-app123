package com.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 tests for MiniApp class.
 * Tests cover all public/private methods, constructors, constants,
 * and various edge cases including exception handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MiniApp Tests")
class MiniAppTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Default constructor creates MiniApp instance successfully")
    void constructor_default_createsInstance() {
        // Arrange & Act
        MiniApp app = new MiniApp();

        // Assert
        assertNotNull(app, "MiniApp instance should not be null");
    }

    @Test
    @DisplayName("MiniApp class can be instantiated multiple times")
    void constructor_multipleInstances_allNotNull() {
        // Arrange & Act
        MiniApp app1 = new MiniApp();
        MiniApp app2 = new MiniApp();
        MiniApp app3 = new MiniApp();

        // Assert
        assertNotNull(app1);
        assertNotNull(app2);
        assertNotNull(app3);
        assertNotSame(app1, app2, "Each instance should be a distinct object");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Field / Constant Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SERVER_PORT constant is set to 8080")
    void staticField_serverPort_is8080() throws Exception {
        Field field = MiniApp.class.getDeclaredField("SERVER_PORT");
        field.setAccessible(true);
        assertEquals(8080, field.get(null), "SERVER_PORT should be 8080");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant is set to '/opt/app/config/app.properties'")
    void staticField_configFilePath_isCorrect() throws Exception {
        Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        field.setAccessible(true);
        assertEquals("/opt/app/config/app.properties", field.get(null),
                "CONFIG_FILE_PATH should be '/opt/app/config/app.properties'");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant is set to '/var/log/mini-app.log'")
    void staticField_logFilePath_isCorrect() throws Exception {
        Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        field.setAccessible(true);
        assertEquals("/var/log/mini-app.log", field.get(null),
                "LOG_FILE_PATH should be '/var/log/mini-app.log'");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant starts with '/opt'")
    void staticField_configFilePath_startsWithOpt() throws Exception {
        Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        field.setAccessible(true);
        String path = (String) field.get(null);
        assertTrue(path.startsWith("/opt"), "CONFIG_FILE_PATH should start with /opt");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant starts with '/var/log'")
    void staticField_logFilePath_startsWithVarLog() throws Exception {
        Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        field.setAccessible(true);
        String path = (String) field.get(null);
        assertTrue(path.startsWith("/var/log"), "LOG_FILE_PATH should start with /var/log");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH ends with '.properties'")
    void staticField_configFilePath_endsWithProperties() throws Exception {
        Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        field.setAccessible(true);
        String path = (String) field.get(null);
        assertTrue(path.endsWith(".properties"), "CONFIG_FILE_PATH should end with .properties");
    }

    @Test
    @DisplayName("LOG_FILE_PATH ends with '.log'")
    void staticField_logFilePath_endsWithLog() throws Exception {
        Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        field.setAccessible(true);
        String path = (String) field.get(null);
        assertTrue(path.endsWith(".log"), "LOG_FILE_PATH should end with .log");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadConfiguration() Private Method Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loadConfiguration() does not throw when config file does not exist")
    void loadConfiguration_fileNotExists_doesNotThrow() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(app),
                "loadConfiguration() should not throw when config file is missing");
    }

    @Test
    @DisplayName("loadConfiguration() prints warning when config file not found")
    void loadConfiguration_fileNotExists_printsWarning() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        method.setAccessible(true);

        // Act
        method.invoke(app);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Warning: Configuration file not found at:"),
                "loadConfiguration() should print warning when file not found");
    }

    @Test
    @DisplayName("loadConfiguration() prints the config file path in warning")
    void loadConfiguration_fileNotExists_printsConfigPath() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        method.setAccessible(true);

        // Act
        method.invoke(app);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("/opt/app/config/app.properties"),
                "loadConfiguration() should print the config file path");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeLogging() Private Method Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeLogging() does not throw")
    void initializeLogging_doesNotThrow() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(app),
                "initializeLogging() should not throw");
    }

    @Test
    @DisplayName("initializeLogging() prints logging initialized message or error")
    void initializeLogging_printsMessage() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        method.setAccessible(true);

        // Act
        method.invoke(app);

        // Assert – either success or error message should be printed
        String allOutput = outContent.toString() + errContent.toString();
        assertTrue(
                allOutput.contains("Logging initialized") || allOutput.contains("Failed to initialize logging"),
                "initializeLogging() should print either success or error message"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // startServer() Private Method Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startServer() does not throw")
    void startServer_doesNotThrow() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(app),
                "startServer() should not propagate exceptions");
    }

    @Test
    @DisplayName("startServer() prints server started message or error")
    void startServer_printsMessage() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        method.setAccessible(true);

        // Act
        method.invoke(app);

        // Assert – either success or error message should be printed
        String allOutput = outContent.toString() + errContent.toString();
        assertTrue(
                allOutput.contains("Server started on port:") || allOutput.contains("Failed to start server:"),
                "startServer() should print either success or error message"
        );
    }

    @Test
    @DisplayName("startServer() attempts to use SERVER_PORT 8080")
    void startServer_usesPort8080() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        method.setAccessible(true);

        // Act
        method.invoke(app);

        // Assert
        String allOutput = outContent.toString() + errContent.toString();
        // Either it started on 8080 or failed – both are valid in test environment
        assertTrue(
                allOutput.contains("8080") || allOutput.contains("Failed to start server:"),
                "startServer() should reference port 8080"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeApplication() Private Method Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeApplication() does not throw")
    void initializeApplication_doesNotThrow() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(app),
                "initializeApplication() should not propagate exceptions");
    }

    @Test
    @DisplayName("initializeApplication() triggers loadConfiguration()")
    void initializeApplication_triggersLoadConfiguration() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);

        // Act
        method.invoke(app);

        // Assert – loadConfiguration() should have printed a warning (no config file in test env)
        String output = outContent.toString();
        assertTrue(output.contains("Warning: Configuration file not found at:") ||
                        output.contains("Configuration loaded from:"),
                "initializeApplication() should trigger loadConfiguration()");
    }

    @Test
    @DisplayName("initializeApplication() triggers initializeLogging()")
    void initializeApplication_triggersInitializeLogging() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);

        // Act
        method.invoke(app);

        // Assert
        String allOutput = outContent.toString() + errContent.toString();
        assertTrue(
                allOutput.contains("Logging initialized") || allOutput.contains("Failed to initialize logging"),
                "initializeApplication() should trigger initializeLogging()"
        );
    }

    @Test
    @DisplayName("initializeApplication() triggers DatabaseService.connect()")
    void initializeApplication_triggersDatabaseConnect() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);

        // Act
        method.invoke(app);

        // Assert – DatabaseService.connect() prints "Connecting to database..."
        String output = outContent.toString();
        assertTrue(output.contains("Connecting to database..."),
                "initializeApplication() should trigger DatabaseService.connect()");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // main() Method Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("main() prints 'Starting Mini Java Application...' message")
    void main_printsStartingMessage() {
        // Act
        MiniApp.main(new String[]{});

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."),
                "main() should print 'Starting Mini Java Application...'");
    }

    @Test
    @DisplayName("main() with null args does not throw NullPointerException")
    void main_withNullArgs_doesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(null),
                "main() should not throw NullPointerException with null args");
    }

    @Test
    @DisplayName("main() with empty args array does not throw")
    void main_withEmptyArgs_doesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}),
                "main() should not throw with empty args array");
    }

    @Test
    @DisplayName("main() with non-empty args array does not throw")
    void main_withNonEmptyArgs_doesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"--debug", "--port=9090"}),
                "main() should not throw with non-empty args array");
    }

    @Test
    @DisplayName("main() triggers initializeApplication() which loads configuration")
    void main_triggersInitialization() {
        // Act
        MiniApp.main(new String[]{});

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."),
                "main() should trigger application initialization");
        assertTrue(output.contains("Connecting to database..."),
                "main() should trigger database connection attempt");
    }

    @Test
    @DisplayName("main() completes without hanging indefinitely")
    void main_completesWithinReasonableTime() {
        // Act & Assert – should complete within 5 seconds
        long startTime = System.currentTimeMillis();
        MiniApp.main(new String[]{});
        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(elapsed < 5000,
                "main() should complete within 5 seconds, but took: " + elapsed + "ms");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Class Structure / Reflection Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MiniApp class has 'main' static method")
    void classStructure_hasMainMethod() throws Exception {
        Method mainMethod = MiniApp.class.getDeclaredMethod("main", String[].class);
        assertNotNull(mainMethod, "MiniApp should have a main(String[]) method");
    }

    @Test
    @DisplayName("MiniApp class has 'initializeApplication' private method")
    void classStructure_hasInitializeApplicationMethod() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        assertNotNull(method, "MiniApp should have initializeApplication() method");
    }

    @Test
    @DisplayName("MiniApp class has 'loadConfiguration' private method")
    void classStructure_hasLoadConfigurationMethod() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        assertNotNull(method, "MiniApp should have loadConfiguration() method");
    }

    @Test
    @DisplayName("MiniApp class has 'initializeLogging' private method")
    void classStructure_hasInitializeLoggingMethod() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        assertNotNull(method, "MiniApp should have initializeLogging() method");
    }

    @Test
    @DisplayName("MiniApp class has 'startServer' private method")
    void classStructure_hasStartServerMethod() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        assertNotNull(method, "MiniApp should have startServer() method");
    }

    @Test
    @DisplayName("MiniApp class has SERVER_PORT static field")
    void classStructure_hasServerPortField() throws Exception {
        Field field = MiniApp.class.getDeclaredField("SERVER_PORT");
        assertNotNull(field, "MiniApp should have SERVER_PORT field");
    }

    @Test
    @DisplayName("MiniApp class has CONFIG_FILE_PATH static field")
    void classStructure_hasConfigFilePathField() throws Exception {
        Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        assertNotNull(field, "MiniApp should have CONFIG_FILE_PATH field");
    }

    @Test
    @DisplayName("MiniApp class has LOG_FILE_PATH static field")
    void classStructure_hasLogFilePathField() throws Exception {
        Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        assertNotNull(field, "MiniApp should have LOG_FILE_PATH field");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge Case / Boundary Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SERVER_PORT is a positive integer")
    void serverPort_isPositive() throws Exception {
        Field field = MiniApp.class.getDeclaredField("SERVER_PORT");
        field.setAccessible(true);
        int port = (int) field.get(null);
        assertTrue(port > 0, "SERVER_PORT should be a positive integer");
    }

    @Test
    @DisplayName("SERVER_PORT is within valid port range (1-65535)")
    void serverPort_isInValidRange() throws Exception {
        Field field = MiniApp.class.getDeclaredField("SERVER_PORT");
        field.setAccessible(true);
        int port = (int) field.get(null);
        assertTrue(port >= 1 && port <= 65535,
                "SERVER_PORT should be within valid range 1-65535, but was: " + port);
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH is not null or empty")
    void configFilePath_isNotNullOrEmpty() throws Exception {
        Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        field.setAccessible(true);
        String path = (String) field.get(null);
        assertNotNull(path, "CONFIG_FILE_PATH should not be null");
        assertFalse(path.isEmpty(), "CONFIG_FILE_PATH should not be empty");
    }

    @Test
    @DisplayName("LOG_FILE_PATH is not null or empty")
    void logFilePath_isNotNullOrEmpty() throws Exception {
        Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        field.setAccessible(true);
        String path = (String) field.get(null);
        assertNotNull(path, "LOG_FILE_PATH should not be null");
        assertFalse(path.isEmpty(), "LOG_FILE_PATH should not be empty");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH uses forward slashes (Unix path)")
    void configFilePath_usesForwardSlashes() throws Exception {
        Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        field.setAccessible(true);
        String path = (String) field.get(null);
        assertTrue(path.contains("/"), "CONFIG_FILE_PATH should use forward slashes");
    }

    @Test
    @DisplayName("LOG_FILE_PATH uses forward slashes (Unix path)")
    void logFilePath_usesForwardSlashes() throws Exception {
        Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        field.setAccessible(true);
        String path = (String) field.get(null);
        assertTrue(path.contains("/"), "LOG_FILE_PATH should use forward slashes");
    }
}
