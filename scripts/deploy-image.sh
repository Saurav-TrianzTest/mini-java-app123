#!/bin/bash

# Deploy Docker Image to AWS ECS Fargate
# This script deploys a containerized application to AWS ECS Fargate

set -e
set -o pipefail

echo "=========================================="
echo "AWS ECS Fargate Deployment Script"
echo "=========================================="
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"
TASK_FAMILY="mini-java-app-task"
SERVICE_NAME="mini-java-app-service"

# Prompt for AWS region
read -p "Enter AWS region (default: us-east-1): " AWS_REGION
AWS_REGION=${AWS_REGION:-us-east-1}

echo "Using AWS region: $AWS_REGION"
echo ""

# Get AWS Account ID
echo "Retrieving AWS Account ID..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

if [ -z "$ACCOUNT_ID" ]; then
    echo "Error: Failed to retrieve AWS Account ID. Please check your AWS credentials."
    exit 1
fi

echo "AWS Account ID: $ACCOUNT_ID"
echo ""

# Prompt for ECS cluster name
read -p "Enter ECS cluster name (default: mini-java-app-cluster): " CLUSTER_NAME
CLUSTER_NAME=${CLUSTER_NAME:-mini-java-app-cluster}

echo "Using cluster: $CLUSTER_NAME"
echo ""

# Check if cluster exists, create if not
echo "Checking if ECS cluster exists..."
aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION >/dev/null 2>&1 || {
    echo "Cluster does not exist. Creating ECS cluster..."
    aws ecs create-cluster --cluster-name $CLUSTER_NAME --region $AWS_REGION
    echo "ECS cluster created successfully"
}
echo ""

# Prompt for VPC configuration
echo "=== Network Configuration ==="
read -p "Enter VPC ID: " VPC_ID

if [ -z "$VPC_ID" ]; then
    echo "Error: VPC ID is required"
    exit 1
fi

# Prompt for subnets (comma-separated)
read -p "Enter subnet IDs (comma-separated, at least 2): " SUBNETS_INPUT
IFS=',' read -ra SUBNETS <<< "$SUBNETS_INPUT"

if [ ${#SUBNETS[@]} -lt 2 ]; then
    echo "Error: At least 2 subnets are required for high availability"
    exit 1
fi

SUBNET_1=$(echo ${SUBNETS[0]} | xargs)
SUBNET_2=$(echo ${SUBNETS[1]} | xargs)

echo "Using subnets: $SUBNET_1, $SUBNET_2"
echo ""

# Prompt for security group
read -p "Enter security group ID (must allow inbound traffic on port 8080): " SECURITY_GROUP

if [ -z "$SECURITY_GROUP" ]; then
    echo "Error: Security group ID is required"
    exit 1
fi

echo ""

# Prompt for Docker image URI
read -p "Enter Docker image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): " IMAGE_URI

if [ -z "$IMAGE_URI" ]; then
    echo "Error: Docker image URI is required"
    exit 1
fi

echo "Using image: $IMAGE_URI"
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
        --name $ALB_NAME \
        --subnets $SUBNET_1 $SUBNET_2 \
        --security-groups $SECURITY_GROUP \
        --scheme internet-facing \
        --type application \
        --ip-address-type ipv4 \
        --region $AWS_REGION \
        --query 'LoadBalancers[0].LoadBalancerArn' \
        --output text)
    
    if [ -z "$ALB_ARN" ]; then
        echo "Error: Failed to create Application Load Balancer"
        exit 1
    fi
    
    echo "ALB created: $ALB_ARN"
    
    # Get ALB DNS name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns $ALB_ARN \
        --region $AWS_REGION \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    echo "ALB DNS: $ALB_DNS"
    echo ""
    
    # Create Target Group with target-type ip (required for Fargate)
    TG_NAME="mini-java-app-tg"
    echo "Creating Target Group: $TG_NAME"
    
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name $TG_NAME \
        --protocol HTTP \
        --port 8080 \
        --vpc-id $VPC_ID \
        --target-type ip \
        --health-check-enabled \
        --health-check-protocol HTTP \
        --health-check-path /actuator/health \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --region $AWS_REGION \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text)
    
    if [ -z "$TARGET_GROUP_ARN" ]; then
        echo "Error: Failed to create Target Group"
        exit 1
    fi
    
    echo "Target Group created: $TARGET_GROUP_ARN"
    echo ""
    
    # Create Listener
    echo "Creating ALB Listener..."
    aws elbv2 create-listener \
        --load-balancer-arn $ALB_ARN \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn=$TARGET_GROUP_ARN \
        --region $AWS_REGION
    
    echo "Listener created successfully"
    echo ""
    
    # Update service definition with load balancer configuration
    sed -i "s|{{TARGET_GROUP_ARN}}|$TARGET_GROUP_ARN|g" ecs/service-definition.json
else
    echo "Skipping load balancer configuration"
    echo ""
    
    # Remove loadBalancers section from service definition
    python3 -c "
import json
with open('ecs/service-definition.json', 'r') as f:
    data = json.load(f)
if 'loadBalancers' in data:
    del data['loadBalancers']
if 'healthCheckGracePeriodSeconds' in data:
    del data['healthCheckGracePeriodSeconds']
with open('ecs/service-definition.json', 'w') as f:
    json.dump(data, f, indent=2)
" 2>/dev/null || {
        # Fallback if python3 is not available
        echo "Warning: Could not remove loadBalancers section. Please remove it manually if not needed."
    }
fi

# Prompt for environment variables from application.properties
echo "=== Application Configuration ==="
echo "Using environment variables from application.properties"
echo ""

read -p "Enter database host (default: localhost): " DB_HOST
DB_HOST=${DB_HOST:-localhost}

read -p "Enter database port (default: 3306): " DB_PORT
DB_PORT=${DB_PORT:-3306}

read -p "Enter database name (default: mini_app_db): " DB_NAME
DB_NAME=${DB_NAME:-mini_app_db}

read -p "Enter database username (default: root): " DB_USERNAME
DB_USERNAME=${DB_USERNAME:-root}

read -sp "Enter database password: " DB_PASSWORD
echo ""

read -p "Enter Redis host (default: localhost): " REDIS_HOST
REDIS_HOST=${REDIS_HOST:-localhost}

read -p "Enter Redis port (default: 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}

read -p "Enter S3 bucket name (default: mini-app-config-bucket): " S3_BUCKET_NAME
S3_BUCKET_NAME=${S3_BUCKET_NAME:-mini-app-config-bucket}

echo ""

# Create CloudWatch log group
echo "Creating CloudWatch log group..."
aws logs create-log-group --log-group-name /ecs/mini-java-app --region $AWS_REGION 2>/dev/null || echo "Log group already exists"
echo ""

# Replace placeholders in task definition
echo "Preparing task definition..."
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" ecs/task-definition.json
sed -i "s|{{AWS_REGION}}|$AWS_REGION|g" ecs/task-definition.json
sed -i "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" ecs/task-definition.json
sed -i "s|{{DB_HOST}}|$DB_HOST|g" ecs/task-definition.json
sed -i "s|{{DB_PORT}}|$DB_PORT|g" ecs/task-definition.json
sed -i "s|{{DB_NAME}}|$DB_NAME|g" ecs/task-definition.json
sed -i "s|{{DB_USERNAME}}|$DB_USERNAME|g" ecs/task-definition.json
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" ecs/task-definition.json
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" ecs/task-definition.json
sed -i "s|{{REDIS_PORT}}|$REDIS_PORT|g" ecs/task-definition.json
sed -i "s|{{S3_BUCKET_NAME}}|$S3_BUCKET_NAME|g" ecs/task-definition.json

echo "Task definition prepared"
echo ""

# Register task definition
echo "Registering ECS task definition..."
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://ecs/task-definition.json \
    --region $AWS_REGION \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

if [ -z "$TASK_DEF_ARN" ]; then
    echo "Error: Failed to register task definition"
    exit 1
fi

echo "Task definition registered: $TASK_DEF_ARN"
echo ""

# Replace placeholders in service definition
echo "Preparing service definition..."
sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" ecs/service-definition.json
sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" ecs/service-definition.json
sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" ecs/service-definition.json
sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" ecs/service-definition.json

echo "Service definition prepared"
echo ""

# Check if service exists
echo "Checking if ECS service exists..."
EXISTING_SERVICE=$(aws ecs describe-services \
    --cluster $CLUSTER_NAME \
    --services $SERVICE_NAME \
    --region $AWS_REGION \
    --query 'services[?status==`ACTIVE`].serviceName' \
    --output text)

if [ -z "$EXISTING_SERVICE" ] || [ "$EXISTING_SERVICE" == "None" ]; then
    echo "Service does not exist. Creating new service..."
    
    aws ecs create-service \
        --cli-input-json file://ecs/service-definition.json \
        --region $AWS_REGION
    
    echo "ECS service created successfully"
else
    echo "Service exists. Updating service..."
    
    aws ecs update-service \
        --cluster $CLUSTER_NAME \
        --service $SERVICE_NAME \
        --task-definition $TASK_DEF_ARN \
        --desired-count 2 \
        --region $AWS_REGION
    
    echo "ECS service updated successfully"
fi

echo ""

# Wait for service to become stable
echo "Waiting for service to become stable (this may take a few minutes)..."
aws ecs wait services-stable \
    --cluster $CLUSTER_NAME \
    --services $SERVICE_NAME \
    --region $AWS_REGION

echo "Service is stable"
echo ""

# Verify deployment
echo "=========================================="
echo "Deployment Verification"
echo "=========================================="

aws ecs describe-services \
    --cluster $CLUSTER_NAME \
    --services $SERVICE_NAME \
    --region $AWS_REGION \
    --query 'services[0].[serviceName,status,runningCount,desiredCount]' \
    --output table

echo ""
echo "=========================================="
echo "Deployment completed successfully!"
echo "=========================================="
echo "Cluster: $CLUSTER_NAME"
echo "Service: $SERVICE_NAME"
echo "Task Definition: $TASK_DEF_ARN"
echo "Region: $AWS_REGION"

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo ""
    echo "Application Load Balancer:"
    echo "  DNS: $ALB_DNS"
    echo "  Access your application at: http://$ALB_DNS"
fi

echo ""
echo "CloudWatch Logs:"
echo "  Log Group: /ecs/mini-java-app"
echo "  View logs: https://console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION#logsV2:log-groups/log-group//ecs/mini-java-app"
echo ""
echo "To view running tasks:"
echo "  aws ecs list-tasks --cluster $CLUSTER_NAME --service-name $SERVICE_NAME --region $AWS_REGION"
echo ""
