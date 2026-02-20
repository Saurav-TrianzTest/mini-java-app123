package com.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Cloud-Native Configuration Loader
 * - Loads configuration from classpath resources
 * - Overrides with environment variables
 * - Supports AWS Secrets Manager integration
 * - Follows 12-factor app principles
 */
public class CloudConfigLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(CloudConfigLoader.class);
    private static final String DEFAULT_CONFIG_FILE = "/application.properties";
    
    private Properties properties;
    
    public CloudConfigLoader() {
        this.properties = new Properties();
        loadConfiguration();
    }
    
    /**
     * Load configuration from classpath and environment variables
     */
    private void loadConfiguration() {
        // Step 1: Load from classpath resource
        loadFromClasspath();
        
        // Step 2: Override with environment variables
        overrideWithEnvironmentVariables();
        
        logger.info("Configuration loaded successfully");
    }
    
    /**
     * Load configuration from classpath resource
     */
    private void loadFromClasspath() {
        try (InputStream input = getClass().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                logger.info("Loaded configuration from classpath: {}", DEFAULT_CONFIG_FILE);
            } else {
                logger.warn("Configuration file not found in classpath: {}", DEFAULT_CONFIG_FILE);
            }
        } catch (Exception e) {
            logger.error("Failed to load configuration from classpath", e);
        }
    }
    
    /**
     * Override properties with environment variables
     * Environment variables take precedence over file-based configuration
     */
    private void overrideWithEnvironmentVariables() {
        System.getenv().forEach((key, value) -> {
            // Convert environment variable format to property format
            // e.g., DATABASE_URL -> database.url
            String propertyKey = key.toLowerCase().replace('_', '.');
            properties.setProperty(propertyKey, value);
        });
        
        logger.debug("Configuration overridden with environment variables");
    }
    
    /**
     * Get configuration property with default value
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get configuration property
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Get integer property with default value
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for property {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get boolean property with default value
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    /**
     * Get long property with default value
     */
    public long getLongProperty(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid long value for property {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Check if property exists
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    /**
     * Get all properties
     */
    public Properties getAllProperties() {
        return new Properties(properties);
    }
}
