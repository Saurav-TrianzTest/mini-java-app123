#!/bin/bash

# ECS Fargate Deployment Script for Mini Java Application
# This script deploys the containerized application to AWS ECS Fargate

set -e
set -o pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Mini Java App - ECS Fargate Deployment${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Get AWS Account ID
echo -e "${GREEN}Retrieving AWS Account ID...${NC}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo -e "${YELLOW}Account ID: $ACCOUNT_ID${NC}"
echo ""

# Prompt for AWS Region
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
export AWS_DEFAULT_REGION=$AWS_REGION

# Prompt for ECS Cluster Name
read -p "Enter ECS Cluster Name: " CLUSTER_NAME

# Check if cluster exists, create if not
echo ""
echo -e "${GREEN}Checking if ECS cluster exists...${NC}"
CLUSTER_EXISTS=$(aws ecs describe-clusters --clusters "$CLUSTER_NAME" --query "clusters[0].clusterName" --output text 2>/dev/null || echo "None")

if [ "$CLUSTER_EXISTS" == "None" ] || [ -z "$CLUSTER_EXISTS" ]; then
    echo -e "${YELLOW}Cluster does not exist. Creating...${NC}"
    aws ecs create-cluster --cluster-name "$CLUSTER_NAME"
    echo -e "${GREEN}Cluster created successfully!${NC}"
else
    echo -e "${GREEN}Cluster exists: $CLUSTER_NAME${NC}"
fi

# Prompt for Network Configuration
echo ""
echo -e "${YELLOW}Network Configuration (Required for Fargate)${NC}"
read -p "Enter VPC ID: " VPC_ID
read -p "Enter Subnet IDs (comma-separated, at least 2): " SUBNETS_INPUT
read -p "Enter Security Group ID: " SECURITY_GROUP

# Parse subnets
IFS=',' read -ra SUBNET_ARRAY <<< "$SUBNETS_INPUT"
SUBNET_1=$(echo "${SUBNET_ARRAY[0]}" | xargs)
SUBNET_2=$(echo "${SUBNET_ARRAY[1]}" | xargs)

# Prompt for Image URI
echo ""
read -p "Enter ECR Image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): " IMAGE_URI

# Prompt for Load Balancer
echo ""
read -p "Do you need a load balancer for this service? (y/n): " NEED_LB

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${GREEN}Creating Application Load Balancer and Target Group...${NC}"
    
    # Create Target Group with target-type ip (required for Fargate awsvpc mode)
    TG_NAME="mini-java-app-tg-$(date +%s)"
    echo -e "${YELLOW}Creating Target Group: $TG_NAME${NC}"
    
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name "$TG_NAME" \
        --protocol HTTP \
        --port 8080 \
        --vpc-id "$VPC_ID" \
        --target-type ip \
        --health-check-enabled \
        --health-check-protocol HTTP \
        --health-check-path "/mini-app" \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text)
    
    echo -e "${GREEN}Target Group created: $TARGET_GROUP_ARN${NC}"
    
    # Create Application Load Balancer
    ALB_NAME="mini-java-app-alb-$(date +%s)"
    echo -e "${YELLOW}Creating Application Load Balancer: $ALB_NAME${NC}"
    
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name "$ALB_NAME" \
        --subnets "$SUBNET_1" "$SUBNET_2" \
        --security-groups "$SECURITY_GROUP" \
        --scheme internet-facing \
        --type application \
        --ip-address-type ipv4 \
        --query 'LoadBalancers[0].LoadBalancerArn' \
        --output text)
    
    echo -e "${GREEN}Load Balancer created: $ALB_ARN${NC}"
    
    # Get ALB DNS Name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns "$ALB_ARN" \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    # Create Listener
    echo -e "${YELLOW}Creating Listener...${NC}"
    aws elbv2 create-listener \
        --load-balancer-arn "$ALB_ARN" \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn="$TARGET_GROUP_ARN"
    
    echo -e "${GREEN}Listener created successfully!${NC}"
    
    # Update service definition with load balancer configuration
    sed -i "s|{{TARGET_GROUP_ARN}}|$TARGET_GROUP_ARN|g" ecs/service-definition.json
else
    # Remove load balancer section from service definition
    echo -e "${YELLOW}Removing load balancer configuration from service definition...${NC}"
    python3 -c "
import json
with open('ecs/service-definition.json', 'r') as f:
    service_def = json.load(f)
if 'loadBalancers' in service_def:
    del service_def['loadBalancers']
if 'healthCheckGracePeriodSeconds' in service_def:
    del service_def['healthCheckGracePeriodSeconds']
with open('ecs/service-definition.json', 'w') as f:
    json.dump(service_def, f, indent=2)
" 2>/dev/null || {
    # Fallback if python3 is not available
    echo -e "${YELLOW}Note: Manual removal of loadBalancers section may be required${NC}"
}
fi

# Prompt for environment variables from application.properties
echo ""
echo -e "${YELLOW}Application Configuration${NC}"
echo -e "${YELLOW}Using default values from application.properties${NC}"
echo -e "${YELLOW}You can customize these in the task definition JSON if needed${NC}"

# Database Configuration
DB_HOST="db.example.com"
DB_PORT="3306"
DB_NAME="mini_app_db"
DB_USERNAME="appuser"
DB_PASSWORD="changeme"

# Redis Configuration
REDIS_HOST="redis.example.com"
REDIS_PORT="6379"
REDIS_PASSWORD=""

# External API Configuration
EXTERNAL_API_URL="http://api.example.com:8080/v1"
EXTERNAL_API_KEY=""

# Payment Service Configuration
PAYMENT_SERVICE_URL="https://payment.service.local/process"
PAYMENT_SERVICE_USERNAME=""
PAYMENT_SERVICE_PASSWORD=""

# S3 Configuration
CONFIG_S3_BUCKET="app-config-bucket"
CONFIG_S3_KEY="config/app.properties"

# Security Configuration
JWT_SECRET=""
ADMIN_USERNAME=""
ADMIN_PASSWORD=""
ENCRYPTION_KEY=""

# Monitoring Configuration
MONITORING_ENDPOINT="http://monitoring.service.local:9090/metrics"
MONITORING_USERNAME=""
MONITORING_PASSWORD=""

# Messaging Configuration
RABBITMQ_HOST="rabbitmq.service.local"
RABBITMQ_PORT="5672"
RABBITMQ_USERNAME=""
RABBITMQ_PASSWORD=""

# Replace placeholders in task definition
echo ""
echo -e "${GREEN}Preparing task definition...${NC}"
cp ecs/task-definition.json ecs/task-definition-temp.json

sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" ecs/task-definition-temp.json
sed -i "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" ecs/task-definition-temp.json
sed -i "s|{{AWS_REGION}}|$AWS_REGION|g" ecs/task-definition-temp.json
sed -i "s|{{DB_HOST}}|$DB_HOST|g" ecs/task-definition-temp.json
sed -i "s|{{DB_PORT}}|$DB_PORT|g" ecs/task-definition-temp.json
sed -i "s|{{DB_NAME}}|$DB_NAME|g" ecs/task-definition-temp.json
sed -i "s|{{DB_USERNAME}}|$DB_USERNAME|g" ecs/task-definition-temp.json
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" ecs/task-definition-temp.json
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" ecs/task-definition-temp.json
sed -i "s|{{REDIS_PORT}}|$REDIS_PORT|g" ecs/task-definition-temp.json
sed -i "s|{{REDIS_PASSWORD}}|$REDIS_PASSWORD|g" ecs/task-definition-temp.json
sed -i "s|{{EXTERNAL_API_URL}}|$EXTERNAL_API_URL|g" ecs/task-definition-temp.json
sed -i "s|{{EXTERNAL_API_KEY}}|$EXTERNAL_API_KEY|g" ecs/task-definition-temp.json
sed -i "s|{{PAYMENT_SERVICE_URL}}|$PAYMENT_SERVICE_URL|g" ecs/task-definition-temp.json
sed -i "s|{{PAYMENT_SERVICE_USERNAME}}|$PAYMENT_SERVICE_USERNAME|g" ecs/task-definition-temp.json
sed -i "s|{{PAYMENT_SERVICE_PASSWORD}}|$PAYMENT_SERVICE_PASSWORD|g" ecs/task-definition-temp.json
sed -i "s|{{CONFIG_S3_BUCKET}}|$CONFIG_S3_BUCKET|g" ecs/task-definition-temp.json
sed -i "s|{{CONFIG_S3_KEY}}|$CONFIG_S3_KEY|g" ecs/task-definition-temp.json
sed -i "s|{{JWT_SECRET}}|$JWT_SECRET|g" ecs/task-definition-temp.json
sed -i "s|{{ADMIN_USERNAME}}|$ADMIN_USERNAME|g" ecs/task-definition-temp.json
sed -i "s|{{ADMIN_PASSWORD}}|$ADMIN_PASSWORD|g" ecs/task-definition-temp.json
sed -i "s|{{ENCRYPTION_KEY}}|$ENCRYPTION_KEY|g" ecs/task-definition-temp.json
sed -i "s|{{MONITORING_ENDPOINT}}|$MONITORING_ENDPOINT|g" ecs/task-definition-temp.json
sed -i "s|{{MONITORING_USERNAME}}|$MONITORING_USERNAME|g" ecs/task-definition-temp.json
sed -i "s|{{MONITORING_PASSWORD}}|$MONITORING_PASSWORD|g" ecs/task-definition-temp.json
sed -i "s|{{RABBITMQ_HOST}}|$RABBITMQ_HOST|g" ecs/task-definition-temp.json
sed -i "s|{{RABBITMQ_PORT}}|$RABBITMQ_PORT|g" ecs/task-definition-temp.json
sed -i "s|{{RABBITMQ_USERNAME}}|$RABBITMQ_USERNAME|g" ecs/task-definition-temp.json
sed -i "s|{{RABBITMQ_PASSWORD}}|$RABBITMQ_PASSWORD|g" ecs/task-definition-temp.json

# Create CloudWatch Log Group
echo ""
echo -e "${GREEN}Creating CloudWatch Log Group...${NC}"
aws logs create-log-group --log-group-name "/ecs/mini-java-app" 2>/dev/null || echo -e "${YELLOW}Log group already exists${NC}"

# Register task definition
echo ""
echo -e "${GREEN}Registering ECS task definition...${NC}"
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://ecs/task-definition-temp.json \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

echo -e "${GREEN}Task definition registered: $TASK_DEF_ARN${NC}"

# Clean up temp file
rm ecs/task-definition-temp.json

# Replace placeholders in service definition
echo ""
echo -e "${GREEN}Preparing service definition...${NC}"
cp ecs/service-definition.json ecs/service-definition-temp.json

sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" ecs/service-definition-temp.json
sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" ecs/service-definition-temp.json

# Check if service exists
SERVICE_NAME="mini-java-app-service"
echo ""
echo -e "${GREEN}Checking if service exists...${NC}"
SERVICE_EXISTS=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --query "services[?serviceName=='$SERVICE_NAME'].serviceName" \
    --output text 2>/dev/null || echo "")

if [ -z "$SERVICE_EXISTS" ] || [ "$SERVICE_EXISTS" == "None" ]; then
    # Create new service
    echo -e "${YELLOW}Service does not exist. Creating new service...${NC}"
    aws ecs create-service --cli-input-json file://ecs/service-definition-temp.json --cluster "$CLUSTER_NAME"
    echo -e "${GREEN}Service created successfully!${NC}"
else
    # Update existing service
    echo -e "${YELLOW}Service exists. Updating service...${NC}"
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service "$SERVICE_NAME" \
        --task-definition "$TASK_DEF_ARN" \
        --force-new-deployment
    echo -e "${GREEN}Service updated successfully!${NC}"
fi

# Clean up temp file
rm ecs/service-definition-temp.json

# Wait for service to stabilize
echo ""
echo -e "${GREEN}Waiting for service to stabilize...${NC}"
echo -e "${YELLOW}This may take several minutes...${NC}"
aws ecs wait services-stable --cluster "$CLUSTER_NAME" --services "$SERVICE_NAME"

# Verify deployment
echo ""
echo -e "${GREEN}Verifying deployment...${NC}"
aws ecs describe-services --cluster "$CLUSTER_NAME" --services "$SERVICE_NAME" --query 'services[0].[serviceName,status,runningCount,desiredCount]' --output table

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment Completed Successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}Deployment Details:${NC}"
echo -e "Cluster: $CLUSTER_NAME"
echo -e "Service: $SERVICE_NAME"
echo -e "Task Definition: $TASK_DEF_ARN"
echo -e "Region: $AWS_REGION"
echo ""

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Load Balancer Details:${NC}"
    echo -e "DNS Name: $ALB_DNS"
    echo -e "Access your application at: http://$ALB_DNS/mini-app"
    echo ""
fi

echo -e "${YELLOW}CloudWatch Logs:${NC}"
echo -e "Log Group: /ecs/mini-java-app"
echo -e "View logs: aws logs tail /ecs/mini-java-app --follow"
echo ""
echo -e "${YELLOW}Useful Commands:${NC}"
echo -e "View service: aws ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE_NAME"
echo -e "List tasks: aws ecs list-tasks --cluster $CLUSTER_NAME --service-name $SERVICE_NAME"
echo -e "Scale service: aws ecs update-service --cluster $CLUSTER_NAME --service $SERVICE_NAME --desired-count <count>"
echo ""
