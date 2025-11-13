#!/bin/bash

# Enable strict error handling
set -e
set -o pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== AWS ECS Fargate Deployment Script for Mini Java App ===${NC}"
echo

# Configuration
APP_NAME="mini-java-app"
TASK_FAMILY="${APP_NAME}-task"
SERVICE_NAME="${APP_NAME}-service"

# Get deployment parameters
echo -e "${YELLOW}=== Deployment Configuration ===${NC}"
echo

# AWS Region
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
    echo -e "${RED}Error: AWS region is required${NC}"
    exit 1
fi

# ECS Cluster
read -p "Enter ECS cluster name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
    echo -e "${RED}Error: ECS cluster name is required${NC}"
    exit 1
fi

# Docker image URI
read -p "Enter Docker image URI: " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
    echo -e "${RED}Error: Docker image URI is required${NC}"
    exit 1
fi

# Network configuration
read -p "Enter VPC ID: " VPC_ID
if [ -z "$VPC_ID" ]; then
    echo -e "${RED}Error: VPC ID is required${NC}"
    exit 1
fi

read -p "Enter subnet IDs (comma-separated): " SUBNET_INPUT
if [ -z "$SUBNET_INPUT" ]; then
    echo -e "${RED}Error: At least one subnet ID is required${NC}"
    exit 1
fi

# Convert comma-separated subnets to array
IFS=',' read -ra SUBNETS <<< "$SUBNET_INPUT"
SUBNET_1=${SUBNETS[0]// /}  # Remove spaces
SUBNET_2=${SUBNETS[1]// /}  # Remove spaces (optional)

if [ -z "$SUBNET_2" ]; then
    SUBNET_2=$SUBNET_1
fi

read -p "Enter security group ID: " SECURITY_GROUP
if [ -z "$SECURITY_GROUP" ]; then
    echo -e "${RED}Error: Security group ID is required${NC}"
    exit 1
fi

echo
echo -e "${BLUE}=== Configuration Summary ===${NC}"
echo -e "${BLUE}App Name:${NC} $APP_NAME"
echo -e "${BLUE}AWS Region:${NC} $AWS_REGION"
echo -e "${BLUE}ECS Cluster:${NC} $CLUSTER_NAME"
echo -e "${BLUE}Image URI:${NC} $IMAGE_URI"
echo -e "${BLUE}VPC ID:${NC} $VPC_ID"
echo -e "${BLUE}Subnets:${NC} $SUBNET_1, $SUBNET_2"
echo -e "${BLUE}Security Group:${NC} $SECURITY_GROUP"
echo

# Get AWS Account ID
echo -e "${BLUE}Getting AWS Account ID...${NC}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to get AWS Account ID${NC}"
    exit 1
fi
echo -e "${BLUE}AWS Account ID:${NC} $ACCOUNT_ID"
echo

# Check/Create ECS cluster
echo -e "${BLUE}Checking ECS cluster...${NC}"
aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION >/dev/null 2>&1 || {
    echo -e "${YELLOW}Creating ECS cluster: $CLUSTER_NAME${NC}"
    aws ecs create-cluster --cluster-name $CLUSTER_NAME --region $AWS_REGION
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Failed to create ECS cluster${NC}"
        exit 1
    fi
}

# Create CloudWatch log group
echo -e "${BLUE}Creating CloudWatch log group...${NC}"
aws logs create-log-group --log-group-name "/ecs/${APP_NAME}" --region $AWS_REGION 2>/dev/null || true

# Load balancer configuration
echo -e "${YELLOW}Do you need a load balancer for this service? (y/n):${NC}"
read -p "Load balancer: " USE_LB

if [[ $USE_LB =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}Creating Application Load Balancer and Target Group...${NC}"
    
    # Create ALB
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name "${APP_NAME}-alb" \
        --subnets $SUBNET_1 $SUBNET_2 \
        --security-groups $SECURITY_GROUP \
        --region $AWS_REGION \
        --query 'LoadBalancers[0].LoadBalancerArn' --output text)
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Failed to create load balancer${NC}"
        exit 1
    fi
    
    # Create Target Group
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name "${APP_NAME}-tg" \
        --protocol HTTP \
        --port 8080 \
        --vpc-id $VPC_ID \
        --target-type ip \
        --health-check-path "/mini-app/actuator/health" \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 5 \
        --region $AWS_REGION \
        --query 'TargetGroups[0].TargetGroupArn' --output text)
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Failed to create target group${NC}"
        exit 1
    fi
    
    # Create ALB Listener
    aws elbv2 create-listener \
        --load-balancer-arn $ALB_ARN \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn=$TARGET_GROUP_ARN \
        --region $AWS_REGION >/dev/null
    
    echo -e "${GREEN}✓ Load balancer created successfully${NC}"
    echo -e "${BLUE}Target Group ARN:${NC} $TARGET_GROUP_ARN"
else
    TARGET_GROUP_ARN=""
    echo -e "${YELLOW}Skipping load balancer creation${NC}"
fi

echo

# Store sensitive configuration in SSM Parameter Store
echo -e "${BLUE}Creating SSM parameters for sensitive configuration...${NC}"

# Create SSM parameters (these would typically be set with actual values)
aws ssm put-parameter --name "/mini-java-app/db-host" --value "your-rds-endpoint" --type "SecureString" --region $AWS_REGION --overwrite 2>/dev/null || true
aws ssm put-parameter --name "/mini-java-app/db-username" --value "admin" --type "SecureString" --region $AWS_REGION --overwrite 2>/dev/null || true
aws ssm put-parameter --name "/mini-java-app/db-password" --value "changeme123" --type "SecureString" --region $AWS_REGION --overwrite 2>/dev/null || true
aws ssm put-parameter --name "/mini-java-app/redis-host" --value "your-redis-endpoint" --type "SecureString" --region $AWS_REGION --overwrite 2>/dev/null || true
aws ssm put-parameter --name "/mini-java-app/redis-password" --value "redis-password" --type "SecureString" --region $AWS_REGION --overwrite 2>/dev/null || true
aws ssm put-parameter --name "/mini-java-app/external-api-key" --value "your-api-key" --type "SecureString" --region $AWS_REGION --overwrite 2>/dev/null || true
aws ssm put-parameter --name "/mini-java-app/jwt-secret" --value "jwt-secret-key-changeme" --type "SecureString" --region $AWS_REGION --overwrite 2>/dev/null || true
aws ssm put-parameter --name "/mini-java-app/admin-username" --value "admin" --type "SecureString" --region $AWS_REGION --overwrite 2>/dev/null || true
aws ssm put-parameter --name "/mini-java-app/admin-password" --value "admin-password-changeme" --type "SecureString" --region $AWS_REGION --overwrite 2>/dev/null || true

echo -e "${GREEN}✓ SSM parameters created${NC}"
echo

# Update JSON files with actual values
echo -e "${BLUE}Updating deployment files with configuration...${NC}"

# Update task definition
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" ecs/task-definition.json
sed -i "s|{{AWS_REGION}}|$AWS_REGION|g" ecs/task-definition.json
sed -i "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" ecs/task-definition.json

# Update service definition
sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" ecs/service-definition.json
sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" ecs/service-definition.json
sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" ecs/service-definition.json
sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" ecs/service-definition.json

if [ ! -z "$TARGET_GROUP_ARN" ]; then
    sed -i "s|{{TARGET_GROUP_ARN}}|$TARGET_GROUP_ARN|g" ecs/service-definition.json
else
    # Remove load balancer section if not using LB
    sed -i '/"loadBalancers":/,/],/d' ecs/service-definition.json
    sed -i '/"healthCheckGracePeriodSeconds":/d' ecs/service-definition.json
fi

# Register task definition
echo -e "${BLUE}Registering ECS task definition...${NC}"
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://ecs/task-definition.json \
    --region $AWS_REGION \
    --query 'taskDefinition.taskDefinitionArn' --output text)

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to register task definition${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Task definition registered successfully${NC}"
echo -e "${BLUE}Task Definition ARN:${NC} $TASK_DEF_ARN"
echo

# Check if service exists
echo -e "${BLUE}Checking if ECS service exists...${NC}"
SERVICE_EXISTS=$(aws ecs describe-services \
    --cluster $CLUSTER_NAME \
    --services $SERVICE_NAME \
    --region $AWS_REGION \
    --query 'services[0].serviceName' --output text 2>/dev/null)

if [ "$SERVICE_EXISTS" = "$SERVICE_NAME" ]; then
    # Update existing service
    echo -e "${YELLOW}Updating existing ECS service...${NC}"
    aws ecs update-service \
        --cluster $CLUSTER_NAME \
        --service $SERVICE_NAME \
        --task-definition "$TASK_DEF_ARN" \
        --region $AWS_REGION >/dev/null
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Failed to update service${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Service updated successfully${NC}"
else
    # Create new service
    echo -e "${YELLOW}Creating new ECS service...${NC}"
    aws ecs create-service \
        --cli-input-json file://ecs/service-definition.json \
        --region $AWS_REGION >/dev/null
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Failed to create service${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Service created successfully${NC}"
fi

echo

# Wait for service stability
echo -e "${BLUE}Waiting for service to become stable (this may take several minutes)...${NC}"
aws ecs wait services-stable --cluster $CLUSTER_NAME --services $SERVICE_NAME --region $AWS_REGION

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Service is stable and running${NC}"
else
    echo -e "${YELLOW}Warning: Service stability check timed out, but deployment may still be in progress${NC}"
fi

echo

# Display deployment information
echo -e "${GREEN}=== Deployment Completed ===${NC}"
echo -e "${BLUE}Cluster:${NC} $CLUSTER_NAME"
echo -e "${BLUE}Service:${NC} $SERVICE_NAME"
echo -e "${BLUE}Task Definition:${NC} $TASK_FAMILY"
echo -e "${BLUE}Image:${NC} $IMAGE_URI"
echo -e "${BLUE}CloudWatch Logs:${NC} /ecs/${APP_NAME}"

if [ ! -z "$TARGET_GROUP_ARN" ]; then
    # Get ALB DNS name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns $ALB_ARN \
        --region $AWS_REGION \
        --query 'LoadBalancers[0].DNSName' --output text)
    
    echo -e "${BLUE}Load Balancer:${NC} http://$ALB_DNS"
    echo -e "${BLUE}Health Check:${NC} http://$ALB_DNS/mini-app/actuator/health"
fi

echo
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Monitor deployment in AWS ECS console"
echo "2. Check CloudWatch logs for application startup"
echo "3. Update SSM parameters with actual database and service configurations"
echo "4. Configure DNS and SSL certificates for production use"
echo

echo -e "${GREEN}Deployment script completed successfully!${NC}"