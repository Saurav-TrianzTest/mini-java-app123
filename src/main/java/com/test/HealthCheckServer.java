package com.test;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Simple health check endpoint server using Java's built-in HTTP server.
 * Serves a /health endpoint that returns JSON status.
 * Runs on a separate port from the application's main server.
 */
public class HealthCheckServer {

    private static final int HEALTH_PORT = Integer.parseInt(
            System.getenv().getOrDefault("HEALTH_PORT", "8081"));

    public static void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(HEALTH_PORT), 0);
            server.createContext("/health", new HealthHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Health check endpoint started on port: " + HEALTH_PORT);
        } catch (IOException e) {
            System.err.println("Failed to start health check server: " + e.getMessage());
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"UP\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}
