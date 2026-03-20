# Cloud Deployment Guide - Mini Java Application

## Overview
This application has been transformed to be fully cloud-ready and follows cloud-native best practices for deployment on AWS, Azure, and GCP.

## Cloud Readiness Fixes Applied

### 1. Hard-coded File Paths (CRITICAL) ✅
**Issue**: Application used absolute file paths (`/opt/app/config/app.properties`, `/var/log/mini-app.log`)

**Fix Applied**:
- Replaced hardcoded file paths with classpath resources
- Configuration loaded from `application.properties` in classpath
- Logging redirected to console output (stdout/stderr)
- Cloud platforms automatically capture console logs (CloudWatch, Stackdriver, Azure Monitor)

**Files Modified**:
- `src/main/java/com/test/MiniApp.java`

### 2. Direct JDBC Connections (HIGH) ✅
**Issue**: Application used direct JDBC connections without connection pooling

**Fix Applied**:
- Implemented HikariCP connection pooling
- Added Spring Boot JDBC starter with auto-configuration
- Configured connection pool settings optimized for cloud environments
- Added connection leak detection and health checks
- Proper connection lifecycle management

**Files Modified**:
- `src/main/java/com/test/DatabaseService.java`
- `pom.xml` (added HikariCP and Spring Boot JDBC dependencies)

### 3. Properties Files in Classpath (MEDIUM) ✅
**Issue**: Configuration properties were hardcoded in classpath, preventing environment-specific changes

**Fix Applied**:
- Externalized all configuration to environment variables
- Used Spring Boot's `${VAR:default}` syntax for environment variable injection
- Created `.env.template` for easy environment setup
- Configuration follows 12-factor app principles
- Supports multiple deployment environments without code changes

**Files Modified**:
- `src/main/resources/application.properties`
- Created `.env.template`

## Architecture Changes

### Before (Not Cloud-Ready)
```
❌ Hardcoded file paths: /opt/app/config/app.properties
❌ Direct JDBC connections: DriverManager.getConnection()
❌ Hardcoded credentials in properties files
❌ Fixed port numbers: 8080
❌ File-based logging: /var/log/mini-app.log
```

### After (Cloud-Ready)
```
✅ Classpath resources: getResourceAsStream("application.properties")
✅ HikariCP connection pooling with Spring Boot
✅ Environment variable configuration: ${DB_HOST:localhost}
✅ Configurable ports: ${SERVER_PORT:8080}
✅ Console logging for cloud log aggregation
✅ Spring Boot Actuator for health checks
✅ Docker containerization support
✅ AWS ECS/Fargate ready
```

## Deployment Instructions

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Docker (for containerized deployment)
- AWS CLI (for AWS deployment)

### Local Development

1. **Set Environment Variables**:
```bash
cp .env.template .env
# Edit .env with your local configuration
source .env
```

2. **Build the Application**:
```bash
mvn clean package
```

3. **Run Locally**:
```bash
java -jar target/mini-java-app-1.0.0.jar
```

### Docker Deployment

1. **Build Docker Image**:
```bash
docker build -t mini-java-app:latest .
```

2. **Run Container**:
```bash
docker run -p 8080:8080 \
  -e DB_HOST=your-db-host \
  -e DB_USERNAME=your-username \
  -e DB_PASSWORD=your-password \
  -e REDIS_HOST=your-redis-host \
  mini-java-app:latest
```

### AWS ECS/Fargate Deployment

1. **Create ECR Repository**:
```bash
aws ecr create-repository --repository-name mini-java-app --region us-east-1
```

2. **Build and Push Image**:
```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Build and tag
docker build -t mini-java-app:latest .
docker tag mini-java-app:latest ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest

# Push to ECR
docker push ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

3. **Store Secrets in AWS Secrets Manager**:
```bash
# Database credentials
aws secretsmanager create-secret \
  --name mini-app/db-password \
  --secret-string "your-db-password" \
  --region us-east-1

# JWT secret
aws secretsmanager create-secret \
  --name mini-app/jwt-secret \
  --secret-string "your-jwt-secret-key" \
  --region us-east-1

# API keys
aws secretsmanager create-secret \
  --name mini-app/api-key \
  --secret-string "your-api-key" \
  --region us-east-1
```

4. **Create ECS Task Definition**:
```bash
# Update aws-ecs-task-definition.json with your ACCOUNT_ID and REGION
aws ecs register-task-definition \
  --cli-input-json file://aws-ecs-task-definition.json
```

5. **Create ECS Service**:
```bash
aws ecs create-service \
  --cluster your-cluster-name \
  --service-name mini-java-app \
  --task-definition mini-java-app \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
```

### AWS Elastic Beanstalk Deployment

1. **Initialize Elastic Beanstalk**:
```bash
eb init -p docker mini-java-app --region us-east-1
```

2. **Create Environment**:
```bash
eb create mini-java-app-prod \
  --instance-type t3.medium \
  --envvars DB_HOST=your-db-host,DB_USERNAME=your-username
```

3. **Deploy**:
```bash
eb deploy
```

### Kubernetes Deployment

1. **Create ConfigMap**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mini-app-config
data:
  SERVER_PORT: "8080"
  CLOUD_PLATFORM: "aws"
  CLOUD_REGION: "us-east-1"
```

2. **Create Secret**:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mini-app-secrets
type: Opaque
stringData:
  DB_PASSWORD: "your-db-password"
  JWT_SECRET: "your-jwt-secret"
```

3. **Deploy Application**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mini-java-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mini-java-app
  template:
    metadata:
      labels:
        app: mini-java-app
    spec:
      containers:
      - name: mini-java-app
        image: ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: mini-app-config
        - secretRef:
            name: mini-app-secrets
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

## Environment Variables Reference

### Required Variables
- `DB_HOST`: Database hostname
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: JWT signing secret (min 256 bits)

### Optional Variables (with defaults)
- `SERVER_PORT`: Server port (default: 8080)
- `DB_PORT`: Database port (default: 3306)
- `DB_NAME`: Database name (default: mini_app_db)
- `REDIS_HOST`: Redis hostname (default: localhost)
- `LOG_LEVEL`: Logging level (default: INFO)

See `.env.template` for complete list.

## Health Checks

The application exposes Spring Boot Actuator endpoints:

- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Info**: `GET /actuator/info`

Configure your load balancer to use `/actuator/health` for health checks.

## Monitoring and Logging

### AWS CloudWatch
- Container logs automatically sent to CloudWatch Logs
- Configure log group: `/ecs/mini-java-app`
- Set up CloudWatch alarms for health check failures

### Application Metrics
- Prometheus metrics available at `/actuator/prometheus`
- Integrate with CloudWatch Container Insights
- Set up custom metrics for business KPIs

## Security Best Practices

1. **Never commit secrets to version control**
2. **Use AWS Secrets Manager or Parameter Store for sensitive data**
3. **Enable encryption at rest and in transit**
4. **Use IAM roles instead of hardcoded credentials**
5. **Implement least privilege access**
6. **Enable VPC security groups and NACLs**
7. **Use HTTPS/TLS for all external communication**

## Troubleshooting

### Application won't start
- Check environment variables are set correctly
- Verify database connectivity
- Check CloudWatch logs for errors

### Database connection failures
- Verify security group allows traffic from ECS tasks
- Check database credentials in Secrets Manager
- Verify RDS instance is in same VPC

### Health check failures
- Increase health check grace period
- Check application logs for startup errors
- Verify `/actuator/health` endpoint is accessible

## Support

For issues or questions, contact the DevOps team or refer to the internal wiki.

## License

Internal use only - Proprietary
