#!/bin/bash

# Enable strict error handling
set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Docker Build and Push Script for Mini Java App ===${NC}"
echo

# Get project name and sanitize it
PROJECT_NAME="mini-java-app"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo -e "${BLUE}Project:${NC} $PROJECT_NAME"
echo -e "${BLUE}Sanitized Image Name:${NC} $IMAGE_NAME"
echo

# Prompt for image tag
echo -e "${YELLOW}Enter image tag (press Enter for 'latest'):${NC}"
read -p "Tag: " IMAGE_TAG
if [ -z "$IMAGE_TAG" ]; then
    IMAGE_TAG="latest"
fi

# Sanitize tag
IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9.-' '-' | sed 's/^-*//;s/-*$//')
if [ -z "$IMAGE_TAG" ]; then
    IMAGE_TAG="latest"
fi

echo -e "${BLUE}Final Tag:${NC} $IMAGE_TAG"
echo

# Registry selection
echo -e "${YELLOW}Select Docker Registry:${NC}"
echo "1. AWS ECR"
echo "2. Docker Hub"
echo
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

case $REGISTRY_CHOICE in
    1)
        echo -e "${BLUE}=== AWS ECR Registry Selected ===${NC}"
        echo
        
        # Get AWS region
        read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
        if [ -z "$AWS_REGION" ]; then
            echo -e "${RED}Error: AWS region is required${NC}"
            exit 1
        fi
        
        # Get AWS account ID
        echo -e "${BLUE}Getting AWS Account ID...${NC}"
        AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
        if [ $? -ne 0 ]; then
            echo -e "${RED}Error: Failed to get AWS Account ID. Please check AWS CLI configuration.${NC}"
            exit 1
        fi
        
        ECR_REPO="$IMAGE_NAME"
        REGISTRY_URL="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
        FULL_IMAGE_NAME="$REGISTRY_URL/$ECR_REPO:$IMAGE_TAG"
        
        echo -e "${BLUE}ECR Repository:${NC} $ECR_REPO"
        echo -e "${BLUE}Registry URL:${NC} $REGISTRY_URL"
        echo -e "${BLUE}Full Image Name:${NC} $FULL_IMAGE_NAME"
        echo
        
        # Login to ECR
        echo -e "${BLUE}Logging into AWS ECR...${NC}"
        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY_URL
        if [ $? -ne 0 ]; then
            echo -e "${RED}Error: ECR login failed${NC}"
            exit 1
        fi
        
        # Check if ECR repository exists, create if not
        echo -e "${BLUE}Checking ECR repository...${NC}"
        aws ecr describe-repositories --repository-names $ECR_REPO --region $AWS_REGION >/dev/null 2>&1 || {
            echo -e "${YELLOW}Creating ECR repository: $ECR_REPO${NC}"
            aws ecr create-repository --repository-name $ECR_REPO --region $AWS_REGION
            if [ $? -ne 0 ]; then
                echo -e "${RED}Error: Failed to create ECR repository${NC}"
                exit 1
            fi
        }
        ;;
        
    2)
        echo -e "${BLUE}=== Docker Hub Registry Selected ===${NC}"
        echo
        
        # Get Docker Hub credentials
        read -p "Enter Docker Hub username: " DOCKER_USERNAME
        if [ -z "$DOCKER_USERNAME" ]; then
            echo -e "${RED}Error: Docker Hub username is required${NC}"
            exit 1
        fi
        
        read -s -p "Enter Docker Hub password: " DOCKER_PASSWORD
        echo
        if [ -z "$DOCKER_PASSWORD" ]; then
            echo -e "${RED}Error: Docker Hub password is required${NC}"
            exit 1
        fi
        
        FULL_IMAGE_NAME="$DOCKER_USERNAME/$IMAGE_NAME:$IMAGE_TAG"
        
        echo -e "${BLUE}Docker Hub Username:${NC} $DOCKER_USERNAME"
        echo -e "${BLUE}Full Image Name:${NC} $FULL_IMAGE_NAME"
        echo
        
        # Login to Docker Hub
        echo -e "${BLUE}Logging into Docker Hub...${NC}"
        echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
        if [ $? -ne 0 ]; then
            echo -e "${RED}Error: Docker Hub login failed${NC}"
            exit 1
        fi
        ;;
        
    *)
        echo -e "${RED}Error: Invalid registry choice${NC}"
        exit 1
        ;;
esac

# Build Docker image
echo -e "${BLUE}Building Docker image...${NC}"
echo -e "${BLUE}Command:${NC} docker build -t $FULL_IMAGE_NAME ."
echo

docker build -t $FULL_IMAGE_NAME .
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Docker build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker build completed successfully${NC}"
echo

# Push Docker image
echo -e "${BLUE}Pushing Docker image to registry...${NC}"
echo -e "${BLUE}Command:${NC} docker push $FULL_IMAGE_NAME"
echo

docker push $FULL_IMAGE_NAME
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Docker push failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker push completed successfully${NC}"
echo
echo -e "${GREEN}=== Build and Push Process Completed ===${NC}"
echo -e "${BLUE}Image:${NC} $FULL_IMAGE_NAME"
echo
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Update your deployment scripts with the image URI: $FULL_IMAGE_NAME"
echo "2. Run the deployment script to deploy to AWS ECS"
echo