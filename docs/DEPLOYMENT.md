# Deployment Guide for mini-java-app

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Building the Docker Image](#building-the-docker-image)
4. [AWS ECS Fargate Deployment](#aws-ecs-fargate-deployment)
5. [Configuration Management](#configuration-management)
6. [Monitoring and Logging](#monitoring-and-logging)
7. [Troubleshooting](#troubleshooting)
8. [Security Considerations](#security-considerations)

---

## Prerequisites

### Required Tools
- **Docker**: Version 20.10 or higher
  - Install: https://docs.docker.com/get-docker/
- **Docker Compose**: Version 2.0 or higher
  - Install: https://docs.docker.com/compose/install/
- **AWS CLI**: Version 2.x
  - Install: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
- **Java**: JDK 11 (for local development)
  - Install: https://adoptium.net/
- **Maven**: Version 3.6 or higher (for local development)
  - Install: https://maven.apache.org/install.html

### AWS Account Requirements
- Active AWS account with appropriate permissions
- IAM user with the following permissions:
  - ECS full access
  - ECR full access
  - CloudWatch Logs full access
  - IAM role creation (for task execution role)
  - VPC and networking permissions
  - Application Load Balancer permissions

### AWS Infrastructure Prerequisites
Before deploying to ECS Fargate, ensure you have:

1. **VPC Configuration**
   - A VPC with at least 2 subnets in different availability zones
   - Internet Gateway attached to the VPC
   - Route tables configured for internet access

2. **Security Groups**
   - Security group allowing inbound traffic on port 8080 (application port)
   - Security group allowing outbound traffic to the internet
   - Security group rules for database, Redis, and other external services

3. **IAM Roles**
   - **ecsTaskExecutionRole**: Allows ECS to pull images from ECR and write logs to CloudWatch
   - **ecsTaskRole**: Allows the application to access AWS services (optional)

---

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd container-fix
```

### 2. Build the Application Locally
```bash
# Using Maven
mvn clean package -DskipTests

# The JAR file will be created in target/mini-java-app-1.0.0.jar
```

### 3. Run Locally with Docker Compose
```bash
# Build and start the application
docker-compose up --build

# Access the application
curl http://localhost:8080/mini-app

# Stop the application
docker-compose down
```

### 4. Environment Variables for Local Development
Create a `.env` file in the project root:
```env
# Database configuration
DATABASE_URL=jdbc:mysql://host.docker.internal:3306/mini_app_db
DATABASE_USERNAME=root
DATABASE_PASSWORD=your_password

# Redis configuration
CACHE_REDIS_HOST=host.docker.internal
CACHE_REDIS_PORT=6379
CACHE_REDIS_PASSWORD=your_redis_password

# Security configuration
SECURITY_JWT_SECRET=your_jwt_secret
SECURITY_ADMIN_USERNAME=admin
SECURITY_ADMIN_PASSWORD=your_admin_password

# External services
EXTERNAL_API_BASE_URL=http://api.example.com:8080/v1
EXTERNAL_API_KEY=your_api_key
```

---

## Building the Docker Image

### Using the Build Script

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
The build script will:
1. Prompt you to select a registry (AWS ECR or Docker Hub)
2. Request registry credentials and configuration
3. Build the Docker image using multi-stage build
4. Tag the image appropriately
5. Push the image to the selected registry

### Manual Docker Build
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

### Step 1: Prepare AWS Infrastructure

#### Create VPC and Subnets (if not exists)
```bash
# Create VPC
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --region us-east-1

# Create subnets
aws ec2 create-subnet --vpc-id vpc-xxxxx --cidr-block 10.0.1.0/24 --availability-zone us-east-1a
aws ec2 create-subnet --vpc-id vpc-xxxxx --cidr-block 10.0.2.0/24 --availability-zone us-east-1b
```

#### Create Security Group
```bash
# Create security group
aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for mini-java-app" \
  --vpc-id vpc-xxxxx

# Add inbound rule for application port
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Add inbound rule for ALB
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0
```

#### Create IAM Roles

**Task Execution Role** (ecsTaskExecutionRole):
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
- `CloudWatchLogsFullAccess`

**Task Role** (ecsTaskRole) - Optional:
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

### Step 2: Create AWS Secrets Manager Secrets

Store sensitive configuration in AWS Secrets Manager:

```bash
# Database credentials
aws secretsmanager create-secret \
  --name mini-java-app/database-url \
  --secret-string "jdbc:mysql://your-rds-endpoint:3306/mini_app_db" \
  --region us-east-1

aws secretsmanager create-secret \
  --name mini-java-app/database-username \
  --secret-string "your_db_username" \
  --region us-east-1

aws secretsmanager create-secret \
  --name mini-java-app/database-password \
  --secret-string "your_db_password" \
  --region us-east-1

# Redis credentials
aws secretsmanager create-secret \
  --name mini-java-app/redis-host \
  --secret-string "your-redis-endpoint" \
  --region us-east-1

aws secretsmanager create-secret \
  --name mini-java-app/redis-password \
  --secret-string "your_redis_password" \
  --region us-east-1

# JWT secret
aws secretsmanager create-secret \
  --name mini-java-app/jwt-secret \
  --secret-string "your_jwt_secret_key" \
  --region us-east-1
```

### Step 3: Deploy to ECS Fargate

#### Using the Deployment Script

**Linux/macOS:**
```bash
cd scripts
chmod +x deploy-image.sh
./deploy-image.sh
```

**Windows:**
```cmd
cd scripts
deploy-image.bat
```

#### Script Workflow
The deployment script will:
1. Prompt for AWS region
2. Retrieve AWS account ID
3. Create or verify ECS cluster
4. Prompt for network configuration (VPC, subnets, security groups)
5. Prompt for Docker image URI
6. Ask if you need a load balancer
7. Create Application Load Balancer and Target Group (if requested)
8. Create CloudWatch Log Group
9. Register ECS task definition
10. Create or update ECS service
11. Wait for service to stabilize
12. Display deployment status and access information

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

# View task details
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --region us-east-1
```

---

## Configuration Management

### ECS Task Definition Configuration

The task definition (`ecs/task-definition.json`) includes:

- **CPU and Memory**: 512 CPU units (0.5 vCPU) and 1024 MB memory
  - Valid Fargate combinations:
    - CPU: 256 → Memory: 512, 1024, 2048
    - CPU: 512 → Memory: 1024, 2048, 3072, 4096
    - CPU: 1024 → Memory: 2048-8192 (increments of 1024)

- **Environment Variables**: Non-sensitive configuration
- **Secrets**: Sensitive data from AWS Secrets Manager
- **Logging**: CloudWatch Logs with `/ecs/mini-java-app` log group

### ECS Service Configuration

The service definition (`ecs/service-definition.json`) includes:

- **Desired Count**: 2 tasks for high availability
- **Launch Type**: FARGATE
- **Network Mode**: awsvpc (required for Fargate)
- **Deployment Configuration**:
  - Maximum Percent: 200% (allows rolling updates)
  - Minimum Healthy Percent: 50%
  - Circuit Breaker: Enabled with automatic rollback

### Updating Configuration

To update environment variables or secrets:

1. Modify the task definition JSON file
2. Register a new task definition revision:
   ```bash
   aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json
   ```
3. Update the service to use the new task definition:
   ```bash
   aws ecs update-service \
     --cluster mini-java-app-cluster \
     --service mini-java-app-service \
     --task-definition mini-java-app-task:2 \
     --force-new-deployment
   ```

---

## Monitoring and Logging

### CloudWatch Logs

View application logs:
```bash
# Tail logs in real-time
aws logs tail /ecs/mini-java-app --follow --region us-east-1

# View logs for specific time range
aws logs filter-log-events \
  --log-group-name /ecs/mini-java-app \
  --start-time 1609459200000 \
  --end-time 1609545600000 \
  --region us-east-1
```

### CloudWatch Metrics

Monitor ECS service metrics:
- CPU Utilization
- Memory Utilization
- Network In/Out
- Task Count

Access metrics in AWS Console:
1. Navigate to CloudWatch → Metrics
2. Select ECS → ClusterName, ServiceName
3. View and create alarms

### Setting Up Alarms

```bash
# CPU utilization alarm
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
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Task Fails to Start

**Symptoms**: Tasks transition from PENDING to STOPPED immediately

**Possible Causes**:
- Invalid Docker image URI
- Insufficient IAM permissions
- Invalid CPU/memory combination
- Network configuration issues

**Solutions**:
```bash
# Check task stopped reason
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --query 'tasks[0].stoppedReason'

# Check CloudWatch logs for errors
aws logs tail /ecs/mini-java-app --follow

# Verify IAM role permissions
aws iam get-role --role-name ecsTaskExecutionRole
```

#### 2. Cannot Pull Docker Image

**Symptoms**: Error message "CannotPullContainerError"

**Solutions**:
- Verify ECR repository exists and image is pushed
- Check ecsTaskExecutionRole has ECR permissions
- Ensure image URI is correct in task definition

```bash
# List ECR images
aws ecr describe-images --repository-name mini-java-app

# Test ECR login
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
```

#### 3. Service Not Reaching Steady State

**Symptoms**: Service stuck in deployment, tasks continuously restarting

**Possible Causes**:
- Application crashes on startup
- Health check failures
- Insufficient resources

**Solutions**:
```bash
# Check service events
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --query 'services[0].events[0:10]'

# Check task logs
aws logs tail /ecs/mini-java-app --follow

# Increase health check grace period
# Edit service-definition.json and set healthCheckGracePeriodSeconds to 300
```

#### 4. Network Connectivity Issues

**Symptoms**: Cannot connect to database, Redis, or external services

**Solutions**:
- Verify security group rules allow outbound traffic
- Check VPC route tables have internet gateway route
- Verify DNS resolution works
- Test connectivity from within the container:

```bash
# Get task ID
TASK_ID=$(aws ecs list-tasks --cluster mini-java-app-cluster --service-name mini-java-app-service --query 'taskArns[0]' --output text)

# Execute command in running container (requires ECS Exec enabled)
aws ecs execute-command \
  --cluster mini-java-app-cluster \
  --task $TASK_ID \
  --container mini-java-app \
  --interactive \
  --command "/bin/sh"
```

#### 5. Invalid CPU/Memory Combination

**Symptoms**: Error "Invalid CPU or memory value specified"

**Solution**: Use valid Fargate CPU/memory combinations:
- CPU: 256 → Memory: 512, 1024, 2048
- CPU: 512 → Memory: 1024, 2048, 3072, 4096
- CPU: 1024 → Memory: 2048, 3072, 4096, 5120, 6144, 7168, 8192

Edit `ecs/task-definition.json` and update cpu and memory values.

---

## Security Considerations

### 1. Use AWS Secrets Manager
- Store all sensitive data (passwords, API keys, tokens) in AWS Secrets Manager
- Reference secrets in task definition using `secrets` parameter
- Never hardcode credentials in environment variables

### 2. IAM Roles and Policies
- Use least privilege principle for IAM roles
- Separate task execution role from task role
- Regularly audit IAM permissions

### 3. Network Security
- Use private subnets for tasks when possible
- Implement security groups with minimal required access
- Use VPC endpoints for AWS services to avoid internet traffic
- Enable VPC Flow Logs for network monitoring

### 4. Container Security
- Use official base images (Eclipse Temurin)
- Run containers as non-root user
- Regularly update base images and dependencies
- Scan images for vulnerabilities using ECR image scanning

### 5. Application Security
- Enable HTTPS/TLS for all external communication
- Implement proper authentication and authorization
- Use strong JWT secrets and rotate regularly
- Enable CloudWatch Logs encryption

### 6. Compliance and Auditing
- Enable AWS CloudTrail for API auditing
- Use AWS Config for compliance monitoring
- Implement log retention policies
- Regular security assessments

---

## Scaling and Performance

### Auto Scaling Configuration

Enable ECS Service Auto Scaling:

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# Create scaling policy based on CPU
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name cpu-scaling-policy \
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
  "ScaleInCooldown": 300,
  "ScaleOutCooldown": 60
}
```

### Performance Tuning

#### JVM Optimization
The Dockerfile includes optimized JVM settings:
- `-Xmx512m -Xms256m`: Heap size limits
- `-XX:+UseContainerSupport`: Container-aware JVM
- `-XX:MaxRAMPercentage=75.0`: Use 75% of container memory

#### Application Optimization
- Enable connection pooling for database
- Implement caching strategies
- Use async processing for long-running tasks
- Optimize database queries

---

## Backup and Disaster Recovery

### Database Backups
- Enable automated RDS backups
- Configure backup retention period
- Test restore procedures regularly

### Configuration Backups
- Version control all infrastructure as code
- Store task definitions in Git
- Document manual configuration steps

### Disaster Recovery Plan
1. Maintain infrastructure in multiple regions
2. Implement cross-region replication for critical data
3. Document recovery procedures
4. Conduct regular DR drills

---

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Java Container Best Practices](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runtime.html)

---

## Support and Maintenance

For issues or questions:
1. Check CloudWatch Logs for application errors
2. Review ECS service events
3. Consult this deployment guide
4. Contact the development team

**Last Updated**: 2024
**Version**: 1.0.0
