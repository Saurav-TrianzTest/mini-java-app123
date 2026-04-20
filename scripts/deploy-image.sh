#!/bin/bash

# Deploy to GCP GKE Script for mini-java-app
# This script deploys the containerized application to Google Kubernetes Engine

set -e
set -o pipefail

echo "=========================================="
echo "GKE Deployment Script for mini-java-app"
echo "=========================================="
echo ""

# Prompt for GCP configuration
echo "=== GCP Configuration ==="
read -p "Enter GCP Project ID: " GCP_PROJECT
read -p "Enter GCP Zone (e.g., us-central1-a): " GCP_ZONE
read -p "Enter GKE Cluster Name: " CLUSTER_NAME

echo ""
echo "=== Docker Image Configuration ==="
read -p "Enter Docker Image URI (with tag): " IMAGE_URI

echo ""
echo "=== Environment Variables Configuration ==="
echo "Configure external service connections (press Enter to skip optional values)"
echo ""

# Database Configuration
read -p "Enter DATABASE_URL (e.g., jdbc:mysql://mysql-host:3306/db): " DATABASE_URL
DATABASE_URL=${DATABASE_URL:-jdbc:mysql://mysql-service:3306/mini_app_db}

read -p "Enter DB_USERNAME: " DB_USERNAME
DB_USERNAME=${DB_USERNAME:-root}

read -sp "Enter DB_PASSWORD: " DB_PASSWORD
echo ""

# Redis Configuration
read -p "Enter REDIS_HOST: " REDIS_HOST
REDIS_HOST=${REDIS_HOST:-redis-service}

read -p "Enter REDIS_PORT (default: 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}

read -sp "Enter REDIS_PASSWORD (optional): " REDIS_PASSWORD
echo ""

# External API Configuration
read -p "Enter EXTERNAL_API_URL (optional): " EXTERNAL_API_URL
EXTERNAL_API_URL=${EXTERNAL_API_URL:-http://api-service:8080/v1}

read -p "Enter EXTERNAL_API_KEY (optional): " EXTERNAL_API_KEY

# Payment Service Configuration
read -p "Enter PAYMENT_SERVICE_URL (optional): " PAYMENT_SERVICE_URL
PAYMENT_SERVICE_URL=${PAYMENT_SERVICE_URL:-http://payment-service/process}

read -p "Enter PAYMENT_SERVICE_USERNAME (optional): " PAYMENT_SERVICE_USERNAME

read -sp "Enter PAYMENT_SERVICE_PASSWORD (optional): " PAYMENT_SERVICE_PASSWORD
echo ""

# GCS Bucket Configuration
read -p "Enter GCS_CONFIG_BUCKET: " GCS_CONFIG_BUCKET
GCS_CONFIG_BUCKET=${GCS_CONFIG_BUCKET:-app-config-bucket}

read -p "Enter GCS_LOG_BUCKET: " GCS_LOG_BUCKET
GCS_LOG_BUCKET=${GCS_LOG_BUCKET:-app-logs-bucket}

read -p "Enter GCS_TEMP_BUCKET: " GCS_TEMP_BUCKET
GCS_TEMP_BUCKET=${GCS_TEMP_BUCKET:-app-temp-bucket}

read -p "Enter GCS_UPLOAD_BUCKET: " GCS_UPLOAD_BUCKET
GCS_UPLOAD_BUCKET=${GCS_UPLOAD_BUCKET:-app-uploads-bucket}

# Security Configuration
read -sp "Enter JWT_SECRET: " JWT_SECRET
echo ""

read -p "Enter ADMIN_USERNAME (default: admin): " ADMIN_USERNAME
ADMIN_USERNAME=${ADMIN_USERNAME:-admin}

read -sp "Enter ADMIN_PASSWORD: " ADMIN_PASSWORD
echo ""

read -sp "Enter ENCRYPTION_KEY: " ENCRYPTION_KEY
echo ""

# Monitoring Configuration
read -p "Enter MONITORING_ENDPOINT (optional): " MONITORING_ENDPOINT
MONITORING_ENDPOINT=${MONITORING_ENDPOINT:-http://monitoring-service:9090/metrics}

read -p "Enter MONITORING_USERNAME (optional): " MONITORING_USERNAME

read -sp "Enter MONITORING_PASSWORD (optional): " MONITORING_PASSWORD
echo ""

# RabbitMQ Configuration
read -p "Enter RABBITMQ_HOST (optional): " RABBITMQ_HOST
RABBITMQ_HOST=${RABBITMQ_HOST:-rabbitmq-service}

read -p "Enter RABBITMQ_PORT (default: 5672): " RABBITMQ_PORT
RABBITMQ_PORT=${RABBITMQ_PORT:-5672}

read -p "Enter RABBITMQ_USERNAME (optional): " RABBITMQ_USERNAME

read -sp "Enter RABBITMQ_PASSWORD (optional): " RABBITMQ_PASSWORD
echo ""

echo ""
echo "=========================================="
echo "Configuring kubectl for GKE..."
echo "=========================================="

# Authenticate with GCP
gcloud config set project "$GCP_PROJECT"

# Get GKE credentials
gcloud container clusters get-credentials "$CLUSTER_NAME" --zone "$GCP_ZONE" --project "$GCP_PROJECT"

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to get GKE credentials"
    exit 1
fi

# Verify cluster connectivity
echo ""
echo "Verifying cluster connectivity..."
kubectl cluster-info

if [ $? -ne 0 ]; then
    echo "ERROR: Cannot connect to Kubernetes cluster"
    exit 1
fi

echo ""
echo "=========================================="
echo "Updating Kubernetes Manifests..."
echo "=========================================="

# Create temporary directory for modified manifests
TEMP_DIR=$(mktemp -d)
cp -r kubernetes/* "$TEMP_DIR/"

# Replace placeholders in deployment.yaml
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DATABASE_URL}}|$DATABASE_URL|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_USERNAME}}|$DB_USERNAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PORT}}|$REDIS_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PASSWORD}}|$REDIS_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{EXTERNAL_API_URL}}|$EXTERNAL_API_URL|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{EXTERNAL_API_KEY}}|$EXTERNAL_API_KEY|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{PAYMENT_SERVICE_URL}}|$PAYMENT_SERVICE_URL|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{PAYMENT_SERVICE_USERNAME}}|$PAYMENT_SERVICE_USERNAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{PAYMENT_SERVICE_PASSWORD}}|$PAYMENT_SERVICE_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{GCS_CONFIG_BUCKET}}|$GCS_CONFIG_BUCKET|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{GCS_LOG_BUCKET}}|$GCS_LOG_BUCKET|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{GCS_TEMP_BUCKET}}|$GCS_TEMP_BUCKET|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{GCS_UPLOAD_BUCKET}}|$GCS_UPLOAD_BUCKET|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{JWT_SECRET}}|$JWT_SECRET|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{ADMIN_USERNAME}}|$ADMIN_USERNAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{ADMIN_PASSWORD}}|$ADMIN_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{ENCRYPTION_KEY}}|$ENCRYPTION_KEY|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{MONITORING_ENDPOINT}}|$MONITORING_ENDPOINT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{MONITORING_USERNAME}}|$MONITORING_USERNAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{MONITORING_PASSWORD}}|$MONITORING_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_HOST}}|$RABBITMQ_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_PORT}}|$RABBITMQ_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_USERNAME}}|$RABBITMQ_USERNAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_PASSWORD}}|$RABBITMQ_PASSWORD|g" "$TEMP_DIR/deployment.yaml"

echo "✓ Manifests updated successfully"

echo ""
echo "=========================================="
echo "Deploying to GKE..."
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
echo "Waiting for Deployment Rollout..."
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

kubectl get pods,svc,ingress -n mini-java-app

echo ""
echo "=========================================="
echo "✓ DEPLOYMENT COMPLETE!"
echo "=========================================="
echo ""
echo "Application deployed to namespace: mini-java-app"
echo ""
echo "To check application logs:"
echo "  kubectl logs -f deployment/mini-java-app -n mini-java-app"
echo ""
echo "To check pod status:"
echo "  kubectl get pods -n mini-java-app"
echo ""
echo "To access the application:"
echo "  kubectl port-forward -n mini-java-app svc/mini-java-app-service 8080:80"
echo "  Then visit: http://localhost:8080/mini-app"
echo ""
echo "To get ingress IP (may take a few minutes):"
echo "  kubectl get ingress mini-java-app-ingress -n mini-java-app"
echo ""
echo "=========================================="

# Cleanup temporary directory
rm -rf "$TEMP_DIR"
