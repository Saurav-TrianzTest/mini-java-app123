# Cloud Readiness Fixes - Mini Java Application

## Overview
This document describes the cloud readiness fixes applied to make the application compatible with AWS cloud deployment.

## Issues Fixed

### 1. Hard-coded File Paths (cr-java-0061)
**Problem**: Application contained absolute file paths that reference specific locations on the host file system, which do not exist in cloud environments.

**Affected Lines**:
- Line 44: `File configFile = new File(CONFIG_FILE_PATH);`
- Line 60: `File logDir = new File("/var/log");`
- Line 65: `File logFile = new File(LOG_FILE_PATH);`

**Solution**: Replaced all hard-coded file paths with Amazon S3 object storage using AWS SDK for Java v2.

## Changes Made

### 1. Added AWS SDK Dependencies (pom.xml)
- Added `software.amazon.awssdk:s3:2.20.26`
- Added `software.amazon.awssdk:auth:2.20.26`

### 2. Created S3Service Class (S3Service.java)
New cloud-native service class that provides:
- `readPropertiesFromS3(String key)`: Read configuration files from S3
- `objectExists(String key)`: Check if S3 object exists
- `writeToS3(String key, String content)`: Write content to S3
- `createLogInS3(String key, String logMessage)`: Create/append log entries in S3

Features:
- Uses DefaultCredentialsProvider for IAM role-based authentication
- Configurable AWS region via environment variable
- Configurable S3 bucket name via environment variable

### 3. Updated MiniApp.java
**Configuration Loading (Line 44)**:
- Removed: `File configFile = new File(CONFIG_FILE_PATH);`
- Added: S3-based configuration loading using `s3Service.readPropertiesFromS3(CONFIG_S3_KEY)`

**Logging Initialization (Lines 60, 65)**:
- Removed: Local file system operations for log directory and file creation
- Added: S3-based logging using `s3Service.createLogInS3(LOG_S3_KEY, initialLogMessage)`

### 4. Updated application.properties
Added cloud-native S3 configuration properties:
```properties
s3.bucket.name=${S3_BUCKET_NAME:mini-app-storage}
s3.config.key=${CONFIG_S3_KEY:config/app.properties}
s3.log.key=${LOG_S3_KEY:logs/mini-app.log}
aws.region=${AWS_REGION:us-east-1}
```

## Environment Variables Required

The following environment variables should be set in the cloud deployment:

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `S3_BUCKET_NAME` | S3 bucket name for application storage | `mini-app-storage` |
| `CONFIG_S3_KEY` | S3 object key for configuration file | `config/app.properties` |
| `LOG_S3_KEY` | S3 object key for log file | `logs/mini-app.log` |
| `AWS_REGION` | AWS region for S3 operations | `us-east-1` |

## AWS IAM Permissions Required

The application requires the following IAM permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:HeadObject"
      ],
      "Resource": "arn:aws:s3:::mini-app-storage/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::mini-app-storage"
    }
  ]
}
```

## Deployment Notes

1. **S3 Bucket Setup**: Create an S3 bucket before deploying the application
2. **IAM Role**: Attach the required IAM policy to the EC2 instance role, ECS task role, or Lambda execution role
3. **Configuration Upload**: Upload the `app.properties` file to S3 at the configured key path
4. **Environment Variables**: Set all required environment variables in the deployment configuration

## Benefits

- ✅ **Cloud-Native**: No dependency on local file system
- ✅ **Scalable**: Works across multiple instances without shared file system
- ✅ **Secure**: Uses IAM roles for authentication (no hardcoded credentials)
- ✅ **Flexible**: Configuration can be updated in S3 without redeploying
- ✅ **Durable**: S3 provides 99.999999999% durability for logs and configuration

## Testing

To test locally with AWS credentials:
1. Configure AWS credentials using AWS CLI or environment variables
2. Create an S3 bucket
3. Set environment variables for bucket name and region
4. Run the application

## Next Steps

Additional cloud readiness improvements to consider:
- Replace hardcoded database credentials with AWS Secrets Manager
- Replace hardcoded ports with environment variables
- Implement structured logging (JSON format) for CloudWatch
- Add connection pooling for database connections
- Implement health check endpoints for load balancers
