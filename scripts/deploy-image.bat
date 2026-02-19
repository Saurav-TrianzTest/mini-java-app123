@echo off
setlocal enabledelayedexpansion

REM ECS Fargate Deployment Script for Windows
echo ========================================
echo   AWS ECS Fargate Deployment Script
echo ========================================
echo.

REM Project configuration
set PROJECT_NAME=mini-java-app
set TASK_FAMILY=%PROJECT_NAME%-task
set SERVICE_NAME=%PROJECT_NAME%-service
set CONTAINER_NAME=%PROJECT_NAME%
set CONTAINER_PORT=8080

REM Prompt for AWS configuration
echo === AWS Configuration ===
set /p "AWS_REGION=Enter AWS Region (e.g., us-east-1): "
set /p "CLUSTER_NAME=Enter ECS Cluster Name: "

REM Check if cluster exists, create if not
echo.
echo Checking ECS cluster...
aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Cluster does not exist. Creating ECS cluster: !CLUSTER_NAME!
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECS cluster
        exit /b 1
    )
    echo Successfully created ECS cluster
)

echo Successfully verified ECS cluster: !CLUSTER_NAME!

REM Get AWS Account ID
echo.
echo Fetching AWS Account ID...
for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i

if "!ACCOUNT_ID!"=="" (
    echo ERROR: Failed to get AWS Account ID
    exit /b 1
)

echo AWS Account ID: !ACCOUNT_ID!

REM Prompt for network configuration
echo.
echo === Network Configuration ===
set /p "VPC_ID=Enter VPC ID: "
set /p "SUBNET_IDS=Enter Subnet IDs (comma-separated, at least 2): "
set /p "SECURITY_GROUP=Enter Security Group ID: "

REM Parse subnets
for /f "tokens=1,2 delims=," %%a in ("!SUBNET_IDS!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)

if "!SUBNET_1!"=="" (
    echo ERROR: At least 2 subnets are required for Fargate
    exit /b 1
)

if "!SUBNET_2!"=="" (
    echo ERROR: At least 2 subnets are required for Fargate
    exit /b 1
)

REM Prompt for container image
echo.
echo === Container Image ===
set /p "IMAGE_URI=Enter Docker Image URI: "

if "!IMAGE_URI!"=="" (
    echo ERROR: Image URI is required
    exit /b 1
)

REM Prompt for external service configuration
echo.
echo === External Service Configuration ===
set /p "DB_HOST=Enter Database Host: "
set /p "DB_USER=Enter Database User: "
set /p "DB_PASSWORD=Enter Database Password: "
set /p "REDIS_HOST=Enter Redis Host: "
set /p "RABBITMQ_HOST=Enter RabbitMQ Host: "

REM Prompt for load balancer
echo.
echo === Load Balancer Configuration ===
set /p "NEED_LB=Do you need a load balancer for this service? (y/n): "

if /i "!NEED_LB!"=="y" (
    echo Creating Application Load Balancer and Target Group...
    
    set TG_NAME=!PROJECT_NAME!-tg
    
    REM Create Target Group
    for /f "tokens=*" %%i in ('aws elbv2 create-target-group --name !TG_NAME! --protocol HTTP --port !CONTAINER_PORT! --vpc-id !VPC_ID! --target-type ip --health-check-path "/mini-app/" --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text 2^>nul') do set TARGET_GROUP_ARN=%%i
    
    if "!TARGET_GROUP_ARN!"=="" (
        for /f "tokens=*" %%i in ('aws elbv2 describe-target-groups --names !TG_NAME! --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%i
    )
    
    if "!TARGET_GROUP_ARN!"=="" (
        echo ERROR: Failed to create or retrieve Target Group
        exit /b 1
    )
    
    echo Successfully created Target Group: !TARGET_GROUP_ARN!
    
    REM Create Application Load Balancer
    set ALB_NAME=!PROJECT_NAME!-alb
    for /f "tokens=*" %%i in ('aws elbv2 create-load-balancer --name !ALB_NAME! --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --scheme internet-facing --type application --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text 2^>nul') do set ALB_ARN=%%i
    
    if "!ALB_ARN!"=="" (
        for /f "tokens=*" %%i in ('aws elbv2 describe-load-balancers --names !ALB_NAME! --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%i
    )
    
    if "!ALB_ARN!"=="" (
        echo ERROR: Failed to create or retrieve Application Load Balancer
        exit /b 1
    )
    
    echo Successfully created Application Load Balancer: !ALB_ARN!
    
    REM Create Listener
    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn=!TARGET_GROUP_ARN! --region !AWS_REGION! >nul 2>&1
    
    echo Successfully created Listener
    
    REM Get ALB DNS name
    for /f "tokens=*" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --region !AWS_REGION! --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%i
    
    echo Load Balancer DNS: !ALB_DNS!
) else (
    echo Skipping load balancer creation
    set TARGET_GROUP_ARN=
)

REM Create CloudWatch Log Group
echo.
echo Creating CloudWatch Log Group...
set LOG_GROUP=/ecs/!PROJECT_NAME!
aws logs create-log-group --log-group-name !LOG_GROUP! --region !AWS_REGION! >nul 2>&1
echo CloudWatch Log Group: !LOG_GROUP!

REM Replace placeholders in task definition
echo.
echo Preparing task definition...
copy ecs\task-definition.json %TEMP%\task-definition.json >nul

powershell -Command "(Get-Content %TEMP%\task-definition.json) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content %TEMP%\task-definition.json"
powershell -Command "(Get-Content %TEMP%\task-definition.json) -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content %TEMP%\task-definition.json"
powershell -Command "(Get-Content %TEMP%\task-definition.json) -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content %TEMP%\task-definition.json"
powershell -Command "(Get-Content %TEMP%\task-definition.json) -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content %TEMP%\task-definition.json"
powershell -Command "(Get-Content %TEMP%\task-definition.json) -replace '{{DB_USER}}', '!DB_USER!' | Set-Content %TEMP%\task-definition.json"
powershell -Command "(Get-Content %TEMP%\task-definition.json) -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content %TEMP%\task-definition.json"
powershell -Command "(Get-Content %TEMP%\task-definition.json) -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content %TEMP%\task-definition.json"
powershell -Command "(Get-Content %TEMP%\task-definition.json) -replace '{{RABBITMQ_HOST}}', '!RABBITMQ_HOST!' | Set-Content %TEMP%\task-definition.json"

REM Register task definition
echo Registering task definition...
for /f "tokens=*" %%i in ('aws ecs register-task-definition --cli-input-json file://%TEMP%/task-definition.json --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i

if "!TASK_DEF_ARN!"=="" (
    echo ERROR: Failed to register task definition
    exit /b 1
)

echo Task Definition registered: !TASK_DEF_ARN!

REM Prepare service definition
echo.
echo Preparing service definition...
copy ecs\service-definition.json %TEMP%\service-definition.json >nul

powershell -Command "(Get-Content %TEMP%\service-definition.json) -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content %TEMP%\service-definition.json"
powershell -Command "(Get-Content %TEMP%\service-definition.json) -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content %TEMP%\service-definition.json"
powershell -Command "(Get-Content %TEMP%\service-definition.json) -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content %TEMP%\service-definition.json"
powershell -Command "(Get-Content %TEMP%\service-definition.json) -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content %TEMP%\service-definition.json"

if "!TARGET_GROUP_ARN!"=="" (
    REM Remove loadBalancers section if no load balancer
    jq "del(.loadBalancers) | del(.healthCheckGracePeriodSeconds)" %TEMP%\service-definition.json > %TEMP%\service-definition-final.json
    move /y %TEMP%\service-definition-final.json %TEMP%\service-definition.json >nul
) else (
    powershell -Command "(Get-Content %TEMP%\service-definition.json) -replace '{{TARGET_GROUP_ARN}}', '!TARGET_GROUP_ARN!' | Set-Content %TEMP%\service-definition.json"
)

REM Check if service exists
echo.
echo Checking if service exists...
for /f "tokens=*" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[?status==`ACTIVE`].serviceName" --output text') do set SERVICE_EXISTS=%%i

if "!SERVICE_EXISTS!"=="" (
    echo Service does not exist. Creating new service...
    aws ecs create-service --cli-input-json file://%TEMP%/service-definition.json --region !AWS_REGION! >nul
    echo Service created: !SERVICE_NAME!
) else (
    echo Service exists. Updating service...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --region !AWS_REGION! >nul
    echo Service updated: !SERVICE_NAME!
)

REM Wait for service to stabilize
echo.
echo Waiting for service to stabilize...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!

echo Service is stable

REM Display deployment summary
echo.
echo ========================================
echo   Deployment Summary
echo ========================================
echo Cluster: !CLUSTER_NAME!
echo Service: !SERVICE_NAME!
echo Task Definition: !TASK_DEF_ARN!
echo Container: !CONTAINER_NAME!
echo CloudWatch Logs: !LOG_GROUP!

if not "!ALB_DNS!"=="" (
    echo Load Balancer DNS: http://!ALB_DNS!
    echo Application URL: http://!ALB_DNS!/mini-app/
)

echo.
echo Deployment completed successfully!
echo.

REM Verify running tasks
for /f "tokens=*" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].runningCount" --output text') do set RUNNING_TASKS=%%i

echo Running tasks: !RUNNING_TASKS!
echo.
echo To view logs:
echo   aws logs tail !LOG_GROUP! --follow --region !AWS_REGION!
echo.

endlocal
