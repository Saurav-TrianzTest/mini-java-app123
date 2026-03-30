# Mini Java App - AWS ECS Fargate Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Local Development](#local-development)
5. [Building and Pushing Docker Image](#building-and-pushing-docker-image)
6. [AWS ECS Fargate Deployment](#aws-ecs-fargate-deployment)
7. [Configuration Management](#configuration-management)
8. [Monitoring and Logging](#monitoring-and-logging)
9. [Troubleshooting](#troubleshooting)
10. [Security Best Practices](#security-best-practices)

---

## Overview

This guide provides comprehensive instructions for containerizing and deploying the **mini-java-app** Java application to AWS ECS Fargate. The application is built using Java 11 with Maven and includes Spring Boot dependencies.

### Application Details
- **Technology Stack**: Java 11, Maven, Spring Boot
- **Build Tool**: Maven 3.9.4
- **Runtime**: Amazon Corretto 11
- **Application Port**: 8080
- **Context Path**: /mini-app
- **Deployment Platform**: AWS ECS Fargate

---

## Prerequisites

### Required Software
1. **Docker** (version 20.10 or later)
   - Download: https://www.docker.com/products/docker-desktop
   - Verify: `docker --version`

2. **AWS CLI** (version 2.x)
   - Download: https://aws.amazon.com/cli/
   - Verify: `aws --version`
   - Configure: `aws configure`

3. **Git** (for version control)
   - Download: https://git-scm.com/downloads
   - Verify: `git --version`

### AWS Account Requirements
1. **AWS Account** with appropriate permissions
2. **IAM User** with the following permissions:
   - ECS Full Access
   - ECR Full Access
   - CloudWatch Logs Full Access
   - VPC Read Access
   - IAM Role Creation (for task execution role)

3. **AWS Resources**:
   - VPC with at least 2 subnets in different availability zones
   - Security Group allowing inbound traffic on port 8080
   - (Optional) Application Load Balancer for production deployments

### IAM Roles Required

#### ECS Task Execution Role
Create an IAM role named `ecsTaskExecutionRole` with the following policy:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    }
  ]
}
```

#### ECS Task Role (Optional)
Create an IAM role named `ecsTaskRole` for application-level permissions:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## Project Structure

```
mini-java-app/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/test/
│       │       ├── MiniApp.java
│       │       └── DatabaseService.java
│       └── resources/
│           └── application.properties
├── ecs/
│   ├── task-definition.json       # ECS Fargate task definition
│   └── service-definition.json    # ECS service configuration
├── scripts/
│   ├── build-push.sh              # Linux/macOS build script
│   ├── build-push.bat             # Windows build script
│   ├── deploy-image.sh            # Linux/macOS deployment script
│   └── deploy-image.bat           # Windows deployment script
├── Dockerfile                      # Multi-stage Docker build
├── docker-compose.yml             # Local development setup
├── .dockerignore                  # Docker build exclusions
├── pom.xml                        # Maven configuration
└── docs/
    └── DEPLOYMENT.md              # This file
```

---

## Local Development

### Step 1: Build the Application Locally

```bash
# Navigate to project directory
cd mini-java-app

# Build with Maven
mvn clean package -DskipTests

# Verify JAR file
ls -lh target/*.jar
```

### Step 2: Run with Docker Compose

```bash
# Build and start the application
docker-compose up --build

# Access the application
curl http://localhost:8080/mini-app

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### Step 3: Test the Docker Image

```bash
# Build the Docker image
docker build -t mini-java-app:test .

# Run the container
docker run -d -p 8080:8080 --name mini-java-app-test mini-java-app:test

# Check logs
docker logs mini-java-app-test

# Test the application
curl http://localhost:8080/mini-app

# Stop and remove
docker stop mini-java-app-test
docker rm mini-java-app-test
```

---

## Building and Pushing Docker Image

### Option 1: AWS ECR (Recommended for ECS)

#### Linux/macOS:
```bash
# Make script executable
chmod +x scripts/build-push.sh

# Run the script
./scripts/build-push.sh

# Follow the prompts:
# 1. Enter image tag (e.g., v1.0.0 or latest)
# 2. Select registry: 1 (AWS ECR)
# 3. Enter AWS region (e.g., us-east-1)
# 4. Enter AWS Account ID
# 5. Enter ECR repository name (default: mini-java-app)
```

#### Windows:
```cmd
# Run the script
scripts\build-push.bat

# Follow the same prompts as above
```

### Option 2: Docker Hub

#### Linux/macOS:
```bash
./scripts/build-push.sh

# Follow the prompts:
# 1. Enter image tag
# 2. Select registry: 2 (Docker Hub)
# 3. Enter Docker Hub username
# 4. Enter Docker Hub password
```

#### Windows:
```cmd
scripts\build-push.bat

# Follow the same prompts as above
```

### Manual Build and Push

```bash
# Build the image
docker build -t mini-java-app:latest .

# Tag for ECR
docker tag mini-java-app:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com

# Push to ECR
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

---

## AWS ECS Fargate Deployment

### Prerequisites Setup

#### 1. Create VPC and Subnets (if not exists)
```bash
# Create VPC
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --region us-east-1

# Create subnets in different AZs
aws ec2 create-subnet --vpc-id vpc-xxxxx --cidr-block 10.0.1.0/24 --availability-zone us-east-1a
aws ec2 create-subnet --vpc-id vpc-xxxxx --cidr-block 10.0.2.0/24 --availability-zone us-east-1b
```

#### 2. Create Security Group
```bash
# Create security group
aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for mini-java-app" \
  --vpc-id vpc-xxxxx

# Allow inbound traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0
```

#### 3. Create CloudWatch Log Group
```bash
aws logs create-log-group --log-group-name /ecs/mini-java-app --region us-east-1
```

### Deployment Steps

#### Linux/macOS:
```bash
# Make script executable
chmod +x scripts/deploy-image.sh

# Run deployment script
./scripts/deploy-image.sh
```

#### Windows:
```cmd
# Run deployment script
scripts\deploy-image.bat
```

### Deployment Script Prompts

The deployment script will prompt for the following information:

1. **AWS Region**: e.g., `us-east-1`
2. **ECS Cluster Name**: e.g., `mini-java-app-cluster` (will be created if doesn't exist)
3. **VPC ID**: e.g., `vpc-0123456789abcdef0`
4. **Subnet IDs**: e.g., `subnet-111111,subnet-222222` (comma-separated, at least 2)
5. **Security Group ID**: e.g., `sg-0123456789abcdef0`
6. **Docker Image URI**: e.g., `123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest`
7. **Load Balancer**: `y` or `n` (script will create ALB and Target Group if yes)

### What the Deployment Script Does

1. **Validates AWS credentials** and retrieves Account ID
2. **Creates or verifies ECS cluster** exists
3. **Creates CloudWatch log group** for application logs
4. **Creates Application Load Balancer** (if requested):
   - Creates ALB with internet-facing scheme
   - Creates Target Group with `target-type: ip` (required for Fargate)
   - Creates HTTP listener on port 80
   - Configures health checks on `/mini-app` endpoint
5. **Registers ECS task definition** with:
   - Fargate launch type
   - CPU: 512 (.5 vCPU)
   - Memory: 1024 MB
   - Container configuration with environment variables
   - CloudWatch logging
6. **Creates or updates ECS service** with:
   - Desired count: 2 tasks
   - Fargate launch type
   - awsvpc network mode
   - Load balancer integration (if applicable)
   - Deployment circuit breaker enabled
7. **Waits for service stability** (may take 3-5 minutes)
8. **Verifies deployment** and displays service status

### Post-Deployment Verification

```bash
# Check service status
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1

# List running tasks
aws ecs list-tasks \
  --cluster mini-java-app-cluster \
  --service-name mini-java-app-service \
  --region us-east-1

# View task details
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --region us-east-1

# Check application logs
aws logs tail /ecs/mini-java-app --follow --region us-east-1
```

### Access the Application

If you created a load balancer:
```bash
# Get ALB DNS name
aws elbv2 describe-load-balancers \
  --names mini-java-app-alb \
  --region us-east-1 \
  --query 'LoadBalancers[0].DNSName' \
  --output text

# Access application
curl http://<alb-dns-name>/mini-app
```

If no load balancer (direct task access):
```bash
# Get task public IP
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --region us-east-1 \
  --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' \
  --output text

# Get public IP from ENI
aws ec2 describe-network-interfaces \
  --network-interface-ids <eni-id> \
  --query 'NetworkInterfaces[0].Association.PublicIp' \
  --output text

# Access application
curl http://<public-ip>:8080/mini-app
```

---

## Configuration Management

### Environment Variables

The application uses environment variables for configuration. These are defined in:
- `docker-compose.yml` for local development
- `ecs/task-definition.json` for ECS deployment

#### Key Environment Variables:

**Java Runtime:**
- `JAVA_OPTS`: JVM options (memory, GC settings)
- `TZ`: Timezone (default: UTC)

**Application:**
- `SERVER_PORT`: Application port (default: 8080)
- `SERVER_HOST`: Bind address (default: 0.0.0.0)
- `SERVER_CONTEXT_PATH`: Application context path (default: /mini-app)

**Database:**
- `DATABASE_URL`: JDBC connection URL
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password
- `DATABASE_DRIVER`: JDBC driver class
- `DATABASE_POOL_MAX_CONNECTIONS`: Connection pool size
- `DATABASE_POOL_TIMEOUT`: Connection timeout

**Cache (Redis):**
- `CACHE_REDIS_HOST`: Redis host
- `CACHE_REDIS_PORT`: Redis port
- `CACHE_REDIS_PASSWORD`: Redis password
- `CACHE_REDIS_DATABASE`: Redis database number

**External Services:**
- `EXTERNAL_API_BASE_URL`: External API endpoint
- `EXTERNAL_API_TIMEOUT`: API timeout
- `EXTERNAL_API_KEY`: API authentication key
- `PAYMENT_SERVICE_URL`: Payment service endpoint
- `PAYMENT_SERVICE_USERNAME`: Payment service username
- `PAYMENT_SERVICE_PASSWORD`: Payment service password

**Security:**
- `SECURITY_JWT_SECRET`: JWT signing secret
- `SECURITY_ADMIN_USERNAME`: Admin username
- `SECURITY_ADMIN_PASSWORD`: Admin password
- `SECURITY_ENCRYPTION_KEY`: Encryption key

**Monitoring:**
- `MONITORING_ENDPOINT`: Monitoring service endpoint
- `MONITORING_USERNAME`: Monitoring username
- `MONITORING_PASSWORD`: Monitoring password

**Messaging (RabbitMQ):**
- `MESSAGING_RABBITMQ_HOST`: RabbitMQ host
- `MESSAGING_RABBITMQ_PORT`: RabbitMQ port
- `MESSAGING_RABBITMQ_USERNAME`: RabbitMQ username
- `MESSAGING_RABBITMQ_PASSWORD`: RabbitMQ password

**General:**
- `ENVIRONMENT`: Environment name (development, staging, production)
- `DEBUG_ENABLED`: Enable debug mode (true/false)
- `LOGGING_LEVEL`: Log level (DEBUG, INFO, WARN, ERROR)

### Using AWS Secrets Manager

For production deployments, use AWS Secrets Manager for sensitive data:

```bash
# Create secret
aws secretsmanager create-secret \
  --name mini-java-app/database \
  --secret-string '{"username":"dbuser","password":"dbpass"}' \
  --region us-east-1

# Update task definition to use secrets
# Add to containerDefinitions[].secrets:
{
  "name": "DATABASE_USERNAME",
  "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:mini-java-app/database:username::"
}
```

---

## Monitoring and Logging

### CloudWatch Logs

Application logs are automatically sent to CloudWatch Logs:

```bash
# View logs in real-time
aws logs tail /ecs/mini-java-app --follow --region us-east-1

# View logs for specific time range
aws logs filter-log-events \
  --log-group-name /ecs/mini-java-app \
  --start-time $(date -d '1 hour ago' +%s)000 \
  --region us-east-1

# Search logs
aws logs filter-log-events \
  --log-group-name /ecs/mini-java-app \
  --filter-pattern "ERROR" \
  --region us-east-1
```

### CloudWatch Metrics

Monitor ECS service metrics:

```bash
# CPU utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average \
  --region us-east-1

# Memory utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name MemoryUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average \
  --region us-east-1
```

### Application Load Balancer Metrics

If using ALB:

```bash
# Target health
aws elbv2 describe-target-health \
  --target-group-arn <target-group-arn> \
  --region us-east-1

# Request count
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApplicationELB \
  --metric-name RequestCount \
  --dimensions Name=LoadBalancer,Value=app/mini-java-app-alb/xxxxx \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Sum \
  --region us-east-1
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Task Fails to Start

**Symptoms:**
- Tasks transition from PENDING to STOPPED
- Error: "CannotPullContainerError"

**Solutions:**
```bash
# Check task stopped reason
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --region us-east-1 \
  --query 'tasks[0].stoppedReason'

# Verify ECR permissions
aws ecr get-login-password --region us-east-1

# Check task execution role
aws iam get-role --role-name ecsTaskExecutionRole

# Verify image exists in ECR
aws ecr describe-images \
  --repository-name mini-java-app \
  --region us-east-1
```

#### 2. Service Fails to Reach Steady State

**Symptoms:**
- Service stuck in deployment
- Tasks continuously restart

**Solutions:**
```bash
# Check service events
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1 \
  --query 'services[0].events[0:10]'

# Check task logs
aws logs tail /ecs/mini-java-app --follow --region us-east-1

# Verify health check configuration
# Ensure health check path is correct: /mini-app
# Increase health check grace period if needed
```

#### 3. Cannot Access Application

**Symptoms:**
- Connection timeout
- 502 Bad Gateway (if using ALB)

**Solutions:**
```bash
# Check security group rules
aws ec2 describe-security-groups \
  --group-ids <security-group-id> \
  --region us-east-1

# Verify port 8080 is allowed inbound
# Verify ALB security group allows traffic from internet

# Check target health
aws elbv2 describe-target-health \
  --target-group-arn <target-group-arn> \
  --region us-east-1

# Verify task is running
aws ecs list-tasks \
  --cluster mini-java-app-cluster \
  --service-name mini-java-app-service \
  --desired-status RUNNING \
  --region us-east-1
```

#### 4. High Memory Usage / OOM Errors

**Symptoms:**
- Tasks killed with exit code 137
- OutOfMemoryError in logs

**Solutions:**
```bash
# Increase task memory in task-definition.json
# Current: "memory": "1024"
# Recommended: "memory": "2048"

# Adjust JVM heap size in JAVA_OPTS
# Current: -Xmx512m -Xms256m
# Recommended: -Xmx1536m -Xms512m

# Re-register task definition and update service
aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task \
  --region us-east-1
```

#### 5. Invalid CPU/Memory Combination

**Symptoms:**
- Error: "Invalid CPU or memory value specified"

**Solutions:**
Valid Fargate CPU/Memory combinations:
- CPU: 256 → Memory: 512, 1024, 2048
- CPU: 512 → Memory: 1024, 2048, 3072, 4096
- CPU: 1024 → Memory: 2048-8192 (increments of 1024)
- CPU: 2048 → Memory: 4096-16384 (increments of 1024)
- CPU: 4096 → Memory: 8192-30720 (increments of 1024)

Current configuration uses: CPU: 512, Memory: 1024 (valid)

#### 6. Database Connection Failures

**Symptoms:**
- SQLException in logs
- Connection timeout errors

**Solutions:**
```bash
# Verify database security group allows traffic from ECS tasks
# Add inbound rule for port 3306 from ECS security group

# Check database endpoint is correct
# Verify DATABASE_URL environment variable

# Test connectivity from ECS task
aws ecs execute-command \
  --cluster mini-java-app-cluster \
  --task <task-id> \
  --container mini-java-app \
  --interactive \
  --command "/bin/sh"

# Inside container:
# telnet <db-host> 3306
```

### Debugging Commands

```bash
# Get task details
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --region us-east-1

# Get container instance details
aws ecs describe-container-instances \
  --cluster mini-java-app-cluster \
  --container-instances <container-instance-arn> \
  --region us-east-1

# View service deployment history
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1 \
  --query 'services[0].deployments'

# Check CloudWatch alarms
aws cloudwatch describe-alarms \
  --alarm-names mini-java-app-cpu-alarm \
  --region us-east-1
```

---

## Security Best Practices

### 1. Use Non-Root User
The Dockerfile creates and uses a non-root user (`appuser`) for running the application.

### 2. Secrets Management
- Never hardcode secrets in code or configuration files
- Use AWS Secrets Manager or Parameter Store for sensitive data
- Rotate secrets regularly

### 3. Network Security
- Use private subnets for ECS tasks when possible
- Restrict security group rules to minimum required access
- Use VPC endpoints for AWS services (ECR, CloudWatch, Secrets Manager)

### 4. IAM Roles
- Use least privilege principle for IAM roles
- Separate task execution role from task role
- Regularly audit IAM permissions

### 5. Image Security
- Scan Docker images for vulnerabilities
- Use official base images (Amazon Corretto)
- Keep base images updated
- Enable ECR image scanning

```bash
# Enable ECR image scanning
aws ecr put-image-scanning-configuration \
  --repository-name mini-java-app \
  --image-scanning-configuration scanOnPush=true \
  --region us-east-1

# View scan results
aws ecr describe-image-scan-findings \
  --repository-name mini-java-app \
  --image-id imageTag=latest \
  --region us-east-1
```

### 6. Logging and Monitoring
- Enable CloudWatch Logs for all containers
- Set up CloudWatch alarms for critical metrics
- Use AWS CloudTrail for API audit logging

### 7. Encryption
- Enable encryption at rest for CloudWatch Logs
- Use HTTPS/TLS for all external communications
- Encrypt sensitive data in transit and at rest

---

## Scaling and Performance

### Auto Scaling Configuration

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --min-capacity 2 \
  --max-capacity 10 \
  --region us-east-1

# Create scaling policy (CPU-based)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration file://scaling-policy.json \
  --region us-east-1
```

**scaling-policy.json:**
```json
{
  "TargetValue": 70.0,
  "PredefinedMetricSpecification": {
    "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
  },
  "ScaleInCooldown": 300,
  "ScaleOutCooldown": 60
}
```

### Performance Tuning

#### JVM Tuning
Adjust `JAVA_OPTS` in task definition:
```
-Xmx1536m -Xms512m 
-XX:+UseG1GC 
-XX:MaxGCPauseMillis=200 
-XX:+UseContainerSupport 
-XX:MaxRAMPercentage=75.0
```

#### Connection Pooling
Adjust database connection pool settings:
```
DATABASE_POOL_MAX_CONNECTIONS=50
DATABASE_POOL_MIN_IDLE=10
DATABASE_POOL_TIMEOUT=30000
```

---

## Rollback Procedures

### Rollback to Previous Task Definition

```bash
# List task definition revisions
aws ecs list-task-definitions \
  --family-prefix mini-java-app-task \
  --region us-east-1

# Update service to previous revision
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:1 \
  --region us-east-1

# Wait for deployment
aws ecs wait services-stable \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1
```

### Blue/Green Deployment

For zero-downtime deployments, use AWS CodeDeploy with ECS:

```bash
# Create CodeDeploy application
aws deploy create-application \
  --application-name mini-java-app \
  --compute-platform ECS \
  --region us-east-1

# Create deployment group
aws deploy create-deployment-group \
  --application-name mini-java-app \
  --deployment-group-name mini-java-app-dg \
  --service-role-arn arn:aws:iam::123456789:role/CodeDeployServiceRole \
  --ecs-services clusterName=mini-java-app-cluster,serviceName=mini-java-app-service \
  --load-balancer-info targetGroupInfoList=[{name=mini-java-app-tg}] \
  --blue-green-deployment-configuration file://blue-green-config.json \
  --region us-east-1
```

---

## Cost Optimization

### 1. Right-Size Resources
- Monitor CPU and memory utilization
- Adjust task CPU/memory based on actual usage
- Use Fargate Spot for non-critical workloads

### 2. Use Fargate Spot

Update service definition:
```json
{
  "capacityProviderStrategy": [
    {
      "capacityProvider": "FARGATE_SPOT",
      "weight": 1,
      "base": 0
    }
  ]
}
```

### 3. Optimize Image Size
- Use multi-stage builds (already implemented)
- Remove unnecessary dependencies
- Use Alpine-based images where possible

### 4. Schedule Tasks
For non-24/7 workloads, use scheduled scaling:
```bash
# Scale down during off-hours
aws application-autoscaling put-scheduled-action \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scheduled-action-name scale-down-evening \
  --schedule "cron(0 22 * * ? *)" \
  --scalable-target-action MinCapacity=0,MaxCapacity=0 \
  --region us-east-1
```

---

## Additional Resources

### AWS Documentation
- [ECS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [ECS Task Definitions](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html)
- [ECS Service Auto Scaling](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html)

### Docker Best Practices
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Dockerfile Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)

### Java in Containers
- [Java SE Support for Docker CPU and Memory Limits](https://blogs.oracle.com/java/post/java-se-support-for-docker-cpu-and-memory-limits)
- [Best Practices: Java Memory Arguments for Containers](https://developers.redhat.com/blog/2017/03/14/java-inside-docker)

---

## Support and Maintenance

### Regular Maintenance Tasks

1. **Weekly:**
   - Review CloudWatch logs for errors
   - Check service health and task status
   - Monitor resource utilization

2. **Monthly:**
   - Update base Docker images
   - Review and rotate secrets
   - Analyze cost and optimize resources
   - Update dependencies (Maven)

3. **Quarterly:**
   - Security audit and vulnerability scanning
   - Review IAM roles and permissions
   - Update documentation
   - Disaster recovery testing

### Getting Help

For issues or questions:
1. Check CloudWatch Logs: `/ecs/mini-java-app`
2. Review ECS service events
3. Consult AWS Support (if applicable)
4. Review this documentation

---

## Conclusion

This deployment guide provides comprehensive instructions for containerizing and deploying the mini-java-app to AWS ECS Fargate. Follow the steps carefully, and refer to the troubleshooting section for common issues.

For production deployments, ensure you:
- Use AWS Secrets Manager for sensitive data
- Enable auto-scaling
- Set up monitoring and alerting
- Implement proper backup and disaster recovery procedures
- Follow security best practices

**Happy Deploying! 🚀**
