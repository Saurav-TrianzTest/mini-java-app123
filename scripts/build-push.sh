#!/bin/bash
set -e
set -o pipefail

# Build and push script for mini-java-container to AWS ECR
# Run from the repository root

echo "=== mini-java-container Build and Push Script ==="

# Project name
PROJECT_NAME="MiniJavaContainer"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

# Ask for target platform
echo ""
echo "Select target platform:"
echo "  1. AWS ECR"
echo "  2. Docker Hub"
read -p "Enter choice (1 or 2): " PLATFORM_CHOICE

if [ "$PLATFORM_CHOICE" = "1" ]; then
    # AWS ECR
    echo ""
    read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
    read -p "Enter AWS ECR registry URL (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com): " REGISTRY_URL
    read -p "Enter ECR repository name: " ECR_REPO
    read -p "Enter image tag (press Enter for 'latest'): " IMAGE_TAG

    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    # Sanitize image tag
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    FULL_IMAGE_NAME="${REGISTRY_URL}/${ECR_REPO}:${IMAGE_TAG}"

    echo ""
    echo "Logging in to AWS ECR..."
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"

    echo ""
    echo "Creating ECR repository if it does not exist..."
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1 || \
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"

    echo ""
    echo "Building Docker image..."
    docker build -f Dockerfile -t "$FULL_IMAGE_NAME" .

    echo ""
    echo "Pushing image to ECR: $FULL_IMAGE_NAME"
    docker push "$FULL_IMAGE_NAME"

    echo ""
    echo "=== Success ==="
    echo "Image pushed: $FULL_IMAGE_NAME"

elif [ "$PLATFORM_CHOICE" = "2" ]; then
    # Docker Hub
    echo ""
    read -p "Enter Docker Hub username: " DOCKER_USERNAME
    read -s -p "Enter Docker Hub password: " DOCKER_PASSWORD
    echo ""
    read -p "Enter image tag (press Enter for 'latest'): " IMAGE_TAG

    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    # Sanitize image tag
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi

    FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}"

    echo ""
    echo "Logging in to Docker Hub..."
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin

    echo ""
    echo "Building Docker image..."
    docker build -f Dockerfile -t "$FULL_IMAGE_NAME" .

    echo ""
    echo "Pushing image to Docker Hub: $FULL_IMAGE_NAME"
    docker push "$FULL_IMAGE_NAME"

    echo ""
    echo "=== Success ==="
    echo "Image pushed: $FULL_IMAGE_NAME"

else
    echo "Invalid choice. Exiting."
    exit 1
fi
