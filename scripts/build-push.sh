#!/bin/bash
set -e

# Build and Push Script for mini-java-app Docker Image
# Supports AWS ECR and Docker Hub

echo "====================================="
echo "Docker Build and Push Script"
echo "====================================="
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"

# Prompt for image tag
read -p "Enter image tag (default: latest): " IMAGE_TAG
IMAGE_TAG=${IMAGE_TAG:-latest}

# Sanitize image tag (lowercase, replace invalid chars with hyphen, trim hyphens)
IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9.-' '-' | sed 's/^-*//;s/-*$//')
if [ -z "$IMAGE_TAG" ]; then
    IMAGE_TAG="latest"
fi
echo "Using sanitized tag: $IMAGE_TAG"
echo ""

# Prompt for registry type
echo "Select container registry:"
echo "1. AWS ECR (Elastic Container Registry)"
echo "2. Docker Hub"
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

if [ "$REGISTRY_CHOICE" = "1" ]; then
    echo ""
    echo "--- AWS ECR Configuration ---"
    
    # AWS ECR configuration
    read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
    read -p "Enter AWS Account ID: " AWS_ACCOUNT_ID
    read -p "Enter ECR repository name (default: mini-java-app): " ECR_REPO
    ECR_REPO=${ECR_REPO:-mini-java-app}
    
    # Sanitize ECR repository name
    ECR_REPO=$(echo "$ECR_REPO" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9/_-' '-' | sed 's/^-*//;s/-*$//')
    
    REGISTRY_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
    FULL_IMAGE_NAME="${REGISTRY_URL}/${ECR_REPO}:${IMAGE_TAG}"
    
    echo ""
    echo "Authenticating with AWS ECR..."
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"
    
    if [ $? -ne 0 ]; then
        echo "ERROR: ECR authentication failed. Please check your AWS credentials."
        exit 1
    fi
    
    echo "Successfully authenticated with ECR"
    echo ""
    
    # Check if ECR repository exists, create if not
    echo "Checking if ECR repository exists..."
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1
    
    if [ $? -ne 0 ]; then
        echo "Repository does not exist. Creating ECR repository: $ECR_REPO"
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"
        echo "ECR repository created successfully"
    else
        echo "ECR repository already exists"
    fi
    echo ""
    
elif [ "$REGISTRY_CHOICE" = "2" ]; then
    echo ""
    echo "--- Docker Hub Configuration ---"
    
    # Docker Hub configuration
    read -p "Enter Docker Hub username: " DOCKER_USERNAME
    read -sp "Enter Docker Hub password or access token: " DOCKER_PASSWORD
    echo ""
    read -p "Enter Docker Hub repository name (default: mini-java-app): " DOCKER_REPO
    DOCKER_REPO=${DOCKER_REPO:-mini-java-app}
    
    # Sanitize Docker Hub repository name
    DOCKER_REPO=$(echo "$DOCKER_REPO" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9_-' '-' | sed 's/^-*//;s/-*$//')
    
    FULL_IMAGE_NAME="${DOCKER_USERNAME}/${DOCKER_REPO}:${IMAGE_TAG}"
    
    echo ""
    echo "Authenticating with Docker Hub..."
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
    
    if [ $? -ne 0 ]; then
        echo "ERROR: Docker Hub authentication failed."
        exit 1
    fi
    
    echo "Successfully authenticated with Docker Hub"
    echo ""
    
else
    echo "ERROR: Invalid choice. Please select 1 or 2."
    exit 1
fi

# Build Docker image
echo "====================================="
echo "Building Docker image..."
echo "Image: $FULL_IMAGE_NAME"
echo "====================================="
echo ""

docker build -t "$FULL_IMAGE_NAME" .

if [ $? -ne 0 ]; then
    echo "ERROR: Docker build failed."
    exit 1
fi

echo ""
echo "Docker image built successfully: $FULL_IMAGE_NAME"
echo ""

# Push Docker image
echo "====================================="
echo "Pushing Docker image to registry..."
echo "====================================="
echo ""

docker push "$FULL_IMAGE_NAME"

if [ $? -ne 0 ]; then
    echo "ERROR: Docker push failed."
    exit 1
fi

echo ""
echo "====================================="
echo "SUCCESS: Image pushed successfully!"
echo "====================================="
echo ""
echo "Image: $FULL_IMAGE_NAME"
echo ""
echo "You can now use this image in your deployment."
