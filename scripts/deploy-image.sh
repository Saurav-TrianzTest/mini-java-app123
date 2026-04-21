#!/bin/bash

# Deploy Mini Java Application to GCP GKE
# This script deploys the containerized application to Google Kubernetes Engine

set -e
set -o pipefail

echo "=========================================="
echo "GKE Deployment Script"
echo "Mini Java Application"
echo "=========================================="
echo ""

# Prompt for GCP configuration
read -p "Enter GCP Project ID: " GCP_PROJECT
read -p "Enter GCP Zone (e.g., us-central1-a): " GCP_ZONE
read -p "Enter GKE Cluster Name: " CLUSTER_NAME

if [ -z "$GCP_PROJECT" ] || [ -z "$GCP_ZONE" ] || [ -z "$CLUSTER_NAME" ]; then
    echo "ERROR: GCP Project ID, Zone, and Cluster Name are required"
    exit 1
fi

echo ""
echo "GCP Configuration:"
echo "  Project: $GCP_PROJECT"
echo "  Zone: $GCP_ZONE"
echo "  Cluster: $CLUSTER_NAME"
echo ""

# Prompt for Docker image URI
read -p "Enter Docker Image URI (e.g., us-central1-docker.pkg.dev/project/repo/mini-java-app:latest): " IMAGE_URI

if [ -z "$IMAGE_URI" ]; then
    echo "ERROR: Docker Image URI is required"
    exit 1
fi

echo ""
echo "Docker Image: $IMAGE_URI"
echo ""

# Prompt for environment variables
echo "=========================================="
echo "Environment Configuration"
echo "=========================================="
echo "Enter values for environment variables (press Enter to use defaults):"
echo ""

read -p "DB_HOST (default: mysql-service): " DB_HOST
DB_HOST=${DB_HOST:-mysql-service}

read -p "DB_PORT (default: 3306): " DB_PORT
DB_PORT=${DB_PORT:-3306}

read -p "DB_NAME (default: mini_app_db): " DB_NAME
DB_NAME=${DB_NAME:-mini_app_db}

read -p "DB_USERNAME (default: root): " DB_USERNAME
DB_USERNAME=${DB_USERNAME:-root}

read -sp "DB_PASSWORD (default: password): " DB_PASSWORD
echo ""
DB_PASSWORD=${DB_PASSWORD:-password}

DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}"

read -p "DB_POOL_MAX_CONNECTIONS (default: 20): " DB_POOL_MAX_CONNECTIONS
DB_POOL_MAX_CONNECTIONS=${DB_POOL_MAX_CONNECTIONS:-20}

read -p "DB_POOL_TIMEOUT (default: 5000): " DB_POOL_TIMEOUT
DB_POOL_TIMEOUT=${DB_POOL_TIMEOUT:-5000}

read -p "DB_QUERY_TIMEOUT (default: 30): " DB_QUERY_TIMEOUT
DB_QUERY_TIMEOUT=${DB_QUERY_TIMEOUT:-30}

read -p "REDIS_HOST (default: redis-service): " REDIS_HOST
REDIS_HOST=${REDIS_HOST:-redis-service}

read -p "REDIS_PORT (default: 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}

read -sp "REDIS_PASSWORD (optional): " REDIS_PASSWORD
echo ""

read -p "REDIS_DATABASE (default: 0): " REDIS_DATABASE
REDIS_DATABASE=${REDIS_DATABASE:-0}

read -p "EXTERNAL_API_URL (default: http://api-service:8080/v1): " EXTERNAL_API_URL
EXTERNAL_API_URL=${EXTERNAL_API_URL:-http://api-service:8080/v1}

read -p "EXTERNAL_API_TIMEOUT (default: 30000): " EXTERNAL_API_TIMEOUT
EXTERNAL_API_TIMEOUT=${EXTERNAL_API_TIMEOUT:-30000}

read -p "EXTERNAL_API_KEY (optional): " EXTERNAL_API_KEY

read -p "PAYMENT_SERVICE_URL (default: http://payment-service/process): " PAYMENT_SERVICE_URL
PAYMENT_SERVICE_URL=${PAYMENT_SERVICE_URL:-http://payment-service/process}

read -p "PAYMENT_SERVICE_USERNAME (optional): " PAYMENT_SERVICE_USERNAME

read -sp "PAYMENT_SERVICE_PASSWORD (optional): " PAYMENT_SERVICE_PASSWORD
echo ""

read -p "GCS_CONFIG_BUCKET (default: gs://app-config-bucket): " GCS_CONFIG_BUCKET
GCS_CONFIG_BUCKET=${GCS_CONFIG_BUCKET:-gs://app-config-bucket}

read -p "GCS_LOG_BUCKET (default: gs://app-logs-bucket): " GCS_LOG_BUCKET
GCS_LOG_BUCKET=${GCS_LOG_BUCKET:-gs://app-logs-bucket}

read -p "GCS_TEMP_BUCKET (default: gs://app-temp-bucket): " GCS_TEMP_BUCKET
GCS_TEMP_BUCKET=${GCS_TEMP_BUCKET:-gs://app-temp-bucket}

read -p "GCS_UPLOAD_BUCKET (default: gs://app-uploads-bucket): " GCS_UPLOAD_BUCKET
GCS_UPLOAD_BUCKET=${GCS_UPLOAD_BUCKET:-gs://app-uploads-bucket}

read -sp "JWT_SECRET (optional): " JWT_SECRET
echo ""

read -p "ADMIN_USERNAME (optional): " ADMIN_USERNAME

read -sp "ADMIN_PASSWORD (optional): " ADMIN_PASSWORD
echo ""

read -sp "ENCRYPTION_KEY (optional): " ENCRYPTION_KEY
echo ""

read -p "MONITORING_ENDPOINT (default: http://monitoring-service:9090/metrics): " MONITORING_ENDPOINT
MONITORING_ENDPOINT=${MONITORING_ENDPOINT:-http://monitoring-service:9090/metrics}

read -p "MONITORING_USERNAME (optional): " MONITORING_USERNAME

read -sp "MONITORING_PASSWORD (optional): " MONITORING_PASSWORD
echo ""

read -p "RABBITMQ_HOST (default: rabbitmq-service): " RABBITMQ_HOST
RABBITMQ_HOST=${RABBITMQ_HOST:-rabbitmq-service}

read -p "RABBITMQ_PORT (default: 5672): " RABBITMQ_PORT
RABBITMQ_PORT=${RABBITMQ_PORT:-5672}

read -p "RABBITMQ_USERNAME (optional): " RABBITMQ_USERNAME

read -sp "RABBITMQ_PASSWORD (optional): " RABBITMQ_PASSWORD
echo ""

read -p "ENVIRONMENT (default: production): " ENVIRONMENT
ENVIRONMENT=${ENVIRONMENT:-production}

read -p "DEBUG_ENABLED (default: false): " DEBUG_ENABLED
DEBUG_ENABLED=${DEBUG_ENABLED:-false}

read -p "LOGGING_LEVEL (default: INFO): " LOGGING_LEVEL
LOGGING_LEVEL=${LOGGING_LEVEL:-INFO}

echo ""
echo "=========================================="
echo "Configuring kubectl for GKE..."
echo "=========================================="

# Configure kubectl to use GKE cluster
gcloud container clusters get-credentials "$CLUSTER_NAME" --zone "$GCP_ZONE" --project "$GCP_PROJECT"

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to configure kubectl for GKE cluster"
    exit 1
fi

# Verify cluster connectivity
echo ""
echo "Verifying cluster connectivity..."
kubectl cluster-info

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to connect to Kubernetes cluster"
    exit 1
fi

echo ""
echo "✓ Successfully connected to GKE cluster"

# Update Kubernetes manifests with actual values
echo ""
echo "=========================================="
echo "Updating Kubernetes manifests..."
echo "=========================================="

# Create temporary directory for updated manifests
TEMP_DIR=$(mktemp -d)
cp -r kubernetes/* "$TEMP_DIR/"

# Update deployment.yaml with image URI and environment variables
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_HOST}}|$DB_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_PORT}}|$DB_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_NAME}}|$DB_NAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_USERNAME}}|$DB_USERNAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_URL}}|$DB_URL|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_POOL_MAX_CONNECTIONS}}|$DB_POOL_MAX_CONNECTIONS|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_POOL_TIMEOUT}}|$DB_POOL_TIMEOUT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_QUERY_TIMEOUT}}|$DB_QUERY_TIMEOUT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PORT}}|$REDIS_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PASSWORD}}|$REDIS_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_DATABASE}}|$REDIS_DATABASE|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{EXTERNAL_API_URL}}|$EXTERNAL_API_URL|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{EXTERNAL_API_TIMEOUT}}|$EXTERNAL_API_TIMEOUT|g" "$TEMP_DIR/deployment.yaml"
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
sed -i "s|{{ENVIRONMENT}}|$ENVIRONMENT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DEBUG_ENABLED}}|$DEBUG_ENABLED|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{LOGGING_LEVEL}}|$LOGGING_LEVEL|g" "$TEMP_DIR/deployment.yaml"

echo "✓ Manifests updated successfully"

# Deploy to Kubernetes
echo ""
echo "=========================================="
echo "Deploying to GKE..."
echo "=========================================="

# Apply namespace
echo ""
echo "Creating namespace..."
kubectl apply -f "$TEMP_DIR/namespace.yaml"

# Apply deployment
echo ""
echo "Deploying application..."
kubectl apply -f "$TEMP_DIR/deployment.yaml"

# Apply service
echo ""
echo "Creating service..."
kubectl apply -f "$TEMP_DIR/service.yaml"

# Apply ingress
echo ""
echo "Creating ingress..."
kubectl apply -f "$TEMP_DIR/ingress.yaml"

# Wait for deployment to complete
echo ""
echo "=========================================="
echo "Waiting for deployment to complete..."
echo "=========================================="
kubectl rollout status deployment/mini-java-app -n mini-java-app --timeout=5m

if [ $? -ne 0 ]; then
    echo "ERROR: Deployment rollout failed"
    echo ""
    echo "Checking pod status..."
    kubectl get pods -n mini-java-app
    echo ""
    echo "Checking pod logs..."
    kubectl logs -n mini-java-app -l app=mini-java-app --tail=50
    exit 1
fi

# Verify deployment
echo ""
echo "=========================================="
echo "Verifying deployment..."
echo "=========================================="
kubectl get pods,svc,ingress -n mini-java-app

# Get ingress IP
echo ""
echo "=========================================="
echo "Deployment Information"
echo "=========================================="
INGRESS_IP=$(kubectl get ingress mini-java-app-ingress -n mini-java-app -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "Pending...")

echo ""
echo "✓ Deployment completed successfully!"
echo ""
echo "Application Details:"
echo "  Namespace: mini-java-app"
echo "  Deployment: mini-java-app"
echo "  Service: mini-java-app-service"
echo "  Ingress IP: $INGRESS_IP"
echo ""
echo "Access your application:"
echo "  Internal: http://mini-java-app-service.mini-java-app.svc.cluster.local"
echo "  External: http://$INGRESS_IP (once DNS is configured)"
echo ""
echo "Useful commands:"
echo "  View pods: kubectl get pods -n mini-java-app"
echo "  View logs: kubectl logs -n mini-java-app -l app=mini-java-app"
echo "  Scale deployment: kubectl scale deployment mini-java-app -n mini-java-app --replicas=3"
echo "  Delete deployment: kubectl delete namespace mini-java-app"
echo ""

# Cleanup temporary directory
rm -rf "$TEMP_DIR"
