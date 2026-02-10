# Multi-stage Dockerfile for Java Spring Boot Application
# Build stage using Maven
FROM maven:3.9.4-eclipse-temurin-11 AS builder

WORKDIR /workspace

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for Docker build)
RUN mvn clean package -DskipTests -B

# Runtime stage using explicit base image
FROM amazoncorretto:11

WORKDIR /app

# Create non-root user for security
RUN yum install -y shadow-utils && \
    groupadd -r appuser && \
    useradd -r -g appuser -s /sbin/nologin appuser && \
    yum clean all

# Create necessary directories
RUN mkdir -p /app/logs /app/config /app/uploads /tmp/mini-app && \
    chown -R appuser:appuser /app /tmp/mini-app

# Copy the built artifact from builder stage
COPY --from=builder --chown=appuser:appuser /workspace/target/*.jar /app/app.jar

# Set environment variables for JVM optimization
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions" \
    SERVER_PORT=8080 \
    CONFIG_DIR=/app/config \
    LOG_DIR=/app/logs \
    TEMP_DIR=/tmp/mini-app \
    UPLOAD_DIR=/app/uploads \
    TZ=UTC

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Run the application with JVM options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
