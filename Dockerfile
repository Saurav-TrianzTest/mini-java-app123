# Multi-stage Dockerfile for Mini Java Application
# Stage 1: Build stage using Maven
FROM maven:3.9.4-eclipse-temurin-11 AS builder

# Set working directory
WORKDIR /workspace

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage using Amazon Corretto 11
FROM amazoncorretto:11

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Create necessary directories
RUN mkdir -p /app/logs /app/config /app/temp && \
    chown -R appuser:appuser /app

# Copy JAR from builder stage
COPY --from=builder /workspace/target/*.jar /app/app.jar

# Set ownership
RUN chown appuser:appuser /app/app.jar

# Switch to non-root user
USER appuser

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080 \
    CONFIG_FILE_PATH=/app/config/app.properties \
    LOG_FILE_PATH=/app/logs/mini-app.log \
    LOG_DIR=/app/logs \
    TZ=UTC

# Expose application port
EXPOSE 8080

# Health check endpoint (application-native)
# Note: No curl/wget installed - rely on ECS service health checks

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
