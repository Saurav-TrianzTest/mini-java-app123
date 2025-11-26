#!/bin/bash
set -e
set -o pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== AWS ECS Fargate Deployment Script ===${NC}"
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"
TASK_FAMILY="mini-java-app-task"
SERVICE_NAME="mini-java-app-service"
CONTAINER_NAME="mini-java-app"

# Prompt for deployment configuration
echo -e "${YELLOW}=== AWS Configuration ===${NC}"
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
read -p "Enter ECS Cluster Name: " CLUSTER_NAME

echo -e "\n${YELLOW}=== Network Configuration ===${NC}"
read -p "Enter VPC ID: " VPC_ID
read -p "Enter Subnet IDs (comma-separated, at least 2): " SUBNET_IDS
read -p "Enter Security Group ID: " SECURITY_GROUP

# Convert comma-separated subnets to array
IFS=',' read -ra SUBNET_ARRAY <<< "$SUBNET_IDS"
SUBNET_1="${SUBNET_ARRAY[0]}"
SUBNET_2="${SUBNET_ARRAY[1]:-$SUBNET_1}"

echo -e "\n${YELLOW}=== Image Configuration ===${NC}"
read -p "Enter Docker Image URI: " IMAGE_URI

echo -e "\n${YELLOW}=== External Service Configuration ===${NC}"
read -p "Enter Database Host: " DB_HOST
read -p "Enter Database Port (default: 3306): " DB_PORT
DB_PORT=${DB_PORT:-3306}
read -p "Enter Database Name: " DB_NAME
read -p "Enter Database Username: " DB_USERNAME
read -sp "Enter Database Password: " DB_PASSWORD
echo ""

read -p "Enter Redis Host: " REDIS_HOST
read -p "Enter Redis Port (default: 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}
read -sp "Enter Redis Password: " REDIS_PASSWORD
echo ""

read -p "Enter External API Base URL: " EXTERNAL_API_BASE_URL
read -p "Enter External API Key: " EXTERNAL_API_KEY

read -p "Enter JWT Secret: " JWT_SECRET
read -p "Enter Admin Username: " ADMIN_USERNAME
read -sp "Enter Admin Password: " ADMIN_PASSWORD
echo ""

read -p "Enter Monitoring Endpoint: " MONITORING_ENDPOINT

# Get AWS Account ID
echo -e "\n${YELLOW}Retrieving AWS Account ID...${NC}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo -e "${GREEN}Account ID: $ACCOUNT_ID${NC}"

# Check/create ECS cluster
echo -e "\n${YELLOW}Checking ECS cluster...${NC}"
aws ecs describe-clusters --clusters "$CLUSTER_NAME" --region "$AWS_REGION" >/dev/null 2>&1 || {
    echo -e "${YELLOW}Cluster does not exist. Creating cluster...${NC}"
    aws ecs create-cluster --cluster-name "$CLUSTER_NAME" --region "$AWS_REGION"
    echo -e "${GREEN}Cluster created successfully${NC}"
}

# Ask about load balancer
echo -e "\n${YELLOW}=== Load Balancer Configuration ===${NC}"
read -p "Do you need a load balancer for this service? (y/n): " NEED_LB

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo -e "\n${YELLOW}Creating Application Load Balancer and Target Group...${NC}"
    
    # Create target group with target-type ip (required for Fargate awsvpc)
    TARGET_GROUP_NAME="${PROJECT_NAME}-tg"
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name "$TARGET_GROUP_NAME" \
        --protocol HTTP \
        --port 8080 \
        --vpc-id "$VPC_ID" \
        --target-type ip \
        --health-check-path "/health" \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --region "$AWS_REGION" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text 2>/dev/null || aws elbv2 describe-target-groups \
        --names "$TARGET_GROUP_NAME" \
        --region "$AWS_REGION" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text)
    
    echo -e "${GREEN}Target Group ARN: $TARGET_GROUP_ARN${NC}"
    
    # Update service definition with load balancer
    SERVICE_DEF_FILE="ecs/service-definition.json"
else
    # Remove load balancer section from service definition
    echo -e "${YELLOW}Skipping load balancer configuration${NC}"
    SERVICE_DEF_FILE="ecs/service-definition-no-lb.json"
    
    # Create a temporary service definition without load balancer
    jq 'del(.loadBalancers, .healthCheckGracePeriodSeconds)' ecs/service-definition.json > "$SERVICE_DEF_FILE"
fi

# Replace placeholders in task definition
echo -e "\n${YELLOW}Preparing task definition...${NC}"
cp ecs/task-definition.json ecs/task-definition-temp.json

sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" ecs/task-definition-temp.json
sed -i "s|{{AWS_REGION}}|$AWS_REGION|g" ecs/task-definition-temp.json
sed -i "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" ecs/task-definition-temp.json
sed -i "s|{{DB_HOST}}|$DB_HOST|g" ecs/task-definition-temp.json
sed -i "s|{{DB_PORT}}|$DB_PORT|g" ecs/task-definition-temp.json
sed -i "s|{{DB_NAME}}|$DB_NAME|g" ecs/task-definition-temp.json
sed -i "s|{{DB_USERNAME}}|$DB_USERNAME|g" ecs/task-definition-temp.json
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" ecs/task-definition-temp.json
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" ecs/task-definition-temp.json
sed -i "s|{{REDIS_PORT}}|$REDIS_PORT|g" ecs/task-definition-temp.json
sed -i "s|{{REDIS_PASSWORD}}|$REDIS_PASSWORD|g" ecs/task-definition-temp.json
sed -i "s|{{EXTERNAL_API_BASE_URL}}|$EXTERNAL_API_BASE_URL|g" ecs/task-definition-temp.json
sed -i "s|{{EXTERNAL_API_KEY}}|$EXTERNAL_API_KEY|g" ecs/task-definition-temp.json
sed -i "s|{{JWT_SECRET}}|$JWT_SECRET|g" ecs/task-definition-temp.json
sed -i "s|{{ADMIN_USERNAME}}|$ADMIN_USERNAME|g" ecs/task-definition-temp.json
sed -i "s|{{ADMIN_PASSWORD}}|$ADMIN_PASSWORD|g" ecs/task-definition-temp.json
sed -i "s|{{MONITORING_ENDPOINT}}|$MONITORING_ENDPOINT|g" ecs/task-definition-temp.json

# Register task definition
echo -e "\n${YELLOW}Registering task definition...${NC}"
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://ecs/task-definition-temp.json \
    --region "$AWS_REGION" \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to register task definition${NC}"
    rm -f ecs/task-definition-temp.json
    exit 1
fi

echo -e "${GREEN}Task definition registered: $TASK_DEF_ARN${NC}"

# Replace placeholders in service definition
echo -e "\n${YELLOW}Preparing service definition...${NC}"
cp "$SERVICE_DEF_FILE" ecs/service-definition-temp.json

sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" ecs/service-definition-temp.json
sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" ecs/service-definition-temp.json

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    sed -i "s|{{TARGET_GROUP_ARN}}|$TARGET_GROUP_ARN|g" ecs/service-definition-temp.json
fi

# Check if service exists
echo -e "\n${YELLOW}Checking if service exists...${NC}"
SERVICE_EXISTS=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[0].serviceName' \
    --output text 2>/dev/null)

if [ "$SERVICE_EXISTS" = "$SERVICE_NAME" ]; then
    echo -e "${YELLOW}Service exists. Updating service...${NC}"
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service "$SERVICE_NAME" \
        --task-definition "$TASK_DEF_ARN" \
        --region "$AWS_REGION" \
        --force-new-deployment
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to update service${NC}"
        rm -f ecs/task-definition-temp.json ecs/service-definition-temp.json
        exit 1
    fi
    
    echo -e "${GREEN}Service updated successfully${NC}"
else
    echo -e "${YELLOW}Service does not exist. Creating service...${NC}"
    aws ecs create-service \
        --cli-input-json file://ecs/service-definition-temp.json \
        --region "$AWS_REGION"
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to create service${NC}"
        rm -f ecs/task-definition-temp.json ecs/service-definition-temp.json
        exit 1
    fi
    
    echo -e "${GREEN}Service created successfully${NC}"
fi

# Wait for service stability
echo -e "\n${YELLOW}Waiting for service to become stable...${NC}"
aws ecs wait services-stable \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Service is stable${NC}"
else
    echo -e "${YELLOW}Warning: Service stability check timed out or failed${NC}"
fi

# Verify deployment
echo -e "\n${YELLOW}Verifying deployment...${NC}"
aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[0].[serviceName,status,runningCount,desiredCount]' \
    --output table

# Display access information
if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo -e "\n${GREEN}=== Load Balancer Information ===${NC}"
    LB_DNS=$(aws elbv2 describe-load-balancers \
        --region "$AWS_REGION" \
        --query "LoadBalancers[?VpcId=='$VPC_ID'].DNSName" \
        --output text | head -n 1)
    
    if [ -n "$LB_DNS" ]; then
        echo -e "${GREEN}Application URL: http://$LB_DNS${NC}"
    fi
fi

echo -e "\n${GREEN}=== CloudWatch Logs ===${NC}"
echo -e "${GREEN}Log Group: /ecs/$PROJECT_NAME${NC}"
echo -e "${GREEN}View logs: aws logs tail /ecs/$PROJECT_NAME --follow --region $AWS_REGION${NC}"

# Cleanup
rm -f ecs/task-definition-temp.json ecs/service-definition-temp.json ecs/service-definition-no-lb.json

echo -e "\n${GREEN}=== Deployment Completed Successfully ===${NC}"
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Monitor service status: aws ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE_NAME --region $AWS_REGION"
echo "2. View running tasks: aws ecs list-tasks --cluster $CLUSTER_NAME --service-name $SERVICE_NAME --region $AWS_REGION"
echo "3. Check application logs: aws logs tail /ecs/$PROJECT_NAME --follow --region $AWS_REGION"
