# Deployment Guide: mini-java-app on AWS ECS Fargate

This guide provides comprehensive instructions for deploying the mini-java Java application to AWS ECS Fargate.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Overview](#project-overview)
3. [Local Development Setup](#local-development-setup)
4. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
5. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
6. [ECS Fargate Setup](#ecs-fargate-setup)
7. [ECS Task Definition Explained](#ecs-task-definition-explained)
8. [ECS Service Configuration](#ecs-service-configuration)
9. [Deployment to AWS ECS Fargate](#deployment-to-aws-ecs-fargate)
10. [Configuration Management](#configuration-management)
11. [Monitoring and Logging](#monitoring-and-logging)
12. [Troubleshooting](#troubleshooting)
13. [Security Best Practices](#security-best-practices)
14. [Scaling and Management](#scaling-and-management)

---

## Prerequisites

### Required Tools

- **Docker**: Version 20.10 or higher
  - Installation: https://docs.docker.com/get-docker/
  - Verify: `docker --version`

- **AWS CLI**: Version 2.x
  - Installation: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
  - Verify: `aws --version`
  - Configure: `aws configure`

- **Java Development Kit**: JDK 11
  - Installation: https://adoptium.net/
  - Verify: `java -version`

- **Maven**: Version 3.6 or higher
  - Installation: https://maven.apache.org/install.html
  - Verify: `mvn --version`

### AWS Account Requirements

- Active AWS account with billing enabled
- IAM user with appropriate permissions:
  - `AmazonECS_FullAccess`
  - `AmazonEC2ContainerRegistryFullAccess`
  - `IAMFullAccess` (for creating service roles)
  - `CloudWatchLogsFullAccess`
  - `ElasticLoadBalancingFullAccess`

### Network Requirements

- VPC with public subnets (at least 2 in different availability zones)
- Internet Gateway attached to VPC
- Security group allowing inbound traffic on port 8080
- Route tables configured for internet access

---

## Project Overview

### Technology Stack

- **Framework**: Spring Boot 2.7.0
- **Language**: Java 11
- **Build Tool**: Maven
- **Package Type**: JAR (executable)
- **Application Type**: Web API
- **Base Image**: Amazon Corretto 11 (explicit)

### Application Configuration

- **Application Port**: 8080
- **Context Path**: /mini-app
- **Health Endpoint**: /actuator/health (Spring Boot Actuator)
- **Dependencies**:
  - spring-boot-starter-web
  - mysql-connector-java 8.0.33

### External Service Dependencies

- **Database**: MySQL 8.0 (configured via environment variables)
- **Cache**: Redis (configured via environment variables)
- **External APIs**: Configurable via environment variables

---

## Local Development Setup

### Clone the Repository

```bash
git clone <repository-url>
cd mini-java
```

### Build the Application Locally

```bash
# Using Maven
mvn clean package -DskipTests

# The JAR file will be created at: target/mini-java-app-1.0.0.jar
```

### Run Locally with Java

```bash
java -jar target/mini-java-app-1.0.0.jar
```

### Build and Run with Docker Compose

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

### Environment Variables for Local Development

Create a `.env` file in the project root:

```env
# Server Configuration
SERVER_PORT=8080
SERVER_HOST=0.0.0.0

# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=mini_app_db
DB_USERNAME=root
DB_PASSWORD=password123

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_secret

# External API Configuration
EXTERNAL_API_URL=http://api.example.com:8080/v1
EXTERNAL_API_KEY=your_api_key_here

# Security Configuration
JWT_SECRET=your_jwt_secret_here
ENCRYPTION_KEY=your_encryption_key_here

# JVM Options
JAVA_OPTS=-Xmx512m -Xms256m
```

---

## Building and Pushing Docker Images

### Using Build Scripts

The project includes automated scripts for building and pushing Docker images to AWS ECR or Docker Hub.

#### Linux/macOS

```bash
cd scripts
chmod +x build-push.sh
./build-push.sh
```

#### Windows

```cmd
cd scripts
build-push.bat
```

### Script Workflow

1. **Select Registry Type**: Choose between AWS ECR or Docker Hub
2. **Enter Registry Credentials**: Provide necessary credentials and configuration
3. **Tag Image**: Enter image tag (defaults to "latest")
4. **Build Image**: Builds Docker image using multi-stage Dockerfile
5. **Authenticate**: Logs into selected registry
6. **Push Image**: Pushes the built image to registry

### Manual Docker Build

If you prefer manual control:

```bash
# Build the image
docker build -t mini-java-app:latest .

# Tag for ECR
docker tag mini-java-app:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest

# Authenticate with ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com

# Create ECR repository (if not exists)
aws ecr create-repository --repository-name mini-java-app --region us-east-1

# Push to ECR
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

---

## AWS ECS Fargate Prerequisites

### IAM Roles

#### 1. ECS Task Execution Role

This role allows ECS to pull container images and write logs.

**Create the role**:

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

#### 2. ECS Task Role (Optional)

This role gives the application permissions to access AWS services.

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

# Attach policies as needed (e.g., S3, DynamoDB, etc.)
```

### VPC and Network Configuration

#### Create VPC (if not exists)

```bash
VPC_ID=$(aws ec2 create-vpc --cidr-block 10.0.0.0/16 --query 'Vpc.VpcId' --output text)
aws ec2 create-tags --resources $VPC_ID --tags Key=Name,Value=mini-java-app-vpc
```

#### Create Subnets

```bash
# Subnet 1 (us-east-1a)
SUBNET_1=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --query 'Subnet.SubnetId' --output text)

# Subnet 2 (us-east-1b)
SUBNET_2=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --query 'Subnet.SubnetId' --output text)
```

#### Create Internet Gateway

```bash
IGW_ID=$(aws ec2 create-internet-gateway --query 'InternetGateway.InternetGatewayId' --output text)
aws ec2 attach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID
```

#### Configure Route Table

```bash
ROUTE_TABLE_ID=$(aws ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query 'RouteTables[0].RouteTableId' --output text)

aws ec2 create-route \
  --route-table-id $ROUTE_TABLE_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $IGW_ID
```

#### Create Security Group

```bash
SECURITY_GROUP_ID=$(aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for mini-java-app" \
  --vpc-id $VPC_ID \
  --query 'GroupId' --output text)

# Allow inbound traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id $SECURITY_GROUP_ID \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow inbound traffic on port 80 (for ALB)
aws ec2 authorize-security-group-ingress \
  --group-id $SECURITY_GROUP_ID \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0
```

### CloudWatch Log Group

```bash
aws logs create-log-group --log-group-name /ecs/mini-java-app --region us-east-1
```

---

## ECS Fargate Setup

### Create ECS Cluster

```bash
aws ecs create-cluster --cluster-name mini-java-app-cluster --region us-east-1
```

---

## ECS Task Definition Explained

The task definition defines how your container should run on ECS Fargate.

### Key Components

#### Launch Type Configuration

```json
"requiresCompatibilities": ["FARGATE"],
"networkMode": "awsvpc"
```

- **requiresCompatibilities**: Specifies Fargate as the launch type
- **networkMode**: Must be "awsvpc" for Fargate (each task gets its own ENI)

#### CPU and Memory

```json
"cpu": "512",
"memory": "1024"
```

**Valid Fargate CPU/Memory Combinations**:

| CPU (vCPU) | Memory (MB) |
|------------|-------------|
| 256 (.25)  | 512, 1024, 2048 |
| 512 (.5)   | 1024, 2048, 3072, 4096 |
| 1024 (1)   | 2048-8192 (increments of 1024) |
| 2048 (2)   | 4096-16384 (increments of 1024) |
| 4096 (4)   | 8192-30720 (increments of 1024) |

#### Execution Role

```json
"executionRoleArn": "arn:aws:iam::{{ACCOUNT_ID}}:role/ecsTaskExecutionRole"
```

Allows ECS to:
- Pull images from ECR
- Write logs to CloudWatch
- Retrieve secrets from Secrets Manager/Parameter Store

#### Container Definition

```json
"containerDefinitions": [{
  "name": "mini-java-app",
  "image": "{{IMAGE_URI}}",
  "essential": true,
  "portMappings": [{"containerPort": 8080, "protocol": "tcp"}],
  "environment": [...],
  "logConfiguration": {...}
}]
```

#### Logging Configuration

```json
"logConfiguration": {
  "logDriver": "awslogs",
  "options": {
    "awslogs-group": "/ecs/mini-java-app",
    "awslogs-region": "us-east-1",
    "awslogs-stream-prefix": "ecs"
  }
}
```

---

## ECS Service Configuration

The service definition manages how tasks are deployed and maintained.

### Key Components

#### Launch Type and Platform

```json
"launchType": "FARGATE",
"platformVersion": "LATEST"
```

#### Network Configuration

```json
"networkConfiguration": {
  "awsvpcConfiguration": {
    "subnets": ["subnet-xxx", "subnet-yyy"],
    "securityGroups": ["sg-xxx"],
    "assignPublicIp": "ENABLED"
  }
}
```

- **subnets**: At least 2 subnets in different AZs for high availability
- **securityGroups**: Controls inbound/outbound traffic
- **assignPublicIp**: "ENABLED" for tasks without NAT gateway

#### Deployment Configuration

```json
"deploymentConfiguration": {
  "maximumPercent": 200,
  "minimumHealthyPercent": 50,
  "deploymentCircuitBreaker": {
    "enable": true,
    "rollback": true
  }
}
```

- **maximumPercent**: Max number of tasks during deployment (200% = double)
- **minimumHealthyPercent**: Min healthy tasks during deployment (50%)
- **deploymentCircuitBreaker**: Automatically rollback failed deployments

#### Load Balancer Integration

```json
"loadBalancers": [{
  "targetGroupArn": "arn:aws:elasticloadbalancing:...",
  "containerName": "mini-java-app",
  "containerPort": 8080
}]
```

**Important**: Target group must use `target-type: ip` for Fargate.

---

## Deployment to AWS ECS Fargate

### Using Deployment Scripts

The project includes automated deployment scripts.

#### Linux/macOS

```bash
cd scripts
chmod +x deploy-image.sh
./deploy-image.sh
```

#### Windows

```cmd
cd scripts
deploy-image.bat
```

### Script Workflow

1. **AWS Configuration**: Enter region and cluster name
2. **Network Configuration**: Provide VPC, subnets, security group
3. **Image URI**: Enter the ECR image URI
4. **Environment Variables**: Configure database, Redis, API keys, etc.
5. **Load Balancer**: Choose whether to create Application Load Balancer
6. **Task Registration**: Registers task definition with ECS
7. **Service Creation/Update**: Creates or updates ECS service
8. **Wait for Stability**: Waits for service to become stable
9. **Display Summary**: Shows deployment details and log group

### Manual Deployment

If you prefer manual deployment:

```bash
# Register task definition
aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json --region us-east-1

# Create service
aws ecs create-service --cli-input-json file://ecs/service-definition.json --region us-east-1

# Or update existing service
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task \
  --force-new-deployment \
  --region us-east-1

# Wait for service stability
aws ecs wait services-stable \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1
```

---

## Configuration Management

### Environment Variables

Environment variables are managed in the task definition. Update them by:

1. Modifying `ecs/task-definition.json`
2. Re-registering the task definition
3. Updating the service with the new task definition

### Secrets Management

For sensitive data, use AWS Secrets Manager:

```json
"secrets": [
  {
    "name": "DB_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"
  }
]
```

### Configuration Files

For complex configurations, use:
- **AWS Systems Manager Parameter Store**
- **S3 buckets** (mount at runtime)
- **ConfigMaps** (for Kubernetes compatibility)

---

## Monitoring and Logging

### CloudWatch Logs

View application logs:

```bash
# Tail logs in real-time
aws logs tail /ecs/mini-java-app --follow --region us-east-1

# Filter logs
aws logs filter-log-events \
  --log-group-name /ecs/mini-java-app \
  --filter-pattern "ERROR" \
  --region us-east-1
```

### CloudWatch Metrics

ECS automatically publishes metrics:
- CPUUtilization
- MemoryUtilization
- NetworkRxBytes
- NetworkTxBytes

### CloudWatch Alarms

Create alarms for critical metrics:

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name mini-java-app-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2
```

### Application Performance Monitoring

Integrate with:
- **AWS X-Ray**: Distributed tracing
- **CloudWatch Application Insights**: Automatic monitoring
- **Third-party tools**: Datadog, New Relic, Dynatrace

---

## Troubleshooting

### Common Issues

#### 1. Task Fails to Start

**Symptoms**: Tasks transition from PENDING to STOPPED

**Solutions**:
- Check CloudWatch logs for error messages
- Verify IAM roles have correct permissions
- Ensure security group allows necessary traffic
- Confirm subnets have internet access (IGW or NAT)
- Validate CPU/memory combinations

```bash
# Describe task to see stop reason
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --region us-east-1
```

#### 2. Cannot Pull Image from ECR

**Error**: "CannotPullContainerError"

**Solutions**:
- Verify executionRoleArn has ECR permissions
- Check image URI is correct
- Ensure repository exists and image is pushed
- Confirm ECR repository policy allows access

```bash
# Test ECR authentication
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
```

#### 3. Service Not Reaching Steady State

**Symptoms**: Service keeps starting and stopping tasks

**Solutions**:
- Check application health endpoint
- Verify environment variables are correct
- Increase health check grace period
- Review application logs for startup errors
- Ensure database/external services are accessible

```bash
# Check service events
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1 \
  --query 'services[0].events[:10]'
```

#### 4. High CPU/Memory Usage

**Solutions**:
- Increase task CPU/memory allocation
- Tune JVM heap settings (JAVA_OPTS)
- Profile application for memory leaks
- Optimize database queries
- Implement caching strategies

#### 5. Network Connectivity Issues

**Symptoms**: Cannot connect to external services

**Solutions**:
- Verify security group rules (outbound)
- Check VPC route tables
- Ensure NAT gateway or IGW is configured
- Test DNS resolution
- Validate service endpoints

---

## Security Best Practices

### Container Security

1. **Use Non-Root User**: Dockerfile already implements this
2. **Minimal Base Image**: Using Amazon Corretto reduces attack surface
3. **Scan Images**: Use ECR image scanning

```bash
aws ecr start-image-scan \
  --repository-name mini-java-app \
  --image-id imageTag=latest \
  --region us-east-1
```

### Network Security

1. **Private Subnets**: Use private subnets with NAT for production
2. **Security Groups**: Implement least privilege access
3. **WAF**: Use AWS WAF with Application Load Balancer
4. **VPC Flow Logs**: Enable for network traffic monitoring

### Secrets Management

1. **Never Hardcode Secrets**: Use Secrets Manager or Parameter Store
2. **Rotate Credentials**: Implement automatic rotation
3. **Encrypt at Rest**: Enable encryption for Secrets Manager
4. **IAM Policies**: Restrict secret access to specific tasks

### Application Security

1. **Update Dependencies**: Regularly update Java dependencies
2. **HTTPS Only**: Use ALB with SSL/TLS certificates
3. **Input Validation**: Implement proper validation and sanitization
4. **Authentication**: Use strong authentication mechanisms (JWT)

---

## Scaling and Management

### Manual Scaling

```bash
# Scale to 5 tasks
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --desired-count 5 \
  --region us-east-1
```

### Auto Scaling

#### Target Tracking Scaling (CPU-based)

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --min-capacity 2 \
  --max-capacity 10

# Create scaling policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --policy-name cpu-target-tracking \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration file://scaling-policy.json
```

**scaling-policy.json**:

```json
{
  "TargetValue": 70.0,
  "PredefinedMetricSpecification": {
    "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
  },
  "ScaleOutCooldown": 60,
  "ScaleInCooldown": 60
}
```

### Blue/Green Deployments

Use AWS CodeDeploy for blue/green deployments:

```bash
# Create deployment group
aws deploy create-deployment-group \
  --application-name mini-java-app \
  --deployment-group-name mini-java-app-dg \
  --deployment-config-name CodeDeployDefault.ECSAllAtOnce \
  --service-role-arn arn:aws:iam::account:role/CodeDeployServiceRole \
  --ecs-services clusterName=mini-java-app-cluster,serviceName=mini-java-app-service \
  --load-balancer-info targetGroupInfoList=[{name=mini-java-app-tg}]
```

### Rolling Updates

Default ECS deployment strategy:
- Gradually replaces tasks with new version
- Maintains minimum healthy tasks
- Automatic rollback on failure (with circuit breaker)

---

## Conclusion

This deployment guide provides a comprehensive overview of deploying the mini-java application to AWS ECS Fargate. For additional assistance:

- **AWS ECS Documentation**: https://docs.aws.amazon.com/ecs/
- **AWS Fargate Documentation**: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html
- **Spring Boot Documentation**: https://spring.io/projects/spring-boot

For issues or questions, please refer to the troubleshooting section or contact your DevOps team.
