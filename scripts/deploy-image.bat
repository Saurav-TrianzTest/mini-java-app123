@echo off
setlocal enabledelayedexpansion

echo === AWS ECS Fargate Deployment Script ===
echo.

REM Project configuration
set PROJECT_NAME=mini-java-app
set TASK_FAMILY=mini-java-app-task
set SERVICE_NAME=mini-java-app-service
set CONTAINER_NAME=mini-java-app

REM Prompt for deployment configuration
echo === AWS Configuration ===
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter ECS Cluster Name: "

echo.
echo === Network Configuration ===
set /p VPC_ID="Enter VPC ID: "
set /p SUBNET_IDS="Enter Subnet IDs (comma-separated, at least 2): "
set /p SECURITY_GROUP="Enter Security Group ID: "

REM Parse subnet IDs
for /f "tokens=1,2 delims=," %%a in ("!SUBNET_IDS!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)
if "!SUBNET_2!"=="" set SUBNET_2=!SUBNET_1!

echo.
echo === Image Configuration ===
set /p IMAGE_URI="Enter Docker Image URI: "

echo.
echo === External Service Configuration ===
set /p DB_HOST="Enter Database Host: "
set /p DB_PORT="Enter Database Port (default: 3306): "
if "!DB_PORT!"=="" set DB_PORT=3306
set /p DB_NAME="Enter Database Name: "
set /p DB_USERNAME="Enter Database Username: "
set /p DB_PASSWORD="Enter Database Password: "

set /p REDIS_HOST="Enter Redis Host: "
set /p REDIS_PORT="Enter Redis Port (default: 6379): "
if "!REDIS_PORT!"=="" set REDIS_PORT=6379
set /p REDIS_PASSWORD="Enter Redis Password: "

set /p EXTERNAL_API_BASE_URL="Enter External API Base URL: "
set /p EXTERNAL_API_KEY="Enter External API Key: "

set /p JWT_SECRET="Enter JWT Secret: "
set /p ADMIN_USERNAME="Enter Admin Username: "
set /p ADMIN_PASSWORD="Enter Admin Password: "

set /p MONITORING_ENDPOINT="Enter Monitoring Endpoint: "

REM Get AWS Account ID
echo.
echo Retrieving AWS Account ID...
for /f "tokens=*" %%a in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%a
echo Account ID: !ACCOUNT_ID!

REM Check/create ECS cluster
echo.
echo Checking ECS cluster...
aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Cluster does not exist. Creating cluster...
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    echo Cluster created successfully
)

REM Ask about load balancer
echo.
echo === Load Balancer Configuration ===
set /p NEED_LB="Do you need a load balancer for this service? (y/n): "

if /i "!NEED_LB!"=="y" (
    echo.
    echo Creating Application Load Balancer and Target Group...
    
    set TARGET_GROUP_NAME=!PROJECT_NAME!-tg
    
    REM Create target group
    for /f "tokens=*" %%a in ('aws elbv2 create-target-group --name !TARGET_GROUP_NAME! --protocol HTTP --port 8080 --vpc-id !VPC_ID! --target-type ip --health-check-path /health --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text 2^>nul') do set TARGET_GROUP_ARN=%%a
    
    if "!TARGET_GROUP_ARN!"=="" (
        for /f "tokens=*" %%a in ('aws elbv2 describe-target-groups --names !TARGET_GROUP_NAME! --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%a
    )
    
    echo Target Group ARN: !TARGET_GROUP_ARN!
    set SERVICE_DEF_FILE=ecs\service-definition.json
) else (
    echo Skipping load balancer configuration
    set SERVICE_DEF_FILE=ecs\service-definition-no-lb.json
    
    REM Create temporary service definition without load balancer
    powershell -Command "(Get-Content ecs\service-definition.json | ConvertFrom-Json | Select-Object -Property * -ExcludeProperty loadBalancers,healthCheckGracePeriodSeconds) | ConvertTo-Json -Depth 10 | Set-Content !SERVICE_DEF_FILE!"
)

REM Replace placeholders in task definition
echo.
echo Preparing task definition...
copy /y ecs\task-definition.json ecs\task-definition-temp.json >nul

powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{IMAGE_URI}}','!IMAGE_URI!' -replace '{{AWS_REGION}}','!AWS_REGION!' -replace '{{ACCOUNT_ID}}','!ACCOUNT_ID!' -replace '{{DB_HOST}}','!DB_HOST!' -replace '{{DB_PORT}}','!DB_PORT!' -replace '{{DB_NAME}}','!DB_NAME!' -replace '{{DB_USERNAME}}','!DB_USERNAME!' -replace '{{DB_PASSWORD}}','!DB_PASSWORD!' -replace '{{REDIS_HOST}}','!REDIS_HOST!' -replace '{{REDIS_PORT}}','!REDIS_PORT!' -replace '{{REDIS_PASSWORD}}','!REDIS_PASSWORD!' -replace '{{EXTERNAL_API_BASE_URL}}','!EXTERNAL_API_BASE_URL!' -replace '{{EXTERNAL_API_KEY}}','!EXTERNAL_API_KEY!' -replace '{{JWT_SECRET}}','!JWT_SECRET!' -replace '{{ADMIN_USERNAME}}','!ADMIN_USERNAME!' -replace '{{ADMIN_PASSWORD}}','!ADMIN_PASSWORD!' -replace '{{MONITORING_ENDPOINT}}','!MONITORING_ENDPOINT!' | Set-Content ecs\task-definition-temp.json"

REM Register task definition
echo.
echo Registering task definition...
for /f "tokens=*" %%a in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition-temp.json --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%a

if !ERRORLEVEL! neq 0 (
    echo Failed to register task definition
    del /f ecs\task-definition-temp.json
    exit /b 1
)

echo Task definition registered: !TASK_DEF_ARN!

REM Replace placeholders in service definition
echo.
echo Preparing service definition...
copy /y !SERVICE_DEF_FILE! ecs\service-definition-temp.json >nul

powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{CLUSTER_NAME}}','!CLUSTER_NAME!' -replace '{{SUBNET_1}}','!SUBNET_1!' -replace '{{SUBNET_2}}','!SUBNET_2!' -replace '{{SECURITY_GROUP}}','!SECURITY_GROUP!' -replace '{{TARGET_GROUP_ARN}}','!TARGET_GROUP_ARN!' | Set-Content ecs\service-definition-temp.json"

REM Check if service exists
echo.
echo Checking if service exists...
for /f "tokens=*" %%a in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].serviceName" --output text 2^>nul') do set SERVICE_EXISTS=%%a

if "!SERVICE_EXISTS!"=="!SERVICE_NAME!" (
    echo Service exists. Updating service...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --region !AWS_REGION! --force-new-deployment
    
    if !ERRORLEVEL! neq 0 (
        echo Failed to update service
        del /f ecs\task-definition-temp.json ecs\service-definition-temp.json
        exit /b 1
    )
    
    echo Service updated successfully
) else (
    echo Service does not exist. Creating service...
    aws ecs create-service --cli-input-json file://ecs/service-definition-temp.json --region !AWS_REGION!
    
    if !ERRORLEVEL! neq 0 (
        echo Failed to create service
        del /f ecs\task-definition-temp.json ecs\service-definition-temp.json
        exit /b 1
    )
    
    echo Service created successfully
)

REM Wait for service stability
echo.
echo Waiting for service to become stable...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!

if !ERRORLEVEL! equ 0 (
    echo Service is stable
) else (
    echo Warning: Service stability check timed out or failed
)

REM Verify deployment
echo.
echo Verifying deployment...
aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].[serviceName,status,runningCount,desiredCount]" --output table

REM Display access information
if /i "!NEED_LB!"=="y" (
    echo.
    echo === Load Balancer Information ===
    for /f "tokens=*" %%a in ('aws elbv2 describe-load-balancers --region !AWS_REGION! --query "LoadBalancers[?VpcId=='!VPC_ID!'].DNSName" --output text') do set LB_DNS=%%a
    
    if not "!LB_DNS!"=="" (
        echo Application URL: http://!LB_DNS!
    )
)

echo.
echo === CloudWatch Logs ===
echo Log Group: /ecs/!PROJECT_NAME!
echo View logs: aws logs tail /ecs/!PROJECT_NAME! --follow --region !AWS_REGION!

REM Cleanup
del /f ecs\task-definition-temp.json ecs\service-definition-temp.json ecs\service-definition-no-lb.json 2>nul

echo.
echo === Deployment Completed Successfully ===
echo Next steps:
echo 1. Monitor service status: aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!
echo 2. View running tasks: aws ecs list-tasks --cluster !CLUSTER_NAME! --service-name !SERVICE_NAME! --region !AWS_REGION!
echo 3. Check application logs: aws logs tail /ecs/!PROJECT_NAME! --follow --region !AWS_REGION!

endlocal
