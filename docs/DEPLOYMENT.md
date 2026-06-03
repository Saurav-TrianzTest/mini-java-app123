# Deployment Guide for Mini Java App on AWS ECS Fargate

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Docker Deployment](#docker-deployment)
5. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
6. [ECS Fargate Setup](#ecs-fargate-setup)
7. [ECS Task Definition Explained](#ecs-task-definition-explained)
8. [ECS Service Configuration](#ecs-service-configuration)
9. [ECS Fargate Deployment Walkthrough](#ecs-fargate-deployment-walkthrough)
10. [ECS-Specific Troubleshooting](#ecs-specific-troubleshooting)
11. [ECS Fargate Scaling and Management](#ecs-fargate-scaling-and-management)
12. [Configuration Management](#configuration-management)
13. [Security Considerations](#security-considerations)
14. [Technology-Specific Notes](#technology-specific-notes)

---

## Overview

This guide provides comprehensive instructions for deploying the Mini Java App, a Spring Boot application, to AWS ECS Fargate. The application is containerized using Docker and deployed as a serverless container service on AWS.

**Application Details:**
- **Technology Stack**: Java 11, Spring Boot 2.7.0, Maven
- **Application Type**: Spring Boot Web Application with REST API
- **Application Port**: 8080
- **Health Check Endpoint**: `/actuator/health`
- **Management Endpoints**: Spring Boot Actuator enabled
- **Base Image**: Amazon Corretto 11 (runtime)

---

## Prerequisites

### Required Software
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 1.29 or higher
- **AWS CLI**: Version 2.x
- **Java**: JDK 11 (for local development)
- **Maven**: Version 3.6 or higher (for local builds)

### AWS Account Requirements
- Active AWS account with appropriate permissions
- IAM user with programmatic access
- AWS CLI configured with credentials

### Required AWS Permissions
Your IAM user/role must have permissions for:
- ECS (create/update clusters, services, task definitions)
- ECR (create repositories, push images)
- EC2 (VPC, subnets, security groups)
- IAM (pass role to ECS tasks)
- CloudWatch Logs (create log groups, write logs)
- Elastic Load Balancing (create ALB, target groups, listeners)

---

## Local Development Setup

### 1. Clone the Repository
```bash
cd /path/to/Testpathway-mcontainerco
```

### 2. Build the Application Locally
```bash
# Using Maven
mvn clean package -DskipTests

# The JAR file will be created in target/mini-java-app-1.0.0.jar
```

### 3. Run Locally (Without Docker)
```bash
# Set environment variables
export SERVER_PORT=8080
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=mini_app_db
export DB_USERNAME=root
export DB_PASSWORD=password123

# Run the application
java -jar target/mini-java-app-1.0.0.jar
```

### 4. Test the Application
```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
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
docker-compose logs -f mini-java-app

# Stop the application
docker-compose down
```

### 3. Test Containerized Application
```bash
# Health check
curl http://localhost:8080/actuator/health

# View container logs
docker logs mini-java-app
```

---

## AWS ECS Fargate Prerequisites

### 1. AWS CLI Configuration
```bash
# Configure AWS CLI
aws configure

# Enter your credentials:
# AWS Access Key ID: YOUR_ACCESS_KEY
# AWS Secret Access Key: YOUR_SECRET_KEY
# Default region name: us-east-1
# Default output format: json

# Verify configuration
aws sts get-caller-identity
```

### 2. VPC and Network Setup

#### Option A: Use Default VPC
```bash
# Get default VPC ID
aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text

# Get default subnets
aws ec2 describe-subnets --filters "Name=vpc-id,Values=YOUR_VPC_ID" --query "Subnets[*].SubnetId" --output text
```

#### Option B: Create New VPC (Recommended for Production)
```bash
# Create VPC
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=mini-java-app-vpc}]'

# Create subnets in different availability zones
aws ec2 create-subnet --vpc-id YOUR_VPC_ID --cidr-block 10.0.1.0/24 --availability-zone us-east-1a
aws ec2 create-subnet --vpc-id YOUR_VPC_ID --cidr-block 10.0.2.0/24 --availability-zone us-east-1b

# Create and attach Internet Gateway
aws ec2 create-internet-gateway --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=mini-java-app-igw}]'
aws ec2 attach-internet-gateway --vpc-id YOUR_VPC_ID --internet-gateway-id YOUR_IGW_ID

# Update route table
aws ec2 create-route --route-table-id YOUR_ROUTE_TABLE_ID --destination-cidr-block 0.0.0.0/0 --gateway-id YOUR_IGW_ID
```

### 3. Security Group Configuration
```bash
# Create security group
aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for Mini Java App" \
  --vpc-id YOUR_VPC_ID

# Allow inbound traffic on port 8080 (application)
aws ec2 authorize-security-group-ingress \
  --group-id YOUR_SG_ID \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow inbound traffic on port 80 (ALB)
aws ec2 authorize-security-group-ingress \
  --group-id YOUR_SG_ID \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# Allow outbound traffic (all)
aws ec2 authorize-security-group-egress \
  --group-id YOUR_SG_ID \
  --protocol -1 \
  --cidr 0.0.0.0/0
```

### 4. IAM Roles Setup

#### ECS Task Execution Role
```bash
# Create trust policy file (trust-policy.json)
cat > trust-policy.json <<EOF
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
EOF

# Create execution role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://trust-policy.json

# Attach AWS managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ECS Task Role (for application permissions)
```bash
# Create task role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file://trust-policy.json

# Attach policies for S3, SSM, etc.
aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess

aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonSSMReadOnlyAccess
```

---

## ECS Fargate Setup

### 1. Create ECR Repository
```bash
# Create repository
aws ecr create-repository --repository-name mini-java-app --region us-east-1

# Get repository URI
aws ecr describe-repositories --repository-names mini-java-app --query "repositories[0].repositoryUri" --output text
```

### 2. Build and Push Docker Image
```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Build image
docker build -t mini-java-app:latest .

# Tag image
docker tag mini-java-app:latest YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest

# Push image
docker push YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

**OR use the automated script:**
```bash
# Linux/macOS
chmod +x scripts/build-push.sh
./scripts/build-push.sh

# Windows
scripts\build-push.bat
```

### 3. Create CloudWatch Log Group
```bash
aws logs create-log-group --log-group-name /ecs/mini-java-app --region us-east-1
```

---

## ECS Task Definition Explained

The task definition (`ecs/task-definition.json`) defines how your container runs on ECS Fargate.

### Key Components:

#### 1. Fargate Configuration
```json
{
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc",
  "cpu": "512",
  "memory": "1024"
}
```
- **requiresCompatibilities**: Must be `["FARGATE"]` for Fargate deployment
- **networkMode**: Must be `"awsvpc"` for Fargate (each task gets its own ENI)
- **cpu**: Valid values: "256", "512", "1024", "2048", "4096"
- **memory**: Must be compatible with CPU (512 CPU = 1024-4096 MB)

#### 2. IAM Roles
```json
{
  "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskRole"
}
```
- **executionRoleArn**: Allows ECS to pull images from ECR and write logs to CloudWatch
- **taskRoleArn**: Allows your application to access AWS services (S3, SSM, etc.)

#### 3. Container Definition
```json
{
  "containerDefinitions": [
    {
      "name": "mini-java-app",
      "image": "ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ]
    }
  ]
}
```
- **name**: Container name (used in service definition)
- **image**: Full ECR image URI
- **essential**: If true, task stops if this container stops
- **portMappings**: Only containerPort is needed for Fargate (no hostPort)

#### 4. Environment Variables
```json
{
  "environment": [
    {
      "name": "SERVER_PORT",
      "value": "8080"
    },
    {
      "name": "JAVA_OPTS",
      "value": "-Xmx512m -Xms256m -XX:+UseContainerSupport"
    }
  ]
}
```

#### 5. Logging Configuration
```json
{
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

---

## ECS Service Configuration

The service definition (`ecs/service-definition.json`) manages the deployment and scaling of your tasks.

### Key Components:

#### 1. Service Configuration
```json
{
  "serviceName": "mini-java-app-service",
  "cluster": "mini-java-app-cluster",
  "taskDefinition": "mini-java-app-task",
  "desiredCount": 2,
  "launchType": "FARGATE"
}
```
- **desiredCount**: Number of tasks to run (2 for high availability)
- **launchType**: Must be "FARGATE"

#### 2. Network Configuration
```json
{
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-xxx", "subnet-yyy"],
      "securityGroups": ["sg-xxx"],
      "assignPublicIp": "ENABLED"
    }
  }
}
```
- **subnets**: At least 2 subnets in different AZs for high availability
- **securityGroups**: Security group allowing inbound traffic on port 8080
- **assignPublicIp**: "ENABLED" if using public subnets, "DISABLED" for private

#### 3. Load Balancer Configuration
```json
{
  "loadBalancers": [
    {
      "targetGroupArn": "arn:aws:elasticloadbalancing:...",
      "containerName": "mini-java-app",
      "containerPort": 8080
    }
  ],
  "healthCheckGracePeriodSeconds": 300
}
```
- **targetGroupArn**: ARN of the target group (created by deploy script)
- **healthCheckGracePeriodSeconds**: Time before health checks start (300s for JVM startup)

#### 4. Deployment Configuration
```json
{
  "deploymentConfiguration": {
    "maximumPercent": 200,
    "minimumHealthyPercent": 50
  }
}
```
- **maximumPercent**: Maximum tasks during deployment (200 = 2x desired count)
- **minimumHealthyPercent**: Minimum healthy tasks during deployment (50%)

---

## ECS Fargate Deployment Walkthrough

### Step 1: Prepare Environment
```bash
# Set variables
export AWS_REGION=us-east-1
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export CLUSTER_NAME=mini-java-app-cluster
export VPC_ID=vpc-xxx
export SUBNET_1=subnet-xxx
export SUBNET_2=subnet-yyy
export SECURITY_GROUP=sg-xxx
```

### Step 2: Build and Push Image
```bash
# Use the automated script
./scripts/build-push.sh

# Follow prompts:
# 1. Select AWS ECR
# 2. Enter AWS region: us-east-1
# 3. Enter AWS Account ID: YOUR_ACCOUNT_ID
# 4. Enter ECR repository name: mini-java-app
# 5. Enter image tag: latest
```

### Step 3: Deploy to ECS Fargate
```bash
# Use the automated deployment script
./scripts/deploy-image.sh

# Follow prompts:
# 1. Enter AWS region: us-east-1
# 2. Enter ECS cluster name: mini-java-app-cluster
# 3. Enter VPC ID: vpc-xxx
# 4. Enter subnet IDs: subnet-xxx,subnet-yyy
# 5. Enter security group ID: sg-xxx
# 6. Enter Docker image URI: ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
# 7. Do you need a load balancer? y
# 8. Enter database configuration (host, port, name, username, password)
# 9. Enter Redis configuration
# 10. Enter S3 bucket name
```

### Step 4: Verify Deployment
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

# View logs
aws logs tail /ecs/mini-java-app --follow --region us-east-1
```

### Step 5: Test the Application
```bash
# Get ALB DNS name
ALB_DNS=$(aws elbv2 describe-load-balancers \
  --names mini-java-app-alb \
  --query "LoadBalancers[0].DNSName" \
  --output text)

# Test health endpoint
curl http://$ALB_DNS/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## ECS-Specific Troubleshooting

### Task Failures

#### Issue: Task fails to start
**Symptoms:**
- Tasks transition from PENDING to STOPPED
- No logs in CloudWatch

**Solutions:**
1. Check task definition is valid:
   ```bash
   aws ecs describe-task-definition --task-definition mini-java-app-task
   ```

2. Verify IAM roles exist and have correct permissions:
   ```bash
   aws iam get-role --role-name ecsTaskExecutionRole
   aws iam get-role --role-name ecsTaskRole
   ```

3. Check ECR image exists and is accessible:
   ```bash
   aws ecr describe-images --repository-name mini-java-app
   ```

#### Issue: Task starts but immediately stops
**Symptoms:**
- Task reaches RUNNING state briefly
- Application logs show errors

**Solutions:**
1. Check CloudWatch logs:
   ```bash
   aws logs tail /ecs/mini-java-app --follow
   ```

2. Common issues:
   - Database connection failures (check DB_HOST, DB_PORT)
   - Missing environment variables
   - JVM memory issues (increase task memory)
   - Application startup errors

### Network Issues

#### Issue: Cannot access application via ALB
**Symptoms:**
- ALB health checks failing
- 503 Service Unavailable errors

**Solutions:**
1. Verify security group allows traffic:
   ```bash
   aws ec2 describe-security-groups --group-ids YOUR_SG_ID
   ```
   - Inbound: Port 8080 from ALB security group
   - Outbound: All traffic

2. Check target group health:
   ```bash
   aws elbv2 describe-target-health --target-group-arn YOUR_TG_ARN
   ```

3. Verify subnets have internet access:
   - Public subnets: Internet Gateway attached
   - Private subnets: NAT Gateway configured

#### Issue: Task cannot pull image from ECR
**Symptoms:**
- Error: "CannotPullContainerError"

**Solutions:**
1. Verify execution role has ECR permissions:
   ```bash
   aws iam list-attached-role-policies --role-name ecsTaskExecutionRole
   ```
   Should include: `AmazonECSTaskExecutionRolePolicy`

2. Check subnets have internet access (for ECR)

3. Verify image URI is correct in task definition

### CPU/Memory Errors

#### Issue: Task stopped due to OutOfMemory
**Symptoms:**
- Task stopped with exit code 137
- Logs show: "java.lang.OutOfMemoryError"

**Solutions:**
1. Increase task memory in task definition:
   ```json
   {
     "cpu": "1024",
     "memory": "2048"
   }
   ```

2. Adjust JVM heap size:
   ```json
   {
     "name": "JAVA_OPTS",
     "value": "-Xmx1536m -Xms512m -XX:+UseContainerSupport"
   }
   ```

3. Valid Fargate CPU/Memory combinations:
   - CPU 512: Memory 1024-4096 MB
   - CPU 1024: Memory 2048-8192 MB
   - CPU 2048: Memory 4096-16384 MB

#### Issue: Task stopped due to CPU throttling
**Symptoms:**
- Slow application performance
- High CPU utilization in CloudWatch metrics

**Solutions:**
1. Increase CPU allocation:
   ```json
   {
     "cpu": "1024",
     "memory": "2048"
   }
   ```

2. Enable Service Auto Scaling (see below)

### Deployment Issues

#### Issue: Service update stuck in progress
**Symptoms:**
- Deployment takes longer than expected
- Old tasks not being replaced

**Solutions:**
1. Check deployment configuration:
   ```bash
   aws ecs describe-services --cluster CLUSTER --services SERVICE
   ```

2. Verify health check grace period is sufficient (300s for Spring Boot)

3. Check if new tasks are failing health checks:
   ```bash
   aws elbv2 describe-target-health --target-group-arn TG_ARN
   ```

4. Force new deployment:
   ```bash
   aws ecs update-service \
     --cluster CLUSTER \
     --service SERVICE \
     --force-new-deployment
   ```

---

## ECS Fargate Scaling and Management

### Service Auto Scaling

#### 1. Register Scalable Target
```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10 \
  --region us-east-1
```

#### 2. Create Scaling Policy (CPU-based)
```bash
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

#### 3. Create Scaling Policy (Memory-based)
```bash
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name memory-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 80.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageMemoryUtilization"
    },
    "ScaleInCooldown": 300,
    "ScaleOutCooldown": 60
  }' \
  --region us-east-1
```

### Blue/Green Deployments

#### 1. Create CodeDeploy Application
```bash
aws deploy create-application \
  --application-name mini-java-app \
  --compute-platform ECS \
  --region us-east-1
```

#### 2. Create Deployment Group
```bash
aws deploy create-deployment-group \
  --application-name mini-java-app \
  --deployment-group-name mini-java-app-dg \
  --service-role-arn arn:aws:iam::ACCOUNT_ID:role/CodeDeployServiceRole \
  --ecs-services clusterName=mini-java-app-cluster,serviceName=mini-java-app-service \
  --load-balancer-info targetGroupPairInfoList=[{targetGroups=[{name=mini-java-app-tg-blue},{name=mini-java-app-tg-green}],prodTrafficRoute={listenerArns=[arn:aws:elasticloadbalancing:...]}}] \
  --deployment-config-name CodeDeployDefault.ECSAllAtOnce \
  --region us-east-1
```

### Rolling Updates

Update service with new task definition:
```bash
# Register new task definition
aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json

# Update service
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:NEW_REVISION \
  --region us-east-1

# Wait for deployment to complete
aws ecs wait services-stable \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1
```

### Monitoring and Observability

#### CloudWatch Metrics
```bash
# View CPU utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 300 \
  --statistics Average \
  --region us-east-1

# View memory utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name MemoryUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 300 \
  --statistics Average \
  --region us-east-1
```

#### CloudWatch Logs Insights
```bash
# Query application logs
aws logs start-query \
  --log-group-name /ecs/mini-java-app \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --query-string 'fields @timestamp, @message | filter @message like /ERROR/ | sort @timestamp desc | limit 20' \
  --region us-east-1
```

---

## Configuration Management

### Environment Variables

#### Method 1: Task Definition (Static)
Define in `ecs/task-definition.json`:
```json
{
  "environment": [
    {
      "name": "DB_HOST",
      "value": "database.example.com"
    }
  ]
}
```

#### Method 2: AWS Systems Manager Parameter Store
```bash
# Store parameter
aws ssm put-parameter \
  --name /mini-java-app/db-password \
  --value "your-secure-password" \
  --type SecureString \
  --region us-east-1

# Reference in task definition
{
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:ssm:us-east-1:ACCOUNT_ID:parameter/mini-java-app/db-password"
    }
  ]
}
```

#### Method 3: AWS Secrets Manager
```bash
# Create secret
aws secretsmanager create-secret \
  --name mini-java-app/database \
  --secret-string '{"username":"admin","password":"secure-password"}' \
  --region us-east-1

# Reference in task definition
{
  "secrets": [
    {
      "name": "DB_USERNAME",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:mini-java-app/database:username::"
    },
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:mini-java-app/database:password::"
    }
  ]
}
```

### Application Configuration Files

#### Method 1: Bake into Docker Image
Include in Dockerfile:
```dockerfile
COPY src/main/resources/application.properties /app/config/
```

#### Method 2: S3 Configuration
```bash
# Upload configuration
aws s3 cp application-prod.properties s3://mini-app-config-bucket/config/

# Download at runtime (add to entrypoint script)
aws s3 cp s3://mini-app-config-bucket/config/application-prod.properties /app/config/
```

#### Method 3: EFS Volume (for shared configuration)
```bash
# Create EFS file system
aws efs create-file-system --tags Key=Name,Value=mini-java-app-config

# Add volume to task definition
{
  "volumes": [
    {
      "name": "config",
      "efsVolumeConfiguration": {
        "fileSystemId": "fs-xxx",
        "transitEncryption": "ENABLED"
      }
    }
  ],
  "mountPoints": [
    {
      "sourceVolume": "config",
      "containerPath": "/app/config",
      "readOnly": true
    }
  ]
}
```

---

## Security Considerations

### 1. Network Security

#### VPC Configuration
- Use private subnets for ECS tasks
- Use public subnets for ALB only
- Configure NAT Gateway for outbound internet access

#### Security Groups
```bash
# ECS Task Security Group
- Inbound: Port 8080 from ALB security group only
- Outbound: All traffic (for database, Redis, S3, etc.)

# ALB Security Group
- Inbound: Port 80/443 from 0.0.0.0/0
- Outbound: Port 8080 to ECS task security group
```

### 2. IAM Security

#### Principle of Least Privilege
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
      "Resource": "arn:aws:s3:::mini-app-config-bucket/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:us-east-1:ACCOUNT_ID:parameter/mini-java-app/*"
    }
  ]
}
```

### 3. Secrets Management

**Never hardcode secrets in:**
- Docker images
- Task definitions
- Source code

**Use AWS Secrets Manager or Parameter Store:**
```bash
# Rotate secrets regularly
aws secretsmanager rotate-secret \
  --secret-id mini-java-app/database \
  --rotation-lambda-arn arn:aws:lambda:... \
  --rotation-rules AutomaticallyAfterDays=30
```

### 4. Container Security

#### Use Non-Root User
Already implemented in Dockerfile:
```dockerfile
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
```

#### Scan Images for Vulnerabilities
```bash
# Using AWS ECR image scanning
aws ecr start-image-scan \
  --repository-name mini-java-app \
  --image-id imageTag=latest \
  --region us-east-1

# View scan results
aws ecr describe-image-scan-findings \
  --repository-name mini-java-app \
  --image-id imageTag=latest \
  --region us-east-1
```

### 5. Logging and Monitoring

#### Enable CloudTrail
```bash
aws cloudtrail create-trail \
  --name mini-java-app-trail \
  --s3-bucket-name mini-java-app-cloudtrail \
  --is-multi-region-trail
```

#### Enable VPC Flow Logs
```bash
aws ec2 create-flow-logs \
  --resource-type VPC \
  --resource-ids vpc-xxx \
  --traffic-type ALL \
  --log-destination-type cloud-watch-logs \
  --log-group-name /aws/vpc/mini-java-app
```

---

## Technology-Specific Notes

### Spring Boot Configuration

#### 1. Actuator Endpoints
The application uses Spring Boot Actuator for health checks and monitoring:
- Health: `/actuator/health`
- Info: `/actuator/info`
- Metrics: `/actuator/metrics`

#### 2. Spring Profiles
Use Spring profiles for environment-specific configuration:
```bash
# Set in task definition
{
  "name": "SPRING_PROFILES_ACTIVE",
  "value": "docker,production"
}
```

#### 3. Graceful Shutdown
Spring Boot supports graceful shutdown:
```properties
# application.properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### Java/JVM Optimization

#### 1. Container-Aware JVM
The Dockerfile uses container-aware JVM flags:
```bash
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

#### 2. Garbage Collection
For production, consider G1GC:
```bash
JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

#### 3. JVM Monitoring
Enable JMX for monitoring:
```bash
JAVA_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
```

### Maven Build Optimization

#### 1. Dependency Caching
The Dockerfile uses layer caching:
```dockerfile
# Copy pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Then copy source
COPY src ./src
RUN mvn clean package -DskipTests -B
```

#### 2. Multi-Module Projects
For multi-module Maven projects:
```dockerfile
# Copy entire project structure
COPY . .

# Build parent POM
RUN mvn clean install -N -DskipTests

# Build all modules
RUN mvn clean package -DskipTests
```

### Database Connectivity

#### 1. Connection Pooling
Configure HikariCP (default in Spring Boot):
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

#### 2. RDS Integration
Use RDS Proxy for better connection management:
```bash
# Create RDS Proxy
aws rds create-db-proxy \
  --db-proxy-name mini-java-app-proxy \
  --engine-family MYSQL \
  --auth [{AuthScheme=SECRETS,SecretArn=arn:aws:secretsmanager:...}] \
  --role-arn arn:aws:iam::ACCOUNT_ID:role/RDSProxyRole \
  --vpc-subnet-ids subnet-xxx subnet-yyy

# Update DB_HOST to proxy endpoint
DB_HOST=mini-java-app-proxy.proxy-xxx.us-east-1.rds.amazonaws.com
```

---

## Additional Resources

### AWS Documentation
- [ECS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [ECS Task Definitions](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html)
- [ECS Service Auto Scaling](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html)

### Spring Boot Documentation
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Spring Boot Docker](https://spring.io/guides/gs/spring-boot-docker/)

### Best Practices
- [AWS ECS Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Java in Containers](https://developers.redhat.com/blog/2017/03/14/java-inside-docker)

---

## Support and Troubleshooting

For issues or questions:
1. Check CloudWatch logs: `/ecs/mini-java-app`
2. Review ECS service events: `aws ecs describe-services`
3. Check task stopped reason: `aws ecs describe-tasks`
4. Review security group rules and network configuration
5. Verify IAM roles and permissions

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Application**: Mini Java App  
**Platform**: AWS ECS Fargate
