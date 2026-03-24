# Cloud Deployment Guide - Mini Java App

## Overview
This application has been modernized for cloud deployment with the following improvements:
- ✅ Replaced hardcoded file paths with environment variables
- ✅ Replaced direct JDBC connections with HikariCP connection pooling
- ✅ Externalized all configuration to environment variables
- ✅ Removed file system dependencies
- ✅ Added cloud-native logging (console output)
- ✅ Made application stateless and container-ready

## Cloud Readiness Issues Fixed

### 1. Hard-coded File Paths (CRITICAL) ✅
**Issue**: Application used absolute file paths (`/opt/app/config/app.properties`, `/var/log/mini-app.log`)
**Fix**: 
- Replaced with classpath resources and environment variables
- Configuration loaded from `classpath:application.properties`
- Logging uses console output (captured by CloudWatch, Stackdriver, etc.)
- File paths configurable via environment variables

### 2. Direct JDBC Connections (HIGH) ✅
**Issue**: Application used direct JDBC connections without connection pooling
**Fix**:
- Implemented HikariCP connection pool
- Added connection timeout and lifecycle management
- Configured pool size via environment variables
- Added connection leak detection

### 3. Properties Files in Classpath (MEDIUM) ✅
**Issue**: Configuration was immutable and packaged in JAR
**Fix**:
- All properties use environment variable references
- Support for Spring profiles (dev, staging, prod, aws)
- Configuration can be overridden without rebuilding
- AWS-specific profile for cloud services

## Environment Variables

### Required Environment Variables
```bash
# Database Configuration
export DB_HOST=your-database-host
export DB_PORT=3306
export DB_NAME=mini_app_db
export DB_USERNAME=your-db-user
export DB_PASSWORD=your-db-password

# Server Configuration
export SERVER_PORT=8080
export SERVER_HOST=0.0.0.0
```

### Optional Environment Variables
```bash
# Connection Pool
export DB_POOL_MAX_SIZE=10
export DB_POOL_MIN_IDLE=2
export DB_CONNECTION_TIMEOUT=30000

# Redis Cache
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password

# External Services
export EXTERNAL_API_URL=https://api.example.com/v1
export PAYMENT_SERVICE_URL=https://payment.example.com/process

# Security (use AWS Secrets Manager in production)
export JWT_SECRET=your-jwt-secret
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=your-admin-password

# Logging
export LOG_LEVEL=INFO
export APP_LOG_LEVEL=DEBUG
```

## AWS Deployment

### AWS ECS Deployment

1. **Build Docker Image**
```bash
docker build -t mini-java-app:latest .
```

2. **Push to ECR**
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag mini-java-app:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

3. **Create ECS Task Definition**
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
        {"name": "SPRING_PROFILES_ACTIVE", "value": "aws"},
        {"name": "DB_HOST", "value": "your-rds-endpoint.rds.amazonaws.com"},
        {"name": "DB_PORT", "value": "3306"},
        {"name": "DB_NAME", "value": "mini_app_db"},
        {"name": "REDIS_HOST", "value": "your-elasticache-endpoint.cache.amazonaws.com"}
      ],
      "secrets": [
        {"name": "DB_USERNAME", "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-username"},
        {"name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"}
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

### AWS EKS Deployment

1. **Create ConfigMap**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mini-app-config
data:
  SPRING_PROFILES_ACTIVE: "aws"
  DB_HOST: "your-rds-endpoint.rds.amazonaws.com"
  DB_PORT: "3306"
  DB_NAME: "mini_app_db"
  SERVER_PORT: "8080"
```

2. **Create Secret**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mini-app-secrets
type: Opaque
stringData:
  DB_USERNAME: "your-db-user"
  DB_PASSWORD: "your-db-password"
  JWT_SECRET: "your-jwt-secret"
```

3. **Create Deployment**
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
        image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: mini-app-config
        - secretRef:
            name: mini-app-secrets
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
```

### AWS RDS Configuration

1. **Create RDS MySQL Instance**
```bash
aws rds create-db-instance \
  --db-instance-identifier mini-app-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --master-username admin \
  --master-user-password <password> \
  --allocated-storage 20 \
  --vpc-security-group-ids sg-xxxxx \
  --db-subnet-group-name my-subnet-group
```

2. **Get RDS Endpoint**
```bash
aws rds describe-db-instances \
  --db-instance-identifier mini-app-db \
  --query 'DBInstances[0].Endpoint.Address'
```

### AWS ElastiCache Configuration

1. **Create ElastiCache Redis Cluster**
```bash
aws elasticache create-cache-cluster \
  --cache-cluster-id mini-app-cache \
  --cache-node-type cache.t3.micro \
  --engine redis \
  --num-cache-nodes 1 \
  --security-group-ids sg-xxxxx
```

## Docker Support

### Dockerfile
```dockerfile
FROM openjdk:11-jre-slim
WORKDIR /app
COPY target/mini-java-app-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml (for local testing)
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=mini_app_db
      - DB_USERNAME=root
      - DB_PASSWORD=password
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    depends_on:
      - mysql
      - redis
  
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=password
      - MYSQL_DATABASE=mini_app_db
    ports:
      - "3306:3306"
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

## Health Checks

The application exposes Spring Boot Actuator endpoints:
- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`

## Monitoring

### AWS CloudWatch
- Application logs are sent to CloudWatch Logs
- Configure log group: `/aws/ecs/mini-java-app`
- Metrics available via CloudWatch Container Insights

### Application Metrics
- HikariCP pool metrics available via JMX
- Spring Boot Actuator metrics endpoint
- Custom application metrics

## Security Best Practices

1. **Use AWS Secrets Manager** for sensitive data
2. **Enable VPC** for database and cache access
3. **Use IAM roles** instead of access keys
4. **Enable SSL/TLS** for database connections
5. **Use security groups** to restrict network access
6. **Enable encryption** at rest and in transit

## Troubleshooting

### Connection Pool Issues
```bash
# Check pool metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.idle
```

### Database Connection Issues
```bash
# Test database connectivity
export DB_HOST=your-rds-endpoint
export DB_USERNAME=admin
export DB_PASSWORD=your-password
mysql -h $DB_HOST -u $DB_USERNAME -p$DB_PASSWORD
```

### View Application Logs
```bash
# AWS ECS
aws logs tail /ecs/mini-java-app --follow

# Kubernetes
kubectl logs -f deployment/mini-java-app
```

## Migration Checklist

- [x] Replace hardcoded file paths with environment variables
- [x] Implement HikariCP connection pooling
- [x] Externalize all configuration
- [x] Remove file system dependencies
- [x] Add cloud-native logging
- [x] Create AWS-specific configuration profile
- [x] Add health check endpoints
- [x] Document environment variables
- [x] Create deployment guides
- [ ] Set up AWS Secrets Manager
- [ ] Configure CloudWatch alarms
- [ ] Set up auto-scaling policies
- [ ] Configure backup strategies

## Support

For issues or questions, contact the DevOps team or refer to:
- AWS ECS Documentation: https://docs.aws.amazon.com/ecs/
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- HikariCP Documentation: https://github.com/brettwooldridge/HikariCP
