# Multi-stage Dockerfile for mini-java-app
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

# Stage 2: Runtime stage
FROM eclipse-temurin:11-jdk

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Create directories for logs and config
RUN mkdir -p /app/logs /app/config && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Set environment variables for JVM optimization
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions" \
    TZ=UTC \
    CONFIG_FILE_PATH=/app/config/app.properties \
    LOG_FILE_PATH=/app/logs/mini-app.log \
    LOG_DIR_PATH=/app/logs

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
