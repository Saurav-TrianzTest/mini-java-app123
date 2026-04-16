# Cloud-Ready Mini Java Application

## Overview
This application has been transformed to be fully cloud-ready and compatible with AWS cloud environments. All hardcoded values, file system dependencies, and cloud-incompatible patterns have been replaced with cloud-native alternatives.

## Cloud Readiness Transformations

### 1. File Storage Migration (AWS S3)
**Blockers Fixed:** cr-java-0061, cr-java-0063

- **Before:** Hardcoded file paths (`/opt/app/config/app.properties`, `/var/log/mini-app.log`)
- **After:** AWS S3 object storage for all file operations
- **Implementation:**
  - Configuration files stored in S3 bucket
  - Logs written to S3 instead of local file system
  - Uses AWS SDK for Java v2 S3 client
  - Asynchronous file operations with CompletableFuture

### 2. Secrets Management (AWS Secrets Manager)
**Blockers Fixed:** cr-java-0069, cr-java-0113

- **Before:** Hardcoded database credentials in source code
- **After:** AWS Secrets Manager for all sensitive credentials
- **Implementation:**
  - Database credentials retrieved from Secrets Manager
  - API keys and encryption keys externalized
  - Automatic credential rotation support
  - Encrypted secret storage

### 3. Configuration Management (AWS Systems Manager Parameter Store)
**Blockers Fixed:** cr-java-0070, cr-java-0077

- **Before:** Hardcoded ports, URLs, and configuration in properties files
- **After:** AWS Systems Manager Parameter Store for runtime configuration
- **Implementation:**
  - Port numbers externalized to environment variables
  - Database host/port/name in Parameter Store
  - External service URLs in Parameter Store
  - Runtime configuration changes without redeployment

### 4. Database Connection Pooling (HikariCP)
**Blockers Fixed:** cr-java-0073, cr-java-0097

- **Before:** Direct JDBC connections without pooling
- **After:** HikariCP connection pool with proper timeouts
- **Implementation:**
  - HikariCP for efficient connection management
  - Connection timeout: 30 seconds
  - Idle timeout: 10 minutes
  - Max lifetime: 30 minutes
  - Pool size: 5-20 connections
  - Compatible with Amazon RDS Proxy

### 5. Asynchronous Operations
**Blockers Fixed:** cr-java-0099

- **Before:** Synchronous blocking I/O operations
- **After:** Asynchronous patterns with CompletableFuture
- **Implementation:**
  - Async configuration loading from S3
  - Async database query execution
  - Non-blocking I/O for better throughput
  - Compatible with AWS SDK v2 async clients

## Environment Variables

The application uses the following environment variables for cloud-native configuration:

### AWS Configuration
- `AWS_REGION` - AWS region (default: us-east-1)
- `S3_BUCKET_NAME` - S3 bucket for configuration and logs
- `CONFIG_S3_KEY` - S3 key for configuration file
- `LOG_S3_KEY_PREFIX` - S3 prefix for log files

### Server Configuration
- `SERVER_PORT` - Server port (default: 8080)
- `SERVER_HOST` - Server host (default: 0.0.0.0)
- `SERVER_CONTEXT_PATH` - Application context path

### Database Configuration
- `DB_SECRET_NAME` - Secrets Manager secret name for database credentials
- `DB_HOST_PARAM` - Parameter Store path for database host
- `DB_PORT_PARAM` - Parameter Store path for database port
- `DB_NAME_PARAM` - Parameter Store path for database name

### Cache Configuration
- `REDIS_HOST_PARAM` - Parameter Store path for Redis host
- `REDIS_PORT_PARAM` - Parameter Store path for Redis port

### Connection Pool Configuration
- `HIKARI_MAX_POOL_SIZE` - Maximum connection pool size (default: 20)
- `HIKARI_MIN_IDLE` - Minimum idle connections (default: 5)
- `HIKARI_CONNECTION_TIMEOUT` - Connection timeout in ms (default: 30000)

## AWS Services Required

### 1. AWS Secrets Manager
Store the following secrets:

**Secret Name:** `mini-app/database`
```json
{
  "username": "your-db-username",
  "password": "your-db-password"
}
```

### 2. AWS Systems Manager Parameter Store
Create the following parameters:

- `/mini-app/database/host` - Database hostname
- `/mini-app/database/port` - Database port (e.g., 3306)
- `/mini-app/database/name` - Database name
- `/mini-app/redis/host` - Redis hostname
- `/mini-app/redis/port` - Redis port (e.g., 6379)
- `/mini-app/server/port` - Server port (e.g., 8080)
- `/mini-app/environment` - Environment name (dev/staging/prod)
- `/mini-app/external-api/url` - External API URL
- `/mini-app/payment-service/url` - Payment service URL

### 3. Amazon S3
Create an S3 bucket with the following structure:
```
mini-app-config-bucket/
├── config/
│   └── app.properties
└── logs/
    └── app-*.log
```

### 4. IAM Permissions
The application requires the following IAM permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:mini-app/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:*:*:parameter/mini-app/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::mini-app-config-bucket/*"
    }
  ]
}
```

## Dependencies

The application uses the following cloud-ready dependencies:

- **HikariCP 5.0.1** - Connection pooling
- **AWS SDK for Java v2 2.20.26** - AWS service integration
  - S3 client for object storage
  - Secrets Manager client for credential management
  - SSM client for parameter store
- **Jackson 2.14.2** - JSON processing
- **MySQL Connector 8.0.33** - Database connectivity
- **Spring Boot 2.7.0** - Application framework

## Deployment Options

### AWS Elastic Container Service (ECS)
- Deploy as ECS task with task role for AWS service access
- Use ECS task definition environment variables
- Integrate with Application Load Balancer

### AWS Elastic Kubernetes Service (EKS)
- Deploy as Kubernetes deployment
- Use Kubernetes secrets for environment variables
- Use IAM roles for service accounts (IRSA)

### AWS Elastic Beanstalk
- Deploy as Java application
- Configure environment variables in Beanstalk console
- Attach IAM instance profile with required permissions

### AWS Lambda
- Package as Lambda function
- Configure environment variables in Lambda console
- Attach Lambda execution role with required permissions

## 12-Factor App Compliance

The application now follows 12-factor app principles:

1. ✅ **Codebase** - Single codebase tracked in version control
2. ✅ **Dependencies** - Explicitly declared in pom.xml
3. ✅ **Config** - Externalized to environment variables and AWS services
4. ✅ **Backing Services** - Attached resources via configuration
5. ✅ **Build, Release, Run** - Strict separation of stages
6. ✅ **Processes** - Stateless, share-nothing processes
7. ✅ **Port Binding** - Self-contained with configurable port
8. ✅ **Concurrency** - Scale out via process model
9. ✅ **Disposability** - Fast startup and graceful shutdown
10. ✅ **Dev/Prod Parity** - Keep environments similar
11. ✅ **Logs** - Treat logs as event streams (S3/CloudWatch)
12. ✅ **Admin Processes** - Run as one-off processes

## Building and Running

### Build
```bash
mvn clean package
```

### Run Locally (with AWS credentials)
```bash
export AWS_REGION=us-east-1
export S3_BUCKET_NAME=mini-app-config-bucket
export DB_SECRET_NAME=mini-app/database
export SERVER_PORT=8080

java -jar target/mini-java-app-1.0.0.jar
```

### Run in Docker
```bash
docker run -e AWS_REGION=us-east-1 \
           -e S3_BUCKET_NAME=mini-app-config-bucket \
           -e DB_SECRET_NAME=mini-app/database \
           -e SERVER_PORT=8080 \
           mini-java-app:1.0.0
```

## Migration Checklist

- [x] Replace hardcoded file paths with S3 storage
- [x] Replace hardcoded credentials with Secrets Manager
- [x] Replace hardcoded ports with environment variables
- [x] Replace direct JDBC with HikariCP connection pool
- [x] Replace classpath properties with Parameter Store
- [x] Add connection timeouts for all network operations
- [x] Implement asynchronous I/O patterns
- [x] Remove all hardcoded configuration values
- [x] Add AWS SDK dependencies
- [x] Update application.properties to use environment variables

## Security Best Practices

1. **No Hardcoded Credentials** - All credentials in Secrets Manager
2. **Encrypted Configuration** - Parameter Store with encryption
3. **IAM Roles** - Use IAM roles instead of access keys
4. **Least Privilege** - Minimal IAM permissions required
5. **Audit Logging** - CloudTrail logs all AWS API calls
6. **Secret Rotation** - Secrets Manager supports automatic rotation

## Monitoring and Observability

- **Logs** - Stored in S3 (can be integrated with CloudWatch Logs)
- **Metrics** - Can be integrated with CloudWatch Metrics
- **Tracing** - Can be integrated with AWS X-Ray
- **Health Checks** - Implement health check endpoints for load balancers

## Support

For issues or questions about the cloud-ready implementation, please refer to:
- AWS SDK for Java v2 documentation
- HikariCP documentation
- AWS Secrets Manager documentation
- AWS Systems Manager Parameter Store documentation
