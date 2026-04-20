@echo off
setlocal enabledelayedexpansion

REM Build and Push Script for Mini Java Application (Windows)
REM This script builds the Docker image and pushes it to a container registry

set PROJECT_NAME=mini-java-app
echo ==========================================
echo Docker Build and Push Script
echo Project: %PROJECT_NAME%
echo ==========================================
echo.

REM Sanitize project name for Docker tag (convert to lowercase and replace invalid chars)
set IMAGE_NAME=%PROJECT_NAME%
for %%i in (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) do (
    set IMAGE_NAME=!IMAGE_NAME:%%i=%%i!
)
set IMAGE_NAME=%IMAGE_NAME: =-%
set IMAGE_NAME=%IMAGE_NAME:A=a%
set IMAGE_NAME=%IMAGE_NAME:B=b%
set IMAGE_NAME=%IMAGE_NAME:C=c%
set IMAGE_NAME=%IMAGE_NAME:D=d%
set IMAGE_NAME=%IMAGE_NAME:E=e%
set IMAGE_NAME=%IMAGE_NAME:F=f%
set IMAGE_NAME=%IMAGE_NAME:G=g%
set IMAGE_NAME=%IMAGE_NAME:H=h%
set IMAGE_NAME=%IMAGE_NAME:I=i%
set IMAGE_NAME=%IMAGE_NAME:J=j%
set IMAGE_NAME=%IMAGE_NAME:K=k%
set IMAGE_NAME=%IMAGE_NAME:L=l%
set IMAGE_NAME=%IMAGE_NAME:M=m%
set IMAGE_NAME=%IMAGE_NAME:N=n%
set IMAGE_NAME=%IMAGE_NAME:O=o%
set IMAGE_NAME=%IMAGE_NAME:P=p%
set IMAGE_NAME=%IMAGE_NAME:Q=q%
set IMAGE_NAME=%IMAGE_NAME:R=r%
set IMAGE_NAME=%IMAGE_NAME:S=s%
set IMAGE_NAME=%IMAGE_NAME:T=t%
set IMAGE_NAME=%IMAGE_NAME:U=u%
set IMAGE_NAME=%IMAGE_NAME:V=v%
set IMAGE_NAME=%IMAGE_NAME:W=w%
set IMAGE_NAME=%IMAGE_NAME:X=x%
set IMAGE_NAME=%IMAGE_NAME:Y=y%
set IMAGE_NAME=%IMAGE_NAME:Z=z%

REM Prompt for registry type
echo Select container registry
echo 1. Azure Container Registry (ACR)
echo 2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    REM Azure ACR
    echo.
    echo === Azure Container Registry (ACR) ===
    set /p ACR_NAME="Enter ACR name (e.g., myregistry): "
    set /p IMAGE_TAG="Enter image tag (default: latest): "
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    set FULL_IMAGE_NAME=!ACR_NAME!.azurecr.io/!IMAGE_NAME!:!IMAGE_TAG!
    
    echo.
    echo Logging into Azure Container Registry...
    az acr login --name !ACR_NAME!
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to login to Azure Container Registry
        exit /b 1
    )
    
) else if "!REGISTRY_CHOICE!"=="2" (
    REM Docker Hub
    echo.
    echo === Docker Hub ===
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub password: "
    set /p IMAGE_TAG="Enter image tag (default: latest): "
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!IMAGE_NAME!:!IMAGE_TAG!
    
    echo.
    echo Logging into Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to login to Docker Hub
        exit /b 1
    )
    
) else (
    echo ERROR: Invalid choice. Please select 1 or 2.
    exit /b 1
)

echo.
echo ==========================================
echo Building Docker image...
echo Image: !FULL_IMAGE_NAME!
echo ==========================================

docker build -t !FULL_IMAGE_NAME! .

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed
    exit /b 1
)

echo.
echo ==========================================
echo Pushing Docker image to registry...
echo ==========================================

docker push !FULL_IMAGE_NAME!

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed
    exit /b 1
)

echo.
echo ==========================================
echo SUCCESS!
echo Image pushed: !FULL_IMAGE_NAME!
echo ==========================================
echo.
echo Next steps:
echo 1. Use this image URI in your Kubernetes deployment
echo 2. Run deploy-image.bat to deploy to Azure AKS
echo.

endlocal
