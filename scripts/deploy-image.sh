#!/bin/bash

# Enable strict error handling
set -e
set -o pipefail

# ECS Fargate Deployment Script
echo "========================================"
echo "  AWS ECS Fargate Deployment Script"
echo "========================================"
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"
TASK_FAMILY="${PROJECT_NAME}-task"
SERVICE_NAME="${PROJECT_NAME}-service"
CONTAINER_NAME="${PROJECT_NAME}"
CONTAINER_PORT=8080

# Prompt for AWS configuration
echo "=== AWS Configuration ==="
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
read -p "Enter ECS Cluster Name: " CLUSTER_NAME

# Check if cluster exists, create if not
echo ""
echo "Checking ECS cluster..."
aws ecs describe-clusters --clusters "$CLUSTER_NAME" --region "$AWS_REGION" >/dev/null 2>&1 || {
    echo "Cluster does not exist. Creating ECS cluster: $CLUSTER_NAME"
    aws ecs create-cluster --cluster-name "$CLUSTER_NAME" --region "$AWS_REGION"
    echo "✓ ECS cluster created successfully"
}

echo "✓ ECS cluster verified: $CLUSTER_NAME"

# Get AWS Account ID
echo ""
echo "Fetching AWS Account ID..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

if [ -z "$ACCOUNT_ID" ]; then
    echo "ERROR: Failed to get AWS Account ID"
    exit 1
fi

echo "AWS Account ID: $ACCOUNT_ID"

# Prompt for network configuration
echo ""
echo "=== Network Configuration ==="
read -p "Enter VPC ID: " VPC_ID
read -p "Enter Subnet IDs (comma-separated, at least 2): " SUBNET_IDS
read -p "Enter Security Group ID: " SECURITY_GROUP

# Convert comma-separated subnets to array
IFS=',' read -ra SUBNET_ARRAY <<< "$SUBNET_IDS"
SUBNET_1="${SUBNET_ARRAY[0]}"
SUBNET_2="${SUBNET_ARRAY[1]}"

if [ -z "$SUBNET_1" ] || [ -z "$SUBNET_2" ]; then
    echo "ERROR: At least 2 subnets are required for Fargate"
    exit 1
fi

# Prompt for container image
echo ""
echo "=== Container Image ==="
read -p "Enter Docker Image URI: " IMAGE_URI

if [ -z "$IMAGE_URI" ]; then
    echo "ERROR: Image URI is required"
    exit 1
fi

# Prompt for external service configuration
echo ""
echo "=== External Service Configuration ==="
read -p "Enter Database Host: " DB_HOST
read -p "Enter Database User: " DB_USER
read -sp "Enter Database Password: " DB_PASSWORD
echo ""
read -p "Enter Redis Host: " REDIS_HOST
read -p "Enter RabbitMQ Host: " RABBITMQ_HOST

# Prompt for load balancer
echo ""
echo "=== Load Balancer Configuration ==="
read -p "Do you need a load balancer for this service? (y/n): " NEED_LB

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo "Creating Application Load Balancer and Target Group..."
    
    # Create Target Group with target-type ip (required for Fargate awsvpc mode)
    TG_NAME="${PROJECT_NAME}-tg"
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name "$TG_NAME" \
        --protocol HTTP \
        --port "$CONTAINER_PORT" \
        --vpc-id "$VPC_ID" \
        --target-type ip \
        --health-check-path "/mini-app/" \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --region "$AWS_REGION" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text 2>/dev/null || \
        aws elbv2 describe-target-groups \
            --names "$TG_NAME" \
            --region "$AWS_REGION" \
            --query 'TargetGroups[0].TargetGroupArn' \
            --output text)
    
    if [ -z "$TARGET_GROUP_ARN" ]; then
        echo "ERROR: Failed to create or retrieve Target Group"
        exit 1
    fi
    
    echo "✓ Target Group ARN: $TARGET_GROUP_ARN"
    
    # Create Application Load Balancer
    ALB_NAME="${PROJECT_NAME}-alb"
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name "$ALB_NAME" \
        --subnets "$SUBNET_1" "$SUBNET_2" \
        --security-groups "$SECURITY_GROUP" \
        --scheme internet-facing \
        --type application \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].LoadBalancerArn' \
        --output text 2>/dev/null || \
        aws elbv2 describe-load-balancers \
            --names "$ALB_NAME" \
            --region "$AWS_REGION" \
            --query 'LoadBalancers[0].LoadBalancerArn' \
            --output text)
    
    if [ -z "$ALB_ARN" ]; then
        echo "ERROR: Failed to create or retrieve Application Load Balancer"
        exit 1
    fi
    
    echo "✓ Application Load Balancer ARN: $ALB_ARN"
    
    # Create Listener
    LISTENER_ARN=$(aws elbv2 create-listener \
        --load-balancer-arn "$ALB_ARN" \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn="$TARGET_GROUP_ARN" \
        --region "$AWS_REGION" \
        --query 'Listeners[0].ListenerArn' \
        --output text 2>/dev/null || \
        aws elbv2 describe-listeners \
            --load-balancer-arn "$ALB_ARN" \
            --region "$AWS_REGION" \
            --query 'Listeners[0].ListenerArn' \
            --output text)
    
    echo "✓ Listener created: $LISTENER_ARN"
    
    # Get ALB DNS name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns "$ALB_ARN" \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    echo "✓ Load Balancer DNS: $ALB_DNS"
else
    echo "Skipping load balancer creation"
    TARGET_GROUP_ARN=""
fi

# Create CloudWatch Log Group
echo ""
echo "Creating CloudWatch Log Group..."
LOG_GROUP="/ecs/${PROJECT_NAME}"
aws logs create-log-group --log-group-name "$LOG_GROUP" --region "$AWS_REGION" 2>/dev/null || echo "Log group already exists"
echo "✓ CloudWatch Log Group: $LOG_GROUP"

# Replace placeholders in task definition
echo ""
echo "Preparing task definition..."
cp ecs/task-definition.json /tmp/task-definition.json

sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" /tmp/task-definition.json
sed -i "s|{{AWS_REGION}}|$AWS_REGION|g" /tmp/task-definition.json
sed -i "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" /tmp/task-definition.json
sed -i "s|{{DB_HOST}}|$DB_HOST|g" /tmp/task-definition.json
sed -i "s|{{DB_USER}}|$DB_USER|g" /tmp/task-definition.json
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" /tmp/task-definition.json
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" /tmp/task-definition.json
sed -i "s|{{RABBITMQ_HOST}}|$RABBITMQ_HOST|g" /tmp/task-definition.json

# Register task definition
echo "Registering task definition..."
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file:///tmp/task-definition.json \
    --region "$AWS_REGION" \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

if [ -z "$TASK_DEF_ARN" ]; then
    echo "ERROR: Failed to register task definition"
    exit 1
fi

echo "✓ Task Definition registered: $TASK_DEF_ARN"

# Prepare service definition
echo ""
echo "Preparing service definition..."
cp ecs/service-definition.json /tmp/service-definition.json

sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" /tmp/service-definition.json
sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" /tmp/service-definition.json
sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" /tmp/service-definition.json
sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" /tmp/service-definition.json

if [ -z "$TARGET_GROUP_ARN" ]; then
    # Remove loadBalancers section if no load balancer
    jq 'del(.loadBalancers) | del(.healthCheckGracePeriodSeconds)' /tmp/service-definition.json > /tmp/service-definition-final.json
    mv /tmp/service-definition-final.json /tmp/service-definition.json
else
    # Replace target group ARN
    sed -i "s|{{TARGET_GROUP_ARN}}|$TARGET_GROUP_ARN|g" /tmp/service-definition.json
fi

# Check if service exists
echo ""
echo "Checking if service exists..."
SERVICE_EXISTS=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[?status==`ACTIVE`].serviceName' \
    --output text)

if [ -z "$SERVICE_EXISTS" ] || [ "$SERVICE_EXISTS" = "None" ]; then
    echo "Service does not exist. Creating new service..."
    
    aws ecs create-service \
        --cli-input-json file:///tmp/service-definition.json \
        --region "$AWS_REGION" >/dev/null
    
    echo "✓ Service created: $SERVICE_NAME"
else
    echo "Service exists. Updating service..."
    
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service "$SERVICE_NAME" \
        --task-definition "$TASK_DEF_ARN" \
        --region "$AWS_REGION" >/dev/null
    
    echo "✓ Service updated: $SERVICE_NAME"
fi

# Wait for service to stabilize
echo ""
echo "Waiting for service to stabilize (this may take several minutes)..."
aws ecs wait services-stable \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION"

echo "✓ Service is stable"

# Display deployment summary
echo ""
echo "========================================"
echo "  Deployment Summary"
echo "========================================"
echo "Cluster: $CLUSTER_NAME"
echo "Service: $SERVICE_NAME"
echo "Task Definition: $TASK_DEF_ARN"
echo "Container: $CONTAINER_NAME"
echo "CloudWatch Logs: $LOG_GROUP"

if [ -n "$ALB_DNS" ]; then
    echo "Load Balancer DNS: http://$ALB_DNS"
    echo "Application URL: http://$ALB_DNS/mini-app/"
fi

echo ""
echo "✓ Deployment completed successfully!"
echo ""

# Verify running tasks
RUNNING_TASKS=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[0].runningCount' \
    --output text)

echo "Running tasks: $RUNNING_TASKS"
echo ""
echo "To view logs:"
echo "  aws logs tail $LOG_GROUP --follow --region $AWS_REGION"
echo ""
