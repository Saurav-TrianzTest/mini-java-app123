@echo off
setlocal enabledelayedexpansion

REM Deploy to Azure AKS Script for Mini Java Application (Windows)
REM This script deploys the containerized application to Azure Kubernetes Service

echo ==========================================
echo Azure AKS Deployment Script
echo Mini Java Application
echo ==========================================
echo.

REM Prompt for Azure AKS details
echo === Azure AKS Configuration ===
set /p RESOURCE_GROUP="Enter Azure Resource Group name: "
set /p CLUSTER_NAME="Enter AKS Cluster name: "
echo.

REM Validate inputs
if "!RESOURCE_GROUP!"=="" (
    echo ERROR: Resource Group is required
    exit /b 1
)
if "!CLUSTER_NAME!"=="" (
    echo ERROR: Cluster Name is required
    exit /b 1
)

REM Prompt for Docker image URI
echo === Docker Image Configuration ===
set /p IMAGE_URI="Enter Docker image URI (e.g., myregistry.azurecr.io/mini-java-app:latest): "
echo.

if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required
    exit /b 1
)

REM Prompt for environment variables
echo === Application Configuration ===
echo Enter values for application environment variables (press Enter to skip optional ones)
echo.

set /p DATABASE_URL="Enter DATABASE_URL (e.g., jdbc:mysql://mysql-host:3306/mini_app_db): "
set /p DB_USERNAME="Enter DB_USERNAME: "
set /p DB_PASSWORD="Enter DB_PASSWORD: "
set /p DB_HOST="Enter DB_HOST: "
set /p DB_PORT="Enter DB_PORT (default: 3306): "
if "!DB_PORT!"=="" set DB_PORT=3306
set /p DB_NAME="Enter DB_NAME: "

echo.
set /p REDIS_HOST="Enter REDIS_HOST (optional): "
set /p REDIS_PORT="Enter REDIS_PORT (default: 6379): "
if "!REDIS_PORT!"=="" set REDIS_PORT=6379
set /p REDIS_PASSWORD="Enter REDIS_PASSWORD (optional): "

echo.
set /p EXTERNAL_API_URL="Enter EXTERNAL_API_URL (optional): "
set /p EXTERNAL_API_KEY="Enter EXTERNAL_API_KEY (optional): "

echo.
set /p PAYMENT_SERVICE_URL="Enter PAYMENT_SERVICE_URL (optional): "
set /p PAYMENT_SERVICE_USERNAME="Enter PAYMENT_SERVICE_USERNAME (optional): "
set /p PAYMENT_SERVICE_PASSWORD="Enter PAYMENT_SERVICE_PASSWORD (optional): "

echo.
set /p AZURE_STORAGE_CONNECTION_STRING="Enter AZURE_STORAGE_CONNECTION_STRING (optional): "

echo.
set /p JWT_SECRET="Enter JWT_SECRET (optional): "
set /p ADMIN_USERNAME="Enter ADMIN_USERNAME (optional): "
set /p ADMIN_PASSWORD="Enter ADMIN_PASSWORD (optional): "

echo.
set /p MONITORING_ENDPOINT="Enter MONITORING_ENDPOINT (optional): "
set /p MONITORING_USERNAME="Enter MONITORING_USERNAME (optional): "
set /p MONITORING_PASSWORD="Enter MONITORING_PASSWORD (optional): "

echo.
set /p RABBITMQ_HOST="Enter RABBITMQ_HOST (optional): "
set /p RABBITMQ_PORT="Enter RABBITMQ_PORT (default: 5672): "
if "!RABBITMQ_PORT!"=="" set RABBITMQ_PORT=5672
set /p RABBITMQ_USERNAME="Enter RABBITMQ_USERNAME (optional): "
set /p RABBITMQ_PASSWORD="Enter RABBITMQ_PASSWORD (optional): "

echo.
echo ==========================================
echo Configuring kubectl for AKS cluster...
echo ==========================================

az aks get-credentials --resource-group !RESOURCE_GROUP! --name !CLUSTER_NAME! --overwrite-existing

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to get AKS credentials
    exit /b 1
)

echo.
echo Verifying cluster connectivity...
kubectl cluster-info

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to connect to Kubernetes cluster
    exit /b 1
)

echo.
echo ==========================================
echo Updating Kubernetes manifests...
echo ==========================================

REM Create temporary directory for modified manifests
set TEMP_DIR=%TEMP%\k8s-deploy-%RANDOM%
mkdir !TEMP_DIR!
xcopy /E /I /Q kubernetes !TEMP_DIR! >nul

REM Update IMAGE_URI in deployment.yaml using PowerShell
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

REM Update environment variables in deployment.yaml
if not "!DATABASE_URL!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DATABASE_URL}}', '!DATABASE_URL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!DB_USERNAME!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_USERNAME}}', '!DB_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!DB_PASSWORD!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!DB_HOST!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!DB_PORT!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!DB_NAME!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

if not "!REDIS_HOST!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!REDIS_PORT!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!REDIS_PASSWORD!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PASSWORD}}', '!REDIS_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

if not "!EXTERNAL_API_URL!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{EXTERNAL_API_URL}}', '!EXTERNAL_API_URL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!EXTERNAL_API_KEY!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{EXTERNAL_API_KEY}}', '!EXTERNAL_API_KEY!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

if not "!PAYMENT_SERVICE_URL!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{PAYMENT_SERVICE_URL}}', '!PAYMENT_SERVICE_URL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!PAYMENT_SERVICE_USERNAME!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{PAYMENT_SERVICE_USERNAME}}', '!PAYMENT_SERVICE_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!PAYMENT_SERVICE_PASSWORD!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{PAYMENT_SERVICE_PASSWORD}}', '!PAYMENT_SERVICE_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

if not "!AZURE_STORAGE_CONNECTION_STRING!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{AZURE_STORAGE_CONNECTION_STRING}}', '!AZURE_STORAGE_CONNECTION_STRING!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

if not "!JWT_SECRET!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{JWT_SECRET}}', '!JWT_SECRET!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!ADMIN_USERNAME!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{ADMIN_USERNAME}}', '!ADMIN_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!ADMIN_PASSWORD!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{ADMIN_PASSWORD}}', '!ADMIN_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

if not "!MONITORING_ENDPOINT!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{MONITORING_ENDPOINT}}', '!MONITORING_ENDPOINT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!MONITORING_USERNAME!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{MONITORING_USERNAME}}', '!MONITORING_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!MONITORING_PASSWORD!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{MONITORING_PASSWORD}}', '!MONITORING_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

if not "!RABBITMQ_HOST!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_HOST}}', '!RABBITMQ_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!RABBITMQ_PORT!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_PORT}}', '!RABBITMQ_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!RABBITMQ_USERNAME!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_USERNAME}}', '!RABBITMQ_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if not "!RABBITMQ_PASSWORD!"=="" powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_PASSWORD}}', '!RABBITMQ_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

echo Manifests updated successfully

echo.
echo ==========================================
echo Deploying to Azure AKS...
echo ==========================================

REM Apply namespace
echo Creating namespace...
kubectl apply -f !TEMP_DIR!\namespace.yaml

REM Apply deployment
echo Deploying application...
kubectl apply -f !TEMP_DIR!\deployment.yaml

REM Apply service
echo Creating service...
kubectl apply -f !TEMP_DIR!\service.yaml

REM Apply ingress
echo Creating ingress...
kubectl apply -f !TEMP_DIR!\ingress.yaml

echo.
echo ==========================================
echo Waiting for deployment to complete...
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

kubectl get pods -n mini-java-app
echo.
kubectl get svc -n mini-java-app
echo.
kubectl get ingress -n mini-java-app

echo.
echo ==========================================
echo SUCCESS!
echo Application deployed to Azure AKS
echo ==========================================
echo.
echo Access your application:
echo - Internal: http://mini-java-app-service.mini-java-app.svc.cluster.local
echo - External: Check ingress address above
echo.
echo Useful commands:
echo - View logs: kubectl logs -f deployment/mini-java-app -n mini-java-app
echo - View pods: kubectl get pods -n mini-java-app
echo - Describe deployment: kubectl describe deployment mini-java-app -n mini-java-app
echo - Scale deployment: kubectl scale deployment mini-java-app --replicas=3 -n mini-java-app
echo.

REM Cleanup temporary directory
rmdir /S /Q !TEMP_DIR!

endlocal
