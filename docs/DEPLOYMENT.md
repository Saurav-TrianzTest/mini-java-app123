# Mini Java Application - AWS ECS Fargate Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Architecture](#architecture)
4. [Local Development](#local-development)
5. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
6. [AWS ECS Fargate Setup](#aws-ecs-fargate-setup)
7. [Deployment Process](#deployment-process)
8. [Configuration Management](#configuration-management)
9. [Monitoring and Logging](#monitoring-and-logging)
10. [Troubleshooting](#troubleshooting)
11. [Scaling and Management](#scaling-and-management)
12. [Security Best Practices](#security-best-practices)

---

## Overview

This guide provides comprehensive instructions for deploying the Mini Java Application to AWS ECS Fargate. The application is containerized using Docker and deployed as a serverless container service on AWS.

### Application Details
- **Technology Stack**: Java 11, Spring Boot 2.7.0, Maven
- **Base Image**: Amazon Corretto 11 (as specified)
- **Application Port**: 8080
- **Context Path**: /mini-app
- **Deployment Platform**: AWS ECS Fargate

---

## Prerequisites

### Required Tools
1. **Docker** (version 20.10 or later)
   - Download: https://www.docker.com/products/docker-desktop
   - Verify: `docker --version`

2. **AWS CLI** (version 2.x)
   - Download: https://aws.amazon.com/cli/
   - Verify: `aws --version`
   - Configure: `aws configure`

3. **Git** (for version control)
   - Download: https://git-scm.com/
   - Verify: `git --version`

4. **Java 11** (for local development)
   - Download: https://adoptium.net/
   - Verify: `java -version`

5. **Maven 3.6+** (for local builds)
   - Download: https://maven.apache.org/download.cgi
   - Verify: `mvn -version`

### AWS Account Requirements
1. **AWS Account** with appropriate permissions
2. **IAM User** with the following permissions:
   - ECS Full Access
   - ECR Full Access
   - CloudWatch Logs Full Access
   - IAM Role Creation (for task execution role)
   - VPC and Networking permissions
   - Application Load Balancer permissions

3. **AWS Resources**:
   - VPC with at least 2 subnets in different availability zones
   - Security Group allowing inbound traffic on port 8080
   - IAM Role: `ecsTaskExecutionRole` (for ECS to pull images and write logs)
   - IAM Role: `ecsTaskRole` (optional, for task-level permissions like S3 access)

---

## Architecture

### Container Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    Application Load Balancer                │
│                         (Port 80)                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    ECS Fargate Service                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              ECS Task (Container)                     │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │   Mini Java Application (Port 8080)            │  │  │
│  │  │   - Java 11 Runtime                            │  │  │
│  │  │   - Spring Boot Application                    │  │  │
│  │  │   - Externalized Configuration                 │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   External Services                          │
│  - RDS MySQL Database                                        │
│  - ElastiCache Redis                                         │
│  - S3 Bucket (Configuration)                                 │
│  - External APIs                                             │
└─────────────────────────────────────────────────────────────┘
```

### Multi-Stage Docker Build
The Dockerfile uses a multi-stage build process:
1. **Builder Stage**: Uses `maven:3.9.4-eclipse-temurin-11` to compile the application
2. **Runtime Stage**: Uses `amazoncorretto:11` for a minimal runtime image

---

## Local Development

### Running with Docker Compose

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd mini-java-app-component
   ```

2. **Configure environment variables**:
   Create a `.env` file in the project root:
   ```env
   # Database Configuration
   DB_HOST=your-db-host.rds.amazonaws.com
   DB_PORT=3306
   DB_NAME=mini_app_db
   DB_USERNAME=appuser
   DB_PASSWORD=your-secure-password
   
   # Redis Configuration
   REDIS_HOST=your-redis.cache.amazonaws.com
   REDIS_PORT=6379
   REDIS_PASSWORD=your-redis-password
   
   # AWS Configuration
   AWS_REGION=us-east-1
   AWS_ACCESS_KEY_ID=your-access-key
   AWS_SECRET_ACCESS_KEY=your-secret-key
   CONFIG_S3_BUCKET=your-config-bucket
   CONFIG_S3_KEY=config/app.properties
   
   # External Services
   EXTERNAL_API_URL=http://api.example.com:8080/v1
   PAYMENT_SERVICE_URL=https://payment.service.local/process
   ```

3. **Build and run with Docker Compose**:
   ```bash
   docker-compose up --build
   ```

4. **Access the application**:
   - Application: http://localhost:8080/mini-app
   - Health Check: http://localhost:8080/mini-app/health

5. **View logs**:
   ```bash
   docker-compose logs -f mini-java-app
   ```

6. **Stop the application**:
   ```bash
   docker-compose down
   ```

### Local Maven Build

1. **Build the application**:
   ```bash
   mvn clean package -DskipTests
   ```

2. **Run locally**:
   ```bash
   java -jar target/mini-java-app-1.0.0.jar
   ```

---

## Building and Pushing Docker Images

### Option 1: AWS ECR (Recommended for ECS)

1. **Run the build script**:
   ```bash
   # Linux/macOS
   chmod +x scripts/build-push.sh
   ./scripts/build-push.sh
   
   # Windows
   scripts\build-push.bat
   ```

2. **Follow the prompts**:
   - Select option `1` for AWS ECR
   - Enter AWS Region (e.g., `us-east-1`)
   - Enter AWS Account ID (e.g., `123456789012`)
   - Enter ECR Repository Name (default: `mini-java-app`)
   - Enter image tag (default: `latest`)

3. **Script will automatically**:
   - Authenticate with AWS ECR
   - Create ECR repository if it doesn't exist
   - Build the Docker image
   - Push the image to ECR

### Option 2: Docker Hub

1. **Run the build script**:
   ```bash
   ./scripts/build-push.sh  # or scripts\build-push.bat on Windows
   ```

2. **Follow the prompts**:
   - Select option `2` for Docker Hub
   - Enter Docker Hub username
   - Enter Docker Hub password/token
   - Enter image tag (default: `latest`)

### Manual Build (Advanced)

```bash
# Build the image
docker build -t mini-java-app:latest .

# Tag for ECR
docker tag mini-java-app:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Push to ECR
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

---

## AWS ECS Fargate Setup

### 1. Create IAM Roles

#### ECS Task Execution Role
This role allows ECS to pull images from ECR and write logs to CloudWatch.

```bash
# Create trust policy file
cat > ecs-task-execution-trust-policy.json <<EOF
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

# Create the role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach AWS managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ECS Task Role (Optional)
This role provides permissions for the application to access AWS services (e.g., S3, DynamoDB).

```bash
# Create the role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach policies as needed (example: S3 access)
aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
```

### 2. Create VPC and Networking (if not exists)

```bash
# Create VPC
VPC_ID=$(aws ec2 create-vpc --cidr-block 10.0.0.0/16 --query 'Vpc.VpcId' --output text)

# Create subnets in different availability zones
SUBNET_1=$(aws ec2 create-subnet --vpc-id $VPC_ID --cidr-block 10.0.1.0/24 --availability-zone us-east-1a --query 'Subnet.SubnetId' --output text)
SUBNET_2=$(aws ec2 create-subnet --vpc-id $VPC_ID --cidr-block 10.0.2.0/24 --availability-zone us-east-1b --query 'Subnet.SubnetId' --output text)

# Create Internet Gateway
IGW_ID=$(aws ec2 create-internet-gateway --query 'InternetGateway.InternetGatewayId' --output text)
aws ec2 attach-internet-gateway --vpc-id $VPC_ID --internet-gateway-id $IGW_ID

# Create route table and associate with subnets
ROUTE_TABLE_ID=$(aws ec2 create-route-table --vpc-id $VPC_ID --query 'RouteTable.RouteTableId' --output text)
aws ec2 create-route --route-table-id $ROUTE_TABLE_ID --destination-cidr-block 0.0.0.0/0 --gateway-id $IGW_ID
aws ec2 associate-route-table --route-table-id $ROUTE_TABLE_ID --subnet-id $SUBNET_1
aws ec2 associate-route-table --route-table-id $ROUTE_TABLE_ID --subnet-id $SUBNET_2

# Create security group
SG_ID=$(aws ec2 create-security-group --group-name mini-java-app-sg --description "Security group for Mini Java App" --vpc-id $VPC_ID --query 'GroupId' --output text)

# Allow inbound traffic on port 8080
aws ec2 authorize-security-group-ingress --group-id $SG_ID --protocol tcp --port 8080 --cidr 0.0.0.0/0

# Allow inbound traffic on port 80 (for ALB)
aws ec2 authorize-security-group-ingress --group-id $SG_ID --protocol tcp --port 80 --cidr 0.0.0.0/0
```

### 3. Create CloudWatch Log Group

```bash
aws logs create-log-group --log-group-name /ecs/mini-java-app
```

### 4. Create ECS Cluster

```bash
aws ecs create-cluster --cluster-name mini-java-app-cluster
```

---

## Deployment Process

### Automated Deployment

1. **Run the deployment script**:
   ```bash
   # Linux/macOS
   chmod +x scripts/deploy-image.sh
   ./scripts/deploy-image.sh
   
   # Windows
   scripts\deploy-image.bat
   ```

2. **Follow the prompts**:
   - Enter AWS Region
   - Enter ECS Cluster Name
   - Enter VPC ID
   - Enter Subnet IDs (comma-separated)
   - Enter Security Group ID
   - Enter ECR Image URI
   - Choose whether to create a load balancer (y/n)

3. **Script will automatically**:
   - Create or verify ECS cluster
   - Create Application Load Balancer and Target Group (if requested)
   - Register task definition with all environment variables
   - Create or update ECS service
   - Wait for service to stabilize
   - Display deployment details and access URLs

### Manual Deployment

#### Step 1: Register Task Definition

```bash
# Update placeholders in task-definition.json
sed -i 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest|g' ecs/task-definition.json
sed -i 's|{{ACCOUNT_ID}}|123456789012|g' ecs/task-definition.json
sed -i 's|{{AWS_REGION}}|us-east-1|g' ecs/task-definition.json

# Register the task definition
aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json
```

#### Step 2: Create ECS Service

```bash
# Update placeholders in service-definition.json
sed -i 's|{{CLUSTER_NAME}}|mini-java-app-cluster|g' ecs/service-definition.json
sed -i 's|{{SUBNET_1}}|subnet-xxxxx|g' ecs/service-definition.json
sed -i 's|{{SUBNET_2}}|subnet-yyyyy|g' ecs/service-definition.json
sed -i 's|{{SECURITY_GROUP}}|sg-zzzzz|g' ecs/service-definition.json

# Create the service
aws ecs create-service --cli-input-json file://ecs/service-definition.json --cluster mini-java-app-cluster
```

#### Step 3: Verify Deployment

```bash
# Check service status
aws ecs describe-services --cluster mini-java-app-cluster --services mini-java-app-service

# List running tasks
aws ecs list-tasks --cluster mini-java-app-cluster --service-name mini-java-app-service

# View task details
aws ecs describe-tasks --cluster mini-java-app-cluster --tasks <task-id>
```

---

## Configuration Management

### Environment Variables

The application uses environment variables for configuration. These are defined in:
1. **Task Definition** (`ecs/task-definition.json`): For ECS deployment
2. **Docker Compose** (`docker-compose.yml`): For local development

#### Key Configuration Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | Application port | 8080 | Yes |
| `DB_HOST` | Database host | localhost | Yes |
| `DB_PORT` | Database port | 3306 | Yes |
| `DB_NAME` | Database name | mini_app_db | Yes |
| `DB_USERNAME` | Database username | root | Yes |
| `DB_PASSWORD` | Database password | - | Yes |
| `REDIS_HOST` | Redis host | redis.service.local | Yes |
| `REDIS_PORT` | Redis port | 6379 | Yes |
| `AWS_REGION` | AWS region | us-east-1 | Yes |
| `CONFIG_S3_BUCKET` | S3 bucket for config | app-config-bucket | Yes |
| `CONFIG_S3_KEY` | S3 key for config | config/app.properties | Yes |

### Updating Configuration

#### For Running Service

1. **Update task definition** with new environment variables
2. **Register new task definition revision**:
   ```bash
   aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json
   ```
3. **Update service** to use new task definition:
   ```bash
   aws ecs update-service \
     --cluster mini-java-app-cluster \
     --service mini-java-app-service \
     --task-definition mini-java-app-task:2 \
     --force-new-deployment
   ```

#### Using AWS Systems Manager Parameter Store (Recommended)

Store sensitive configuration in Parameter Store:

```bash
# Store database password
aws ssm put-parameter \
  --name /mini-java-app/db-password \
  --value "your-secure-password" \
  --type SecureString

# Reference in task definition
{
  "name": "DB_PASSWORD",
  "valueFrom": "arn:aws:ssm:us-east-1:123456789012:parameter/mini-java-app/db-password"
}
```

---

## Monitoring and Logging

### CloudWatch Logs

All application logs are automatically sent to CloudWatch Logs.

#### View Logs

```bash
# Tail logs in real-time
aws logs tail /ecs/mini-java-app --follow

# View logs for specific time range
aws logs tail /ecs/mini-java-app --since 1h

# Filter logs
aws logs tail /ecs/mini-java-app --filter-pattern "ERROR"
```

#### Log Insights Queries

```sql
-- Find all errors
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 100

-- Application startup time
fields @timestamp, @message
| filter @message like /Started MiniApp/
| sort @timestamp desc

-- Database connection issues
fields @timestamp, @message
| filter @message like /Database connection failed/
| sort @timestamp desc
```

### CloudWatch Metrics

ECS automatically publishes metrics to CloudWatch:
- CPU Utilization
- Memory Utilization
- Network In/Out
- Task Count

#### View Metrics

```bash
# Get CPU utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 3600 \
  --statistics Average
```

### Application Load Balancer Monitoring

If using ALB, monitor:
- Target Health
- Request Count
- Response Time
- HTTP Error Codes

```bash
# Check target health
aws elbv2 describe-target-health --target-group-arn <target-group-arn>
```

---

## Troubleshooting

### Common Issues

#### 1. Task Fails to Start

**Symptoms**: Tasks start and immediately stop

**Possible Causes**:
- Invalid Docker image
- Insufficient CPU/Memory
- Missing IAM permissions
- Application crashes on startup

**Solutions**:
```bash
# Check task stopped reason
aws ecs describe-tasks --cluster mini-java-app-cluster --tasks <task-id> --query 'tasks[0].stoppedReason'

# View container logs
aws logs tail /ecs/mini-java-app --since 30m

# Check task definition
aws ecs describe-task-definition --task-definition mini-java-app-task
```

#### 2. Cannot Pull Image from ECR

**Symptoms**: "CannotPullContainerError"

**Solutions**:
- Verify `ecsTaskExecutionRole` has ECR permissions
- Check image URI is correct
- Verify ECR repository exists

```bash
# Test ECR access
aws ecr describe-repositories --repository-names mini-java-app

# Check execution role
aws iam get-role --role-name ecsTaskExecutionRole
```

#### 3. Service Not Reaching Healthy State

**Symptoms**: Tasks keep restarting, service never stabilizes

**Solutions**:
- Check health check configuration
- Verify application is listening on correct port
- Check security group allows traffic
- Increase health check grace period

```bash
# Update health check grace period
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --health-check-grace-period-seconds 300
```

#### 4. Database Connection Failures

**Symptoms**: "Database connection failed" in logs

**Solutions**:
- Verify database endpoint is correct
- Check security group allows traffic from ECS tasks
- Verify database credentials
- Check VPC networking configuration

```bash
# Test database connectivity from ECS task
aws ecs execute-command \
  --cluster mini-java-app-cluster \
  --task <task-id> \
  --container mini-java-app \
  --interactive \
  --command "/bin/sh"

# Inside container
nc -zv your-db-host.rds.amazonaws.com 3306
```

#### 5. Out of Memory Errors

**Symptoms**: Tasks killed due to OOM

**Solutions**:
- Increase task memory in task definition
- Adjust JVM heap size in `JAVA_OPTS`
- Monitor memory usage

```bash
# Update task definition with more memory
# Change "memory": "1024" to "memory": "2048"
# Valid Fargate combinations:
# CPU: 512 -> Memory: 1024, 2048, 3072, 4096
# CPU: 1024 -> Memory: 2048-8192 (increments of 1024)
```

### Debugging Commands

```bash
# Get service events
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --query 'services[0].events[0:10]'

# Get task details
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-id>

# Execute command in running container
aws ecs execute-command \
  --cluster mini-java-app-cluster \
  --task <task-id> \
  --container mini-java-app \
  --interactive \
  --command "/bin/bash"

# View CloudWatch logs
aws logs get-log-events \
  --log-group-name /ecs/mini-java-app \
  --log-stream-name ecs/mini-java-app/<task-id>
```

---

## Scaling and Management

### Manual Scaling

```bash
# Scale to 5 tasks
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --desired-count 5
```

### Auto Scaling

#### Step 1: Register Scalable Target

```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10
```

#### Step 2: Create Scaling Policy

**CPU-based scaling**:
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
  }'
```

**Memory-based scaling**:
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
  }'
```

### Blue/Green Deployments

For zero-downtime deployments, use AWS CodeDeploy with ECS:

1. **Create CodeDeploy application**:
   ```bash
   aws deploy create-application \
     --application-name mini-java-app \
     --compute-platform ECS
   ```

2. **Create deployment group**:
   ```bash
   aws deploy create-deployment-group \
     --application-name mini-java-app \
     --deployment-group-name mini-java-app-dg \
     --deployment-config-name CodeDeployDefault.ECSAllAtOnce \
     --service-role-arn arn:aws:iam::123456789012:role/CodeDeployServiceRole \
     --ecs-services clusterName=mini-java-app-cluster,serviceName=mini-java-app-service \
     --load-balancer-info targetGroupPairInfoList=[{targetGroups=[{name=mini-java-app-tg-blue},{name=mini-java-app-tg-green}],prodTrafficRoute={listenerArns=[arn:aws:elasticloadbalancing:us-east-1:123456789012:listener/app/mini-java-app-alb/xxx/yyy]}}]
   ```

### Rolling Updates

Update service with new task definition:

```bash
# Register new task definition
aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json

# Update service with rolling update
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:3 \
  --deployment-configuration "maximumPercent=200,minimumHealthyPercent=50" \
  --force-new-deployment
```

---

## Security Best Practices

### 1. Use Secrets Manager for Sensitive Data

```bash
# Store database password in Secrets Manager
aws secretsmanager create-secret \
  --name mini-java-app/db-password \
  --secret-string "your-secure-password"

# Reference in task definition
{
  "name": "DB_PASSWORD",
  "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:mini-java-app/db-password"
}
```

### 2. Use Private Subnets

Deploy ECS tasks in private subnets with NAT Gateway for outbound internet access.

### 3. Implement Network Segmentation

- Use separate security groups for ALB, ECS tasks, and databases
- Follow principle of least privilege for security group rules

### 4. Enable Container Insights

```bash
aws ecs update-cluster-settings \
  --cluster mini-java-app-cluster \
  --settings name=containerInsights,value=enabled
```

### 5. Use IAM Roles for Tasks

Grant only necessary permissions to task role:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::app-config-bucket/*"
    }
  ]
}
```

### 6. Enable VPC Flow Logs

```bash
aws ec2 create-flow-logs \
  --resource-type VPC \
  --resource-ids $VPC_ID \
  --traffic-type ALL \
  --log-destination-type cloud-watch-logs \
  --log-group-name /aws/vpc/mini-java-app
```

### 7. Regular Security Scanning

- Scan Docker images for vulnerabilities using AWS ECR image scanning
- Enable automatic scanning on push

```bash
aws ecr put-image-scanning-configuration \
  --repository-name mini-java-app \
  --image-scanning-configuration scanOnPush=true
```

---

## Cost Optimization

### 1. Right-Size Resources

Monitor CPU and memory usage and adjust task definition accordingly.

### 2. Use Fargate Spot

For non-critical workloads, use Fargate Spot for up to 70% cost savings:

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

### 3. Implement Auto Scaling

Scale down during off-peak hours to reduce costs.

### 4. Use CloudWatch Alarms

Set up billing alarms to monitor costs:

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name mini-java-app-cost-alarm \
  --alarm-description "Alert when estimated charges exceed $100" \
  --metric-name EstimatedCharges \
  --namespace AWS/Billing \
  --statistic Maximum \
  --period 21600 \
  --evaluation-periods 1 \
  --threshold 100 \
  --comparison-operator GreaterThanThreshold
```

---

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Spring Boot on AWS](https://aws.amazon.com/blogs/opensource/spring-boot-on-aws/)

---

## Support and Maintenance

For issues or questions:
1. Check CloudWatch Logs for application errors
2. Review ECS service events
3. Consult AWS Support
4. Review this deployment guide

---

**Last Updated**: 2024
**Version**: 1.0.0
