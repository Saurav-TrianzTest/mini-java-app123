# AWS Configuration Guide for Mini Java Application

This guide explains how to configure the cloud-ready Mini Java Application in AWS.

## Overview

The application has been migrated to use AWS cloud-native services:
- **AWS Secrets Manager** - For storing sensitive credentials (database passwords, API keys, encryption keys)
- **AWS Systems Manager Parameter Store** - For storing configuration parameters (hosts, ports, URLs)
- **Amazon S3** - For file storage (configuration files, logs, uploads)
- **HikariCP with RDS Proxy** - For optimized database connection pooling
- **Asynchronous I/O** - For improved throughput and resource utilization

## Required AWS Resources

### 1. AWS Secrets Manager Secrets

Create the following secrets in AWS Secrets Manager:

#### Database Credentials
```bash
aws secretsmanager create-secret \
  --name mini-app/database/credentials \
  --description "Database credentials for Mini Java App" \
  --secret-string '{"username":"db_user","password":"secure_password_here"}'
```

#### JWT Secret
```bash
aws secretsmanager create-secret \
  --name mini-app/security/jwt-secret \
  --description "JWT signing secret" \
  --secret-string '{"secret":"your_jwt_secret_key_here"}'
```

#### Admin Credentials
```bash
aws secretsmanager create-secret \
  --name mini-app/security/admin-credentials \
  --description "Admin user credentials" \
  --secret-string '{"username":"admin","password":"secure_admin_password"}'
```

#### Encryption Key
```bash
aws secretsmanager create-secret \
  --name mini-app/security/encryption-key \
  --description "Application encryption key" \
  --secret-string '{"key":"your_encryption_key_here"}'
```

#### Monitoring Credentials
```bash
aws secretsmanager create-secret \
  --name mini-app/monitoring/credentials \
  --description "Monitoring service credentials" \
  --secret-string '{"username":"monitor_user","password":"monitor_password"}'
```

#### RabbitMQ Credentials
```bash
aws secretsmanager create-secret \
  --name mini-app/messaging/rabbitmq-credentials \
  --description "RabbitMQ credentials" \
  --secret-string '{"username":"rabbitmq_user","password":"rabbitmq_password"}'
```

### 2. AWS Systems Manager Parameter Store Parameters

Create the following parameters in Parameter Store:

```bash
# Server Configuration
aws ssm put-parameter --name /mini-app/SERVER_PORT --value "8080" --type String
aws ssm put-parameter --name /mini-app/SERVER_HOST --value "0.0.0.0" --type String

# Database Configuration
aws ssm put-parameter --name /mini-app/DB_HOST --value "your-rds-endpoint.region.rds.amazonaws.com" --type String
aws ssm put-parameter --name /mini-app/DB_PORT --value "3306" --type String
aws ssm put-parameter --name /mini-app/DB_NAME --value "mini_app_db" --type String
aws ssm put-parameter --name /mini-app/DB_POOL_MAX_SIZE --value "20" --type String
aws ssm put-parameter --name /mini-app/DB_POOL_MIN_IDLE --value "5" --type String
aws ssm put-parameter --name /mini-app/DB_QUERY_TIMEOUT --value "30" --type String

# Redis/ElastiCache Configuration
aws ssm put-parameter --name /mini-app/REDIS_HOST --value "your-elasticache-endpoint.cache.amazonaws.com" --type String
aws ssm put-parameter --name /mini-app/REDIS_PORT --value "6379" --type String

# External Service URLs
aws ssm put-parameter --name /mini-app/EXTERNAL_API_URL --value "https://api.example.com/v1" --type String
aws ssm put-parameter --name /mini-app/PAYMENT_SERVICE_URL --value "https://payment.example.com/process" --type String

# Monitoring Configuration
aws ssm put-parameter --name /mini-app/MONITORING_ENDPOINT --value "https://monitoring.example.com/metrics" --type String

# RabbitMQ Configuration
aws ssm put-parameter --name /mini-app/RABBITMQ_HOST --value "your-amazonmq-endpoint.mq.region.amazonaws.com" --type String
aws ssm put-parameter --name /mini-app/RABBITMQ_PORT --value "5672" --type String
```

### 3. Amazon S3 Buckets

Create the following S3 buckets:

```bash
# Configuration bucket
aws s3 mb s3://mini-app-config --region us-east-1

# Logs bucket
aws s3 mb s3://mini-app-logs --region us-east-1

# Uploads bucket
aws s3 mb s3://mini-app-uploads --region us-east-1
```

Upload initial configuration file:
```bash
aws s3 cp src/main/resources/application.properties s3://mini-app-config/config/app.properties
```

### 4. IAM Role and Policies

Create an IAM role for the application with the following policies:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:*:*:secret:mini-app/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters",
        "ssm:GetParametersByPath"
      ],
      "Resource": [
        "arn:aws:ssm:*:*:parameter/mini-app/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::mini-app-config/*",
        "arn:aws:s3:::mini-app-logs/*",
        "arn:aws:s3:::mini-app-uploads/*",
        "arn:aws:s3:::mini-app-config",
        "arn:aws:s3:::mini-app-logs",
        "arn:aws:s3:::mini-app-uploads"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "rds:DescribeDBInstances",
        "rds:DescribeDBClusters"
      ],
      "Resource": "*"
    }
  ]
}
```

## Environment Variables

Set the following environment variables when running the application:

```bash
# AWS Configuration
export AWS_REGION=us-east-1

# Application Configuration
export SERVER_PORT=8080
export ENVIRONMENT=production

# AWS Resource Names
export DB_SECRET_NAME=mini-app/database/credentials
export CONFIG_BUCKET=mini-app-config
export CONFIG_KEY=config/app.properties
export LOG_BUCKET=mini-app-logs
export LOG_KEY_PREFIX=logs/
export UPLOAD_BUCKET=mini-app-uploads

# Optional: Override defaults
export DEBUG_ENABLED=false
export LOGGING_LEVEL=INFO
```

## Deployment Options

### Option 1: AWS Elastic Beanstalk
```bash
eb init -p java-11 mini-java-app
eb create mini-java-app-env --instance-profile mini-app-role
eb deploy
```

### Option 2: Amazon ECS (Fargate)
1. Build Docker image (handled separately)
2. Push to Amazon ECR
3. Create ECS task definition with IAM role
4. Deploy to ECS cluster

### Option 3: Amazon EKS
1. Build Docker image (handled separately)
2. Push to Amazon ECR
3. Create Kubernetes deployment with service account
4. Apply IRSA (IAM Roles for Service Accounts)

## Database Setup

### Using Amazon RDS
1. Create RDS MySQL instance
2. Enable RDS Proxy for connection pooling
3. Store credentials in Secrets Manager
4. Update Parameter Store with RDS endpoint

### Using Amazon Aurora
1. Create Aurora MySQL cluster
2. Enable Aurora Serverless v2 (optional)
3. Configure RDS Proxy
4. Update configuration parameters

## Monitoring and Logging

### CloudWatch Integration
- Application logs are written to S3
- Configure CloudWatch Logs agent to stream S3 logs
- Set up CloudWatch alarms for application metrics

### X-Ray Integration (Optional)
Add AWS X-Ray SDK for distributed tracing:
```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-xray-recorder-sdk-core</artifactId>
    <version>2.11.0</version>
</dependency>
```

## Security Best Practices

1. **Secrets Rotation**: Enable automatic rotation for Secrets Manager secrets
2. **Encryption**: Enable encryption at rest for S3 buckets and RDS
3. **Network Security**: Use VPC, security groups, and NACLs
4. **IAM Least Privilege**: Grant only required permissions
5. **Audit Logging**: Enable CloudTrail for API audit logs

## Cost Optimization

1. Use RDS Proxy to reduce database connections
2. Enable S3 lifecycle policies for log retention
3. Use ElastiCache reserved instances for Redis
4. Configure auto-scaling for compute resources
5. Use AWS Cost Explorer to monitor spending

## Troubleshooting

### Connection Issues
- Verify security group rules allow traffic
- Check IAM role permissions
- Verify Parameter Store values are correct

### Secrets Manager Issues
- Ensure IAM role has GetSecretValue permission
- Verify secret names match configuration
- Check secret JSON format is correct

### S3 Access Issues
- Verify bucket names and regions
- Check IAM role has S3 permissions
- Ensure buckets exist and are accessible

## Migration Checklist

- [ ] Create all Secrets Manager secrets
- [ ] Create all Parameter Store parameters
- [ ] Create S3 buckets
- [ ] Create IAM role with required policies
- [ ] Set up RDS/Aurora database
- [ ] Configure RDS Proxy (optional)
- [ ] Set up ElastiCache for Redis
- [ ] Deploy application to AWS
- [ ] Verify application starts successfully
- [ ] Test database connectivity
- [ ] Test S3 file operations
- [ ] Configure monitoring and alarms
- [ ] Set up log aggregation
- [ ] Enable secrets rotation
- [ ] Document runbook procedures

## Support

For issues or questions, refer to:
- AWS Documentation: https://docs.aws.amazon.com/
- AWS Support: https://console.aws.amazon.com/support/
