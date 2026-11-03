#!/bin/bash
set -e
set -o pipefail

# =============================================================
# deploy-image.sh – Deploy mini-java-app to AWS EKS
# =============================================================

APP_NAME="mini-java-app"
NAMESPACE="mini-java-app"

echo "=============================================="
echo "  Deploy $APP_NAME to AWS EKS"
echo "=============================================="
echo ""

# ---- Cluster configuration ----
read -rp "Enter AWS Region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS Region is required."
  exit 1
fi

read -rp "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS Cluster Name is required."
  exit 1
fi

read -rp "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required."
  exit 1
fi

echo ""
echo "--- Application Environment Variables ---"
echo "Press Enter to skip any variable (default values will be used)."
echo ""

read -rp "Enter DB_HOST (database hostname): " DB_HOST
[ -z "$DB_HOST" ] && DB_HOST="localhost"

read -rp "Enter DB_PORT (default: 3306): " DB_PORT
[ -z "$DB_PORT" ] && DB_PORT="3306"

read -rp "Enter DB_NAME (default: mini_app_db): " DB_NAME
[ -z "$DB_NAME" ] && DB_NAME="mini_app_db"

read -rp "Enter DB_USERNAME (default: root): " DB_USERNAME
[ -z "$DB_USERNAME" ] && DB_USERNAME="root"

read -rsp "Enter DB_PASSWORD: " DB_PASSWORD
echo ""
[ -z "$DB_PASSWORD" ] && DB_PASSWORD=""

read -rp "Enter REDIS_HOST (default: redis-service): " REDIS_HOST
[ -z "$REDIS_HOST" ] && REDIS_HOST="redis-service"

read -rp "Enter REDIS_PORT (default: 6379): " REDIS_PORT
[ -z "$REDIS_PORT" ] && REDIS_PORT="6379"

read -rp "Enter EXTERNAL_API_URL (default: http://external-api-service/v1): " EXTERNAL_API_URL
[ -z "$EXTERNAL_API_URL" ] && EXTERNAL_API_URL="http://external-api-service/v1"

read -rp "Enter PAYMENT_SERVICE_URL (default: http://payment-service/process): " PAYMENT_SERVICE_URL
[ -z "$PAYMENT_SERVICE_URL" ] && PAYMENT_SERVICE_URL="http://payment-service/process"

echo ""
echo "--- Configuring kubectl for EKS ---"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to cluster."; exit 1; }

echo ""
echo "--- Updating Kubernetes manifests ---"

# Replace placeholders in deployment.yaml using pipe delimiter
sed -i "s|{{IMAGE_URI}}|${IMAGE_URI}|g"                   kubernetes/deployment.yaml
sed -i "s|{{DB_HOST}}|${DB_HOST}|g"                       kubernetes/deployment.yaml
sed -i "s|{{DB_PORT}}|${DB_PORT}|g"                       kubernetes/deployment.yaml
sed -i "s|{{DB_NAME}}|${DB_NAME}|g"                       kubernetes/deployment.yaml
sed -i "s|{{DB_USERNAME}}|${DB_USERNAME}|g"               kubernetes/deployment.yaml
sed -i "s|{{DB_PASSWORD}}|${DB_PASSWORD}|g"               kubernetes/deployment.yaml
sed -i "s|{{REDIS_HOST}}|${REDIS_HOST}|g"                 kubernetes/deployment.yaml
sed -i "s|{{REDIS_PORT}}|${REDIS_PORT}|g"                 kubernetes/deployment.yaml
sed -i "s|{{EXTERNAL_API_URL}}|${EXTERNAL_API_URL}|g"     kubernetes/deployment.yaml
sed -i "s|{{PAYMENT_SERVICE_URL}}|${PAYMENT_SERVICE_URL}|g" kubernetes/deployment.yaml

echo ""
echo "--- Applying Kubernetes manifests ---"
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

echo ""
echo "--- Waiting for deployment rollout ---"
kubectl rollout status deployment/"$APP_NAME" -n "$NAMESPACE" --timeout=300s

echo ""
echo "--- Verifying deployed resources ---"
kubectl get pods,svc,ingress -n "$NAMESPACE"

echo ""
echo "--- Application Access ---"
INGRESS_HOST=$(kubectl get ingress "${APP_NAME}-ingress" -n "$NAMESPACE" \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
echo "Ingress hostname: $INGRESS_HOST"
echo "Application URL:  http://$INGRESS_HOST"
echo ""
echo "Health check:     http://$INGRESS_HOST/health"
echo ""
echo "=============================================="
echo "  Deployment complete!"
echo "=============================================="
echo ""
echo "Rollback command (if needed):"
echo "  kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE"
