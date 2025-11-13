# Mini Java App - AWS ECS Fargate Deployment Guide

This guide provides comprehensive instructions for deploying the Mini Java App to AWS ECS Fargate.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
4. [Build and Push Docker Image](#build-and-push-docker-image)
5. [ECS Fargate Deployment](#ecs-fargate-deployment)
6. [Configuration Management](#configuration-management)
7. [Monitoring and Logging](#monitoring-and-logging)
8. [Scaling and Management](#scaling-and-management)
9. [Troubleshooting](#troubleshooting)
10. [Security Considerations](#security-considerations)

## Prerequisites

### System Requirements

- **Java**: JDK 11 or higher
- **Maven**: 3.6+ (for building locally)
- **Docker**: 20.10+ with Docker Compose
- **AWS CLI**: 2.0+ configured with appropriate permissions
- **Git**: For version control

### Java Application Details

- **Framework**: Spring Boot 2.7.0
- **Java Version**: 11
- **Build Tool**: Maven
- **Package Type**: JAR
- **Default Port**: 8080
- **Context Path**: `/mini-app`
- **Health Endpoint**: `/mini-app/actuator/health`

## Local Development Setup

### 1. Clone and Build

```bash
# Build the application
mvn clean package -DskipTests

# Run locally
java -jar target/mini-java-app-1.0.0.jar
```

### 2. Docker Development

```bash
# Build Docker image
docker build -t mini-java-app:latest .

# Run with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f mini-java-app

# Stop services
docker-compose down
```

### 3. Environment Variables

The application supports the following environment variables:

#### Server Configuration
- `SERVER_PORT`: Application port (default: 8080)
- `SERVER_HOST`: Bind address (default: 0.0.0.0)

#### Database Configuration
- `DB_HOST`: Database hostname
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `DB_POOL_MAX_CONNECTIONS`: Max connection pool size
- `DB_POOL_TIMEOUT`: Connection timeout

#### Cache Configuration
- `REDIS_HOST`: Redis hostname
- `REDIS_PASSWORD`: Redis password

#### Application Directories
- `APP_CONFIG_DIR`: Configuration directory (/app/config)
- `APP_LOG_DIR`: Log directory (/app/logs)
- `APP_TEMP_DIR`: Temporary files directory (/app/temp)
- `APP_UPLOAD_DIR`: Upload directory (/app/uploads)

## AWS ECS Fargate Prerequisites

### 1. AWS Account Setup

#### Required AWS Services
- **Amazon ECS**: Container orchestration
- **Amazon ECR**: Container registry (optional)
- **Amazon VPC**: Network isolation
- **AWS Systems Manager**: Parameter Store for secrets
- **Amazon CloudWatch**: Logging and monitoring
- **Elastic Load Balancing**: Load balancer (optional)

#### IAM Roles

Create the following IAM roles:

##### ECS Task Execution Role
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

Attach policies:
- `AmazonECSTaskExecutionRolePolicy`
- Custom policy for SSM Parameter Store access

##### ECS Task Role (Optional)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

### 2. Network Configuration

#### VPC Setup
Ensure you have:
- VPC with at least 2 availability zones
- Public subnets for Fargate tasks (if using public IP)
- Private subnets for database/cache resources
- Internet Gateway for public subnets
- NAT Gateway for private subnets (if needed)

#### Security Groups
Create security groups with appropriate rules:

##### Application Security Group
```bash
# Allow HTTP traffic on port 8080
aws ec2 authorize-security-group-ingress \
    --group-id sg-xxxxxxxxx \
    --protocol tcp \
    --port 8080 \
    --cidr 0.0.0.0/0

# Allow ALB traffic (if using load balancer)
aws ec2 authorize-security-group-ingress \
    --group-id sg-xxxxxxxxx \
    --protocol tcp \
    --port 8080 \
    --source-group sg-alb-xxxxxxxxx
```

### 3. CloudWatch Log Group

```bash
# Create log group
aws logs create-log-group --log-group-name "/ecs/mini-java-app"

# Set retention policy (optional)
aws logs put-retention-policy \
    --log-group-name "/ecs/mini-java-app" \
    --retention-in-days 14
```

## Build and Push Docker Image

### 1. Using Build Script (Recommended)

#### Linux/macOS
```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

#### Windows
```batch
scripts\build-push.bat
```

### 2. Manual Build Process

#### AWS ECR
```bash
# Get ECR login token
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com

# Create ECR repository
aws ecr create-repository --repository-name mini-java-app

# Build and tag image
docker build -t mini-java-app:latest .
docker tag mini-java-app:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest

# Push image
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

#### Docker Hub
```bash
# Login to Docker Hub
docker login

# Build and tag image
docker build -t username/mini-java-app:latest .

# Push image
docker push username/mini-java-app:latest
```

## ECS Fargate Deployment

### 1. Using Deployment Script (Recommended)

#### Linux/macOS
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows
```batch
scripts\deploy-image.bat
```

### 2. Manual Deployment Process

#### Step 1: Create ECS Cluster
```bash
aws ecs create-cluster --cluster-name mini-java-app-cluster
```

#### Step 2: Register Task Definition
```bash
# Update ecs/task-definition.json with your image URI
aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json
```

#### Step 3: Create ECS Service
```bash
# Update ecs/service-definition.json with your network configuration
aws ecs create-service --cli-input-json file://ecs/service-definition.json
```

#### Step 4: Verify Deployment
```bash
# Check service status
aws ecs describe-services --cluster mini-java-app-cluster --services mini-java-app-service

# Check running tasks
aws ecs list-tasks --cluster mini-java-app-cluster --service-name mini-java-app-service
```

### 3. ECS Task Definition Explained

#### Fargate Configuration
- **CPU**: 512 (.5 vCPU) - Valid values: 256, 512, 1024, 2048, 4096
- **Memory**: 1024 MB - Must be valid combination with CPU
- **Network Mode**: awsvpc (required for Fargate)
- **Launch Type**: FARGATE

#### Container Configuration
- **Image**: Your Docker image URI
- **Port Mapping**: Container port 8080
- **Essential**: true (container failure causes task failure)
- **Health Check**: Uses netcat to check port availability
- **Logging**: CloudWatch logs with awslogs driver

#### Environment Variables vs Secrets
- **Environment**: Non-sensitive configuration
- **Secrets**: Sensitive data from SSM Parameter Store

### 4. ECS Service Configuration

#### Service Settings
- **Desired Count**: 2 (for high availability)
- **Launch Type**: FARGATE
- **Network**: awsvpc with public IP assignment
- **Deployment**: Rolling update with circuit breaker

#### Load Balancer Integration
- **Target Group**: IP target type (required for Fargate)
- **Health Check**: Custom path `/mini-app/actuator/health`
- **Grace Period**: 300 seconds for application startup

## Configuration Management

### 1. SSM Parameter Store

Store sensitive configuration in AWS Systems Manager:

```bash
# Database configuration
aws ssm put-parameter --name "/mini-java-app/db-host" --value "your-rds-endpoint" --type "SecureString"
aws ssm put-parameter --name "/mini-java-app/db-username" --value "admin" --type "SecureString"
aws ssm put-parameter --name "/mini-java-app/db-password" --value "secure-password" --type "SecureString"

# Cache configuration
aws ssm put-parameter --name "/mini-java-app/redis-host" --value "your-redis-endpoint" --type "SecureString"
aws ssm put-parameter --name "/mini-java-app/redis-password" --value "redis-password" --type "SecureString"

# API keys and secrets
aws ssm put-parameter --name "/mini-java-app/external-api-key" --value "your-api-key" --type "SecureString"
aws ssm put-parameter --name "/mini-java-app/jwt-secret" --value "jwt-secret-key" --type "SecureString"
aws ssm put-parameter --name "/mini-java-app/admin-username" --value "admin" --type "SecureString"
aws ssm put-parameter --name "/mini-java-app/admin-password" --value "admin-password" --type "SecureString"
```

### 2. Environment-Specific Configuration

Use different parameter paths for environments:
- Development: `/mini-java-app/dev/`
- Staging: `/mini-java-app/staging/`
- Production: `/mini-java-app/prod/`

### 3. Configuration Validation

The application validates required environment variables at startup. Missing configuration will cause the container to fail health checks.

## Monitoring and Logging

### 1. CloudWatch Logs

#### Access Application Logs
```bash
# View real-time logs
aws logs tail /ecs/mini-java-app --follow

# View specific time range
aws logs filter-events --log-group-name "/ecs/mini-java-app" \
    --start-time 1609459200000 --end-time 1609462800000
```

#### Log Configuration
- **Log Group**: `/ecs/mini-java-app`
- **Log Stream**: `ecs/{container-name}/{task-id}`
- **Retention**: 14 days (configurable)

### 2. Container Insights

Enable ECS Container Insights for detailed metrics:

```bash
aws ecs put-account-setting --name "containerInsights" --value "enabled"
```

### 3. Health Monitoring

#### Application Health Checks
- **Container Health**: Netcat check on port 8080
- **ALB Health**: HTTP GET to `/mini-app/actuator/health`
- **ECS Service Health**: Based on container and ALB health

#### Custom Metrics
Implement custom CloudWatch metrics in your application:
- JVM metrics (heap, GC)
- Application metrics (request count, response time)
- Business metrics (user sessions, transactions)

## Scaling and Management

### 1. Manual Scaling

```bash
# Scale service to 5 instances
aws ecs update-service --cluster mini-java-app-cluster \
    --service mini-java-app-service --desired-count 5
```

### 2. Auto Scaling

#### Create Auto Scaling Target
```bash
aws application-autoscaling register-scalable-target \
    --service-namespace ecs \
    --resource-id service/mini-java-app-cluster/mini-java-app-service \
    --scalable-dimension ecs:service:DesiredCount \
    --min-capacity 2 \
    --max-capacity 10
```

#### Create Scaling Policies
```bash
# Scale up policy
aws application-autoscaling put-scaling-policy \
    --policy-name mini-java-app-scale-up \
    --service-namespace ecs \
    --resource-id service/mini-java-app-cluster/mini-java-app-service \
    --scalable-dimension ecs:service:DesiredCount \
    --policy-type TargetTrackingScaling \
    --target-tracking-scaling-policy-configuration file://scale-up-policy.json
```

### 3. Rolling Deployments

#### Update Service with New Image
```bash
# Register new task definition
aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json

# Update service
aws ecs update-service --cluster mini-java-app-cluster \
    --service mini-java-app-service \
    --task-definition mini-java-app-task:2
```

#### Blue/Green Deployments
Use AWS CodeDeploy with ECS for blue/green deployments:
- Zero-downtime deployments
- Automatic rollback on failure
- Traffic shifting control

## Troubleshooting

### 1. Common ECS Issues

#### Task Fails to Start
```bash
# Check task definition
aws ecs describe-task-definition --task-definition mini-java-app-task

# Check task logs
aws ecs describe-tasks --cluster mini-java-app-cluster --tasks TASK_ID

# Check CloudWatch logs
aws logs filter-events --log-group-name "/ecs/mini-java-app" --start-time 1h
```

**Common Causes:**
- Invalid CPU/memory combinations
- Incorrect IAM permissions
- Missing or invalid environment variables
- Network connectivity issues
- Image pull failures

#### Service Health Check Failures
```bash
# Check target group health
aws elbv2 describe-target-health --target-group-arn TARGET_GROUP_ARN

# Check security group rules
aws ec2 describe-security-groups --group-ids sg-xxxxxxxxx
```

**Common Causes:**
- Application not listening on correct port
- Health check path returning errors
- Security group blocking traffic
- Application startup time exceeding grace period

### 2. Java Application Issues

#### Memory Issues
- Monitor JVM heap usage in CloudWatch
- Adjust `JAVA_OPTS` memory settings
- Consider increasing ECS task memory

#### Startup Issues
- Check application logs for Spring Boot startup errors
- Verify database connectivity
- Validate environment variable configuration

#### Performance Issues
- Enable JVM profiling flags
- Monitor GC metrics
- Check database connection pool settings

### 3. Network Issues

#### Connectivity Problems
```bash
# Test network connectivity from ECS task
aws ecs execute-command --cluster mini-java-app-cluster \
    --task TASK_ID --container mini-java-app \
    --interactive --command "/bin/sh"
```

#### DNS Resolution
- Verify VPC DNS settings
- Check security group rules
- Validate subnet route tables

### 4. Debugging Commands

```bash
# Get service events
aws ecs describe-services --cluster mini-java-app-cluster \
    --services mini-java-app-service --query 'services[0].events'

# List running tasks
aws ecs list-tasks --cluster mini-java-app-cluster \
    --service-name mini-java-app-service

# Get task details
aws ecs describe-tasks --cluster mini-java-app-cluster --tasks TASK_ID

# Check CloudWatch metrics
aws cloudwatch get-metric-statistics \
    --namespace AWS/ECS \
    --metric-name CPUUtilization \
    --dimensions Name=ServiceName,Value=mini-java-app-service \
    --start-time 2023-01-01T00:00:00Z \
    --end-time 2023-01-01T01:00:00Z \
    --period 300 --statistics Average
```

## Security Considerations

### 1. Container Security

- **Non-root user**: Application runs as non-root user (appuser)
- **Minimal base image**: Uses Alpine Linux for smaller attack surface
- **No package managers**: Runtime image doesn't include package managers
- **Read-only root filesystem**: Consider enabling for additional security

### 2. Network Security

- **VPC isolation**: Deploy in private subnets when possible
- **Security groups**: Implement least privilege access
- **ALB termination**: Terminate SSL/TLS at load balancer
- **WAF integration**: Consider AWS WAF for web application protection

### 3. Secrets Management

- **SSM Parameter Store**: Store all sensitive data securely
- **IAM roles**: Use task roles for service-to-service authentication
- **Encryption**: Enable encryption at rest and in transit
- **Rotation**: Implement secret rotation policies

### 4. Monitoring and Auditing

- **CloudTrail**: Enable API call logging
- **GuardDuty**: Enable threat detection
- **Config**: Monitor configuration compliance
- **Security Hub**: Centralized security findings

### 5. Best Practices

- Regular security updates to base images
- Vulnerability scanning with ECR image scanning
- Implement least privilege IAM policies
- Enable VPC Flow Logs for network monitoring
- Use AWS Secrets Manager for automatic secret rotation

---

## Conclusion

This deployment guide provides a comprehensive approach to deploying the Mini Java App on AWS ECS Fargate. The configuration emphasizes:

- **Scalability**: Auto-scaling and load balancing
- **Security**: Secure secrets management and network isolation
- **Reliability**: Health checks and rolling deployments
- **Observability**: Comprehensive logging and monitoring
- **Maintainability**: Infrastructure as code and automation

For production deployments, ensure all security configurations are properly reviewed and tested. Consider implementing CI/CD pipelines for automated deployments and use AWS services like CodePipeline and CodeDeploy for advanced deployment strategies.

**Important Notes:**
- Update SSM parameters with actual production values
- Configure appropriate resource limits based on load testing
- Implement backup and disaster recovery procedures
- Establish monitoring alerts and incident response procedures
- Regular security audits and compliance checks