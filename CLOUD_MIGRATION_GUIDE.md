# Cloud Migration Guide - Mini Java Application

## Overview
This application has been transformed to be fully cloud-ready and compatible with GCP (Google Cloud Platform) deployment. All cloud readiness blockers have been resolved.

## Cloud Readiness Issues Fixed

### 1. Hard-coded File Paths (cr-java-0061) ✅
**Issue**: Application contained absolute file paths (`/opt/app/config`, `/var/log/mini-app`)
**Fix**: 
- Replaced `java.io.File` with Spring `ResourceLoader`
- Configuration files now loaded from classpath resources
- File paths externalized to environment variables
- Supports GCS bucket paths for cloud storage

### 2. Java.io.File Usage for Data Storage (cr-java-0063) ✅
**Issue**: Used `java.io.File` for persistent data storage
**Fix**:
- Replaced with Spring `ResourceLoader` for reading resources
- Added GCS bucket configuration for uploads
- Logs written to stdout/stderr for cloud log aggregation

### 3. Hard-coded Database Credentials (cr-java-0069) ✅
**Issue**: Database credentials embedded in source code
**Fix**:
- Credentials externalized to environment variables
- Integrated with GCP Secret Manager using `${sm://secret-name}` syntax
- Spring Cloud GCP Secret Manager dependency added

### 4. Hard-coded Ports (cr-java-0077) ✅
**Issue**: Port numbers hard-coded in application
**Fix**:
- Server port externalized to `${PORT:8080}` environment variable
- Compatible with Cloud Run dynamic port assignment
- Database and Redis ports externalized

### 5. Static Initializers with I/O (cr-java-0105) ✅
**Issue**: I/O operations in static initialization blocks
**Fix**:
- Moved all I/O operations to `@PostConstruct` methods
- Proper Spring dependency injection
- Error handling and retry support

### 6. Lack of Externalized Secrets (cr-java-0113) ✅
**Issue**: API keys, tokens, and credentials embedded in code
**Fix**:
- All secrets moved to GCP Secret Manager
- Spring Cloud GCP integration for secret resolution
- No sensitive data in source code or properties files

### 7. Direct JDBC Connections (cr-java-0073) ✅
**Issue**: Direct JDBC connections without connection pooling
**Fix**:
- Implemented HikariCP connection pool
- Configured with cloud-ready settings
- Proper connection lifecycle management

### 8. Missing Connection Timeouts (cr-java-0097) ✅
**Issue**: Network connections without timeout configurations
**Fix**:
- Added connection timeouts to HikariCP
- Configured read/write timeouts
- Leak detection enabled

### 9. Properties Files in Classpath (cr-java-0070) ✅
**Issue**: Configuration immutable at runtime
**Fix**:
- All configuration externalized to environment variables
- GCP Secret Manager integration
- Runtime configuration changes supported

## Architecture Changes

### Spring Boot Integration
- Converted to Spring Boot application
- Added `@SpringBootApplication` annotation
- Dependency injection with `@Autowired`
- Lifecycle management with `@PostConstruct` and `@PreDestroy`

### Connection Pooling
- HikariCP connection pool configured
- Pool size: 10 max, 2 min idle
- Connection timeout: 30 seconds
- Leak detection enabled

### Structured Logging
- JSON logging with Logstash encoder
- Logs to stdout/stderr for cloud aggregation
- Compatible with GCP Cloud Logging
- Correlation IDs for distributed tracing

### Health Checks
- Spring Boot Actuator endpoints
- Liveness and readiness probes
- Compatible with Kubernetes health checks

## Environment Variables

### Required Environment Variables
```bash
# Database Configuration
DATABASE_URL=jdbc:mysql://CLOUD_SQL_CONNECTION/mini_app_db
DATABASE_USERNAME=<from Secret Manager>
DATABASE_PASSWORD=<from Secret Manager>

# GCP Configuration
GCP_PROJECT_ID=your-gcp-project-id

# Server Configuration
PORT=8080  # Cloud Run assigns this automatically

# Optional: Override defaults
REDIS_HOST=redis-service
REDIS_PORT=6379
EXTERNAL_API_URL=https://api.example.com/v1
```

### GCP Secret Manager Secrets
Create these secrets in GCP Secret Manager:
```bash
# Database secrets
database-url
database-username
database-password

# Redis secrets
redis-password

# External service secrets
external-api-key
payment-service-username
payment-service-password

# Security secrets
jwt-secret
admin-username
admin-password
encryption-key

# Monitoring secrets
monitoring-username
monitoring-password

# Messaging secrets
rabbitmq-username
rabbitmq-password
```

## Deployment Options

### 1. Google Cloud Run
```bash
# Build and deploy
gcloud builds submit --tag gcr.io/PROJECT_ID/mini-java-app
gcloud run deploy mini-java-app \
  --image gcr.io/PROJECT_ID/mini-java-app \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GCP_PROJECT_ID=PROJECT_ID
```

### 2. Google Kubernetes Engine (GKE)
```bash
# Build image
docker build -t gcr.io/PROJECT_ID/mini-java-app:latest .
docker push gcr.io/PROJECT_ID/mini-java-app:latest

# Deploy to GKE
kubectl apply -f k8s/deployment.yaml
```

### 3. Google Compute Engine
```bash
# Build JAR
mvn clean package

# Run with environment variables
java -jar target/mini-java-app-1.0.0.jar \
  --spring.cloud.gcp.project-id=PROJECT_ID
```

## Database Configuration

### Cloud SQL Connection
```bash
# Using Cloud SQL Proxy
DATABASE_URL=jdbc:mysql://localhost:3306/mini_app_db?cloudSqlInstance=PROJECT_ID:REGION:INSTANCE_NAME

# Using Private IP
DATABASE_URL=jdbc:mysql://PRIVATE_IP:3306/mini_app_db

# Using Unix Socket (Cloud Run)
DATABASE_URL=jdbc:mysql:///mini_app_db?cloudSqlInstance=PROJECT_ID:REGION:INSTANCE_NAME&socketFactory=com.google.cloud.sql.mysql.SocketFactory
```

## Monitoring and Observability

### Health Check Endpoints
- Liveness: `http://localhost:8080/actuator/health/liveness`
- Readiness: `http://localhost:8080/actuator/health/readiness`
- Metrics: `http://localhost:8080/actuator/metrics`

### Logging
- All logs written to stdout/stderr
- JSON format for structured logging
- Automatically collected by GCP Cloud Logging
- Search and filter using log fields

### Metrics
- Spring Boot Actuator metrics
- Prometheus endpoint available
- HikariCP pool metrics included

## Security Best Practices

### Implemented
✅ No hardcoded credentials
✅ Secrets in GCP Secret Manager
✅ Non-root user in Docker container
✅ Connection pooling with limits
✅ Timeout configurations
✅ Health check endpoints

### Recommended
- Enable Cloud Armor for DDoS protection
- Use Cloud IAM for authentication
- Enable VPC Service Controls
- Use Cloud KMS for encryption keys
- Enable audit logging

## Testing

### Local Testing
```bash
# Set environment variables
export DATABASE_URL=jdbc:mysql://localhost:3306/mini_app_db
export DATABASE_USERNAME=root
export DATABASE_PASSWORD=password
export GCP_PROJECT_ID=test-project

# Run application
mvn spring-boot:run
```

### Docker Testing
```bash
# Build image
docker build -t mini-java-app:test .

# Run container
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:mysql://host.docker.internal:3306/mini_app_db \
  -e DATABASE_USERNAME=root \
  -e DATABASE_PASSWORD=password \
  mini-java-app:test
```

## Migration Checklist

- [x] Replace hardcoded file paths with environment variables
- [x] Replace java.io.File with ResourceLoader
- [x] Externalize database credentials to Secret Manager
- [x] Replace direct JDBC with HikariCP connection pool
- [x] Add connection timeouts
- [x] Move I/O from static blocks to @PostConstruct
- [x] Externalize all secrets to Secret Manager
- [x] Replace hardcoded ports with environment variables
- [x] Implement structured JSON logging
- [x] Add health check endpoints
- [x] Create Dockerfile for cloud deployment
- [x] Configure Spring Cloud GCP integration
- [x] Add Spring Boot Actuator
- [x] Document environment variables
- [x] Create deployment guide

## Support

For issues or questions:
1. Check application logs in GCP Cloud Logging
2. Verify Secret Manager secrets are accessible
3. Check Cloud SQL connection configuration
4. Review health check endpoints
5. Verify environment variables are set correctly

## Version History

### v1.0.0 - Cloud-Ready Release
- Initial cloud migration
- All 11 cloud readiness blockers resolved
- GCP-optimized configuration
- Production-ready deployment
