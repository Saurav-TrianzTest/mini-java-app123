@echo off
setlocal enabledelayedexpansion

echo === Docker Image Build and Push Script ===
echo.

REM Project configuration
set PROJECT_NAME=mini-java-app

REM Sanitize image name: lowercase, replace special chars with hyphens
set IMAGE_NAME=!PROJECT_NAME!
for %%i in (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) do (
    set IMAGE_NAME=!IMAGE_NAME:%%i=%%i!
)
set IMAGE_NAME=!IMAGE_NAME: =-!
set IMAGE_NAME=!IMAGE_NAME:_=-!
for /f "delims=" %%a in ("!IMAGE_NAME!") do set IMAGE_NAME=%%a

echo Project: !PROJECT_NAME!
echo Sanitized Image Name: !IMAGE_NAME!
echo.

REM Prompt for registry type
echo Select container registry:
echo 1. AWS ECR (Elastic Container Registry)
echo 2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    REM AWS ECR Configuration
    echo.
    echo === AWS ECR Configuration ===
    
    set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
    set /p AWS_ACCOUNT_ID="Enter AWS Account ID: "
    set /p ECR_REPO="Enter ECR Repository Name (default: !IMAGE_NAME!): "
    if "!ECR_REPO!"==" " set ECR_REPO=!IMAGE_NAME!
    
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!
    
    echo.
    echo Authenticating with AWS ECR...
    aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    
    if !ERRORLEVEL! neq 0 (
        echo Failed to authenticate with AWS ECR
        exit /b 1
    )
    
    echo Successfully authenticated with AWS ECR
    
    REM Check if repository exists, create if not
    echo.
    echo Checking if ECR repository exists...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    
    if !ERRORLEVEL! neq 0 (
        echo Repository does not exist. Creating ECR repository...
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION! --image-scanning-configuration scanOnPush=true
        
        if !ERRORLEVEL! equ 0 (
            echo ECR repository created successfully
        ) else (
            echo Failed to create ECR repository
            exit /b 1
        )
    )
    echo ECR repository is ready
    
) else if "!REGISTRY_CHOICE!"=="2" (
    REM Docker Hub Configuration
    echo.
    echo === Docker Hub Configuration ===
    
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub password or access token: "
    
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!IMAGE_NAME!
    
    echo.
    echo Authenticating with Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    
    if !ERRORLEVEL! neq 0 (
        echo Failed to authenticate with Docker Hub
        exit /b 1
    )
    
    echo Successfully authenticated with Docker Hub
    
) else (
    echo Invalid choice. Exiting.
    exit /b 1
)

REM Prompt for image tag
set /p IMAGE_TAG="Enter image tag (default: latest): "
if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest

REM Sanitize tag
for %%i in (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) do (
    set IMAGE_TAG=!IMAGE_TAG:%%i=%%i!
)
set IMAGE_TAG=!IMAGE_TAG: =-!

if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest

set FULL_IMAGE_NAME=!FULL_IMAGE_NAME!:!IMAGE_TAG!

echo.
echo Building Docker image...
echo Image: !FULL_IMAGE_NAME!

REM Build Docker image
docker build -t !FULL_IMAGE_NAME! .

if !ERRORLEVEL! neq 0 (
    echo Docker build failed
    exit /b 1
)

echo Docker build completed successfully

REM Push image to registry
echo.
echo Pushing image to registry...
docker push !FULL_IMAGE_NAME!

if !ERRORLEVEL! neq 0 (
    echo Docker push failed
    exit /b 1
)

echo.
echo === Build and Push Completed Successfully ===
echo Image: !FULL_IMAGE_NAME!
echo.
echo Next steps:
echo 1. Update ECS task definition with image URI: !FULL_IMAGE_NAME!
echo 2. Run deployment script: scripts\deploy-image.bat

endlocal
