#!/bin/bash
set -e
set -o pipefail

# Deploy to Azure AKS Script for Mini Java Application
# This script deploys the containerized application to Azure Kubernetes Service

echo "=========================================="
echo "Azure AKS Deployment Script"
echo "Mini Java Application"
echo "=========================================="
echo ""

# Prompt for Azure AKS details
echo "=== Azure AKS Configuration ==="
read -p "Enter Azure Resource Group name: " RESOURCE_GROUP
read -p "Enter AKS Cluster name: " CLUSTER_NAME
echo ""

# Validate inputs
if [ -z "$RESOURCE_GROUP" ] || [ -z "$CLUSTER_NAME" ]; then
    echo "ERROR: Resource Group and Cluster Name are required"
    exit 1
fi

# Prompt for Docker image URI
echo "=== Docker Image Configuration ==="
read -p "Enter Docker image URI (e.g., myregistry.azurecr.io/mini-java-app:latest): " IMAGE_URI
echo ""

if [ -z "$IMAGE_URI" ]; then
    echo "ERROR: Docker image URI is required"
    exit 1
fi

# Prompt for environment variables
echo "=== Application Configuration ==="
echo "Enter values for application environment variables (press Enter to skip optional ones)"
echo ""

read -p "Enter DATABASE_URL (e.g., jdbc:mysql://mysql-host:3306/mini_app_db): " DATABASE_URL
read -p "Enter DB_USERNAME: " DB_USERNAME
read -sp "Enter DB_PASSWORD: " DB_PASSWORD
echo ""
read -p "Enter DB_HOST: " DB_HOST
read -p "Enter DB_PORT (default: 3306): " DB_PORT
DB_PORT=${DB_PORT:-3306}
read -p "Enter DB_NAME: " DB_NAME

echo ""
read -p "Enter REDIS_HOST (optional): " REDIS_HOST
read -p "Enter REDIS_PORT (default: 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}
read -sp "Enter REDIS_PASSWORD (optional): " REDIS_PASSWORD
echo ""

echo ""
read -p "Enter EXTERNAL_API_URL (optional): " EXTERNAL_API_URL
read -p "Enter EXTERNAL_API_KEY (optional): " EXTERNAL_API_KEY

echo ""
read -p "Enter PAYMENT_SERVICE_URL (optional): " PAYMENT_SERVICE_URL
read -p "Enter PAYMENT_SERVICE_USERNAME (optional): " PAYMENT_SERVICE_USERNAME
read -sp "Enter PAYMENT_SERVICE_PASSWORD (optional): " PAYMENT_SERVICE_PASSWORD
echo ""

echo ""
read -p "Enter AZURE_STORAGE_CONNECTION_STRING (optional): " AZURE_STORAGE_CONNECTION_STRING

echo ""
read -p "Enter JWT_SECRET (optional): " JWT_SECRET
read -p "Enter ADMIN_USERNAME (optional): " ADMIN_USERNAME
read -sp "Enter ADMIN_PASSWORD (optional): " ADMIN_PASSWORD
echo ""

echo ""
read -p "Enter MONITORING_ENDPOINT (optional): " MONITORING_ENDPOINT
read -p "Enter MONITORING_USERNAME (optional): " MONITORING_USERNAME
read -sp "Enter MONITORING_PASSWORD (optional): " MONITORING_PASSWORD
echo ""

echo ""
read -p "Enter RABBITMQ_HOST (optional): " RABBITMQ_HOST
read -p "Enter RABBITMQ_PORT (default: 5672): " RABBITMQ_PORT
RABBITMQ_PORT=${RABBITMQ_PORT:-5672}
read -p "Enter RABBITMQ_USERNAME (optional): " RABBITMQ_USERNAME
read -sp "Enter RABBITMQ_PASSWORD (optional): " RABBITMQ_PASSWORD
echo ""

echo ""
echo "=========================================="
echo "Configuring kubectl for AKS cluster..."
echo "=========================================="

az aks get-credentials --resource-group "$RESOURCE_GROUP" --name "$CLUSTER_NAME" --overwrite-existing

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to get AKS credentials"
    exit 1
fi

echo ""
echo "Verifying cluster connectivity..."
kubectl cluster-info

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to connect to Kubernetes cluster"
    exit 1
fi

echo ""
echo "=========================================="
echo "Updating Kubernetes manifests..."
echo "=========================================="

# Create temporary directory for modified manifests
TEMP_DIR=$(mktemp -d)
cp -r kubernetes/* "$TEMP_DIR/"

# Update IMAGE_URI in deployment.yaml
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TEMP_DIR/deployment.yaml"

# Update environment variables in deployment.yaml
sed -i "s|{{DATABASE_URL}}|${DATABASE_URL:-jdbc:mysql://mysql-host:3306/mini_app_db}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_USERNAME}}|${DB_USERNAME:-root}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_PASSWORD}}|${DB_PASSWORD:-password}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_HOST}}|${DB_HOST:-mysql-host}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_PORT}}|${DB_PORT:-3306}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_NAME}}|${DB_NAME:-mini_app_db}|g" "$TEMP_DIR/deployment.yaml"

sed -i "s|{{REDIS_HOST}}|${REDIS_HOST:-redis-host}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PORT}}|${REDIS_PORT:-6379}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PASSWORD}}|${REDIS_PASSWORD}|g" "$TEMP_DIR/deployment.yaml"

sed -i "s|{{EXTERNAL_API_URL}}|${EXTERNAL_API_URL:-http://api.example.com:8080/v1}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{EXTERNAL_API_KEY}}|${EXTERNAL_API_KEY}|g" "$TEMP_DIR/deployment.yaml"

sed -i "s|{{PAYMENT_SERVICE_URL}}|${PAYMENT_SERVICE_URL:-https://payment.internal.company.com/process}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{PAYMENT_SERVICE_USERNAME}}|${PAYMENT_SERVICE_USERNAME}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{PAYMENT_SERVICE_PASSWORD}}|${PAYMENT_SERVICE_PASSWORD}|g" "$TEMP_DIR/deployment.yaml"

sed -i "s|{{AZURE_STORAGE_CONNECTION_STRING}}|${AZURE_STORAGE_CONNECTION_STRING}|g" "$TEMP_DIR/deployment.yaml"

sed -i "s|{{JWT_SECRET}}|${JWT_SECRET}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{ADMIN_USERNAME}}|${ADMIN_USERNAME}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{ADMIN_PASSWORD}}|${ADMIN_PASSWORD}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{ENCRYPTION_KEY}}|${ENCRYPTION_KEY}|g" "$TEMP_DIR/deployment.yaml"

sed -i "s|{{MONITORING_ENDPOINT}}|${MONITORING_ENDPOINT:-http://monitoring-service:9090/metrics}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{MONITORING_USERNAME}}|${MONITORING_USERNAME}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{MONITORING_PASSWORD}}|${MONITORING_PASSWORD}|g" "$TEMP_DIR/deployment.yaml"

sed -i "s|{{RABBITMQ_HOST}}|${RABBITMQ_HOST:-rabbitmq-host}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_PORT}}|${RABBITMQ_PORT:-5672}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_USERNAME}}|${RABBITMQ_USERNAME}|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_PASSWORD}}|${RABBITMQ_PASSWORD}|g" "$TEMP_DIR/deployment.yaml"

echo "Manifests updated successfully"

echo ""
echo "=========================================="
echo "Deploying to Azure AKS..."
echo "=========================================="

# Apply namespace
echo "Creating namespace..."
kubectl apply -f "$TEMP_DIR/namespace.yaml"

# Apply deployment
echo "Deploying application..."
kubectl apply -f "$TEMP_DIR/deployment.yaml"

# Apply service
echo "Creating service..."
kubectl apply -f "$TEMP_DIR/service.yaml"

# Apply ingress
echo "Creating ingress..."
kubectl apply -f "$TEMP_DIR/ingress.yaml"

echo ""
echo "=========================================="
echo "Waiting for deployment to complete..."
echo "=========================================="

kubectl rollout status deployment/mini-java-app -n mini-java-app --timeout=5m

if [ $? -ne 0 ]; then
    echo "WARNING: Deployment rollout did not complete within timeout"
    echo "Check deployment status with: kubectl get pods -n mini-java-app"
fi

echo ""
echo "=========================================="
echo "Deployment Status"
echo "=========================================="

kubectl get pods -n mini-java-app
echo ""
kubectl get svc -n mini-java-app
echo ""
kubectl get ingress -n mini-java-app

echo ""
echo "=========================================="
echo "SUCCESS!"
echo "Application deployed to Azure AKS"
echo "=========================================="
echo ""
echo "Access your application:"
echo "- Internal: http://mini-java-app-service.mini-java-app.svc.cluster.local"
echo "- External: Check ingress address above"
echo ""
echo "Useful commands:"
echo "- View logs: kubectl logs -f deployment/mini-java-app -n mini-java-app"
echo "- View pods: kubectl get pods -n mini-java-app"
echo "- Describe deployment: kubectl describe deployment mini-java-app -n mini-java-app"
echo "- Scale deployment: kubectl scale deployment mini-java-app --replicas=3 -n mini-java-app"
echo ""

# Cleanup temporary directory
rm -rf "$TEMP_DIR"
