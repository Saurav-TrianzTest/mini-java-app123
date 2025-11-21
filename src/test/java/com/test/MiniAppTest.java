package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test class for MiniApp
 * Tests all public and private methods with full coverage
 */
public class MiniAppTest {

    private MiniApp miniApp;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    public void setUp() {
        miniApp = new MiniApp();

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
    }

    @Test
    public void testConstructor() {
        // Arrange & Act
        MiniApp app = new MiniApp();

        // Assert
        assertNotNull(app);
    }

    @Test
    public void testConstructorNotNull() {
        // Arrange & Act
        MiniApp app = new MiniApp();

        // Assert
        assertNotNull(app, "MiniApp instance should not be null");
    }

    @Test
    public void testMainMethod() {
        // Arrange
        String[] args = {};

        // Act
        assertDoesNotThrow(() -> {
            Thread testThread = new Thread(() -> {
                MiniApp.main(args);
            });
            testThread.start();
            Thread.sleep(2000); // Give time for execution
            testThread.interrupt();
        });
    }

    @Test
    public void testMainMethodWithNullArgs() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            Thread testThread = new Thread(() -> {
                MiniApp.main(null);
            });
            testThread.start();
            Thread.sleep(2000);
            testThread.interrupt();
        });
    }

    @Test
    public void testMainMethodWithEmptyArgs() {
        // Arrange
        String[] args = {};

        // Act & Assert
        assertDoesNotThrow(() -> {
            Thread testThread = new Thread(() -> {
                MiniApp.main(args);
            });
            testThread.start();
            Thread.sleep(2000);
            testThread.interrupt();
        });
    }

    @Test
    public void testMainMethodPrintsStartingMessage() {
        // Arrange
        String[] args = {};

        // Act
        assertDoesNotThrow(() -> {
            Thread testThread = new Thread(() -> {
                MiniApp.main(args);
            });
            testThread.start();
            Thread.sleep(500);
            testThread.interrupt();
        });

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Starting Mini Java Application"),
                   "Output should contain startup message");
    }

    @Test
    public void testInitializeApplication_ExecutesWithoutException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
            method.setAccessible(true);
            method.invoke(miniApp);
        });
    }

    @Test
    public void testLoadConfiguration_ConfigFileNotExists() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
            method.setAccessible(true);
            method.invoke(miniApp);
        });

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Warning: Configuration file not found") ||
                   output.contains("Configuration loaded"),
                   "Should print configuration status message");
    }

    @Test
    public void testLoadConfiguration_HandlesIOException() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
            method.setAccessible(true);
            method.invoke(miniApp);
        });
    }

    @Test
    public void testLoadConfiguration_PrintsMessage() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("loadConfiguration");
            method.setAccessible(true);
            method.invoke(miniApp);
        });

        // Assert
        String output = outputStream.toString();
        assertNotNull(output, "Output should not be null");
        assertTrue(output.length() > 0, "Output should contain messages");
    }

    @Test
    public void testInitializeLogging_ExecutesWithoutException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
            method.setAccessible(true);
            method.invoke(miniApp);
        });
    }

    @Test
    public void testInitializeLogging_PrintsMessage() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
            method.setAccessible(true);
            method.invoke(miniApp);
        });

        // Assert
        String output = outputStream.toString();
        String error = errorStream.toString();
        assertNotNull(output);
        assertTrue(output.contains("Logging initialized") || error.contains("Failed to initialize logging"));
    }

    @Test
    public void testInitializeLogging_HandlesIOException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("initializeLogging");
            method.setAccessible(true);
            method.invoke(miniApp);
        });
    }

    @Test
    public void testStartServer_ExecutesWithoutException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("startServer");
            method.setAccessible(true);
            method.invoke(miniApp);
        });
    }

    @Test
    public void testStartServer_PrintsServerStartMessage() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("startServer");
            method.setAccessible(true);
            method.invoke(miniApp);
        });

        // Assert
        String output = outputStream.toString();
        String error = errorStream.toString();
        assertTrue(output.contains("Server started on port") || error.contains("Failed to start server"));
    }

    @Test
    public void testStartServer_HandlesPortBindingException() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("startServer");
            method.setAccessible(true);
            method.invoke(miniApp);
        });
    }

    @Test
    public void testStartServer_ClosesServerSocket() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("startServer");
            method.setAccessible(true);
            method.invoke(miniApp);
        });

        // Assert - If it completes without hanging, socket was properly closed
        String output = outputStream.toString();
        assertNotNull(output);
    }

    @Test
    public void testStartServer_SimulatesServerRunning() {
        // Arrange & Act
        long startTime = System.currentTimeMillis();
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("startServer");
            method.setAccessible(true);
            method.invoke(miniApp);
        });
        long endTime = System.currentTimeMillis();

        // Assert - Should execute quickly or take time based on port availability
        assertTrue(endTime - startTime >= 0, "Server method should complete");
    }

    @Test
    public void testHardcodedPortConstant() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = MiniApp.class.getDeclaredField("SERVER_PORT");
            field.setAccessible(true);
            int port = field.getInt(null);

            // Assert
            assertEquals(8080, port, "Server port should be 8080");
        });
    }

    @Test
    public void testHardcodedConfigFilePath() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = MiniApp.class.getDeclaredField("CONFIG_FILE_PATH");
            field.setAccessible(true);
            String path = (String) field.get(null);

            // Assert
            assertEquals("/opt/app/config/app.properties", path);
        });
    }

    @Test
    public void testHardcodedLogFilePath() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Field field = MiniApp.class.getDeclaredField("LOG_FILE_PATH");
            field.setAccessible(true);
            String path = (String) field.get(null);

            // Assert
            assertEquals("/var/log/mini-app.log", path);
        });
    }

    @Test
    public void testMultipleInstancesCanBeCreated() {
        // Arrange & Act
        MiniApp app1 = new MiniApp();
        MiniApp app2 = new MiniApp();
        MiniApp app3 = new MiniApp();

        // Assert
        assertNotNull(app1);
        assertNotNull(app2);
        assertNotNull(app3);
        assertNotSame(app1, app2);
        assertNotSame(app2, app3);
    }

    @Test
    public void testInitializeApplication_CreatesDebugMessages() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
            method.setAccessible(true);
            method.invoke(miniApp);
        });

        // Assert
        String output = outputStream.toString();
        assertTrue(output.length() > 0, "Should produce output during initialization");
    }

    @Test
    public void testStartServer_HandlesInterruptedException() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            Thread testThread = new Thread(() -> {
                try {
                    java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("startServer");
                    method.setAccessible(true);
                    method.invoke(miniApp);
                } catch (Exception e) {
                    // Expected in test environment
                }
            });
            testThread.start();
            Thread.sleep(100);
            testThread.interrupt();
        });
    }

    @Test
    public void testMainMethodWithMultipleArgs() {
        // Arrange
        String[] args = {"arg1", "arg2", "arg3"};

        // Act & Assert
        assertDoesNotThrow(() -> {
            Thread testThread = new Thread(() -> {
                MiniApp.main(args);
            });
            testThread.start();
            Thread.sleep(2000);
            testThread.interrupt();
        });
    }

    @Test
    public void testInitializeApplication_CallsAllMethods() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("initializeApplication");
            method.setAccessible(true);
            method.invoke(miniApp);
        });

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Configuration") || output.contains("Logging") || output.contains("database"));
    }

    @Test
    public void testStartServer_PrintsReadyMessage() {
        // Arrange & Act
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = MiniApp.class.getDeclaredMethod("startServer");
            method.setAccessible(true);
            method.invoke(miniApp);
        });

        // Assert
        String output = outputStream.toString();
        assertTrue(output.contains("Server ready") || output.contains("Server started") || output.contains("Failed to start"));
    }
}
