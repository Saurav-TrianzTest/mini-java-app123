package com.test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for containerization readiness.
 * Provides a /health HTTP GET endpoint returning application status.
 * Required for Kubernetes liveness and readiness probes.
 */
@RestController
public class HealthController {

    /**
     * Health check endpoint.
     * Returns HTTP 200 with JSON status when the application is healthy.
     * Used by Kubernetes liveness/readiness probes and load balancers.
     *
     * @return JSON response with status "UP"
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("application", "mini-java-app");
        return ResponseEntity.ok(status);
    }
}
