# Deployment Guide: Mini Java App on AWS ECS Fargate

This guide provides comprehensive instructions for deploying the Mini Java App to AWS ECS Fargate using Docker containers.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Building and Testing Docker Image](#building-and-testing-docker-image)
4. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
5. [ECS Fargate Setup](#ecs-fargate-setup)
6. [Building and Pushing Docker Image](#building-and-pushing-docker-image)
7. [Deploying to AWS ECS Fargate](#deploying-to-aws-ecs-fargate)
8. [Post-Deployment Verification](#post-deployment-verification)
9. [Configuration Management](#configuration-management)
10. [Troubleshooting](#troubleshooting)
11. [Scaling and Management](#scaling-and-management)
12. [Security Best Practices](#security-best-practices)

---

## Prerequisites

### Required Tools

- **Docker**: Version 20.10 or later
- **Docker Compose**: Version 2.0 or later
- **AWS CLI**: Version 2.x
- **Java**: JDK 11 (for local development)
- **Maven**: Version 3.6 or later (for local builds)
- **Git**: For version control

### AWS Account Requirements

- Active AWS account with appropriate permissions
- AWS CLI configured with credentials
- IAM permissions for:
  - ECS (Elastic Container Service)
  - ECR (Elastic Container Registry)
  - VPC and networking resources
  - CloudWatch Logs
  - IAM role creation/management
  - Application Load Balancer (optional)

### System Requirements

- **Linux/macOS**: 4GB RAM minimum, 8GB recommended
- **Windows**: 8GB RAM minimum, 16GB recommended
- **Disk Space**: 10GB free space for Docker images and builds

---

## Local Development Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd mini-java-app
```

### 2. Build Locally with Maven

```bash
mvn clean package
```

### 3. Run with Docker Compose

```bash
# Build and start the application
docker-compose up --build

# Access the application
curl http://localhost:8080/health
```

### 4. Test Health Endpoints

```bash
# Main application endpoint
curl http://localhost:8080

# Health check endpoint
curl http://localhost:8081/health
```

### 5. View Logs

```bash
docker-compose logs -f mini-java-app
```

### 6. Stop the Application

```bash
docker-compose down
```

---

## Building and Testing Docker Image

### Build Docker Image

```bash
docker build -t mini-java-app:latest .
```

### Run Docker Container

```bash
docker run -d \
  -p 8080:8080 \
  -p 8081:8081 \
  -e SERVER_PORT=8080 \
  -e HEALTH_CHECK_PORT=8081 \
  --name mini-java-app \
  mini-java-app:latest
```

### Test Container

```bash
# Check container status
docker ps

# View logs
docker logs -f mini-java-app

# Test endpoints
curl http://localhost:8080
curl http://localhost:8081/health
```

### Stop and Remove Container

```bash
docker stop mini-java-app
docker rm mini-java-app
```

---

## AWS ECS Fargate Prerequisites

### 1. IAM Roles Setup

#### ECS Task Execution Role

Create a role named `ecsTaskExecutionRole` with the following policy:

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
        "logs:PutLogEvents",
        "logs:CreateLogGroup"
      ],
      "Resource": "*"
    }
  ]
}
```

**Using AWS CLI:**

```bash
# Create trust policy file
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

# Create role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://trust-policy.json

# Attach policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ECS Task Role (Optional)

Create a role named `ecsTaskRole` for application-level permissions:

```bash
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file://trust-policy.json

# Attach custom policies as needed for your application
```

### 2. VPC and Networking Setup

#### Create VPC (if needed)

```bash
VPC_ID=$(aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --query 'Vpc.VpcId' \
  --output text)

echo "VPC ID: $VPC_ID"
```

#### Create Subnets

```bash
# Public Subnet 1
SUBNET_1=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --query 'Subnet.SubnetId' \
  --output text)

# Public Subnet 2
SUBNET_2=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --query 'Subnet.SubnetId' \
  --output text)

echo "Subnet 1: $SUBNET_1"
echo "Subnet 2: $SUBNET_2"
```

#### Create Internet Gateway

```bash
IGW_ID=$(aws ec2 create-internet-gateway \
  --query 'InternetGateway.InternetGatewayId' \
  --output text)

aws ec2 attach-internet-gateway \
  --vpc-id $VPC_ID \
  --internet-gateway-id $IGW_ID
```

#### Create Route Table

```bash
RTB_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --query 'RouteTable.RouteTableId' \
  --output text)

aws ec2 create-route \
  --route-table-id $RTB_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $IGW_ID

aws ec2 associate-route-table \
  --route-table-id $RTB_ID \
  --subnet-id $SUBNET_1

aws ec2 associate-route-table \
  --route-table-id $RTB_ID \
  --subnet-id $SUBNET_2
```

#### Create Security Group

```bash
SG_ID=$(aws ec2 create-security-group \
  --group-name mini-java-app-sg \
  --description "Security group for Mini Java App" \
  --vpc-id $VPC_ID \
  --query 'GroupId' \
  --output text)

# Allow HTTP traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow health check traffic on port 8081
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 8081 \
  --cidr 0.0.0.0/0

echo "Security Group ID: $SG_ID"
```

### 3. CloudWatch Log Group

```bash
aws logs create-log-group \
  --log-group-name /ecs/mini-java-app \
  --region us-east-1
```

---

## ECS Fargate Setup

### Understanding ECS Task Definition

The task definition (`ecs/task-definition.json`) specifies:

- **Launch Type**: FARGATE (serverless)
- **Network Mode**: awsvpc (required for Fargate)
- **CPU/Memory**: Valid combinations for Fargate
  - CPU: "512" (.5 vCPU)
  - Memory: "1024" (1GB)
- **Container Configuration**:
  - Image URI
  - Port mappings (8080, 8081)
  - Environment variables
  - Logging to CloudWatch

### Valid Fargate CPU/Memory Combinations

| CPU Value | Memory Values (MB) |
|-----------|-------------------|
| 256 (.25 vCPU) | 512, 1024, 2048 |
| 512 (.5 vCPU) | 1024, 2048, 3072, 4096 |
| 1024 (1 vCPU) | 2048-8192 (increments of 1024) |
| 2048 (2 vCPU) | 4096-16384 (increments of 1024) |
| 4096 (4 vCPU) | 8192-30720 (increments of 1024) |

### Understanding ECS Service Definition

The service definition (`ecs/service-definition.json`) specifies:

- **Service Name**: mini-java-app-service
- **Desired Count**: 2 (for high availability)
- **Launch Type**: FARGATE
- **Network Configuration**:
  - Subnets (minimum 2 for HA)
  - Security Groups
  - Public IP assignment
- **Deployment Configuration**:
  - Rolling update strategy
  - Circuit breaker for automatic rollback
- **Load Balancer** (optional):
  - Target group integration
  - Health check grace period

---

## Building and Pushing Docker Image

### Option 1: Using AWS ECR

#### Linux/macOS:

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

#### Windows:

```cmd
scripts\build-push.bat
```

#### Manual Steps:

```bash
# Set variables
AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="123456789012"
ECR_REPO="mini-java-app"
IMAGE_TAG="latest"

# Authenticate with ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Create ECR repository (if not exists)
aws ecr create-repository \
  --repository-name $ECR_REPO \
  --region $AWS_REGION \
  --image-scanning-configuration scanOnPush=true

# Build image
docker build -t $ECR_REPO:$IMAGE_TAG .

# Tag image
docker tag $ECR_REPO:$IMAGE_TAG \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG

# Push image
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
```

### Option 2: Using Docker Hub

```bash
# Login to Docker Hub
docker login

# Build image
docker build -t <username>/mini-java-app:latest .

# Push image
docker push <username>/mini-java-app:latest
```

---

## Deploying to AWS ECS Fargate

### Automated Deployment

#### Linux/macOS:

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows:

```cmd
scripts\deploy-image.bat
```

### Manual Deployment

#### 1. Create ECS Cluster

```bash
aws ecs create-cluster \
  --cluster-name mini-java-app-cluster \
  --region us-east-1
```

#### 2. Register Task Definition

```bash
# Update task definition with your image URI
sed -i 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest|g' \
  ecs/task-definition.json

sed -i 's|{{AWS_REGION}}|us-east-1|g' ecs/task-definition.json
sed -i 's|{{ACCOUNT_ID}}|123456789012|g' ecs/task-definition.json

# Register task definition
aws ecs register-task-definition \
  --cli-input-json file://ecs/task-definition.json \
  --region us-east-1
```

#### 3. Create ECS Service

```bash
# Update service definition with your configuration
sed -i 's|{{CLUSTER_NAME}}|mini-java-app-cluster|g' ecs/service-definition.json
sed -i 's|{{SUBNET_1}}|subnet-12345678|g' ecs/service-definition.json
sed -i 's|{{SUBNET_2}}|subnet-87654321|g' ecs/service-definition.json
sed -i 's|{{SECURITY_GROUP}}|sg-12345678|g' ecs/service-definition.json

# If using load balancer, update target group ARN
sed -i 's|{{TARGET_GROUP_ARN}}|arn:aws:elasticloadbalancing:...|g' \
  ecs/service-definition.json

# Create service
aws ecs create-service \
  --cli-input-json file://ecs/service-definition.json \
  --region us-east-1
```

#### 4. Wait for Service Stability

```bash
aws ecs wait services-stable \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1
```

---

## Post-Deployment Verification

### 1. Check Service Status

```bash
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --region us-east-1
```

### 2. List Running Tasks

```bash
aws ecs list-tasks \
  --cluster mini-java-app-cluster \
  --service-name mini-java-app-service \
  --region us-east-1
```

### 3. View Task Details

```bash
TASK_ARN=$(aws ecs list-tasks \
  --cluster mini-java-app-cluster \
  --service-name mini-java-app-service \
  --region us-east-1 \
  --query 'taskArns[0]' \
  --output text)

aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks $TASK_ARN \
  --region us-east-1
```

### 4. View CloudWatch Logs

```bash
aws logs tail /ecs/mini-java-app \
  --follow \
  --region us-east-1
```

### 5. Test Application

```bash
# If using load balancer
LB_DNS=$(aws elbv2 describe-load-balancers \
  --region us-east-1 \
  --query 'LoadBalancers[0].DNSName' \
  --output text)

curl http://$LB_DNS
curl http://$LB_DNS/health
```

---

## Configuration Management

### Environment Variables

Update environment variables in `ecs/task-definition.json`:

```json
"environment": [
  {
    "name": "DB_HOST",
    "value": "your-database-host"
  },
  {
    "name": "DB_PORT",
    "value": "3306"
  }
]
```

### Using AWS Secrets Manager

```json
"secrets": [
  {
    "name": "DB_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:region:account-id:secret:secret-name"
  }
]
```

### Using AWS Systems Manager Parameter Store

```json
"secrets": [
  {
    "name": "API_KEY",
    "valueFrom": "arn:aws:ssm:region:account-id:parameter/parameter-name"
  }
]
```

---

## Troubleshooting

### Common Issues

#### 1. Task Fails to Start

**Symptoms**: Tasks immediately stop or fail health checks

**Solutions**:
- Check CloudWatch logs: `aws logs tail /ecs/mini-java-app --follow`
- Verify IAM roles have correct permissions
- Ensure security groups allow required ports
- Check if image URI is correct and accessible
- Verify CPU/memory combination is valid for Fargate

#### 2. Container Cannot Pull Image

**Symptoms**: "CannotPullContainerError"

**Solutions**:
- Verify ECR repository exists and image is pushed
- Check ecsTaskExecutionRole has ECR permissions
- Ensure image URI in task definition is correct
- Verify network connectivity to ECR

#### 3. Health Check Failures

**Symptoms**: Tasks repeatedly restart

**Solutions**:
- Increase `startPeriod` in health check configuration
- Verify health endpoint returns 200 status
- Check application logs for startup errors
- Ensure health check port (8081) is exposed and accessible

#### 4. Network Connectivity Issues

**Symptoms**: Cannot connect to external services

**Solutions**:
- Verify security groups allow outbound traffic
- Check NAT Gateway for private subnets
- Ensure DNS resolution is working
- Verify VPC endpoints if using private ECR

#### 5. Memory/CPU Issues

**Symptoms**: "OutOfMemory" errors or task throttling

**Solutions**:
- Increase memory allocation in task definition
- Adjust JVM heap size (JAVA_OPTS)
- Monitor CloudWatch metrics for resource utilization
- Use valid Fargate CPU/memory combinations

### Debugging Commands

```bash
# View service events
aws ecs describe-services \
  --cluster mini-java-app-cluster \
  --services mini-java-app-service \
  --query 'services[0].events' \
  --region us-east-1

# Get task stopped reason
aws ecs describe-tasks \
  --cluster mini-java-app-cluster \
  --tasks <task-id> \
  --query 'tasks[0].stoppedReason' \
  --region us-east-1

# View container logs
aws logs get-log-events \
  --log-group-name /ecs/mini-java-app \
  --log-stream-name <stream-name> \
  --region us-east-1
```

---

## Scaling and Management

### Manual Scaling

```bash
# Update desired count
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --desired-count 4 \
  --region us-east-1
```

### Auto Scaling

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

#### 2. Create Scaling Policy

```bash
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/mini-java-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration \
    'PredefinedMetricSpecification={PredefinedMetricType=ECSServiceAverageCPUUtilization},TargetValue=70.0' \
  --region us-east-1
```

### Blue/Green Deployments

```bash
# Update service with new task definition
aws ecs update-service \
  --cluster mini-java-app-cluster \
  --service mini-java-app-service \
  --task-definition mini-java-app-task:2 \
  --force-new-deployment \
  --region us-east-1
```

### Rolling Updates

Configure in service definition:

```json
"deploymentConfiguration": {
  "maximumPercent": 200,
  "minimumHealthyPercent": 50
}
```

---

## Security Best Practices

### 1. Use Non-Root User

The Dockerfile already creates and uses a non-root user (`appuser`).

### 2. Scan Images for Vulnerabilities

```bash
# Enable ECR scanning
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

### 3. Use Secrets Management

Never hardcode sensitive data in task definitions. Use:
- AWS Secrets Manager
- AWS Systems Manager Parameter Store
- Environment variables injected at runtime

### 4. Network Security

- Use private subnets for tasks
- Implement least-privilege security groups
- Enable VPC Flow Logs for monitoring
- Use AWS WAF for web application firewall

### 5. IAM Best Practices

- Use separate task execution and task roles
- Apply principle of least privilege
- Enable IAM access analyzer
- Rotate credentials regularly

### 6. Logging and Monitoring

- Enable CloudWatch Logs
- Set up CloudWatch Alarms
- Use AWS X-Ray for distributed tracing
- Implement centralized log aggregation

### 7. Compliance

- Enable AWS Config for compliance monitoring
- Use AWS CloudTrail for audit logging
- Implement tagging strategy for resource tracking
- Regular security audits and reviews

---

## Technology-Specific Notes

### Java Application Considerations

#### JVM Memory Settings

The Dockerfile sets appropriate JVM flags:

```bash
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

- `-Xmx512m`: Maximum heap size (adjust based on container memory)
- `-Xms256m`: Initial heap size
- `-XX:+UseContainerSupport`: Enable container-aware JVM
- `-XX:MaxRAMPercentage=75.0`: Use 75% of container memory for heap

#### Startup Time

Java applications may take longer to start. Adjust health check settings:

```json
"healthCheck": {
  "startPeriod": 60
}
```

#### Graceful Shutdown

Ensure proper signal handling for graceful shutdown:

```bash
# Add shutdown hook in Java application
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // Cleanup code
}));
```

#### Spring Boot Integration

If using Spring Boot:
- Use Spring Boot Actuator for health checks
- Configure management port separately
- Implement readiness and liveness probes
- Use Spring Cloud AWS for native AWS integration

---

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Java on Docker Best Practices](https://docs.docker.com/language/java/)
- [AWS CLI Reference](https://docs.aws.amazon.com/cli/latest/reference/ecs/)

---

## Support and Feedback

For issues, questions, or feedback:
- Create an issue in the project repository
- Contact the DevOps team
- Refer to internal documentation and runbooks

---

**Document Version**: 1.0
**Last Updated**: 2025-11-26
**Maintained By**: DevOps Team
