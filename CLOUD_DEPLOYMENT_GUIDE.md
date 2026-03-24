# Cloud Deployment Guide - Mini Java Application

## Overview
This application has been transformed to be fully cloud-ready and follows 12-factor app principles. All hardcoded values have been externalized to environment variables, and the application now uses cloud-native patterns.

## Cloud Readiness Fixes Applied

### 1. Hard-coded File Paths (CRITICAL) - FIXED ✅
**Issue**: Application contained absolute file paths (`/opt/app/config/app.properties`, `/var/log/mini-app.log`)

**Fix Applied**:
- Replaced absolute file paths with classpath resources
- Configuration now loaded from `application.properties` in classpath
- Logging changed to console output (captured by cloud platforms)
- Removed all file system dependencies

**Environment Variables**:
- No file paths needed - configuration is in classpath or environment variables

### 2. Direct JDBC Connections (HIGH) - FIXED ✅
**Issue**: Application used direct JDBC connections without connection pooling

**Fix Applied**:
- Implemented HikariCP connection pooling
- Added Spring Boot Data JPA with auto-configuration
- Proper connection lifecycle management
- Connection pool monitoring and leak detection

**Environment Variables**:
```bash
DATABASE_URL=jdbc:mysql://your-rds-endpoint:3306/mini_app_db
DATABASE_USERNAME=your_db_user
DATABASE_PASSWORD=your_db_password
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
DB_CONNECTION_TIMEOUT=30000
```

### 3. Properties Files in Classpath (MEDIUM) - FIXED ✅
**Issue**: Configuration properties were immutable at runtime

**Fix Applied**:
- All properties now use environment variable placeholders
- Configuration can be overridden without rebuilding
- Supports Spring profiles for environment-specific configuration
- External configuration via command-line arguments

**Environment Variables**: See complete list below

## AWS Deployment Instructions

### Prerequisites
- AWS Account with appropriate permissions
- AWS CLI configured
- Docker installed (for containerization)

### Option 1: AWS Elastic Beanstalk

1. **Package the application**:
```bash
mvn clean package
```

2. **Create Elastic Beanstalk application**:
```bash
eb init -p "Corretto 11" mini-java-app --region us-east-1
```

3. **Set environment variables**:
```bash
eb setenv \
  DATABASE_URL=jdbc:mysql://your-rds.amazonaws.com:3306/mini_app_db \
  DATABASE_USERNAME=admin \
  DATABASE_PASSWORD=your_password \
  SERVER_PORT=5000 \
  REDIS_HOST=your-elasticache.amazonaws.com \
  REDIS_PORT=6379
```

4. **Deploy**:
```bash
eb create mini-java-app-env
eb deploy
```

### Option 2: AWS ECS (Elastic Container Service)

1. **Create Dockerfile**:
```dockerfile
FROM amazoncorretto:11
WORKDIR /app
COPY target/mini-java-app-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

2. **Build and push to ECR**:
```bash
aws ecr create-repository --repository-name mini-java-app
docker build -t mini-java-app .
docker tag mini-java-app:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

3. **Create ECS Task Definition** with environment variables:
```json
{
  "family": "mini-java-app",
  "containerDefinitions": [
    {
      "name": "mini-java-app",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest",
      "memory": 512,
      "cpu": 256,
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {"name": "DATABASE_URL", "value": "jdbc:mysql://your-rds.amazonaws.com:3306/mini_app_db"},
        {"name": "DATABASE_USERNAME", "value": "admin"},
        {"name": "REDIS_HOST", "value": "your-elasticache.amazonaws.com"},
        {"name": "SERVER_PORT", "value": "8080"}
      ],
      "secrets": [
        {"name": "DATABASE_PASSWORD", "valueFrom": "arn:aws:secretsmanager:us-east-1:account:secret:db-password"},
        {"name": "JWT_SECRET", "valueFrom": "arn:aws:secretsmanager:us-east-1:account:secret:jwt-secret"}
      ]
    }
  ]
}
```

### Option 3: AWS Lambda (with Spring Cloud Function)

For serverless deployment, additional modifications would be needed to use Spring Cloud Function adapter.

## Required Environment Variables

### Database Configuration
```bash
DATABASE_URL=jdbc:mysql://host:port/database
DATABASE_USERNAME=username
DATABASE_PASSWORD=password
DATABASE_HOST=localhost
DATABASE_PORT=3306
DATABASE_NAME=mini_app_db
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
DB_CONNECTION_TIMEOUT=30000
```

### Server Configuration
```bash
SERVER_PORT=8080
SERVER_HOST=0.0.0.0
SERVER_CONTEXT_PATH=/mini-app
```

### Redis Cache Configuration
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
```

### External API Configuration
```bash
EXTERNAL_API_URL=http://api.example.com/v1
EXTERNAL_API_KEY=your_api_key
EXTERNAL_API_TIMEOUT=30000
```

### Payment Service Configuration
```bash
PAYMENT_SERVICE_URL=https://payment.example.com/process
PAYMENT_SERVICE_USERNAME=username
PAYMENT_SERVICE_PASSWORD=password
```

### Security Configuration
```bash
JWT_SECRET=your_jwt_secret_key
ADMIN_USERNAME=admin
ADMIN_PASSWORD=secure_password
ENCRYPTION_KEY=your_encryption_key
```

### Monitoring Configuration
```bash
MONITORING_ENDPOINT=http://monitoring.example.com/metrics
MONITORING_USERNAME=monitor_user
MONITORING_PASSWORD=monitor_password
```

### Messaging Configuration
```bash
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=rabbitmq_user
RABBITMQ_PASSWORD=rabbitmq_password
```

### Cloud Storage Configuration
```bash
CLOUD_STORAGE_BUCKET=mini-app-storage
CLOUD_STORAGE_REGION=us-east-1
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
```

### Application Configuration
```bash
SPRING_PROFILE=production
APP_ENVIRONMENT=production
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
```

## AWS Services Integration

### Amazon RDS (Database)
1. Create RDS MySQL instance
2. Set `DATABASE_URL` to RDS endpoint
3. Use RDS IAM authentication for enhanced security

### Amazon ElastiCache (Redis)
1. Create ElastiCache Redis cluster
2. Set `REDIS_HOST` to ElastiCache endpoint
3. Configure security groups for access

### AWS Secrets Manager (Secrets)
1. Store sensitive values in Secrets Manager
2. Reference in ECS task definition using `secrets` field
3. Application will automatically retrieve values

### Amazon S3 (File Storage)
1. Create S3 bucket for file storage
2. Set `CLOUD_STORAGE_BUCKET` environment variable
3. Use AWS SDK for S3 operations (replace local file operations)

### AWS CloudWatch (Logging & Monitoring)
- Console logs automatically captured
- Set up CloudWatch alarms for metrics
- Use CloudWatch Insights for log analysis

## Health Checks

The application exposes Spring Boot Actuator endpoints:

- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`

Configure your load balancer to use `/actuator/health` for health checks.

## Monitoring

### HikariCP Connection Pool Monitoring
The application provides connection pool statistics via the `DatabaseService.getPoolStats()` method.

Monitor these metrics:
- Active connections
- Idle connections
- Total connections
- Threads awaiting connection

### Application Metrics
Enable Prometheus metrics export:
```bash
PROMETHEUS_ENABLED=true
```

Access metrics at: `http://localhost:8080/actuator/prometheus`

## Troubleshooting

### Connection Pool Issues
- Check `DB_POOL_MAX_SIZE` is appropriate for your workload
- Monitor connection pool statistics
- Check for connection leaks (leak detection enabled at 60 seconds)

### Configuration Issues
- Verify all required environment variables are set
- Check CloudWatch logs for configuration errors
- Use `/actuator/env` endpoint to verify configuration (disable in production)

### Performance Issues
- Adjust HikariCP pool settings
- Monitor database connection metrics
- Check CloudWatch metrics for bottlenecks

## Security Best Practices

1. **Never commit secrets to version control**
2. **Use AWS Secrets Manager for sensitive values**
3. **Enable encryption at rest and in transit**
4. **Use IAM roles instead of access keys when possible**
5. **Implement least privilege access**
6. **Enable AWS CloudTrail for audit logging**
7. **Use VPC for network isolation**
8. **Enable AWS WAF for web application firewall**

## Cost Optimization

1. **Right-size your instances** based on actual usage
2. **Use Auto Scaling** to handle variable load
3. **Enable RDS and ElastiCache reserved instances** for predictable workloads
4. **Use S3 lifecycle policies** for old data
5. **Monitor CloudWatch metrics** to identify optimization opportunities

## Next Steps

1. Set up CI/CD pipeline (AWS CodePipeline, GitHub Actions)
2. Implement blue-green deployment strategy
3. Set up automated testing in deployment pipeline
4. Configure auto-scaling policies
5. Implement distributed tracing (AWS X-Ray)
6. Set up centralized logging (CloudWatch Logs Insights)
7. Implement disaster recovery plan

## Support

For issues or questions:
- Check CloudWatch logs for error messages
- Review application metrics in CloudWatch
- Verify environment variables are correctly set
- Check security group and network configuration
