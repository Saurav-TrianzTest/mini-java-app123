@echo off
setlocal enabledelayedexpansion

REM ECS Fargate Deployment Script for mini-java-app (Windows)
REM This script deploys the Docker image to AWS ECS Fargate

echo ==========================================
echo AWS ECS Fargate Deployment Script
echo ==========================================
echo.

REM Prompt for AWS configuration
echo === AWS Configuration ===
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter ECS Cluster Name: "
set /p IMAGE_URI="Enter Docker Image URI (from ECR or Docker Hub): "

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

REM Trim whitespace
set SUBNET_1=!SUBNET_1: =!
set SUBNET_2=!SUBNET_2: =!

echo.
echo === Load Balancer Configuration ===
set /p NEED_LB="Do you need a load balancer for this service? (y/n): "

REM Get AWS Account ID
echo.
echo === Getting AWS Account ID ===
for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
echo Account ID: !ACCOUNT_ID!

REM Check/Create ECS Cluster
echo.
echo === Checking ECS Cluster ===
aws ecs describe-clusters --clusters "!CLUSTER_NAME!" --region "!AWS_REGION!" >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Creating ECS cluster: !CLUSTER_NAME!
    aws ecs create-cluster --cluster-name "!CLUSTER_NAME!" --region "!AWS_REGION!"
)
echo ECS cluster ready: !CLUSTER_NAME!

REM Create CloudWatch Log Group
echo.
echo === Creating CloudWatch Log Group ===
aws logs create-log-group --log-group-name "/ecs/mini-java-app" --region "!AWS_REGION!" 2>nul
if !ERRORLEVEL! neq 0 (
    echo Log group already exists
)

REM Prepare task definition
echo.
echo === Preparing Task Definition ===
copy ecs\task-definition.json ecs\task-definition-temp.json >nul

REM Replace placeholders using PowerShell
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content ecs\task-definition-temp.json"

REM Register task definition
echo.
echo === Registering Task Definition ===
for /f "tokens=*" %%i in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition-temp.json --region "!AWS_REGION!" --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i
echo Task definition registered: !TASK_DEF_ARN!

REM Prepare service definition
echo.
echo === Preparing Service Definition ===
copy ecs\service-definition.json ecs\service-definition-temp.json >nul

REM Replace placeholders
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content ecs\service-definition-temp.json"

REM Handle load balancer
if /i "!NEED_LB!"=="y" (
    echo.
    echo === Creating Application Load Balancer ===
    
    REM Create ALB
    for /f "tokens=*" %%i in ('aws elbv2 create-load-balancer --name mini-java-app-alb --subnets "!SUBNET_1!" "!SUBNET_2!" --security-groups "!SECURITY_GROUP!" --region "!AWS_REGION!" --query "LoadBalancers[0].LoadBalancerArn" --output text 2^>nul') do set ALB_ARN=%%i
    
    if "!ALB_ARN!"=="" (
        echo Load balancer may already exist, retrieving ARN...
        for /f "tokens=*" %%i in ('aws elbv2 describe-load-balancers --names mini-java-app-alb --region "!AWS_REGION!" --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%i
    )
    
    echo Load Balancer ARN: !ALB_ARN!
    
    REM Create Target Group
    echo Creating Target Group...
    for /f "tokens=*" %%i in ('aws elbv2 create-target-group --name mini-java-app-tg --protocol HTTP --port 8080 --vpc-id "!VPC_ID!" --target-type ip --health-check-path "/mini-app" --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region "!AWS_REGION!" --query "TargetGroups[0].TargetGroupArn" --output text 2^>nul') do set TG_ARN=%%i
    
    if "!TG_ARN!"=="" (
        for /f "tokens=*" %%i in ('aws elbv2 describe-target-groups --names mini-java-app-tg --region "!AWS_REGION!" --query "TargetGroups[0].TargetGroupArn" --output text') do set TG_ARN=%%i
    )
    
    echo Target Group ARN: !TG_ARN!
    
    REM Create Listener
    echo Creating ALB Listener...
    aws elbv2 create-listener --load-balancer-arn "!ALB_ARN!" --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn="!TG_ARN!" --region "!AWS_REGION!" 2>nul
    
    REM Add load balancer configuration to service definition
    powershell -Command "$json = Get-Content ecs\service-definition-temp.json | ConvertFrom-Json; $json | Add-Member -NotePropertyName loadBalancers -NotePropertyValue @(@{targetGroupArn='!TG_ARN!';containerName='mini-java-app';containerPort=8080}) -Force; $json | Add-Member -NotePropertyName healthCheckGracePeriodSeconds -NotePropertyValue 300 -Force; $json | ConvertTo-Json -Depth 10 | Set-Content ecs\service-definition-temp.json"
    
    REM Get ALB DNS name
    for /f "tokens=*" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns "!ALB_ARN!" --region "!AWS_REGION!" --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%i
)

REM Check if service exists
echo.
echo === Checking Service Status ===
for /f "tokens=*" %%i in ('aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "mini-java-app-service" --region "!AWS_REGION!" --query "services[0].serviceName" --output text 2^>nul') do set SERVICE_EXISTS=%%i

if "!SERVICE_EXISTS!"=="mini-java-app-service" (
    echo Service exists, updating...
    aws ecs update-service --cluster "!CLUSTER_NAME!" --service "mini-java-app-service" --task-definition "!TASK_DEF_ARN!" --region "!AWS_REGION!" --force-new-deployment
) else (
    echo Creating new service...
    aws ecs create-service --cli-input-json file://ecs/service-definition-temp.json --region "!AWS_REGION!"
)

REM Wait for service stability
echo.
echo === Waiting for Service Stability ===
echo This may take a few minutes...
aws ecs wait services-stable --cluster "!CLUSTER_NAME!" --services "mini-java-app-service" --region "!AWS_REGION!"

REM Verify deployment
echo.
echo === Deployment Verification ===
aws ecs describe-services --cluster "!CLUSTER_NAME!" --services "mini-java-app-service" --region "!AWS_REGION!" --query "services[0].[serviceName,status,runningCount,desiredCount]" --output table

REM Cleanup temporary files
del /f /q ecs\task-definition-temp.json ecs\service-definition-temp.json 2>nul

echo.
echo ==========================================
echo Deployment completed successfully!
echo ==========================================
echo Cluster: !CLUSTER_NAME!
echo Service: mini-java-app-service
echo Task Definition: !TASK_DEF_ARN!
echo CloudWatch Logs: /ecs/mini-java-app
if /i "!NEED_LB!"=="y" (
    echo Load Balancer DNS: !ALB_DNS!
    echo Application URL: http://!ALB_DNS!/mini-app
)
echo.
echo To view logs:
echo aws logs tail /ecs/mini-java-app --follow --region !AWS_REGION!
echo.

endlocal
