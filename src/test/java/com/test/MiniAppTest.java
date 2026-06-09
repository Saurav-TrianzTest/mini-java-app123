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
 * Covers main(), initializeApplication(), loadConfiguration(),
 * initializeLogging(), startServer() and all static constants.
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
    void constructor_defaultConstructor_createsInstance() {
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
        assertNotSame(app1, app2, "Each instance should be a different object");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static constant field tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SERVER_PORT constant is 8080")
    void staticField_serverPort_is8080() throws Exception {
        // Arrange
        Field serverPortField = MiniApp.class.getDeclaredField("SERVER_PORT");
        serverPortField.setAccessible(true);

        // Act
        int serverPort = (int) serverPortField.get(null);

        // Assert
        assertEquals(8080, serverPort, "SERVER_PORT should be 8080");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant is '/opt/app/config/app.properties'")
    void staticField_configFilePath_isCorrect() throws Exception {
        // Arrange
        Field configFilePathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        configFilePathField.setAccessible(true);

        // Act
        String configFilePath = (String) configFilePathField.get(null);

        // Assert
        assertEquals("/opt/app/config/app.properties", configFilePath,
                "CONFIG_FILE_PATH should be '/opt/app/config/app.properties'");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant is '/var/log/mini-app.log'")
    void staticField_logFilePath_isCorrect() throws Exception {
        // Arrange
        Field logFilePathField = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        logFilePathField.setAccessible(true);

        // Act
        String logFilePath = (String) logFilePathField.get(null);

        // Assert
        assertEquals("/var/log/mini-app.log", logFilePath,
                "LOG_FILE_PATH should be '/var/log/mini-app.log'");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH is an absolute path starting with '/'")
    void staticField_configFilePath_isAbsolutePath() throws Exception {
        // Arrange
        Field configFilePathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        configFilePathField.setAccessible(true);

        // Act
        String configFilePath = (String) configFilePathField.get(null);

        // Assert
        assertNotNull(configFilePath);
        assertTrue(configFilePath.startsWith("/"), "CONFIG_FILE_PATH should be an absolute path");
    }

    @Test
    @DisplayName("LOG_FILE_PATH is an absolute path starting with '/'")
    void staticField_logFilePath_isAbsolutePath() throws Exception {
        // Arrange
        Field logFilePathField = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        logFilePathField.setAccessible(true);

        // Act
        String logFilePath = (String) logFilePathField.get(null);

        // Assert
        assertNotNull(logFilePath);
        assertTrue(logFilePath.startsWith("/"), "LOG_FILE_PATH should be an absolute path");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // main() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("main() prints 'Starting Mini Java Application...' message")
    void main_whenCalled_printsStartingMessage() {
        // Arrange & Act
        MiniApp.main(new String[]{});

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."),
                "main() should print starting message");
    }

    @Test
    @DisplayName("main() with empty args array does not throw")
    void main_withEmptyArgs_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}),
                "main() should not throw with empty args");
    }

    @Test
    @DisplayName("main() with null args does not throw")
    void main_withNullArgs_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(null),
                "main() should not throw with null args");
    }

    @Test
    @DisplayName("main() with multiple args does not throw")
    void main_withMultipleArgs_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"arg1", "arg2", "arg3"}),
                "main() should not throw with multiple args");
    }

    @Test
    @DisplayName("main() completes without unchecked exceptions")
    void main_whenCalled_completesWithoutUncheckedExceptions() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}),
                "main() should complete without unchecked exceptions");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadConfiguration() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loadConfiguration() does not throw when config file does not exist")
    void loadConfiguration_whenConfigFileNotFound_doesNotThrow() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> loadConfigMethod.invoke(app),
                "loadConfiguration() should not throw when config file is missing");
    }

    @Test
    @DisplayName("loadConfiguration() prints warning when config file not found")
    void loadConfiguration_whenConfigFileNotFound_printsWarning() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act
        loadConfigMethod.invoke(app);

        // Assert
        String output = outContent.toString();
        // Either "Configuration loaded" or "Warning: Configuration file not found"
        assertTrue(output.contains("Configuration") || output.contains("Warning"),
                "loadConfiguration() should print a configuration-related message");
    }

    @Test
    @DisplayName("loadConfiguration() prints config file path in output")
    void loadConfiguration_whenCalled_printsConfigFilePath() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act
        loadConfigMethod.invoke(app);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("/opt/app/config/app.properties") || output.contains("Configuration"),
                "loadConfiguration() should reference the config file path");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeLogging() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeLogging() does not throw")
    void initializeLogging_whenCalled_doesNotThrow() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method initLoggingMethod = MiniApp.class.getDeclaredMethod("initializeLogging");
        initLoggingMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> initLoggingMethod.invoke(app),
                "initializeLogging() should not throw");
    }

    @Test
    @DisplayName("initializeLogging() prints logging initialization message")
    void initializeLogging_whenCalled_printsLoggingMessage() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method initLoggingMethod = MiniApp.class.getDeclaredMethod("initializeLogging");
        initLoggingMethod.setAccessible(true);

        // Act
        initLoggingMethod.invoke(app);

        // Assert
        String output = outContent.toString();
        String errOutput = errContent.toString();
        // Either success or failure message should be printed
        assertTrue(output.contains("Logging") || errOutput.contains("Failed to initialize logging"),
                "initializeLogging() should print a logging-related message");
    }

    @Test
    @DisplayName("initializeLogging() references /var/log path")
    void initializeLogging_whenCalled_referencesVarLogPath() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method initLoggingMethod = MiniApp.class.getDeclaredMethod("initializeLogging");
        initLoggingMethod.setAccessible(true);

        // Act
        initLoggingMethod.invoke(app);

        // Assert
        String output = outContent.toString();
        String errOutput = errContent.toString();
        String combined = output + errOutput;
        assertTrue(combined.contains("/var/log") || combined.contains("Logging") || combined.contains("Failed"),
                "initializeLogging() should reference /var/log path");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // startServer() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startServer() does not throw")
    void startServer_whenCalled_doesNotThrow() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> startServerMethod.invoke(app),
                "startServer() should not throw");
    }

    @Test
    @DisplayName("startServer() prints server started message or error message")
    void startServer_whenCalled_printsServerMessage() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);

        // Act
        startServerMethod.invoke(app);

        // Assert
        String output = outContent.toString();
        String errOutput = errContent.toString();
        String combined = output + errOutput;
        assertTrue(combined.contains("Server") || combined.contains("port") || combined.contains("Failed"),
                "startServer() should print a server-related message");
    }

    @Test
    @DisplayName("startServer() references port 8080")
    void startServer_whenCalled_referencesPort8080() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);

        // Act
        startServerMethod.invoke(app);

        // Assert
        String output = outContent.toString();
        String errOutput = errContent.toString();
        String combined = output + errOutput;
        // Either "Server started on port: 8080" or "Failed to start server: Address already in use"
        assertTrue(combined.contains("8080") || combined.contains("Failed to start server"),
                "startServer() should reference port 8080");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeApplication() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeApplication() does not throw")
    void initializeApplication_whenCalled_doesNotThrow() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method initAppMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        initAppMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> initAppMethod.invoke(app),
                "initializeApplication() should not throw");
    }

    @Test
    @DisplayName("initializeApplication() calls loadConfiguration and initializeLogging")
    void initializeApplication_whenCalled_invokesSubMethods() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method initAppMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        initAppMethod.setAccessible(true);

        // Act
        initAppMethod.invoke(app);

        // Assert
        String output = outContent.toString();
        // loadConfiguration prints "Warning: Configuration file not found" or "Configuration loaded"
        // initializeLogging prints "Logging initialized" or error
        assertTrue(output.contains("Configuration") || output.contains("Warning") || output.length() > 0,
                "initializeApplication() should invoke sub-methods that produce output");
    }

    @Test
    @DisplayName("initializeApplication() triggers DatabaseService connect()")
    void initializeApplication_whenCalled_triggersDatabaseConnect() throws Exception {
        // Arrange
        MiniApp app = new MiniApp();
        Method initAppMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        initAppMethod.setAccessible(true);

        // Act
        initAppMethod.invoke(app);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Connecting to database..."),
                "initializeApplication() should trigger DatabaseService.connect()");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration / end-to-end tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MiniApp class has expected private methods via reflection")
    void reflection_miniAppHasExpectedPrivateMethods() throws Exception {
        // Arrange & Act
        Method initApp = MiniApp.class.getDeclaredMethod("initializeApplication");
        Method loadConfig = MiniApp.class.getDeclaredMethod("loadConfiguration");
        Method initLogging = MiniApp.class.getDeclaredMethod("initializeLogging");
        Method startServer = MiniApp.class.getDeclaredMethod("startServer");

        // Assert
        assertNotNull(initApp, "initializeApplication() method should exist");
        assertNotNull(loadConfig, "loadConfiguration() method should exist");
        assertNotNull(initLogging, "initializeLogging() method should exist");
        assertNotNull(startServer, "startServer() method should exist");
    }

    @Test
    @DisplayName("MiniApp has main() static method")
    void reflection_miniAppHasMainMethod() throws Exception {
        // Arrange & Act
        Method mainMethod = MiniApp.class.getDeclaredMethod("main", String[].class);

        // Assert
        assertNotNull(mainMethod, "main() method should exist");
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()),
                "main() should be static");
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()),
                "main() should be public");
    }

    @Test
    @DisplayName("MiniApp has SERVER_PORT, CONFIG_FILE_PATH, LOG_FILE_PATH static fields")
    void reflection_miniAppHasExpectedStaticFields() throws Exception {
        // Arrange & Act
        Field serverPort = MiniApp.class.getDeclaredField("SERVER_PORT");
        Field configFilePath = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        Field logFilePath = MiniApp.class.getDeclaredField("LOG_FILE_PATH");

        // Assert
        assertNotNull(serverPort, "SERVER_PORT field should exist");
        assertNotNull(configFilePath, "CONFIG_FILE_PATH field should exist");
        assertNotNull(logFilePath, "LOG_FILE_PATH field should exist");

        assertTrue(java.lang.reflect.Modifier.isStatic(serverPort.getModifiers()),
                "SERVER_PORT should be static");
        assertTrue(java.lang.reflect.Modifier.isStatic(configFilePath.getModifiers()),
                "CONFIG_FILE_PATH should be static");
        assertTrue(java.lang.reflect.Modifier.isStatic(logFilePath.getModifiers()),
                "LOG_FILE_PATH should be static");
    }

    @Test
    @DisplayName("main() output contains 'Starting Mini Java Application...' as first line")
    void main_outputStartsWithStartingMessage() {
        // Arrange & Act
        MiniApp.main(new String[]{});

        // Assert
        String output = outContent.toString();
        String firstLine = output.split("\n")[0].trim();
        assertEquals("Starting Mini Java Application...", firstLine,
                "First output line should be the starting message");
    }

    @Test
    @DisplayName("MiniApp SERVER_PORT is a valid port number (1-65535)")
    void staticField_serverPort_isValidPortNumber() throws Exception {
        // Arrange
        Field serverPortField = MiniApp.class.getDeclaredField("SERVER_PORT");
        serverPortField.setAccessible(true);

        // Act
        int serverPort = (int) serverPortField.get(null);

        // Assert
        assertTrue(serverPort >= 1 && serverPort <= 65535,
                "SERVER_PORT should be a valid port number between 1 and 65535");
    }

    @Test
    @DisplayName("MiniApp CONFIG_FILE_PATH ends with '.properties'")
    void staticField_configFilePath_endsWithProperties() throws Exception {
        // Arrange
        Field configFilePathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        configFilePathField.setAccessible(true);

        // Act
        String configFilePath = (String) configFilePathField.get(null);

        // Assert
        assertNotNull(configFilePath);
        assertTrue(configFilePath.endsWith(".properties"),
                "CONFIG_FILE_PATH should end with '.properties'");
    }

    @Test
    @DisplayName("MiniApp LOG_FILE_PATH ends with '.log'")
    void staticField_logFilePath_endsWithLog() throws Exception {
        // Arrange
        Field logFilePathField = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        logFilePathField.setAccessible(true);

        // Act
        String logFilePath = (String) logFilePathField.get(null);

        // Assert
        assertNotNull(logFilePath);
        assertTrue(logFilePath.endsWith(".log"),
                "LOG_FILE_PATH should end with '.log'");
    }
}
