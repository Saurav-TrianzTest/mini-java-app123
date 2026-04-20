@echo off
setlocal enabledelayedexpansion

REM Deploy to GCP GKE Script for mini-java-app
REM This script deploys the containerized application to Google Kubernetes Engine

echo ==========================================
echo GKE Deployment Script for mini-java-app
echo ==========================================
echo.

REM Prompt for GCP configuration
echo === GCP Configuration ===
set /p GCP_PROJECT="Enter GCP Project ID: "
set /p GCP_ZONE="Enter GCP Zone (e.g., us-central1-a): "
set /p CLUSTER_NAME="Enter GKE Cluster Name: "

echo.
echo === Docker Image Configuration ===
set /p IMAGE_URI="Enter Docker Image URI (with tag): "

echo.
echo === Environment Variables Configuration ===
echo Configure external service connections (press Enter to skip optional values)
echo.

REM Database Configuration
set /p DATABASE_URL="Enter DATABASE_URL (e.g., jdbc:mysql://mysql-host:3306/db): "
if "!DATABASE_URL!"=="" set DATABASE_URL=jdbc:mysql://mysql-service:3306/mini_app_db

set /p DB_USERNAME="Enter DB_USERNAME: "
if "!DB_USERNAME!"=="" set DB_USERNAME=root

set /p DB_PASSWORD="Enter DB_PASSWORD: "

REM Redis Configuration
set /p REDIS_HOST="Enter REDIS_HOST: "
if "!REDIS_HOST!"=="" set REDIS_HOST=redis-service

set /p REDIS_PORT="Enter REDIS_PORT (default: 6379): "
if "!REDIS_PORT!"=="" set REDIS_PORT=6379

set /p REDIS_PASSWORD="Enter REDIS_PASSWORD (optional): "

REM External API Configuration
set /p EXTERNAL_API_URL="Enter EXTERNAL_API_URL (optional): "
if "!EXTERNAL_API_URL!"=="" set EXTERNAL_API_URL=http://api-service:8080/v1

set /p EXTERNAL_API_KEY="Enter EXTERNAL_API_KEY (optional): "

REM Payment Service Configuration
set /p PAYMENT_SERVICE_URL="Enter PAYMENT_SERVICE_URL (optional): "
if "!PAYMENT_SERVICE_URL!"=="" set PAYMENT_SERVICE_URL=http://payment-service/process

set /p PAYMENT_SERVICE_USERNAME="Enter PAYMENT_SERVICE_USERNAME (optional): "
set /p PAYMENT_SERVICE_PASSWORD="Enter PAYMENT_SERVICE_PASSWORD (optional): "

REM GCS Bucket Configuration
set /p GCS_CONFIG_BUCKET="Enter GCS_CONFIG_BUCKET: "
if "!GCS_CONFIG_BUCKET!"=="" set GCS_CONFIG_BUCKET=app-config-bucket

set /p GCS_LOG_BUCKET="Enter GCS_LOG_BUCKET: "
if "!GCS_LOG_BUCKET!"=="" set GCS_LOG_BUCKET=app-logs-bucket

set /p GCS_TEMP_BUCKET="Enter GCS_TEMP_BUCKET: "
if "!GCS_TEMP_BUCKET!"=="" set GCS_TEMP_BUCKET=app-temp-bucket

set /p GCS_UPLOAD_BUCKET="Enter GCS_UPLOAD_BUCKET: "
if "!GCS_UPLOAD_BUCKET!"=="" set GCS_UPLOAD_BUCKET=app-uploads-bucket

REM Security Configuration
set /p JWT_SECRET="Enter JWT_SECRET: "

set /p ADMIN_USERNAME="Enter ADMIN_USERNAME (default: admin): "
if "!ADMIN_USERNAME!"=="" set ADMIN_USERNAME=admin

set /p ADMIN_PASSWORD="Enter ADMIN_PASSWORD: "
set /p ENCRYPTION_KEY="Enter ENCRYPTION_KEY: "

REM Monitoring Configuration
set /p MONITORING_ENDPOINT="Enter MONITORING_ENDPOINT (optional): "
if "!MONITORING_ENDPOINT!"=="" set MONITORING_ENDPOINT=http://monitoring-service:9090/metrics

set /p MONITORING_USERNAME="Enter MONITORING_USERNAME (optional): "
set /p MONITORING_PASSWORD="Enter MONITORING_PASSWORD (optional): "

REM RabbitMQ Configuration
set /p RABBITMQ_HOST="Enter RABBITMQ_HOST (optional): "
if "!RABBITMQ_HOST!"=="" set RABBITMQ_HOST=rabbitmq-service

set /p RABBITMQ_PORT="Enter RABBITMQ_PORT (default: 5672): "
if "!RABBITMQ_PORT!"=="" set RABBITMQ_PORT=5672

set /p RABBITMQ_USERNAME="Enter RABBITMQ_USERNAME (optional): "
set /p RABBITMQ_PASSWORD="Enter RABBITMQ_PASSWORD (optional): "

echo.
echo ==========================================
echo Configuring kubectl for GKE...
echo ==========================================

REM Authenticate with GCP
gcloud config set project "!GCP_PROJECT!"

REM Get GKE credentials
gcloud container clusters get-credentials "!CLUSTER_NAME!" --zone "!GCP_ZONE!" --project "!GCP_PROJECT!"

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to get GKE credentials
    exit /b 1
)

REM Verify cluster connectivity
echo.
echo Verifying cluster connectivity...
kubectl cluster-info

if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to Kubernetes cluster
    exit /b 1
)

echo.
echo ==========================================
echo Updating Kubernetes Manifests...
echo ==========================================

REM Create temporary directory for modified manifests
set TEMP_DIR=%TEMP%\k8s-deploy-%RANDOM%
mkdir "!TEMP_DIR!"
xcopy /E /I /Q kubernetes "!TEMP_DIR!"

REM Replace placeholders in deployment.yaml using PowerShell
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DATABASE_URL}}', '!DATABASE_URL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_USERNAME}}', '!DB_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PASSWORD}}', '!REDIS_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{EXTERNAL_API_URL}}', '!EXTERNAL_API_URL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{EXTERNAL_API_KEY}}', '!EXTERNAL_API_KEY!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{PAYMENT_SERVICE_URL}}', '!PAYMENT_SERVICE_URL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{PAYMENT_SERVICE_USERNAME}}', '!PAYMENT_SERVICE_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{PAYMENT_SERVICE_PASSWORD}}', '!PAYMENT_SERVICE_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{GCS_CONFIG_BUCKET}}', '!GCS_CONFIG_BUCKET!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{GCS_LOG_BUCKET}}', '!GCS_LOG_BUCKET!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{GCS_TEMP_BUCKET}}', '!GCS_TEMP_BUCKET!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{GCS_UPLOAD_BUCKET}}', '!GCS_UPLOAD_BUCKET!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{JWT_SECRET}}', '!JWT_SECRET!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{ADMIN_USERNAME}}', '!ADMIN_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{ADMIN_PASSWORD}}', '!ADMIN_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{ENCRYPTION_KEY}}', '!ENCRYPTION_KEY!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{MONITORING_ENDPOINT}}', '!MONITORING_ENDPOINT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{MONITORING_USERNAME}}', '!MONITORING_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{MONITORING_PASSWORD}}', '!MONITORING_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_HOST}}', '!RABBITMQ_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_PORT}}', '!RABBITMQ_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_USERNAME}}', '!RABBITMQ_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_PASSWORD}}', '!RABBITMQ_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

echo Success: Manifests updated successfully

echo.
echo ==========================================
echo Deploying to GKE...
echo ==========================================

REM Apply namespace
echo Creating namespace...
kubectl apply -f "!TEMP_DIR!\namespace.yaml"

REM Apply deployment
echo Deploying application...
kubectl apply -f "!TEMP_DIR!\deployment.yaml"

REM Apply service
echo Creating service...
kubectl apply -f "!TEMP_DIR!\service.yaml"

REM Apply ingress
echo Creating ingress...
kubectl apply -f "!TEMP_DIR!\ingress.yaml"

echo.
echo ==========================================
echo Waiting for Deployment Rollout...
echo ==========================================

kubectl rollout status deployment/mini-java-app -n mini-java-app --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo WARNING: Deployment rollout did not complete within timeout
    echo Check deployment status with: kubectl get pods -n mini-java-app
)

echo.
echo ==========================================
echo Deployment Status
echo ==========================================

kubectl get pods,svc,ingress -n mini-java-app

echo.
echo ==========================================
echo SUCCESS - DEPLOYMENT COMPLETE!
echo ==========================================
echo.
echo Application deployed to namespace: mini-java-app
echo.
echo To check application logs:
echo   kubectl logs -f deployment/mini-java-app -n mini-java-app
echo.
echo To check pod status:
echo   kubectl get pods -n mini-java-app
echo.
echo To access the application:
echo   kubectl port-forward -n mini-java-app svc/mini-java-app-service 8080:80
echo   Then visit: http://localhost:8080/mini-app
echo.
echo To get ingress IP (may take a few minutes):
echo   kubectl get ingress mini-java-app-ingress -n mini-java-app
echo.
echo ==========================================

REM Cleanup temporary directory
rmdir /S /Q "!TEMP_DIR!"

endlocal
