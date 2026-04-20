#!/bin/bash
set -e

# Build and Push Script for Mini Java Application
# This script builds the Docker image and pushes it to a container registry

PROJECT_NAME="mini-java-app"
echo "=========================================="
echo "Docker Build and Push Script"
echo "Project: $PROJECT_NAME"
echo "=========================================="
echo ""

# Sanitize project name for Docker tag
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

# Prompt for registry type
echo "Select container registry:"
echo "1. Azure Container Registry (ACR)"
echo "2. Docker Hub"
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

if [ "$REGISTRY_CHOICE" = "1" ]; then
    # Azure ACR
    echo ""
    echo "=== Azure Container Registry (ACR) ==="
    read -p "Enter ACR name (e.g., myregistry): " ACR_NAME
    read -p "Enter image tag (default: latest): " IMAGE_TAG
    IMAGE_TAG=${IMAGE_TAG:-latest}
    
    # Sanitize tag
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9.-' '-' | sed 's/^-*//;s/-*$//')
    IMAGE_TAG=${IMAGE_TAG:-latest}
    
    FULL_IMAGE_NAME="${ACR_NAME}.azurecr.io/${IMAGE_NAME}:${IMAGE_TAG}"
    
    echo ""
    echo "Logging into Azure Container Registry..."
    az acr login --name "$ACR_NAME"
    
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to login to Azure Container Registry"
        exit 1
    fi
    
elif [ "$REGISTRY_CHOICE" = "2" ]; then
    # Docker Hub
    echo ""
    echo "=== Docker Hub ==="
    read -p "Enter Docker Hub username: " DOCKER_USERNAME
    read -sp "Enter Docker Hub password: " DOCKER_PASSWORD
    echo ""
    read -p "Enter image tag (default: latest): " IMAGE_TAG
    IMAGE_TAG=${IMAGE_TAG:-latest}
    
    # Sanitize tag
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9.-' '-' | sed 's/^-*//;s/-*$//')
    IMAGE_TAG=${IMAGE_TAG:-latest}
    
    FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}"
    
    echo ""
    echo "Logging into Docker Hub..."
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
    
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to login to Docker Hub"
        exit 1
    fi
    
else
    echo "ERROR: Invalid choice. Please select 1 or 2."
    exit 1
fi

echo ""
echo "=========================================="
echo "Building Docker image..."
echo "Image: $FULL_IMAGE_NAME"
echo "=========================================="

docker build -t "$FULL_IMAGE_NAME" .

if [ $? -ne 0 ]; then
    echo "ERROR: Docker build failed"
    exit 1
fi

echo ""
echo "=========================================="
echo "Pushing Docker image to registry..."
echo "=========================================="

docker push "$FULL_IMAGE_NAME"

if [ $? -ne 0 ]; then
    echo "ERROR: Docker push failed"
    exit 1
fi

echo ""
echo "=========================================="
echo "SUCCESS!"
echo "Image pushed: $FULL_IMAGE_NAME"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Use this image URI in your Kubernetes deployment"
echo "2. Run deploy-image.sh to deploy to Azure AKS"
echo ""
