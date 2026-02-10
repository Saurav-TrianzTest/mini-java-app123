# AWS Deployment Guide for Mini Java Application

## Overview
This guide provides step-by-step instructions for deploying the cloud-ready Mini Java application to AWS.

## Prerequisites
- AWS CLI configured with appropriate credentials
- Docker installed locally
- AWS account with necessary permissions

## Architecture Components

### AWS Services Used
1. **Amazon ECS/EKS** - Container orchestration
2. **Amazon RDS** - MySQL database
3. **Amazon ElastiCache** - Redis cache
4. **AWS Secrets Manager** - Secure credential storage
5. **Amazon CloudWatch** - Logging and monitoring
6. **Application Load Balancer** - Traffic distribution
7. **Amazon ECR** - Container registry

## Deployment Steps

### 1. Create RDS MySQL Instance

```bash
aws rds create-db-instance \
  --db-instance-identifier mini-app-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0 \
  --master-username admin \
  --master-user-password YOUR_PASSWORD \
  --allocated-storage 20 \
  --vpc-security-group-ids sg-xxxxx \
  --db-subnet-group-name your-subnet-group
```

### 2. Create ElastiCache Redis Cluster

```bash
aws elasticache create-cache-cluster \
  --cache-cluster-id mini-app-redis \
  --cache-node-type cache.t3.micro \
  --engine redis \
  --num-cache-nodes 1 \
  --security-group-ids sg-xxxxx
```

### 3. Store Secrets in AWS Secrets Manager

```bash
# Database credentials
aws secretsmanager create-secret \
  --name mini-app/db-credentials \
  --secret-string '{"username":"admin","password":"YOUR_DB_PASSWORD"}'

# JWT secret
aws secretsmanager create-secret \
  --name mini-app/jwt-secret \
  --secret-string '{"secret":"YOUR_JWT_SECRET"}'
```

### 4. Build and Push Docker Image to ECR

```bash
# Create ECR repository
aws ecr create-repository --repository-name mini-java-app

# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Build and tag image
docker build -t mini-java-app .
docker tag mini-java-app:latest YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest

# Push to ECR
docker push YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

### 5. Create ECS Task Definition

Create a file named `task-definition.json`:

```json
{
  "family": "mini-java-app",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "mini-java-app",
      "image": "YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {"name": "SERVER_PORT", "value": "8080"},
        {"name": "ENVIRONMENT", "value": "production"},
        {"name": "LOGGING_LEVEL", "value": "INFO"}
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:mini-app/db-credentials:password::"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:mini-app/jwt-secret:secret::"
        }
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
  ]
}
```

Register the task definition:

```bash
aws ecs register-task-definition --cli-input-json file://task-definition.json
```

### 6. Create ECS Service

```bash
aws ecs create-service \
  --cluster your-cluster-name \
  --service-name mini-java-app-service \
  --task-definition mini-java-app \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxx],securityGroups=[sg-xxxxx],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:region:account:targetgroup/mini-app-tg/xxxxx,containerName=mini-java-app,containerPort=8080"
```

### 7. Configure Application Load Balancer

```bash
# Create target group
aws elbv2 create-target-group \
  --name mini-app-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id vpc-xxxxx \
  --target-type ip \
  --health-check-path /health

# Create load balancer
aws elbv2 create-load-balancer \
  --name mini-app-alb \
  --subnets subnet-xxxxx subnet-yyyyy \
  --security-groups sg-xxxxx
```

## Environment Variables Configuration

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| DB_HOST | RDS endpoint | `mini-app-db.xxxxx.us-east-1.rds.amazonaws.com` |
| DB_PORT | Database port | `3306` |
| DB_NAME | Database name | `mini_app_db` |
| DB_USERNAME | Database user | `admin` |
| DB_PASSWORD | Database password | (from Secrets Manager) |
| REDIS_HOST | ElastiCache endpoint | `mini-app-redis.xxxxx.cache.amazonaws.com` |
| REDIS_PORT | Redis port | `6379` |
| JWT_SECRET | JWT signing key | (from Secrets Manager) |
| SERVER_PORT | Application port | `8080` |
| ENVIRONMENT | Deployment environment | `production` |
| LOGGING_LEVEL | Log level | `INFO` |
| AWS_REGION | AWS region | `us-east-1` |

## Monitoring and Logging

### CloudWatch Logs
All application logs are sent to CloudWatch Logs in structured JSON format:

```bash
# View logs
aws logs tail /ecs/mini-java-app --follow
```

### CloudWatch Metrics
Monitor application metrics:
- CPU utilization
- Memory utilization
- Request count
- Database connections

## Security Best Practices

1. **Use Secrets Manager** - Never hardcode credentials
2. **Enable VPC Flow Logs** - Monitor network traffic
3. **Use Security Groups** - Restrict access to necessary ports
4. **Enable CloudTrail** - Audit API calls
5. **Rotate Credentials** - Regular password rotation
6. **Use IAM Roles** - Task execution role for ECS
7. **Enable Encryption** - RDS encryption at rest

## Scaling Configuration

### Auto Scaling Policy

```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/your-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/your-cluster/mini-java-app-service \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration file://scaling-policy.json
```

## Troubleshooting

### Common Issues

1. **Database Connection Timeout**
   - Check security group rules
   - Verify RDS endpoint
   - Check DB credentials in Secrets Manager

2. **Application Not Starting**
   - Check CloudWatch logs
   - Verify environment variables
   - Check container health

3. **High Memory Usage**
   - Adjust JVM heap settings
   - Increase task memory
   - Check for memory leaks

## Cost Optimization

1. Use Fargate Spot for non-critical workloads
2. Right-size RDS instances
3. Enable RDS storage autoscaling
4. Use Reserved Instances for predictable workloads
5. Implement caching with ElastiCache

## Rollback Procedure

```bash
# Rollback to previous task definition
aws ecs update-service \
  --cluster your-cluster-name \
  --service mini-java-app-service \
  --task-definition mini-java-app:PREVIOUS_REVISION
```

## Support

For issues or questions, contact the DevOps team or create a ticket in the support system.
