package com.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application class to enable actuator health check endpoint
 * Health check available at: /actuator/health
 */
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        
        // Also run the original MiniApp logic
        MiniApp.main(args);
    }
}
