# Multi-stage Dockerfile for Cloud-Ready Java Application
# Optimized for AWS ECS, Azure Container Instances, and GCP Cloud Run

# Stage 1: Build Stage
FROM maven:3.8.6-openjdk-11-slim AS build

WORKDIR /build

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime Stage
FROM openjdk:11-jre-slim

# Add metadata labels
LABEL maintainer="your-team@example.com"
LABEL version="1.0.0"
LABEL description="Cloud-Ready Mini Java Application"
LABEL cloud.compatible="AWS,Azure,GCP"

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /build/target/mini-java-app-1.0.0.jar app.jar

# Create directories for temporary files (if needed)
RUN mkdir -p /app/tmp /app/logs && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port (configurable via SERVER_PORT environment variable)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

# Set JVM options for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom"

# Default environment variables (can be overridden at runtime)
ENV SERVER_PORT=8080
ENV ENVIRONMENT=production
ENV LOG_LEVEL=INFO

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Alternative: Use exec form for better signal handling
# ENTRYPOINT ["java", "-jar", "app.jar"]
