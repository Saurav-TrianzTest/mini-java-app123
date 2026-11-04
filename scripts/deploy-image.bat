@echo off
setlocal enabledelayedexpansion

REM Deploy image to AWS EKS for mini-java-container
REM Run from the repository root

echo === mini-java-container EKS Deploy Script ===

REM Project name
set "PROJECT_NAME=MiniJavaContainer"
for /f "delims=" %%i in ('powershell -command "$n='MiniJavaContainer'; ($n.ToLower() -replace '[^a-z0-9]', '-') -replace '^-+|-+$',''"') do set "IMAGE_NAME=%%i"

REM Ask for AWS region
set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "

REM Ask for EKS cluster name
set /p CLUSTER_NAME="Enter EKS cluster name: "

REM Ask for image URI
set /p IMAGE_URI="Enter full image URI (registry/repo:tag): "

REM Ask for environment variables
echo.
echo Enter values for environment variables (or press Enter to skip):
echo.

set /p DB_HOST="Enter value for DB_HOST (or press Enter to skip): "
set /p DB_PORT="Enter value for DB_PORT (or press Enter to skip): "
set /p DB_NAME="Enter value for DB_NAME (or press Enter to skip): "
set /p REDIS_HOST="Enter value for REDIS_HOST (or press Enter to skip): "
set /p REDIS_PORT="Enter value for REDIS_PORT (or press Enter to skip): "
set /p EXTERNAL_API_URL="Enter value for EXTERNAL_API_URL (or press Enter to skip): "
set /p PAYMENT_SERVICE_URL="Enter value for PAYMENT_SERVICE_URL (or press Enter to skip): "

REM Credentials (not echoed)
echo.
echo Enter credential values (not echoed):
set /p DB_USERNAME="Enter value for DB_USERNAME (or press Enter to skip): "
echo.
set /p DB_PASSWORD="Enter value for DB_PASSWORD (or press Enter to skip): "
echo.

REM Configure kubectl for the EKS cluster
echo.
echo Configuring kubectl for EKS cluster: !CLUSTER_NAME! in region !AWS_REGION!...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo Failed to configure kubectl for EKS cluster
    exit /b 1
)

echo.
echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo Failed to connect to Kubernetes cluster
    exit /b 1
)

REM Apply namespace
echo.
echo Applying namespace...
kubectl apply -f kubernetes/namespace.yaml
if !ERRORLEVEL! neq 0 (
    echo Failed to apply namespace
    exit /b 1
)

REM Create Kubernetes Secret for credentials
echo.
echo Creating Kubernetes Secret for credentials...
set "SECRET_ARGS="
if not "!DB_USERNAME!"=="" (
    set "SECRET_ARGS=!SECRET_ARGS! --from-literal=db-username=!DB_USERNAME!"
)
if not "!DB_PASSWORD!"=="" (
    set "SECRET_ARGS=!SECRET_ARGS! --from-literal=db-password=!DB_PASSWORD!"
)
if not "!SECRET_ARGS!"=="" (
    kubectl create secret generic !IMAGE_NAME!-secrets -n !IMAGE_NAME! !SECRET_ARGS! --dry-run=client -o yaml | kubectl apply -f -
    if !ERRORLEVEL! neq 0 (
        echo Failed to create Kubernetes Secret
        exit /b 1
    )
)

REM Prepare deployment.yaml with placeholders replaced
echo.
echo Deploying application...

REM Copy deployment.yaml to temp file for editing
copy kubernetes\deployment.yaml kubernetes\deployment.yaml.tmp >nul

REM Replace IMAGE_URI placeholder
powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"

REM Replace environment variable placeholders
if not "!DB_HOST!"=="" (
    powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{DB_HOST}}', '!DB_HOST!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
)
if not "!DB_PORT!"=="" (
    powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{DB_PORT}}', '!DB_PORT!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
)
if not "!DB_NAME!"=="" (
    powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{DB_NAME}}', '!DB_NAME!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
)
if not "!REDIS_HOST!"=="" (
    powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
)
if not "!REDIS_PORT!"=="" (
    powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
)
if not "!EXTERNAL_API_URL!"=="" (
    powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{EXTERNAL_API_URL}}', '!EXTERNAL_API_URL!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
)
if not "!PAYMENT_SERVICE_URL!"=="" (
    powershell -command "(gc kubernetes\deployment.yaml.tmp) -replace '{{PAYMENT_SERVICE_URL}}', '!PAYMENT_SERVICE_URL!' | Out-File -Encoding UTF8 kubernetes\deployment.yaml.tmp"
)

REM Apply deployment
kubectl apply -f kubernetes\deployment.yaml.tmp
if !ERRORLEVEL! neq 0 (
    echo Failed to apply deployment
    del kubernetes\deployment.yaml.tmp >nul 2>&1
    exit /b 1
)
del kubernetes\deployment.yaml.tmp >nul

REM Apply service
echo.
echo Applying service...
kubectl apply -f kubernetes/service.yaml
if !ERRORLEVEL! neq 0 (
    echo Failed to apply service
    exit /b 1
)

REM Apply ingress
echo.
echo Applying ingress...
kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 (
    echo Failed to apply ingress
    exit /b 1
)

REM Wait for deployment to be ready
echo.
echo Waiting for deployment to be ready...
kubectl rollout status deployment/!IMAGE_NAME! -n !IMAGE_NAME!
if !ERRORLEVEL! neq 0 (
    echo Deployment rollout failed
    exit /b 1
)

REM Show resources
echo.
echo === Deployment Status ===
kubectl get pods,svc,ingress -n !IMAGE_NAME!

echo.
echo === Application URL ===
echo http://!IMAGE_NAME!.example.com

echo.
echo === Deployment Complete ===
echo If the deployment fails, rollback with:
echo   kubectl rollout undo deployment/!IMAGE_NAME! -n !IMAGE_NAME!

endlocal
