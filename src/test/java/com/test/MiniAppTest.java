package com.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 tests for MiniApp class.
 * Tests cover: constructor, main(), initializeApplication(), loadConfiguration(),
 * initializeLogging(), startServer(), and all static constant fields.
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
        outContent.reset();
        errContent.reset();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Default constructor creates a non-null MiniApp instance")
    void constructor_defaultConstructor_createsNonNullInstance() {
        MiniApp app = new MiniApp();
        assertNotNull(app, "MiniApp instance should not be null");
    }

    @Test
    @DisplayName("Two separate MiniApp instances are independent objects")
    void constructor_twoInstances_areIndependent() {
        MiniApp app1 = new MiniApp();
        MiniApp app2 = new MiniApp();
        assertNotSame(app1, app2, "Two MiniApp instances should be different objects");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static Constant Field Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SERVER_PORT constant equals 8080")
    void staticField_serverPort_equals8080() throws Exception {
        Field field = MiniApp.class.getDeclaredField("SERVER_PORT");
        field.setAccessible(true);
        assertEquals(8080, field.get(null), "SERVER_PORT should be 8080");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant is not null and not empty")
    void staticField_configFilePath_isNotNullOrEmpty() throws Exception {
        Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        field.setAccessible(true);
        String value = (String) field.get(null);
        assertNotNull(value, "CONFIG_FILE_PATH should not be null");
        assertFalse(value.isEmpty(), "CONFIG_FILE_PATH should not be empty");
    }

    @Test
    @DisplayName("CONFIG_FILE_PATH constant contains expected path segment")
    void staticField_configFilePath_containsExpectedPath() throws Exception {
        Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
        field.setAccessible(true);
        String value = (String) field.get(null);
        assertTrue(value.contains("app.properties"),
                "CONFIG_FILE_PATH should reference app.properties");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant is not null and not empty")
    void staticField_logFilePath_isNotNullOrEmpty() throws Exception {
        Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        field.setAccessible(true);
        String value = (String) field.get(null);
        assertNotNull(value, "LOG_FILE_PATH should not be null");
        assertFalse(value.isEmpty(), "LOG_FILE_PATH should not be empty");
    }

    @Test
    @DisplayName("LOG_FILE_PATH constant contains '.log' extension")
    void staticField_logFilePath_containsLogExtension() throws Exception {
        Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
        field.setAccessible(true);
        String value = (String) field.get(null);
        assertTrue(value.endsWith(".log"), "LOG_FILE_PATH should end with .log");
    }

    @Test
    @DisplayName("SERVER_PORT constant is a positive integer")
    void staticField_serverPort_isPositive() throws Exception {
        Field field = MiniApp.class.getDeclaredField("SERVER_PORT");
        field.setAccessible(true);
        int port = (int) field.get(null);
        assertTrue(port > 0, "SERVER_PORT should be a positive integer");
    }

    @Test
    @DisplayName("SERVER_PORT constant is within valid port range (1-65535)")
    void staticField_serverPort_isWithinValidRange() throws Exception {
        Field field = MiniApp.class.getDeclaredField("SERVER_PORT");
        field.setAccessible(true);
        int port = (int) field.get(null);
        assertTrue(port >= 1 && port <= 65535,
                "SERVER_PORT should be within valid port range 1-65535");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // main() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("main() with empty args array does not throw exception")
    void main_withEmptyArgs_doesNotThrow() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}),
                "main() should not throw with empty args");
    }

    @Test
    @DisplayName("main() with null args does not throw exception")
    void main_withNullArgs_doesNotThrow() {
        assertDoesNotThrow(() -> MiniApp.main(null),
                "main() should not throw with null args");
    }

    @Test
    @DisplayName("main() with non-empty args does not throw exception")
    void main_withNonEmptyArgs_doesNotThrow() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"--debug", "--port=9090"}),
                "main() should not throw with non-empty args");
    }

    @Test
    @DisplayName("main() prints 'Starting Mini Java Application...' message")
    void main_printsStartingMessage() {
        // Act
        MiniApp.main(new String[]{});

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application..."),
                "main() should print starting message");
    }

    @Test
    @DisplayName("main() is a static method")
    void main_isStaticMethod() throws NoSuchMethodException {
        Method method = MiniApp.class.getMethod("main", String[].class);
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "main() should be a static method");
    }

    @Test
    @DisplayName("main() is a public method")
    void main_isPublicMethod() throws NoSuchMethodException {
        Method method = MiniApp.class.getMethod("main", String[].class);
        assertTrue(Modifier.isPublic(method.getModifiers()),
                "main() should be a public method");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeApplication() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeApplication() can be invoked via reflection without throwing")
    void initializeApplication_viaReflection_doesNotThrow() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "initializeApplication() should not propagate exceptions");
    }

    @Test
    @DisplayName("initializeApplication() triggers loadConfiguration() output")
    void initializeApplication_triggersLoadConfiguration() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        method.setAccessible(true);
        method.invoke(miniApp);

        String output = outContent.toString() + errContent.toString();
        // Either config loaded or warning printed
        assertTrue(
                output.contains("Configuration loaded") || output.contains("Warning: Configuration file not found"),
                "initializeApplication() should trigger configuration loading output"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // loadConfiguration() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loadConfiguration() does not throw exception when config file is missing")
    void loadConfiguration_whenConfigFileMissing_doesNotThrow() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "loadConfiguration() should handle missing config file gracefully");
    }

    @Test
    @DisplayName("loadConfiguration() prints warning when config file does not exist")
    void loadConfiguration_whenConfigFileMissing_printsWarning() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        method.setAccessible(true);
        method.invoke(miniApp);

        String output = outContent.toString();
        // In test environment /opt/app/config/app.properties likely doesn't exist
        assertTrue(
                output.contains("Warning: Configuration file not found") ||
                output.contains("Configuration loaded"),
                "loadConfiguration() should print either a warning or success message"
        );
    }

    @Test
    @DisplayName("loadConfiguration() can be called multiple times without throwing")
    void loadConfiguration_calledMultipleTimes_doesNotThrow() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        method.setAccessible(true);
        assertDoesNotThrow(() -> {
            method.invoke(miniApp);
            method.invoke(miniApp);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initializeLogging() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initializeLogging() does not throw exception")
    void initializeLogging_doesNotThrow() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "initializeLogging() should not propagate exceptions");
    }

    @Test
    @DisplayName("initializeLogging() prints logging initialization message or error")
    void initializeLogging_printsMessage() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        method.setAccessible(true);
        method.invoke(miniApp);

        String combined = outContent.toString() + errContent.toString();
        assertTrue(
                combined.contains("Logging initialized") || combined.contains("Failed to initialize logging"),
                "initializeLogging() should print a status message"
        );
    }

    @Test
    @DisplayName("initializeLogging() can be called multiple times without throwing")
    void initializeLogging_calledMultipleTimes_doesNotThrow() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        method.setAccessible(true);
        assertDoesNotThrow(() -> {
            method.invoke(miniApp);
            method.invoke(miniApp);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // startServer() Tests (via reflection)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startServer() does not throw exception")
    void startServer_doesNotThrow() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(miniApp),
                "startServer() should not propagate exceptions");
    }

    @Test
    @DisplayName("startServer() prints server started message or error")
    void startServer_printsMessage() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        method.setAccessible(true);
        method.invoke(miniApp);

        String combined = outContent.toString() + errContent.toString();
        assertTrue(
                combined.contains("Server started on port") || combined.contains("Failed to start server"),
                "startServer() should print a status message"
        );
    }

    @Test
    @DisplayName("startServer() can be called multiple times without throwing")
    void startServer_calledMultipleTimes_doesNotThrow() throws Exception {
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        method.setAccessible(true);
        assertDoesNotThrow(() -> {
            method.invoke(miniApp);
            method.invoke(miniApp);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Class Structure Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MiniApp is a concrete (non-abstract) class")
    void classStructure_isConcreteClass() {
        assertFalse(Modifier.isAbstract(MiniApp.class.getModifiers()),
                "MiniApp should be a concrete class");
    }

    @Test
    @DisplayName("MiniApp has public static main(String[]) method")
    void classStructure_hasPublicStaticMainMethod() throws NoSuchMethodException {
        Method method = MiniApp.class.getMethod("main", String[].class);
        assertNotNull(method);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    @Test
    @DisplayName("MiniApp has private initializeApplication() method")
    void classStructure_hasPrivateInitializeApplicationMethod() throws NoSuchMethodException {
        Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
        assertNotNull(method);
        assertTrue(Modifier.isPrivate(method.getModifiers()));
    }

    @Test
    @DisplayName("MiniApp has private loadConfiguration() method")
    void classStructure_hasPrivateLoadConfigurationMethod() throws NoSuchMethodException {
        Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
        assertNotNull(method);
        assertTrue(Modifier.isPrivate(method.getModifiers()));
    }

    @Test
    @DisplayName("MiniApp has private initializeLogging() method")
    void classStructure_hasPrivateInitializeLoggingMethod() throws NoSuchMethodException {
        Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
        assertNotNull(method);
        assertTrue(Modifier.isPrivate(method.getModifiers()));
    }

    @Test
    @DisplayName("MiniApp has private startServer() method")
    void classStructure_hasPrivateStartServerMethod() throws NoSuchMethodException {
        Method method = MiniApp.class.getDeclaredMethod("startServer");
        assertNotNull(method);
        assertTrue(Modifier.isPrivate(method.getModifiers()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integration / Full Lifecycle Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Full application lifecycle via main() completes without exception")
    void lifecycle_fullApplicationViaMain_completesWithoutException() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}),
                "Full application lifecycle should complete without exception");
    }

    @Test
    @DisplayName("main() output contains 'Starting Mini Java Application...'")
    void main_outputContainsStartingMessage() {
        MiniApp.main(new String[]{});
        assertTrue(outContent.toString().contains("Starting Mini Java Application..."));
    }

    @Test
    @DisplayName("New MiniApp instance is of type MiniApp")
    void constructor_instanceIsOfTypeMiniApp() {
        MiniApp app = new MiniApp();
        assertInstanceOf(MiniApp.class, app);
    }
}
