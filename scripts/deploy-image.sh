#!/bin/bash

# ECS Fargate Deployment Script for mini-java-app
# This script deploys the Docker image to AWS ECS Fargate

set -e
set -o pipefail

echo "=========================================="
echo "AWS ECS Fargate Deployment Script"
echo "=========================================="
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"
SERVICE_NAME="mini-java-app-service"
TASK_FAMILY="mini-java-app-task"

echo "Project: $PROJECT_NAME"
echo ""

# Prompt for AWS region
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
export AWS_DEFAULT_REGION="$AWS_REGION"

echo "Using AWS region: $AWS_REGION"
echo ""

# Get AWS Account ID
echo "Retrieving AWS Account ID..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "AWS Account ID: $ACCOUNT_ID"
echo ""

# Prompt for ECS cluster name
read -p "Enter ECS cluster name (will be created if doesn't exist): " CLUSTER_NAME

echo ""
echo "Checking if ECS cluster exists..."
aws ecs describe-clusters --clusters "$CLUSTER_NAME" --region "$AWS_REGION" >/dev/null 2>&1 || {
    echo "Cluster does not exist. Creating ECS cluster: $CLUSTER_NAME"
    aws ecs create-cluster --cluster-name "$CLUSTER_NAME" --region "$AWS_REGION"
    echo "ECS cluster created successfully"
}
echo "ECS cluster ready: $CLUSTER_NAME"
echo ""

# Prompt for network configuration
echo "=== Network Configuration ==="
read -p "Enter VPC ID: " VPC_ID
read -p "Enter Subnet IDs (comma-separated, at least 2): " SUBNETS_INPUT
read -p "Enter Security Group ID: " SECURITY_GROUP

# Parse subnets
IFS=',' read -ra SUBNET_ARRAY <<< "$SUBNETS_INPUT"
SUBNET_1="${SUBNET_ARRAY[0]}"
SUBNET_2="${SUBNET_ARRAY[1]}"

echo ""
echo "VPC: $VPC_ID"
echo "Subnet 1: $SUBNET_1"
echo "Subnet 2: $SUBNET_2"
echo "Security Group: $SECURITY_GROUP"
echo ""

# Prompt for Docker image URI
read -p "Enter Docker image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): " IMAGE_URI

echo ""
echo "Image URI: $IMAGE_URI"
echo ""

# Load balancer configuration
read -p "Do you need a load balancer for this service? (y/n): " NEED_LB

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo ""
    echo "=== Creating Application Load Balancer ==="
    
    # Create ALB
    ALB_NAME="mini-java-app-alb"
    echo "Creating Application Load Balancer: $ALB_NAME"
    
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name "$ALB_NAME" \
        --subnets "$SUBNET_1" "$SUBNET_2" \
        --security-groups "$SECURITY_GROUP" \
        --scheme internet-facing \
        --type application \
        --ip-address-type ipv4 \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].LoadBalancerArn' \
        --output text 2>/dev/null || echo "")
    
    if [ -z "$ALB_ARN" ]; then
        echo "Load balancer may already exist, retrieving ARN..."
        ALB_ARN=$(aws elbv2 describe-load-balancers \
            --names "$ALB_NAME" \
            --region "$AWS_REGION" \
            --query 'LoadBalancers[0].LoadBalancerArn' \
            --output text)
    fi
    
    echo "Load Balancer ARN: $ALB_ARN"
    
    # Get ALB DNS name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns "$ALB_ARN" \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    echo "Load Balancer DNS: $ALB_DNS"
    echo ""
    
    # Create Target Group
    TG_NAME="mini-java-app-tg"
    echo "Creating Target Group: $TG_NAME"
    
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
        --region "$AWS_REGION" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text 2>/dev/null || echo "")
    
    if [ -z "$TARGET_GROUP_ARN" ]; then
        echo "Target group may already exist, retrieving ARN..."
        TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups \
            --names "$TG_NAME" \
            --region "$AWS_REGION" \
            --query 'TargetGroups[0].TargetGroupArn' \
            --output text)
    fi
    
    echo "Target Group ARN: $TARGET_GROUP_ARN"
    echo ""
    
    # Create Listener
    echo "Creating ALB Listener..."
    aws elbv2 create-listener \
        --load-balancer-arn "$ALB_ARN" \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn="$TARGET_GROUP_ARN" \
        --region "$AWS_REGION" >/dev/null 2>&1 || echo "Listener may already exist"
    
    echo "ALB Listener created"
    echo ""
    
    USE_LOAD_BALANCER=true
else
    echo "Skipping load balancer creation"
    USE_LOAD_BALANCER=false
    TARGET_GROUP_ARN=""
fi

# Create CloudWatch Log Group
echo "=== Creating CloudWatch Log Group ==="
LOG_GROUP="/ecs/$PROJECT_NAME"
aws logs create-log-group --log-group-name "$LOG_GROUP" --region "$AWS_REGION" 2>/dev/null || echo "Log group already exists"
echo "CloudWatch Log Group: $LOG_GROUP"
echo ""

# Prepare task definition
echo "=== Preparing Task Definition ==="
TASK_DEF_FILE="../ecs/task-definition.json"

# Create temporary task definition with replaced placeholders
TEMP_TASK_DEF=$(mktemp)
sed "s|{{IMAGE_URI}}|$IMAGE_URI|g; s|{{AWS_REGION}}|$AWS_REGION|g; s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" "$TASK_DEF_FILE" > "$TEMP_TASK_DEF"

echo "Task definition prepared"
echo ""

# Register task definition
echo "=== Registering Task Definition ==="
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://"$TEMP_TASK_DEF" \
    --region "$AWS_REGION" \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

echo "Task Definition registered: $TASK_DEF_ARN"
echo ""

# Clean up temporary file
rm -f "$TEMP_TASK_DEF"

# Prepare service definition
echo "=== Preparing Service Definition ==="
SERVICE_DEF_FILE="../ecs/service-definition.json"

# Create temporary service definition with replaced placeholders
TEMP_SERVICE_DEF=$(mktemp)
sed "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g; s|{{SUBNET_1}}|$SUBNET_1|g; s|{{SUBNET_2}}|$SUBNET_2|g; s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g; s|{{TARGET_GROUP_ARN}}|$TARGET_GROUP_ARN|g" "$SERVICE_DEF_FILE" > "$TEMP_SERVICE_DEF"

# Remove loadBalancers section if not using load balancer
if [ "$USE_LOAD_BALANCER" = false ]; then
    # Remove loadBalancers and healthCheckGracePeriodSeconds from service definition
    python3 -c "
import json
import sys
with open('$TEMP_SERVICE_DEF', 'r') as f:
    data = json.load(f)
data.pop('loadBalancers', None)
data.pop('healthCheckGracePeriodSeconds', None)
with open('$TEMP_SERVICE_DEF', 'w') as f:
    json.dump(data, f, indent=2)
" 2>/dev/null || {
    # Fallback if python3 is not available
    jq 'del(.loadBalancers, .healthCheckGracePeriodSeconds)' "$TEMP_SERVICE_DEF" > "${TEMP_SERVICE_DEF}.tmp" && mv "${TEMP_SERVICE_DEF}.tmp" "$TEMP_SERVICE_DEF"
}
fi

echo "Service definition prepared"
echo ""

# Check if service exists
echo "=== Checking if Service Exists ==="
EXISTING_SERVICE=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[?status==`ACTIVE`].serviceName' \
    --output text 2>/dev/null || echo "")

if [ -z "$EXISTING_SERVICE" ] || [ "$EXISTING_SERVICE" = "None" ]; then
    echo "Service does not exist. Creating new service..."
    
    # Create service
    aws ecs create-service \
        --cli-input-json file://"$TEMP_SERVICE_DEF" \
        --region "$AWS_REGION"
    
    echo "Service created: $SERVICE_NAME"
else
    echo "Service exists. Updating service..."
    
    # Update service with new task definition
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service "$SERVICE_NAME" \
        --task-definition "$TASK_DEF_ARN" \
        --force-new-deployment \
        --region "$AWS_REGION"
    
    echo "Service updated: $SERVICE_NAME"
fi

echo ""

# Clean up temporary file
rm -f "$TEMP_SERVICE_DEF"

# Wait for service to stabilize
echo "=== Waiting for Service to Stabilize ==="
echo "This may take several minutes..."
aws ecs wait services-stable \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION"

echo "Service is stable"
echo ""

# Verify deployment
echo "=== Deployment Verification ==="
aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[0].{ServiceName:serviceName,Status:status,DesiredCount:desiredCount,RunningCount:runningCount,TaskDefinition:taskDefinition}' \
    --output table

echo ""
echo "=========================================="
echo "DEPLOYMENT SUCCESSFUL!"
echo "=========================================="
echo ""
echo "Service Details:"
echo "  Cluster: $CLUSTER_NAME"
echo "  Service: $SERVICE_NAME"
echo "  Task Definition: $TASK_DEF_ARN"
echo "  Region: $AWS_REGION"
echo ""

if [ "$USE_LOAD_BALANCER" = true ]; then
    echo "Application URL:"
    echo "  http://$ALB_DNS/mini-app"
    echo ""
fi

echo "CloudWatch Logs:"
echo "  Log Group: $LOG_GROUP"
echo "  View logs: aws logs tail $LOG_GROUP --follow --region $AWS_REGION"
echo ""
echo "To view running tasks:"
echo "  aws ecs list-tasks --cluster $CLUSTER_NAME --service-name $SERVICE_NAME --region $AWS_REGION"
echo ""
