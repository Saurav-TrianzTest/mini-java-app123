# Deployment Guide - mini-java-app

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Building Docker Image](#building-docker-image)
4. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
5. [ECS Fargate Setup](#ecs-fargate-setup)
6. [ECS Task Definition](#ecs-task-definition)
7. [ECS Service Configuration](#ecs-service-configuration)
8. [Deployment Process](#deployment-process)
9. [Troubleshooting](#troubleshooting)
10. [Monitoring and Scaling](#monitoring-and-scaling)
11. [Security Considerations](#security-considerations)

---

## Prerequisites

### Required Software
- **Docker**: Version 20.10 or later
- **Docker Compose**: Version 2.0 or later (for local development)
- **AWS CLI**: Version 2.x
- **Git**: For version control
- **Java Development Kit (JDK)**: Java 11 (for local development)
- **Maven**: 3.6+ (for local builds)

### AWS Account Requirements
- Active AWS account with appropriate permissions
- IAM user with ECS, ECR, VPC, and CloudWatch permissions
- AWS CLI configured with credentials (`aws configure`)

### System Requirements
- **Operating System**: Linux, macOS, or Windows 10/11
- **Memory**: Minimum 8GB RAM
- **Disk Space**: At least 10GB free space

---

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd mini-java-app
```

### 2. Configure Application Properties
Edit `src/main/resources/application.properties` for local development:

```properties
server.port=8080
server.host=0.0.0.0
server.context-path=/mini-app

# Database configuration (use local or external database)
spring.datasource.url=jdbc:mysql://localhost:3306/mini_app_db
spring.datasource.username=root
spring.datasource.password=password

# Redis configuration
spring.redis.host=localhost
spring.redis.port=6379

# RabbitMQ configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```

### 3. Build and Run Locally with Maven
```bash
# Build the application
mvn clean package

# Run the application
java -jar target/mini-java-app-1.0.0.jar
```

Access the application at: `http://localhost:8080/mini-app/`

### 4. Run with Docker Compose (Local Testing)
```bash
# Build and start the application container
docker-compose up --build

# Run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop containers
docker-compose down
```

**Note**: Docker Compose configuration includes only the application container. External services (MySQL, Redis, RabbitMQ) must be provided separately or configured via environment variables.

---

## Building Docker Image

### Dockerfile Overview
The project uses a multi-stage Dockerfile:
- **Stage 1 (Builder)**: Uses `maven:3.9.4-eclipse-temurin-11` to build the application
- **Stage 2 (Runtime)**: Uses `eclipse-temurin:11-jre-alpine` for a minimal runtime image

### Build Locally
```bash
# Build Docker image
docker build -t mini-java-app:latest .

# Test the image locally
docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_USER=root \
  -e DB_PASSWORD=password \
  mini-java-app:latest
```

### Build and Push to Registry

#### Linux/macOS
```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

#### Windows
```cmd
scripts\build-push.bat
```

The script will:
1. Prompt for registry selection (AWS ECR or Docker Hub)
2. Authenticate with the selected registry
3. Build the Docker image
4. Tag and push to the registry
5. For ECR: Automatically create repository if it doesn't exist

---

## AWS ECS Fargate Prerequisites

### 1. IAM Roles

ECS requires two IAM roles:

#### a) ECS Task Execution Role (`ecsTaskExecutionRole`)
Allows ECS to pull images from ECR and write logs to CloudWatch.

**Trust Policy**:
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

**Attached Policies**:
- `AmazonECSTaskExecutionRolePolicy` (AWS managed)

**Create Role**:
```bash
aws iam create-role --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://trust-policy.json

aws iam attach-role-policy --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### b) ECS Task Role (`ecsTaskRole`) - Optional
Allows the application to access AWS services (S3, DynamoDB, etc.).

**Trust Policy**: Same as above

**Attached Policies**: Add specific permissions based on application needs (e.g., S3, DynamoDB)

### 2. VPC and Networking

#### Create VPC (if needed)
```bash
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --region us-east-1
```

#### Create Subnets
ECS Fargate requires at least 2 subnets in different Availability Zones:

```bash
# Subnet 1
aws ec2 create-subnet --vpc-id <vpc-id> \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a

# Subnet 2
aws ec2 create-subnet --vpc-id <vpc-id> \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b
```

#### Create Security Group
```bash
aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for mini-java-app" \
  --vpc-id <vpc-id>

# Allow inbound traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id <security-group-id> \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow outbound traffic (usually enabled by default)
aws ec2 authorize-security-group-egress \
  --group-id <security-group-id> \
  --protocol -1 \
  --cidr 0.0.0.0/0
```

### 3. CloudWatch Log Group
```bash
aws logs create-log-group --log-group-name /ecs/mini-java-app --region us-east-1
```

---

## ECS Fargate Setup

### 1. Create ECS Cluster
```bash
aws ecs create-cluster --cluster-name mini-java-app-cluster --region us-east-1
```

### 2. Push Docker Image to ECR

Use the `build-push.sh` or `build-push.bat` script (see [Building Docker Image](#building-docker-image)).

Alternatively, manual steps:

```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Create ECR repository
aws ecr create-repository --repository-name mini-java-app --region us-east-1

# Tag and push image
docker tag mini-java-app:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

---

## ECS Task Definition

The task definition (`ecs/task-definition.json`) defines:

### Key Configuration
- **Launch Type**: `FARGATE`
- **Network Mode**: `awsvpc` (required for Fargate)
- **CPU**: `512` (0.5 vCPU)
- **Memory**: `1024` MB (1 GB)

### Valid Fargate CPU/Memory Combinations

| CPU (vCPU) | Memory (MB) |
|------------|-------------|
| 256 (.25)  | 512, 1024, 2048 |
| 512 (.5)   | 1024, 2048, 3072, 4096 |
| 1024 (1)   | 2048-8192 (increments of 1024) |
| 2048 (2)   | 4096-16384 (increments of 1024) |
| 4096 (4)   | 8192-30720 (increments of 1024) |

### Container Definition
- **Image**: ECR image URI (e.g., `<account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest`)
- **Port Mappings**: Container port `8080`
- **Environment Variables**: Application configuration (DB, Redis, RabbitMQ)
- **Logging**: CloudWatch Logs with `/ecs/mini-java-app` log group

### Environment Variables
The following environment variables are configured in the task definition:

```json
{
  "environment": [
    {"name": "JAVA_OPTS", "value": "-Xmx512m -Xms256m -XX:+UseContainerSupport"},
    {"name": "SERVER_PORT", "value": "8080"},
    {"name": "DB_HOST", "value": "<database-host>"},
    {"name": "DB_USER", "value": "<database-user>"},
    {"name": "DB_PASSWORD", "value": "<database-password>"},
    {"name": "REDIS_HOST", "value": "<redis-host>"},
    {"name": "RABBITMQ_HOST", "value": "<rabbitmq-host>"}
  ]
}
```

**Security Note**: For production, use AWS Secrets Manager or Parameter Store for sensitive values.

---

## ECS Service Configuration

The service definition (`ecs/service-definition.json`) defines:

### Key Configuration
- **Service Name**: `mini-java-app-service`
- **Launch Type**: `FARGATE`
- **Desired Count**: `2` (number of tasks)
- **Network Configuration**: `awsvpc` with subnets and security group
- **Deployment Configuration**:
  - `maximumPercent`: `200` (allows rolling updates)
  - `minimumHealthyPercent`: `50` (ensures availability)

### Load Balancer (Optional)
If using an Application Load Balancer (ALB):

```json
"loadBalancers": [
  {
    "targetGroupArn": "<target-group-arn>",
    "containerName": "mini-java-app",
    "containerPort": 8080
  }
],
"healthCheckGracePeriodSeconds": 300
```

**Note**: The deployment script can automatically create an ALB and target group.

---

## Deployment Process

### Step 1: Build and Push Docker Image

```bash
# Linux/macOS
./scripts/build-push.sh

# Windows
scripts\build-push.bat
```

### Step 2: Deploy to ECS Fargate

```bash
# Linux/macOS
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh

# Windows
scripts\deploy-image.bat
```

The deployment script will:
1. Prompt for AWS region and ECS cluster name
2. Create ECS cluster if it doesn't exist
3. Prompt for network configuration (VPC, subnets, security group)
4. Prompt for Docker image URI from ECR
5. Prompt for external service configuration (database, Redis, RabbitMQ)
6. Optionally create Application Load Balancer and Target Group
7. Create CloudWatch log group
8. Register ECS task definition with placeholders replaced
9. Create or update ECS service
10. Wait for service to stabilize
11. Display deployment summary with service details and access URLs

### Step 3: Verify Deployment

```bash
# Check service status
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1

# View running tasks
aws ecs list-tasks \
  --cluster mini-java-app-cluster \
  --service-name mini-java-app-service \
  --region us-east-1

# View CloudWatch logs
aws logs tail /ecs/mini-java-app --follow --region us-east-1
```

---

## Troubleshooting

### Common Issues

#### 1. Task Fails to Start
**Symptoms**: Tasks transition from `PENDING` to `STOPPED` immediately

**Causes**:
- Invalid CPU/memory combination
- Image pull errors (check ECR permissions)
- Missing IAM execution role

**Solution**:
```bash
# Check task stopped reason
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-id> \
  --region us-east-1 \
  --query 'tasks[0].stoppedReason'

# Check CloudWatch logs for errors
aws logs tail /ecs/mini-java-app --region us-east-1
```

#### 2. Network Connectivity Issues
**Symptoms**: Application cannot connect to database or external services

**Causes**:
- Security group not allowing outbound traffic
- Subnets not properly configured
- NAT Gateway missing (if using private subnets)

**Solution**:
- Verify security group rules allow outbound traffic to required services
- Ensure subnets have route to internet (NAT Gateway or Internet Gateway)
- Check VPC DNS settings (`enableDnsHostnames` and `enableDnsSupport`)

#### 3. CPU/Memory Errors
**Symptoms**: `OutOfMemory` errors or task restarts

**Causes**:
- JVM heap size exceeds container memory
- Application memory leaks

**Solution**:
- Adjust `JAVA_OPTS` in task definition: `-Xmx512m -Xms256m`
- Increase task memory allocation (use valid Fargate combinations)
- Profile application for memory leaks

#### 4. Health Check Failures
**Symptoms**: ALB marks targets as unhealthy

**Causes**:
- Application not responding on health check path
- Health check timeout too short
- Application startup time exceeds grace period

**Solution**:
- Verify health check endpoint is accessible: `/mini-app/`
- Increase `healthCheckGracePeriodSeconds` in service definition (default: 300)
- Adjust ALB target group health check settings

#### 5. Image Pull Errors
**Symptoms**: `CannotPullContainerError`

**Causes**:
- ECR repository permissions
- Invalid image URI
- Missing execution role

**Solution**:
```bash
# Verify image exists
aws ecr describe-images \
  --repository-name mini-java-app \
  --region us-east-1

# Check execution role permissions
aws iam get-role --role-name ecsTaskExecutionRole
```

---

## Monitoring and Scaling

### CloudWatch Monitoring

#### View Logs
```bash
# Tail logs in real-time
aws logs tail /ecs/mini-java-app --follow --region us-east-1

# Filter logs by time range
aws logs filter-log-events \
  --log-group-name /ecs/mini-java-app \
  --start-time 1609459200000 \
  --region us-east-1
```

#### CloudWatch Metrics
Monitor the following metrics in CloudWatch:
- **CPUUtilization**: Container CPU usage
- **MemoryUtilization**: Container memory usage
- **TargetResponseTime**: ALB response time (if using ALB)
- **RequestCount**: Number of requests (if using ALB)

### Service Auto Scaling

#### Configure Target Tracking Scaling
```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10 \
  --region us-east-1

# Create scaling policy (CPU-based)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
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
  }' \
  --region us-east-1
```

### Blue/Green Deployments

For zero-downtime deployments, use AWS CodeDeploy with ECS:

```bash
# Update service with CODE_DEPLOY deployment controller
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --deployment-controller type=CODE_DEPLOY \
  --region us-east-1
```

---

## Security Considerations

### 1. Use AWS Secrets Manager
Store sensitive values (database passwords, API keys) in AWS Secrets Manager:

```json
"secrets": [
  {
    "name": "DB_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:db-password"
  }
]
```

### 2. Network Security
- Use private subnets for ECS tasks with NAT Gateway for outbound access
- Restrict security group rules to only necessary ports and sources
- Enable VPC Flow Logs for network monitoring

### 3. IAM Least Privilege
- Grant only required permissions to task execution role
- Use separate task role for application-specific AWS access
- Regularly review and audit IAM policies

### 4. Container Security
- Use minimal base images (e.g., `eclipse-temurin:11-jre-alpine`)
- Scan images for vulnerabilities using ECR image scanning
- Run containers as non-root user (already configured in Dockerfile)
- Keep base images and dependencies up to date

### 5. Application Security
- Enable HTTPS/TLS for ALB listeners
- Implement authentication and authorization in the application
- Use environment-specific configurations (dev, staging, prod)
- Enable AWS WAF for web application firewall protection

### 6. Logging and Auditing
- Enable CloudTrail for API activity logging
- Configure CloudWatch alarms for anomalous behavior
- Implement structured logging in the application (JSON format)
- Regularly review logs for security incidents

---

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Java Performance Tuning](https://docs.oracle.com/en/java/javase/11/gctuning/)

---

## Support and Maintenance

For issues or questions:
- Check CloudWatch logs: `aws logs tail /ecs/mini-java-app --follow`
- Review ECS service events: `aws ecs describe-services --cluster mini-java-app-cluster --services mini-java-app-service`
- Contact DevOps team or AWS Support

---

**Document Version**: 1.0  
**Last Updated**: 2026-02-19  
**Target Platform**: AWS ECS Fargate  
**Application**: mini-java-app (Java 11, Spring Boot 2.7.0)
