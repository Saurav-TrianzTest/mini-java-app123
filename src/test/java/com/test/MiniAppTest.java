package com.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 tests for MiniApp class.
 * Tests cover: constructor, main(), initializeApplication(),
 * loadConfiguration(), initializeLogging(), startServer(),
 * and all static constants.
 */
@DisplayName("MiniApp Tests")
class MiniAppTest {

    private MiniApp miniApp;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        miniApp = new MiniApp();
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
    @DisplayName("Default constructor creates a non-null MiniApp instance")
    void constructor_defaultConstructor_createsInstance() {
        // Arrange & Act
        MiniApp app = new MiniApp();

        // Assert
        assertNotNull(app, "MiniApp instance should not be null");
    }

    @Test
    @DisplayName("Multiple MiniApp instances can be created independently")
    void constructor_multipleInstances_areIndependent() {
        // Arrange & Act
        MiniApp app1 = new MiniApp();
        MiniApp app2 = new MiniApp();

        // Assert
        assertNotNull(app1);
        assertNotNull(app2);
        assertNotSame(app1, app2, "Each MiniApp instance should be a distinct object");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Constants Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SERVER_PORT constant has expected value of 8080")
    void staticConstants_serverPort_isEightZeroEightZero() throws Exception {
        Field serverPortField = MiniApp.class.getDeclaredField("SERVER_PORT");
        serverPortField.setAccessible(true);
        int serverPort = (int) serverPortField.get(null);

        assertEquals(8080, serverPort, "SERVER_PORT should be 8080");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant is not null or empty")
    void staticConstants_configFilePath_isNotNullOrEmpty() throws Exception {
        Field configPathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        configPathField.setAccessible(true);
        String configPath = (String) configPathField.get(null);

        assertNotNull(configPath, "CONFIG_FILE_PATH should not be null");
        assertFalse(configPath.isEmpty(), "CONFIG_FILE_PATH should not be empty");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant has expected absolute path")
    void staticConstants_configFilePath_hasExpectedPath() throws Exception {
        Field configPathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        configPathField.setAccessible(true);
        String configPath = (String) configPathField.get(null);

        assertEquals("/opt/app/config/app.properties", configPath,
                "CONFIG_FILE_PATH should be /opt/app/config/app.properties");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant has expected absolute path")
    void staticConstants_logFilePath_hasExpectedPath() throws Exception {
        Field logPathField = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        logPathField.setAccessible(true);
        String logPath = (String) logPathField.get(null);

        assertEquals("/var/log/mini-app.log", logPath,
                "LOG_FILE_PATH should be /var/log/mini-app.log");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant is not null or empty")
    void staticConstants_logFilePath_isNotNullOrEmpty() throws Exception {
        Field logPathField = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        logPathField.setAccessible(true);
        String logPath = (String) logPathField.get(null);

        assertNotNull(logPath, "LOG_FILE_PATH should not be null");
        assertFalse(logPath.isEmpty(), "LOG_FILE_PATH should not be empty");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadConfiguration() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loadConfiguration() does not throw when config file does not exist")
    void loadConfiguration_whenConfigFileDoesNotExist_doesNotThrow() throws Exception {
        // Arrange
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> loadConfigMethod.invoke(miniApp),
                "loadConfiguration() should not throw when config file is missing");
    }

    @Test
    @DisplayName("loadConfiguration() prints warning when config file does not exist")
    void loadConfiguration_whenConfigFileDoesNotExist_printsWarning() throws Exception {
        // Arrange
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act
        loadConfigMethod.invoke(miniApp);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Warning: Configuration file not found"),
                "Should print warning when config file is not found");
    }

    @Test
    @DisplayName("loadConfiguration() mentions the config file path in warning")
    void loadConfiguration_whenConfigFileDoesNotExist_mentionsPathInWarning() throws Exception {
        // Arrange
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act
        loadConfigMethod.invoke(miniApp);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("/opt/app/config/app.properties"),
                "Warning should mention the config file path");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeLogging() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeLogging() does not throw exception")
    void initializeLogging_whenCalled_doesNotThrow() throws Exception {
        // Arrange
        Method initLoggingMethod = MiniApp.class.getDeclaredMethod("initializeLogging");
        initLoggingMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> initLoggingMethod.invoke(miniApp),
                "initializeLogging() should not propagate exceptions");
    }

    @Test
    @DisplayName("initializeLogging() prints logging initialized message or error")
    void initializeLogging_whenCalled_printsMessage() throws Exception {
        // Arrange
        Method initLoggingMethod = MiniApp.class.getDeclaredMethod("initializeLogging");
        initLoggingMethod.setAccessible(true);

        // Act
        initLoggingMethod.invoke(miniApp);

        // Assert – either success or failure message should appear
        String combinedOutput = outContent.toString() + errContent.toString();
        boolean hasLoggingMessage = combinedOutput.contains("Logging initialized")
                || combinedOutput.contains("Failed to initialize logging");
        assertTrue(hasLoggingMessage,
                "Should print either logging initialized or failure message");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // startServer() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startServer() does not throw exception")
    void startServer_whenCalled_doesNotThrow() throws Exception {
        // Arrange
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> startServerMethod.invoke(miniApp),
                "startServer() should not propagate exceptions");
    }

    @Test
    @DisplayName("startServer() prints server started message or error message")
    void startServer_whenCalled_printsMessage() throws Exception {
        // Arrange
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);

        // Act
        startServerMethod.invoke(miniApp);

        // Assert – either success or failure message should appear
        String combinedOutput = outContent.toString() + errContent.toString();
        boolean hasServerMessage = combinedOutput.contains("Server started on port")
                || combinedOutput.contains("Failed to start server");
        assertTrue(hasServerMessage,
                "Should print either server started or failure message");
    }

    @Test
    @DisplayName("startServer() references port 8080 in output when successful")
    void startServer_whenPortAvailable_mentionsPort8080() throws Exception {
        // Arrange
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);

        // Act
        startServerMethod.invoke(miniApp);

        // Assert
        String combinedOutput = outContent.toString() + errContent.toString();
        // Port 8080 should appear either in success or failure context
        assertTrue(combinedOutput.contains("8080") || combinedOutput.contains("Failed to start server"),
                "Output should reference port 8080 or indicate failure");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeApplication() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeApplication() does not throw exception")
    void initializeApplication_whenCalled_doesNotThrow() throws Exception {
        // Arrange
        Method initAppMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        initAppMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> initAppMethod.invoke(miniApp),
                "initializeApplication() should not propagate exceptions");
    }

    @Test
    @DisplayName("initializeApplication() triggers loadConfiguration and prints warning for missing config")
    void initializeApplication_whenCalled_triggersLoadConfiguration() throws Exception {
        // Arrange
        Method initAppMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        initAppMethod.setAccessible(true);

        // Act
        initAppMethod.invoke(miniApp);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Warning: Configuration file not found")
                        || output.contains("Configuration loaded"),
                "initializeApplication() should trigger loadConfiguration()");
    }

    @Test
    @DisplayName("initializeApplication() triggers database connect attempt")
    void initializeApplication_whenCalled_triggersDatabaseConnect() throws Exception {
        // Arrange
        Method initAppMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        initAppMethod.setAccessible(true);

        // Act
        initAppMethod.invoke(miniApp);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Connecting to database..."),
                "initializeApplication() should trigger DatabaseService.connect()");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // main() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("main() does not throw exception with empty args array")
    void main_withEmptyArgs_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}),
                "main() should not throw with empty args");
    }

    @Test
    @DisplayName("main() does not throw exception with null args")
    void main_withNullArgs_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(null),
                "main() should not throw with null args");
    }

    @Test
    @DisplayName("main() prints starting application message")
    void main_whenCalled_printsStartingMessage() {
        // Arrange & Act
        MiniApp.main(new String[]{});

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."),
                "main() should print starting message");
    }

    @Test
    @DisplayName("main() with extra args does not throw exception")
    void main_withExtraArgs_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"arg1", "arg2", "arg3"}),
                "main() should not throw with extra arguments");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reflection / Class Structure Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MiniApp class has main method with correct signature")
    void classStructure_mainMethod_hasCorrectSignature() throws Exception {
        Method mainMethod = MiniApp.class.getDeclaredMethod("main", String[].class);
        assertNotNull(mainMethod, "main() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()),
                "main() should be public");
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()),
                "main() should be static");
    }

    @Test
    @DisplayName("MiniApp class has initializeApplication private method")
    void classStructure_initializeApplicationMethod_exists() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        assertNotNull(method, "initializeApplication() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()),
                "initializeApplication() should be private");
    }

    @Test
    @DisplayName("MiniApp class has loadConfiguration private method")
    void classStructure_loadConfigurationMethod_exists() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        assertNotNull(method, "loadConfiguration() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()),
                "loadConfiguration() should be private");
    }

    @Test
    @DisplayName("MiniApp class has initializeLogging private method")
    void classStructure_initializeLoggingMethod_exists() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        assertNotNull(method, "initializeLogging() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()),
                "initializeLogging() should be private");
    }

    @Test
    @DisplayName("MiniApp class has startServer private method")
    void classStructure_startServerMethod_exists() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        assertNotNull(method, "startServer() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()),
                "startServer() should be private");
    }
}
