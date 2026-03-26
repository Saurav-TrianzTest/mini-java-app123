@echo off
setlocal enabledelayedexpansion

REM Build and Push Script for mini-java-app (Windows)
REM This script builds the Docker image and pushes it to the selected registry

echo ==========================================
echo Docker Build and Push Script
echo ==========================================
echo.

REM Project configuration
set PROJECT_NAME=mini-java-app

REM Sanitize image name: lowercase, hyphenate
set IMAGE_NAME=%PROJECT_NAME%
for %%i in (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) do (
    set IMAGE_NAME=!IMAGE_NAME:%%i=%%i!
)
set IMAGE_NAME=%IMAGE_NAME: =-%
set IMAGE_NAME=%IMAGE_NAME%
REM Convert to lowercase using PowerShell
for /f "delims=" %%a in ('powershell -Command "'%IMAGE_NAME%'.ToLower() -replace '[^a-z0-9-]','-' -replace '^-+','' -replace '-+$',''"') do set IMAGE_NAME=%%a

echo Project: %PROJECT_NAME%
echo Image Name: %IMAGE_NAME%
echo.

REM Prompt for image tag
set /p IMAGE_TAG="Enter image tag (default: latest): "
if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest

REM Sanitize tag
for /f "delims=" %%a in ('powershell -Command "'!IMAGE_TAG!'.ToLower() -replace '[^a-z0-9.-]','-' -replace '^-+','' -replace '-+$',''"') do set IMAGE_TAG=%%a
echo Using tag: !IMAGE_TAG!
echo.

REM Registry selection
echo Select Docker Registry:
echo 1. AWS ECR (Elastic Container Registry)
echo 2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    echo.
    echo === AWS ECR Configuration ===
    
    REM Prompt for AWS region
    set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
    
    REM Prompt for AWS account ID
    set /p AWS_ACCOUNT_ID="Enter AWS Account ID: "
    
    REM ECR repository name
    set ECR_REPO=!IMAGE_NAME!
    
    REM Construct ECR registry URL
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!
    
    echo.
    echo Registry URL: !REGISTRY_URL!
    echo Repository: !ECR_REPO!
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    REM Login to ECR
    echo Logging in to AWS ECR...
    for /f "delims=" %%p in ('aws ecr get-login-password --region !AWS_REGION!') do set ECR_PASSWORD=%%p
    echo !ECR_PASSWORD! | docker login --username AWS --password-stdin !REGISTRY_URL!
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: ECR login failed
        exit /b 1
    )
    
    echo ECR login successful
    echo.
    
    REM Check if ECR repository exists, create if not
    echo Checking if ECR repository exists...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Repository does not exist. Creating ECR repository: !ECR_REPO!
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
        echo ECR repository created successfully
    )
    echo.
    
) else if "!REGISTRY_CHOICE!"=="2" (
    echo.
    echo === Docker Hub Configuration ===
    
    REM Prompt for Docker Hub username
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    
    REM Prompt for Docker Hub password
    set /p DOCKER_PASSWORD="Enter Docker Hub password: "
    
    REM Construct Docker Hub image name
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!IMAGE_NAME!:!IMAGE_TAG!
    
    echo.
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    REM Login to Docker Hub
    echo Logging in to Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Docker Hub login failed
        exit /b 1
    )
    
    echo Docker Hub login successful
    echo.
    
) else (
    echo ERROR: Invalid choice. Please select 1 or 2.
    exit /b 1
)

REM Build Docker image
echo ==========================================
echo Building Docker image...
echo ==========================================
docker build -t "!FULL_IMAGE_NAME!" .

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed
    exit /b 1
)

echo.
echo Docker image built successfully: !FULL_IMAGE_NAME!
echo.

REM Push Docker image
echo ==========================================
echo Pushing Docker image to registry...
echo ==========================================
docker push "!FULL_IMAGE_NAME!"

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed
    exit /b 1
)

echo.
echo ==========================================
echo SUCCESS!
echo ==========================================
echo Image pushed successfully: !FULL_IMAGE_NAME!
echo.
echo You can now use this image in your deployment:
echo   !FULL_IMAGE_NAME!
echo.

endlocal
