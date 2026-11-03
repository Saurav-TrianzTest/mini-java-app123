# ============================================================
# Stage 1: Builder
# ============================================================
FROM maven:3.9.4-eclipse-temurin-11 AS builder

WORKDIR /workspace

# Copy dependency descriptor first for layer caching
COPY pom.xml .

# Download all dependencies (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for Docker build)
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

# JVM tuning for containerized environments
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UnlockExperimentalVMOptions \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=UTC"

# Spring Boot profile
ENV SPRING_PROFILES_ACTIVE=docker

# Application environment variables (override at runtime)
ENV SERVER_PORT=8080
ENV DB_HOST=localhost
ENV DB_PORT=3306
ENV DB_NAME=mini_app_db
ENV DB_USERNAME=root
ENV DB_PASSWORD=""
ENV REDIS_HOST=redis-service
ENV REDIS_PORT=6379
ENV EXTERNAL_API_URL=http://api.example.com:8080/v1
ENV PAYMENT_SERVICE_URL=https://payment.internal.company.com/process
ENV LOG_DIR_PATH=/mnt/filestore/logs
ENV CONFIG_FILE_PATH=/app/config/app.properties

# Copy the built JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Create log and config directories
RUN mkdir -p /mnt/filestore/logs /app/config && \
    chown -R appuser:appgroup /app /mnt/filestore/logs

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Entry point with JVM options
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
