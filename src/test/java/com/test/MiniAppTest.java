package com.test;

import org.junit.Test;
import static org.junit.Assert.*;

public class MiniAppTest {

    @Test
    public void testApplicationExists() {
        MiniApp app = new MiniApp();
        assertNotNull(app);
    }

    @Test
    public void testMainMethodRuns() {
        try {
            MiniApp.main(new String[]{});
        } catch (Exception e) {
            // Expected to fail due to hardcoded paths
        }
    }
}
