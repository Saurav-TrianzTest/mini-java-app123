# Mini Java Application - AWS ECS Fargate Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Local Development](#local-development)
5. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
6. [Building and Pushing Docker Image](#building-and-pushing-docker-image)
7. [ECS Fargate Deployment](#ecs-fargate-deployment)
8. [Configuration Management](#configuration-management)
9. [Monitoring and Logging](#monitoring-and-logging)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)
12. [Scaling and Management](#scaling-and-management)

---

## Overview

This guide provides comprehensive instructions for containerizing and deploying the Mini Java Application to AWS ECS Fargate. The application is a Java 11 application built with Maven and Spring Boot.

**Technology Stack:**
- Java 11
- Maven 3.9.4
- Spring Boot 2.7.0
- MySQL Connector 8.0.33
- Amazon Corretto 11 (Runtime)

**Deployment Platform:**
- AWS ECS Fargate
- Docker containerization
- CloudWatch Logs for monitoring

---

## Prerequisites

### Required Software
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **AWS CLI**: Version 2.x
- **Git**: For version control
- **Java 11**: For local development (optional)
- **Maven 3.6+**: For local builds (optional)

### AWS Account Requirements
- Active AWS account with appropriate permissions
- IAM user with permissions for:
  - ECS (create/update clusters, services, task definitions)
  - ECR (create repositories, push images)
  - EC2 (VPC, subnets, security groups)
  - IAM (create/manage roles)
  - CloudWatch Logs (create log groups)
  - Elastic Load Balancing (create ALB, target groups)

### Install AWS CLI
```bash
# Linux/macOS
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Windows
# Download and run the AWS CLI MSI installer from:
# https://awscli.amazonaws.com/AWSCLIV2.msi

# Verify installation
aws --version

# Configure AWS credentials
aws configure
```

---

## Project Structure

```
Test_Comp1/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/test/
│       │       ├── MiniApp.java
│       │       └── DatabaseService.java
│       └── resources/
│           └── application.properties
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── scripts/
│   ├── build-push.sh
│   ├── build-push.bat
│   ├── deploy-image.sh
│   └── deploy-image.bat
├── ecs/
│   ├── task-definition.json
│   └── service-definition.json
└── docs/
    └── DEPLOYMENT.md
```

---

## Local Development

### Using Docker Compose

1. **Build and run locally:**
```bash
docker-compose up --build
```

2. **Access the application:**
```
http://localhost:8080
```

3. **View logs:**
```bash
docker-compose logs -f mini-java-app
```

4. **Stop the application:**
```bash
docker-compose down
```

### Environment Variables

Configure the application using environment variables in `docker-compose.yml`:

```yaml
environment:
  SERVER_PORT: "8080"
  DATABASE_URL: "jdbc:mysql://host.docker.internal:3306/mini_app_db"
  DATABASE_USERNAME: "root"
  DATABASE_PASSWORD: "password123"
  JAVA_OPTS: "-Xmx512m -Xms256m"
```

---

## AWS ECS Fargate Prerequisites

### 1. Create VPC and Networking

```bash
# Create VPC
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --region us-east-1

# Create subnets (at least 2 in different AZs)
aws ec2 create-subnet --vpc-id vpc-xxxxx --cidr-block 10.0.1.0/24 --availability-zone us-east-1a
aws ec2 create-subnet --vpc-id vpc-xxxxx --cidr-block 10.0.2.0/24 --availability-zone us-east-1b

# Create Internet Gateway
aws ec2 create-internet-gateway
aws ec2 attach-internet-gateway --vpc-id vpc-xxxxx --internet-gateway-id igw-xxxxx

# Create route table and associate with subnets
aws ec2 create-route-table --vpc-id vpc-xxxxx
aws ec2 create-route --route-table-id rtb-xxxxx --destination-cidr-block 0.0.0.0/0 --gateway-id igw-xxxxx
```

### 2. Create Security Group

```bash
# Create security group
aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for Mini Java App" \
  --vpc-id vpc-xxxxx

# Allow inbound traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow inbound traffic on port 80 (for ALB)
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0
```

### 3. Create IAM Roles

**ECS Task Execution Role:**
```bash
# Create trust policy file: ecs-trust-policy.json
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

# Create role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://ecs-trust-policy.json

# Attach managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

**ECS Task Role (for application permissions):**
```bash
# Create role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file://ecs-trust-policy.json

# Attach policies as needed (e.g., S3, DynamoDB access)
```

### 4. Create CloudWatch Log Group

```bash
aws logs create-log-group --log-group-name /ecs/mini-java-app --region us-east-1
```

---

## Building and Pushing Docker Image

### Option 1: Using AWS ECR

**Linux/macOS:**
```bash
cd /path/to/Test_Comp1
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

**Windows:**
```cmd
cd C:\path\to\Test_Comp1
scripts\build-push.bat
```

**Interactive Prompts:**
1. Select registry type: `1` (AWS ECR)
2. Enter AWS Region: `us-east-1`
3. Enter AWS Account ID: `123456789012`
4. Enter image tag: `latest` (or version number)

The script will:
- Authenticate with AWS ECR
- Create ECR repository if it doesn't exist
- Build Docker image
- Push image to ECR

### Option 2: Using Docker Hub

**Interactive Prompts:**
1. Select registry type: `2` (Docker Hub)
2. Enter Docker Hub username
3. Enter Docker Hub password
4. Enter image tag: `latest`

---

## ECS Fargate Deployment

### Understanding ECS Components

**Task Definition:**
- Blueprint for your application
- Specifies container image, CPU, memory, environment variables
- Defines logging configuration

**Service:**
- Maintains desired number of task instances
- Handles load balancing and auto-scaling
- Manages deployments and updates

**Cluster:**
- Logical grouping of tasks and services
- Can contain multiple services

### Deploy to ECS Fargate

**Linux/macOS:**
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

**Windows:**
```cmd
scripts\deploy-image.bat
```

**Interactive Prompts:**
1. AWS Region: `us-east-1`
2. ECS Cluster Name: `mini-java-app-cluster`
3. VPC ID: `vpc-xxxxx`
4. Subnet IDs: `subnet-xxxxx,subnet-yyyyy`
5. Security Group ID: `sg-xxxxx`
6. Docker Image URI: `123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest`
7. Need load balancer?: `y` or `n`

The script will:
- Create ECS cluster (if doesn't exist)
- Create Application Load Balancer and Target Group (if requested)
- Create CloudWatch log group
- Register task definition
- Create or update ECS service
- Wait for service to stabilize

### Deployment Output

```
==========================================
Deployment Complete!
==========================================

Service Name: mini-java-app-service
Cluster: mini-java-app-cluster
Region: us-east-1
Task Definition: arn:aws:ecs:us-east-1:123456789012:task-definition/mini-java-app-task:1

Load Balancer DNS: mini-java-app-alb-123456789.us-east-1.elb.amazonaws.com
Application URL: http://mini-java-app-alb-123456789.us-east-1.elb.amazonaws.com

CloudWatch Logs: /ecs/mini-java-app
```

---

## Configuration Management

### Task Definition Configuration

**CPU and Memory (Fargate Valid Combinations):**
```json
{
  "cpu": "512",     // 0.5 vCPU
  "memory": "1024"  // 1 GB
}
```

**Valid Fargate CPU/Memory Combinations:**
- CPU: 256 (.25 vCPU) → Memory: 512, 1024, 2048 MB
- CPU: 512 (.5 vCPU) → Memory: 1024, 2048, 3072, 4096 MB
- CPU: 1024 (1 vCPU) → Memory: 2048-8192 MB (increments of 1024)
- CPU: 2048 (2 vCPU) → Memory: 4096-16384 MB (increments of 1024)
- CPU: 4096 (4 vCPU) → Memory: 8192-30720 MB (increments of 1024)

### Environment Variables

Configure application behavior via environment variables in `task-definition.json`:

```json
{
  "environment": [
    {
      "name": "JAVA_OPTS",
      "value": "-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
    },
    {
      "name": "SERVER_PORT",
      "value": "8080"
    },
    {
      "name": "DATABASE_URL",
      "value": "jdbc:mysql://rds-endpoint:3306/mini_app_db"
    }
  ]
}
```

### Using AWS Secrets Manager

For sensitive data, use AWS Secrets Manager:

```json
{
  "secrets": [
    {
      "name": "DATABASE_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:db-password-xxxxx"
    }
  ]
}
```

---

## Monitoring and Logging

### CloudWatch Logs

**View logs in real-time:**
```bash
aws logs tail /ecs/mini-java-app --follow --region us-east-1
```

**View logs for specific task:**
```bash
aws logs tail /ecs/mini-java-app --follow --filter-pattern "task-id" --region us-east-1
```

### CloudWatch Metrics

Monitor ECS service metrics:
- CPUUtilization
- MemoryUtilization
- Running task count
- Pending task count

**View metrics:**
```bash
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 3600 \
  --statistics Average \
  --region us-east-1
```

### Application Health Checks

The application uses process-based health checks. For production, consider implementing:
- HTTP health check endpoint
- Spring Boot Actuator health endpoint
- Custom health check logic

---

## Troubleshooting

### Common Issues

#### 1. Task Fails to Start

**Symptoms:**
- Tasks transition from PENDING to STOPPED
- No running tasks in service

**Solutions:**
```bash
# Check task stopped reason
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks task-id \
  --region us-east-1 \
  --query 'tasks[0].stoppedReason'

# Common causes:
# - Invalid CPU/memory combination
# - Image pull errors (check ECR permissions)
# - Container health check failures
# - Insufficient resources in cluster
```

#### 2. Cannot Pull Image from ECR

**Symptoms:**
- Error: "CannotPullContainerError"

**Solutions:**
```bash
# Verify ECR repository exists
aws ecr describe-repositories --repository-names mini-java-app --region us-east-1

# Check task execution role has ECR permissions
aws iam get-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-name AmazonECSTaskExecutionRolePolicy

# Verify image exists in ECR
aws ecr list-images --repository-name mini-java-app --region us-east-1
```

#### 3. Service Not Reaching Steady State

**Symptoms:**
- Service stuck in deployment
- Tasks continuously restarting

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

# Common causes:
# - Application crashes on startup
# - Health check failures
# - Insufficient memory (OOM errors)
# - Network connectivity issues
```

#### 4. Load Balancer Health Check Failures

**Symptoms:**
- Target group shows unhealthy targets
- ALB returns 503 errors

**Solutions:**
```bash
# Check target health
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/mini-java-app-tg/xxxxx

# Verify security group allows traffic from ALB
# Verify application is listening on correct port
# Check application logs for errors
```

#### 5. High Memory Usage / OOM Errors

**Symptoms:**
- Tasks killed with exit code 137
- CloudWatch shows high memory utilization

**Solutions:**
```bash
# Increase task memory in task definition
# Adjust JVM heap size in JAVA_OPTS
# Example: -Xmx768m -Xms384m (for 1024 MB task memory)

# Update task definition
aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json

# Update service with new task definition
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:2
```

### Debugging Commands

```bash
# List running tasks
aws ecs list-tasks --cluster mini-java-app-cluster --region us-east-1

# Describe specific task
aws ecs describe-tasks --cluster mini-java-app-cluster --tasks task-id --region us-east-1

# View service details
aws ecs describe-services --cluster mini-java-app-cluster --services mini-java-app-service --region us-east-1

# Check task definition
aws ecs describe-task-definition --task-definition mini-java-app-task --region us-east-1

# View CloudWatch logs
aws logs tail /ecs/mini-java-app --follow --region us-east-1
```

---

## Security Considerations

### 1. Network Security

- **Use private subnets** for ECS tasks when possible
- **Configure security groups** to allow only necessary traffic
- **Use VPC endpoints** for AWS services (ECR, CloudWatch, Secrets Manager)
- **Enable VPC Flow Logs** for network monitoring

### 2. IAM Permissions

- **Principle of least privilege**: Grant only necessary permissions
- **Separate roles**: Use different roles for task execution and task runtime
- **Rotate credentials**: Regularly rotate AWS access keys and secrets

### 3. Container Security

- **Use official base images**: Amazon Corretto, Eclipse Temurin
- **Scan images for vulnerabilities**: Use AWS ECR image scanning
- **Run as non-root user**: Dockerfile creates and uses `appuser`
- **Keep images updated**: Regularly rebuild with latest base images

### 4. Secrets Management

- **Never hardcode secrets** in task definitions or code
- **Use AWS Secrets Manager** or Parameter Store for sensitive data
- **Rotate secrets regularly**
- **Audit secret access** using CloudTrail

### 5. Logging and Monitoring

- **Enable CloudWatch Logs** for all containers
- **Set up CloudWatch Alarms** for critical metrics
- **Use AWS CloudTrail** for API audit logging
- **Implement application-level logging** with appropriate log levels

---

## Scaling and Management

### Auto Scaling

**Configure Service Auto Scaling:**
```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10 \
  --region us-east-1

# Create scaling policy (target tracking)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name cpu-target-tracking \
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

### Manual Scaling

```bash
# Scale service to 5 tasks
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --desired-count 5 \
  --region us-east-1
```

### Blue/Green Deployments

Use AWS CodeDeploy for blue/green deployments:

```bash
# Create deployment group
aws deploy create-deployment-group \
  --application-name mini-java-app \
  --deployment-group-name mini-java-app-dg \
  --deployment-config-name CodeDeployDefault.ECSAllAtOnce \
  --service-role-arn arn:aws:iam::123456789012:role/CodeDeployServiceRole \
  --ecs-services clusterName=mini-java-app-cluster,serviceName=mini-java-app-service \
  --load-balancer-info targetGroupInfoList=[{name=mini-java-app-tg}] \
  --blue-green-deployment-configuration file://blue-green-config.json
```

### Rolling Updates

ECS supports rolling updates by default:

```bash
# Update service with new task definition
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:2 \
  --deployment-configuration maximumPercent=200,minimumHealthyPercent=50 \
  --region us-east-1
```

### Rollback

```bash
# Rollback to previous task definition
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:1 \
  --force-new-deployment \
  --region us-east-1
```

---

## Additional Resources

### AWS Documentation
- [ECS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [ECS Task Definitions](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html)
- [ECS Service Auto Scaling](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html)
- [CloudWatch Logs](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/WhatIsCloudWatchLogs.html)

### Best Practices
- [ECS Best Practices Guide](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Java Container Best Practices](https://aws.amazon.com/blogs/containers/java-application-optimization-on-amazon-ecs-on-aws-fargate/)

### Support
- AWS Support: https://aws.amazon.com/support/
- ECS Forum: https://forums.aws.amazon.com/forum.jspa?forumID=187
- Stack Overflow: Tag `amazon-ecs`

---

## Conclusion

This deployment guide provides comprehensive instructions for containerizing and deploying the Mini Java Application to AWS ECS Fargate. Follow the steps carefully, and refer to the troubleshooting section for common issues.

For production deployments, ensure you:
- Implement proper security measures
- Set up monitoring and alerting
- Configure auto-scaling
- Use secrets management for sensitive data
- Implement CI/CD pipelines for automated deployments

**Happy Deploying! 🚀**
