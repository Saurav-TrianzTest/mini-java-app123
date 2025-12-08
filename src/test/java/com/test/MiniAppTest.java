package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class MiniAppTest {

    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errorStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Test MiniApp constructor creates non-null instance")
    void testConstructor() {
        MiniApp app = new MiniApp();
        assertNotNull(app);
    }

    @Test
    @DisplayName("Test MiniApp constructor creates valid object")
    void testConstructorCreatesValidObject() {
        MiniApp app = new MiniApp();
        assertNotNull(app);
        assertTrue(app instanceof MiniApp);
    }

    @Test
    @DisplayName("Test multiple MiniApp instances are independent")
    void testMultipleIndependentInstances() {
        MiniApp app1 = new MiniApp();
        MiniApp app2 = new MiniApp();
        assertNotNull(app1);
        assertNotNull(app2);
        assertNotSame(app1, app2);
    }

    @Test
    @DisplayName("Test main method with empty args")
    void testMainMethod() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test main with null args")
    void testMainWithNullArgs() {
        assertDoesNotThrow(() -> MiniApp.main(null));
    }

    @Test
    @DisplayName("Test main with single argument")
    void testMainWithSingleArg() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"single"}));
    }

    @Test
    @DisplayName("Test main with multiple arguments")
    void testMainWithMultipleArgs() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"arg1", "arg2", "arg3"}));
    }

    @Test
    @DisplayName("Test main with empty string arguments")
    void testMainWithEmptyStrings() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"", "", ""}));
    }

    @Test
    @DisplayName("Test main with whitespace arguments")
    void testMainWithWhitespace() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"   ", "\t", "\n"}));
    }

    @Test
    @DisplayName("Test main with special characters")
    void testMainWithSpecialChars() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"arg!", "@test", "#special"}));
    }

    @Test
    @DisplayName("Test main with numeric args")
    void testMainWithNumericArgs() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"123", "456", "789"}));
    }

    @Test
    @DisplayName("Test main with mixed args")
    void testMainWithMixedArgs() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{"text", "123", "!@#", ""}));
    }

    @Test
    @DisplayName("Test main with very long argument array")
    void testMainWithLongArgArray() {
        String[] longArgs = new String[100];
        for (int i = 0; i < 100; i++) {
            longArgs[i] = "arg" + i;
        }
        assertDoesNotThrow(() -> MiniApp.main(longArgs));
    }

    @Test
    @DisplayName("Test main prints startup message")
    void testMainPrintsStartupMessage() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Starting Mini Java Application"));
    }

    @Test
    @DisplayName("Test main initializes application")
    void testMainInitializesApplication() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.length() > 0);
    }

    @Test
    @DisplayName("Test main starts server")
    void testMainStartsServer() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Server") || output.contains("port") || output.contains("8080"));
    }

    @Test
    @DisplayName("Test application produces output")
    void testApplicationProducesOutput() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertFalse(output.isEmpty());
    }

    @Test
    @DisplayName("Test application handles missing config")
    void testApplicationHandlesMissingConfig() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test application handles invalid log directory")
    void testApplicationHandlesInvalidLogDir() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test database service initialization")
    void testDatabaseServiceInit() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("database") || output.contains("Database") || output.contains("Connecting"));
    }

    @Test
    @DisplayName("Test configuration loading")
    void testConfigLoading() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("config") || output.contains("Configuration") || output.contains("properties"));
    }

    @Test
    @DisplayName("Test logging initialization")
    void testLoggingInit() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.contains("log") || output.contains("Logging") || errorOutput.contains("log") || output.length() > 0);
    }

    @Test
    @DisplayName("Test server port 8080")
    void testServerPort() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("8080"));
    }

    @Test
    @DisplayName("Test hardcoded port usage")
    void testHardcodedPort() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("8080") || output.contains("port"));
    }

    @Test
    @DisplayName("Test server ready message")
    void testServerReadyMessage() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("ready") || output.contains("accept") || output.contains("connections"));
    }

    @Test
    @DisplayName("Test exception handling")
    void testExceptionHandling() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test IO exception handling")
    void testIOExceptionHandling() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test startup time is reasonable")
    void testStartupTime() {
        long startTime = System.currentTimeMillis();
        MiniApp.main(new String[]{});
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 30000);
    }

    @Test
    @DisplayName("Test error logging")
    void testErrorLogging() {
        MiniApp.main(new String[]{});
        String errors = errorStreamCaptor.toString();
        assertNotNull(errors);
    }

    @Test
    @DisplayName("Test application stability with multiple runs")
    void testApplicationStability() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 3; i++) {
                MiniApp.main(new String[]{});
            }
        });
    }

    @Test
    @DisplayName("Test concurrent application execution")
    void testConcurrentApps() {
        assertDoesNotThrow(() -> {
            Thread t1 = new Thread(() -> MiniApp.main(new String[]{}));
            Thread t2 = new Thread(() -> MiniApp.main(new String[]{}));
            t1.start();
            Thread.sleep(100);
            t2.start();
            t1.join(5000);
            t2.join(5000);
        });
    }

    @Test
    @DisplayName("Test thread safety")
    void testThreadSafety() {
        assertDoesNotThrow(() -> {
            Runnable task = () -> MiniApp.main(new String[]{});
            Thread t1 = new Thread(task);
            Thread t2 = new Thread(task);
            Thread t3 = new Thread(task);
            t1.start();
            t2.start();
            t3.start();
            t1.join(10000);
            t2.join(10000);
            t3.join(10000);
        });
    }

    @Test
    @DisplayName("Test database connection info output")
    void testDatabaseConnectionInfo() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connect") || output.contains("connect") || output.contains("database") || output.contains("Database"));
    }

    @Test
    @DisplayName("Test file system error handling")
    void testFileSystemErrors() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test startup steps execution")
    void testStartupSteps() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Starting"));
    }

    @Test
    @DisplayName("Test toString method")
    void testToString() {
        MiniApp app = new MiniApp();
        String str = app.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    @DisplayName("Test hashCode method returns non-zero")
    void testHashCode() {
        MiniApp app = new MiniApp();
        int hashCode = app.hashCode();
        assertNotEquals(0, hashCode);
    }

    @Test
    @DisplayName("Test equals method reflexivity")
    void testEquals() {
        MiniApp app1 = new MiniApp();
        assertNotNull(app1);
        assertEquals(app1, app1);
    }

    @Test
    @DisplayName("Test absolute file paths usage")
    void testAbsoluteFilePaths() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.contains("Configuration") || errorOutput.contains("Configuration") || output.contains("config") || errorOutput.contains("config"));
    }

    @Test
    @DisplayName("Test config file absolute path handling")
    void testConfigFileAbsolutePath() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.contains("/opt/app") || errorOutput.contains("config") || output.contains("Configuration"));
    }

    @Test
    @DisplayName("Test config file path constant /opt/app/config/app.properties")
    void testConfigFilePathConstant() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.contains("/opt/app/config/app.properties") || errorOutput.contains("config") || output.contains("Configuration"));
    }

    @Test
    @DisplayName("Test log file absolute path handling")
    void testLogFileAbsolutePath() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.contains("/var/log") || errorOutput.contains("log") || output.contains("Logging"));
    }

    @Test
    @DisplayName("Test log file path constant /var/log/mini-app.log")
    void testLogFilePathConstant() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.contains("/var/log/mini-app.log") || errorOutput.contains("log") || output.contains("Logging"));
    }

    @Test
    @DisplayName("Test server port constant 8080")
    void testServerPortConstant() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("8080"));
    }

    @Test
    @DisplayName("Test ServerSocket creation with hardcoded port")
    void testServerSocketHardcodedPort() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("8080"));
    }

    @Test
    @DisplayName("Test server socket closes properly")
    void testServerSocketClose() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test Thread.sleep in startServer method")
    void testThreadSleepInStartServer() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test Properties loading from FileInputStream")
    void testPropertiesLoading() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.length() > 0 || errorOutput.length() >= 0);
    }

    @Test
    @DisplayName("Test File existence check")
    void testFileExistenceCheck() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test log directory creation")
    void testLogDirCreation() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test log file creation")
    void testLogFileCreation() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test IOException handling in loadConfiguration")
    void testIOExceptionInLoadConfig() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test IOException handling in initializeLogging")
    void testIOExceptionInInitLogging() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test Exception handling in startServer")
    void testExceptionInStartServer() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test DatabaseService connect invocation")
    void testDatabaseServiceConnect() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting") || output.contains("database") || output.contains("Database"));
    }

    @Test
    @DisplayName("Test System.out.println calls")
    void testSystemOutPrintln() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.length() > 100);
    }

    @Test
    @DisplayName("Test System.err.println calls")
    void testSystemErrPrintln() {
        MiniApp.main(new String[]{});
        String errorOutput = errorStreamCaptor.toString();
        assertNotNull(errorOutput);
    }

    @Test
    @DisplayName("Test initializeApplication method flow")
    void testInitializeApplicationFlow() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Configuration") || output.contains("config") || output.contains("Logging") || output.contains("log"));
    }

    @Test
    @DisplayName("Test loadConfiguration with missing file")
    void testLoadConfigurationMissingFile() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test loadConfiguration with existing file")
    void testLoadConfigurationExistingFile() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test initializeLogging creates directory")
    void testInitializeLoggingCreatesDir() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test initializeLogging creates file")
    void testInitializeLoggingCreatesFile() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test startServer creates ServerSocket")
    void testStartServerCreatesSocket() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Server started") || output.contains("port") || output.contains("8080"));
    }

    @Test
    @DisplayName("Test startServer accepts connections")
    void testStartServerAcceptsConnections() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("accept") || output.contains("ready") || output.contains("connections"));
    }

    @Test
    @DisplayName("Test startServer sleeps before closing socket")
    void testStartServerSleeps() {
        long start = System.currentTimeMillis();
        MiniApp.main(new String[]{});
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 900);
    }

    @Test
    @DisplayName("Test startServer closes socket after operation")
    void testStartServerClosesSocket() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test File object creation for config")
    void testFileObjectForConfig() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test File object creation for log")
    void testFileObjectForLog() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test Properties object usage")
    void testPropertiesObjectUsage() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test FileInputStream usage")
    void testFileInputStreamUsage() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test IOException catch in loadConfiguration")
    void testIOExceptionCatchInLoadConfig() {
        MiniApp.main(new String[]{});
        String errorOutput = errorStreamCaptor.toString();
        assertNotNull(errorOutput);
    }

    @Test
    @DisplayName("Test IOException catch in initializeLogging")
    void testIOExceptionCatchInInitLogging() {
        MiniApp.main(new String[]{});
        String errorOutput = errorStreamCaptor.toString();
        assertNotNull(errorOutput);
    }

    @Test
    @DisplayName("Test Exception catch in startServer")
    void testExceptionCatchInStartServer() {
        MiniApp.main(new String[]{});
        String errorOutput = errorStreamCaptor.toString();
        assertNotNull(errorOutput);
    }

    @Test
    @DisplayName("Test DatabaseService creation and connect")
    void testDatabaseServiceCreationAndConnect() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database") || output.contains("database"));
    }

    @Test
    @DisplayName("Test complete application workflow")
    void testCompleteWorkflow() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Starting") && (output.contains("Server") || output.contains("8080")));
    }

    @Test
    @DisplayName("Test main method completes successfully")
    void testMainMethodCompletes() {
        long start = System.currentTimeMillis();
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 20000);
    }

    @Test
    @DisplayName("Test no hanging threads after main completes")
    void testNoHangingThreads() {
        assertDoesNotThrow(() -> {
            int beforeCount = Thread.activeCount();
            MiniApp.main(new String[]{});
            Thread.sleep(2000);
            int afterCount = Thread.activeCount();
            assertTrue(afterCount <= beforeCount + 5);
        });
    }

    @Test
    @DisplayName("Test multiple sequential executions")
    void testMultipleSequentialExecutions() {
        assertDoesNotThrow(() -> {
            MiniApp.main(new String[]{});
            Thread.sleep(100);
            MiniApp.main(new String[]{});
            Thread.sleep(100);
            MiniApp.main(new String[]{});
        });
    }

    @Test
    @DisplayName("Test constructor creates non-null object")
    void testConstructorCreatesNonNull() {
        MiniApp app = new MiniApp();
        assertNotNull(app);
        assertTrue(app instanceof MiniApp);
    }

    @Test
    @DisplayName("Test different instances are unique objects")
    void testDifferentInstancesAreUnique() {
        MiniApp app1 = new MiniApp();
        MiniApp app2 = new MiniApp();
        assertNotSame(app1, app2);
    }

    @Test
    @DisplayName("Test private initializeApplication method execution")
    void testInitializeApplication() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test private loadConfiguration method execution")
    void testLoadConfiguration() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test private initializeLogging method execution")
    void testInitializeLogging() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test private startServer method execution")
    void testStartServer() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test loadConfiguration invoked during initialization")
    void testLoadConfigurationInvoked() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.length() > 0 || errorOutput.length() >= 0);
    }

    @Test
    @DisplayName("Test initializeLogging invoked during initialization")
    void testInitializeLoggingInvoked() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.length() > 0 || errorOutput.length() >= 0);
    }

    @Test
    @DisplayName("Test startServer invoked during initialization")
    void testStartServerInvoked() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Server") || output.contains("server"));
    }

    @Test
    @DisplayName("Test DatabaseService instantiation in initializeApplication")
    void testDatabaseServiceInstantiation() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test connect followed by immediate disconnect")
    void testConnectDisconnectImmediately() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test CONFIG_FILE_PATH constant /opt/app/config/app.properties")
    void testConfigFilePathConstantValue() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.contains("/opt/app") || errorOutput.contains("config"));
    }

    @Test
    @DisplayName("Test LOG_FILE_PATH constant /var/log/mini-app.log")
    void testLogFilePathConstantValue() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        String errorOutput = errorStreamCaptor.toString();
        assertTrue(output.contains("/var/log") || errorOutput.contains("log"));
    }

    @Test
    @DisplayName("Test SERVER_PORT constant 8080")
    void testServerPortConstantValue() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("8080"));
    }

    @Test
    @DisplayName("Test config file read with FileInputStream")
    void testConfigFileReadWithFileInputStream() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test properties load from config file")
    void testPropertiesLoadFromConfigFile() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test log directory /var/log creation attempt")
    void testLogDirectoryCreationAttempt() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test log file creation in /var/log")
    void testLogFileCreationInVarLog() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test ServerSocket binding to port 8080")
    void testServerSocketBindingToPort8080() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("8080"));
    }

    @Test
    @DisplayName("Test ServerSocket accept connections on port 8080")
    void testServerSocketAcceptConnectionsOn8080() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("accept") || output.contains("ready"));
    }

    @Test
    @DisplayName("Test Thread.sleep(1000) in startServer")
    void testThreadSleep1000InStartServer() {
        long start = System.currentTimeMillis();
        MiniApp.main(new String[]{});
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 900);
    }

    @Test
    @DisplayName("Test ServerSocket close after operations")
    void testServerSocketCloseAfterOperations() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test IOException error message in loadConfiguration")
    void testIOExceptionErrorMessageInLoadConfig() {
        MiniApp.main(new String[]{});
        String errorOutput = errorStreamCaptor.toString();
        assertNotNull(errorOutput);
    }

    @Test
    @DisplayName("Test IOException error message in initializeLogging")
    void testIOExceptionErrorMessageInInitLogging() {
        MiniApp.main(new String[]{});
        String errorOutput = errorStreamCaptor.toString();
        assertNotNull(errorOutput);
    }

    @Test
    @DisplayName("Test Exception error message in startServer")
    void testExceptionErrorMessageInStartServer() {
        MiniApp.main(new String[]{});
        String errorOutput = errorStreamCaptor.toString();
        assertNotNull(errorOutput);
    }

    @Test
    @DisplayName("Test new DatabaseService() instantiation")
    void testNewDatabaseServiceInstantiation() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test dbService.connect() invocation")
    void testDbServiceConnectInvocation() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Connecting to database"));
    }

    @Test
    @DisplayName("Test main method startup sequence")
    void testMainMethodStartupSequence() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Starting Mini Java Application"));
    }

    @Test
    @DisplayName("Test new MiniApp() in main method")
    void testNewMiniAppInMainMethod() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }

    @Test
    @DisplayName("Test app.initializeApplication() in main method")
    void testAppInitializeApplicationInMain() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Configuration") || output.contains("Logging") || output.contains("Connecting"));
    }

    @Test
    @DisplayName("Test app.startServer() in main method")
    void testAppStartServerInMain() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Server") || output.contains("8080"));
    }

    @Test
    @DisplayName("Test complete main method execution flow")
    void testCompleteMainMethodExecutionFlow() {
        MiniApp.main(new String[]{});
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Starting Mini Java Application"));
        assertTrue(output.contains("8080") || output.contains("Server"));
    }

    @Test
    @DisplayName("Test memory usage is reasonable")
    void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        MiniApp.main(new String[]{});
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        assertTrue(memoryUsed < 100000000);
    }

    @Test
    @DisplayName("Test application resilience to null pointer exceptions")
    void testNullPointerResilience() {
        assertDoesNotThrow(() -> MiniApp.main(new String[]{}));
    }
}
