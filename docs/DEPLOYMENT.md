# Deployment Guide – mini-java-app on AWS EKS

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Local Development with Docker Compose](#local-development-with-docker-compose)
5. [Build and Push Docker Image](#build-and-push-docker-image)
6. [AWS EKS Deployment](#aws-eks-deployment)
7. [Kubernetes Manifest Reference](#kubernetes-manifest-reference)
8. [Configuration Management](#configuration-management)
9. [Scaling and Management](#scaling-and-management)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)
12. [Java-Specific Notes](#java-specific-notes)

---

## Overview

**Application**: mini-java-app  
**Framework**: Spring Boot 2.7.0  
**Java Version**: 11  
**Build Tool**: Maven  
**Package Type**: JAR  
**Application Port**: 8080  
**Health Endpoint**: `/health`  
**Target Platform**: AWS EKS (Elastic Kubernetes Service)

---

## Prerequisites

### Local Development
| Tool | Version | Purpose |
|------|---------|---------|
| Docker | 20.10+ | Container build and run |
| Docker Compose | 2.x | Local multi-container orchestration |
| Java JDK | 11+ | Local build (optional) |
| Maven | 3.9+ | Local build (optional) |

### AWS EKS Deployment
| Tool | Version | Purpose |
|------|---------|---------|
| AWS CLI | 2.x | AWS authentication and ECR access |
| kubectl | 1.27+ | Kubernetes cluster management |
| eksctl | 0.150+ | EKS cluster creation (optional) |

### AWS IAM Permissions Required
```
ecr:GetAuthorizationToken
ecr:BatchCheckLayerAvailability
ecr:GetDownloadUrlForLayer
ecr:BatchGetImage
ecr:PutImage
ecr:InitiateLayerUpload
ecr:UploadLayerPart
ecr:CompleteLayerUpload
ecr:CreateRepository
ecr:DescribeRepositories
eks:DescribeCluster
eks:ListClusters
```

---

## Project Structure

```
mini-java-app/
├── Dockerfile                  # Multi-stage Docker build
├── docker-compose.yml          # Local development compose file
├── .dockerignore               # Docker build exclusions
├── pom.xml                     # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/test/
│       │   ├── MiniApp.java        # Spring Boot entry point
│       │   ├── HealthController.java  # /health endpoint
│       │   └── DatabaseService.java   # DB/cache connectivity
│       └── resources/
│           └── application.properties
├── kubernetes/
│   ├── namespace.yaml          # Kubernetes namespace
│   ├── deployment.yaml         # Application deployment
│   ├── service.yaml            # ClusterIP service
│   └── ingress.yaml            # AWS ALB ingress
├── scripts/
│   ├── build-push.sh           # Linux/macOS build & push
│   ├── build-push.bat          # Windows build & push
│   ├── deploy-image.sh         # Linux/macOS EKS deploy
│   └── deploy-image.bat        # Windows EKS deploy
└── docs/
    └── DEPLOYMENT.md           # This file
```

---

## Local Development with Docker Compose

### 1. Configure Environment Variables

Create a `.env` file in the project root (never commit this file):

```bash
DB_HOST=your-database-host
DB_PORT=3306
DB_NAME=mini_app_db
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password
REDIS_HOST=your-redis-host
REDIS_PORT=6379
EXTERNAL_API_URL=http://your-api-host/v1
PAYMENT_SERVICE_URL=http://your-payment-service/process
```

### 2. Build and Start the Application

```bash
# Build and start
docker compose up --build

# Start in background
docker compose up -d --build

# View logs
docker compose logs -f mini-java-app

# Stop
docker compose down
```

### 3. Verify the Application

```bash
# Health check
curl http://localhost:8080/health

# Expected response:
# {"status":"UP","application":"mini-java-app"}
```

---

## Build and Push Docker Image

### Linux / macOS

```bash
# Make the script executable
chmod +x scripts/build-push.sh

# Run from project root
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (defaults to `latest`)
2. Select registry type (AWS ECR or Docker Hub)
3. Provide registry credentials

### Windows

```cmd
scripts\build-push.bat
```

### Manual Docker Build

```bash
# Build
docker build -t mini-java-app:latest .

# Tag for ECR
docker tag mini-java-app:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest

# Push to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest
```

---

## AWS EKS Deployment

### Step 1: Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### Step 2: Create or Connect to EKS Cluster

```bash
# Create a new cluster (if needed)
eksctl create cluster \
  --name my-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4

# Connect to existing cluster
aws eks update-kubeconfig --region us-east-1 --name my-cluster

# Verify connectivity
kubectl cluster-info
kubectl get nodes
```

### Step 3: Install AWS Load Balancer Controller (for Ingress)

```bash
# Add the EKS chart repository
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install the controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=my-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Step 4: Deploy the Application

#### Using the deploy script (recommended)

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will prompt for:
- AWS Region
- EKS Cluster Name
- Docker image URI
- All required environment variables (DB, Redis, external services)

#### Manual deployment

```bash
# Update the image URI in deployment.yaml
sed -i 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest|g' \
  kubernetes/deployment.yaml

# Update other placeholders
sed -i 's|{{DB_HOST}}|your-db-host|g' kubernetes/deployment.yaml
sed -i 's|{{DB_PORT}}|3306|g' kubernetes/deployment.yaml
sed -i 's|{{DB_NAME}}|mini_app_db|g' kubernetes/deployment.yaml
sed -i 's|{{DB_USERNAME}}|your-db-user|g' kubernetes/deployment.yaml
sed -i 's|{{DB_PASSWORD}}|your-db-password|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_HOST}}|your-redis-host|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_PORT}}|6379|g' kubernetes/deployment.yaml
sed -i 's|{{EXTERNAL_API_URL}}|http://your-api/v1|g' kubernetes/deployment.yaml
sed -i 's|{{PAYMENT_SERVICE_URL}}|http://your-payment/process|g' kubernetes/deployment.yaml

# Apply manifests in order
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# Wait for rollout
kubectl rollout status deployment/mini-java-app -n mini-java-app

# Verify
kubectl get pods,svc,ingress -n mini-java-app
```

### Step 5: Access the Application

```bash
# Get the ALB hostname
kubectl get ingress mini-java-app-ingress -n mini-java-app \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

# Test health endpoint
curl http://<ALB_HOSTNAME>/health
```

---

## Kubernetes Manifest Reference

### namespace.yaml
Creates the `mini-java-app` namespace to isolate all application resources.

### deployment.yaml
- **Replicas**: 2 (high availability)
- **Image**: Placeholder `{{IMAGE_URI}}` replaced at deploy time
- **Resources**: requests: 250m CPU / 512Mi RAM; limits: 500m CPU / 1Gi RAM
- **Liveness Probe**: `GET /health` – starts after 60s, every 15s
- **Readiness Probe**: `GET /health` – starts after 30s, every 10s
- **Security**: Runs as non-root user (UID 1000)

### service.yaml
- **Type**: ClusterIP (internal cluster access only)
- **Port mapping**: 80 → 8080

### ingress.yaml
- **Controller**: AWS Load Balancer Controller (ALB)
- **Scheme**: internet-facing
- **Target type**: IP (direct pod routing)
- **Health check path**: `/health`

---

## Configuration Management

### Environment Variables Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Application HTTP port |
| `SPRING_PROFILES_ACTIVE` | `docker` | Spring profile |
| `DB_HOST` | `localhost` | MySQL database hostname |
| `DB_PORT` | `3306` | MySQL database port |
| `DB_NAME` | `mini_app_db` | MySQL database name |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | _(empty)_ | MySQL password |
| `REDIS_HOST` | `redis-service` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `EXTERNAL_API_URL` | `http://external-api-service/v1` | External API base URL |
| `PAYMENT_SERVICE_URL` | `http://payment-service/process` | Payment service URL |
| `CONFIG_FILE_PATH` | `/app/config/app.properties` | Config file path |
| `LOG_FILE_PATH` | `/app/logs/mini-app.log` | Log file path |
| `LOG_DIR_PATH` | `/app/logs` | Log directory path |
| `JAVA_OPTS` | `-Xmx512m -Xms256m ...` | JVM options |

### Using Kubernetes Secrets for Sensitive Values

```bash
# Create a secret for database credentials
kubectl create secret generic mini-java-app-secrets \
  --from-literal=DB_PASSWORD=your-password \
  -n mini-java-app

# Reference in deployment.yaml
# - name: DB_PASSWORD
#   valueFrom:
#     secretKeyRef:
#       name: mini-java-app-secrets
#       key: DB_PASSWORD
```

---

## Scaling and Management

### Horizontal Pod Autoscaler

```bash
# Create HPA
kubectl autoscale deployment mini-java-app \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n mini-java-app

# Check HPA status
kubectl get hpa -n mini-java-app
```

### Manual Scaling

```bash
kubectl scale deployment mini-java-app --replicas=4 -n mini-java-app
```

### Rolling Update

```bash
# Update image
kubectl set image deployment/mini-java-app \
  mini-java-app=123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:v2.0.0 \
  -n mini-java-app

# Monitor rollout
kubectl rollout status deployment/mini-java-app -n mini-java-app
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/mini-java-app -n mini-java-app

# Rollback to specific revision
kubectl rollout history deployment/mini-java-app -n mini-java-app
kubectl rollout undo deployment/mini-java-app --to-revision=2 -n mini-java-app
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n mini-java-app

# Describe pod for events
kubectl describe pod <pod-name> -n mini-java-app

# View pod logs
kubectl logs <pod-name> -n mini-java-app
kubectl logs <pod-name> -n mini-java-app --previous  # crashed pod
```

### Common Issues

**CrashLoopBackOff**
- Check logs: `kubectl logs <pod-name> -n mini-java-app`
- Verify environment variables are set correctly
- Ensure database/Redis hosts are reachable from the cluster
- Check JVM memory settings (increase if OOMKilled)

**ImagePullBackOff**
- Verify ECR repository exists and image tag is correct
- Check IAM role attached to EKS node group has ECR pull permissions
- Verify `aws-auth` ConfigMap includes the correct IAM role

**Liveness/Readiness Probe Failures**
- JVM startup can be slow; increase `initialDelaySeconds` if needed
- Verify `/health` endpoint returns HTTP 200
- Check application logs for startup errors

**Ingress Not Getting ALB Hostname**
- Verify AWS Load Balancer Controller is installed and running
- Check controller logs: `kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller`
- Ensure the EKS node IAM role has the required ALB permissions

### Useful Commands

```bash
# Get all resources in namespace
kubectl get all -n mini-java-app

# Execute shell in running pod
kubectl exec -it <pod-name> -n mini-java-app -- /bin/sh

# Port-forward for local testing
kubectl port-forward svc/mini-java-app-service 8080:80 -n mini-java-app

# View resource usage
kubectl top pods -n mini-java-app
kubectl top nodes
```

---

## Security Considerations

1. **Non-root container**: The application runs as UID 1000 (non-root)
2. **Secrets management**: Use Kubernetes Secrets (or AWS Secrets Manager) for passwords and API keys — never hardcode in manifests
3. **Network policies**: Consider adding Kubernetes NetworkPolicy to restrict pod-to-pod traffic
4. **Image scanning**: Enable ECR image scanning on push to detect vulnerabilities
5. **RBAC**: Apply least-privilege IAM roles to EKS node groups
6. **TLS**: Configure HTTPS on the ALB ingress using ACM certificates:
   ```yaml
   alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:123456789012:certificate/xxx
   alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
   ```
7. **Resource limits**: Always set CPU/memory limits to prevent noisy-neighbour issues

---

## Java-Specific Notes

### JVM Container Awareness
The Dockerfile sets the following JVM flags for proper container behaviour:
```
-XX:+UseContainerSupport        # Respect container CPU/memory limits
-XX:MaxRAMPercentage=75.0       # Use 75% of container memory for heap
-XX:+UnlockExperimentalVMOptions
-Xmx512m -Xms256m              # Explicit heap bounds
```

### Spring Boot Actuator
The application exposes `/health` via Spring Boot Actuator. The management endpoint is configured in `application.properties`:
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
```

### Graceful Shutdown
The Kubernetes deployment sets `terminationGracePeriodSeconds: 30` to allow in-flight requests to complete before the pod is terminated.

### Startup Time
Spring Boot applications typically take 15–30 seconds to start. The liveness probe is configured with `initialDelaySeconds: 60` to avoid premature restarts during JVM warm-up.

### Logging
Application logs are written to stdout/stderr (Docker best practice). Use `kubectl logs` or a log aggregation solution (e.g., AWS CloudWatch Container Insights, Fluentd) to collect and analyse logs.
