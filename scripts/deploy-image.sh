#!/bin/bash
set -e
set -o pipefail

# AWS ECS Fargate Deployment Script for mini-java-app
# This script deploys a containerized Java application to AWS ECS Fargate

echo "====================================="
echo "AWS ECS Fargate Deployment Script"
echo "====================================="
echo ""

# Configuration
PROJECT_NAME="mini-java-app"
TASK_FAMILY="${PROJECT_NAME}-task"
SERVICE_NAME="${PROJECT_NAME}-service"

# Prompt for AWS configuration
echo "--- AWS Configuration ---"
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
read -p "Enter ECS cluster name: " CLUSTER_NAME
echo ""

# Get AWS Account ID
echo "Retrieving AWS Account ID..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to retrieve AWS Account ID. Please check your AWS credentials."
    exit 1
fi
echo "AWS Account ID: $ACCOUNT_ID"
echo ""

# Check if cluster exists, create if not
echo "Checking if ECS cluster exists..."
aws ecs describe-clusters --clusters "$CLUSTER_NAME" --region "$AWS_REGION" --query "clusters[0].clusterName" --output text 2>/dev/null | grep -q "$CLUSTER_NAME"
if [ $? -ne 0 ]; then
    echo "Cluster does not exist. Creating ECS cluster: $CLUSTER_NAME"
    aws ecs create-cluster --cluster-name "$CLUSTER_NAME" --region "$AWS_REGION"
    echo "ECS cluster created successfully"
else
    echo "ECS cluster already exists"
fi
echo ""

# Prompt for network configuration
echo "--- Network Configuration ---"
read -p "Enter VPC ID: " VPC_ID
read -p "Enter subnet IDs (comma-separated, at least 2): " SUBNET_INPUT
read -p "Enter security group ID: " SECURITY_GROUP
echo ""

# Parse subnets
IFS=',' read -ra SUBNETS <<< "$SUBNET_INPUT"
SUBNET_1=${SUBNETS[0]}
SUBNET_2=${SUBNETS[1]:-$SUBNET_1}

# Trim whitespace
SUBNET_1=$(echo "$SUBNET_1" | xargs)
SUBNET_2=$(echo "$SUBNET_2" | xargs)
SECURITY_GROUP=$(echo "$SECURITY_GROUP" | xargs)

# Prompt for image URI
echo "--- Container Image ---"
read -p "Enter ECR image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): " IMAGE_URI
echo ""

# Prompt for environment variables
echo "--- Database Configuration ---"
read -p "Enter database host: " DB_HOST
read -p "Enter database port (default: 3306): " DB_PORT
DB_PORT=${DB_PORT:-3306}
read -p "Enter database name: " DB_NAME
read -p "Enter database username: " DB_USERNAME
read -sp "Enter database password: " DB_PASSWORD
echo ""
echo ""

echo "--- Redis Configuration ---"
read -p "Enter Redis host: " REDIS_HOST
read -p "Enter Redis port (default: 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}
echo ""

echo "--- External API Configuration ---"
read -p "Enter external API URL: " EXTERNAL_API_URL
echo ""

echo "--- Security Configuration ---"
read -sp "Enter JWT secret: " JWT_SECRET
echo ""
read -sp "Enter encryption key: " ENCRYPTION_KEY
echo ""
echo ""

# Create CloudWatch log group
echo "Creating CloudWatch log group..."
LOG_GROUP="/ecs/${PROJECT_NAME}"
aws logs create-log-group --log-group-name "$LOG_GROUP" --region "$AWS_REGION" 2>/dev/null || echo "Log group already exists"
echo ""

# Ask about load balancer
echo "--- Load Balancer Configuration ---"
read -p "Do you need a load balancer for this service? (y/n): " NEED_LB
echo ""

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo "Creating Application Load Balancer and Target Group..."
    
    # Create target group with target-type ip (required for Fargate awsvpc mode)
    TG_NAME="${PROJECT_NAME}-tg"
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name "$TG_NAME" \
        --protocol HTTP \
        --port 8080 \
        --vpc-id "$VPC_ID" \
        --target-type ip \
        --health-check-enabled \
        --health-check-protocol HTTP \
        --health-check-path "/actuator/health" \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --region "$AWS_REGION" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text 2>/dev/null)
    
    if [ -z "$TARGET_GROUP_ARN" ]; then
        # Target group might already exist, try to describe it
        TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups \
            --names "$TG_NAME" \
            --region "$AWS_REGION" \
            --query 'TargetGroups[0].TargetGroupArn' \
            --output text 2>/dev/null)
    fi
    
    echo "Target Group ARN: $TARGET_GROUP_ARN"
    
    # Create ALB
    ALB_NAME="${PROJECT_NAME}-alb"
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name "$ALB_NAME" \
        --subnets "$SUBNET_1" "$SUBNET_2" \
        --security-groups "$SECURITY_GROUP" \
        --scheme internet-facing \
        --type application \
        --ip-address-type ipv4 \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].LoadBalancerArn' \
        --output text 2>/dev/null)
    
    if [ -z "$ALB_ARN" ]; then
        # ALB might already exist
        ALB_ARN=$(aws elbv2 describe-load-balancers \
            --names "$ALB_NAME" \
            --region "$AWS_REGION" \
            --query 'LoadBalancers[0].LoadBalancerArn' \
            --output text 2>/dev/null)
    fi
    
    echo "Load Balancer ARN: $ALB_ARN"
    
    # Get ALB DNS name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns "$ALB_ARN" \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    # Create listener
    aws elbv2 create-listener \
        --load-balancer-arn "$ALB_ARN" \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn="$TARGET_GROUP_ARN" \
        --region "$AWS_REGION" 2>/dev/null || echo "Listener may already exist"
    
    echo "Load balancer created successfully"
    echo ""
else
    TARGET_GROUP_ARN=""
    echo "Skipping load balancer creation"
    echo ""
fi

# Replace placeholders in task definition
echo "Preparing task definition..."
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
sed -i "s|{{EXTERNAL_API_URL}}|$EXTERNAL_API_URL|g" ecs/task-definition-temp.json
sed -i "s|{{JWT_SECRET}}|$JWT_SECRET|g" ecs/task-definition-temp.json
sed -i "s|{{ENCRYPTION_KEY}}|$ENCRYPTION_KEY|g" ecs/task-definition-temp.json

echo "Task definition prepared"
echo ""

# Register task definition
echo "Registering task definition..."
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://ecs/task-definition-temp.json \
    --region "$AWS_REGION" \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to register task definition."
    rm -f ecs/task-definition-temp.json
    exit 1
fi

echo "Task definition registered: $TASK_DEF_ARN"
rm -f ecs/task-definition-temp.json
echo ""

# Prepare service definition
echo "Preparing service definition..."
cp ecs/service-definition.json ecs/service-definition-temp.json

sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" ecs/service-definition-temp.json
sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" ecs/service-definition-temp.json

if [ -z "$TARGET_GROUP_ARN" ]; then
    # Remove loadBalancers section if no load balancer
    jq 'del(.loadBalancers) | del(.healthCheckGracePeriodSeconds)' ecs/service-definition-temp.json > ecs/service-definition-temp2.json
    mv ecs/service-definition-temp2.json ecs/service-definition-temp.json
else
    sed -i "s|{{TARGET_GROUP_ARN}}|$TARGET_GROUP_ARN|g" ecs/service-definition-temp.json
fi

echo "Service definition prepared"
echo ""

# Check if service exists
echo "Checking if service exists..."
EXISTING_SERVICE=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[0].serviceName' \
    --output text 2>/dev/null)

if [ "$EXISTING_SERVICE" = "$SERVICE_NAME" ]; then
    echo "Service exists. Updating service..."
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service "$SERVICE_NAME" \
        --task-definition "$TASK_DEF_ARN" \
        --region "$AWS_REGION" \
        --force-new-deployment
    
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to update service."
        rm -f ecs/service-definition-temp.json
        exit 1
    fi
    
    echo "Service updated successfully"
else
    echo "Service does not exist. Creating service..."
    aws ecs create-service \
        --cli-input-json file://ecs/service-definition-temp.json \
        --region "$AWS_REGION"
    
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to create service."
        rm -f ecs/service-definition-temp.json
        exit 1
    fi
    
    echo "Service created successfully"
fi

rm -f ecs/service-definition-temp.json
echo ""

# Wait for service stability
echo "Waiting for service to become stable..."
echo "This may take several minutes..."
aws ecs wait services-stable \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION"

if [ $? -eq 0 ]; then
    echo "Service is stable"
else
    echo "WARNING: Service stability check timed out or failed"
fi
echo ""

# Display service information
echo "====================================="
echo "Deployment Summary"
echo "====================================="
echo "Cluster: $CLUSTER_NAME"
echo "Service: $SERVICE_NAME"
echo "Task Definition: $TASK_DEF_ARN"
echo "Region: $AWS_REGION"
if [ -n "$ALB_DNS" ]; then
    echo "Load Balancer DNS: http://$ALB_DNS"
fi
echo "CloudWatch Logs: $LOG_GROUP"
echo ""

# Get running tasks
RUNNING_COUNT=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[0].runningCount' \
    --output text)

echo "Running tasks: $RUNNING_COUNT"
echo ""

echo "====================================="
echo "Deployment completed successfully!"
echo "====================================="
echo ""
echo "View logs with:"
echo "aws logs tail $LOG_GROUP --follow --region $AWS_REGION"
echo ""
