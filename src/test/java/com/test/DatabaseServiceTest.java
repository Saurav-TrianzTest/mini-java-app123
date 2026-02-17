package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DatabaseService
 */
public class DatabaseServiceTest {

    private DatabaseService dbService;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    public void setUp() {
        dbService = new DatabaseService();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        outContent.reset();
        errContent.reset();
    }

    @Test
    public void testDatabaseServiceCreation() {
        assertNotNull(dbService, "DatabaseService instance should be created");
    }

    @Test
    public void testConnectAttempt() {
        dbService.connect();
        String output = outContent.toString() + errContent.toString();
        assertTrue(output.contains("Connecting to database") ||
                   output.contains("Database connection failed"),
                   "Connect method should attempt database connection");
    }

    @Test
    public void testDisconnectMethodExists() {
        assertDoesNotThrow(() -> dbService.disconnect(),
                          "Disconnect method should not throw exception even without connection");
    }

    @Test
    public void testExecuteQuery() {
        assertDoesNotThrow(() -> dbService.executeQuery("SELECT 1"),
                          "ExecuteQuery should handle queries gracefully");
    }
}
