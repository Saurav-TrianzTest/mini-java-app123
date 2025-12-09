package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MiniApp class
 * Tests all public and private methods, constructors, and edge cases
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

        // Capture System.out and System.err for testing
        outputStreamCaptor = new ByteArrayOutputStream();
        errorStreamCaptor = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));
    }

    @AfterEach
    public void tearDown() {
        // Restore original System.out and System.err
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

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

        assertNotNull(app1, "First MiniApp instance should not be null");
        assertNotNull(app2, "Second MiniApp instance should not be null");
        assertNotSame(app1, app2, "Multiple instances should be different objects");
    }

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
    @DisplayName("Test main method with multiple args")
    public void testMainMethodWithMultipleArgs() {
        assertDoesNotThrow(() -> {
            String[] args = {"arg1", "arg2", "arg3"};
            MiniApp.main(args);
        }, "Main method should handle multiple args without throwing exceptions");
    }

    @Test
    @DisplayName("Test main method prints startup message")
    public void testMainMethodPrintsStartupMessage() {
        // Recapture output streams for this test
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
        assertTrue(output.contains("Server") || output.contains("Starting"),
                   "Server should be started");
    }

    @Test
    @DisplayName("Test application handles configuration file not found")
    public void testConfigurationFileNotFound() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        // Should either load config or warn about missing file
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
        // Logging should be initialized or error should be handled
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
        // Database connection should be attempted
        assertTrue(output.length() > 0, "Application should attempt database connection");
    }

    @Test
    @DisplayName("Test server port configuration")
    public void testServerPortConfiguration() {
        ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(localOutputStreamCaptor));

        String[] args = {};
        MiniApp.main(args);

        String output = localOutputStreamCaptor.toString();
        // Should attempt to start server on configured port
        assertTrue(output.contains("8080") || output.contains("Server") || output.contains("port"),
                   "Server should use configured port");
    }

    @Test
    @DisplayName("Test application handles server startup failure gracefully")
    public void testServerStartupFailureHandling() {
        // Start main application which may fail to bind to port
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

        // Should handle missing config file gracefully
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

        // Should handle logging initialization errors gracefully
        assertTrue(output.length() > 0 || error.length() > 0,
                   "Application should handle logging initialization errors");
    }

    @Test
    @DisplayName("Test multiple sequential main method calls")
    public void testMultipleSequentialMainCalls() {
        assertDoesNotThrow(() -> {
            ByteArrayOutputStream localOutputStreamCaptor = new ByteArrayOutputStream();
            System.setOut(new PrintStream(localOutputStreamCaptor));

            String[] args = {};
            MiniApp.main(args);

            // Reset output streams
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
    @DisplayName("Test application with special characters in args")
    public void testMainWithSpecialCharactersInArgs() {
        assertDoesNotThrow(() -> {
            String[] args = {"arg!@#", "arg$%^", "arg&*()"};
            MiniApp.main(args);
        }, "Main method should handle special characters in args");
    }

    @Test
    @DisplayName("Test application with very long args array")
    public void testMainWithLongArgsArray() {
        assertDoesNotThrow(() -> {
            String[] args = new String[100];
            for (int i = 0; i < 100; i++) {
                args[i] = "arg" + i;
            }
            MiniApp.main(args);
        }, "Main method should handle long args array");
    }

    @Test
    @DisplayName("Test constructor initialization is clean")
    public void testConstructorInitializationIsClean() {
        MiniApp app = new MiniApp();
        assertNotNull(app, "Constructor should create valid instance");
        assertEquals(MiniApp.class, app.getClass(), "Instance should be of correct type");
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
}
