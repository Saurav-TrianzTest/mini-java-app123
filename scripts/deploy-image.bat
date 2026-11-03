@echo off
setlocal enabledelayedexpansion

:: =============================================================
:: deploy-image.bat - Deploy mini-java-app to AWS EKS (Windows)
:: =============================================================

set "APP_NAME=mini-java-app"
set "NAMESPACE=mini-java-app"

echo ==============================================
echo   Deploy %APP_NAME% to AWS EKS
echo ==============================================
echo.

:: ---- Cluster configuration ----
set /p "AWS_REGION=Enter AWS Region (e.g. us-east-1): "
if "!AWS_REGION!"=="" (
    echo ERROR: AWS Region is required.
    exit /b 1
)

set /p "CLUSTER_NAME=Enter EKS Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS Cluster Name is required.
    exit /b 1
)

set /p "IMAGE_URI=Enter full Docker image URI: "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

echo.
echo --- Application Environment Variables ---
echo Press Enter to skip any variable (defaults will be used).
echo.

set /p "DB_HOST=Enter DB_HOST (database hostname, default: localhost): "
if "!DB_HOST!"=="" set "DB_HOST=localhost"

set /p "DB_PORT=Enter DB_PORT (default: 3306): "
if "!DB_PORT!"=="" set "DB_PORT=3306"

set /p "DB_NAME=Enter DB_NAME (default: mini_app_db): "
if "!DB_NAME!"=="" set "DB_NAME=mini_app_db"

set /p "DB_USERNAME=Enter DB_USERNAME (default: root): "
if "!DB_USERNAME!"=="" set "DB_USERNAME=root"

set /p "DB_PASSWORD=Enter DB_PASSWORD: "
if "!DB_PASSWORD!"=="" set "DB_PASSWORD="

set /p "REDIS_HOST=Enter REDIS_HOST (default: redis-service): "
if "!REDIS_HOST!"=="" set "REDIS_HOST=redis-service"

set /p "REDIS_PORT=Enter REDIS_PORT (default: 6379): "
if "!REDIS_PORT!"=="" set "REDIS_PORT=6379"

set /p "EXTERNAL_API_URL=Enter EXTERNAL_API_URL (default: http://external-api-service/v1): "
if "!EXTERNAL_API_URL!"=="" set "EXTERNAL_API_URL=http://external-api-service/v1"

set /p "PAYMENT_SERVICE_URL=Enter PAYMENT_SERVICE_URL (default: http://payment-service/process): "
if "!PAYMENT_SERVICE_URL!"=="" set "PAYMENT_SERVICE_URL=http://payment-service/process"

echo.
echo --- Configuring kubectl for EKS ---
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to cluster.
    exit /b 1
)

echo.
echo --- Updating Kubernetes manifests ---
powershell -NoProfile -Command ^
  "(Get-Content kubernetes\deployment.yaml) ^
   -replace '{{IMAGE_URI}}','!IMAGE_URI!' ^
   -replace '{{DB_HOST}}','!DB_HOST!' ^
   -replace '{{DB_PORT}}','!DB_PORT!' ^
   -replace '{{DB_NAME}}','!DB_NAME!' ^
   -replace '{{DB_USERNAME}}','!DB_USERNAME!' ^
   -replace '{{DB_PASSWORD}}','!DB_PASSWORD!' ^
   -replace '{{REDIS_HOST}}','!REDIS_HOST!' ^
   -replace '{{REDIS_PORT}}','!REDIS_PORT!' ^
   -replace '{{EXTERNAL_API_URL}}','!EXTERNAL_API_URL!' ^
   -replace '{{PAYMENT_SERVICE_URL}}','!PAYMENT_SERVICE_URL!' ^
   | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update deployment manifest.
    exit /b 1
)

echo.
echo --- Applying Kubernetes manifests ---
kubectl apply -f kubernetes\namespace.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply namespace. & exit /b 1 )

kubectl apply -f kubernetes\deployment.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply deployment. & exit /b 1 )

kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply service. & exit /b 1 )

kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply ingress. & exit /b 1 )

echo.
echo --- Waiting for deployment rollout ---
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed.
    echo Rollback command: kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
    exit /b 1
)

echo.
echo --- Verifying deployed resources ---
kubectl get pods,svc,ingress -n !NAMESPACE!

echo.
echo ==============================================
echo   Deployment complete!
echo ==============================================
echo.
echo Rollback command (if needed):
echo   kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!

endlocal
