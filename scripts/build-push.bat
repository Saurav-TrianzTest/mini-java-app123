@echo off
setlocal enabledelayedexpansion

REM Build and push script for mini-java-container to AWS ECR or Docker Hub
REM Run from the repository root

echo === mini-java-container Build and Push Script ===

REM Project name
set "PROJECT_NAME=MiniJavaContainer"
REM Sanitize image name with PowerShell
for /f "delims=" %%i in ('powershell -command "$n='MiniJavaContainer'; ($n.ToLower() -replace '[^a-z0-9]', '-') -replace '^-+|-+$',''"') do set "IMAGE_NAME=%%i"

REM Ask for target platform
echo.
echo Select target platform:
echo   1. AWS ECR
echo   2. Docker Hub
set /p PLATFORM_CHOICE="Enter choice (1 or 2): "

if "!PLATFORM_CHOICE!"=="1" (
    REM AWS ECR
    echo.
    set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
    set /p REGISTRY_URL="Enter AWS ECR registry URL (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com): "
    set /p ECR_REPO="Enter ECR repository name: "
    set /p IMAGE_TAG="Enter image tag (press Enter for 'latest'): "

    if "!IMAGE_TAG!"=="" (
        set "IMAGE_TAG=latest"
    )

    REM Sanitize image tag with PowerShell
    for /f "delims=" %%i in ('powershell -command "$t='!IMAGE_TAG!'; ($t -replace '[^a-z0-9._-]', '-') -replace '^-+|-+$',''"') do set "IMAGE_TAG=%%i"
    if "!IMAGE_TAG!"=="" (
        set "IMAGE_TAG=latest"
    )

    set "FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!"

    echo.
    echo Logging in to AWS ECR...
    aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    if !ERRORLEVEL! neq 0 (
        echo ECR login failed
        exit /b 1
    )

    echo.
    echo Creating ECR repository if it does not exist...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Creating ECR repository...
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
    )

    echo.
    echo Building Docker image...
    docker build -f Dockerfile -t "!FULL_IMAGE_NAME!" .
    if !ERRORLEVEL! neq 0 (
        echo Docker build failed
        exit /b 1
    )

    echo.
    echo Pushing image to ECR: !FULL_IMAGE_NAME!
    docker push "!FULL_IMAGE_NAME!"
    if !ERRORLEVEL! neq 0 (
        echo Docker push failed
        exit /b 1
    )

    echo.
    echo === Success ===
    echo Image pushed: !FULL_IMAGE_NAME!

) else if ("!PLATFORM_CHOICE!"=="2") (
    REM Docker Hub
    echo.
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub password (hidden): "
    echo.
    set /p IMAGE_TAG="Enter image tag (press Enter for 'latest'): "

    if "!IMAGE_TAG!"=="" (
        set "IMAGE_TAG=latest"
    )

    REM Sanitize image tag with PowerShell
    for /f "delims=" %%i in ('powershell -command "$t='!IMAGE_TAG!'; ($t -replace '[^a-z0-9._-]', '-') -replace '^-+|-+$',''"') do set "IMAGE_TAG=%%i"
    if "!IMAGE_TAG!"=="" (
        set "IMAGE_TAG=latest"
    )

    set "FULL_IMAGE_NAME=!DOCKER_USERNAME!/!IMAGE_NAME!:!IMAGE_TAG!"

    echo.
    echo Logging in to Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username "!DOCKER_USERNAME!" --password-stdin
    if !ERRORLEVEL! neq 0 (
        echo Docker Hub login failed
        exit /b 1
    )

    echo.
    echo Building Docker image...
    docker build -f Dockerfile -t "!FULL_IMAGE_NAME!" .
    if !ERRORLEVEL! neq 0 (
        echo Docker build failed
        exit /b 1
    )

    echo.
    echo Pushing image to Docker Hub: !FULL_IMAGE_NAME!
    docker push "!FULL_IMAGE_NAME!"
    if !ERRORLEVEL! neq 0 (
        echo Docker push failed
        exit /b 1
    )

    echo.
    echo === Success ===
    echo Image pushed: !FULL_IMAGE_NAME!

) else (
    echo Invalid choice. Exiting.
    exit /b 1
)

endlocal
