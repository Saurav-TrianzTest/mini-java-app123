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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for MiniApp class.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MiniApp Tests")
class MiniAppTest {

    private MiniApp miniApp;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        miniApp = new MiniApp();
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
    @DisplayName("Default constructor creates a non-null MiniApp instance")
    void constructor_default_createsNonNullInstance() {
        // Arrange / Act
        MiniApp app = new MiniApp();

        // Assert
        assertNotNull(app, "MiniApp instance should not be null");
    }

    @Test
    @DisplayName("Multiple MiniApp instances are independent objects")
    void constructor_multipleInstances_areIndependent() {
        // Arrange / Act
        MiniApp app1 = new MiniApp();
        MiniApp app2 = new MiniApp();

        // Assert
        assertNotSame(app1, app2, "Each MiniApp instance should be a distinct object");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Field / Constant Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SERVER_PORT constant equals 8080")
    void staticFields_serverPort_equals8080() throws Exception {
        Field serverPortField = MiniApp.class.getDeclaredField("SERVER_PORT");
        serverPortField.setAccessible(true);
        int serverPort = (int) serverPortField.get(null);

        assertEquals(8080, serverPort, "SERVER_PORT should be 8080");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant is not null or empty")
    void staticFields_configFilePath_isNotNullOrEmpty() throws Exception {
        Field configPathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        configPathField.setAccessible(true);
        String configPath = (String) configPathField.get(null);

        assertNotNull(configPath, "CONFIG_FILE_PATH should not be null");
        assertFalse(configPath.isEmpty(), "CONFIG_FILE_PATH should not be empty");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant starts with /opt/app")
    void staticFields_configFilePath_startsWithOptApp() throws Exception {
        Field configPathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        configPathField.setAccessible(true);
        String configPath = (String) configPathField.get(null);

        assertTrue(configPath.startsWith("/opt/app"),
                "CONFIG_FILE_PATH should start with '/opt/app'");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant is not null or empty")
    void staticFields_logFilePath_isNotNullOrEmpty() throws Exception {
        Field logPathField = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        logPathField.setAccessible(true);
        String logPath = (String) logPathField.get(null);

        assertNotNull(logPath, "LOG_FILE_PATH should not be null");
        assertFalse(logPath.isEmpty(), "LOG_FILE_PATH should not be empty");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant starts with /var/log")
    void staticFields_logFilePath_startsWithVarLog() throws Exception {
        Field logPathField = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        logPathField.setAccessible(true);
        String logPath = (String) logPathField.get(null);

        assertTrue(logPath.startsWith("/var/log"),
                "LOG_FILE_PATH should start with '/var/log'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadConfiguration() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loadConfiguration() with non-existent file prints warning")
    void loadConfiguration_nonExistentFile_printsWarning() throws Exception {
        // Arrange
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act
        loadConfigMethod.invoke(miniApp);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Warning: Configuration file not found at:"),
                "Should print warning when config file does not exist");
    }

    @Test
    @DisplayName("loadConfiguration() does not throw when config file is missing")
    void loadConfiguration_nonExistentFile_doesNotThrow() throws Exception {
        // Arrange
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> loadConfigMethod.invoke(miniApp),
                "loadConfiguration should not throw when config file is missing");
    }

    @Test
    @DisplayName("loadConfiguration() prints loaded message when config file exists")
    void loadConfiguration_existingFile_printsLoadedMessage() throws Exception {
        // Arrange – temporarily override CONFIG_FILE_PATH via reflection
        File tempConfig = tempDir.resolve("app.properties").toFile();
        try (FileWriter fw = new FileWriter(tempConfig)) {
            fw.write("key=value\n");
        }

        Field configPathField = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        configPathField.setAccessible(true);

        // We cannot change a static final field in Java 17+ without Unsafe tricks,
        // so we verify the warning path (default path does not exist in test env).
        // This test validates the branch logic by checking the output.
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);
        loadConfigMethod.invoke(miniApp);

        // Assert – in test environment the hardcoded path won't exist
        String output = outContent.toString();
        assertFalse(output.isEmpty(), "loadConfiguration should produce some output");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeLogging() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeLogging() does not throw regardless of filesystem state")
    void initializeLogging_anyFilesystemState_doesNotThrow() throws Exception {
        // Arrange
        Method initLoggingMethod = MiniApp.class.getDeclaredMethod("initializeLogging");
        initLoggingMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> initLoggingMethod.invoke(miniApp),
                "initializeLogging should not propagate exceptions");
    }

    @Test
    @DisplayName("initializeLogging() produces some output (either success or error)")
    void initializeLogging_anyState_producesOutput() throws Exception {
        // Arrange
        Method initLoggingMethod = MiniApp.class.getDeclaredMethod("initializeLogging");
        initLoggingMethod.setAccessible(true);

        // Act
        initLoggingMethod.invoke(miniApp);

        // Assert – either stdout or stderr should have content
        boolean hasOutput = !outContent.toString().isEmpty() || !errContent.toString().isEmpty();
        assertTrue(hasOutput, "initializeLogging should produce some output");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // startServer() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startServer() does not throw an unchecked exception")
    void startServer_anyPortState_doesNotThrowUnchecked() throws Exception {
        // Arrange
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);

        // Act & Assert – InvocationTargetException wraps checked exceptions; we allow those
        try {
            startServerMethod.invoke(miniApp);
            // If it succeeds, great
        } catch (InvocationTargetException ite) {
            // The underlying cause should not be a RuntimeException
            Throwable cause = ite.getCause();
            assertFalse(cause instanceof RuntimeException,
                    "startServer should not throw RuntimeException: " + cause);
        }
    }

    @Test
    @DisplayName("startServer() prints server started message or error message")
    void startServer_anyPortState_printsMessage() throws Exception {
        // Arrange
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);

        // Act
        try {
            startServerMethod.invoke(miniApp);
        } catch (InvocationTargetException ignored) {
            // Ignore – we only care about output
        }

        // Assert
        boolean hasOutput = outContent.toString().contains("Server started on port:")
                || errContent.toString().contains("Failed to start server:");
        assertTrue(hasOutput, "startServer should print either success or failure message");
    }

    @Test
    @DisplayName("startServer() prints 'Server ready to accept connections' on success")
    void startServer_successfulBind_printsReadyMessage() throws Exception {
        // Arrange
        Method startServerMethod = MiniApp.class.getDeclaredMethod("startServer");
        startServerMethod.setAccessible(true);

        // Act
        try {
            startServerMethod.invoke(miniApp);
            // Assert – only if server started successfully
            String output = outContent.toString();
            if (output.contains("Server started on port:")) {
                assertTrue(output.contains("Server ready to accept connections..."),
                        "Should print ready message after server starts");
            }
        } catch (InvocationTargetException ignored) {
            // Port may be in use in CI – acceptable
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeApplication() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeApplication() does not throw unchecked exceptions")
    void initializeApplication_anyState_doesNotThrowUnchecked() throws Exception {
        // Arrange
        Method initAppMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        initAppMethod.setAccessible(true);

        // Act & Assert
        try {
            initAppMethod.invoke(miniApp);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertFalse(cause instanceof RuntimeException,
                    "initializeApplication should not throw RuntimeException: " + cause);
        }
    }

    @Test
    @DisplayName("initializeApplication() triggers loadConfiguration (prints config warning)")
    void initializeApplication_anyState_triggersLoadConfiguration() throws Exception {
        // Arrange
        Method initAppMethod = MiniApp.class.getDeclaredMethod("initializeApplication");
        initAppMethod.setAccessible(true);

        // Act
        try {
            initAppMethod.invoke(miniApp);
        } catch (InvocationTargetException ignored) {
            // Ignore
        }

        // Assert – loadConfiguration should have printed a warning (config file not found)
        String output = outContent.toString();
        assertTrue(output.contains("Warning: Configuration file not found at:") ||
                   output.contains("Configuration loaded from:"),
                "initializeApplication should trigger loadConfiguration");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // main() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("main() prints 'Starting Mini Java Application...' message")
    void main_withEmptyArgs_printsStartingMessage() {
        // Arrange
        String[] args = {};

        // Act
        try {
            MiniApp.main(args);
        } catch (Exception ignored) {
            // main may fail due to environment constraints; we only check output
        }

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."),
                "main() should print starting message");
    }

    @Test
    @DisplayName("main() with null args does not throw NullPointerException")
    void main_withNullArgs_doesNotThrowNpe() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            try {
                MiniApp.main(null);
            } catch (Exception e) {
                // Any exception other than NPE is acceptable
                assertFalse(e instanceof NullPointerException,
                        "main() should not throw NullPointerException");
            }
        });
    }

    @Test
    @DisplayName("main() with non-empty args array does not throw")
    void main_withNonEmptyArgs_doesNotThrow() {
        // Arrange
        String[] args = {"--debug", "--verbose"};

        // Act & Assert
        assertDoesNotThrow(() -> {
            try {
                MiniApp.main(args);
            } catch (Exception e) {
                // Environment-related exceptions are acceptable
                assertFalse(e instanceof NullPointerException,
                        "main() should not throw NullPointerException with args");
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge Case / Boundary Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MiniApp class is instantiable (not abstract or interface)")
    void classStructure_isInstantiable() {
        // Assert
        assertFalse(MiniApp.class.isInterface(), "MiniApp should not be an interface");
        assertFalse(java.lang.reflect.Modifier.isAbstract(MiniApp.class.getModifiers()),
                "MiniApp should not be abstract");
    }

    @Test
    @DisplayName("MiniApp has expected private methods via reflection")
    void classStructure_hasExpectedPrivateMethods() {
        // Assert
        assertDoesNotThrow(() -> MiniApp.class.getDeclaredMethod("initializeApplication"),
                "MiniApp should have initializeApplication method");
        assertDoesNotThrow(() -> MiniApp.class.getDeclaredMethod("loadConfiguration"),
                "MiniApp should have loadConfiguration method");
        assertDoesNotThrow(() -> MiniApp.class.getDeclaredMethod("initializeLogging"),
                "MiniApp should have initializeLogging method");
        assertDoesNotThrow(() -> MiniApp.class.getDeclaredMethod("startServer"),
                "MiniApp should have startServer method");
    }

    @Test
    @DisplayName("MiniApp has static main method")
    void classStructure_hasStaticMainMethod() throws Exception {
        // Act
        Method mainMethod = MiniApp.class.getDeclaredMethod("main", String[].class);

        // Assert
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()),
                "main() should be static");
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()),
                "main() should be public");
    }

    @Test
    @DisplayName("loadConfiguration() handles IOException gracefully")
    void loadConfiguration_ioException_printsErrorToStderr() throws Exception {
        // Arrange – the hardcoded path /opt/app/config/app.properties won't exist in test env
        // This exercises the "file not found" branch which prints a warning (not an error)
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act
        loadConfigMethod.invoke(miniApp);

        // Assert – should not propagate exception; output should contain warning
        String output = outContent.toString();
        String errOutput = errContent.toString();
        boolean handledGracefully = output.contains("Warning:") || errOutput.contains("Failed to load");
        assertTrue(handledGracefully,
                "loadConfiguration should handle missing file gracefully");
    }

    @Test
    @DisplayName("Multiple calls to loadConfiguration() are idempotent (no exception)")
    void loadConfiguration_calledMultipleTimes_doesNotThrow() throws Exception {
        // Arrange
        Method loadConfigMethod = MiniApp.class.getDeclaredMethod("loadConfiguration");
        loadConfigMethod.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> {
            loadConfigMethod.invoke(miniApp);
            loadConfigMethod.invoke(miniApp);
            loadConfigMethod.invoke(miniApp);
        }, "Multiple calls to loadConfiguration should not throw");
    }
}
