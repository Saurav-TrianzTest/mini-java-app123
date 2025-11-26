# Multi-stage Dockerfile for Java Spring Boot Application
# Stage 1: Build stage
FROM maven:3.9.4-eclipse-temurin-11 AS builder

WORKDIR /workspace

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:11-jdk

WORKDIR /app

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Create directories for application data
RUN mkdir -p /app/logs /app/config /app/temp /app/uploads && \
    chown -R appuser:appuser /app

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080 \
    HEALTH_CHECK_PORT=8081 \
    CONFIG_DIR=/app/config \
    LOG_DIR=/app/logs \
    TEMP_DIR=/app/temp \
    UPLOAD_DIR=/app/uploads \
    TZ=UTC

# Expose application ports
EXPOSE 8080 8081

# Switch to non-root user
USER appuser

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
