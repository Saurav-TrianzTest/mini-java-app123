# Deployment Guide for mini-java-app

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Docker Deployment](#docker-deployment)
4. [AWS ECS Fargate Deployment](#aws-ecs-fargate-deployment)
5. [Configuration Management](#configuration-management)
6. [Troubleshooting](#troubleshooting)
7. [Security Considerations](#security-considerations)
8. [Monitoring and Logging](#monitoring-and-logging)

---

## Prerequisites

### Required Tools
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **AWS CLI**: Version 2.x (for ECS deployment)
- **Java**: JDK 11 (for local development)
- **Maven**: Version 3.6+ (for local builds)

### AWS Requirements (for ECS Fargate)
- AWS Account with appropriate permissions
- IAM roles configured:
  - `ecsTaskExecutionRole` - For ECS to pull images and write logs
  - `ecsTaskRole` - For application to access AWS services
- VPC with at least 2 subnets in different availability zones
- Security group allowing inbound traffic on port 8080
- ECR repository (will be created automatically by build script)

### System Requirements
- **Memory**: Minimum 2GB RAM for Docker
- **Disk Space**: At least 5GB free space
- **Network**: Internet connection for pulling base images

---

## Local Development Setup

### 1. Clone and Build Locally

```bash
# Navigate to project directory
cd mini-java-app

# Build with Maven
mvn clean package -DskipTests

# Run the application
java -jar target/mini-java-app-1.0.0.jar
```

### 2. Configure Application

Edit `src/main/resources/application.properties` to configure:
- Server port (default: 8080)
- Database connection details
- External service URLs
- Logging configuration

### 3. Test Locally

```bash
# Check application health
curl http://localhost:8080/mini-app

# View logs
tail -f logs/mini-app.log
```

---

## Docker Deployment

### 1. Build Docker Image Locally

```bash
# Build the image
docker build -t mini-java-app:latest .

# Verify the image
docker images | grep mini-java-app
```

### 2. Run with Docker Compose

```bash
# Start the application
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### 3. Test Containerized Application

```bash
# Check container status
docker ps

# Test the application
curl http://localhost:8080/mini-app

# View container logs
docker logs mini-java-app
```

### 4. Environment Variables

Configure external services by setting environment variables in `docker-compose.yml`:

```yaml
environment:
  - DATABASE_URL=jdbc:mysql://your-db-host:3306/mini_app_db
  - DATABASE_USERNAME=your_username
  - DATABASE_PASSWORD=your_password
  - CACHE_REDIS_HOST=your-redis-host
  - CACHE_REDIS_PORT=6379
```

---

## AWS ECS Fargate Deployment

### Step 1: Prepare AWS Environment

#### 1.1 Create IAM Roles

**ECS Task Execution Role** (if not exists):
```bash
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

**ECS Task Role** (for application permissions):
```bash
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'
```

#### 1.2 Configure VPC and Security Groups

**Create Security Group**:
```bash
# Create security group
aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for mini-java-app" \
  --vpc-id vpc-xxxxxxxx

# Allow inbound traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow outbound traffic
aws ec2 authorize-security-group-egress \
  --group-id sg-xxxxxxxx \
  --protocol -1 \
  --cidr 0.0.0.0/0
```

#### 1.3 Create CloudWatch Log Group

```bash
aws logs create-log-group \
  --log-group-name /ecs/mini-java-app \
  --region us-east-1
```

### Step 2: Build and Push Docker Image

#### 2.1 Using Linux/macOS

```bash
# Navigate to project directory
cd mini-java-app

# Run build and push script
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

**Script will prompt for**:
1. Registry type (AWS ECR or Docker Hub)
2. AWS Region (if ECR)
3. AWS Account ID (if ECR)
4. Repository name
5. Image tag

#### 2.2 Using Windows

```cmd
# Navigate to project directory
cd mini-java-app

# Run build and push script
scripts\build-push.bat
```

**Example ECR Image URI**:
```
123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

### Step 3: Deploy to ECS Fargate

#### 3.1 Using Linux/macOS

```bash
# Run deployment script
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### 3.2 Using Windows

```cmd
# Run deployment script
scripts\deploy-image.bat
```

**Script will prompt for**:
1. AWS Region
2. ECS Cluster Name
3. Docker Image URI (from Step 2)
4. VPC ID
5. Subnet IDs (comma-separated)
6. Security Group ID
7. Load Balancer requirement (y/n)

### Step 4: Verify Deployment

#### 4.1 Check Service Status

```bash
aws ecs describe-services \
  --cluster your-cluster-name \
  --services mini-java-app-service \
  --region us-east-1
```

#### 4.2 View Running Tasks

```bash
aws ecs list-tasks \
  --cluster your-cluster-name \
  --service-name mini-java-app-service \
  --region us-east-1
```

#### 4.3 Check CloudWatch Logs

```bash
# Tail logs in real-time
aws logs tail /ecs/mini-java-app --follow --region us-east-1

# View specific log stream
aws logs get-log-events \
  --log-group-name /ecs/mini-java-app \
  --log-stream-name ecs/mini-java-app/task-id \
  --region us-east-1
```

#### 4.4 Test Application

If using Load Balancer:
```bash
# Get ALB DNS name
aws elbv2 describe-load-balancers \
  --names mini-java-app-alb \
  --query 'LoadBalancers[0].DNSName' \
  --output text

# Test application
curl http://your-alb-dns-name/mini-app
```

If using public IP:
```bash
# Get task public IP
aws ecs describe-tasks \
  --cluster your-cluster-name \
  --tasks task-id \
  --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' \
  --output text

# Test application
curl http://task-public-ip:8080/mini-app
```

---

## ECS Task Definition Explained

### CPU and Memory Configuration

**Valid Fargate CPU/Memory Combinations**:

| CPU (vCPU) | Memory (MB) |
|------------|-------------|
| 256 (.25)  | 512, 1024, 2048 |
| 512 (.5)   | 1024, 2048, 3072, 4096 |
| 1024 (1)   | 2048-8192 (increments of 1024) |
| 2048 (2)   | 4096-16384 (increments of 1024) |
| 4096 (4)   | 8192-30720 (increments of 1024) |

**Default Configuration**:
- CPU: 512 (.5 vCPU)
- Memory: 1024 MB

### Container Configuration

```json
{
  "name": "mini-java-app",
  "image": "your-image-uri",
  "essential": true,
  "portMappings": [
    {
      "containerPort": 8080,
      "protocol": "tcp"
    }
  ],
  "environment": [
    {"name": "JAVA_OPTS", "value": "-Xmx512m -Xms256m"},
    {"name": "SERVER_PORT", "value": "8080"}
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
```

### Network Mode

**awsvpc** (required for Fargate):
- Each task gets its own elastic network interface (ENI)
- Tasks have their own private IP addresses
- Security groups are applied at the task level

---

## ECS Service Configuration

### Service Parameters

```json
{
  "serviceName": "mini-java-app-service",
  "desiredCount": 2,
  "launchType": "FARGATE",
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-xxx", "subnet-yyy"],
      "securityGroups": ["sg-xxx"],
      "assignPublicIp": "ENABLED"
    }
  }
}
```

### Deployment Configuration

- **Maximum Percent**: 200 (allows rolling updates)
- **Minimum Healthy Percent**: 50 (maintains availability)
- **Circuit Breaker**: Enabled with automatic rollback

### Load Balancer Integration

When using Application Load Balancer:
```json
{
  "loadBalancers": [{
    "targetGroupArn": "arn:aws:elasticloadbalancing:...",
    "containerName": "mini-java-app",
    "containerPort": 8080
  }],
  "healthCheckGracePeriodSeconds": 300
}
```

---

## Configuration Management

### Environment Variables

**Application Configuration**:
- `SERVER_PORT`: Application port (default: 8080)
- `SERVER_HOST`: Bind address (default: 0.0.0.0)
- `SERVER_CONTEXT_PATH`: Application context path

**JVM Configuration**:
- `JAVA_OPTS`: JVM options for memory and performance
  - Default: `-Xmx512m -Xms256m -XX:+UseContainerSupport`

**External Services**:
- `DATABASE_URL`: JDBC connection string
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password
- `CACHE_REDIS_HOST`: Redis host
- `CACHE_REDIS_PORT`: Redis port

### Secrets Management

**Using AWS Secrets Manager**:

1. Create secret:
```bash
aws secretsmanager create-secret \
  --name mini-java-app/db-password \
  --secret-string "your-password"
```

2. Update task definition:
```json
{
  "secrets": [
    {
      "name": "DATABASE_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:region:account:secret:mini-java-app/db-password"
    }
  ]
}
```

3. Grant permissions to task execution role:
```bash
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
```

---

## Troubleshooting

### Common Issues

#### 1. Task Fails to Start

**Symptoms**: Tasks start and immediately stop

**Possible Causes**:
- Invalid CPU/memory combination
- Image pull errors
- Application crashes on startup

**Solutions**:
```bash
# Check task stopped reason
aws ecs describe-tasks \
  --cluster your-cluster \
  --tasks task-id \
  --query 'tasks[0].stoppedReason'

# Check CloudWatch logs
aws logs tail /ecs/mini-java-app --follow

# Verify image exists
aws ecr describe-images \
  --repository-name mini-java-app
```

#### 2. Network Connectivity Issues

**Symptoms**: Cannot reach application, health checks fail

**Possible Causes**:
- Security group misconfiguration
- Subnet routing issues
- No public IP assigned

**Solutions**:
```bash
# Verify security group rules
aws ec2 describe-security-groups --group-ids sg-xxx

# Check subnet route table
aws ec2 describe-route-tables --filters "Name=association.subnet-id,Values=subnet-xxx"

# Ensure assignPublicIp is ENABLED
aws ecs describe-services \
  --cluster your-cluster \
  --services mini-java-app-service \
  --query 'services[0].networkConfiguration'
```

#### 3. Out of Memory Errors

**Symptoms**: Tasks crash with OOM errors

**Solutions**:
- Increase task memory in task definition
- Adjust JVM heap size in JAVA_OPTS
- Monitor memory usage in CloudWatch

```bash
# Update JAVA_OPTS for 1GB memory
JAVA_OPTS="-Xmx768m -Xms256m -XX:MaxRAMPercentage=75.0"
```

#### 4. Image Pull Errors

**Symptoms**: "CannotPullContainerError"

**Solutions**:
```bash
# Verify ECR permissions
aws ecr get-login-password --region us-east-1

# Check task execution role has ECR permissions
aws iam get-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-name AmazonECSTaskExecutionRolePolicy

# Verify image exists
aws ecr describe-images \
  --repository-name mini-java-app \
  --image-ids imageTag=latest
```

#### 5. Load Balancer Health Check Failures

**Symptoms**: Tasks are running but marked unhealthy

**Solutions**:
- Verify health check path is correct
- Increase health check grace period
- Check application logs for errors

```bash
# Update target group health check
aws elbv2 modify-target-group \
  --target-group-arn arn:aws:elasticloadbalancing:... \
  --health-check-path /mini-app \
  --health-check-interval-seconds 30 \
  --healthy-threshold-count 2
```

### Debugging Commands

```bash
# Get task details
aws ecs describe-tasks \
  --cluster your-cluster \
  --tasks task-id

# View all service events
aws ecs describe-services \
  --cluster your-cluster \
  --services mini-java-app-service \
  --query 'services[0].events'

# Check task definition
aws ecs describe-task-definition \
  --task-definition mini-java-app-task

# List all tasks in cluster
aws ecs list-tasks \
  --cluster your-cluster \
  --service-name mini-java-app-service
```

---

## ECS Fargate Scaling and Management

### Auto Scaling

#### 1. Configure Target Tracking Scaling

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/your-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# Create scaling policy (CPU-based)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/your-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleInCooldown": 300,
    "ScaleOutCooldown": 60
  }'
```

#### 2. Manual Scaling

```bash
# Scale up
aws ecs update-service \
  --cluster your-cluster \
  --service mini-java-app-service \
  --desired-count 5

# Scale down
aws ecs update-service \
  --cluster your-cluster \
  --service mini-java-app-service \
  --desired-count 2
```

### Blue/Green Deployments

```bash
# Create new task definition revision
aws ecs register-task-definition \
  --cli-input-json file://ecs/task-definition.json

# Update service with new task definition
aws ecs update-service \
  --cluster your-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:2 \
  --force-new-deployment
```

### Rolling Updates

```bash
# Update service with new image
aws ecs update-service \
  --cluster your-cluster \
  --service mini-java-app-service \
  --force-new-deployment \
  --deployment-configuration '{
    "maximumPercent": 200,
    "minimumHealthyPercent": 100
  }'
```

---

## Security Considerations

### 1. Container Security

- **Non-root User**: Application runs as non-root user `appuser`
- **Read-only Root Filesystem**: Consider enabling for enhanced security
- **No Privileged Mode**: Never run containers in privileged mode

### 2. Network Security

- **Security Groups**: Restrict inbound traffic to necessary ports only
- **Private Subnets**: Use private subnets with NAT Gateway for production
- **VPC Endpoints**: Use VPC endpoints for AWS services (ECR, CloudWatch)

### 3. Secrets Management

- **Never hardcode secrets** in task definitions
- Use AWS Secrets Manager or Parameter Store
- Rotate secrets regularly
- Grant least privilege IAM permissions

### 4. IAM Roles

**Task Execution Role** (minimum permissions):
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

**Task Role** (application-specific):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::your-bucket/*"
    }
  ]
}
```

### 5. Image Security

- Scan images for vulnerabilities using ECR image scanning
- Use official base images (eclipse-temurin)
- Keep base images updated
- Minimize image layers

```bash
# Enable ECR image scanning
aws ecr put-image-scanning-configuration \
  --repository-name mini-java-app \
  --image-scanning-configuration scanOnPush=true

# View scan results
aws ecr describe-image-scan-findings \
  --repository-name mini-java-app \
  --image-id imageTag=latest
```

---

## Monitoring and Logging

### CloudWatch Metrics

**ECS Service Metrics**:
- CPUUtilization
- MemoryUtilization
- RunningTaskCount
- DesiredTaskCount

**View Metrics**:
```bash
# Get CPU utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=your-cluster \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 3600 \
  --statistics Average
```

### CloudWatch Logs

**Log Groups**:
- `/ecs/mini-java-app` - Application logs

**Viewing Logs**:
```bash
# Tail logs in real-time
aws logs tail /ecs/mini-java-app --follow

# Filter logs
aws logs filter-log-events \
  --log-group-name /ecs/mini-java-app \
  --filter-pattern "ERROR"

# Export logs to S3
aws logs create-export-task \
  --log-group-name /ecs/mini-java-app \
  --from 1609459200000 \
  --to 1609545600000 \
  --destination s3-bucket-name \
  --destination-prefix logs/
```

### CloudWatch Alarms

```bash
# Create CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name mini-java-app-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=your-cluster

# Create memory alarm
aws cloudwatch put-metric-alarm \
  --alarm-name mini-java-app-high-memory \
  --alarm-description "Alert when memory exceeds 80%" \
  --metric-name MemoryUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=your-cluster
```

### Application Performance Monitoring

**JVM Metrics** (if using Spring Boot Actuator):
- Heap memory usage
- Garbage collection statistics
- Thread count
- HTTP request metrics

**Enable Actuator** (add to pom.xml):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Actuator Endpoints**:
- `/actuator/health` - Health check
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application information

---

## Cost Optimization

### 1. Right-sizing Resources

- Start with minimal CPU/memory (512/1024)
- Monitor actual usage in CloudWatch
- Adjust based on metrics

### 2. Spot Instances

Consider using Fargate Spot for non-critical workloads:
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

### 3. Auto Scaling

- Scale down during off-peak hours
- Use target tracking scaling policies
- Set appropriate min/max capacity

### 4. Log Retention

```bash
# Set log retention to 7 days
aws logs put-retention-policy \
  --log-group-name /ecs/mini-java-app \
  --retention-in-days 7
```

---

## Additional Resources

### AWS Documentation
- [ECS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [ECS Task Definitions](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html)
- [ECS Service Auto Scaling](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html)

### Best Practices
- [ECS Best Practices Guide](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Java Container Best Practices](https://docs.oracle.com/en/java/javase/11/docs/specs/man/java.html)

### Support
- AWS Support: https://console.aws.amazon.com/support/
- ECS Forum: https://forums.aws.amazon.com/forum.jspa?forumID=187
- Docker Community: https://forums.docker.com/

---

## Appendix

### A. Complete Deployment Checklist

- [ ] AWS CLI installed and configured
- [ ] Docker installed and running
- [ ] IAM roles created (ecsTaskExecutionRole, ecsTaskRole)
- [ ] VPC and subnets configured
- [ ] Security groups created with proper rules
- [ ] ECR repository created
- [ ] CloudWatch log group created
- [ ] Application built and tested locally
- [ ] Docker image built and pushed to registry
- [ ] Task definition registered
- [ ] ECS service created
- [ ] Load balancer configured (if needed)
- [ ] Auto scaling configured (if needed)
- [ ] CloudWatch alarms set up
- [ ] Application tested and verified

### B. Quick Reference Commands

```bash
# Build and push
./scripts/build-push.sh

# Deploy to ECS
./scripts/deploy-image.sh

# View logs
aws logs tail /ecs/mini-java-app --follow

# Check service status
aws ecs describe-services --cluster your-cluster --services mini-java-app-service

# Scale service
aws ecs update-service --cluster your-cluster --service mini-java-app-service --desired-count 3

# Force new deployment
aws ecs update-service --cluster your-cluster --service mini-java-app-service --force-new-deployment
```

### C. Environment Variables Reference

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| SERVER_PORT | Application port | 8080 | No |
| JAVA_OPTS | JVM options | -Xmx512m -Xms256m | No |
| DATABASE_URL | Database connection string | - | Yes |
| DATABASE_USERNAME | Database username | - | Yes |
| DATABASE_PASSWORD | Database password | - | Yes |
| CACHE_REDIS_HOST | Redis host | - | No |
| CACHE_REDIS_PORT | Redis port | 6379 | No |
| TZ | Timezone | UTC | No |
| ENVIRONMENT | Environment name | production | No |
| LOGGING_LEVEL | Log level | INFO | No |

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Maintained By**: DevOps Team
