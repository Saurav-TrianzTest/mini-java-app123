# Mini Java Application - AWS ECS Fargate Deployment Guide

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Docker Build and Push](#docker-build-and-push)
5. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
6. [ECS Task Definition](#ecs-task-definition)
7. [ECS Service Configuration](#ecs-service-configuration)
8. [Deployment Process](#deployment-process)
9. [Verification and Testing](#verification-and-testing)
10. [Troubleshooting](#troubleshooting)
11. [Scaling and Management](#scaling-and-management)
12. [Security Considerations](#security-considerations)

---

## Overview

This guide provides step-by-step instructions for deploying the Mini Java Application to AWS ECS Fargate. The application is a Java 11 Spring Boot-based microservice containerized with Docker and deployed using AWS Elastic Container Service (ECS) with Fargate launch type.

**Application Details:**
- **Technology**: Java 11, Spring Boot 2.7.0, Maven
- **Runtime**: Amazon Corretto 11
- **Application Port**: 8080
- **Context Path**: /mini-app
- **External Dependencies**: MySQL, Redis, RabbitMQ

---

## Prerequisites

### Required Tools

1. **Docker Desktop** (v20.10 or later)
   - Download: https://www.docker.com/products/docker-desktop
   - Verify: `docker --version`

2. **AWS CLI** (v2.x)
   - Installation: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
   - Verify: `aws --version`
   - Configure: `aws configure`

3. **Git** (for version control)
   - Download: https://git-scm.com/downloads

### AWS Account Requirements

- Active AWS account with appropriate permissions
- IAM user with permissions for:
  - ECS (Task Definitions, Services, Clusters)
  - ECR (Repository creation, image push/pull)
  - VPC (Subnets, Security Groups)
  - IAM (Role creation for ECS tasks)
  - CloudWatch Logs (Log group creation)
  - EC2 (for Load Balancer creation if needed)

---

## Local Development Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd mini-java-app
```

### 2. Configure Environment Variables

Create a `.env` file for local development:

```bash
# Server configuration
SERVER_PORT=8080
SERVER_HOST=0.0.0.0

# Database configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=mini_app_db
DB_USER=root
DB_PASSWORD=password123

# Redis configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672

# JVM options
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseContainerSupport
```

### 3. Run with Docker Compose

```bash
docker-compose up --build
```

Access the application:
- **Application**: http://localhost:8080/mini-app
- **Health Check**: http://localhost:8080/actuator/health

### 4. Stop the Application

```bash
docker-compose down
```

---

## Docker Build and Push

### Option 1: Using Build Script (Recommended)

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
2. Request credentials and repository details
3. Build the Docker image with proper tagging
4. Authenticate with the selected registry
5. Push the image to the registry

### Option 2: Manual Build and Push

#### AWS ECR

```bash
# Set variables
AWS_REGION=us-east-1
AWS_ACCOUNT_ID=123456789012
REPO_NAME=mini-java-app
IMAGE_TAG=latest

# Authenticate with ECR
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Create ECR repository (if not exists)
aws ecr create-repository --repository-name $REPO_NAME --region $AWS_REGION

# Build image
docker build -t $REPO_NAME:$IMAGE_TAG .

# Tag image
docker tag $REPO_NAME:$IMAGE_TAG $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPO_NAME:$IMAGE_TAG

# Push image
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPO_NAME:$IMAGE_TAG
```

#### Docker Hub

```bash
# Set variables
DOCKER_USERNAME=your-username
IMAGE_TAG=latest

# Login to Docker Hub
docker login -u $DOCKER_USERNAME

# Build and tag
docker build -t $DOCKER_USERNAME/mini-java-app:$IMAGE_TAG .

# Push
docker push $DOCKER_USERNAME/mini-java-app:$IMAGE_TAG
```

---

## AWS ECS Fargate Prerequisites

### 1. VPC and Networking Setup

**Requirements:**
- VPC with at least 2 subnets in different Availability Zones
- Internet Gateway attached to VPC (for public access)
- Route tables configured for internet access

**Create VPC (if needed):**

```bash
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --region us-east-1
```

### 2. Security Group Configuration

**Create Security Group:**

```bash
aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for Mini Java App" \
  --vpc-id vpc-xxxxxx \
  --region us-east-1
```

**Add Inbound Rules:**

```bash
# Allow HTTP traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0 \
  --region us-east-1
```

### 3. IAM Roles Setup

#### ECS Task Execution Role

This role allows ECS to pull images from ECR and write logs to CloudWatch.

**Create Role:**

```bash
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://trust-policy.json
```

**trust-policy.json:**

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

**Attach Policy:**

```bash
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ECS Task Role (Optional)

This role provides permissions for the application to access AWS services.

```bash
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file://trust-policy.json

aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
```

### 4. CloudWatch Log Group

```bash
aws logs create-log-group \
  --log-group-name /ecs/mini-java-app \
  --region us-east-1
```

---

## ECS Task Definition

The task definition specifies how your container should run.

**Key Components:**

1. **Launch Type**: FARGATE
2. **Network Mode**: awsvpc (required for Fargate)
3. **CPU and Memory**: 512 CPU units (0.5 vCPU) and 1024 MB memory
4. **Container Definition**:
   - Image URI from ECR/Docker Hub
   - Port mappings (8080)
   - Environment variables
   - Log configuration

**Valid Fargate CPU/Memory Combinations:**

| CPU (vCPU) | Memory (MB) |
|------------|-------------|
| 256 (.25)  | 512, 1024, 2048 |
| 512 (.5)   | 1024, 2048, 3072, 4096 |
| 1024 (1)   | 2048-8192 (1 GB increments) |
| 2048 (2)   | 4096-16384 (1 GB increments) |
| 4096 (4)   | 8192-30720 (1 GB increments) |

**Default Configuration**: cpu: "512", memory: "1024"

---

## ECS Service Configuration

The service ensures your tasks are running and handles load balancing.

**Key Components:**

1. **Desired Count**: 2 (for high availability)
2. **Launch Type**: FARGATE
3. **Network Configuration**:
   - Subnets (at least 2 for HA)
   - Security Groups
   - Public IP assignment
4. **Deployment Configuration**:
   - Rolling update strategy
   - Circuit breaker for automatic rollback
5. **Load Balancer** (optional):
   - Application Load Balancer (ALB)
   - Target Group with ip target type
   - Health check configuration

---

## Deployment Process

### Option 1: Using Deployment Script (Recommended)

#### Linux/macOS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows

```cmd
scripts\deploy-image.bat
```

The script will:
1. Prompt for AWS region and cluster name
2. Request network configuration (VPC, subnets, security group)
3. Request external service configurations (Database, Redis, RabbitMQ)
4. Ask if load balancer is needed
5. Register task definition
6. Create or update ECS service
7. Wait for deployment to stabilize
8. Display deployment status

### Option 2: Manual Deployment

#### Step 1: Register Task Definition

```bash
aws ecs register-task-definition \
  --cli-input-json file://ecs/task-definition.json \
  --region us-east-1
```

#### Step 2: Create ECS Cluster

```bash
aws ecs create-cluster \
  --cluster-name mini-java-app-cluster \
  --region us-east-1
```

#### Step 3: Create ECS Service

```bash
aws ecs create-service \
  --cli-input-json file://ecs/service-definition.json \
  --region us-east-1
```

#### Step 4: Wait for Stability

```bash
aws ecs wait services-stable \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1
```

---

## Verification and Testing

### 1. Check Service Status

```bash
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1
```

### 2. View Running Tasks

```bash
aws ecs list-tasks \
  --cluster mini-java-app-cluster \
  --service-name mini-java-app-service \
  --region us-east-1
```

### 3. Check CloudWatch Logs

```bash
aws logs tail /ecs/mini-java-app --follow --region us-east-1
```

### 4. Test Application Endpoints

```bash
# Get load balancer DNS (if using ALB)
ALB_DNS=$(aws elbv2 describe-load-balancers \
  --region us-east-1 \
  --query 'LoadBalancers[0].DNSName' \
  --output text)

# Test health endpoint
curl http://$ALB_DNS/actuator/health

# Test application endpoint
curl http://$ALB_DNS/mini-app
```

---

## Troubleshooting

### Common Issues

#### 1. Task Fails to Start

**Symptom**: Tasks transition to STOPPED state immediately

**Solutions**:
- Check CloudWatch logs for application errors
- Verify IAM execution role has correct permissions
- Ensure image exists in ECR and is accessible
- Check security group allows outbound internet access

```bash
# View task stopped reason
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --region us-east-1 \
  --query 'tasks[0].stoppedReason'
```

#### 2. Invalid CPU/Memory Configuration

**Symptom**: Error: "Invalid CPU or memory value specified"

**Solution**: Use valid Fargate CPU/memory combinations:
- cpu: "512", memory: "1024" (default)
- cpu: "1024", memory: "2048"

Refer to the valid combinations table above.

#### 3. Network Connectivity Issues

**Symptom**: Cannot access application or tasks cannot pull image

**Solutions**:
- Verify subnets have internet gateway route
- Check security group allows inbound traffic on port 8080
- Ensure assignPublicIp is ENABLED
- Verify VPC DNS settings are enabled

#### 4. Health Check Failures

**Symptom**: Tasks are marked unhealthy and replaced

**Solutions**:
- Verify health check path is correct (/actuator/health)
- Increase health check grace period (300 seconds for Java apps)
- Check application logs for startup errors
- Ensure JVM has sufficient memory

```bash
# Update health check grace period
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --health-check-grace-period-seconds 300 \
  --region us-east-1
```

#### 5. Database Connection Errors

**Symptom**: Application logs show "Cannot connect to database"

**Solutions**:
- Verify database hostname and port are correct
- Check database security group allows inbound traffic from ECS security group
- Ensure credentials are correct in task definition
- Verify VPC networking allows communication

---

## Scaling and Management

### Manual Scaling

```bash
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --desired-count 4 \
  --region us-east-1
```

### Auto Scaling Setup

#### Register Scalable Target

```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10 \
  --region us-east-1
```

#### Create Scaling Policy

```bash
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
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

### Update Task Definition

```bash
# Register new task definition version
aws ecs register-task-definition \
  --cli-input-json file://ecs/task-definition.json \
  --region us-east-1

# Update service with new task definition
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:2 \
  --force-new-deployment \
  --region us-east-1
```

### Blue/Green Deployments

For zero-downtime deployments, configure AWS CodeDeploy with ECS:

1. Create CodeDeploy application
2. Configure deployment group with ECS cluster
3. Set up AppSpec file with traffic shifting
4. Trigger deployment through CodeDeploy

---

## Security Considerations

### 1. Secrets Management

**Use AWS Secrets Manager or Parameter Store:**

```json
{
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:db-password"
    }
  ]
}
```

### 2. Container Security

- Run as non-root user (already configured in Dockerfile)
- Use minimal base images (Amazon Corretto)
- Regularly scan images for vulnerabilities
- Keep dependencies up to date

### 3. Network Security

- Use private subnets with NAT Gateway for production
- Restrict security group rules to minimum required
- Enable VPC Flow Logs for monitoring
- Use AWS PrivateLink for AWS service access

### 4. IAM Best Practices

- Use least privilege principle for task roles
- Separate execution role and task role
- Regularly audit IAM permissions
- Enable CloudTrail for API logging

### 5. Logging and Monitoring

- Enable CloudWatch Container Insights
- Set up alarms for critical metrics
- Implement distributed tracing (AWS X-Ray)
- Use structured logging (JSON format)

---

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS Fargate Documentation](https://docs.aws.amazon.com/fargate/)
- [Spring Boot on AWS](https://aws.amazon.com/blogs/opensource/spring-boot-on-aws/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Java in Containers](https://aws.amazon.com/blogs/opensource/java-application-optimization-on-amazon-ecs-with-amazon-corretto/)

---

## Support

For issues or questions:
- Check CloudWatch logs: `/ecs/mini-java-app`
- Review AWS service quotas and limits
- Consult AWS Support or community forums
- Review application-specific logs and metrics

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-07  
**Maintained By**: DevOps Team