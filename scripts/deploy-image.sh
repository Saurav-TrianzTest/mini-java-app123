#!/bin/bash
set -e
set -o pipefail

echo "============================================"
echo "Mini Java App - ECS Fargate Deployment"
echo "============================================"
echo ""

# Prompt for AWS configuration
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
read -p "Enter ECS Cluster name: " CLUSTER_NAME
read -p "Enter ECR Image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): " IMAGE_URI

echo ""
echo "Network Configuration"
echo "---------------------"
read -p "Enter VPC ID: " VPC_ID
read -p "Enter Subnet IDs (comma-separated, at least 2): " SUBNET_IDS
read -p "Enter Security Group ID: " SECURITY_GROUP

# Convert comma-separated subnets to array
IFS=',' read -ra SUBNETS <<< "$SUBNET_IDS"
SUBNET_1=${SUBNETS[0]}
SUBNET_2=${SUBNETS[1]:-$SUBNET_1}

echo ""
echo "External Services Configuration"
echo "-------------------------------"
read -p "Enter Database Host: " DB_HOST
read -p "Enter Database Port (default: 3306): " DB_PORT
DB_PORT=${DB_PORT:-3306}
read -p "Enter Database Name: " DB_NAME
read -p "Enter Database User: " DB_USER
read -sp "Enter Database Password: " DB_PASSWORD
echo ""
read -p "Enter Redis Host: " REDIS_HOST
read -p "Enter RabbitMQ Host: " RABBITMQ_HOST

echo ""
echo "Getting AWS Account ID..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "Account ID: $ACCOUNT_ID"

echo ""
echo "Checking if ECS cluster exists..."
aws ecs describe-clusters --clusters "$CLUSTER_NAME" --region "$AWS_REGION" >/dev/null 2>&1 || {
    echo "Creating ECS cluster: $CLUSTER_NAME"
    aws ecs create-cluster --cluster-name "$CLUSTER_NAME" --region "$AWS_REGION"
}

echo ""
read -p "Do you need a load balancer for this service? (y/n): " NEED_LB

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo ""
    echo "Creating Application Load Balancer and Target Group..."
    
    # Create target group with ip target type (required for Fargate)
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name mini-java-app-tg \
        --protocol HTTP \
        --port 8080 \
        --vpc-id "$VPC_ID" \
        --target-type ip \
        --health-check-enabled \
        --health-check-path "/actuator/health" \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --region "$AWS_REGION" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text 2>/dev/null || aws elbv2 describe-target-groups \
        --names mini-java-app-tg \
        --region "$AWS_REGION" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text)
    
    echo "Target Group ARN: $TARGET_GROUP_ARN"
    
    # Add loadBalancers section to service definition
    LB_CONFIG="\"loadBalancers\": [{\"targetGroupArn\": \"$TARGET_GROUP_ARN\", \"containerName\": \"mini-java-app\", \"containerPort\": 8080}], \"healthCheckGracePeriodSeconds\": 300,"
else
    LB_CONFIG=""
fi

echo ""
echo "Preparing task definition..."
cp ecs/task-definition.json ecs/task-definition-deploy.json

# Replace placeholders in task definition
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" ecs/task-definition-deploy.json
sed -i "s|{{AWS_REGION}}|$AWS_REGION|g" ecs/task-definition-deploy.json
sed -i "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" ecs/task-definition-deploy.json
sed -i "s|{{DB_HOST}}|$DB_HOST|g" ecs/task-definition-deploy.json
sed -i "s|{{DB_PORT}}|$DB_PORT|g" ecs/task-definition-deploy.json
sed -i "s|{{DB_NAME}}|$DB_NAME|g" ecs/task-definition-deploy.json
sed -i "s|{{DB_USER}}|$DB_USER|g" ecs/task-definition-deploy.json
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" ecs/task-definition-deploy.json
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" ecs/task-definition-deploy.json
sed -i "s|{{RABBITMQ_HOST}}|$RABBITMQ_HOST|g" ecs/task-definition-deploy.json

echo "Registering task definition..."
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://ecs/task-definition-deploy.json \
    --region "$AWS_REGION" \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

echo "Task Definition ARN: $TASK_DEF_ARN"

echo ""
echo "Checking if service exists..."
SERVICE_EXISTS=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services mini-java-app-service \
    --region "$AWS_REGION" \
    --query 'services[?status==`ACTIVE`].serviceName' \
    --output text)

if [ -z "$SERVICE_EXISTS" ]; then
    echo "Creating new ECS service..."
    
    # Prepare service definition
    cp ecs/service-definition.json ecs/service-definition-deploy.json
    sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" ecs/service-definition-deploy.json
    sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" ecs/service-definition-deploy.json
    sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" ecs/service-definition-deploy.json
    sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" ecs/service-definition-deploy.json
    
    # Add load balancer configuration if needed
    if [ -n "$LB_CONFIG" ]; then
        sed -i "s|\"desiredCount\":|$LB_CONFIG\"desiredCount\":|" ecs/service-definition-deploy.json
    fi
    
    aws ecs create-service \
        --cli-input-json file://ecs/service-definition-deploy.json \
        --region "$AWS_REGION"
else
    echo "Updating existing ECS service..."
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service mini-java-app-service \
        --task-definition "$TASK_DEF_ARN" \
        --force-new-deployment \
        --region "$AWS_REGION"
fi

echo ""
echo "Waiting for service to become stable..."
aws ecs wait services-stable \
    --cluster "$CLUSTER_NAME" \
    --services mini-java-app-service \
    --region "$AWS_REGION"

echo ""
echo "Deployment verification..."
aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services mini-java-app-service \
    --region "$AWS_REGION" \
    --query 'services[0].[serviceName,status,runningCount,desiredCount]' \
    --output table

if [ -n "$TARGET_GROUP_ARN" ]; then
    echo ""
    echo "Load Balancer Information:"
    aws elbv2 describe-load-balancers \
        --region "$AWS_REGION" \
        --query "LoadBalancers[?State.Code=='active'].DNSName" \
        --output table
fi

echo ""
echo "============================================"
echo "SUCCESS: Deployment completed"
echo "Cluster: $CLUSTER_NAME"
echo "Service: mini-java-app-service"
echo "Task Definition: $TASK_DEF_ARN"
echo "CloudWatch Logs: /ecs/mini-java-app"
echo "============================================"

# Cleanup temporary files
rm -f ecs/task-definition-deploy.json ecs/service-definition-deploy.json