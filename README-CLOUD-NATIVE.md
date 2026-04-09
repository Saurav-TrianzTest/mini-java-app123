# Cloud-Native Mini Java Application

## Overview
This application has been transformed to be fully cloud-ready and compatible with AWS cloud environments. All hardcoded values, file system dependencies, and direct database connections have been replaced with cloud-native patterns.

## Cloud Readiness Fixes Applied

### 1. Configuration Management (cr-java-0069, cr-java-0070)
- **Replaced**: Hardcoded database credentials
- **With**: AWS Secrets Manager integration
- **Secret Names**:
  - `mini-app/database` - Database credentials (username, password, host, port, dbname)
  - `mini-app/redis` - Redis cache credentials
  - `mini-app/external-api` - External API keys
  - `mini-app/payment-service` - Payment service credentials
  - `mini-app/jwt-secret` - JWT signing secret
  - `mini-app/admin-credentials` - Admin credentials
  - `mini-app/encryption-key` - Encryption key
  - `mini-app/monitoring` - Monitoring credentials
  - `mini-app/rabbitmq` - RabbitMQ credentials

### 2. File System Dependencies (cr-java-0061, cr-java-0063)
- **Replaced**: Hardcoded file paths (`/opt/app/config`, `/var/log`)
- **With**: Amazon S3 object storage
- **Implementation**: `S3StorageService` class
- **Configuration**:
  - `CONFIG_S3_BUCKET` - S3 bucket for configuration files
  - `CONFIG_S3_KEY` - S3 object key for configuration (default: `config/app.properties`)

### 3. Network & Communication (cr-java-0077)
- **Replaced**: Hardcoded ports (8080, 3306, 6379)
- **With**: Environment variables and AWS Parameter Store
- **Environment Variables**:
  - `SERVER_PORT` - Application server port (default: 8080)
  - `DB_PORT` - Database port (default: 3306)
  - `REDIS_PORT` - Redis cache port (default: 6379)

### 4. Database & Persistence (cr-java-0073)
- **Replaced**: Direct JDBC connections
- **With**: HikariCP connection pooling
- **Features**:
  - Connection pool with configurable size
  - Connection timeout and validation
  - Leak detection
  - Automatic connection recycling
- **Configuration**:
  - `DB_POOL_MAX_SIZE` - Maximum pool size (default: 20)
  - `DB_POOL_MIN_IDLE` - Minimum idle connections (default: 5)
  - `DB_CONNECTION_TIMEOUT_MS` - Connection timeout (default: 30000)

### 5. Startup & Initialization (cr-java-0105)
- **Replaced**: Static initializers with I/O operations
- **With**: Lazy initialization patterns
- **Implementation**: Moved I/O operations from static blocks to instance methods

### 6. Security & Authentication (cr-java-0113)
- **Replaced**: Hardcoded secrets in source code
- **With**: AWS Secrets Manager integration
- **Implementation**: `CloudConfigurationManager` class

### 7. Resource Management (cr-java-0097)
- **Added**: Connection timeouts for all network operations
- **Implementation**: 
  - AWS SDK clients configured with 10s API timeout
  - Database connections with 30s timeout
  - Proper resource cleanup in finally blocks

### 8. Logging & Monitoring
- **Replaced**: File-based logging
- **With**: Structured JSON logging to stdout
- **Implementation**: Logback configuration with JSON format
- **Features**:
  - Async logging for performance
  - Correlation IDs support
  - Cloud monitoring integration ready

## Environment Variables

### Required for AWS Deployment
```bash
# AWS Configuration
AWS_REGION=us-east-1

# Database Secret
DB_SECRET_NAME=mini-app/database

# S3 Configuration
CONFIG_S3_BUCKET=mini-app-config-bucket
CONFIG_S3_KEY=config/app.properties

# Server Configuration
SERVER_PORT=8080
```

### Optional (with defaults)
```bash
# Database Pool Configuration
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=5
DB_CONNECTION_TIMEOUT_MS=30000

# External Services
EXTERNAL_API_URL=http://api.example.com/v1
PAYMENT_SERVICE_URL=https://payment.internal.company.com/process
REDIS_HOST=localhost
REDIS_PORT=6379

# Logging
LOG_LEVEL=INFO
```

## AWS Secrets Manager Setup

### Database Secret Format
```json
{
  "username": "db_user",
  "password": "db_password",
  "host": "database.region.rds.amazonaws.com",
  "port": 3306,
  "dbname": "mini_app_db"
}
```

### Create Secrets using AWS CLI
```bash
# Database credentials
aws secretsmanager create-secret \
  --name mini-app/database \
  --secret-string '{"username":"db_user","password":"db_password","host":"database.region.rds.amazonaws.com","port":3306,"dbname":"mini_app_db"}'

# Redis credentials
aws secretsmanager create-secret \
  --name mini-app/redis \
  --secret-string '{"password":"redis_password"}'

# External API key
aws secretsmanager create-secret \
  --name mini-app/external-api \
  --secret-string '{"apiKey":"your_api_key"}'
```

## AWS Parameter Store Setup

### Create Parameters using AWS CLI
```bash
# Server port
aws ssm put-parameter \
  --name /mini-app/SERVER_PORT \
  --value "8080" \
  --type String

# Database pool size
aws ssm put-parameter \
  --name /mini-app/DB_POOL_MAX_SIZE \
  --value "20" \
  --type String

# Log level
aws ssm put-parameter \
  --name /mini-app/LOG_LEVEL \
  --value "INFO" \
  --type String
```

## S3 Bucket Setup

### Create S3 Bucket
```bash
# Create bucket
aws s3 mb s3://mini-app-config-bucket --region us-east-1

# Upload configuration file
aws s3 cp src/main/resources/application.properties \
  s3://mini-app-config-bucket/config/app.properties
```

## IAM Permissions Required

### IAM Policy for Application
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
        "arn:aws:s3:::mini-app-config-bucket",
        "arn:aws:s3:::mini-app-config-bucket/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "rds:DescribeDBInstances"
      ],
      "Resource": "*"
    }
  ]
}
```

## Deployment Options

### 1. AWS ECS (Elastic Container Service)
```bash
# Build Docker image
docker build -t mini-app:latest .

# Tag for ECR
docker tag mini-app:latest <account-id>.dkr.ecr.<region>.amazonaws.com/mini-app:latest

# Push to ECR
docker push <account-id>.dkr.ecr.<region>.amazonaws.com/mini-app:latest

# Deploy to ECS using task definition with environment variables
```

### 2. AWS EKS (Elastic Kubernetes Service)
```yaml
# Kubernetes deployment with environment variables
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mini-app
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: mini-app
        image: <account-id>.dkr.ecr.<region>.amazonaws.com/mini-app:latest
        env:
        - name: AWS_REGION
          value: "us-east-1"
        - name: DB_SECRET_NAME
          value: "mini-app/database"
        - name: CONFIG_S3_BUCKET
          value: "mini-app-config-bucket"
```

### 3. AWS Elastic Beanstalk
```bash
# Package application
mvn clean package

# Deploy to Elastic Beanstalk
eb init -p java-11 mini-app
eb create mini-app-env --envvars AWS_REGION=us-east-1,DB_SECRET_NAME=mini-app/database
eb deploy
```

## Local Development

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Docker (optional, for local database)

### Run Locally with Environment Variables
```bash
# Set environment variables
export AWS_REGION=us-east-1
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=mini_app_db
export DB_USERNAME=root
export DB_PASSWORD=password
export SERVER_PORT=8080

# Build and run
mvn clean package
java -jar target/mini-java-app-1.0.0.jar
```

### Run with Docker Compose
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - AWS_REGION=us-east-1
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=mini_app_db
      - DB_USERNAME=root
      - DB_PASSWORD=password
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=password
      - MYSQL_DATABASE=mini_app_db
```

## Build and Package

### Maven Build
```bash
# Clean and package
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Build Docker image
mvn clean package
docker build -t mini-app:latest .
```

## Monitoring and Logging

### CloudWatch Logs
- Application logs are written to stdout in JSON format
- CloudWatch Logs agent automatically collects logs
- Use CloudWatch Insights for log analysis

### CloudWatch Metrics
- HikariCP connection pool metrics
- Application performance metrics
- Custom business metrics

### X-Ray Tracing
- AWS SDK automatically integrates with X-Ray
- Add X-Ray daemon to ECS/EKS for distributed tracing

## Health Checks

### Endpoints
- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

## Troubleshooting

### Common Issues

1. **Cannot connect to database**
   - Check DB_SECRET_NAME environment variable
   - Verify secret exists in Secrets Manager
   - Check security group rules for RDS

2. **Cannot load configuration from S3**
   - Check CONFIG_S3_BUCKET environment variable
   - Verify IAM role has S3 read permissions
   - Check bucket and object exist

3. **Timeout errors**
   - Increase connection timeout values
   - Check network connectivity
   - Verify security group rules

## Migration Checklist

- [ ] Create AWS Secrets Manager secrets
- [ ] Create AWS Parameter Store parameters
- [ ] Create S3 bucket for configuration
- [ ] Upload configuration files to S3
- [ ] Create IAM role with required permissions
- [ ] Update RDS security groups
- [ ] Configure CloudWatch Logs
- [ ] Test application locally with environment variables
- [ ] Deploy to AWS environment
- [ ] Verify health checks
- [ ] Monitor logs and metrics

## Support

For issues or questions, please contact the DevOps team or refer to the AWS documentation:
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [AWS Systems Manager Parameter Store](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html)
- [Amazon S3](https://docs.aws.amazon.com/s3/)
- [Amazon RDS](https://docs.aws.amazon.com/rds/)
