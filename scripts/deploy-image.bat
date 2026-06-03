@echo off
setlocal enabledelayedexpansion

REM Deploy Docker Image to AWS ECS Fargate
REM This script deploys a containerized application to AWS ECS Fargate

echo ==========================================
echo AWS ECS Fargate Deployment Script
echo ==========================================
echo.

REM Project configuration
set PROJECT_NAME=mini-java-app
set TASK_FAMILY=mini-java-app-task
set SERVICE_NAME=mini-java-app-service

REM Prompt for AWS region
set /p AWS_REGION="Enter AWS region (default: us-east-1): "
if "!AWS_REGION!"=="" set AWS_REGION=us-east-1

echo Using AWS region: !AWS_REGION!
echo.

REM Get AWS Account ID
echo Retrieving AWS Account ID...
for /f "delims=" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i

if "!ACCOUNT_ID!"=="" (
    echo Error: Failed to retrieve AWS Account ID. Please check your AWS credentials.
    exit /b 1
)

echo AWS Account ID: !ACCOUNT_ID!
echo.

REM Prompt for ECS cluster name
set /p CLUSTER_NAME="Enter ECS cluster name (default: mini-java-app-cluster): "
if "!CLUSTER_NAME!"=="" set CLUSTER_NAME=mini-java-app-cluster

echo Using cluster: !CLUSTER_NAME!
echo.

REM Check if cluster exists, create if not
echo Checking if ECS cluster exists...
aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Cluster does not exist. Creating ECS cluster...
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    echo ECS cluster created successfully
)
echo.

REM Prompt for VPC configuration
echo === Network Configuration ===
set /p VPC_ID="Enter VPC ID: "

if "!VPC_ID!"=="" (
    echo Error: VPC ID is required
    exit /b 1
)

REM Prompt for subnets
set /p SUBNETS_INPUT="Enter subnet IDs (comma-separated, at least 2): "

REM Parse subnets
for /f "tokens=1,2 delims=," %%a in ("!SUBNETS_INPUT!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)

REM Trim whitespace
set SUBNET_1=!SUBNET_1: =!
set SUBNET_2=!SUBNET_2: =!

if "!SUBNET_1!"=="" (
    echo Error: At least 2 subnets are required
    exit /b 1
)

if "!SUBNET_2!"=="" (
    echo Error: At least 2 subnets are required
    exit /b 1
)

echo Using subnets: !SUBNET_1!, !SUBNET_2!
echo.

REM Prompt for security group
set /p SECURITY_GROUP="Enter security group ID (must allow inbound traffic on port 8080): "

if "!SECURITY_GROUP!"=="" (
    echo Error: Security group ID is required
    exit /b 1
)

echo.

REM Prompt for Docker image URI
set /p IMAGE_URI="Enter Docker image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): "

if "!IMAGE_URI!"=="" (
    echo Error: Docker image URI is required
    exit /b 1
)

echo Using image: !IMAGE_URI!
echo.

REM Load balancer configuration
set /p NEED_LB="Do you need a load balancer for this service? (y/n): "

if /i "!NEED_LB!"=="y" (
    echo.
    echo === Creating Application Load Balancer ===
    
    REM Create ALB
    set ALB_NAME=mini-java-app-alb
    echo Creating Application Load Balancer: !ALB_NAME!
    
    for /f "delims=" %%i in ('aws elbv2 create-load-balancer --name !ALB_NAME! --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --scheme internet-facing --type application --ip-address-type ipv4 --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%i
    
    if "!ALB_ARN!"=="" (
        echo Error: Failed to create Application Load Balancer
        exit /b 1
    )
    
    echo ALB created: !ALB_ARN!
    
    REM Get ALB DNS name
    for /f "delims=" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --region !AWS_REGION! --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%i
    
    echo ALB DNS: !ALB_DNS!
    echo.
    
    REM Create Target Group
    set TG_NAME=mini-java-app-tg
    echo Creating Target Group: !TG_NAME!
    
    for /f "delims=" %%i in ('aws elbv2 create-target-group --name !TG_NAME! --protocol HTTP --port 8080 --vpc-id !VPC_ID! --target-type ip --health-check-enabled --health-check-protocol HTTP --health-check-path /actuator/health --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%i
    
    if "!TARGET_GROUP_ARN!"=="" (
        echo Error: Failed to create Target Group
        exit /b 1
    )
    
    echo Target Group created: !TARGET_GROUP_ARN!
    echo.
    
    REM Create Listener
    echo Creating ALB Listener...
    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn=!TARGET_GROUP_ARN! --region !AWS_REGION!
    
    echo Listener created successfully
    echo.
    
    REM Update service definition with load balancer
    powershell -Command "(Get-Content ecs\service-definition.json) -replace '{{TARGET_GROUP_ARN}}', '!TARGET_GROUP_ARN!' | Set-Content ecs\service-definition.json"
) else (
    echo Skipping load balancer configuration
    echo.
)

REM Prompt for environment variables
echo === Application Configuration ===
echo Using environment variables from application.properties
echo.

set /p DB_HOST="Enter database host (default: localhost): "
if "!DB_HOST!"=="" set DB_HOST=localhost

set /p DB_PORT="Enter database port (default: 3306): "
if "!DB_PORT!"=="" set DB_PORT=3306

set /p DB_NAME="Enter database name (default: mini_app_db): "
if "!DB_NAME!"=="" set DB_NAME=mini_app_db

set /p DB_USERNAME="Enter database username (default: root): "
if "!DB_USERNAME!"=="" set DB_USERNAME=root

set /p DB_PASSWORD="Enter database password: "

set /p REDIS_HOST="Enter Redis host (default: localhost): "
if "!REDIS_HOST!"=="" set REDIS_HOST=localhost

set /p REDIS_PORT="Enter Redis port (default: 6379): "
if "!REDIS_PORT!"=="" set REDIS_PORT=6379

set /p S3_BUCKET_NAME="Enter S3 bucket name (default: mini-app-config-bucket): "
if "!S3_BUCKET_NAME!"=="" set S3_BUCKET_NAME=mini-app-config-bucket

echo.

REM Create CloudWatch log group
echo Creating CloudWatch log group...
aws logs create-log-group --log-group-name /ecs/mini-java-app --region !AWS_REGION! 2>nul
echo.

REM Replace placeholders in task definition
echo Preparing task definition...
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{DB_USERNAME}}', '!DB_USERNAME!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Set-Content ecs\task-definition.json"
powershell -Command "(Get-Content ecs\task-definition.json) -replace '{{S3_BUCKET_NAME}}', '!S3_BUCKET_NAME!' | Set-Content ecs\task-definition.json"

echo Task definition prepared
echo.

REM Register task definition
echo Registering ECS task definition...
for /f "delims=" %%i in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i

if "!TASK_DEF_ARN!"=="" (
    echo Error: Failed to register task definition
    exit /b 1
)

echo Task definition registered: !TASK_DEF_ARN!
echo.

REM Replace placeholders in service definition
echo Preparing service definition...
powershell -Command "(Get-Content ecs\service-definition.json) -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content ecs\service-definition.json"
powershell -Command "(Get-Content ecs\service-definition.json) -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content ecs\service-definition.json"
powershell -Command "(Get-Content ecs\service-definition.json) -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content ecs\service-definition.json"
powershell -Command "(Get-Content ecs\service-definition.json) -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content ecs\service-definition.json"

echo Service definition prepared
echo.

REM Check if service exists
echo Checking if ECS service exists...
for /f "delims=" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[?status==`ACTIVE`].serviceName" --output text') do set EXISTING_SERVICE=%%i

if "!EXISTING_SERVICE!"=="" (
    echo Service does not exist. Creating new service...
    
    aws ecs create-service --cli-input-json file://ecs/service-definition.json --region !AWS_REGION!
    
    echo ECS service created successfully
) else (
    echo Service exists. Updating service...
    
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --desired-count 2 --region !AWS_REGION!
    
    echo ECS service updated successfully
)

echo.

REM Wait for service to become stable
echo Waiting for service to become stable (this may take a few minutes)...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!

echo Service is stable
echo.

REM Verify deployment
echo ==========================================
echo Deployment Verification
echo ==========================================

aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].[serviceName,status,runningCount,desiredCount]" --output table

echo.
echo ==========================================
echo Deployment completed successfully!
echo ==========================================
echo Cluster: !CLUSTER_NAME!
echo Service: !SERVICE_NAME!
echo Task Definition: !TASK_DEF_ARN!
echo Region: !AWS_REGION!

if /i "!NEED_LB!"=="y" (
    echo.
    echo Application Load Balancer:
    echo   DNS: !ALB_DNS!
    echo   Access your application at: http://!ALB_DNS!
)

echo.
echo CloudWatch Logs:
echo   Log Group: /ecs/mini-java-app
echo   View logs: https://console.aws.amazon.com/cloudwatch/home?region=!AWS_REGION!#logsV2:log-groups/log-group//ecs/mini-java-app
echo.
echo To view running tasks:
echo   aws ecs list-tasks --cluster !CLUSTER_NAME! --service-name !SERVICE_NAME! --region !AWS_REGION!
echo.

endlocal
