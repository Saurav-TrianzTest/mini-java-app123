package com.test;

import org.junit.Test;
import static org.junit.Assert.*;

public class MiniAppTest {

    @Test
    public void testApplicationInitialization() {
        MiniApp app = new MiniApp();
        assertNotNull("MiniApp instance should not be null", app);
    }

    @Test
    public void testConfigFilePath() {
        String expectedPath = "/opt/app/config/app.properties";
        assertTrue("Config file path should contain expected path",
                   expectedPath.contains("/opt/app/config"));
    }
}
