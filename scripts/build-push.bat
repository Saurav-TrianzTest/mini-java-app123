@echo off
setlocal enabledelayedexpansion

:: ============================================================
:: build-push.bat – Build and push Docker image for McontainerMonoTest
:: ============================================================

set "PROJECT_NAME=mcontainermonotest"

echo ============================================================
echo   McontainerMonoTest - Docker Build ^& Push
echo ============================================================

:: Sanitize image name via PowerShell
for /f "delims=" %%i in ('powershell -NoProfile -Command "$n = 'mcontainermonotest' -replace '[^a-z0-9]','-'; $n = $n.ToLower().Trim('-'); while($n -match '--'){{$n=$n -replace '--','-'}}; $n"') do set "IMAGE_NAME=%%i"

:: Prompt for image tag
set /p "IMAGE_TAG_INPUT=Enter image tag [latest]: "
if "!IMAGE_TAG_INPUT!"=="" set "IMAGE_TAG_INPUT=latest"
for /f "delims=" %%i in ('powershell -NoProfile -Command "$t = '!IMAGE_TAG_INPUT!' -replace '[^a-z0-9._-]','-'; $t = $t.ToLower().Trim('-'); if($t -eq ''){{$t='latest'}}; $t"') do set "IMAGE_TAG=%%i"
if "!IMAGE_TAG!"=="" set "IMAGE_TAG=latest"

echo.
echo Select container registry:
echo   1) Google Artifact Registry
echo   2) Docker Hub
set /p "REGISTRY_CHOICE=Enter choice [1]: "
if "!REGISTRY_CHOICE!"=="" set "REGISTRY_CHOICE=1"

if "!REGISTRY_CHOICE!"=="1" (
    echo.
    set /p "GCP_PROJECT=Enter GCP Project ID: "
    set /p "GCP_REGION=Enter GCP Region (e.g. us-central1): "
    set /p "AR_REPO=Enter Artifact Registry repository name: "

    set "FULL_IMAGE_NAME=!GCP_REGION!-docker.pkg.dev/!GCP_PROJECT!/!AR_REPO!/!IMAGE_NAME!:!IMAGE_TAG!"

    echo.
    echo Authenticating with Google Artifact Registry...
    gcloud auth configure-docker !GCP_REGION!-docker.pkg.dev
    if !ERRORLEVEL! neq 0 (
        echo Artifact Registry authentication failed.
        exit /b 1
    )
) else if "!REGISTRY_CHOICE!"=="2" (
    echo.
    set /p "DOCKER_USERNAME=Enter Docker Hub username: "
    set /p "DOCKER_PASSWORD=Enter Docker Hub password/token: "

    set "FULL_IMAGE_NAME=!DOCKER_USERNAME!/!IMAGE_NAME!:!IMAGE_TAG!"

    echo Authenticating with Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    if !ERRORLEVEL! neq 0 (
        echo Docker Hub authentication failed.
        exit /b 1
    )
) else (
    echo Invalid choice. Exiting.
    exit /b 1
)

echo.
echo Building Docker image: !FULL_IMAGE_NAME!
docker build -f Dockerfile -t "!FULL_IMAGE_NAME!" .
if !ERRORLEVEL! neq 0 (
    echo Docker build failed.
    exit /b 1
)

echo.
echo Pushing Docker image: !FULL_IMAGE_NAME!
docker push "!FULL_IMAGE_NAME!"
if !ERRORLEVEL! neq 0 (
    echo Docker push failed.
    exit /b 1
)

echo.
echo ============================================================
echo   Build and push complete!
echo   Image: !FULL_IMAGE_NAME!
echo ============================================================

endlocal
