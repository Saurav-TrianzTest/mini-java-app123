package com.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MiniAppTest {

    private PrintStream origOut;
    private PrintStream origErr;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void setUp() {
        origOut = System.out;
        origErr = System.err;
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @AfterEach
    void tearDown() {
        System.setOut(origOut);
        System.setErr(origErr);
    }

    private void invoke(MiniApp app, String name) throws Exception {
        Method m = MiniApp.class.getDeclaredMethod(name);
        m.setAccessible(true);
        try {
            m.invoke(app);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void constructor_createsInstance() {
        assertNotNull(new MiniApp());
    }

    @Test
    void main_runsWithoutThrowing() {
        assertDoesNotThrow(() -> MiniApp.main(new String[0]));
        assertTrue(out.toString().contains("Starting Mini Java Application..."));
    }

    @Test
    void loadConfiguration_logsOutcome() throws Exception {
        invoke(new MiniApp(), "loadConfiguration");
        String o = out.toString() + err.toString();
        assertTrue(o.contains("/opt/app/config/app.properties") || o.contains("Failed to load configuration"));
    }

    @Test
    void initializeLogging_logsOutcome() throws Exception {
        invoke(new MiniApp(), "initializeLogging");
        String o = out.toString() + err.toString();
        assertTrue(o.contains("Logging initialized at: /var/log/mini-app.log")
            || o.contains("Failed to initialize logging"));
    }

    @Test
    void startServer_logsOutcome() throws Exception {
        invoke(new MiniApp(), "startServer");
        String o = out.toString() + err.toString();
        assertTrue(o.contains("Server started on port: 8080") || o.contains("Failed to start server"));
    }

    @Test
    void initializeApplication_runsWithoutThrowing() {
        assertDoesNotThrow(() -> invoke(new MiniApp(), "initializeApplication"));
        assertTrue(out.toString().contains("Connecting to database..."));
    }
}
