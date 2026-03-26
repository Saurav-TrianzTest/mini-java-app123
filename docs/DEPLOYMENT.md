# Deployment Guide for mini-java-app

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Project Overview](#project-overview)
3. [Local Development Setup](#local-development-setup)
4. [Docker Deployment](#docker-deployment)
5. [AWS ECS Fargate Deployment](#aws-ecs-fargate-deployment)
6. [Configuration Management](#configuration-management)
7. [Troubleshooting](#troubleshooting)
8. [Security Considerations](#security-considerations)
9. [Monitoring and Logging](#monitoring-and-logging)

---

## Prerequisites

### Required Tools
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **AWS CLI**: Version 2.x
- **Java**: JDK 11 (for local development)
- **Maven**: Version 3.6+ (for local builds)

### AWS Requirements
- AWS Account with appropriate permissions
- IAM roles configured:
  - `ecsTaskExecutionRole` - For ECS to pull images and write logs
  - `ecsTaskRole` - For application to access AWS services
- VPC with at least 2 subnets in different availability zones
- Security groups configured to allow:
  - Inbound: Port 8080 (application)
  - Outbound: All traffic (for external dependencies)

### AWS CLI Configuration
```bash
# Configure AWS CLI
aws configure

# Verify configuration
aws sts get-caller-identity
```

---

## Project Overview

### Technology Stack
- **Language**: Java 11
- **Build Tool**: Maven 3.9.4
- **Framework**: Spring Boot 2.7.0
- **Database**: MySQL 8.0.33
- **Container Runtime**: Docker
- **Deployment Platform**: AWS ECS Fargate

### Application Details
- **Application Port**: 8080
- **Package Type**: JAR
- **Base Image**: Eclipse Temurin 11 JDK
- **Runtime Image**: Eclipse Temurin 11 JDK

### Project Structure
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

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd mini-java-app
```

### 2. Build the Application Locally
```bash
# Using Maven
mvn clean package -DskipTests

# Run the application
java -jar target/mini-java-app-1.0.0.jar
```

### 3. Configure Environment Variables
Create a `.env` file in the project root:
```properties
# Application Configuration
SERVER_PORT=8080
APP_BASE_PATH=/app

# Database Configuration
DATABASE_URL=jdbc:mysql://localhost:3306/mini_app_db
DATABASE_USERNAME=root
DATABASE_PASSWORD=password123

# Cache Configuration
CACHE_REDIS_HOST=localhost
CACHE_REDIS_PORT=6379
CACHE_REDIS_PASSWORD=redis_secret

# External Services
EXTERNAL_API_BASE_URL=http://api.example.com:8080/v1
PAYMENT_SERVICE_URL=https://payment.internal.company.com/process

# Security
SECURITY_JWT_SECRET=your_jwt_secret
SECURITY_ADMIN_USERNAME=admin
SECURITY_ADMIN_PASSWORD=admin_password

# Monitoring
MONITORING_ENDPOINT=http://monitoring.internal.company.com:9090/metrics

# Messaging
MESSAGING_RABBITMQ_HOST=localhost
MESSAGING_RABBITMQ_PORT=5672
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

### 3. Test the Application
```bash
# Check if the application is running
curl http://localhost:8080/health

# View application logs
docker logs mini-java-app
```

### 4. Docker Compose Configuration
The `docker-compose.yml` file includes:
- Application service only (no infrastructure services)
- Environment variables for external service connections
- Volume mounts for logs and configuration
- Health check configuration
- Network configuration

**Note**: Database, Redis, RabbitMQ, and other infrastructure services should be provided separately. The application connects to them via environment variables.

---

## AWS ECS Fargate Deployment

### Overview
AWS ECS Fargate is a serverless compute engine for containers that eliminates the need to manage EC2 instances. This deployment uses:
- **Launch Type**: FARGATE
- **Network Mode**: awsvpc (required for Fargate)
- **CPU**: 512 (.5 vCPU)
- **Memory**: 1024 MB (1 GB)

### Step 1: Prerequisites Setup

#### 1.1 Create IAM Roles

**ECS Task Execution Role** (required):
```bash
# Create trust policy
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

# Create role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach AWS managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

**ECS Task Role** (optional, for application AWS access):
```bash
# Create task role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach policies as needed (e.g., S3, DynamoDB access)
```

#### 1.2 Create VPC and Networking (if not exists)
```bash
# Create VPC
VPC_ID=$(aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --query 'Vpc.VpcId' \
  --output text)

# Create subnets in different AZs
SUBNET_1=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --query 'Subnet.SubnetId' \
  --output text)

SUBNET_2=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --query 'Subnet.SubnetId' \
  --output text)

# Create Internet Gateway
IGW_ID=$(aws ec2 create-internet-gateway \
  --query 'InternetGateway.InternetGatewayId' \
  --output text)

aws ec2 attach-internet-gateway \
  --vpc-id $VPC_ID \
  --internet-gateway-id $IGW_ID

# Create route table and associate with subnets
ROUTE_TABLE_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --query 'RouteTable.RouteTableId' \
  --output text)

aws ec2 create-route \
  --route-table-id $ROUTE_TABLE_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $IGW_ID

aws ec2 associate-route-table \
  --subnet-id $SUBNET_1 \
  --route-table-id $ROUTE_TABLE_ID

aws ec2 associate-route-table \
  --subnet-id $SUBNET_2 \
  --route-table-id $ROUTE_TABLE_ID
```

#### 1.3 Create Security Group
```bash
# Create security group
SECURITY_GROUP_ID=$(aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for mini-java-app" \
  --vpc-id $VPC_ID \
  --query 'GroupId' \
  --output text)

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

#### 1.4 Create CloudWatch Log Group
```bash
aws logs create-log-group \
  --log-group-name /ecs/mini-java-app \
  --region us-east-1
```

### Step 2: Build and Push Docker Image

#### Option 1: Using AWS ECR
```bash
# Run the build-push script
cd scripts
chmod +x build-push.sh
./build-push.sh

# Follow the prompts:
# 1. Select "1" for AWS ECR
# 2. Enter AWS region (e.g., us-east-1)
# 3. Enter AWS Account ID
# 4. Enter image tag (e.g., latest)
```

#### Option 2: Using Docker Hub
```bash
# Run the build-push script
cd scripts
chmod +x build-push.sh
./build-push.sh

# Follow the prompts:
# 1. Select "2" for Docker Hub
# 2. Enter Docker Hub username
# 3. Enter Docker Hub password
# 4. Enter image tag (e.g., latest)
```

#### Windows Users
```cmd
cd scripts
build-push.bat
```

### Step 3: Deploy to ECS Fargate

#### Automated Deployment
```bash
# Run the deployment script
cd scripts
chmod +x deploy-image.sh
./deploy-image.sh

# Follow the prompts:
# 1. Enter AWS region
# 2. Enter ECS cluster name
# 3. Enter VPC ID
# 4. Enter subnet IDs (comma-separated)
# 5. Enter security group ID
# 6. Enter Docker image URI
# 7. Choose whether to create a load balancer (y/n)
```

#### Windows Users
```cmd
cd scripts
deploy-image.bat
```

#### Manual Deployment Steps

**1. Register Task Definition**
```bash
# Update placeholders in task-definition.json
sed -i 's|{{IMAGE_URI}}|123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest|g' ecs/task-definition.json
sed -i 's|{{AWS_REGION}}|us-east-1|g' ecs/task-definition.json
sed -i 's|{{ACCOUNT_ID}}|123456789|g' ecs/task-definition.json

# Register task definition
aws ecs register-task-definition \
  --cli-input-json file://ecs/task-definition.json \
  --region us-east-1
```

**2. Create ECS Cluster**
```bash
aws ecs create-cluster \
  --cluster-name mini-java-app-cluster \
  --region us-east-1
```

**3. Create ECS Service**
```bash
# Update placeholders in service-definition.json
sed -i 's|{{CLUSTER_NAME}}|mini-java-app-cluster|g' ecs/service-definition.json
sed -i 's|{{SUBNET_1}}|subnet-xxxxx|g' ecs/service-definition.json
sed -i 's|{{SUBNET_2}}|subnet-yyyyy|g' ecs/service-definition.json
sed -i 's|{{SECURITY_GROUP}}|sg-zzzzz|g' ecs/service-definition.json

# Create service
aws ecs create-service \
  --cli-input-json file://ecs/service-definition.json \
  --region us-east-1
```

**4. Wait for Service to Stabilize**
```bash
aws ecs wait services-stable \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1
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

# View task details
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --region us-east-1
```

### Step 5: Access the Application

#### With Load Balancer
```bash
# Get load balancer DNS name
aws elbv2 describe-load-balancers \
  --names mini-java-app-alb \
  --query 'LoadBalancers[0].DNSName' \
  --output text

# Access application
curl http://<alb-dns-name>/health
```

#### Without Load Balancer (Direct Task Access)
```bash
# Get task public IP
TASK_ARN=$(aws ecs list-tasks \
  --cluster mini-java-app-cluster \
  --service-name mini-java-app-service \
  --query 'taskArns[0]' \
  --output text)

PUBLIC_IP=$(aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks $TASK_ARN \
  --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' \
  --output text | xargs -I {} aws ec2 describe-network-interfaces \
  --network-interface-ids {} \
  --query 'NetworkInterfaces[0].Association.PublicIp' \
  --output text)

# Access application
curl http://$PUBLIC_IP:8080/health
```

---

## Configuration Management

### Environment Variables
All configuration is externalized via environment variables. Update the task definition to modify:

```json
{
  "environment": [
    {
      "name": "DATABASE_URL",
      "value": "jdbc:mysql://your-rds-endpoint:3306/mini_app_db"
    },
    {
      "name": "DATABASE_USERNAME",
      "value": "admin"
    }
  ]
}
```

### Using AWS Secrets Manager (Recommended for Production)
```bash
# Create secret
aws secretsmanager create-secret \
  --name mini-java-app/database \
  --secret-string '{"username":"admin","password":"secure_password"}' \
  --region us-east-1

# Update task definition to use secrets
{
  "secrets": [
    {
      "name": "DATABASE_USERNAME",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:mini-java-app/database:username::"
    },
    {
      "name": "DATABASE_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:mini-java-app/database:password::"
    }
  ]
}
```

### Using AWS Systems Manager Parameter Store
```bash
# Create parameters
aws ssm put-parameter \
  --name /mini-java-app/database/url \
  --value "jdbc:mysql://your-rds-endpoint:3306/mini_app_db" \
  --type String \
  --region us-east-1

# Update task definition
{
  "secrets": [
    {
      "name": "DATABASE_URL",
      "valueFrom": "arn:aws:ssm:us-east-1:123456789:parameter/mini-java-app/database/url"
    }
  ]
}
```

---

## Troubleshooting

### Common Issues

#### 1. Task Fails to Start
**Symptoms**: Tasks transition from PENDING to STOPPED immediately

**Possible Causes**:
- Invalid CPU/memory combination
- Image pull errors
- Missing IAM permissions
- Network configuration issues

**Solutions**:
```bash
# Check stopped task reason
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --query 'tasks[0].stoppedReason' \
  --output text

# Check CloudWatch logs
aws logs tail /ecs/mini-java-app --follow --region us-east-1

# Verify IAM role permissions
aws iam get-role --role-name ecsTaskExecutionRole
```

#### 2. Cannot Pull Image from ECR
**Symptoms**: "CannotPullContainerError"

**Solutions**:
```bash
# Verify ECR repository exists
aws ecr describe-repositories --repository-names mini-java-app

# Check IAM permissions for ecsTaskExecutionRole
aws iam list-attached-role-policies --role-name ecsTaskExecutionRole

# Manually test image pull
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker pull <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

#### 3. Invalid CPU/Memory Combination
**Symptoms**: "ClientException: Invalid CPU or memory value specified"

**Valid Fargate CPU/Memory Combinations**:
- CPU: 256 (.25 vCPU) → Memory: 512, 1024, 2048 MB
- CPU: 512 (.5 vCPU) → Memory: 1024, 2048, 3072, 4096 MB
- CPU: 1024 (1 vCPU) → Memory: 2048-8192 MB (increments of 1024)
- CPU: 2048 (2 vCPU) → Memory: 4096-16384 MB (increments of 1024)
- CPU: 4096 (4 vCPU) → Memory: 8192-30720 MB (increments of 1024)

**Solution**: Update task definition with valid combination

#### 4. Network Issues
**Symptoms**: Tasks start but cannot be reached

**Solutions**:
```bash
# Verify security group rules
aws ec2 describe-security-groups --group-ids <security-group-id>

# Check subnet route tables
aws ec2 describe-route-tables --filters "Name=association.subnet-id,Values=<subnet-id>"

# Verify internet gateway attachment
aws ec2 describe-internet-gateways --filters "Name=attachment.vpc-id,Values=<vpc-id>"

# Enable auto-assign public IP on subnets
aws ec2 modify-subnet-attribute \
  --subnet-id <subnet-id> \
  --map-public-ip-on-launch
```

#### 5. Service Not Reaching Steady State
**Symptoms**: Service stuck in deployment

**Solutions**:
```bash
# Check service events
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --query 'services[0].events[0:10]'

# Check task health
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --query 'tasks[0].healthStatus'

# Force new deployment
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --force-new-deployment
```

#### 6. Application Crashes or OOM Errors
**Symptoms**: Tasks stop with exit code 137 or 143

**Solutions**:
```bash
# Check CloudWatch logs for OOM errors
aws logs filter-log-events \
  --log-group-name /ecs/mini-java-app \
  --filter-pattern "OutOfMemoryError"

# Increase memory allocation in task definition
# Update JAVA_OPTS in task definition:
"JAVA_OPTS": "-Xmx768m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Or increase task memory
"memory": "2048"
```

### Debugging Commands

```bash
# View all logs
aws logs tail /ecs/mini-java-app --follow --region us-east-1

# Filter logs by time
aws logs tail /ecs/mini-java-app --since 1h --region us-east-1

# Get task execution logs
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-arn> \
  --include TAGS

# Check service metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 3600 \
  --statistics Average
```

---

## Security Considerations

### 1. Container Security
- ✅ Running as non-root user (appuser)
- ✅ Using official Eclipse Temurin base images
- ✅ Multi-stage build to minimize image size
- ✅ No unnecessary packages installed

### 2. Network Security
- Use private subnets for tasks (recommended for production)
- Restrict security group rules to minimum required
- Use VPC endpoints for AWS services (avoid internet traffic)
- Enable VPC Flow Logs for network monitoring

### 3. Secrets Management
- ✅ Never hardcode secrets in task definitions
- ✅ Use AWS Secrets Manager or Parameter Store
- ✅ Rotate secrets regularly
- ✅ Use IAM roles for AWS service access

### 4. IAM Best Practices
- Follow principle of least privilege
- Use separate task execution and task roles
- Enable CloudTrail for audit logging
- Regularly review and rotate credentials

### 5. Image Security
```bash
# Scan images for vulnerabilities
aws ecr start-image-scan \
  --repository-name mini-java-app \
  --image-id imageTag=latest

# View scan results
aws ecr describe-image-scan-findings \
  --repository-name mini-java-app \
  --image-id imageTag=latest
```

---

## Monitoring and Logging

### CloudWatch Logs
```bash
# View logs in real-time
aws logs tail /ecs/mini-java-app --follow --region us-east-1

# Create log insights query
aws logs start-query \
  --log-group-name /ecs/mini-java-app \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --query-string 'fields @timestamp, @message | filter @message like /ERROR/ | sort @timestamp desc | limit 20'
```

### CloudWatch Metrics
Key metrics to monitor:
- **CPUUtilization**: Task CPU usage
- **MemoryUtilization**: Task memory usage
- **TargetResponseTime**: Application response time (with ALB)
- **HealthyHostCount**: Number of healthy tasks (with ALB)

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
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster

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
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-java-app-cluster
```

### Application Performance Monitoring (APM)
Consider integrating:
- AWS X-Ray for distributed tracing
- CloudWatch Application Insights
- Third-party APM tools (New Relic, Datadog, Dynatrace)

---

## ECS Fargate Scaling and Management

### Auto Scaling Configuration
```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# Create scaling policy (target tracking)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
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
  "ScaleInCooldown": 300,
  "ScaleOutCooldown": 60
}
```

### Blue/Green Deployments
```bash
# Update service with deployment controller
aws ecs create-service \
  --cluster mini-java-app-cluster \
  --service-name mini-java-app-service \
  --task-definition mini-java-app-task \
  --desired-count 2 \
  --launch-type FARGATE \
  --deployment-controller type=CODE_DEPLOY \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-zzz],assignPublicIp=ENABLED}"
```

### Rolling Updates
```bash
# Update service with new task definition
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:2 \
  --force-new-deployment
```

### Service Discovery
```bash
# Create Cloud Map namespace
aws servicediscovery create-private-dns-namespace \
  --name mini-java-app.local \
  --vpc <vpc-id>

# Create service discovery service
aws servicediscovery create-service \
  --name mini-java-app \
  --dns-config "NamespaceId=<namespace-id>,DnsRecords=[{Type=A,TTL=60}]" \
  --health-check-custom-config FailureThreshold=1

# Update ECS service with service registry
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --service-registries "registryArn=<service-registry-arn>"
```

---

## Cost Optimization

### Fargate Pricing
- Charged based on vCPU and memory resources
- Billed per second with 1-minute minimum
- No upfront costs or long-term commitments

### Cost Optimization Tips
1. **Right-size resources**: Start with minimal CPU/memory and scale up as needed
2. **Use Fargate Spot**: Save up to 70% for fault-tolerant workloads
3. **Enable auto-scaling**: Scale down during low-traffic periods
4. **Use CloudWatch Logs retention**: Set appropriate retention periods
5. **Optimize image size**: Smaller images = faster pulls = lower costs

### Fargate Spot
```bash
# Update service to use Fargate Spot
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --capacity-provider-strategy \
    capacityProvider=FARGATE_SPOT,weight=1,base=0 \
    capacityProvider=FARGATE,weight=0,base=2
```

---

## Additional Resources

### AWS Documentation
- [ECS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [ECS Task Definitions](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html)
- [ECS Service Definition](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service_definition_parameters.html)
- [Fargate Task Networking](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/fargate-task-networking.html)

### Best Practices
- [ECS Best Practices Guide](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Java Container Best Practices](https://docs.oracle.com/en/java/javase/11/docs/specs/man/java.html)

### Support
- AWS Support: https://console.aws.amazon.com/support/
- ECS Forum: https://forums.aws.amazon.com/forum.jspa?forumID=187
- GitHub Issues: <repository-url>/issues

---

## Appendix

### A. Complete Deployment Checklist
- [ ] AWS CLI configured
- [ ] IAM roles created (ecsTaskExecutionRole, ecsTaskRole)
- [ ] VPC and subnets configured
- [ ] Security groups configured
- [ ] CloudWatch log group created
- [ ] Docker image built and pushed
- [ ] Task definition registered
- [ ] ECS cluster created
- [ ] ECS service created
- [ ] Application accessible
- [ ] Monitoring and alarms configured
- [ ] Auto-scaling configured (optional)
- [ ] Load balancer configured (optional)

### B. Environment Variables Reference
See the task definition file for complete list of environment variables.

### C. Port Reference
- **8080**: Application HTTP port
- **80**: Load balancer HTTP port (if using ALB)
- **443**: Load balancer HTTPS port (if using ALB with SSL)

### D. Useful Commands Cheat Sheet
```bash
# Build and push
./scripts/build-push.sh

# Deploy to ECS
./scripts/deploy-image.sh

# View logs
aws logs tail /ecs/mini-java-app --follow

# List tasks
aws ecs list-tasks --cluster mini-java-app-cluster

# Describe service
aws ecs describe-services --cluster mini-java-app-cluster --services mini-java-app-service

# Update service
aws ecs update-service --cluster mini-java-app-cluster --service mini-java-app-service --force-new-deployment

# Scale service
aws ecs update-service --cluster mini-java-app-cluster --service mini-java-app-service --desired-count 4

# Delete service
aws ecs delete-service --cluster mini-java-app-cluster --service mini-java-app-service --force

# Delete cluster
aws ecs delete-cluster --cluster mini-java-app-cluster
```

---

**Document Version**: 1.0  
**Last Updated**: 2024-01-01  
**Maintained By**: DevOps Team
