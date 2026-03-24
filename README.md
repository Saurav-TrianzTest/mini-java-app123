# Mini Java Application - Cloud-Ready Version

## Overview
This application has been transformed from a traditional Java application with hardcoded configurations to a fully cloud-ready, containerized application following 12-factor app principles.

## Cloud Readiness Transformation Summary

### ✅ Issues Fixed

#### 1. **Hard-coded File Paths (CRITICAL)** - RESOLVED
- **Original Issue**: Application used absolute file paths (`/opt/app/config/app.properties`, `/var/log/mini-app.log`)
- **Impact**: Application would fail in cloud environments where file systems are ephemeral
- **Fix Applied**:
  - Replaced absolute file paths with classpath resources
  - Configuration loaded from `application.properties` in classpath
  - Logging changed to console output (captured by cloud platforms)
  - Removed all file system dependencies
- **Files Modified**: `MiniApp.java`

#### 2. **Direct JDBC Connections (HIGH)** - RESOLVED
- **Original Issue**: Application used direct JDBC connections without connection pooling
- **Impact**: Poor resource utilization, connection leaks, inability to scale
- **Fix Applied**:
  - Implemented HikariCP connection pooling
  - Added Spring Boot Data JPA with auto-configuration
  - Proper connection lifecycle management with `@PostConstruct` and `@PreDestroy`
  - Connection pool monitoring and leak detection
  - Configurable pool settings via environment variables
- **Files Modified**: `DatabaseService.java`, `pom.xml`

#### 3. **Properties Files in Classpath (MEDIUM)** - RESOLVED
- **Original Issue**: Configuration properties were immutable at runtime
- **Impact**: Required rebuilding application for configuration changes
- **Fix Applied**:
  - All properties now use environment variable placeholders
  - Configuration can be overridden without rebuilding
  - Supports Spring profiles for environment-specific configuration
  - External configuration via command-line arguments
- **Files Modified**: `application.properties`

### 🔧 Additional Improvements

#### Spring Boot Integration
- Converted to Spring Boot application with `@SpringBootApplication`
- Added Spring Boot Actuator for health checks and metrics
- Enabled externalized configuration management
- Added Spring Boot Maven plugin for executable JAR packaging

#### Connection Pooling
- Implemented HikariCP with optimal settings for cloud environments
- Configurable pool size, timeouts, and connection lifecycle
- Leak detection enabled for debugging
- Connection pool statistics available for monitoring

#### Environment Variable Support
All configuration now supports environment variables:
- Database credentials and connection settings
- Redis cache configuration
- External API endpoints and credentials
- Security settings (JWT, encryption keys)
- Monitoring and logging configuration
- Cloud storage settings

#### Containerization
- Created optimized multi-stage Dockerfile
- Added docker-compose.yml for local testing
- Non-root user for security
- Health checks configured
- JVM optimized for containerized environments

#### AWS Deployment Support
- Created ECS task definition template
- Integrated with AWS Secrets Manager for sensitive values
- CloudWatch logging configuration
- Support for RDS, ElastiCache, and S3

## Project Structure

```
Basic-Raghav/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── test/
│   │   │           ├── MiniApp.java              # Main application (Spring Boot)
│   │   │           └── DatabaseService.java      # Database service with HikariCP
│   │   └── resources/
│   │       └── application.properties            # Externalized configuration
├── pom.xml                                       # Maven dependencies with HikariCP
├── Dockerfile                                    # Multi-stage Docker build
├── docker-compose.yml                            # Local testing environment
├── aws-ecs-task-definition.json                 # AWS ECS deployment template
├── CLOUD_DEPLOYMENT_GUIDE.md                    # Detailed deployment guide
└── README.md                                     # This file
```

## Quick Start

### Local Development

1. **Build the application**:
```bash
mvn clean package
```

2. **Run with Docker Compose** (includes MySQL and Redis):
```bash
docker-compose up -d
```

3. **Access the application**:
- Application: http://localhost:8080
- Health Check: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics

### AWS Deployment

See [CLOUD_DEPLOYMENT_GUIDE.md](CLOUD_DEPLOYMENT_GUIDE.md) for detailed AWS deployment instructions.

## Configuration

### Required Environment Variables

#### Database (Required)
```bash
DATABASE_URL=jdbc:mysql://host:port/database
DATABASE_USERNAME=username
DATABASE_PASSWORD=password
```

#### Server (Optional - defaults provided)
```bash
SERVER_PORT=8080
SERVER_HOST=0.0.0.0
```

#### Redis Cache (Optional)
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

See [CLOUD_DEPLOYMENT_GUIDE.md](CLOUD_DEPLOYMENT_GUIDE.md) for complete list of environment variables.

## Health Checks

The application exposes Spring Boot Actuator endpoints:

- **Health**: `GET /actuator/health`
  - Returns application health status
  - Includes database connection status
  - Used by load balancers and orchestrators

- **Metrics**: `GET /actuator/metrics`
  - JVM metrics
  - HikariCP connection pool metrics
  - HTTP request metrics

- **Info**: `GET /actuator/info`
  - Application information
  - Build details

## Monitoring

### HikariCP Connection Pool
Monitor connection pool health:
- Active connections
- Idle connections
- Total connections
- Threads awaiting connection

Access via `DatabaseService.getPoolStats()` method.

### Application Metrics
Enable Prometheus metrics:
```bash
PROMETHEUS_ENABLED=true
```

Access at: `http://localhost:8080/actuator/prometheus`

## Security

### Best Practices Implemented
- ✅ No hardcoded credentials in code
- ✅ Environment variables for sensitive data
- ✅ AWS Secrets Manager integration
- ✅ Non-root container user
- ✅ Minimal container image (Alpine-based)
- ✅ Health checks for reliability
- ✅ Connection leak detection

### Production Recommendations
1. Use AWS Secrets Manager for all sensitive values
2. Enable encryption at rest and in transit
3. Use IAM roles instead of access keys
4. Implement least privilege access
5. Enable AWS CloudTrail for audit logging
6. Use VPC for network isolation
7. Enable AWS WAF for web application firewall

## Testing

### Local Testing with Docker Compose
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

### Testing with Environment Variables
```bash
# Set environment variables
export DATABASE_URL=jdbc:mysql://localhost:3306/mini_app_db
export DATABASE_USERNAME=root
export DATABASE_PASSWORD=password
export SERVER_PORT=8080

# Run application
java -jar target/mini-java-app-1.0.0.jar
```

## Troubleshooting

### Connection Pool Issues
- Check `DB_POOL_MAX_SIZE` is appropriate for your workload
- Monitor connection pool statistics
- Check for connection leaks (leak detection enabled at 60 seconds)

### Configuration Issues
- Verify all required environment variables are set
- Check application logs for configuration errors
- Use `/actuator/env` endpoint to verify configuration (disable in production)

### Container Issues
- Check container logs: `docker logs mini-java-app`
- Verify health check: `docker inspect mini-java-app`
- Check resource limits: `docker stats mini-java-app`

## Migration from Original Application

### Breaking Changes
1. **File Paths**: No longer uses absolute file paths
   - Configuration must be provided via environment variables or classpath
   
2. **Database Connections**: Now uses connection pooling
   - Direct JDBC connections replaced with HikariCP
   
3. **Configuration**: All hardcoded values removed
   - Must provide configuration via environment variables

### Migration Steps
1. Set up required environment variables
2. Configure cloud services (RDS, ElastiCache, S3)
3. Store secrets in AWS Secrets Manager
4. Deploy using provided ECS task definition
5. Configure load balancer health checks
6. Set up CloudWatch monitoring

## Performance Optimization

### JVM Settings
The Dockerfile includes optimized JVM settings for containers:
- Container-aware memory management
- G1 garbage collector
- String deduplication
- Optimized for cloud environments

### Connection Pool Tuning
Adjust HikariCP settings based on your workload:
```bash
DB_POOL_MAX_SIZE=20          # Maximum connections
DB_POOL_MIN_IDLE=5           # Minimum idle connections
DB_CONNECTION_TIMEOUT=30000  # Connection timeout (ms)
```

## Support and Documentation

- **Deployment Guide**: [CLOUD_DEPLOYMENT_GUIDE.md](CLOUD_DEPLOYMENT_GUIDE.md)
- **AWS ECS Template**: [aws-ecs-task-definition.json](aws-ecs-task-definition.json)
- **Docker Compose**: [docker-compose.yml](docker-compose.yml)

## License

Copyright © 2024. All rights reserved.

## Changelog

### Version 1.0.0 (Cloud-Ready)
- ✅ Fixed hardcoded file paths (CRITICAL)
- ✅ Implemented HikariCP connection pooling (HIGH)
- ✅ Externalized all configuration (MEDIUM)
- ✅ Added Spring Boot integration
- ✅ Added containerization support
- ✅ Added AWS deployment templates
- ✅ Added health checks and monitoring
- ✅ Added comprehensive documentation
