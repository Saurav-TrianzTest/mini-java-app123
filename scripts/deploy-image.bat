@echo off
setlocal enabledelayedexpansion

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

REM Prompt for AWS region
set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
if "!AWS_REGION!"=="" (
    echo Error: AWS region is required
    exit /b 1
)

echo Using AWS region: !AWS_REGION!
echo.

REM Get AWS Account ID
echo Retrieving AWS Account ID...
for /f "delims=" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
if "!ACCOUNT_ID!"=="" (
    echo Error: Failed to retrieve AWS Account ID
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
aws ecs describe-clusters --clusters "!CLUSTER_NAME!" --region "!AWS_REGION!" >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Cluster does not exist. Creating ECS cluster: !CLUSTER_NAME!
    aws ecs create-cluster --cluster-name "!CLUSTER_NAME!" --region "!AWS_REGION!"
    if !ERRORLEVEL! neq 0 (
        echo Error: Failed to create ECS cluster
        exit /b 1
    )
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

set /p SUBNETS_INPUT="Enter Subnet IDs (comma-separated, at least 2): "
if "!SUBNETS_INPUT!"=="" (
    echo Error: At least 2 subnet IDs are required
    exit /b 1
)

REM Parse subnets
for /f "tokens=1,2 delims=," %%a in ("!SUBNETS_INPUT!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)

REM Trim spaces
set SUBNET_1=!SUBNET_1: =!
set SUBNET_2=!SUBNET_2: =!

if "!SUBNET_1!"=="" (
    echo Error: At least 2 valid subnet IDs are required
    exit /b 1
)
if "!SUBNET_2!"=="" (
    echo Error: At least 2 valid subnet IDs are required
    exit /b 1
)

set /p SECURITY_GROUP="Enter Security Group ID: "
if "!SECURITY_GROUP!"=="" (
    echo Error: Security Group ID is required
    exit /b 1
)

echo.
echo Network Configuration:
echo   VPC: !VPC_ID!
echo   Subnet 1: !SUBNET_1!
echo   Subnet 2: !SUBNET_2!
echo   Security Group: !SECURITY_GROUP!
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
    
    set ALB_NAME=mini-java-app-alb
    echo Creating Application Load Balancer: !ALB_NAME!
    
    for /f "delims=" %%i in ('aws elbv2 create-load-balancer --name "!ALB_NAME!" --subnets "!SUBNET_1!" "!SUBNET_2!" --security-groups "!SECURITY_GROUP!" --scheme internet-facing --type application --ip-address-type ipv4 --region "!AWS_REGION!" --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%i
    
    if "!ALB_ARN!"=="" (
        echo Error: Failed to create Application Load Balancer
        exit /b 1
    )
    
    echo ALB created: !ALB_ARN!
    
    REM Get ALB DNS name
    for /f "delims=" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns "!ALB_ARN!" --region "!AWS_REGION!" --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%i
    
    echo ALB DNS: !ALB_DNS!
    
    REM Create Target Group with target-type ip
    set TG_NAME=mini-java-app-tg
    echo Creating Target Group: !TG_NAME!
    
    for /f "delims=" %%i in ('aws elbv2 create-target-group --name "!TG_NAME!" --protocol HTTP --port 8080 --vpc-id "!VPC_ID!" --target-type ip --health-check-enabled --health-check-protocol HTTP --health-check-path "/mini-app" --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region "!AWS_REGION!" --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%i
    
    if "!TARGET_GROUP_ARN!"=="" (
        echo Error: Failed to create Target Group
        exit /b 1
    )
    
    echo Target Group created: !TARGET_GROUP_ARN!
    
    REM Create Listener
    echo Creating ALB Listener...
    aws elbv2 create-listener --load-balancer-arn "!ALB_ARN!" --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn="!TARGET_GROUP_ARN!" --region "!AWS_REGION!" >nul
    
    echo ALB Listener created
    echo.
) else (
    echo Skipping load balancer creation
    set TARGET_GROUP_ARN=
    echo.
)

REM Create CloudWatch log group
echo Creating CloudWatch log group...
aws logs create-log-group --log-group-name "/ecs/%PROJECT_NAME%" --region "!AWS_REGION!" 2>nul
if !ERRORLEVEL! neq 0 (
    echo Log group already exists
)
echo.

REM Replace placeholders in task definition
echo Preparing task definition...
copy ecs\task-definition.json ecs\task-definition-temp.json >nul

powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content ecs\task-definition-temp.json"

echo Task definition prepared
echo.

REM Register task definition
echo Registering ECS task definition...
for /f "delims=" %%i in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition-temp.json --region "!AWS_REGION!" --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i

if "!TASK_DEF_ARN!"=="" (
    echo Error: Failed to register task definition
    del ecs\task-definition-temp.json
    exit /b 1
)

echo Task definition registered: !TASK_DEF_ARN!
echo.

REM Prepare service definition
echo Preparing service definition...
copy ecs\service-definition.json ecs\service-definition-temp.json >nul

powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content ecs\service-definition-temp.json"

if not "!TARGET_GROUP_ARN!"=="" (
    powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{TARGET_GROUP_ARN}}', '!TARGET_GROUP_ARN!' | Set-Content ecs\service-definition-temp.json"
) else (
    REM Remove loadBalancers section if no load balancer
    powershell -Command "$content = Get-Content ecs\service-definition-temp.json -Raw; $content = $content -replace '(?s)\"loadBalancers\":\s*\[.*?\],\s*', ''; $content = $content -replace '\"healthCheckGracePeriodSeconds\":\s*\d+,\s*', ''; $content | Set-Content ecs\service-definition-temp.json"
)

echo Service definition prepared
echo.

REM Check if service exists
echo Checking if ECS service exists...
for /f "delims=" %%i in ('aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!" --query "services[?status==`ACTIVE`].serviceName" --output text') do set EXISTING_SERVICE=%%i

if "!EXISTING_SERVICE!"=="" (
    echo Service does not exist. Creating new service...
    
    aws ecs create-service --cli-input-json file://ecs/service-definition-temp.json --region "!AWS_REGION!" >nul
    
    if !ERRORLEVEL! neq 0 (
        echo Error: Failed to create ECS service
        del ecs\task-definition-temp.json ecs\service-definition-temp.json
        exit /b 1
    )
    
    echo ECS service created: !SERVICE_NAME!
) else (
    echo Service exists. Updating service...
    
    aws ecs update-service --cluster "!CLUSTER_NAME!" --service "!SERVICE_NAME!" --task-definition "!TASK_DEF_ARN!" --desired-count 2 --region "!AWS_REGION!" >nul
    
    if !ERRORLEVEL! neq 0 (
        echo Error: Failed to update ECS service
        del ecs\task-definition-temp.json ecs\service-definition-temp.json
        exit /b 1
    )
    
    echo ECS service updated: !SERVICE_NAME!
)

echo.

REM Clean up temporary files
del ecs\task-definition-temp.json ecs\service-definition-temp.json

REM Wait for service to stabilize
echo Waiting for service to stabilize (this may take a few minutes)...
aws ecs wait services-stable --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!"

echo Service is stable
echo.

REM Verify deployment
echo ==========================================
echo Deployment Verification
echo ==========================================

for /f "delims=" %%i in ('aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!" --query "services[0].[runningCount,desiredCount,status]" --output text') do set SERVICE_INFO=%%i

echo Service Status: !SERVICE_INFO!
echo.

if not "!ALB_DNS!"=="" (
    echo Application URL: http://!ALB_DNS!/mini-app
    echo.
)

echo CloudWatch Logs: /ecs/%PROJECT_NAME%
echo Region: !AWS_REGION!
echo.

echo ==========================================
echo Deployment Completed Successfully
echo ==========================================
echo.
echo To view logs:
echo   aws logs tail /ecs/%PROJECT_NAME% --follow --region !AWS_REGION!
echo.
echo To check service status:
echo   aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!
echo.

endlocal
