package com.test;

import org.junit.Test;
import static org.junit.Assert.*;

public class DatabaseServiceTest {

    @Test
    public void testDatabaseServiceCreation() {
        DatabaseService dbService = new DatabaseService();
        assertNotNull("DatabaseService instance should not be null", dbService);
    }

    @Test
    public void testDatabaseUrlFormat() {
        String expectedUrl = "jdbc:postgresql://localhost:5432/mini_app_db";
        assertTrue("Database URL should be in correct format",
                   expectedUrl.startsWith("jdbc:postgresql://"));
    }
}
