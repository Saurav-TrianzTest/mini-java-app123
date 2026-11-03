# ============================================================
# Stage 1: Builder
# ============================================================
FROM maven:3.9.4-eclipse-temurin-11 AS builder

WORKDIR /workspace

# Copy dependency descriptor first for layer caching
COPY pom.xml .

# Download all dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application JAR (skip tests for Docker build)
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:11-jdk

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -s /bin/false appuser

# Set timezone
ENV TZ=UTC

# JVM tuning for containers
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions -Djava.security.egd=file:/dev/./urandom"

# Application environment variables (override at runtime)
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=docker
ENV DB_HOST=localhost
ENV DB_PORT=3306
ENV DB_NAME=mini_app_db
ENV DB_USERNAME=root
ENV DB_PASSWORD=""
ENV REDIS_HOST=redis-service
ENV REDIS_PORT=6379
ENV EXTERNAL_API_URL=http://external-api-service/v1
ENV PAYMENT_SERVICE_URL=http://payment-service/process
ENV CONFIG_FILE_PATH=/app/config/app.properties
ENV LOG_FILE_PATH=/app/logs/mini-app.log
ENV LOG_DIR_PATH=/app/logs

# Create required directories
RUN mkdir -p /app/config /app/logs /app/temp /app/uploads && \
    chown -R appuser:appgroup /app

# Copy the built JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
