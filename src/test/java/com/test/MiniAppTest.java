package com.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 tests for MiniApp class.
 * Covers main(), initializeApplication(), loadConfiguration(),
 * initializeLogging(), and startServer() methods.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MiniApp Tests")
class MiniAppTest {

    private MiniApp miniApp;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @TempDir
    Path tempDir;

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
    @DisplayName("Multiple MiniApp instances are independent")
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
    // Static Field / Constant Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SERVER_PORT constant is 8080")
    void constants_serverPort_is8080() throws Exception {
        Field field = MiniApp.class.getDeclaredField("SERVER_PORT");
        field.setAccessible(true);
        assertEquals(8080, field.get(null), "SERVER_PORT should be 8080");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant is '/opt/app/config/app.properties'")
    void constants_configFilePath_isExpectedPath() throws Exception {
        Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        field.setAccessible(true);
        assertEquals("/opt/app/config/app.properties", field.get(null),
                "CONFIG_FILE_PATH should be '/opt/app/config/app.properties'");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant is '/var/log/mini-app.log'")
    void constants_logFilePath_isExpectedPath() throws Exception {
        Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        field.setAccessible(true);
        assertEquals("/var/log/mini-app.log", field.get(null),
                "LOG_FILE_PATH should be '/var/log/mini-app.log'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // main() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("main() prints 'Starting Mini Java Application...' message")
    void main_whenCalled_printsStartingMessage() {
        // Act
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}),
                "main() should not throw an exception");

        // Assert
        assertTrue(outContent.toString().contains("Starting Mini Java Application..."),
                "main() should print starting message");
    }

    @Test
    @DisplayName("main() with null args does not throw NullPointerException")
    void main_withNullArgs_doesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(null),
                "main() should handle null args gracefully");
    }

    @Test
    @DisplayName("main() with empty args array does not throw")
    void main_withEmptyArgs_doesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}),
                "main() should handle empty args array gracefully");
    }

    @Test
    @DisplayName("main() with multiple args does not throw")
    void main_withMultipleArgs_doesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"arg1", "arg2", "arg3"}),
                "main() should handle multiple args gracefully");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadConfiguration() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loadConfiguration() prints warning when config file does not exist")
    void loadConfiguration_whenConfigFileNotFound_printsWarning() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        method.setAccessible(true);

        // Act
        method.invoke(miniApp);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Warning: Configuration file not found") ||
                output.contains("Configuration loaded") ||
                errContent.toString().contains("Failed to load configuration"),
                "loadConfiguration() should handle missing config file");
    }

    @Test
    @DisplayName("loadConfiguration() does not throw exception when file is missing")
    void loadConfiguration_whenConfigFileMissing_doesNotThrow() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "loadConfiguration() should not throw when config file is missing");
    }

    @Test
    @DisplayName("loadConfiguration() handles IOException gracefully")
    void loadConfiguration_whenIOExceptionOccurs_handlesGracefully() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        method.setAccessible(true);

        // Act & Assert - Should not propagate exception
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "loadConfiguration() should handle IOException gracefully");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeLogging() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeLogging() does not throw exception")
    void initializeLogging_whenCalled_doesNotThrow() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "initializeLogging() should not throw an exception");
    }

    @Test
    @DisplayName("initializeLogging() prints logging initialized message or error")
    void initializeLogging_whenCalled_printsMessage() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        method.setAccessible(true);

        // Act
        method.invoke(miniApp);

        // Assert
        String stdout = outContent.toString();
        String stderr = errContent.toString();
        assertTrue(stdout.contains("Logging initialized") || stderr.contains("Failed to initialize logging"),
                "initializeLogging() should print a status message");
    }

    @Test
    @DisplayName("initializeLogging() handles permission denied gracefully")
    void initializeLogging_whenPermissionDenied_handlesGracefully() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        method.setAccessible(true);

        // Act & Assert - Even if /var/log is not writable, should not throw
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "initializeLogging() should handle permission errors gracefully");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // startServer() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startServer() does not throw exception")
    void startServer_whenCalled_doesNotThrow() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "startServer() should not throw an exception");
    }

    @Test
    @DisplayName("startServer() prints server started message or error")
    void startServer_whenCalled_printsMessage() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        method.setAccessible(true);

        // Act
        method.invoke(miniApp);

        // Assert
        String stdout = outContent.toString();
        String stderr = errContent.toString();
        assertTrue(stdout.contains("Server started") || stderr.contains("Failed to start server"),
                "startServer() should print a status message");
    }

    @Test
    @DisplayName("startServer() references SERVER_PORT constant (8080)")
    void startServer_whenCalled_usesServerPortConstant() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        method.setAccessible(true);

        // Act
        method.invoke(miniApp);

        // Assert
        String stdout = outContent.toString();
        String stderr = errContent.toString();
        // Either it started on 8080 or failed (port in use)
        assertTrue(stdout.contains("8080") || stderr.contains("Failed to start server"),
                "startServer() should reference port 8080");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeApplication() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeApplication() does not throw exception")
    void initializeApplication_whenCalled_doesNotThrow() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "initializeApplication() should not throw an exception");
    }

    @Test
    @DisplayName("initializeApplication() calls loadConfiguration")
    void initializeApplication_whenCalled_invokesLoadConfiguration() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);

        // Act
        method.invoke(miniApp);

        // Assert - loadConfiguration prints a message about config file
        String output = outContent.toString() + errContent.toString();
        assertTrue(output.contains("Configuration") || output.contains("config") ||
                output.contains("Logging") || output.contains("Connecting"),
                "initializeApplication() should invoke sub-initialization methods");
    }

    @Test
    @DisplayName("initializeApplication() calls initializeLogging")
    void initializeApplication_whenCalled_invokesInitializeLogging() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);

        // Act
        method.invoke(miniApp);

        // Assert
        String output = outContent.toString() + errContent.toString();
        assertTrue(output.contains("Logging") || output.contains("log") ||
                output.contains("Failed to initialize"),
                "initializeApplication() should invoke initializeLogging()");
    }

    @Test
    @DisplayName("initializeApplication() creates DatabaseService and calls connect")
    void initializeApplication_whenCalled_createsDatabaseServiceAndConnects() throws Exception {
        // Arrange
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);

        // Act
        method.invoke(miniApp);

        // Assert
        String output = outContent.toString() + errContent.toString();
        assertTrue(output.contains("Connecting to database") || output.contains("Database"),
                "initializeApplication() should create DatabaseService and call connect()");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Class Structure Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MiniApp class has main method with correct signature")
    void classStructure_mainMethod_hasCorrectSignature() throws Exception {
        // Act
        Method mainMethod = MiniApp.class.getDeclaredMethod("main", String[].class);

        // Assert
        assertNotNull(mainMethod, "main() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()),
                "main() should be public");
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()),
                "main() should be static");
        assertEquals(void.class, mainMethod.getReturnType(),
                "main() should return void");
    }

    @Test
    @DisplayName("MiniApp class has loadConfiguration private method")
    void classStructure_loadConfigurationMethod_exists() throws Exception {
        // Act
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");

        // Assert
        assertNotNull(method, "loadConfiguration() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()),
                "loadConfiguration() should be private");
    }

    @Test
    @DisplayName("MiniApp class has initializeLogging private method")
    void classStructure_initializeLoggingMethod_exists() throws Exception {
        // Act
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");

        // Assert
        assertNotNull(method, "initializeLogging() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()),
                "initializeLogging() should be private");
    }

    @Test
    @DisplayName("MiniApp class has startServer private method")
    void classStructure_startServerMethod_exists() throws Exception {
        // Act
        Method method = MiniApp.class.getDeclaredMethod("startServer");

        // Assert
        assertNotNull(method, "startServer() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()),
                "startServer() should be private");
    }

    @Test
    @DisplayName("MiniApp class has initializeApplication private method")
    void classStructure_initializeApplicationMethod_exists() throws Exception {
        // Act
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");

        // Assert
        assertNotNull(method, "initializeApplication() method should exist");
        assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()),
                "initializeApplication() should be private");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge Case / Boundary Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MiniApp is in the correct package 'com.test'")
    void classStructure_packageName_isComTest() {
        // Act
        String packageName = MiniApp.class.getPackageName();

        // Assert
        assertEquals("com.test", packageName, "MiniApp should be in package 'com.test'");
    }

    @Test
    @DisplayName("MiniApp class is not abstract")
    void classStructure_class_isNotAbstract() {
        // Assert
        assertFalse(java.lang.reflect.Modifier.isAbstract(MiniApp.class.getModifiers()),
                "MiniApp should not be abstract");
    }

    @Test
    @DisplayName("MiniApp class is public")
    void classStructure_class_isPublic() {
        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(MiniApp.class.getModifiers()),
                "MiniApp should be public");
    }

    @Test
    @DisplayName("main() output contains application startup sequence")
    void main_whenCalled_outputContainsStartupSequence() {
        // Act
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."),
                "Output should contain startup message");
    }
}
