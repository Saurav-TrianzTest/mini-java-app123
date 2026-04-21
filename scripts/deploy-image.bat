@echo off
setlocal enabledelayedexpansion

REM Deploy Mini Java Application to GCP GKE (Windows)
REM This script deploys the containerized application to Google Kubernetes Engine

echo ==========================================
echo GKE Deployment Script
echo Mini Java Application
echo ==========================================
echo.

REM Prompt for GCP configuration
set /p GCP_PROJECT="Enter GCP Project ID: "
set /p GCP_ZONE="Enter GCP Zone (e.g., us-central1-a): "
set /p CLUSTER_NAME="Enter GKE Cluster Name: "

if "!GCP_PROJECT!"=="" (
    echo ERROR: GCP Project ID is required
    exit /b 1
)
if "!GCP_ZONE!"=="" (
    echo ERROR: GCP Zone is required
    exit /b 1
)
if "!CLUSTER_NAME!"=="" (
    echo ERROR: GKE Cluster Name is required
    exit /b 1
)

echo.
echo GCP Configuration:
echo   Project: !GCP_PROJECT!
echo   Zone: !GCP_ZONE!
echo   Cluster: !CLUSTER_NAME!
echo.

REM Prompt for Docker image URI
set /p IMAGE_URI="Enter Docker Image URI (e.g., us-central1-docker.pkg.dev/project/repo/mini-java-app:latest): "

if "!IMAGE_URI!"=="" (
    echo ERROR: Docker Image URI is required
    exit /b 1
)

echo.
echo Docker Image: !IMAGE_URI!
echo.

REM Prompt for environment variables
echo ==========================================
echo Environment Configuration
echo ==========================================
echo Enter values for environment variables (press Enter to use defaults):
echo.

set /p DB_HOST="DB_HOST (default: mysql-service): "
if "!DB_HOST!"=="" set DB_HOST=mysql-service

set /p DB_PORT="DB_PORT (default: 3306): "
if "!DB_PORT!"=="" set DB_PORT=3306

set /p DB_NAME="DB_NAME (default: mini_app_db): "
if "!DB_NAME!"=="" set DB_NAME=mini_app_db

set /p DB_USERNAME="DB_USERNAME (default: root): "
if "!DB_USERNAME!"=="" set DB_USERNAME=root

set /p DB_PASSWORD="DB_PASSWORD (default: password): "
if "!DB_PASSWORD!"=="" set DB_PASSWORD=password

set DB_URL=jdbc:mysql://!DB_HOST!:!DB_PORT!/!DB_NAME!

set /p DB_POOL_MAX_CONNECTIONS="DB_POOL_MAX_CONNECTIONS (default: 20): "
if "!DB_POOL_MAX_CONNECTIONS!"=="" set DB_POOL_MAX_CONNECTIONS=20

set /p DB_POOL_TIMEOUT="DB_POOL_TIMEOUT (default: 5000): "
if "!DB_POOL_TIMEOUT!"=="" set DB_POOL_TIMEOUT=5000

set /p DB_QUERY_TIMEOUT="DB_QUERY_TIMEOUT (default: 30): "
if "!DB_QUERY_TIMEOUT!"=="" set DB_QUERY_TIMEOUT=30

set /p REDIS_HOST="REDIS_HOST (default: redis-service): "
if "!REDIS_HOST!"=="" set REDIS_HOST=redis-service

set /p REDIS_PORT="REDIS_PORT (default: 6379): "
if "!REDIS_PORT!"=="" set REDIS_PORT=6379

set /p REDIS_PASSWORD="REDIS_PASSWORD (optional): "

set /p REDIS_DATABASE="REDIS_DATABASE (default: 0): "
if "!REDIS_DATABASE!"=="" set REDIS_DATABASE=0

set /p EXTERNAL_API_URL="EXTERNAL_API_URL (default: http://api-service:8080/v1): "
if "!EXTERNAL_API_URL!"=="" set EXTERNAL_API_URL=http://api-service:8080/v1

set /p EXTERNAL_API_TIMEOUT="EXTERNAL_API_TIMEOUT (default: 30000): "
if "!EXTERNAL_API_TIMEOUT!"=="" set EXTERNAL_API_TIMEOUT=30000

set /p EXTERNAL_API_KEY="EXTERNAL_API_KEY (optional): "

set /p PAYMENT_SERVICE_URL="PAYMENT_SERVICE_URL (default: http://payment-service/process): "
if "!PAYMENT_SERVICE_URL!"=="" set PAYMENT_SERVICE_URL=http://payment-service/process

set /p PAYMENT_SERVICE_USERNAME="PAYMENT_SERVICE_USERNAME (optional): "

set /p PAYMENT_SERVICE_PASSWORD="PAYMENT_SERVICE_PASSWORD (optional): "

set /p GCS_CONFIG_BUCKET="GCS_CONFIG_BUCKET (default: gs://app-config-bucket): "
if "!GCS_CONFIG_BUCKET!"=="" set GCS_CONFIG_BUCKET=gs://app-config-bucket

set /p GCS_LOG_BUCKET="GCS_LOG_BUCKET (default: gs://app-logs-bucket): "
if "!GCS_LOG_BUCKET!"=="" set GCS_LOG_BUCKET=gs://app-logs-bucket

set /p GCS_TEMP_BUCKET="GCS_TEMP_BUCKET (default: gs://app-temp-bucket): "
if "!GCS_TEMP_BUCKET!"=="" set GCS_TEMP_BUCKET=gs://app-temp-bucket

set /p GCS_UPLOAD_BUCKET="GCS_UPLOAD_BUCKET (default: gs://app-uploads-bucket): "
if "!GCS_UPLOAD_BUCKET!"=="" set GCS_UPLOAD_BUCKET=gs://app-uploads-bucket

set /p JWT_SECRET="JWT_SECRET (optional): "

set /p ADMIN_USERNAME="ADMIN_USERNAME (optional): "

set /p ADMIN_PASSWORD="ADMIN_PASSWORD (optional): "

set /p ENCRYPTION_KEY="ENCRYPTION_KEY (optional): "

set /p MONITORING_ENDPOINT="MONITORING_ENDPOINT (default: http://monitoring-service:9090/metrics): "
if "!MONITORING_ENDPOINT!"=="" set MONITORING_ENDPOINT=http://monitoring-service:9090/metrics

set /p MONITORING_USERNAME="MONITORING_USERNAME (optional): "

set /p MONITORING_PASSWORD="MONITORING_PASSWORD (optional): "

set /p RABBITMQ_HOST="RABBITMQ_HOST (default: rabbitmq-service): "
if "!RABBITMQ_HOST!"=="" set RABBITMQ_HOST=rabbitmq-service

set /p RABBITMQ_PORT="RABBITMQ_PORT (default: 5672): "
if "!RABBITMQ_PORT!"=="" set RABBITMQ_PORT=5672

set /p RABBITMQ_USERNAME="RABBITMQ_USERNAME (optional): "

set /p RABBITMQ_PASSWORD="RABBITMQ_PASSWORD (optional): "

set /p ENVIRONMENT="ENVIRONMENT (default: production): "
if "!ENVIRONMENT!"=="" set ENVIRONMENT=production

set /p DEBUG_ENABLED="DEBUG_ENABLED (default: false): "
if "!DEBUG_ENABLED!"=="" set DEBUG_ENABLED=false

set /p LOGGING_LEVEL="LOGGING_LEVEL (default: INFO): "
if "!LOGGING_LEVEL!"=="" set LOGGING_LEVEL=INFO

echo.
echo ==========================================
echo Configuring kubectl for GKE...
echo ==========================================

REM Configure kubectl to use GKE cluster
gcloud container clusters get-credentials "!CLUSTER_NAME!" --zone "!GCP_ZONE!" --project "!GCP_PROJECT!"

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl for GKE cluster
    exit /b 1
)

REM Verify cluster connectivity
echo.
echo Verifying cluster connectivity...
kubectl cluster-info

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to connect to Kubernetes cluster
    exit /b 1
)

echo.
echo Success: Connected to GKE cluster
echo.

REM Update Kubernetes manifests with actual values
echo ==========================================
echo Updating Kubernetes manifests...
echo ==========================================

REM Create temporary directory for updated manifests
set TEMP_DIR=%TEMP%\k8s-deploy-%RANDOM%
mkdir "!TEMP_DIR!"
xcopy /E /I /Q kubernetes "!TEMP_DIR!"

REM Update deployment.yaml with PowerShell (more reliable than batch string replacement)
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_USERNAME}}', '!DB_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_URL}}', '!DB_URL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_POOL_MAX_CONNECTIONS}}', '!DB_POOL_MAX_CONNECTIONS!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_POOL_TIMEOUT}}', '!DB_POOL_TIMEOUT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_QUERY_TIMEOUT}}', '!DB_QUERY_TIMEOUT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PASSWORD}}', '!REDIS_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_DATABASE}}', '!REDIS_DATABASE!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{EXTERNAL_API_URL}}', '!EXTERNAL_API_URL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{EXTERNAL_API_TIMEOUT}}', '!EXTERNAL_API_TIMEOUT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
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
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{ENVIRONMENT}}', '!ENVIRONMENT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DEBUG_ENABLED}}', '!DEBUG_ENABLED!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{LOGGING_LEVEL}}', '!LOGGING_LEVEL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

echo Success: Manifests updated
echo.

REM Deploy to Kubernetes
echo ==========================================
echo Deploying to GKE...
echo ==========================================

REM Apply namespace
echo.
echo Creating namespace...
kubectl apply -f "!TEMP_DIR!\namespace.yaml"

REM Apply deployment
echo.
echo Deploying application...
kubectl apply -f "!TEMP_DIR!\deployment.yaml"

REM Apply service
echo.
echo Creating service...
kubectl apply -f "!TEMP_DIR!\service.yaml"

REM Apply ingress
echo.
echo Creating ingress...
kubectl apply -f "!TEMP_DIR!\ingress.yaml"

REM Wait for deployment to complete
echo.
echo ==========================================
echo Waiting for deployment to complete...
echo ==========================================
kubectl rollout status deployment/mini-java-app -n mini-java-app --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed
    echo.
    echo Checking pod status...
    kubectl get pods -n mini-java-app
    echo.
    echo Checking pod logs...
    kubectl logs -n mini-java-app -l app=mini-java-app --tail=50
    exit /b 1
)

REM Verify deployment
echo.
echo ==========================================
echo Verifying deployment...
echo ==========================================
kubectl get pods,svc,ingress -n mini-java-app

REM Get ingress IP
echo.
echo ==========================================
echo Deployment Information
echo ==========================================

for /f "tokens=*" %%i in ('kubectl get ingress mini-java-app-ingress -n mini-java-app -o jsonpath^="{.status.loadBalancer.ingress[0].ip}" 2^>nul') do set INGRESS_IP=%%i
if "!INGRESS_IP!"=="" set INGRESS_IP=Pending...

echo.
echo Success: Deployment completed successfully!
echo.
echo Application Details:
echo   Namespace: mini-java-app
echo   Deployment: mini-java-app
echo   Service: mini-java-app-service
echo   Ingress IP: !INGRESS_IP!
echo.
echo Access your application:
echo   Internal: http://mini-java-app-service.mini-java-app.svc.cluster.local
echo   External: http://!INGRESS_IP! (once DNS is configured)
echo.
echo Useful commands:
echo   View pods: kubectl get pods -n mini-java-app
echo   View logs: kubectl logs -n mini-java-app -l app=mini-java-app
echo   Scale deployment: kubectl scale deployment mini-java-app -n mini-java-app --replicas=3
echo   Delete deployment: kubectl delete namespace mini-java-app
echo.

REM Cleanup temporary directory
rmdir /S /Q "!TEMP_DIR!"
