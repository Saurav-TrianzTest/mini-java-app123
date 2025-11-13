@echo off
setlocal enabledelayedexpansion

echo === Docker Build and Push Script for Mini Java App ===
echo.

:: Get project name and sanitize it
set PROJECT_NAME=mini-java-app
set IMAGE_NAME=mini-java-app

echo Project: !PROJECT_NAME!
echo Sanitized Image Name: !IMAGE_NAME!
echo.

:: Prompt for image tag
set /p IMAGE_TAG="Enter image tag (press Enter for 'latest'): "
if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest

echo Final Tag: !IMAGE_TAG!
echo.

:: Registry selection
echo Select Docker Registry:
echo 1. AWS ECR
echo 2. Docker Hub
echo.
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    echo === AWS ECR Registry Selected ===
    echo.
    
    :: Get AWS region
    set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
    if "!AWS_REGION!"=="" (
        echo Error: AWS region is required
        exit /b 1
    )
    
    :: Get AWS account ID
    echo Getting AWS Account ID...
    for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text') do set AWS_ACCOUNT_ID=%%i
    if !ERRORLEVEL! neq 0 (
        echo Error: Failed to get AWS Account ID. Please check AWS CLI configuration.
        exit /b 1
    )
    
    set ECR_REPO=!IMAGE_NAME!
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!
    
    echo ECR Repository: !ECR_REPO!
    echo Registry URL: !REGISTRY_URL!
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    :: Login to ECR
    echo Logging into AWS ECR...
    aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    if !ERRORLEVEL! neq 0 (
        echo Error: ECR login failed
        exit /b 1
    )
    
    :: Check if ECR repository exists, create if not
    echo Checking ECR repository...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Creating ECR repository: !ECR_REPO!
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
        if !ERRORLEVEL! neq 0 (
            echo Error: Failed to create ECR repository
            exit /b 1
        )
    )
    
) else if "!REGISTRY_CHOICE!"=="2" (
    echo === Docker Hub Registry Selected ===
    echo.
    
    :: Get Docker Hub credentials
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    if "!DOCKER_USERNAME!"=="" (
        echo Error: Docker Hub username is required
        exit /b 1
    )
    
    set /p DOCKER_PASSWORD="Enter Docker Hub password: "
    if "!DOCKER_PASSWORD!"=="" (
        echo Error: Docker Hub password is required
        exit /b 1
    )
    
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!IMAGE_NAME!:!IMAGE_TAG!
    
    echo Docker Hub Username: !DOCKER_USERNAME!
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    :: Login to Docker Hub
    echo Logging into Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    if !ERRORLEVEL! neq 0 (
        echo Error: Docker Hub login failed
        exit /b 1
    )
    
) else (
    echo Error: Invalid registry choice
    exit /b 1
)

:: Build Docker image
echo Building Docker image...
echo Command: docker build -t !FULL_IMAGE_NAME! .
echo.

docker build -t !FULL_IMAGE_NAME! .
if !ERRORLEVEL! neq 0 (
    echo Error: Docker build failed
    exit /b 1
)

echo Docker build completed successfully
echo.

:: Push Docker image
echo Pushing Docker image to registry...
echo Command: docker push !FULL_IMAGE_NAME!
echo.

docker push !FULL_IMAGE_NAME!
if !ERRORLEVEL! neq 0 (
    echo Error: Docker push failed
    exit /b 1
)

echo Docker push completed successfully
echo.
echo === Build and Push Process Completed ===
echo Image: !FULL_IMAGE_NAME!
echo.
echo Next steps:
echo 1. Update your deployment scripts with the image URI: !FULL_IMAGE_NAME!
echo 2. Run the deployment script to deploy to AWS ECS
echo.

pause