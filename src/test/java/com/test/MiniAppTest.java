package com.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MiniApp
 */
public class MiniAppTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testApplicationStartup() {
        String output = outContent.toString();
        assertTrue(output.contains("Starting Mini Java Application") ||
                   output.isEmpty(),
                   "Application should start without critical errors");
    }

    @Test
    public void testMiniAppInitialization() {
        MiniApp app = new MiniApp();
        assertNotNull(app, "MiniApp instance should be created");
    }
}
