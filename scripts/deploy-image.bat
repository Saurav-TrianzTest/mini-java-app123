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
set SERVICE_NAME=mini-java-app-service
set TASK_FAMILY=mini-java-app-task

echo Project: %PROJECT_NAME%
echo.

REM Prompt for AWS region
set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
set AWS_DEFAULT_REGION=!AWS_REGION!

echo Using AWS region: !AWS_REGION!
echo.

REM Get AWS Account ID
echo Retrieving AWS Account ID...
for /f "delims=" %%a in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%a
echo AWS Account ID: !ACCOUNT_ID!
echo.

REM Prompt for ECS cluster name
set /p CLUSTER_NAME="Enter ECS cluster name (will be created if doesn't exist): "

echo.
echo Checking if ECS cluster exists...
aws ecs describe-clusters --clusters "!CLUSTER_NAME!" --region "!AWS_REGION!" >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Cluster does not exist. Creating ECS cluster: !CLUSTER_NAME!
    aws ecs create-cluster --cluster-name "!CLUSTER_NAME!" --region "!AWS_REGION!"
    echo ECS cluster created successfully
)
echo ECS cluster ready: !CLUSTER_NAME!
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

echo.
echo VPC: !VPC_ID!
echo Subnet 1: !SUBNET_1!
echo Subnet 2: !SUBNET_2!
echo Security Group: !SECURITY_GROUP!
echo.

REM Prompt for Docker image URI
set /p IMAGE_URI="Enter Docker image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): "

echo.
echo Image URI: !IMAGE_URI!
echo.

REM Load balancer configuration
set /p NEED_LB="Do you need a load balancer for this service? (y/n): "

if /i "!NEED_LB!"=="y" (
    echo.
    echo === Creating Application Load Balancer ===
    
    REM Create ALB
    set ALB_NAME=mini-java-app-alb
    echo Creating Application Load Balancer: !ALB_NAME!
    
    for /f "delims=" %%a in ('aws elbv2 create-load-balancer --name "!ALB_NAME!" --subnets "!SUBNET_1!" "!SUBNET_2!" --security-groups "!SECURITY_GROUP!" --scheme internet-facing --type application --ip-address-type ipv4 --region "!AWS_REGION!" --query "LoadBalancers[0].LoadBalancerArn" --output text 2^>nul') do set ALB_ARN=%%a
    
    if "!ALB_ARN!"=="" (
        echo Load balancer may already exist, retrieving ARN...
        for /f "delims=" %%a in ('aws elbv2 describe-load-balancers --names "!ALB_NAME!" --region "!AWS_REGION!" --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%a
    )
    
    echo Load Balancer ARN: !ALB_ARN!
    
    REM Get ALB DNS name
    for /f "delims=" %%a in ('aws elbv2 describe-load-balancers --load-balancer-arns "!ALB_ARN!" --region "!AWS_REGION!" --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%a
    
    echo Load Balancer DNS: !ALB_DNS!
    echo.
    
    REM Create Target Group
    set TG_NAME=mini-java-app-tg
    echo Creating Target Group: !TG_NAME!
    
    for /f "delims=" %%a in ('aws elbv2 create-target-group --name "!TG_NAME!" --protocol HTTP --port 8080 --vpc-id "!VPC_ID!" --target-type ip --health-check-enabled --health-check-protocol HTTP --health-check-path "/mini-app" --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region "!AWS_REGION!" --query "TargetGroups[0].TargetGroupArn" --output text 2^>nul') do set TARGET_GROUP_ARN=%%a
    
    if "!TARGET_GROUP_ARN!"=="" (
        echo Target group may already exist, retrieving ARN...
        for /f "delims=" %%a in ('aws elbv2 describe-target-groups --names "!TG_NAME!" --region "!AWS_REGION!" --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%a
    )
    
    echo Target Group ARN: !TARGET_GROUP_ARN!
    echo.
    
    REM Create Listener
    echo Creating ALB Listener...
    aws elbv2 create-listener --load-balancer-arn "!ALB_ARN!" --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn="!TARGET_GROUP_ARN!" --region "!AWS_REGION!" >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Listener may already exist
    )
    
    echo ALB Listener created
    echo.
    
    set USE_LOAD_BALANCER=true
) else (
    echo Skipping load balancer creation
    set USE_LOAD_BALANCER=false
    set TARGET_GROUP_ARN=
)

REM Create CloudWatch Log Group
echo === Creating CloudWatch Log Group ===
set LOG_GROUP=/ecs/%PROJECT_NAME%
aws logs create-log-group --log-group-name "!LOG_GROUP!" --region "!AWS_REGION!" 2>nul
if !ERRORLEVEL! neq 0 (
    echo Log group already exists
)
echo CloudWatch Log Group: !LOG_GROUP!
echo.

REM Prepare task definition
echo === Preparing Task Definition ===
set TASK_DEF_FILE=..\ecs\task-definition.json

REM Create temporary task definition with replaced placeholders
set TEMP_TASK_DEF=%TEMP%\task-def-%RANDOM%.json
powershell -Command "(Get-Content '%TASK_DEF_FILE%') -replace '{{IMAGE_URI}}','!IMAGE_URI!' -replace '{{AWS_REGION}}','!AWS_REGION!' -replace '{{ACCOUNT_ID}}','!ACCOUNT_ID!' | Set-Content '!TEMP_TASK_DEF!'"

echo Task definition prepared
echo.

REM Register task definition
echo === Registering Task Definition ===
for /f "delims=" %%a in ('aws ecs register-task-definition --cli-input-json file://!TEMP_TASK_DEF! --region "!AWS_REGION!" --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%a

echo Task Definition registered: !TASK_DEF_ARN!
echo.

REM Clean up temporary file
del /f /q "!TEMP_TASK_DEF!" 2>nul

REM Prepare service definition
echo === Preparing Service Definition ===
set SERVICE_DEF_FILE=..\ecs\service-definition.json

REM Create temporary service definition with replaced placeholders
set TEMP_SERVICE_DEF=%TEMP%\service-def-%RANDOM%.json
powershell -Command "(Get-Content '%SERVICE_DEF_FILE%') -replace '{{CLUSTER_NAME}}','!CLUSTER_NAME!' -replace '{{SUBNET_1}}','!SUBNET_1!' -replace '{{SUBNET_2}}','!SUBNET_2!' -replace '{{SECURITY_GROUP}}','!SECURITY_GROUP!' -replace '{{TARGET_GROUP_ARN}}','!TARGET_GROUP_ARN!' | Set-Content '!TEMP_SERVICE_DEF!'"

REM Remove loadBalancers section if not using load balancer
if "!USE_LOAD_BALANCER!"=="false" (
    powershell -Command "$json = Get-Content '!TEMP_SERVICE_DEF!' | ConvertFrom-Json; $json.PSObject.Properties.Remove('loadBalancers'); $json.PSObject.Properties.Remove('healthCheckGracePeriodSeconds'); $json | ConvertTo-Json -Depth 10 | Set-Content '!TEMP_SERVICE_DEF!'"
)

echo Service definition prepared
echo.

REM Check if service exists
echo === Checking if Service Exists ===
for /f "delims=" %%a in ('aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!" --query "services[?status==`ACTIVE`].serviceName" --output text 2^>nul') do set EXISTING_SERVICE=%%a

if "!EXISTING_SERVICE!"=="" (
    echo Service does not exist. Creating new service...
    
    REM Create service
    aws ecs create-service --cli-input-json file://!TEMP_SERVICE_DEF! --region "!AWS_REGION!"
    
    echo Service created: !SERVICE_NAME!
) else (
    echo Service exists. Updating service...
    
    REM Update service with new task definition
    aws ecs update-service --cluster "!CLUSTER_NAME!" --service "!SERVICE_NAME!" --task-definition "!TASK_DEF_ARN!" --force-new-deployment --region "!AWS_REGION!"
    
    echo Service updated: !SERVICE_NAME!
)

echo.

REM Clean up temporary file
del /f /q "!TEMP_SERVICE_DEF!" 2>nul

REM Wait for service to stabilize
echo === Waiting for Service to Stabilize ===
echo This may take several minutes...
aws ecs wait services-stable --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!"

echo Service is stable
echo.

REM Verify deployment
echo === Deployment Verification ===
aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!" --query "services[0].{ServiceName:serviceName,Status:status,DesiredCount:desiredCount,RunningCount:runningCount,TaskDefinition:taskDefinition}" --output table

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
    echo Application URL:
    echo   http://!ALB_DNS!/mini-app
    echo.
)

echo CloudWatch Logs:
echo   Log Group: !LOG_GROUP!
echo   View logs: aws logs tail !LOG_GROUP! --follow --region !AWS_REGION!
echo.
echo To view running tasks:
echo   aws ecs list-tasks --cluster !CLUSTER_NAME! --service-name !SERVICE_NAME! --region !AWS_REGION!
echo.

endlocal
