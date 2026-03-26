@echo off
setlocal enabledelayedexpansion

REM ECS Fargate Deployment Script for mini-java-app (Windows)
REM This script deploys the Docker image to AWS ECS Fargate

echo ==========================================
echo AWS ECS Fargate Deployment Script
echo ==========================================
echo.

REM Project configuration
set PROJECT_NAME=mini-java-app
set TASK_FAMILY=mini-java-app-task
set SERVICE_NAME=mini-java-app-service

echo Project: %PROJECT_NAME%
echo.

REM Prompt for AWS configuration
echo === AWS Configuration ===
set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter ECS cluster name: "

REM Get AWS Account ID
echo.
echo Retrieving AWS Account ID...
for /f "delims=" %%a in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%a
echo AWS Account ID: !ACCOUNT_ID!
echo.

REM Check if cluster exists, create if not
echo Checking if ECS cluster exists...
aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Cluster does not exist. Creating ECS cluster: !CLUSTER_NAME!
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECS cluster
        exit /b 1
    )
    echo ECS cluster created successfully
)
echo.

REM Prompt for network configuration
echo === Network Configuration ===
set /p VPC_ID="Enter VPC ID: "
set /p SUBNETS_INPUT="Enter Subnet IDs (comma-separated, at least 2): "
set /p SECURITY_GROUP="Enter Security Group ID: "

REM Parse subnets
for /f "tokens=1,2 delims=," %%a in ("!SUBNETS_INPUT!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)
if "!SUBNET_2!"=="" set SUBNET_2=!SUBNET_1!

echo.
echo VPC: !VPC_ID!
echo Subnet 1: !SUBNET_1!
echo Subnet 2: !SUBNET_2!
echo Security Group: !SECURITY_GROUP!
echo.

REM Prompt for Docker image URI
echo === Docker Image Configuration ===
set /p IMAGE_URI="Enter Docker image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): "
echo.

REM Load balancer configuration
echo === Load Balancer Configuration ===
set /p NEED_LB="Do you need a load balancer for this service? (y/n): "

if /i "!NEED_LB!"=="y" (
    echo.
    echo Creating Application Load Balancer and Target Group...
    
    REM Create target group with target-type ip
    set TARGET_GROUP_NAME=!PROJECT_NAME!-tg
    
    echo Creating target group: !TARGET_GROUP_NAME!
    for /f "delims=" %%a in ('aws elbv2 create-target-group --name !TARGET_GROUP_NAME! --protocol HTTP --port 8080 --vpc-id !VPC_ID! --target-type ip --health-check-enabled --health-check-protocol HTTP --health-check-path /health --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text 2^>nul') do set TARGET_GROUP_ARN=%%a
    
    if "!TARGET_GROUP_ARN!"=="" (
        echo Target group may already exist. Retrieving existing target group...
        for /f "delims=" %%a in ('aws elbv2 describe-target-groups --names !TARGET_GROUP_NAME! --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%a
    )
    
    echo Target Group ARN: !TARGET_GROUP_ARN!
    echo.
    
    REM Create Application Load Balancer
    set ALB_NAME=!PROJECT_NAME!-alb
    
    echo Creating Application Load Balancer: !ALB_NAME!
    for /f "delims=" %%a in ('aws elbv2 create-load-balancer --name !ALB_NAME! --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --scheme internet-facing --type application --ip-address-type ipv4 --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text 2^>nul') do set ALB_ARN=%%a
    
    if "!ALB_ARN!"=="" (
        echo Load balancer may already exist. Retrieving existing load balancer...
        for /f "delims=" %%a in ('aws elbv2 describe-load-balancers --names !ALB_NAME! --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%a
    )
    
    echo Load Balancer ARN: !ALB_ARN!
    echo.
    
    REM Create listener
    echo Creating listener for load balancer...
    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn=!TARGET_GROUP_ARN! --region !AWS_REGION! >nul 2>&1
    echo.
    
    REM Get ALB DNS name
    for /f "delims=" %%a in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --region !AWS_REGION! --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%a
    
    echo Load Balancer DNS: !ALB_DNS!
    echo.
    
    set USE_LOAD_BALANCER=true
) else (
    echo Skipping load balancer configuration
    echo.
    set USE_LOAD_BALANCER=false
)

REM Create CloudWatch log group
echo === CloudWatch Logs Configuration ===
set LOG_GROUP=/ecs/!PROJECT_NAME!
echo Creating CloudWatch log group: !LOG_GROUP!
aws logs create-log-group --log-group-name !LOG_GROUP! --region !AWS_REGION! >nul 2>&1
echo.

REM Prepare task definition
echo === Preparing Task Definition ===
set TASK_DEF_FILE=..\ecs\task-definition.json
set TASK_DEF_TEMP=%TEMP%\task-definition-!PROJECT_NAME!.json

copy !TASK_DEF_FILE! !TASK_DEF_TEMP! >nul

REM Replace placeholders in task definition using PowerShell
powershell -Command "(Get-Content '!TASK_DEF_TEMP!') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TASK_DEF_TEMP!'"
powershell -Command "(Get-Content '!TASK_DEF_TEMP!') -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content '!TASK_DEF_TEMP!'"
powershell -Command "(Get-Content '!TASK_DEF_TEMP!') -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content '!TASK_DEF_TEMP!'"

echo Task definition prepared
echo.

REM Register task definition
echo === Registering Task Definition ===
for /f "delims=" %%a in ('aws ecs register-task-definition --cli-input-json file://!TASK_DEF_TEMP! --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%a

echo Task definition registered: !TASK_DEF_ARN!
echo.

REM Prepare service definition
echo === Preparing Service Definition ===
set SERVICE_DEF_FILE=..\ecs\service-definition.json
set SERVICE_DEF_TEMP=%TEMP%\service-definition-!PROJECT_NAME!.json

copy !SERVICE_DEF_FILE! !SERVICE_DEF_TEMP! >nul

REM Replace placeholders in service definition
powershell -Command "(Get-Content '!SERVICE_DEF_TEMP!') -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content '!SERVICE_DEF_TEMP!'"
powershell -Command "(Get-Content '!SERVICE_DEF_TEMP!') -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content '!SERVICE_DEF_TEMP!'"
powershell -Command "(Get-Content '!SERVICE_DEF_TEMP!') -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content '!SERVICE_DEF_TEMP!'"
powershell -Command "(Get-Content '!SERVICE_DEF_TEMP!') -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content '!SERVICE_DEF_TEMP!'"

if "!USE_LOAD_BALANCER!"=="true" (
    powershell -Command "(Get-Content '!SERVICE_DEF_TEMP!') -replace '{{TARGET_GROUP_ARN}}', '!TARGET_GROUP_ARN!' | Set-Content '!SERVICE_DEF_TEMP!'"
) else (
    REM Remove loadBalancers section if not using load balancer
    powershell -Command "$json = Get-Content '!SERVICE_DEF_TEMP!' | ConvertFrom-Json; $json.PSObject.Properties.Remove('loadBalancers'); $json.PSObject.Properties.Remove('healthCheckGracePeriodSeconds'); $json | ConvertTo-Json -Depth 10 | Set-Content '!SERVICE_DEF_TEMP!'" 2>nul
)

echo Service definition prepared
echo.

REM Check if service exists
echo === Checking Service Status ===
for /f "delims=" %%a in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[?status==`ACTIVE`].serviceName" --output text 2^>nul') do set EXISTING_SERVICE=%%a

if "!EXISTING_SERVICE!"=="" (
    echo Service does not exist. Creating new service...
    aws ecs create-service --cli-input-json file://!SERVICE_DEF_TEMP! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create service
        exit /b 1
    )
    echo Service created successfully
) else (
    echo Service exists. Updating service with new task definition...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --force-new-deployment --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to update service
        exit /b 1
    )
    echo Service updated successfully
)
echo.

REM Wait for service to stabilize
echo === Waiting for Service to Stabilize ===
echo This may take several minutes...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!

echo Service is stable
echo.

REM Verify deployment
echo === Deployment Verification ===
aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].[serviceName,status,runningCount,desiredCount]" --output table

echo.
echo ==========================================
echo DEPLOYMENT SUCCESSFUL!
echo ==========================================
echo.
echo Service Details:
echo   Cluster: !CLUSTER_NAME!
echo   Service: !SERVICE_NAME!
echo   Task Definition: !TASK_DEF_ARN!
echo   Region: !AWS_REGION!
echo.

if "!USE_LOAD_BALANCER!"=="true" (
    echo Load Balancer:
    echo   DNS Name: !ALB_DNS!
    echo   Access your application at: http://!ALB_DNS!
    echo.
)

echo CloudWatch Logs:
echo   Log Group: !LOG_GROUP!
echo   View logs: aws logs tail !LOG_GROUP! --follow --region !AWS_REGION!
echo.
echo ==========================================

REM Cleanup temp files
del /f /q !TASK_DEF_TEMP! >nul 2>&1
del /f /q !SERVICE_DEF_TEMP! >nul 2>&1

endlocal
