#!/bin/bash
set -e
set -o pipefail

# Deploy image to AWS EKS for mini-java-container
# Run from the repository root

echo "=== mini-java-container EKS Deploy Script ==="

PROJECT_NAME="MiniJavaContainer"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

# Ask for AWS region
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION

# Ask for EKS cluster name
read -p "Enter EKS cluster name: " CLUSTER_NAME

# Ask for image URI
read -p "Enter full image URI (registry/repo:tag): " IMAGE_URI

# Ask for environment variables
echo ""
echo "Enter values for environment variables (or press Enter to skip):"
echo ""

# Non-credential environment variables
read -p "Enter value for DB_HOST (or press Enter to skip): " DB_HOST
read -p "Enter value for DB_PORT (or press Enter to skip): " DB_PORT
read -p "Enter value for DB_NAME (or press Enter to skip): " DB_NAME
read -p "Enter value for REDIS_HOST (or press Enter to skip): " REDIS_HOST
read -p "Enter value for REDIS_PORT (or press Enter to skip): " REDIS_PORT
read -p "Enter value for EXTERNAL_API_URL (or press Enter to skip): " EXTERNAL_API_URL
read -p "Enter value for PAYMENT_SERVICE_URL (or press Enter to skip): " PAYMENT_SERVICE_URL

# Credentials (not echoed)
echo ""
echo "Enter credential values (not echoed):"
read -s -p "Enter value for DB_USERNAME (or press Enter to skip): " DB_USERNAME
echo ""
read -s -p "Enter value for DB_PASSWORD (or press Enter to skip): " DB_PASSWORD
echo ""

# Configure kubectl for the EKS cluster
echo ""
echo "Configuring kubectl for EKS cluster: $CLUSTER_NAME in region $AWS_REGION..."
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo ""
echo "Verifying cluster connectivity..."
kubectl cluster-info || exit 1

# Apply namespace
echo ""
echo "Applying namespace..."
kubectl apply -f kubernetes/namespace.yaml

# Create Kubernetes Secret for credentials
echo ""
echo "Creating Kubernetes Secret for credentials..."
if [ -n "$DB_USERNAME" ] || [ -n "$DB_PASSWORD" ]; then
    SECRET_ARGS=""
    if [ -n "$DB_USERNAME" ]; then
        SECRET_ARGS="$SECRET_ARGS --from-literal=db-username=$DB_USERNAME"
    fi
    if [ -n "$DB_PASSWORD" ]; then
        SECRET_ARGS="$SECRET_ARGS --from-literal=db-password=$DB_PASSWORD"
    fi
    kubectl create secret generic ${IMAGE_NAME}-secrets -n ${IMAGE_NAME} $SECRET_ARGS --dry-run=client -o yaml | kubectl apply -f -
fi

# Prepare deployment.yaml with placeholders replaced
echo ""
echo "Deploying application..."

# Replace IMAGE_URI placeholder
sed -i.bak "s|{{IMAGE_URI}}|${IMAGE_URI}|g" kubernetes/deployment.yaml

# Replace environment variable placeholders
if [ -n "$DB_HOST" ]; then
    sed -i.bak "s|{{DB_HOST}}|${DB_HOST}|g" kubernetes/deployment.yaml
fi
if [ -n "$DB_PORT" ]; then
    sed -i.bak "s|{{DB_PORT}}|${DB_PORT}|g" kubernetes/deployment.yaml
fi
if [ -n "$DB_NAME" ]; then
    sed -i.bak "s|{{DB_NAME}}|${DB_NAME}|g" kubernetes/deployment.yaml
fi
if [ -n "$REDIS_HOST" ]; then
    sed -i.bak "s|{{REDIS_HOST}}|${REDIS_HOST}|g" kubernetes/deployment.yaml
fi
if [ -n "$REDIS_PORT" ]; then
    sed -i.bak "s|{{REDIS_PORT}}|${REDIS_PORT}|g" kubernetes/deployment.yaml
fi
if [ -n "$EXTERNAL_API_URL" ]; then
    sed -i.bak "s|{{EXTERNAL_API_URL}}|${EXTERNAL_API_URL}|g" kubernetes/deployment.yaml
fi
if [ -n "$PAYMENT_SERVICE_URL" ]; then
    sed -i.bak "s|{{PAYMENT_SERVICE_URL}}|${PAYMENT_SERVICE_URL}|g" kubernetes/deployment.yaml
fi

# Remove backup files
rm -f kubernetes/deployment.yaml.bak

# Apply deployment
kubectl apply -f kubernetes/deployment.yaml

# Apply service
echo ""
echo "Applying service..."
kubectl apply -f kubernetes/service.yaml

# Apply ingress
echo ""
echo "Applying ingress..."
kubectl apply -f kubernetes/ingress.yaml

# Wait for deployment to be ready
echo ""
echo "Waiting for deployment to be ready..."
kubectl rollout status deployment/${IMAGE_NAME} -n ${IMAGE_NAME}

# Show resources
echo ""
echo "=== Deployment Status ==="
kubectl get pods,svc,ingress -n ${IMAGE_NAME}

echo ""
echo "=== Application URL ==="
echo "http://${IMAGE_NAME}.example.com"

echo ""
echo "=== Deployment Complete ==="
echo "If the deployment fails, rollback with:"
echo "  kubectl rollout undo deployment/${IMAGE_NAME} -n ${IMAGE_NAME}"
