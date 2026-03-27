#!/bin/bash
set -e
set -o pipefail

# ECS Fargate Deployment Script for mini-java-app
# This script deploys the Docker image to AWS ECS Fargate

echo "=========================================="
echo "AWS ECS Fargate Deployment Script"
echo "=========================================="
echo ""

# Prompt for AWS configuration
echo "=== AWS Configuration ==="
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
read -p "Enter ECS Cluster Name: " CLUSTER_NAME
read -p "Enter Docker Image URI (from ECR or Docker Hub): " IMAGE_URI

echo ""
echo "=== Network Configuration ==="
read -p "Enter VPC ID: " VPC_ID
read -p "Enter Subnet IDs (comma-separated, at least 2): " SUBNET_IDS
read -p "Enter Security Group ID: " SECURITY_GROUP

# Parse subnet IDs
IFS=',' read -ra SUBNETS <<< "$SUBNET_IDS"
SUBNET_1="${SUBNETS[0]}"
SUBNET_2="${SUBNETS[1]}"

# Trim whitespace
SUBNET_1=$(echo "$SUBNET_1" | xargs)
SUBNET_2=$(echo "$SUBNET_2" | xargs)

echo ""
echo "=== Load Balancer Configuration ==="
read -p "Do you need a load balancer for this service? (y/n): " NEED_LB

# Get AWS Account ID
echo ""
echo "=== Getting AWS Account ID ==="
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "Account ID: $ACCOUNT_ID"

# Check/Create ECS Cluster
echo ""
echo "=== Checking ECS Cluster ==="
aws ecs describe-clusters --clusters "$CLUSTER_NAME" --region "$AWS_REGION" >/dev/null 2>&1 || {
    echo "Creating ECS cluster: $CLUSTER_NAME"
    aws ecs create-cluster --cluster-name "$CLUSTER_NAME" --region "$AWS_REGION"
}
echo "ECS cluster ready: $CLUSTER_NAME"

# Create CloudWatch Log Group
echo ""
echo "=== Creating CloudWatch Log Group ==="
aws logs create-log-group --log-group-name "/ecs/mini-java-app" --region "$AWS_REGION" 2>/dev/null || echo "Log group already exists"

# Prepare task definition
echo ""
echo "=== Preparing Task Definition ==="
cp ecs/task-definition.json ecs/task-definition-temp.json

# Replace placeholders
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" ecs/task-definition-temp.json
sed -i "s|{{AWS_REGION}}|$AWS_REGION|g" ecs/task-definition-temp.json
sed -i "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" ecs/task-definition-temp.json

# Register task definition
echo ""
echo "=== Registering Task Definition ==="
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://ecs/task-definition-temp.json \
    --region "$AWS_REGION" \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

echo "Task definition registered: $TASK_DEF_ARN"

# Prepare service definition
echo ""
echo "=== Preparing Service Definition ==="
cp ecs/service-definition.json ecs/service-definition-temp.json

# Replace placeholders
sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" ecs/service-definition-temp.json
sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" ecs/service-definition-temp.json

# Handle load balancer
if [[ "$NEED_LB" == "y" || "$NEED_LB" == "Y" ]]; then
    echo ""
    echo "=== Creating Application Load Balancer ==="
    
    # Create ALB
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name mini-java-app-alb \
        --subnets "$SUBNET_1" "$SUBNET_2" \
        --security-groups "$SECURITY_GROUP" \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].LoadBalancerArn' \
        --output text 2>/dev/null || echo "")
    
    if [ -z "$ALB_ARN" ]; then
        echo "Load balancer may already exist, retrieving ARN..."
        ALB_ARN=$(aws elbv2 describe-load-balancers \
            --names mini-java-app-alb \
            --region "$AWS_REGION" \
            --query 'LoadBalancers[0].LoadBalancerArn' \
            --output text)
    fi
    
    echo "Load Balancer ARN: $ALB_ARN"
    
    # Create Target Group
    echo "Creating Target Group..."
    TG_ARN=$(aws elbv2 create-target-group \
        --name mini-java-app-tg \
        --protocol HTTP \
        --port 8080 \
        --vpc-id "$VPC_ID" \
        --target-type ip \
        --health-check-path "/mini-app" \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --region "$AWS_REGION" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text 2>/dev/null || \
        aws elbv2 describe-target-groups \
            --names mini-java-app-tg \
            --region "$AWS_REGION" \
            --query 'TargetGroups[0].TargetGroupArn' \
            --output text)
    
    echo "Target Group ARN: $TG_ARN"
    
    # Create Listener
    echo "Creating ALB Listener..."
    aws elbv2 create-listener \
        --load-balancer-arn "$ALB_ARN" \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn="$TG_ARN" \
        --region "$AWS_REGION" 2>/dev/null || echo "Listener may already exist"
    
    # Add load balancer configuration to service definition
    jq --arg tg "$TG_ARN" '.loadBalancers = [{"targetGroupArn": $tg, "containerName": "mini-java-app", "containerPort": 8080}] | .healthCheckGracePeriodSeconds = 300' \
        ecs/service-definition-temp.json > ecs/service-definition-temp2.json
    mv ecs/service-definition-temp2.json ecs/service-definition-temp.json
    
    # Get ALB DNS name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns "$ALB_ARN" \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
fi

# Check if service exists
echo ""
echo "=== Checking Service Status ==="
SERVICE_EXISTS=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "mini-java-app-service" \
    --region "$AWS_REGION" \
    --query 'services[0].serviceName' \
    --output text 2>/dev/null)

if [ "$SERVICE_EXISTS" == "mini-java-app-service" ]; then
    echo "Service exists, updating..."
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service "mini-java-app-service" \
        --task-definition "$TASK_DEF_ARN" \
        --region "$AWS_REGION" \
        --force-new-deployment
else
    echo "Creating new service..."
    aws ecs create-service \
        --cli-input-json file://ecs/service-definition-temp.json \
        --region "$AWS_REGION"
fi

# Wait for service stability
echo ""
echo "=== Waiting for Service Stability ==="
echo "This may take a few minutes..."
aws ecs wait services-stable \
    --cluster "$CLUSTER_NAME" \
    --services "mini-java-app-service" \
    --region "$AWS_REGION"

# Verify deployment
echo ""
echo "=== Deployment Verification ==="
aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "mini-java-app-service" \
    --region "$AWS_REGION" \
    --query 'services[0].[serviceName,status,runningCount,desiredCount]' \
    --output table

# Cleanup temporary files
rm -f ecs/task-definition-temp.json ecs/service-definition-temp.json

echo ""
echo "=========================================="
echo "✓ Deployment completed successfully!"
echo "=========================================="
echo "Cluster: $CLUSTER_NAME"
echo "Service: mini-java-app-service"
echo "Task Definition: $TASK_DEF_ARN"
echo "CloudWatch Logs: /ecs/mini-java-app"
if [[ "$NEED_LB" == "y" || "$NEED_LB" == "Y" ]]; then
    echo "Load Balancer DNS: $ALB_DNS"
    echo "Application URL: http://$ALB_DNS/mini-app"
fi
echo ""
echo "To view logs:"
echo "aws logs tail /ecs/mini-java-app --follow --region $AWS_REGION"
echo ""
