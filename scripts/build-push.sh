#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Docker Image Build and Push Script ===${NC}"
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"

# Sanitize image name: lowercase, replace spaces/special chars with hyphens, trim leading/trailing hyphens
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo -e "${YELLOW}Project:${NC} $PROJECT_NAME"
echo -e "${YELLOW}Sanitized Image Name:${NC} $IMAGE_NAME"
echo ""

# Prompt for registry type
echo "Select container registry:"
echo "1. AWS ECR (Elastic Container Registry)"
echo "2. Docker Hub"
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

if [ "$REGISTRY_CHOICE" = "1" ]; then
    # AWS ECR Configuration
    echo -e "\n${GREEN}=== AWS ECR Configuration ===${NC}"
    
    read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
    read -p "Enter AWS Account ID: " AWS_ACCOUNT_ID
    read -p "Enter ECR Repository Name (default: $IMAGE_NAME): " ECR_REPO
    ECR_REPO=${ECR_REPO:-$IMAGE_NAME}
    
    REGISTRY_URL="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
    FULL_IMAGE_NAME="$REGISTRY_URL/$ECR_REPO"
    
    echo -e "\n${YELLOW}Authenticating with AWS ECR...${NC}"
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to authenticate with AWS ECR${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Successfully authenticated with AWS ECR${NC}"
    
    # Check if repository exists, create if not
    echo -e "\n${YELLOW}Checking if ECR repository exists...${NC}"
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1 || {
        echo -e "${YELLOW}Repository does not exist. Creating ECR repository...${NC}"
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION" --image-scanning-configuration scanOnPush=true
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}ECR repository created successfully${NC}"
        else
            echo -e "${RED}Failed to create ECR repository${NC}"
            exit 1
        fi
    }
    echo -e "${GREEN}ECR repository is ready${NC}"
    
elif [ "$REGISTRY_CHOICE" = "2" ]; then
    # Docker Hub Configuration
    echo -e "\n${GREEN}=== Docker Hub Configuration ===${NC}"
    
    read -p "Enter Docker Hub username: " DOCKER_USERNAME
    read -sp "Enter Docker Hub password or access token: " DOCKER_PASSWORD
    echo ""
    
    FULL_IMAGE_NAME="$DOCKER_USERNAME/$IMAGE_NAME"
    
    echo -e "\n${YELLOW}Authenticating with Docker Hub...${NC}"
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to authenticate with Docker Hub${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Successfully authenticated with Docker Hub${NC}"
    
else
    echo -e "${RED}Invalid choice. Exiting.${NC}"
    exit 1
fi

# Prompt for image tag
read -p "\nEnter image tag (default: latest): " IMAGE_TAG
IMAGE_TAG=${IMAGE_TAG:-latest}

# Sanitize tag: lowercase, replace spaces/special chars with hyphens, trim leading/trailing hyphens
IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9.-' '-' | sed 's/^-*//;s/-*$//')

# Default to 'latest' if tag is empty after sanitization
if [ -z "$IMAGE_TAG" ]; then
    IMAGE_TAG="latest"
fi

FULL_IMAGE_NAME="$FULL_IMAGE_NAME:$IMAGE_TAG"

echo -e "\n${YELLOW}Building Docker image...${NC}"
echo -e "${YELLOW}Image:${NC} $FULL_IMAGE_NAME"

# Build Docker image
docker build -t "$FULL_IMAGE_NAME" .

if [ $? -ne 0 ]; then
    echo -e "${RED}Docker build failed${NC}"
    exit 1
fi

echo -e "${GREEN}Docker build completed successfully${NC}"

# Push image to registry
echo -e "\n${YELLOW}Pushing image to registry...${NC}"
docker push "$FULL_IMAGE_NAME"

if [ $? -ne 0 ]; then
    echo -e "${RED}Docker push failed${NC}"
    exit 1
fi

echo -e "\n${GREEN}=== Build and Push Completed Successfully ===${NC}"
echo -e "${GREEN}Image:${NC} $FULL_IMAGE_NAME"
echo -e "\n${YELLOW}Next steps:${NC}"
echo "1. Update ECS task definition with image URI: $FULL_IMAGE_NAME"
echo "2. Run deployment script: ./scripts/deploy-image.sh"
