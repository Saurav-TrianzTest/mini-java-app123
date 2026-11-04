# Build stage
FROM maven:3.9.4-eclipse-temurin-11 AS builder

WORKDIR /workspace

# Copy pom.xml first and resolve dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src/ src/
RUN mvn clean package -DskipTests -B

# Copy runtime dependencies for the classpath
RUN mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib -B

# Runtime stage
FROM eclipse-temurin:11-jdk

WORKDIR /app

# Create directories for config, logs, and uploads
RUN mkdir -p /app/config /app/logs /app/uploads /tmp/mini-app

# Copy the application JAR and runtime dependencies
COPY --from=builder /workspace/target/*.jar app.jar
COPY --from=builder /workspace/target/lib/ lib/

# Non-root user
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser
USER appuser

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    TZ=UTC \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    APP_PORT=8080 \
    HEALTH_PORT=8081 \
    CONFIG_PATH=/app/config/app.properties \
    LOG_PATH=/app/logs/mini-app.log \
    LOG_DIR=/app/logs \
    DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=mini_app_db \
    REDIS_HOST=localhost \
    REDIS_PORT=6379 \
    EXTERNAL_API_URL=http://api.example.com:8080/v1 \
    PAYMENT_SERVICE_URL=https://payment.internal.company.com/process

# Expose application and health check ports
EXPOSE 8080 8081

# Start the application with the main class
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -cp \"app.jar:lib/*\" com.test.MiniApp"]
