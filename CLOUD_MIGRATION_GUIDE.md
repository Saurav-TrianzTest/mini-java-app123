# Cloud Migration Guide - Mini Java Application

## Overview
This application has been transformed to be cloud-native and ready for deployment on AWS. All cloud readiness blockers have been resolved.

## Cloud Readiness Issues Fixed

### 1. Hard-coded File Paths (cr-java-0061)
**Problem:** Application used absolute file paths (`/opt/app/config/app.properties`, `/var/log/mini-app.log`)
**Solution:** Replaced with Amazon S3 object storage
- Configuration files now stored in S3 bucket
- Log files written to S3 instead of local file system
- S3 bucket and keys configurable via environment variables

### 2. Java.io.File Usage (cr-java-0063)
**Problem:** Used `java.io.File` for persistent storage
**Solution:** Migrated to AWS SDK for Java v2 S3 client
- All file operations now use S3Client
- Supports cloud-native, durable, and scalable storage
- No dependency on host-level file system

### 3. Hard-coded Database Credentials (cr-java-0069)
**Problem:** Database credentials embedded in source code
**Solution:** Migrated to AWS Secrets Manager
- Database credentials stored in AWS Secrets Manager
- Automatic credential rotation support
- No credentials in source code or container images

### 4. Hard-coded Ports (cr-java-0077)
**Problem:** Port numbers hard-coded in application
**Solution:** Externalized to AWS Parameter Store and environment variables
- Server port configurable via Parameter Store or environment variable
- Database port from Secrets Manager
- Redis port from Parameter Store
- Supports dynamic port assignment in container orchestration

### 5. Lack of Externalized Secrets (cr-java-0113)
**Problem:** API keys and secrets embedded in code
**Solution:** All secrets moved to AWS Secrets Manager
- JWT secrets in Secrets Manager
- Admin credentials in Secrets Manager
- Encryption keys in Secrets Manager
- API keys in Secrets Manager

### 6. Direct JDBC Connections (cr-java-0073)
**Problem:** Raw JDBC connections without pooling
**Solution:** Implemented HikariCP connection pooling
- HikariCP for efficient connection management
- Cloud-optimized pool settings
- Ready for Amazon RDS Proxy integration
- Proper connection lifecycle management

### 7. Missing Connection Timeouts (cr-java-0097)
**Problem:** No timeout configurations for connections
**Solution:** Added explicit timeouts
- Connection timeout: 30 seconds (configurable)
- Query timeout: 30 seconds (configurable)
- Idle timeout: 10 minutes (configurable)
- Max lifetime: 30 minutes (configurable)

### 8. Properties Files in Classpath (cr-java-0070)
**Problem:** Configuration bundled in classpath
**Solution:** Externalized to AWS Systems Manager Parameter Store
- Runtime configuration changes without redeployment
- Environment-specific configuration
- Centralized configuration management

## Architecture Changes

### New Components
1. **AwsConfigurationManager.java** - Manages AWS Secrets Manager and Parameter Store integration
2. **Updated DatabaseService.java** - Uses HikariCP and AWS services
3. **Updated MiniApp.java** - Uses S3 for file operations
4. **Updated pom.xml** - Added AWS SDK and HikariCP dependencies

### Dependencies Added
- AWS SDK for Java v2 (S3, Secrets Manager, Systems Manager)
- HikariCP 5.0.1
- Gson 2.10.1 (for JSON parsing)

## AWS Services Required

### 1. AWS Secrets Manager
Create the following secrets:

```bash
# Database credentials (JSON format)
aws secretsmanager create-secret \
  --name mini-app/database \
  --secret-string '{"username":"dbuser","password":"dbpass","host":"db.example.com","port":"3306","dbname":"mini_app_db"}'

# Redis credentials (JSON format)
aws secretsmanager create-secret \
  --name mini-app/redis \
  --secret-string '{"password":"redis_password"}'

# JWT secret
aws secretsmanager create-secret \
  --name mini-app/jwt-secret \
  --secret-string 'your-jwt-secret-key'

# Admin credentials (JSON format)
aws secretsmanager create-secret \
  --name mini-app/admin-credentials \
  --secret-string '{"username":"admin","password":"secure_password"}'

# Encryption key
aws secretsmanager create-secret \
  --name mini-app/encryption-key \
  --secret-string 'your-encryption-key'
```

### 2. AWS Systems Manager Parameter Store
Create the following parameters:

```bash
# Server configuration
aws ssm put-parameter --name /mini-app/server/port --value "8080" --type String

# Database pool configuration
aws ssm put-parameter --name /mini-app/db/pool-size --value "20" --type String
aws ssm put-parameter --name /mini-app/db/min-idle --value "5" --type String
aws ssm put-parameter --name /mini-app/db/connection-timeout --value "30000" --type String
aws ssm put-parameter --name /mini-app/db/idle-timeout --value "600000" --type String
aws ssm put-parameter --name /mini-app/db/max-lifetime --value "1800000" --type String
aws ssm put-parameter --name /mini-app/db/query-timeout --value "30" --type String

# Redis configuration
aws ssm put-parameter --name /mini-app/redis/host --value "redis.example.com" --type String
aws ssm put-parameter --name /mini-app/redis/port --value "6379" --type String

# External services
aws ssm put-parameter --name /mini-app/external-api/url --value "http://api.example.com:8080/v1" --type String
aws ssm put-parameter --name /mini-app/payment-service/url --value "https://payment.example.com/process" --type String

# S3 configuration
aws ssm put-parameter --name /mini-app/s3/bucket-name --value "mini-app-storage" --type String
aws ssm put-parameter --name /mini-app/s3/config-key --value "config/app.properties" --type String
aws ssm put-parameter --name /mini-app/s3/log-key --value "logs/mini-app.log" --type String
```

### 3. Amazon S3
Create S3 bucket for file storage:

```bash
# Create bucket
aws s3 mb s3://mini-app-storage --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket mini-app-storage \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket mini-app-storage \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
```

### 4. IAM Role Configuration
Create IAM role with the following permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:*:*:secret:mini-app/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
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
        "arn:aws:s3:::mini-app-storage",
        "arn:aws:s3:::mini-app-storage/*"
      ]
    }
  ]
}
```

## Environment Variables

Set the following environment variables in your deployment environment (ECS, EKS, Elastic Beanstalk):

```bash
# Required
AWS_REGION=us-east-1
S3_BUCKET_NAME=mini-app-storage

# Optional (have defaults)
DB_SECRET_NAME=mini-app/database
SERVER_PORT=8080
CONFIG_S3_KEY=config/app.properties
LOG_S3_KEY=logs/mini-app.log
```

## Deployment Options

### Option 1: Amazon ECS (Elastic Container Service)
- Use task IAM role with required permissions
- Set environment variables in task definition
- Use AWS Fargate for serverless container execution

### Option 2: Amazon EKS (Elastic Kubernetes Service)
- Use IRSA (IAM Roles for Service Accounts)
- Configure environment variables in deployment manifest
- Use ConfigMap for non-sensitive configuration

### Option 3: AWS Elastic Beanstalk
- Configure environment properties in Elastic Beanstalk console
- Attach IAM instance profile with required permissions
- Use .ebextensions for additional configuration

## Testing Locally

To test locally with AWS services:

1. Configure AWS credentials:
```bash
aws configure
```

2. Set environment variables:
```bash
export AWS_REGION=us-east-1
export S3_BUCKET_NAME=mini-app-storage
export DB_SECRET_NAME=mini-app/database
```

3. Build and run:
```bash
mvn clean package
java -jar target/mini-java-app-1.0.0.jar
```

## 12-Factor App Compliance

This application now follows 12-factor app principles:

1. ✅ **Codebase** - One codebase tracked in version control
2. ✅ **Dependencies** - Explicitly declared in pom.xml
3. ✅ **Config** - Stored in environment (AWS Secrets Manager, Parameter Store)
4. ✅ **Backing Services** - Attached resources (RDS, S3, ElastiCache)
5. ✅ **Build, Release, Run** - Strictly separated stages
6. ✅ **Processes** - Stateless processes
7. ✅ **Port Binding** - Self-contained with configurable port
8. ✅ **Concurrency** - Scale out via process model
9. ✅ **Disposability** - Fast startup and graceful shutdown
10. ✅ **Dev/Prod Parity** - Keep environments similar
11. ✅ **Logs** - Treat logs as event streams (S3/CloudWatch)
12. ✅ **Admin Processes** - Run as one-off processes

## Migration Checklist

- [x] Replace hard-coded file paths with S3
- [x] Replace java.io.File with S3Client
- [x] Externalize database credentials to Secrets Manager
- [x] Externalize ports to Parameter Store
- [x] Externalize all secrets to Secrets Manager
- [x] Implement HikariCP connection pooling
- [x] Add connection timeouts
- [x] Externalize configuration to Parameter Store
- [x] Add AWS SDK dependencies
- [x] Update application.properties with environment variables
- [x] Create AwsConfigurationManager utility class
- [x] Update DatabaseService with cloud-native patterns
- [x] Update MiniApp with S3 integration

## Next Steps

1. Create AWS resources (Secrets Manager, Parameter Store, S3)
2. Configure IAM roles and policies
3. Build container image (separate workflow)
4. Deploy to AWS (ECS/EKS/Elastic Beanstalk)
5. Configure monitoring and logging (CloudWatch)
6. Set up auto-scaling policies
7. Configure backup and disaster recovery

## Support

For issues or questions about the cloud migration, refer to:
- AWS Documentation: https://docs.aws.amazon.com/
- AWS SDK for Java: https://docs.aws.amazon.com/sdk-for-java/
- HikariCP: https://github.com/brettwooldridge/HikariCP
