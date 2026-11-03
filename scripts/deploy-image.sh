#!/bin/bash
set -e
set -o pipefail

# ============================================================
# deploy-image.sh – Deploy McontainerMonoTest to GCP GKE
# ============================================================

APP_NAME="mcontainermonotest"
NAMESPACE="mcontainermonotest"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "============================================================"
echo "  McontainerMonoTest – GKE Deployment"
echo "============================================================"

# ---- Collect GCP / cluster details ----
read -rp "Enter GCP Project ID: " GCP_PROJECT
if [ -z "$GCP_PROJECT" ]; then
    echo "ERROR: GCP Project ID is required." >&2
    exit 1
fi

read -rp "Enter GCP Zone (e.g. us-central1-a): " GCP_ZONE
if [ -z "$GCP_ZONE" ]; then
    echo "ERROR: GCP Zone is required." >&2
    exit 1
fi

read -rp "Enter GKE Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
    echo "ERROR: GKE Cluster Name is required." >&2
    exit 1
fi

read -rp "Enter full Docker image URI (e.g. us-central1-docker.pkg.dev/my-project/my-repo/mcontainermonotest:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
    echo "ERROR: Docker image URI is required." >&2
    exit 1
fi

echo ""
echo "---- Optional: Application Environment Variables ----"
echo "Press Enter to skip any variable."

read -rp "Enter DB_HOST (database host): " DB_HOST
read -rp "Enter DB_PORT [3306]: " DB_PORT
DB_PORT="${DB_PORT:-3306}"
read -rp "Enter DB_NAME [mini_app_db]: " DB_NAME
DB_NAME="${DB_NAME:-mini_app_db}"
read -rp "Enter DB_USERNAME [root]: " DB_USERNAME
DB_USERNAME="${DB_USERNAME:-root}"
read -rsp "Enter DB_PASSWORD: " DB_PASSWORD
echo ""
read -rp "Enter REDIS_HOST [redis-service]: " REDIS_HOST
REDIS_HOST="${REDIS_HOST:-redis-service}"
read -rp "Enter REDIS_PORT [6379]: " REDIS_PORT
REDIS_PORT="${REDIS_PORT:-6379}"
read -rp "Enter EXTERNAL_API_URL: " EXTERNAL_API_URL
EXTERNAL_API_URL="${EXTERNAL_API_URL:-http://api.example.com:8080/v1}"
read -rp "Enter PAYMENT_SERVICE_URL: " PAYMENT_SERVICE_URL
PAYMENT_SERVICE_URL="${PAYMENT_SERVICE_URL:-https://payment.internal.company.com/process}"

# ---- Configure kubectl ----
echo ""
echo "Configuring kubectl for cluster: ${CLUSTER_NAME}..."
gcloud container clusters get-credentials "${CLUSTER_NAME}" \
    --zone "${GCP_ZONE}" \
    --project "${GCP_PROJECT}"

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to cluster." >&2; exit 1; }

# ---- Update Kubernetes manifests with actual values ----
echo ""
echo "Updating Kubernetes manifests..."

DEPLOY_YAML="${PROJECT_ROOT}/kubernetes/deployment.yaml"
cp "${DEPLOY_YAML}" "${DEPLOY_YAML}.bak"

sed -i "s|{{IMAGE_URI}}|${IMAGE_URI}|g"                         "${DEPLOY_YAML}"
sed -i "s|{{DB_HOST}}|${DB_HOST}|g"                             "${DEPLOY_YAML}"
sed -i "s|{{DB_PORT}}|${DB_PORT}|g"                             "${DEPLOY_YAML}"
sed -i "s|{{DB_NAME}}|${DB_NAME}|g"                             "${DEPLOY_YAML}"
sed -i "s|{{DB_USERNAME}}|${DB_USERNAME}|g"                     "${DEPLOY_YAML}"
sed -i "s|{{DB_PASSWORD}}|${DB_PASSWORD}|g"                     "${DEPLOY_YAML}"
sed -i "s|{{REDIS_HOST}}|${REDIS_HOST}|g"                       "${DEPLOY_YAML}"
sed -i "s|{{REDIS_PORT}}|${REDIS_PORT}|g"                       "${DEPLOY_YAML}"
sed -i "s|{{EXTERNAL_API_URL}}|${EXTERNAL_API_URL}|g"           "${DEPLOY_YAML}"
sed -i "s|{{PAYMENT_SERVICE_URL}}|${PAYMENT_SERVICE_URL}|g"     "${DEPLOY_YAML}"

# ---- Apply manifests in order ----
echo ""
echo "Applying Kubernetes manifests..."

echo "  [1/4] Applying namespace..."
kubectl apply -f "${PROJECT_ROOT}/kubernetes/namespace.yaml"

echo "  [2/4] Applying deployment..."
kubectl apply -f "${PROJECT_ROOT}/kubernetes/deployment.yaml"

echo "  [3/4] Applying service..."
kubectl apply -f "${PROJECT_ROOT}/kubernetes/service.yaml"

echo "  [4/4] Applying ingress..."
kubectl apply -f "${PROJECT_ROOT}/kubernetes/ingress.yaml"

# ---- Restore original manifest (keep placeholders for re-runs) ----
mv "${DEPLOY_YAML}.bak" "${DEPLOY_YAML}"

# ---- Wait for rollout ----
echo ""
echo "Waiting for deployment rollout..."
kubectl rollout status deployment/"${APP_NAME}" -n "${NAMESPACE}" --timeout=300s

# ---- Verify resources ----
echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n "${NAMESPACE}"

# ---- Display access URL ----
echo ""
INGRESS_IP=$(kubectl get ingress "${APP_NAME}-ingress" -n "${NAMESPACE}" \
    -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")

echo "============================================================"
echo "  Deployment complete!"
echo "  Namespace : ${NAMESPACE}"
echo "  Image     : ${IMAGE_URI}"
if [ "$INGRESS_IP" != "pending" ] && [ -n "$INGRESS_IP" ]; then
    echo "  Access URL: http://${INGRESS_IP}"
else
    echo "  Ingress IP is still being provisioned. Run:"
    echo "    kubectl get ingress -n ${NAMESPACE}"
fi
echo ""
echo "  Rollback command (if needed):"
echo "    kubectl rollout undo deployment/${APP_NAME} -n ${NAMESPACE}"
echo "============================================================"
