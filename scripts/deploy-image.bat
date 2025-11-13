@echo off
setlocal enabledelayedexpansion

echo === AWS ECS Fargate Deployment Script for Mini Java App ===
echo.

:: Configuration
set APP_NAME=mini-java-app
set TASK_FAMILY=!APP_NAME!-task
set SERVICE_NAME=!APP_NAME!-service

:: Get deployment parameters
echo === Deployment Configuration ===
echo.

:: AWS Region
set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
if "!AWS_REGION!"=="" (
    echo Error: AWS region is required
    exit /b 1
)

:: ECS Cluster
set /p CLUSTER_NAME="Enter ECS cluster name: "
if "!CLUSTER_NAME!"=="" (
    echo Error: ECS cluster name is required
    exit /b 1
)

:: Docker image URI
set /p IMAGE_URI="Enter Docker image URI: "
if "!IMAGE_URI!"=="" (
    echo Error: Docker image URI is required
    exit /b 1
)

:: Network configuration
set /p VPC_ID="Enter VPC ID: "
if "!VPC_ID!"=="" (
    echo Error: VPC ID is required
    exit /b 1
)

set /p SUBNET_INPUT="Enter subnet IDs (comma-separated): "
if "!SUBNET_INPUT!"=="" (
    echo Error: At least one subnet ID is required
    exit /b 1
)

:: Parse subnets
for /f "tokens=1,2 delims=," %%a in ("!SUBNET_INPUT!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)
:: Remove spaces
set SUBNET_1=!SUBNET_1: =!
set SUBNET_2=!SUBNET_2: =!
if "!SUBNET_2!"=="" set SUBNET_2=!SUBNET_1!

set /p SECURITY_GROUP="Enter security group ID: "
if "!SECURITY_GROUP!"=="" (
    echo Error: Security group ID is required
    exit /b 1
)

echo.
echo === Configuration Summary ===
echo App Name: !APP_NAME!
echo AWS Region: !AWS_REGION!
echo ECS Cluster: !CLUSTER_NAME!
echo Image URI: !IMAGE_URI!
echo VPC ID: !VPC_ID!
echo Subnets: !SUBNET_1!, !SUBNET_2!
echo Security Group: !SECURITY_GROUP!
echo.

:: Get AWS Account ID
echo Getting AWS Account ID...
for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
if !ERRORLEVEL! neq 0 (
    echo Error: Failed to get AWS Account ID
    exit /b 1
)
echo AWS Account ID: !ACCOUNT_ID!
echo.

:: Check/Create ECS cluster
echo Checking ECS cluster...
aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Creating ECS cluster: !CLUSTER_NAME!
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo Error: Failed to create ECS cluster
        exit /b 1
    )
)

:: Create CloudWatch log group
echo Creating CloudWatch log group...
aws logs create-log-group --log-group-name "/ecs/!APP_NAME!" --region !AWS_REGION! 2>nul

:: Load balancer configuration
set /p USE_LB="Do you need a load balancer for this service? (y/n): "

if /i "!USE_LB!"=="y" (
    echo Creating Application Load Balancer and Target Group...
    
    :: Create ALB
    for /f "tokens=*" %%i in ('aws elbv2 create-load-balancer --name "!APP_NAME!-alb" --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%i
    
    if !ERRORLEVEL! neq 0 (
        echo Error: Failed to create load balancer
        exit /b 1
    )
    
    :: Create Target Group
    for /f "tokens=*" %%i in ('aws elbv2 create-target-group --name "!APP_NAME!-tg" --protocol HTTP --port 8080 --vpc-id !VPC_ID! --target-type ip --health-check-path "/mini-app/actuator/health" --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 5 --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%i
    
    if !ERRORLEVEL! neq 0 (
        echo Error: Failed to create target group
        exit /b 1
    )
    
    :: Create ALB Listener
    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn=!TARGET_GROUP_ARN! --region !AWS_REGION! >nul
    
    echo Load balancer created successfully
    echo Target Group ARN: !TARGET_GROUP_ARN!
) else (
    set TARGET_GROUP_ARN=
    echo Skipping load balancer creation
)

echo.

:: Store sensitive configuration in SSM Parameter Store
echo Creating SSM parameters for sensitive configuration...

aws ssm put-parameter --name "/mini-java-app/db-host" --value "your-rds-endpoint" --type "SecureString" --region !AWS_REGION! --overwrite 2>nul
aws ssm put-parameter --name "/mini-java-app/db-username" --value "admin" --type "SecureString" --region !AWS_REGION! --overwrite 2>nul
aws ssm put-parameter --name "/mini-java-app/db-password" --value "changeme123" --type "SecureString" --region !AWS_REGION! --overwrite 2>nul
aws ssm put-parameter --name "/mini-java-app/redis-host" --value "your-redis-endpoint" --type "SecureString" --region !AWS_REGION! --overwrite 2>nul
aws ssm put-parameter --name "/mini-java-app/redis-password" --value "redis-password" --type "SecureString" --region !AWS_REGION! --overwrite 2>nul
aws ssm put-parameter --name "/mini-java-app/external-api-key" --value "your-api-key" --type "SecureString" --region !AWS_REGION! --overwrite 2>nul
aws ssm put-parameter --name "/mini-java-app/jwt-secret" --value "jwt-secret-key-changeme" --type "SecureString" --region !AWS_REGION! --overwrite 2>nul
aws ssm put-parameter --name "/mini-java-app/admin-username" --value "admin" --type "SecureString" --region !AWS_REGION! --overwrite 2>nul
aws ssm put-parameter --name "/mini-java-app/admin-password" --value "admin-password-changeme" --type "SecureString" --region !AWS_REGION! --overwrite 2>nul

echo SSM parameters created
echo.

:: Update JSON files with actual values using PowerShell
echo Updating deployment files with configuration...

powershell -Command "(Get-Content 'ecs\task-definition.json') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content 'ecs\task-definition.json'"
powershell -Command "(Get-Content 'ecs\task-definition.json') -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content 'ecs\task-definition.json'"
powershell -Command "(Get-Content 'ecs\task-definition.json') -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content 'ecs\task-definition.json'"

powershell -Command "(Get-Content 'ecs\service-definition.json') -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content 'ecs\service-definition.json'"
powershell -Command "(Get-Content 'ecs\service-definition.json') -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content 'ecs\service-definition.json'"
powershell -Command "(Get-Content 'ecs\service-definition.json') -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content 'ecs\service-definition.json'"
powershell -Command "(Get-Content 'ecs\service-definition.json') -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content 'ecs\service-definition.json'"

if not "!TARGET_GROUP_ARN!"=="" (
    powershell -Command "(Get-Content 'ecs\service-definition.json') -replace '{{TARGET_GROUP_ARN}}', '!TARGET_GROUP_ARN!' | Set-Content 'ecs\service-definition.json'"
) else (
    :: Remove load balancer section if not using LB
    powershell -Command "$content = Get-Content 'ecs\service-definition.json' -Raw; $content = $content -replace '(?s)\s*\"loadBalancers\":\s*\[.*?\],', ''; $content = $content -replace '\s*\"healthCheckGracePeriodSeconds\":\s*\d+,?', ''; $content | Set-Content 'ecs\service-definition.json'"
)

:: Register task definition
echo Registering ECS task definition...
for /f "tokens=*" %%i in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition.json --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i

if !ERRORLEVEL! neq 0 (
    echo Error: Failed to register task definition
    exit /b 1
)

echo Task definition registered successfully
echo Task Definition ARN: !TASK_DEF_ARN!
echo.

:: Check if service exists
echo Checking if ECS service exists...
for /f "tokens=*" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].serviceName" --output text 2^>nul') do set SERVICE_EXISTS=%%i

if "!SERVICE_EXISTS!"=="!SERVICE_NAME!" (
    :: Update existing service
    echo Updating existing ECS service...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition "!TASK_DEF_ARN!" --region !AWS_REGION! >nul
    
    if !ERRORLEVEL! neq 0 (
        echo Error: Failed to update service
        exit /b 1
    )
    
    echo Service updated successfully
) else (
    :: Create new service
    echo Creating new ECS service...
    aws ecs create-service --cli-input-json file://ecs/service-definition.json --region !AWS_REGION! >nul
    
    if !ERRORLEVEL! neq 0 (
        echo Error: Failed to create service
        exit /b 1
    )
    
    echo Service created successfully
)

echo.

:: Wait for service stability
echo Waiting for service to become stable (this may take several minutes)...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!

if !ERRORLEVEL! equ 0 (
    echo Service is stable and running
) else (
    echo Warning: Service stability check timed out, but deployment may still be in progress
)

echo.

:: Display deployment information
echo === Deployment Completed ===
echo Cluster: !CLUSTER_NAME!
echo Service: !SERVICE_NAME!
echo Task Definition: !TASK_FAMILY!
echo Image: !IMAGE_URI!
echo CloudWatch Logs: /ecs/!APP_NAME!

if not "!TARGET_GROUP_ARN!"=="" (
    :: Get ALB DNS name
    for /f "tokens=*" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --region !AWS_REGION! --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%i
    
    echo Load Balancer: http://!ALB_DNS!
    echo Health Check: http://!ALB_DNS!/mini-app/actuator/health
)

echo.
echo Next steps:
echo 1. Monitor deployment in AWS ECS console
echo 2. Check CloudWatch logs for application startup
echo 3. Update SSM parameters with actual database and service configurations
echo 4. Configure DNS and SSL certificates for production use
echo.

echo Deployment script completed successfully!
pause