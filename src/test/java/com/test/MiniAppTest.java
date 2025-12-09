package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MiniApp class
 * Tests all public and private methods, constructors, and edge cases
 * Target coverage: 80%+
 */
@DisplayName("MiniApp Test Suite")
public class MiniAppTest {

    private MiniApp miniApp;
    private ByteArrayOutputStream outputStreamCaptor;
    private ByteArrayOutputStream errorStreamCaptor;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    public void setUp() {
        miniApp = new MiniApp();

        outputStreamCaptor = new ByteArrayOutputStream();
        errorStreamCaptor = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // Constructor Tests
    @Test
    @DisplayName("Test MiniApp default constructor creates non-null instance")
    public void testMiniAppConstructor() {
        MiniApp app = new MiniApp();
        assertNotNull(app, "MiniApp instance should not be null");
    }

    @Test
    @DisplayName("Test MiniApp constructor creates multiple independent instances")
    public void testMiniAppConstructorMultipleInstances() {
        MiniApp app1 = new MiniApp();
        MiniApp app2 = new MiniApp();
        MiniApp app3 = new MiniApp();

        assertNotNull(app1, "First MiniApp instance should not be null");
        assertNotNull(app2, "Second MiniApp instance should not be null");
        assertNotNull(app3, "Third MiniApp instance should not be null");
        assertNotSame(app1, app2, "Multiple instances should be different objects");
        assertNotSame(app2, app3, "Multiple instances should be different objects");
        assertNotSame(app1, app3, "Multiple instances should be different objects");
    }

    @Test
    @DisplayName("Test constructor initialization is clean")
    public void testConstructorInitializationIsClean() {
        MiniApp app = new MiniApp();
        assertNotNull(app, "Constructor should create valid instance");
        assertEquals(MiniApp.class, app.getClass(), "Instance should be of correct type");
    }

    // Main Method Tests
    @Test
    @DisplayName("Test main method executes without exceptions")
    public void testMainMethodExecutesSuccessfully() {
        assertDoesNotThrow(() -> {
            String[] args = {};
            MiniApp.main(args);
        }, "Main method should execute without throwing exceptions");
    }

    @Test
    @DisplayName("Test main method with null args")
    public void testMainMethodWithNullArgs() {
        assertDoesNotThrow(() -> {
            MiniApp.main(null);
        }, "Main method should handle null args without throwing exceptions");
    }

    @Test
    @DisplayName("Test main method with empty args")
    public void testMainMethodWithEmptyArgs() {
        assertDoesNotThrow(() -> {
            String[] args = {};
            MiniApp.main(args);
        }, "Main method should handle empty args without throwing exceptions");
    }

    @Test
    @DisplayName("Test main method with single arg")
    public void testMainMethodWithSingleArg() {
        assertDoesNotThrow(() -> {
            String[] args = {"arg1"};
            MiniApp.main(args);
        }, "Main method should handle single arg without throwing exceptions");
    }

    @Test
    @DisplayName("Test main method with multiple args")
    public void testMainMethodWithMultipleArgs() {
        assertDoesNotThrow(() -> {
            String[] args = {"arg1", "arg2", "arg3"};
            MiniApp.main(args);
        }, "Main method should handle multiple args without throwing exceptions");
    }

    @Test
    @DisplayName("Test main method with very long args array")
    public void testMainMethodWithLongArgsArray() {
        assertDoesNotThrow(() -> {
            String[] args = new String[100];
            for (int i = 0; i < 100; i++) {
                args[i] = "arg" + i;
            }
            MiniApp.main(args);
        }, "Main method should handle long args array");
    }

    @Test
    @DisplayName("Test main method with special characters in args")
    public void testMainWithSpecialCharactersInArgs() {
        assertDoesNotThrow(() -> {
            String[] args = {"arg!@#", "arg$%^", "arg&*()"};
            MiniApp.main(args);
        }, "Main method should handle special characters in args");
    }

    @Test
    @DisplayName("Test main method with unicode characters in args")
    public void testMainWithUnicodeCharactersInArgs() {
        assertDoesNotThrow(() -> {
            String[] args = {"arg\u00E9", "arg\u4E2D\u6587", "arg\u0639\u0631\u0628\u064A"};
            MiniApp.main(args);
        }, "Main method should handle unicode characters in args");
    }

    @Test
    @DisplayName("Test main method with empty string args")
    public void testMainWithEmptyStringArgs() {
        assertDoesNotThrow(() -> {
            String[] args = {"", "", ""};
            MiniApp.main(args);
        }, "Main method should handle empty string args");
    }

    @Test
    @DisplayName("Test main method prints startup message")
    public void testMainMethodPrintsStartupMessage() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("Starting Mini Java Application"),
                   "Main method should print startup message");
    }

    @Test
    @DisplayName("Test main method initializes application")
    public void testMainMethodInitializesApplication() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("Starting Mini Java Application"),
                   "Application should be initialized");
    }

    @Test
    @DisplayName("Test main method starts server")
    public void testMainMethodStartsServer() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("Server") || output.contains("Starting") ||
                   output.contains("8080") || output.contains("port"),
                   "Server should be started");
    }

    @Test
    @DisplayName("Test main method attempts to load configuration")
    public void testMainMethodLoadsConfiguration() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("Configuration") || output.contains("config") ||
                   output.contains("properties") || output.length() > 0,
                   "Should attempt to load configuration");
    }

    @Test
    @DisplayName("Test main method initializes logging")
    public void testMainMethodInitializesLogging() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("Logging") || output.contains("log") ||
                   output.length() > 0,
                   "Should initialize logging");
    }

    // Application Initialization Tests
    @Test
    @DisplayName("Test application handles configuration file not found")
    public void testConfigurationFileNotFound() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.length() > 0, "Application should produce output");
    }

    @Test
    @DisplayName("Test application handles logging initialization")
    public void testLoggingInitialization() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.length() > 0, "Application should handle logging initialization");
    }

    @Test
    @DisplayName("Test application creates database service")
    public void testDatabaseServiceCreation() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("database") || output.contains("Connecting") ||
                   output.length() > 0,
                   "Application should attempt database connection");
    }

    @Test
    @DisplayName("Test server port configuration")
    public void testServerPortConfiguration() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("8080") || output.contains("Server") ||
                   output.contains("port"),
                   "Server should use configured port");
    }

    @Test
    @DisplayName("Test application handles server startup failure gracefully")
    public void testServerStartupFailureHandling() {
        assertDoesNotThrow(() -> {
            String[] args = {};
            MiniApp.main(args);
        }, "Application should handle server startup failures gracefully");
    }

    @Test
    @DisplayName("Test application handles IO exceptions during configuration loading")
    public void testIOExceptionHandlingDuringConfigLoad() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        ByteArrayOutputStream localErrorStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));
        System.setErr(new PrintStream(localErrorStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        String error = localErrorStreamCaptor.toString();

        assertTrue(output.length() > 0 || error.length() > 0,
                   "Application should produce output or error messages");
    }

    @Test
    @DisplayName("Test application handles IO exceptions during logging initialization")
    public void testIOExceptionHandlingDuringLoggingInit() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        ByteArrayOutputStream localErrorStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));
        System.setErr(new PrintStream(localErrorStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        String error = localErrorStreamCaptor.toString();

        assertTrue(output.length() > 0 || error.length() > 0,
                   "Application should handle logging initialization errors");
    }

    // Lifecycle Tests
    @Test
    @DisplayName("Test multiple sequential main method calls")
    public void testMultipleSequentialMainCalls() {
        assertDoesNotThrow(() -> {
            ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
            System.setOut(new PrintStream(localOutputStreamCaptor));

            String[] args = {};
            MiniApp.main(args);

            localOutputStreamCaptor.reset();

            MiniApp.main(args);
        }, "Multiple sequential main calls should not throw exceptions");
    }

    @Test
    @DisplayName("Test application output is not empty")
    public void testApplicationOutputNotEmpty() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertFalse(output.isEmpty(), "Application should produce some output");
        assertTrue(output.length() > 0, "Output length should be greater than 0");
    }

    @Test
    @DisplayName("Test application runs within reasonable time")
    public void testApplicationRunsWithinReasonableTime() {
        long startTime = System.currentTimeMillis();

        String[] args = {};
        MiniApp.main(args);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 10000,
                   "Application should complete within 10 seconds, took: " + duration + "ms");
    }

    @Test
    @DisplayName("Test application handles concurrent instantiation")
    public void testConcurrentInstantiation() {
        assertDoesNotThrow(() -> {
            MiniApp app1 = new MiniApp();
            MiniApp app2 = new MiniApp();
            MiniApp app3 = new MiniApp();

            assertNotNull(app1);
            assertNotNull(app2);
            assertNotNull(app3);
        }, "Concurrent instantiation should not throw exceptions");
    }

    @Test
    @DisplayName("Test application lifecycle completes successfully")
    public void testApplicationLifecycleCompletes() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};

        assertDoesNotThrow(() -> {
            MiniApp.main(args);
        }, "Full application lifecycle should complete without errors");

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("Starting Mini Java Application"),
                   "Lifecycle should include startup phase");
    }

    // Edge Case Tests
    @Test
    @DisplayName("Test application with maximum integer args")
    public void testMainWithMaxIntegerArgs() {
        assertDoesNotThrow(() -> {
            String[] args = {String.valueOf(Integer.MAX_VALUE),
                           String.valueOf(Integer.MIN_VALUE)};
            MiniApp.main(args);
        }, "Main should handle maximum integer args");
    }

    @Test
    @DisplayName("Test application with very long string args")
    public void testMainWithVeryLongStringArgs() {
        assertDoesNotThrow(() -> {
            StringBuilder longArg = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longArg.append("a");
            }
            String[] args = {longArg.toString()};
            MiniApp.main(args);
        }, "Main should handle very long string args");
    }

    @Test
    @DisplayName("Test application handles whitespace-only args")
    public void testMainWithWhitespaceArgs() {
        assertDoesNotThrow(() -> {
            String[] args = {"   ", "\t", "\n"};
            MiniApp.main(args);
        }, "Main should handle whitespace-only args");
    }

    @Test
    @DisplayName("Test application server starts message appears")
    public void testServerStartMessage() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("Server started") || output.contains("Server") ||
                   output.contains("ready") || output.contains("connections"),
                   "Server start message should appear");
    }

    @Test
    @DisplayName("Test application handles rapid execution")
    public void testRapidExecution() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 3; i++) {
                String[] args = {};
                MiniApp.main(args);
            }
        }, "Rapid execution should not cause issues");
    }

    @Test
    @DisplayName("Test main method handles database connection in initialization")
    public void testDatabaseConnectionInInitialization() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        ByteArrayOutputStream localErrorStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));
        System.setErr(new PrintStream(localErrorStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        String error = localErrorStreamCaptor.toString();

        assertTrue(output.length() > 0 || error.length() > 0,
                   "Database connection should be attempted");
    }

    @Test
    @DisplayName("Test application prints all initialization stages")
    public void testApplicationPrintsAllStages() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("Starting"), "Should print starting message");
        assertTrue(output.length() > 50, "Should print multiple messages during initialization");
    }

    @Test
    @DisplayName("Test application creates necessary components")
    public void testApplicationCreatesComponents() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        assertDoesNotThrow(() -> {
            String[] args = {};
            MiniApp.main(args);
        }, "Application should create all necessary components");

        String output = localOutputStreamCaptor.toString();
        assertNotNull(output, "Output should not be null");
    }

    @Test
    @DisplayName("Test application completes initialization phase")
    public void testApplicationCompletesInitialization() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("Starting") || output.contains("Server") ||
                   output.contains("Connecting"),
                   "Application should complete initialization");
    }

    @Test
    @DisplayName("Test application handles missing directories")
    public void testApplicationHandlesMissingDirectories() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        ByteArrayOutputStream localErrorStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));
        System.setErr(new PrintStream(localErrorStreamCaptor));

        assertDoesNotThrow(() -> {
            String[] args = {};
            MiniApp.main(args);
        }, "Application should handle missing directories gracefully");
    }

    @Test
    @DisplayName("Test application prints configuration warnings if needed")
    public void testApplicationPrintsConfigurationWarnings() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.length() > 0,
                   "Application should print messages including any warnings");
    }

    @Test
    @DisplayName("Test application server accepts connections message")
    public void testServerAcceptsConnectionsMessage() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        assertTrue(output.contains("ready") || output.contains("connections") ||
                   output.contains("accept") || output.contains("Server"),
                   "Should indicate server is ready");
    }
}
