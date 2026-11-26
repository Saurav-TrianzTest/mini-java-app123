package com.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseServiceTest {

    private DatabaseService service;
    private PrintStream origOut;
    private PrintStream origErr;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void setUp() {
        service = new DatabaseService();
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

    private void setConnection(Connection c) throws Exception {
        Field f = DatabaseService.class.getDeclaredField("connection");
        f.setAccessible(true);
        f.set(service, c);
    }

    private Object invokePrivate(String name) throws Exception {
        Method m = DatabaseService.class.getDeclaredMethod(name);
        m.setAccessible(true);
        try {
            return m.invoke(service);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private Connection fakeConnection(List<String> calls, boolean closed, boolean throwOnPrepare, boolean throwOnClose) {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
                calls.add(method.getName());
                switch (method.getName()) {
                    case "isClosed":
                        return closed;
                    case "close":
                        if (throwOnClose) {
                            throw new SQLException("close failed");
                        }
                        return null;
                    case "prepareStatement":
                        if (throwOnPrepare) {
                            throw new SQLException("prepare failed");
                        }
                        return Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[] {java.sql.PreparedStatement.class},
                            (p, m, a) -> {
                                calls.add("stmt." + m.getName());
                                if (m.getReturnType() == boolean.class) {
                                    return false;
                                }
                                return null;
                            });
                    default:
                        return null;
                }
            });
    }

    @Test
    void constructor_createsInstance() {
        assertNotNull(service);
    }

    @Test
    void connect_withoutDatabase_doesNotThrowAndLogsAttempt() {
        assertDoesNotThrow(() -> service.connect());
        String o = out.toString();
        assertTrue(o.contains("Connecting to database..."));
        // Either driver missing or connection failed (no DB available)
        String e = err.toString();
        assertTrue(e.contains("Database") || o.contains("Connected to database"));
    }

    @Test
    void connectToCache_printsHostAndPort() throws Exception {
        invokePrivate("connectToCache");
        assertTrue(out.toString().contains("127.0.0.1:6379"));
    }

    @Test
    void initializeExternalServices_printsUrls() throws Exception {
        invokePrivate("initializeExternalServices");
        String o = out.toString();
        assertTrue(o.contains("http://api.example.com:8080/v1"));
        assertTrue(o.contains("https://payment.internal.company.com/process"));
    }

    @Test
    void executeQuery_withNoConnection_doesNothing() {
        assertDoesNotThrow(() -> service.executeQuery("SELECT 1"));
        assertEquals("", out.toString());
    }

    @Test
    void executeQuery_withOpenConnection_executesStatement() throws Exception {
        List<String> calls = new ArrayList<>();
        setConnection(fakeConnection(calls, false, false, false));
        service.executeQuery("SELECT 1");
        assertTrue(out.toString().contains("Executing query: SELECT 1"));
        assertTrue(calls.contains("prepareStatement"));
        assertTrue(calls.contains("stmt.setQueryTimeout"));
        assertTrue(calls.contains("stmt.execute"));
        assertTrue(calls.contains("stmt.close"));
    }

    @Test
    void executeQuery_withClosedConnection_skipsExecution() throws Exception {
        List<String> calls = new ArrayList<>();
        setConnection(fakeConnection(calls, true, false, false));
        service.executeQuery("SELECT 1");
        assertFalse(calls.contains("prepareStatement"));
    }

    @Test
    void executeQuery_whenSqlException_logsError() throws Exception {
        List<String> calls = new ArrayList<>();
        setConnection(fakeConnection(calls, false, true, false));
        service.executeQuery("BAD SQL");
        assertTrue(err.toString().contains("Query execution failed: prepare failed"));
    }

    @Test
    void disconnect_withNoConnection_doesNothing() {
        assertDoesNotThrow(() -> service.disconnect());
        assertEquals("", out.toString());
    }

    @Test
    void disconnect_withOpenConnection_closesIt() throws Exception {
        List<String> calls = new ArrayList<>();
        setConnection(fakeConnection(calls, false, false, false));
        service.disconnect();
        assertTrue(calls.contains("close"));
        assertTrue(out.toString().contains("Database connection closed"));
    }

    @Test
    void disconnect_withClosedConnection_doesNotClose() throws Exception {
        List<String> calls = new ArrayList<>();
        setConnection(fakeConnection(calls, true, false, false));
        service.disconnect();
        assertFalse(calls.contains("close"));
    }

    @Test
    void disconnect_whenCloseFails_logsError() throws Exception {
        List<String> calls = new ArrayList<>();
        setConnection(fakeConnection(calls, false, false, true));
        service.disconnect();
        assertTrue(err.toString().contains("Failed to close database connection: close failed"));
    }
}
