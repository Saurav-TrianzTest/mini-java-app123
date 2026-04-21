@echo off
setlocal enabledelayedexpansion

REM Build and Push Docker Image Script for Mini Java Application (Windows)
REM Supports Google Artifact Registry and Docker Hub

echo ==========================================
echo Docker Image Build and Push Script
echo ==========================================
echo.

REM Project configuration
set PROJECT_NAME=mini-java-app

REM Sanitize project name for Docker image naming (convert to lowercase and replace invalid chars)
set IMAGE_NAME=%PROJECT_NAME%
for %%i in (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) do (
    set IMAGE_NAME=!IMAGE_NAME:%%i=%%i!
)
set IMAGE_NAME=%IMAGE_NAME: =-%
set IMAGE_NAME=%IMAGE_NAME:_=-%
call :tolower IMAGE_NAME

echo Project: %PROJECT_NAME%
echo Image Name: %IMAGE_NAME%
echo.

REM Prompt for image tag
set /p IMAGE_TAG="Enter image tag (default: latest): "
if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest

REM Sanitize tag
set IMAGE_TAG=!IMAGE_TAG: =-!
set IMAGE_TAG=!IMAGE_TAG:_=-!
call :tolower IMAGE_TAG

echo Using tag: !IMAGE_TAG!
echo.

REM Select registry type
echo Select Docker Registry:
echo 1. Google Artifact Registry
echo 2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    echo.
    echo === Google Artifact Registry Configuration ===
    
    REM Prompt for GCP details
    set /p GCP_PROJECT="Enter GCP Project ID: "
    set /p GCP_REGION="Enter GCP Region (e.g., us-central1): "
    set /p ARTIFACT_REPO="Enter Artifact Registry Repository Name: "
    
    REM Construct full image name
    set FULL_IMAGE_NAME=!GCP_REGION!-docker.pkg.dev/!GCP_PROJECT!/!ARTIFACT_REPO!/!IMAGE_NAME!:!IMAGE_TAG!
    
    echo.
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    REM Authenticate with GCP
    echo Authenticating with Google Cloud...
    gcloud auth login
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: GCP authentication failed
        exit /b 1
    )
    
    REM Set GCP project
    gcloud config set project "!GCP_PROJECT!"
    
    REM Configure Docker for Artifact Registry
    echo Configuring Docker for Artifact Registry...
    gcloud auth configure-docker !GCP_REGION!-docker.pkg.dev
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Artifact Registry authentication failed
        exit /b 1
    )
    
) else if "!REGISTRY_CHOICE!"=="2" (
    echo.
    echo === Docker Hub Configuration ===
    
    REM Prompt for Docker Hub credentials
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub password/token: "
    
    REM Construct full image name
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!IMAGE_NAME!:!IMAGE_TAG!
    
    echo.
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    REM Authenticate with Docker Hub
    echo Authenticating with Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Docker Hub authentication failed
        exit /b 1
    )
    
) else (
    echo ERROR: Invalid choice. Please select 1 or 2.
    exit /b 1
)

REM Build Docker image
echo.
echo ==========================================
echo Building Docker Image...
echo ==========================================
docker build -t "!FULL_IMAGE_NAME!" .

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed
    exit /b 1
)

echo.
echo Success: Docker image built successfully: !FULL_IMAGE_NAME!

REM Push Docker image
echo.
echo ==========================================
echo Pushing Docker Image...
echo ==========================================
docker push "!FULL_IMAGE_NAME!"

if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed
    exit /b 1
)

echo.
echo ==========================================
echo Success: Image pushed successfully
echo ==========================================
echo Image: !FULL_IMAGE_NAME!
echo.
echo Next steps:
echo 1. Update kubernetes/deployment.yaml with image: !FULL_IMAGE_NAME!
echo 2. Run deploy-image.bat to deploy to GKE
echo.

goto :eof

:tolower
REM Convert variable to lowercase
for %%L in (a b c d e f g h i j k l m n o p q r s t u v w x y z) do (
    call set %1=%%%1:%%L=%%L%%
)
goto :eof
