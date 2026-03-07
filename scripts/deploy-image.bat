@echo off
setlocal enabledelayedexpansion

echo ============================================
echo Mini Java App - ECS Fargate Deployment
echo ============================================
echo.

REM Prompt for AWS configuration
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter ECS Cluster name: "
set /p IMAGE_URI="Enter ECR Image URI: "

echo.
echo Network Configuration
echo ---------------------
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
echo External Services Configuration
echo -------------------------------
set /p DB_HOST="Enter Database Host: "
set /p DB_PORT="Enter Database Port (default: 3306): "
if "!DB_PORT!"=="" set DB_PORT=3306
set /p DB_NAME="Enter Database Name: "
set /p DB_USER="Enter Database User: "
set /p DB_PASSWORD="Enter Database Password: "
set /p REDIS_HOST="Enter Redis Host: "
set /p RABBITMQ_HOST="Enter RabbitMQ Host: "

echo.
echo Getting AWS Account ID...
for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
echo Account ID: !ACCOUNT_ID!

echo.
echo Checking if ECS cluster exists...
aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Creating ECS cluster: !CLUSTER_NAME!
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
)

echo.
set /p NEED_LB="Do you need a load balancer for this service? (y/n): "

set TARGET_GROUP_ARN=
if /i "!NEED_LB!"=="y" (
    echo.
    echo Creating Application Load Balancer and Target Group...
    
    aws elbv2 create-target-group --name mini-java-app-tg --protocol HTTP --port 8080 --vpc-id !VPC_ID! --target-type ip --health-check-enabled --health-check-path "/actuator/health" --region !AWS_REGION! >nul 2>&1
    
    for /f "tokens=*" %%i in ('aws elbv2 describe-target-groups --names mini-java-app-tg --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%i
    
    echo Target Group ARN: !TARGET_GROUP_ARN!
)

echo.
echo Preparing task definition...
copy ecs\task-definition.json ecs\task-definition-deploy.json >nul

REM Replace placeholders using PowerShell
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{DB_USER}}', '!DB_USER!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{RABBITMQ_HOST}}', '!RABBITMQ_HOST!' | Set-Content ecs\task-definition-deploy.json"

echo Registering task definition...
for /f "tokens=*" %%i in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition-deploy.json --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i

echo Task Definition ARN: !TASK_DEF_ARN!

echo.
echo Checking if service exists...
for /f "tokens=*" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services mini-java-app-service --region !AWS_REGION! --query "services[?status=='ACTIVE'].serviceName" --output text') do set SERVICE_EXISTS=%%i

if "!SERVICE_EXISTS!"=="" (
    echo Creating new ECS service...
    
    copy ecs\service-definition.json ecs\service-definition-deploy.json >nul
    powershell -Command "(Get-Content ecs\service-definition-deploy.json) -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content ecs\service-definition-deploy.json"
    powershell -Command "(Get-Content ecs\service-definition-deploy.json) -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content ecs\service-definition-deploy.json"
    powershell -Command "(Get-Content ecs\service-definition-deploy.json) -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content ecs\service-definition-deploy.json"
    powershell -Command "(Get-Content ecs\service-definition-deploy.json) -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content ecs\service-definition-deploy.json"
    
    if not "!TARGET_GROUP_ARN!"=="" (
        powershell -Command "$json = Get-Content ecs\service-definition-deploy.json | ConvertFrom-Json; $lb = @{targetGroupArn='!TARGET_GROUP_ARN!'; containerName='mini-java-app'; containerPort=8080}; $json | Add-Member -NotePropertyName 'loadBalancers' -NotePropertyValue @($lb) -Force; $json | Add-Member -NotePropertyName 'healthCheckGracePeriodSeconds' -NotePropertyValue 300 -Force; $json | ConvertTo-Json -Depth 10 | Set-Content ecs\service-definition-deploy.json"
    )
    
    aws ecs create-service --cli-input-json file://ecs/service-definition-deploy.json --region !AWS_REGION!
) else (
    echo Updating existing ECS service...
    aws ecs update-service --cluster !CLUSTER_NAME! --service mini-java-app-service --task-definition !TASK_DEF_ARN! --force-new-deployment --region !AWS_REGION!
)

echo.
echo Waiting for service to become stable...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services mini-java-app-service --region !AWS_REGION!

echo.
echo Deployment verification...
aws ecs describe-services --cluster !CLUSTER_NAME! --services mini-java-app-service --region !AWS_REGION! --query "services[0].[serviceName,status,runningCount,desiredCount]" --output table

echo.
echo ============================================
echo SUCCESS: Deployment completed
echo Cluster: !CLUSTER_NAME!
echo Service: mini-java-app-service
echo Task Definition: !TASK_DEF_ARN!
echo CloudWatch Logs: /ecs/mini-java-app
echo ============================================

REM Cleanup
del /q ecs\task-definition-deploy.json ecs\service-definition-deploy.json 2>nul

endlocal