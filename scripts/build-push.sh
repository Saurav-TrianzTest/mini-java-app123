#!/bin/bash

# Build and Push Script for mini-java-app
# This script builds the Docker image and pushes it to the selected registry

set -e

echo "=========================================="
echo "Docker Build and Push Script"
echo "=========================================="
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"

# Sanitize image name: lowercase, hyphenate spaces/special chars, trim hyphens
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo "Project: $PROJECT_NAME"
echo "Sanitized Image Name: $IMAGE_NAME"
echo ""

# Prompt for image tag
read -p "Enter image tag (default: latest): " IMAGE_TAG
IMAGE_TAG=${IMAGE_TAG:-latest}

# Sanitize tag: lowercase, hyphenate, trim hyphens
IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9.-' '-' | sed 's/^-*//;s/-*$//')

# Default to 'latest' if tag is empty after sanitization
if [ -z "$IMAGE_TAG" ]; then
    IMAGE_TAG="latest"
fi

echo "Using tag: $IMAGE_TAG"
echo ""

# Registry selection
echo "Select Docker Registry:"
echo "1. AWS ECR (Elastic Container Registry)"
echo "2. Docker Hub"
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

if [ "$REGISTRY_CHOICE" == "1" ]; then
    echo ""
    echo "=== AWS ECR Configuration ==="
    
    # Prompt for AWS region
    read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
    
    # Prompt for AWS Account ID
    read -p "Enter AWS Account ID: " AWS_ACCOUNT_ID
    
    # ECR repository name (same as image name)
    ECR_REPO="$IMAGE_NAME"
    
    # Construct ECR registry URL
    REGISTRY_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
    FULL_IMAGE_NAME="${REGISTRY_URL}/${ECR_REPO}:${IMAGE_TAG}"
    
    echo ""
    echo "ECR Repository: $ECR_REPO"
    echo "Full Image Name: $FULL_IMAGE_NAME"
    echo ""
    
    # Authenticate with ECR
    echo "Authenticating with AWS ECR..."
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"
    
    if [ $? -ne 0 ]; then
        echo "ERROR: ECR authentication failed"
        exit 1
    fi
    
    echo "ECR authentication successful"
    echo ""
    
    # Check if ECR repository exists, create if not
    echo "Checking if ECR repository exists..."
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1 || {
        echo "Repository does not exist. Creating ECR repository: $ECR_REPO"
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"
        echo "ECR repository created successfully"
    }
    echo ""
    
elif [ "$REGISTRY_CHOICE" == "2" ]; then
    echo ""
    echo "=== Docker Hub Configuration ==="
    
    # Prompt for Docker Hub username
    read -p "Enter Docker Hub username: " DOCKER_USERNAME
    
    # Prompt for Docker Hub password
    read -sp "Enter Docker Hub password: " DOCKER_PASSWORD
    echo ""
    
    # Construct Docker Hub image name
    FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}"
    
    echo ""
    echo "Full Image Name: $FULL_IMAGE_NAME"
    echo ""
    
    # Authenticate with Docker Hub
    echo "Authenticating with Docker Hub..."
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
    
    if [ $? -ne 0 ]; then
        echo "ERROR: Docker Hub authentication failed"
        exit 1
    fi
    
    echo "Docker Hub authentication successful"
    echo ""
    
else
    echo "ERROR: Invalid choice. Please select 1 or 2."
    exit 1
fi

# Build Docker image
echo "=========================================="
echo "Building Docker image..."
echo "=========================================="
docker build -t "$FULL_IMAGE_NAME" .

if [ $? -ne 0 ]; then
    echo "ERROR: Docker build failed"
    exit 1
fi

echo ""
echo "Docker image built successfully: $FULL_IMAGE_NAME"
echo ""

# Push Docker image
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
echo "=========================================="
echo "Image pushed successfully: $FULL_IMAGE_NAME"
echo ""
echo "Next steps:"
echo "1. Use this image URI in your ECS task definition"
echo "2. Run the deploy-image.sh script to deploy to ECS"
echo "=========================================="
