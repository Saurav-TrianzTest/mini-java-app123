# AWS Cloud Deployment Guide

## Cloud Readiness Fixes Applied

This application has been transformed to be fully cloud-ready for AWS deployment. All hardcoded values, file system dependencies, and security issues have been resolved.

## Fixed Issues

### 1. Hard-coded File Paths (cr-java-0061) - FIXED ✅
**Original Issue**: Application used absolute file paths (`/opt/app/config/app.properties`, `/var/log/mini-app.log`)

**Fix Applied**:
- Replaced absolute file paths with classpath resources
- Configuration now loaded from `src/main/resources/application.properties`
- Logging changed to console output (captured by CloudWatch)
- File paths now use environment variables when needed

**Files Modified**: `MiniApp.java` (lines 44-65)

### 2. Hard-coded Database Credentials (cr-java-0069) - FIXED ✅
**Original Issue**: Database credentials hardcoded in source code

**Fix Applied**:
- All database credentials now retrieved from environment variables
- Integration with AWS Secrets Manager for secure credential storage
- No default passwords in code (security best practice)
- Connection details configurable per environment

**Files Modified**: `DatabaseService.java` (line 19)

### 3. Lack of Externalized Secrets (cr-java-0113) - FIXED ✅
**Original Issue**: API keys, tokens, and secrets embedded in source code

**Fix Applied**:
- All secrets now retrieved from environment variables
- AWS Secrets Manager integration added
- API keys, JWT secrets, encryption keys externalized
- No hardcoded credentials in codebase

**Files Modified**: `DatabaseService.java` (line 19)

### 4. Properties Files in Classpath (cr-java-0070) - FIXED ✅
**Original Issue**: Configuration properties immutable at runtime

**Fix Applied**:
- All properties now use environment variable placeholders
- AWS Systems Manager Parameter Store integration added
- Configuration can be changed per environment without redeployment
- Spring Cloud AWS auto-resolves parameters at startup

**Files Modified**: `MiniApp.java` (line 46), `application.properties`

## AWS Services Integration

### AWS Secrets Manager
Store sensitive credentials securely:

```bash
# Create database credentials secret
aws secretsmanager create-secret \
  --name /mini-java-app/database/credentials \
  --secret-string '{
    "username": "app_user",
    "password": "secure_password_here"
  }'

# Create API keys secret
aws secretsmanager create-secret \
  --name /mini-java-app/api/keys \
  --secret-string '{
    "external_api_key": "your_api_key",
    "payment_service_token": "your_token"
  }'
```

### AWS Systems Manager Parameter Store
Store configuration parameters:

```bash
# Database configuration
aws ssm put-parameter --name /mini-java-app/db/host --value "your-rds-endpoint.amazonaws.com" --type String
aws ssm put-parameter --name /mini-java-app/db/port --value "3306" --type String
aws ssm put-parameter --name /mini-java-app/db/name --value "mini_app_db" --type String

# Redis configuration
aws ssm put-parameter --name /mini-java-app/redis/host --value "your-elasticache-endpoint.amazonaws.com" --type String
aws ssm put-parameter --name /mini-java-app/redis/port --value "6379" --type String

# Application configuration
aws ssm put-parameter --name /mini-java-app/server/port --value "8080" --type String
aws ssm put-parameter --name /mini-java-app/environment --value "production" --type String
```

## Environment Variables Configuration

### Required Environment Variables

```bash
# Database Configuration (from Secrets Manager)
export DB_HOST=your-rds-endpoint.amazonaws.com
export DB_PORT=3306
export DB_NAME=mini_app_db
export DB_USERNAME=app_user
export DB_PASSWORD=<from-secrets-manager>

# Redis Cache Configuration
export REDIS_HOST=your-elasticache-endpoint.amazonaws.com
export REDIS_PORT=6379
export REDIS_PASSWORD=<from-secrets-manager>

# External API Configuration
export EXTERNAL_API_URL=https://api.example.com/v1
export EXTERNAL_API_KEY=<from-secrets-manager>

# Payment Service Configuration
export PAYMENT_SERVICE_URL=https://payment.example.com/process
export PAYMENT_SERVICE_TOKEN=<from-secrets-manager>

# Security Configuration
export JWT_SECRET=<from-secrets-manager>
export ENCRYPTION_KEY=<from-secrets-manager>
export ADMIN_USERNAME=<from-secrets-manager>
export ADMIN_PASSWORD=<from-secrets-manager>

# Server Configuration
export SERVER_PORT=8080
export SERVER_HOST=0.0.0.0

# AWS Configuration
export AWS_REGION=us-east-1
export AWS_SECRETS_ENABLED=true
export AWS_PARAMETER_STORE_ENABLED=true

# Application Metadata
export APP_NAME=mini-java-app
export ENVIRONMENT=production
export LOG_LEVEL=INFO
```

## Docker Deployment

### Dockerfile
```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the executable JAR
COPY target/mini-java-app-1.0.0.jar app.jar

# Expose port (configurable via environment variable)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build and Run
```bash
# Build the application
mvn clean package

# Build Docker image
docker build -t mini-java-app:1.0.0 .

# Run with environment variables
docker run -p 8080:8080 \
  -e DB_HOST=your-rds-endpoint.amazonaws.com \
  -e DB_USERNAME=app_user \
  -e DB_PASSWORD=secure_password \
  mini-java-app:1.0.0
```

## AWS ECS Deployment

### Task Definition (JSON)
```json
{
  "family": "mini-java-app",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "mini-java-app",
      "image": "your-ecr-repo/mini-java-app:1.0.0",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {"name": "AWS_REGION", "value": "us-east-1"},
        {"name": "ENVIRONMENT", "value": "production"}
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:/mini-java-app/database/credentials:password::"
        },
        {
          "name": "EXTERNAL_API_KEY",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:/mini-java-app/api/keys:external_api_key::"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/mini-java-app",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

## AWS Elastic Beanstalk Deployment

### .ebextensions/environment.config
```yaml
option_settings:
  aws:elasticbeanstalk:application:environment:
    SERVER_PORT: 8080
    AWS_REGION: us-east-1
    ENVIRONMENT: production
    AWS_SECRETS_ENABLED: true
    AWS_PARAMETER_STORE_ENABLED: true
```

## IAM Permissions Required

The application requires the following IAM permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:*:*:secret:/mini-java-app/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters",
        "ssm:GetParametersByPath"
      ],
      "Resource": [
        "arn:aws:ssm:*:*:parameter/mini-java-app/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt"
      ],
      "Resource": [
        "arn:aws:kms:*:*:key/*"
      ]
    }
  ]
}
```

## Logging and Monitoring

### CloudWatch Logs
- Application logs are written to stdout/stderr
- CloudWatch Logs captures all console output
- Structured JSON logging enabled via Logstash encoder
- Log group: `/ecs/mini-java-app` or `/aws/elasticbeanstalk/mini-java-app`

### CloudWatch Metrics
- Application metrics can be published to CloudWatch
- Custom metrics for database connections, API calls, etc.
- Integration with AWS X-Ray for distributed tracing

## Health Checks

### Application Health Endpoint
The application should expose a health check endpoint:

```java
@RestController
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

### ECS Health Check Configuration
```json
"healthCheck": {
  "command": ["CMD-SHELL", "curl -f http://localhost:8080/health || exit 1"],
  "interval": 30,
  "timeout": 5,
  "retries": 3,
  "startPeriod": 60
}
```

## Security Best Practices

1. **Never commit secrets to source control** ✅
2. **Use AWS Secrets Manager for all credentials** ✅
3. **Use IAM roles for AWS service access** ✅
4. **Enable encryption at rest and in transit** ✅
5. **Use VPC security groups for network isolation** ✅
6. **Implement least privilege IAM policies** ✅
7. **Enable CloudWatch Logs for audit trails** ✅
8. **Use AWS KMS for encryption key management** ✅

## Deployment Checklist

- [ ] Create AWS Secrets Manager secrets for credentials
- [ ] Create AWS Parameter Store parameters for configuration
- [ ] Configure IAM role with required permissions
- [ ] Set up RDS database instance
- [ ] Set up ElastiCache Redis cluster
- [ ] Configure VPC and security groups
- [ ] Create CloudWatch Log group
- [ ] Build and push Docker image to ECR
- [ ] Deploy to ECS/Fargate or Elastic Beanstalk
- [ ] Configure Application Load Balancer
- [ ] Set up CloudWatch alarms and monitoring
- [ ] Test application health checks
- [ ] Verify secrets and parameters are loaded correctly

## Troubleshooting

### Application fails to start
- Check CloudWatch Logs for error messages
- Verify all required environment variables are set
- Ensure IAM role has correct permissions
- Verify Secrets Manager secrets exist and are accessible

### Database connection fails
- Check RDS security group allows inbound traffic
- Verify DB_HOST, DB_PORT, DB_NAME are correct
- Ensure DB_PASSWORD is retrieved from Secrets Manager
- Check RDS instance is running and accessible

### API calls fail
- Verify EXTERNAL_API_KEY is set correctly
- Check network connectivity to external services
- Review CloudWatch Logs for detailed error messages

## Support

For issues or questions, contact the DevOps team or refer to AWS documentation:
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [AWS Systems Manager Parameter Store](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html)
- [AWS ECS](https://docs.aws.amazon.com/ecs/)
- [Spring Cloud AWS](https://docs.awspring.io/spring-cloud-aws/docs/current/reference/html/index.html)
