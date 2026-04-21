# Mini Java Application - Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Docker Deployment](#docker-deployment)
5. [GCP GKE Deployment](#gcp-gke-deployment)
6. [Configuration Management](#configuration-management)
7. [Troubleshooting](#troubleshooting)
8. [Security Considerations](#security-considerations)
9. [Technology-Specific Notes](#technology-specific-notes)

---

## Overview

This guide provides comprehensive instructions for deploying the Mini Java Application to various environments, with a focus on Google Cloud Platform (GCP) Google Kubernetes Engine (GKE).

**Application Details:**
- **Technology Stack**: Java 11, Spring Boot 2.7.0, Maven
- **Application Type**: Spring Boot Web Application
- **Default Port**: 8080
- **Health Endpoint**: `/mini-app/actuator/health`
- **Base Image**: Amazon Corretto 11 (JDK 11)

**External Dependencies:**
- MySQL Database
- Redis Cache
- Google Cloud Storage (GCS) Buckets
- External APIs (Payment Service, Monitoring, etc.)
- RabbitMQ Message Queue

---

## Prerequisites

### Required Tools

#### For Local Development:
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **Java JDK**: Version 11 or higher (for local builds)
- **Maven**: Version 3.6 or higher

#### For GCP GKE Deployment:
- **Google Cloud SDK (gcloud)**: Latest version
  ```bash
  # Install gcloud CLI
  curl https://sdk.cloud.google.com | bash
  exec -l $SHELL
  gcloud init
  ```

- **kubectl**: Kubernetes command-line tool
  ```bash
  # Install kubectl via gcloud
  gcloud components install kubectl
  ```

- **GCP Account**: Active GCP account with appropriate permissions
- **GKE Cluster**: Pre-configured GKE cluster (or create one)

### GCP Permissions Required
- `container.clusters.get`
- `container.clusters.update`
- `container.pods.create`
- `container.deployments.create`
- `artifactregistry.repositories.uploadArtifacts` (for Artifact Registry)

### External Services Setup
Before deploying, ensure the following external services are available:

1. **MySQL Database**
   - Host: Accessible from your deployment environment
   - Database: `mini_app_db` (or custom name)
   - User credentials with appropriate permissions

2. **Redis Cache**
   - Host: Accessible from your deployment environment
   - Port: 6379 (default)

3. **Google Cloud Storage Buckets**
   - `gs://app-config-bucket` - Application configuration
   - `gs://app-logs-bucket` - Application logs
   - `gs://app-temp-bucket` - Temporary files
   - `gs://app-uploads-bucket` - User uploads

4. **RabbitMQ Message Queue** (if using messaging features)
   - Host: Accessible from your deployment environment
   - Port: 5672 (default)

---

## Local Development Setup

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd mini-java-app-component
```

### Step 2: Configure Environment Variables
Create a `.env` file in the project root:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=mini_app_db
DB_USERNAME=root
DB_PASSWORD=password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# GCS Buckets
GCS_CONFIG_BUCKET=gs://app-config-bucket
GCS_LOG_BUCKET=gs://app-logs-bucket
GCS_TEMP_BUCKET=gs://app-temp-bucket
GCS_UPLOAD_BUCKET=gs://app-uploads-bucket

# External Services
EXTERNAL_API_URL=http://localhost:8081/v1
PAYMENT_SERVICE_URL=http://localhost:8082/process

# Security
JWT_SECRET=your-secret-key
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
ENCRYPTION_KEY=your-encryption-key

# Application
ENVIRONMENT=development
DEBUG_ENABLED=true
LOGGING_LEVEL=DEBUG
```

### Step 3: Build the Application Locally
```bash
# Using Maven
mvn clean package -DskipTests

# The JAR file will be created in target/mini-java-app-1.0.0.jar
```

### Step 4: Run with Docker Compose
```bash
# Build and start the application
docker-compose up --build

# Run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### Step 5: Verify Local Deployment
```bash
# Check application health
curl http://localhost:8080/mini-app/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## Docker Deployment

### Step 1: Build Docker Image

#### Option A: Build Locally
```bash
# Build the Docker image
docker build -t mini-java-app:latest .

# Verify the image
docker images | grep mini-java-app
```

#### Option B: Use Build Script (Recommended)

**Linux/macOS:**
```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

**Windows:**
```cmd
scripts\build-push.bat
```

The script will prompt you to:
1. Select registry type (Google Artifact Registry or Docker Hub)
2. Enter registry credentials
3. Enter image tag (default: latest)
4. Build and push the image automatically

### Step 2: Run Docker Container
```bash
# Run the container
docker run -d \
  --name mini-java-app \
  -p 8080:8080 \
  -e DB_HOST=mysql-host \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=password \
  mini-java-app:latest

# View logs
docker logs -f mini-java-app

# Stop the container
docker stop mini-java-app
docker rm mini-java-app
```

---

## GCP GKE Deployment

### Step 1: Set Up GCP Project

```bash
# Authenticate with GCP
gcloud auth login

# Set your project
gcloud config set project YOUR_PROJECT_ID

# Enable required APIs
gcloud services enable container.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

### Step 2: Create GKE Cluster (if not exists)

```bash
# Create a GKE cluster
gcloud container clusters create mini-java-app-cluster \
  --zone us-central1-a \
  --num-nodes 3 \
  --machine-type n1-standard-2 \
  --enable-autoscaling \
  --min-nodes 2 \
  --max-nodes 5

# Get cluster credentials
gcloud container clusters get-credentials mini-java-app-cluster \
  --zone us-central1-a
```

### Step 3: Create Artifact Registry Repository

```bash
# Create repository
gcloud artifacts repositories create mini-java-app-repo \
  --repository-format=docker \
  --location=us-central1 \
  --description="Mini Java Application Docker Repository"

# Configure Docker authentication
gcloud auth configure-docker us-central1-docker.pkg.dev
```

### Step 4: Build and Push Docker Image

**Linux/macOS:**
```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

**Windows:**
```cmd
scripts\build-push.bat
```

Follow the prompts:
1. Select "1" for Google Artifact Registry
2. Enter GCP Project ID: `YOUR_PROJECT_ID`
3. Enter GCP Region: `us-central1`
4. Enter Repository Name: `mini-java-app-repo`
5. Enter image tag: `v1.0.0` (or `latest`)

The script will:
- Authenticate with GCP
- Build the Docker image
- Push to Artifact Registry

### Step 5: Deploy to GKE

**Linux/macOS:**
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

**Windows:**
```cmd
scripts\deploy-image.bat
```

The deployment script will prompt you for:

1. **GCP Configuration:**
   - GCP Project ID
   - GCP Zone (e.g., `us-central1-a`)
   - GKE Cluster Name

2. **Docker Image URI:**
   - Full image path (e.g., `us-central1-docker.pkg.dev/project/repo/mini-java-app:v1.0.0`)

3. **Environment Variables:**
   - Database credentials (DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD)
   - Redis configuration (REDIS_HOST, REDIS_PORT)
   - GCS bucket paths
   - External service URLs
   - Security credentials (JWT_SECRET, ADMIN credentials)
   - Application settings (ENVIRONMENT, LOGGING_LEVEL)

The script will:
- Configure kubectl for your GKE cluster
- Update Kubernetes manifests with your configuration
- Deploy namespace, deployment, service, and ingress
- Wait for deployment to complete
- Display deployment status and access information

### Step 6: Verify GKE Deployment

```bash
# Check namespace
kubectl get namespace mini-java-app

# Check pods
kubectl get pods -n mini-java-app

# Check services
kubectl get svc -n mini-java-app

# Check ingress
kubectl get ingress -n mini-java-app

# View pod logs
kubectl logs -n mini-java-app -l app=mini-java-app

# Describe pod for detailed information
kubectl describe pod -n mini-java-app -l app=mini-java-app
```

### Step 7: Access the Application

#### Internal Access (within cluster):
```bash
# Port-forward for testing
kubectl port-forward -n mini-java-app svc/mini-java-app-service 8080:80

# Access via browser or curl
curl http://localhost:8080/mini-app/actuator/health
```

#### External Access (via Ingress):
```bash
# Get ingress IP
kubectl get ingress mini-java-app-ingress -n mini-java-app

# Wait for IP to be assigned (may take 5-10 minutes)
# Access via: http://<INGRESS_IP>/mini-app/actuator/health
```

---

## Configuration Management

### Environment Variables

The application uses environment variables for configuration. All variables are defined in:
- `docker-compose.yml` (for local Docker deployment)
- `kubernetes/deployment.yaml` (for Kubernetes deployment)

**Key Configuration Variables:**

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | `8080` |
| `DB_HOST` | Database host | `mysql-service` |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `mini_app_db` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `password` |
| `REDIS_HOST` | Redis cache host | `redis-service` |
| `REDIS_PORT` | Redis port | `6379` |
| `GCS_CONFIG_BUCKET` | GCS config bucket | `gs://app-config-bucket` |
| `GCS_LOG_BUCKET` | GCS log bucket | `gs://app-logs-bucket` |
| `JWT_SECRET` | JWT secret key | (required) |
| `ENVIRONMENT` | Environment name | `production` |
| `LOGGING_LEVEL` | Log level | `INFO` |

### Kubernetes Secrets (Recommended for Production)

For production deployments, use Kubernetes Secrets for sensitive data:

```bash
# Create secret for database credentials
kubectl create secret generic db-credentials \
  --from-literal=username=root \
  --from-literal=password=your-secure-password \
  -n mini-java-app

# Create secret for JWT
kubectl create secret generic jwt-secret \
  --from-literal=secret=your-jwt-secret \
  -n mini-java-app

# Update deployment.yaml to use secrets
# Replace:
#   - name: DB_PASSWORD
#     value: "{{DB_PASSWORD}}"
# With:
#   - name: DB_PASSWORD
#     valueFrom:
#       secretKeyRef:
#         name: db-credentials
#         key: password
```

### ConfigMaps for Non-Sensitive Configuration

```bash
# Create ConfigMap
kubectl create configmap app-config \
  --from-literal=environment=production \
  --from-literal=logging-level=INFO \
  -n mini-java-app

# Use in deployment.yaml
#   - name: ENVIRONMENT
#     valueFrom:
#       configMapKeyRef:
#         name: app-config
#         key: environment
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Pod Fails to Start

**Symptoms:**
- Pod status: `CrashLoopBackOff` or `Error`

**Diagnosis:**
```bash
# Check pod status
kubectl get pods -n mini-java-app

# View pod logs
kubectl logs -n mini-java-app -l app=mini-java-app

# Describe pod for events
kubectl describe pod -n mini-java-app -l app=mini-java-app
```

**Common Causes:**
- **Database connection failure**: Verify DB_HOST, DB_PORT, credentials
- **Missing environment variables**: Check deployment.yaml configuration
- **Image pull errors**: Verify image URI and registry authentication
- **Insufficient resources**: Check resource limits in deployment.yaml

**Solutions:**
```bash
# Update environment variables
kubectl edit deployment mini-java-app -n mini-java-app

# Restart deployment
kubectl rollout restart deployment/mini-java-app -n mini-java-app

# Scale down and up
kubectl scale deployment mini-java-app -n mini-java-app --replicas=0
kubectl scale deployment mini-java-app -n mini-java-app --replicas=2
```

#### 2. Health Check Failures

**Symptoms:**
- Pods restarting frequently
- Readiness probe failures

**Diagnosis:**
```bash
# Check health endpoint
kubectl port-forward -n mini-java-app svc/mini-java-app-service 8080:80
curl http://localhost:8080/mini-app/actuator/health
```

**Solutions:**
- Increase `initialDelaySeconds` in liveness/readiness probes (Java apps need time to start)
- Verify health endpoint path: `/mini-app/actuator/health`
- Check application logs for startup errors

#### 3. Service Not Accessible

**Symptoms:**
- Cannot access application via service or ingress

**Diagnosis:**
```bash
# Check service
kubectl get svc -n mini-java-app
kubectl describe svc mini-java-app-service -n mini-java-app

# Check endpoints
kubectl get endpoints -n mini-java-app

# Check ingress
kubectl get ingress -n mini-java-app
kubectl describe ingress mini-java-app-ingress -n mini-java-app
```

**Solutions:**
```bash
# Verify service selector matches pod labels
kubectl get pods -n mini-java-app --show-labels

# Test service internally
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -n mini-java-app -- \
  curl http://mini-java-app-service/mini-app/actuator/health

# Check ingress backend health
kubectl get ingress mini-java-app-ingress -n mini-java-app -o yaml
```

#### 4. Image Pull Errors

**Symptoms:**
- Pod status: `ImagePullBackOff` or `ErrImagePull`

**Diagnosis:**
```bash
kubectl describe pod -n mini-java-app -l app=mini-java-app
```

**Solutions:**
```bash
# Verify image exists
gcloud artifacts docker images list us-central1-docker.pkg.dev/PROJECT/REPO

# Re-authenticate Docker
gcloud auth configure-docker us-central1-docker.pkg.dev

# Create image pull secret (if using private registry)
kubectl create secret docker-registry artifact-registry-secret \
  --docker-server=us-central1-docker.pkg.dev \
  --docker-username=_json_key \
  --docker-password="$(cat key.json)" \
  -n mini-java-app

# Add to deployment.yaml:
# spec:
#   imagePullSecrets:
#   - name: artifact-registry-secret
```

#### 5. Database Connection Issues

**Symptoms:**
- Application logs show database connection errors
- Pods crash after startup

**Diagnosis:**
```bash
# Check database connectivity from pod
kubectl exec -it -n mini-java-app <pod-name> -- /bin/sh
# Inside pod:
# nc -zv mysql-host 3306
```

**Solutions:**
- Verify DB_HOST resolves correctly (use FQDN if external)
- Check database firewall rules allow GKE cluster IPs
- Verify database credentials are correct
- Ensure database exists and user has permissions

#### 6. GCS Access Issues

**Symptoms:**
- Application cannot read/write to GCS buckets
- Permission denied errors in logs

**Solutions:**
```bash
# Verify GCS buckets exist
gsutil ls gs://app-config-bucket

# Check GKE service account permissions
gcloud projects get-iam-policy PROJECT_ID

# Grant GCS permissions to GKE service account
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:GKE_SA@PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/storage.objectAdmin"

# Or use Workload Identity (recommended)
# https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity
```

### Debugging Commands

```bash
# View all resources in namespace
kubectl get all -n mini-java-app

# Get detailed pod information
kubectl describe pod <pod-name> -n mini-java-app

# View pod logs (last 100 lines)
kubectl logs -n mini-java-app <pod-name> --tail=100

# Follow logs in real-time
kubectl logs -n mini-java-app -l app=mini-java-app -f

# Execute commands in pod
kubectl exec -it -n mini-java-app <pod-name> -- /bin/sh

# View events
kubectl get events -n mini-java-app --sort-by='.lastTimestamp'

# Check resource usage
kubectl top pods -n mini-java-app
kubectl top nodes
```

---

## Security Considerations

### 1. Secrets Management
- **Never commit secrets to version control**
- Use Kubernetes Secrets for sensitive data
- Consider using Google Secret Manager for production
- Rotate secrets regularly

### 2. Network Security
- Use Network Policies to restrict pod-to-pod communication
- Enable GKE Private Cluster for production
- Use Cloud Armor for DDoS protection
- Implement TLS/SSL for ingress (use managed certificates)

### 3. Image Security
- Scan images for vulnerabilities (use GCP Container Analysis)
- Use minimal base images (distroless or alpine)
- Run containers as non-root user (already implemented)
- Keep base images updated

### 4. Access Control
- Use GCP IAM for fine-grained access control
- Enable Workload Identity for pod-to-GCP service authentication
- Implement RBAC for Kubernetes resources
- Use least privilege principle

### 5. Monitoring and Logging
- Enable GKE logging and monitoring
- Set up alerts for critical events
- Use Cloud Logging for centralized log management
- Implement audit logging

---

## Technology-Specific Notes

### Java Spring Boot Configuration

#### JVM Memory Settings
The application uses optimized JVM settings for containerized environments:
```bash
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

**Tuning Recommendations:**
- For production, adjust `-Xmx` based on container memory limits
- Use `-XX:MaxRAMPercentage=75.0` to leave memory for non-heap usage
- Monitor GC logs: Add `-Xlog:gc*:file=/tmp/gc.log` for debugging

#### Spring Boot Actuator
Health check endpoint: `/mini-app/actuator/health`

**Available Endpoints:**
- `/mini-app/actuator/health` - Health status
- `/mini-app/actuator/info` - Application information
- `/mini-app/actuator/metrics` - Application metrics

**Enable Additional Endpoints:**
Update `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

#### Spring Profiles
The application supports Spring profiles for environment-specific configuration:

```bash
# Set active profile
export SPRING_PROFILES_ACTIVE=production

# Or in Kubernetes deployment:
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "production"
```

Create profile-specific configuration files:
- `application-dev.properties`
- `application-staging.properties`
- `application-production.properties`

#### Maven Build Optimization
For faster builds, use Maven daemon:
```bash
# Install Maven daemon
brew install mvnd  # macOS
# or download from https://github.com/apache/maven-mvnd

# Build with mvnd
mvnd clean package -DskipTests
```

### Google Cloud Storage Integration

The application uses Google Cloud Storage for file operations. Ensure:

1. **Service Account Permissions:**
   ```bash
   gcloud projects add-iam-policy-binding PROJECT_ID \
     --member="serviceAccount:SA@PROJECT_ID.iam.gserviceaccount.com" \
     --role="roles/storage.objectAdmin"
   ```

2. **Workload Identity (Recommended):**
   ```bash
   # Enable Workload Identity on cluster
   gcloud container clusters update CLUSTER_NAME \
     --workload-pool=PROJECT_ID.svc.id.goog

   # Create Kubernetes service account
   kubectl create serviceaccount gcs-sa -n mini-java-app

   # Bind to GCP service account
   gcloud iam service-accounts add-iam-policy-binding \
     GSA@PROJECT_ID.iam.gserviceaccount.com \
     --role roles/iam.workloadIdentityUser \
     --member "serviceAccount:PROJECT_ID.svc.id.goog[mini-java-app/gcs-sa]"

   # Annotate Kubernetes service account
   kubectl annotate serviceaccount gcs-sa -n mini-java-app \
     iam.gke.io/gcp-service-account=GSA@PROJECT_ID.iam.gserviceaccount.com

   # Update deployment to use service account
   # spec:
   #   serviceAccountName: gcs-sa
   ```

### Database Connection Pooling

The application uses connection pooling for MySQL. Optimize settings:

```properties
# application.properties
database.pool.max-connections=20
database.pool.timeout=5000
database.pool.min-idle=5
database.pool.max-lifetime=1800000
```

**Recommendations:**
- Set `max-connections` based on expected load
- Monitor connection pool metrics
- Use Cloud SQL Proxy for secure connections to Cloud SQL

---

## Scaling and Performance

### Horizontal Pod Autoscaling (HPA)

```bash
# Create HPA
kubectl autoscale deployment mini-java-app \
  -n mini-java-app \
  --cpu-percent=70 \
  --min=2 \
  --max=10

# Check HPA status
kubectl get hpa -n mini-java-app

# Describe HPA
kubectl describe hpa mini-java-app -n mini-java-app
```

### Vertical Pod Autoscaling (VPA)

```bash
# Install VPA (if not already installed)
kubectl apply -f https://github.com/kubernetes/autoscaler/releases/download/vertical-pod-autoscaler-0.13.0/vpa-v0.13.0.yaml

# Create VPA
cat <<EOF | kubectl apply -f -
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: mini-java-app-vpa
  namespace: mini-java-app
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mini-java-app
  updatePolicy:
    updateMode: "Auto"
EOF
```

### Performance Tuning

1. **JVM Tuning:**
   - Use G1GC for better latency: `-XX:+UseG1GC`
   - Enable JIT compilation: `-XX:+TieredCompilation`
   - Optimize for throughput: `-XX:+UseStringDeduplication`

2. **Spring Boot Tuning:**
   - Enable lazy initialization: `spring.main.lazy-initialization=true`
   - Optimize Tomcat threads: `server.tomcat.threads.max=200`
   - Enable HTTP/2: `server.http2.enabled=true`

3. **Database Tuning:**
   - Use prepared statements
   - Enable query caching
   - Optimize connection pool size

---

## Rollback Procedures

### Rollback Kubernetes Deployment

```bash
# View rollout history
kubectl rollout history deployment/mini-java-app -n mini-java-app

# Rollback to previous version
kubectl rollout undo deployment/mini-java-app -n mini-java-app

# Rollback to specific revision
kubectl rollout undo deployment/mini-java-app -n mini-java-app --to-revision=2

# Check rollout status
kubectl rollout status deployment/mini-java-app -n mini-java-app
```

### Blue-Green Deployment

For zero-downtime deployments:

1. Deploy new version with different label (e.g., `version: v2`)
2. Test new version internally
3. Update service selector to point to new version
4. Monitor for issues
5. Delete old version if successful

---

## Monitoring and Observability

### GCP Monitoring

```bash
# Enable GKE monitoring
gcloud container clusters update CLUSTER_NAME \
  --enable-cloud-logging \
  --enable-cloud-monitoring \
  --zone ZONE
```

### Prometheus Integration

```bash
# Add Prometheus annotations to deployment
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/mini-app/actuator/prometheus"
```

### Logging Best Practices

- Use structured logging (JSON format)
- Include correlation IDs for request tracing
- Set appropriate log levels per environment
- Use Cloud Logging for centralized log management

---

## Maintenance

### Regular Tasks

1. **Update Dependencies:**
   ```bash
   mvn versions:display-dependency-updates
   mvn versions:use-latest-releases
   ```

2. **Update Base Images:**
   - Monitor for security updates
   - Rebuild images regularly
   - Test thoroughly before deploying

3. **Database Maintenance:**
   - Regular backups
   - Index optimization
   - Query performance analysis

4. **Certificate Renewal:**
   - Monitor certificate expiration
   - Use managed certificates for automatic renewal

---

## Support and Resources

### Documentation
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [GKE Documentation](https://cloud.google.com/kubernetes-engine/docs)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Docker Documentation](https://docs.docker.com/)

### Useful Links
- [GCP Console](https://console.cloud.google.com/)
- [Artifact Registry](https://console.cloud.google.com/artifacts)
- [GKE Clusters](https://console.cloud.google.com/kubernetes/list)

### Getting Help
- Check application logs: `kubectl logs -n mini-java-app -l app=mini-java-app`
- Review GKE events: `kubectl get events -n mini-java-app`
- Contact DevOps team for infrastructure issues
- Refer to troubleshooting section above

---

## Appendix

### A. Complete Environment Variable Reference

See [Configuration Management](#configuration-management) section for complete list.

### B. Kubernetes Manifest Templates

All Kubernetes manifests are located in the `kubernetes/` directory:
- `namespace.yaml` - Namespace definition
- `deployment.yaml` - Application deployment
- `service.yaml` - Service definition
- `ingress.yaml` - Ingress configuration

### C. Script Reference

- `scripts/build-push.sh` - Build and push Docker image (Linux/macOS)
- `scripts/build-push.bat` - Build and push Docker image (Windows)
- `scripts/deploy-image.sh` - Deploy to GKE (Linux/macOS)
- `scripts/deploy-image.bat` - Deploy to GKE (Windows)

---

**Document Version**: 1.0.0  
**Last Updated**: 2024  
**Maintained By**: DevOps Team
