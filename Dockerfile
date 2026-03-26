# Multi-stage Dockerfile for mini-java-app
# Stage 1: Build stage
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

# Stage 2: Runtime stage
FROM eclipse-temurin:11-jdk

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Create directories for application data
RUN mkdir -p /app/logs /app/config /app/temp && \
    chown -R appuser:appuser /app

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    APP_BASE_PATH="/app" \
    TZ="UTC"

# Expose application port
EXPOSE 8080

# Switch to non-root user
USER appuser

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
