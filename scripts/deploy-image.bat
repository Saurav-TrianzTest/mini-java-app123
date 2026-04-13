@echo off
setlocal enabledelayedexpansion

REM ECS Fargate Deployment Script for Mini Java Application (Windows)
REM This script deploys the containerized application to AWS ECS Fargate

echo ========================================
echo Mini Java App - ECS Fargate Deployment
echo ========================================
echo.

REM Get AWS Account ID
echo Retrieving AWS Account ID...
for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
echo Account ID: !ACCOUNT_ID!
echo.

REM Prompt for AWS Region
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
set AWS_DEFAULT_REGION=!AWS_REGION!

REM Prompt for ECS Cluster Name
set /p CLUSTER_NAME="Enter ECS Cluster Name: "

REM Check if cluster exists, create if not
echo.
echo Checking if ECS cluster exists...
for /f "tokens=*" %%i in ('aws ecs describe-clusters --clusters !CLUSTER_NAME! --query "clusters[0].clusterName" --output text 2^>nul') do set CLUSTER_EXISTS=%%i

if "!CLUSTER_EXISTS!"=="None" (
    echo Cluster does not exist. Creating...
    aws ecs create-cluster --cluster-name !CLUSTER_NAME!
    echo Cluster created successfully!
) else if "!CLUSTER_EXISTS!"=="" (
    echo Cluster does not exist. Creating...
    aws ecs create-cluster --cluster-name !CLUSTER_NAME!
    echo Cluster created successfully!
) else (
    echo Cluster exists: !CLUSTER_NAME!
)

REM Prompt for Network Configuration
echo.
echo Network Configuration (Required for Fargate)
set /p VPC_ID="Enter VPC ID: "
set /p SUBNETS_INPUT="Enter Subnet IDs (comma-separated, at least 2): "
set /p SECURITY_GROUP="Enter Security Group ID: "

REM Parse subnets
for /f "tokens=1,2 delims=," %%a in ("!SUBNETS_INPUT!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)
set SUBNET_1=!SUBNET_1: =!
set SUBNET_2=!SUBNET_2: =!

REM Prompt for Image URI
echo.
set /p IMAGE_URI="Enter ECR Image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): "

REM Prompt for Load Balancer
echo.
set /p NEED_LB="Do you need a load balancer for this service? (y/n): "

if /i "!NEED_LB!"=="y" (
    echo.
    echo Creating Application Load Balancer and Target Group...
    
    REM Create Target Group with target-type ip
    for /f "tokens=*" %%i in ('powershell -Command "Get-Date -Format 'yyyyMMddHHmmss'"') do set TIMESTAMP=%%i
    set TG_NAME=mini-java-app-tg-!TIMESTAMP!
    echo Creating Target Group: !TG_NAME!
    
    for /f "tokens=*" %%i in ('aws elbv2 create-target-group --name !TG_NAME! --protocol HTTP --port 8080 --vpc-id !VPC_ID! --target-type ip --health-check-enabled --health-check-protocol HTTP --health-check-path "/mini-app" --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%i
    
    echo Target Group created: !TARGET_GROUP_ARN!
    
    REM Create Application Load Balancer
    set ALB_NAME=mini-java-app-alb-!TIMESTAMP!
    echo Creating Application Load Balancer: !ALB_NAME!
    
    for /f "tokens=*" %%i in ('aws elbv2 create-load-balancer --name !ALB_NAME! --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --scheme internet-facing --type application --ip-address-type ipv4 --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%i
    
    echo Load Balancer created: !ALB_ARN!
    
    REM Get ALB DNS Name
    for /f "tokens=*" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%i
    
    REM Create Listener
    echo Creating Listener...
    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn=!TARGET_GROUP_ARN!
    
    echo Listener created successfully!
    
    REM Update service definition with load balancer configuration
    powershell -Command "(Get-Content ecs\service-definition.json) -replace '{{TARGET_GROUP_ARN}}', '!TARGET_GROUP_ARN!' | Set-Content ecs\service-definition.json"
) else (
    REM Remove load balancer section from service definition
    echo Removing load balancer configuration from service definition...
    powershell -Command "$json = Get-Content ecs\service-definition.json | ConvertFrom-Json; $json.PSObject.Properties.Remove('loadBalancers'); $json.PSObject.Properties.Remove('healthCheckGracePeriodSeconds'); $json | ConvertTo-Json -Depth 10 | Set-Content ecs\service-definition.json"
)

REM Application Configuration
echo.
echo Application Configuration
echo Using default values from application.properties
echo You can customize these in the task definition JSON if needed

set DB_HOST=db.example.com
set DB_PORT=3306
set DB_NAME=mini_app_db
set DB_USERNAME=appuser
set DB_PASSWORD=changeme
set REDIS_HOST=redis.example.com
set REDIS_PORT=6379
set REDIS_PASSWORD=
set EXTERNAL_API_URL=http://api.example.com:8080/v1
set EXTERNAL_API_KEY=
set PAYMENT_SERVICE_URL=https://payment.service.local/process
set PAYMENT_SERVICE_USERNAME=
set PAYMENT_SERVICE_PASSWORD=
set CONFIG_S3_BUCKET=app-config-bucket
set CONFIG_S3_KEY=config/app.properties
set JWT_SECRET=
set ADMIN_USERNAME=
set ADMIN_PASSWORD=
set ENCRYPTION_KEY=
set MONITORING_ENDPOINT=http://monitoring.service.local:9090/metrics
set MONITORING_USERNAME=
set MONITORING_PASSWORD=
set RABBITMQ_HOST=rabbitmq.service.local
set RABBITMQ_PORT=5672
set RABBITMQ_USERNAME=
set RABBITMQ_PASSWORD=

REM Replace placeholders in task definition
echo.
echo Preparing task definition...
copy ecs\task-definition.json ecs\task-definition-temp.json >nul

powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_USERNAME}}', '!DB_USERNAME!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{REDIS_PASSWORD}}', '!REDIS_PASSWORD!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{EXTERNAL_API_URL}}', '!EXTERNAL_API_URL!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{EXTERNAL_API_KEY}}', '!EXTERNAL_API_KEY!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{PAYMENT_SERVICE_URL}}', '!PAYMENT_SERVICE_URL!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{PAYMENT_SERVICE_USERNAME}}', '!PAYMENT_SERVICE_USERNAME!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{PAYMENT_SERVICE_PASSWORD}}', '!PAYMENT_SERVICE_PASSWORD!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{CONFIG_S3_BUCKET}}', '!CONFIG_S3_BUCKET!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{CONFIG_S3_KEY}}', '!CONFIG_S3_KEY!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{JWT_SECRET}}', '!JWT_SECRET!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{ADMIN_USERNAME}}', '!ADMIN_USERNAME!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{ADMIN_PASSWORD}}', '!ADMIN_PASSWORD!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{ENCRYPTION_KEY}}', '!ENCRYPTION_KEY!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{MONITORING_ENDPOINT}}', '!MONITORING_ENDPOINT!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{MONITORING_USERNAME}}', '!MONITORING_USERNAME!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{MONITORING_PASSWORD}}', '!MONITORING_PASSWORD!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{RABBITMQ_HOST}}', '!RABBITMQ_HOST!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{RABBITMQ_PORT}}', '!RABBITMQ_PORT!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{RABBITMQ_USERNAME}}', '!RABBITMQ_USERNAME!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{RABBITMQ_PASSWORD}}', '!RABBITMQ_PASSWORD!' | Set-Content ecs\task-definition-temp.json"

REM Create CloudWatch Log Group
echo.
echo Creating CloudWatch Log Group...
aws logs create-log-group --log-group-name "/ecs/mini-java-app" 2>nul || echo Log group already exists

REM Register task definition
echo.
echo Registering ECS task definition...
for /f "tokens=*" %%i in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition-temp.json --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i

echo Task definition registered: !TASK_DEF_ARN!

REM Clean up temp file
del ecs\task-definition-temp.json

REM Replace placeholders in service definition
echo.
echo Preparing service definition...
copy ecs\service-definition.json ecs\service-definition-temp.json >nul

powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content ecs\service-definition-temp.json"

REM Check if service exists
set SERVICE_NAME=mini-java-app-service
echo.
echo Checking if service exists...
for /f "tokens=*" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --query "services[?serviceName=='!SERVICE_NAME!'].serviceName" --output text 2^>nul') do set SERVICE_EXISTS=%%i

if "!SERVICE_EXISTS!"=="" (
    REM Create new service
    echo Service does not exist. Creating new service...
    aws ecs create-service --cli-input-json file://ecs/service-definition-temp.json --cluster !CLUSTER_NAME!
    echo Service created successfully!
) else (
    REM Update existing service
    echo Service exists. Updating service...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --force-new-deployment
    echo Service updated successfully!
)

REM Clean up temp file
del ecs\service-definition-temp.json

REM Wait for service to stabilize
echo.
echo Waiting for service to stabilize...
echo This may take several minutes...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME!

REM Verify deployment
echo.
echo Verifying deployment...
aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --query "services[0].[serviceName,status,runningCount,desiredCount]" --output table

echo.
echo ========================================
echo Deployment Completed Successfully!
echo ========================================
echo.
echo Deployment Details:
echo Cluster: !CLUSTER_NAME!
echo Service: !SERVICE_NAME!
echo Task Definition: !TASK_DEF_ARN!
echo Region: !AWS_REGION!
echo.

if /i "!NEED_LB!"=="y" (
    echo Load Balancer Details:
    echo DNS Name: !ALB_DNS!
    echo Access your application at: http://!ALB_DNS!/mini-app
    echo.
)

echo CloudWatch Logs:
echo Log Group: /ecs/mini-java-app
echo View logs: aws logs tail /ecs/mini-java-app --follow
echo.
echo Useful Commands:
echo View service: aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME!
echo List tasks: aws ecs list-tasks --cluster !CLUSTER_NAME! --service-name !SERVICE_NAME!
echo Scale service: aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --desired-count ^<count^>
echo.

endlocal
