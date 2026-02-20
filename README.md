# Mini Java Application - Cloud-Ready Version

## Overview

This application has been transformed to be fully cloud-ready and compatible with AWS, Azure, and GCP environments. All hardcoded values have been replaced with environment variables, and the application follows cloud-native best practices.

## Cloud Readiness Features

### ✅ Configuration Management
- All configuration values are externalized via environment variables
- No hardcoded credentials or sensitive data in source code
- Support for AWS Secrets Manager integration
- Follows 12-factor app principles

### ✅ Database Persistence
- HikariCP connection pooling for cloud resilience
- Configurable connection timeouts and pool sizes
- Health checks for container orchestration
- Proper resource management and cleanup

### ✅ File System Independence
- Uses classpath resources instead of absolute file paths
- AWS S3 integration for cloud storage
- No local file system dependencies
- Configurable storage backend (S3 or local)

### ✅ Network Communication
- Port configuration via environment variables
- HTTP-based communication (no raw sockets)
- Proper timeout configurations
- Health check endpoints

### ✅ Logging & Monitoring
- Structured JSON logging for cloud log aggregation
- Console output for container log collection
- Correlation IDs for distributed tracing
- Integration with cloud monitoring systems

### ✅ Security
- No hardcoded credentials
- Environment variable-based authentication
- Support for cloud IAM and secrets management
- Non-root container user

### ✅ Containerization
- Multi-stage Docker build for optimized images
- Health checks for orchestration
- Graceful shutdown support
- Resource limits and monitoring

## Environment Variables

All configuration is done via environment variables. See `.env.example` for a complete list.

### Required Variables

```bash
# Database
DATABASE_URL=jdbc:mysql://your-db-host:3306/mini_app_db
DATABASE_USERNAME=your-username
DATABASE_PASSWORD=your-password

# AWS Configuration
AWS_REGION=us-east-1
S3_BUCKET_NAME=your-bucket-name
```

### Optional Variables

See `.env.example` for all optional configuration variables with their default values.

## Local Development

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Docker and Docker Compose (for containerized development)

### Running Locally with Docker Compose

1. Copy the environment file:
```bash
cp .env.example .env
```

2. Update `.env` with your configuration

3. Start all services:
```bash
docker-compose up -d
```

4. View logs:
```bash
docker-compose logs -f app
```

5. Stop services:
```bash
docker-compose down
```

### Running Locally without Docker

1. Set environment variables:
```bash
export DATABASE_URL=jdbc:mysql://localhost:3306/mini_app_db
export DATABASE_USERNAME=root
export DATABASE_PASSWORD=password
# ... other variables
```

2. Build the application:
```bash
mvn clean package
```

3. Run the application:
```bash
java -jar target/mini-java-app-1.0.0.jar
```

## Cloud Deployment

### AWS Deployment

#### Using AWS ECS (Elastic Container Service)

1. Build and push Docker image to ECR:
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker build -t mini-java-app .
docker tag mini-java-app:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

2. Create ECS task definition with environment variables from AWS Secrets Manager

3. Deploy to ECS cluster

#### Using AWS Elastic Beanstalk

1. Create `Dockerrun.aws.json`:
```json
{
  "AWSEBDockerrunVersion": "1",
  "Image": {
    "Name": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest"
  },
  "Ports": [
    {
      "ContainerPort": 8080
    }
  ]
}
```

2. Deploy using EB CLI:
```bash
eb init -p docker mini-java-app
eb create mini-java-app-env
eb deploy
```

#### Using AWS Lambda (with Spring Cloud Function)

For serverless deployment, additional modifications would be needed to support AWS Lambda runtime.

### Azure Deployment

#### Using Azure Container Instances

```bash
az container create \
  --resource-group myResourceGroup \
  --name mini-java-app \
  --image <registry>.azurecr.io/mini-java-app:latest \
  --cpu 1 --memory 1 \
  --registry-login-server <registry>.azurecr.io \
  --registry-username <username> \
  --registry-password <password> \
  --environment-variables \
    DATABASE_URL=<value> \
    DATABASE_USERNAME=<value> \
    DATABASE_PASSWORD=<value>
```

### GCP Deployment

#### Using Google Cloud Run

```bash
gcloud run deploy mini-java-app \
  --image gcr.io/<project-id>/mini-java-app:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars DATABASE_URL=<value>,DATABASE_USERNAME=<value>,DATABASE_PASSWORD=<value>
```

## Health Checks

The application provides health check endpoints for container orchestration:

- **Health Check**: `/actuator/health`
- **Readiness**: Application is ready when database connection is established
- **Liveness**: Application is alive when it can respond to HTTP requests

## Monitoring

### Structured Logging

All logs are output in JSON format for easy integration with cloud logging systems:

- AWS CloudWatch Logs
- Azure Monitor
- Google Cloud Logging
- ELK Stack
- Splunk

### Metrics

The application exposes metrics for monitoring:

- Database connection pool metrics
- HTTP request metrics
- JVM metrics
- Custom application metrics

## Security Best Practices

1. **No Hardcoded Credentials**: All credentials are provided via environment variables
2. **AWS Secrets Manager**: Use AWS Secrets Manager for sensitive data
3. **IAM Roles**: Use IAM roles instead of access keys when running in AWS
4. **Non-Root User**: Container runs as non-root user
5. **Network Security**: Use security groups and network policies

## Troubleshooting

### Database Connection Issues

Check environment variables:
```bash
echo $DATABASE_URL
echo $DATABASE_USERNAME
```

View connection pool metrics in logs:
```bash
docker-compose logs app | grep HikariPool
```

### S3 Storage Issues

Verify AWS credentials and bucket access:
```bash
aws s3 ls s3://$S3_BUCKET_NAME
```

### Application Logs

View structured JSON logs:
```bash
docker-compose logs app | jq .
```

## Migration from Legacy Version

### Key Changes

1. **File Paths**: Replaced absolute paths with classpath resources and S3
2. **Database**: Added HikariCP connection pooling
3. **Configuration**: Externalized all configuration to environment variables
4. **Logging**: Changed from file-based to structured console logging
5. **Networking**: Replaced raw sockets with HTTP-based communication

### Migration Checklist

- [ ] Set all required environment variables
- [ ] Configure AWS S3 bucket for file storage
- [ ] Update database connection strings
- [ ] Configure secrets in AWS Secrets Manager
- [ ] Test health check endpoints
- [ ] Verify structured logging output
- [ ] Test horizontal scaling
- [ ] Validate graceful shutdown

## Support

For issues or questions, please contact the development team.

## License

Copyright © 2024. All rights reserved.
