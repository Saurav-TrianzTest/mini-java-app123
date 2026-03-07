# Multi-stage Dockerfile for Mini Java Application
# Builder stage with Maven and JDK 11
FROM maven:3.9.4-eclipse-temurin-11 AS builder

WORKDIR /workspace

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Runtime stage with Amazon Corretto 11
FROM amazoncorretto:11

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Create necessary directories
RUN mkdir -p /app/config /app/logs /app/temp /app/uploads && \
    chown -R appuser:appuser /app

# Copy JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions -Djava.security.egd=file:/dev/./urandom"

# Application configuration
ENV SERVER_PORT=8080 \
    SERVER_HOST=0.0.0.0 \
    CONFIG_DIR=/app/config \
    LOG_DIR=/app/logs \
    TEMP_DIR=/app/temp \
    UPLOAD_DIR=/app/uploads

# Expose application port
EXPOSE 8080

# Switch to non-root user
USER appuser

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]