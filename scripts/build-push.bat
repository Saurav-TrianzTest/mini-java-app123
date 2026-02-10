@echo off
setlocal enabledelayedexpansion

REM Build and Push Script for mini-java-app Docker Image
REM Supports AWS ECR and Docker Hub

echo =====================================
echo Docker Build and Push Script
echo =====================================
echo.

REM Project configuration
set PROJECT_NAME=mini-java-app

REM Prompt for image tag
set /p IMAGE_TAG="Enter image tag (default: latest): "
if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest

REM Sanitize image tag using PowerShell
for /f "delims=" %%i in ('powershell -Command "'!IMAGE_TAG!' -replace '[^a-zA-Z0-9.-]','-' -replace '^-+|-+$','' | ForEach-Object { $_.ToLower() }"') do set IMAGE_TAG=%%i
if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
echo Using sanitized tag: !IMAGE_TAG!
echo.

REM Prompt for registry type
echo Select container registry:
echo 1. AWS ECR (Elastic Container Registry)
echo 2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    echo.
    echo --- AWS ECR Configuration ---
    
    REM AWS ECR configuration
    set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
    set /p AWS_ACCOUNT_ID="Enter AWS Account ID: "
    set /p ECR_REPO="Enter ECR repository name (default: mini-java-app): "
    if "!ECR_REPO!"=="" set ECR_REPO=mini-java-app
    
    REM Sanitize ECR repository name using PowerShell
    for /f "delims=" %%i in ('powershell -Command "'!ECR_REPO!' -replace '[^a-zA-Z0-9/_-]','-' -replace '^-+|-+$','' | ForEach-Object { $_.ToLower() }"') do set ECR_REPO=%%i
    
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!
    
    echo.
    echo Authenticating with AWS ECR...
    
    REM Get ECR login password and authenticate
    for /f "delims=" %%p in ('aws ecr get-login-password --region !AWS_REGION!') do set ECR_PASSWORD=%%p
    echo !ECR_PASSWORD! | docker login --username AWS --password-stdin !REGISTRY_URL!
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: ECR authentication failed. Please check your AWS credentials.
        exit /b 1
    )
    
    echo Successfully authenticated with ECR
    echo.
    
    REM Check if ECR repository exists, create if not
    echo Checking if ECR repository exists...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    
    if !ERRORLEVEL! neq 0 (
        echo Repository does not exist. Creating ECR repository: !ECR_REPO!
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
        if !ERRORLEVEL! neq 0 (
            echo ERROR: Failed to create ECR repository.
            exit /b 1
        )
        echo ECR repository created successfully
    ) else (
        echo ECR repository already exists
    )
    echo.
    
) else if "!REGISTRY_CHOICE!"=="2" (
    echo.
    echo --- Docker Hub Configuration ---
    
    REM Docker Hub configuration
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub password or access token: "
    set /p DOCKER_REPO="Enter Docker Hub repository name (default: mini-java-app): "
    if "!DOCKER_REPO!"=="" set DOCKER_REPO=mini-java-app
    
    REM Sanitize Docker Hub repository name using PowerShell
    for /f "delims=" %%i in ('powershell -Command "'!DOCKER_REPO!' -replace '[^a-zA-Z0-9_-]','-' -replace '^-+|-+$','' | ForEach-Object { $_.ToLower() }"') do set DOCKER_REPO=%%i
    
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!DOCKER_REPO!:!IMAGE_TAG!
    
    echo.
    echo Authenticating with Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Docker Hub authentication failed.
        exit /b 1
    )
    
    echo Successfully authenticated with Docker Hub
    echo.
    
) else (
    echo ERROR: Invalid choice. Please select 1 or 2.
    exit /b 1
)

REM Build Docker image
echo =====================================
echo Building Docker image...
echo Image: !FULL_IMAGE_NAME!
echo =====================================
echo.

docker build -t "!FULL_IMAGE_NAME!" .

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed.
    exit /b 1
)

echo.
echo Docker image built successfully: !FULL_IMAGE_NAME!
echo.

REM Push Docker image
echo =====================================
echo Pushing Docker image to registry...
echo =====================================
echo.

docker push "!FULL_IMAGE_NAME!"

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed.
    exit /b 1
)

echo.
echo =====================================
echo SUCCESS: Image pushed successfully!
echo =====================================
echo.
echo Image: !FULL_IMAGE_NAME!
echo.
echo You can now use this image in your deployment.

pause
