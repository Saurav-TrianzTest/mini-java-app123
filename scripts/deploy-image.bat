@echo off
setlocal enabledelayedexpansion

REM AWS ECS Fargate Deployment Script for mini-java-app
REM This script deploys a containerized Java application to AWS ECS Fargate

echo =====================================
echo AWS ECS Fargate Deployment Script
echo =====================================
echo.

REM Configuration
set PROJECT_NAME=mini-java-app
set TASK_FAMILY=!PROJECT_NAME!-task
set SERVICE_NAME=!PROJECT_NAME!-service

REM Prompt for AWS configuration
echo --- AWS Configuration ---
set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter ECS cluster name: "
echo.

REM Get AWS Account ID
echo Retrieving AWS Account ID...
for /f "delims=" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to retrieve AWS Account ID. Please check your AWS credentials.
    exit /b 1
)
echo AWS Account ID: !ACCOUNT_ID!
echo.

REM Check if cluster exists, create if not
echo Checking if ECS cluster exists...
aws ecs describe-clusters --clusters "!CLUSTER_NAME!" --region "!AWS_REGION!" --query "clusters[0].clusterName" --output text >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Cluster does not exist. Creating ECS cluster: !CLUSTER_NAME!
    aws ecs create-cluster --cluster-name "!CLUSTER_NAME!" --region "!AWS_REGION!"
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECS cluster.
        exit /b 1
    )
    echo ECS cluster created successfully
) else (
    echo ECS cluster already exists
)
echo.

REM Prompt for network configuration
echo --- Network Configuration ---
set /p VPC_ID="Enter VPC ID: "
set /p SUBNET_INPUT="Enter subnet IDs (comma-separated, at least 2): "
set /p SECURITY_GROUP="Enter security group ID: "
echo.

REM Parse subnets (simple parsing for first two)
for /f "tokens=1,2 delims=," %%a in ("!SUBNET_INPUT!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)
if "!SUBNET_2!"==" " set SUBNET_2=!SUBNET_1!

REM Trim whitespace
set SUBNET_1=!SUBNET_1: =!
set SUBNET_2=!SUBNET_2: =!
set SECURITY_GROUP=!SECURITY_GROUP: =!

REM Prompt for image URI
echo --- Container Image ---
set /p IMAGE_URI="Enter ECR image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): "
echo.

REM Prompt for environment variables
echo --- Database Configuration ---
set /p DB_HOST="Enter database host: "
set /p DB_PORT="Enter database port (default: 3306): "
if "!DB_PORT!"==" " set DB_PORT=3306
set /p DB_NAME="Enter database name: "
set /p DB_USERNAME="Enter database username: "
set /p DB_PASSWORD="Enter database password: "
echo.

echo --- Redis Configuration ---
set /p REDIS_HOST="Enter Redis host: "
set /p REDIS_PORT="Enter Redis port (default: 6379): "
if "!REDIS_PORT!"==" " set REDIS_PORT=6379
echo.

echo --- External API Configuration ---
set /p EXTERNAL_API_URL="Enter external API URL: "
echo.

echo --- Security Configuration ---
set /p JWT_SECRET="Enter JWT secret: "
set /p ENCRYPTION_KEY="Enter encryption key: "
echo.

REM Create CloudWatch log group
echo Creating CloudWatch log group...
set LOG_GROUP=/ecs/!PROJECT_NAME!
aws logs create-log-group --log-group-name "!LOG_GROUP!" --region "!AWS_REGION!" 2>nul
if !ERRORLEVEL! neq 0 (
    echo Log group already exists or error occurred
)
echo.

REM Ask about load balancer
echo --- Load Balancer Configuration ---
set /p NEED_LB="Do you need a load balancer for this service? (y/n): "
echo.

set TARGET_GROUP_ARN=
set ALB_DNS=

if /i "!NEED_LB!"=="y" (
    echo Creating Application Load Balancer and Target Group...
    
    REM Create target group with target-type ip
    set TG_NAME=!PROJECT_NAME!-tg
    for /f "delims=" %%i in ('aws elbv2 create-target-group --name "!TG_NAME!" --protocol HTTP --port 8080 --vpc-id "!VPC_ID!" --target-type ip --health-check-enabled --health-check-protocol HTTP --health-check-path "/actuator/health" --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region "!AWS_REGION!" --query "TargetGroups[0].TargetGroupArn" --output text 2^>nul') do set TARGET_GROUP_ARN=%%i
    
    if "!TARGET_GROUP_ARN!"==" " (
        REM Target group might already exist
        for /f "delims=" %%i in ('aws elbv2 describe-target-groups --names "!TG_NAME!" --region "!AWS_REGION!" --query "TargetGroups[0].TargetGroupArn" --output text 2^>nul') do set TARGET_GROUP_ARN=%%i
    )
    
    echo Target Group ARN: !TARGET_GROUP_ARN!
    
    REM Create ALB
    set ALB_NAME=!PROJECT_NAME!-alb
    for /f "delims=" %%i in ('aws elbv2 create-load-balancer --name "!ALB_NAME!" --subnets "!SUBNET_1!" "!SUBNET_2!" --security-groups "!SECURITY_GROUP!" --scheme internet-facing --type application --ip-address-type ipv4 --region "!AWS_REGION!" --query "LoadBalancers[0].LoadBalancerArn" --output text 2^>nul') do set ALB_ARN=%%i
    
    if "!ALB_ARN!"==" " (
        REM ALB might already exist
        for /f "delims=" %%i in ('aws elbv2 describe-load-balancers --names "!ALB_NAME!" --region "!AWS_REGION!" --query "LoadBalancers[0].LoadBalancerArn" --output text 2^>nul') do set ALB_ARN=%%i
    )
    
    echo Load Balancer ARN: !ALB_ARN!
    
    REM Get ALB DNS name
    for /f "delims=" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns "!ALB_ARN!" --region "!AWS_REGION!" --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%i
    
    REM Create listener
    aws elbv2 create-listener --load-balancer-arn "!ALB_ARN!" --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn="!TARGET_GROUP_ARN!" --region "!AWS_REGION!" 2>nul
    
    echo Load balancer created successfully
    echo.
) else (
    echo Skipping load balancer creation
    echo.
)

REM Replace placeholders in task definition
echo Preparing task definition...
copy ecs\task-definition.json ecs\task-definition-temp.json >nul

powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{IMAGE_URI}}','!IMAGE_URI!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{AWS_REGION}}','!AWS_REGION!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{ACCOUNT_ID}}','!ACCOUNT_ID!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_HOST}}','!DB_HOST!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_PORT}}','!DB_PORT!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_NAME}}','!DB_NAME!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_USERNAME}}','!DB_USERNAME!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_PASSWORD}}','!DB_PASSWORD!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{REDIS_HOST}}','!REDIS_HOST!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{REDIS_PORT}}','!REDIS_PORT!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{EXTERNAL_API_URL}}','!EXTERNAL_API_URL!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{JWT_SECRET}}','!JWT_SECRET!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{ENCRYPTION_KEY}}','!ENCRYPTION_KEY!' | Set-Content ecs\task-definition-temp.json"

echo Task definition prepared
echo.

REM Register task definition
echo Registering task definition...
for /f "delims=" %%i in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition-temp.json --region "!AWS_REGION!" --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to register task definition.
    del ecs\task-definition-temp.json
    exit /b 1
)

echo Task definition registered: !TASK_DEF_ARN!
del ecs\task-definition-temp.json
echo.

REM Prepare service definition
echo Preparing service definition...
copy ecs\service-definition.json ecs\service-definition-temp.json >nul

powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{CLUSTER_NAME}}','!CLUSTER_NAME!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_1}}','!SUBNET_1!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_2}}','!SUBNET_2!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SECURITY_GROUP}}','!SECURITY_GROUP!' | Set-Content ecs\service-definition-temp.json"

if "!TARGET_GROUP_ARN!"==" " (
    REM Remove loadBalancers section if no load balancer
    powershell -Command "$json = Get-Content ecs\service-definition-temp.json | ConvertFrom-Json; $json.PSObject.Properties.Remove('loadBalancers'); $json.PSObject.Properties.Remove('healthCheckGracePeriodSeconds'); $json | ConvertTo-Json -Depth 10 | Set-Content ecs\service-definition-temp.json"
) else (
    powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{TARGET_GROUP_ARN}}','!TARGET_GROUP_ARN!' | Set-Content ecs\service-definition-temp.json"
)

echo Service definition prepared
echo.

REM Check if service exists
echo Checking if service exists...
for /f "delims=" %%i in ('aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!" --query "services[0].serviceName" --output text 2^>nul') do set EXISTING_SERVICE=%%i

if "!EXISTING_SERVICE!"=="!SERVICE_NAME!" (
    echo Service exists. Updating service...
    aws ecs update-service --cluster "!CLUSTER_NAME!" --service "!SERVICE_NAME!" --task-definition "!TASK_DEF_ARN!" --region "!AWS_REGION!" --force-new-deployment
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to update service.
        del ecs\service-definition-temp.json
        exit /b 1
    )
    
    echo Service updated successfully
) else (
    echo Service does not exist. Creating service...
    aws ecs create-service --cli-input-json file://ecs/service-definition-temp.json --region "!AWS_REGION!"
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create service.
        del ecs\service-definition-temp.json
        exit /b 1
    )
    
    echo Service created successfully
)

del ecs\service-definition-temp.json
echo.

REM Wait for service stability
echo Waiting for service to become stable...
echo This may take several minutes...
aws ecs wait services-stable --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!"

if !ERRORLEVEL! equ 0 (
    echo Service is stable
) else (
    echo WARNING: Service stability check timed out or failed
)
echo.

REM Display service information
echo =====================================
echo Deployment Summary
echo =====================================
echo Cluster: !CLUSTER_NAME!
echo Service: !SERVICE_NAME!
echo Task Definition: !TASK_DEF_ARN!
echo Region: !AWS_REGION!
if not "!ALB_DNS!"==" " (
    echo Load Balancer DNS: http://!ALB_DNS!
)
echo CloudWatch Logs: !LOG_GROUP!
echo.

REM Get running tasks
for /f "delims=" %%i in ('aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "!SERVICE_NAME!" --region "!AWS_REGION!" --query "services[0].runningCount" --output text') do set RUNNING_COUNT=%%i

echo Running tasks: !RUNNING_COUNT!
echo.

echo =====================================
echo Deployment completed successfully!
echo =====================================
echo.
echo View logs with:
echo aws logs tail !LOG_GROUP! --follow --region !AWS_REGION!
echo.

pause
