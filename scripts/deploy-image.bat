@echo off
setlocal enabledelayedexpansion

:: ============================================================
:: deploy-image.bat – Deploy McontainerMonoTest to GCP GKE
:: ============================================================

set "APP_NAME=mcontainermonotest"
set "NAMESPACE=mcontainermonotest"

echo ============================================================
echo   McontainerMonoTest - GKE Deployment
echo ============================================================

:: ---- Collect GCP / cluster details ----
set /p "GCP_PROJECT=Enter GCP Project ID: "
if "!GCP_PROJECT!"=="" (
    echo ERROR: GCP Project ID is required.
    exit /b 1
)

set /p "GCP_ZONE=Enter GCP Zone (e.g. us-central1-a): "
if "!GCP_ZONE!"=="" (
    echo ERROR: GCP Zone is required.
    exit /b 1
)

set /p "CLUSTER_NAME=Enter GKE Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: GKE Cluster Name is required.
    exit /b 1
)

set /p "IMAGE_URI=Enter full Docker image URI: "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

echo.
echo ---- Optional: Application Environment Variables ----
echo Press Enter to skip any variable.

set /p "DB_HOST=Enter DB_HOST (database host): "
set /p "DB_PORT=Enter DB_PORT [3306]: "
if "!DB_PORT!"=="" set "DB_PORT=3306"
set /p "DB_NAME=Enter DB_NAME [mini_app_db]: "
if "!DB_NAME!"=="" set "DB_NAME=mini_app_db"
set /p "DB_USERNAME=Enter DB_USERNAME [root]: "
if "!DB_USERNAME!"=="" set "DB_USERNAME=root"
set /p "DB_PASSWORD=Enter DB_PASSWORD: "
set /p "REDIS_HOST=Enter REDIS_HOST [redis-service]: "
if "!REDIS_HOST!"=="" set "REDIS_HOST=redis-service"
set /p "REDIS_PORT=Enter REDIS_PORT [6379]: "
if "!REDIS_PORT!"=="" set "REDIS_PORT=6379"
set /p "EXTERNAL_API_URL=Enter EXTERNAL_API_URL: "
if "!EXTERNAL_API_URL!"=="" set "EXTERNAL_API_URL=http://api.example.com:8080/v1"
set /p "PAYMENT_SERVICE_URL=Enter PAYMENT_SERVICE_URL: "
if "!PAYMENT_SERVICE_URL!"=="" set "PAYMENT_SERVICE_URL=https://payment.internal.company.com/process"

:: ---- Configure kubectl ----
echo.
echo Configuring kubectl for cluster: !CLUSTER_NAME!...
gcloud container clusters get-credentials !CLUSTER_NAME! --zone !GCP_ZONE! --project !GCP_PROJECT!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to get cluster credentials.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to cluster.
    exit /b 1
)

:: ---- Update deployment.yaml with PowerShell ----
echo.
echo Updating Kubernetes manifests...

set "DEPLOY_YAML=kubernetes\deployment.yaml"
copy "!DEPLOY_YAML!" "!DEPLOY_YAML!.bak" >nul

powershell -NoProfile -Command ^
    "(Get-Content '!DEPLOY_YAML!') ^
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
    | Set-Content '!DEPLOY_YAML!'"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update deployment manifest.
    copy "!DEPLOY_YAML!.bak" "!DEPLOY_YAML!" >nul
    exit /b 1
)

:: ---- Apply manifests in order ----
echo.
echo Applying Kubernetes manifests...

echo   [1/4] Applying namespace...
kubectl apply -f kubernetes\namespace.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply namespace. & exit /b 1 )

echo   [2/4] Applying deployment...
kubectl apply -f kubernetes\deployment.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply deployment. & exit /b 1 )

echo   [3/4] Applying service...
kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply service. & exit /b 1 )

echo   [4/4] Applying ingress...
kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply ingress. & exit /b 1 )

:: ---- Restore original manifest ----
copy "!DEPLOY_YAML!.bak" "!DEPLOY_YAML!" >nul
del "!DEPLOY_YAML!.bak" >nul

:: ---- Wait for rollout ----
echo.
echo Waiting for deployment rollout...
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo WARNING: Rollout did not complete within timeout.
    echo Run: kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
    exit /b 1
)

:: ---- Verify resources ----
echo.
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n !NAMESPACE!

echo.
echo ============================================================
echo   Deployment complete!
echo   Namespace : !NAMESPACE!
echo   Image     : !IMAGE_URI!
echo.
echo   To rollback:
echo     kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
echo ============================================================

endlocal
