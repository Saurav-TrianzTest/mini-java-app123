# Deployment Guide for mini-java-app

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
5. [GCP GKE Deployment](#gcp-gke-deployment)
6. [Configuration Management](#configuration-management)
7. [Troubleshooting](#troubleshooting)
8. [Scaling and Management](#scaling-and-management)
9. [Security Considerations](#security-considerations)
10. [Technology-Specific Notes](#technology-specific-notes)

---

## Overview

This guide provides comprehensive instructions for deploying the **mini-java-app** Spring Boot application to Google Kubernetes Engine (GKE). The application is containerized using Docker and deployed using Kubernetes manifests optimized for GCP.

### Application Details
- **Technology Stack**: Spring Boot 2.7.0
- **Java Version**: 11
- **Build Tool**: Maven
- **Application Port**: 8080
- **Health Endpoint**: `/actuator/health`
- **Context Path**: `/mini-app`

### External Dependencies
The application requires connections to:
- MySQL Database
- Redis Cache
- RabbitMQ Message Broker
- Google Cloud Storage (GCS) Buckets
- External APIs and Payment Services

---

## Prerequisites

### Required Tools
1. **Docker** (version 20.10 or higher)
   - Install from: https://docs.docker.com/get-docker/
   - Verify: `docker --version`

2. **Google Cloud SDK (gcloud)**
   - Install from: https://cloud.google.com/sdk/docs/install
   - Verify: `gcloud --version`

3. **kubectl** (Kubernetes CLI)
   - Install from: https://kubernetes.io/docs/tasks/tools/
   - Verify: `kubectl version --client`

4. **Maven** (for local builds)
   - Install from: https://maven.apache.org/install.html
   - Verify: `mvn --version`

### GCP Requirements
1. **GCP Project** with billing enabled
2. **GKE Cluster** created and running
3. **Artifact Registry** repository (or Docker Hub account)
4. **IAM Permissions**:
   - `container.clusters.get`
   - `container.deployments.create`
   - `artifactregistry.repositories.uploadArtifacts`

### External Services Setup
Before deploying, ensure the following services are available:
- MySQL database instance (Cloud SQL or external)
- Redis instance (Memorystore or external)
- RabbitMQ instance (Cloud Pub/Sub or external)
- GCS buckets created for config, logs, temp, and uploads

---

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd MJA_pr_2024_cz_GCP_comp
```

### 2. Build the Application Locally
```bash
mvn clean package -DskipTests
```

The JAR file will be created in `target/mini-java-app-1.0.0.jar`

### 3. Run with Docker Compose
```bash
# Update environment variables in docker-compose.yml
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### 4. Access the Application
- Application: http://localhost:8080/mini-app
- Health Check: http://localhost:8080/actuator/health
- Info Endpoint: http://localhost:8080/actuator/info

---

## Building and Pushing Docker Images

### Option 1: Using build-push.sh (Linux/macOS)

```bash
cd scripts
chmod +x build-push.sh
./build-push.sh
```

**Interactive Prompts:**
1. Enter image tag (default: latest)
2. Select registry type:
   - **1**: Google Artifact Registry
   - **2**: Docker Hub
3. Provide registry credentials

**For Google Artifact Registry:**
- GCP Project ID
- GCP Region (e.g., us-central1)
- Artifact Registry Repository Name

**For Docker Hub:**
- Docker Hub Username
- Docker Hub Password/Token

### Option 2: Using build-push.bat (Windows)

```cmd
cd scripts
build-push.bat
```

Follow the same interactive prompts as the shell script.

### Manual Build and Push

```bash
# Build image
docker build -t mini-java-app:latest .

# Tag for registry
docker tag mini-java-app:latest <registry>/<repo>/mini-java-app:latest

# Push to registry
docker push <registry>/<repo>/mini-java-app:latest
```

---

## GCP GKE Deployment

### Step 1: Create GKE Cluster (if not exists)

```bash
# Set project
gcloud config set project <PROJECT_ID>

# Create cluster
gcloud container clusters create mini-java-app-cluster \
  --zone us-central1-a \
  --num-nodes 3 \
  --machine-type n1-standard-2 \
  --enable-autoscaling \
  --min-nodes 2 \
  --max-nodes 5

# Get credentials
gcloud container clusters get-credentials mini-java-app-cluster \
  --zone us-central1-a
```

### Step 2: Deploy Using deploy-image.sh (Linux/macOS)

```bash
cd scripts
chmod +x deploy-image.sh
./deploy-image.sh
```

**Interactive Prompts:**
1. GCP Project ID
2. GCP Zone (e.g., us-central1-a)
3. GKE Cluster Name
4. Docker Image URI (full path with tag)
5. Environment variables for external services:
   - Database connection details
   - Redis configuration
   - RabbitMQ settings
   - GCS bucket names
   - Security credentials
   - API keys and endpoints

### Step 3: Deploy Using deploy-image.bat (Windows)

```cmd
cd scripts
deploy-image.bat
```

Follow the same interactive prompts as the shell script.

### Step 4: Verify Deployment

```bash
# Check namespace
kubectl get namespace mini-java-app

# Check pods
kubectl get pods -n mini-java-app

# Check services
kubectl get svc -n mini-java-app

# Check ingress
kubectl get ingress -n mini-java-app

# View logs
kubectl logs -f deployment/mini-java-app -n mini-java-app

# Check pod details
kubectl describe pod <pod-name> -n mini-java-app
```

### Step 5: Access the Application

**Port Forwarding (for testing):**
```bash
kubectl port-forward -n mini-java-app svc/mini-java-app-service 8080:80
```
Then access: http://localhost:8080/mini-app

**Via Ingress (production):**
```bash
# Get ingress IP
kubectl get ingress mini-java-app-ingress -n mini-java-app

# Wait for IP assignment (may take 5-10 minutes)
# Access via: http://<INGRESS_IP>/mini-app
```

---

## Configuration Management

### Environment Variables

The application uses environment variables for configuration. Key variables include:

#### Server Configuration
- `SERVER_PORT`: Application port (default: 8080)
- `SERVER_HOST`: Bind address (default: 0.0.0.0)
- `SERVER_CONTEXT_PATH`: Context path (default: /mini-app)

#### Database Configuration
- `DATABASE_URL`: JDBC connection string
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `DB_POOL_MAX_CONNECTIONS`: Connection pool size (default: 20)

#### Redis Configuration
- `REDIS_HOST`: Redis server hostname
- `REDIS_PORT`: Redis port (default: 6379)
- `REDIS_PASSWORD`: Redis password (optional)

#### Security Configuration
- `JWT_SECRET`: JWT signing secret
- `ADMIN_USERNAME`: Admin username
- `ADMIN_PASSWORD`: Admin password
- `ENCRYPTION_KEY`: Encryption key for sensitive data

#### GCS Configuration
- `GCS_CONFIG_BUCKET`: Configuration bucket name
- `GCS_LOG_BUCKET`: Logs bucket name
- `GCS_TEMP_BUCKET`: Temporary files bucket
- `GCS_UPLOAD_BUCKET`: User uploads bucket

### Kubernetes Secrets (Recommended)

For production, use Kubernetes Secrets instead of plain environment variables:

```bash
# Create secret for database credentials
kubectl create secret generic db-credentials \
  --from-literal=username=<username> \
  --from-literal=password=<password> \
  -n mini-java-app

# Create secret for JWT
kubectl create secret generic jwt-secret \
  --from-literal=secret=<jwt-secret> \
  -n mini-java-app
```

Update `deployment.yaml` to reference secrets:
```yaml
env:
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: db-credentials
      key: password
```

### ConfigMaps

For non-sensitive configuration:

```bash
kubectl create configmap app-config \
  --from-literal=environment=production \
  --from-literal=log-level=INFO \
  -n mini-java-app
```

---

## Troubleshooting

### Common Issues

#### 1. Pods Not Starting

**Symptoms:**
- Pods stuck in `Pending` or `CrashLoopBackOff` state

**Diagnosis:**
```bash
kubectl describe pod <pod-name> -n mini-java-app
kubectl logs <pod-name> -n mini-java-app
```

**Common Causes:**
- Insufficient cluster resources
- Image pull errors
- Missing environment variables
- Health check failures

**Solutions:**
- Scale cluster nodes: `gcloud container clusters resize <cluster> --num-nodes 4`
- Verify image URI is correct
- Check environment variables in deployment.yaml
- Increase `initialDelaySeconds` in health probes

#### 2. Service Not Accessible

**Symptoms:**
- Cannot access application via service or ingress

**Diagnosis:**
```bash
kubectl get svc -n mini-java-app
kubectl get endpoints -n mini-java-app
kubectl describe ingress mini-java-app-ingress -n mini-java-app
```

**Solutions:**
- Verify service selector matches pod labels
- Check ingress backend configuration
- Ensure firewall rules allow traffic
- Wait for ingress IP assignment (can take 5-10 minutes)

#### 3. Database Connection Failures

**Symptoms:**
- Application logs show database connection errors

**Solutions:**
- Verify `DATABASE_URL` is correct
- Check database credentials
- Ensure database allows connections from GKE cluster
- For Cloud SQL, use Cloud SQL Proxy sidecar

#### 4. Health Check Failures

**Symptoms:**
- Pods restarting frequently
- Readiness probe failures

**Solutions:**
- Verify `/actuator/health` endpoint is accessible
- Increase `initialDelaySeconds` to allow JVM startup (60s recommended)
- Check application logs for startup errors
- Verify Spring Boot Actuator is enabled

#### 5. Image Pull Errors

**Symptoms:**
- `ImagePullBackOff` or `ErrImagePull` status

**Solutions:**
- Verify image URI is correct
- Check Artifact Registry permissions
- Configure image pull secrets if using private registry:
```bash
kubectl create secret docker-registry regcred \
  --docker-server=<registry> \
  --docker-username=<username> \
  --docker-password=<password> \
  -n mini-java-app
```

---

## Scaling and Management

### Horizontal Pod Autoscaling (HPA)

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
# Scale to 5 replicas
kubectl scale deployment mini-java-app --replicas=5 -n mini-java-app

# Verify scaling
kubectl get pods -n mini-java-app
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/mini-java-app \
  mini-java-app=<new-image-uri> \
  -n mini-java-app

# Monitor rollout
kubectl rollout status deployment/mini-java-app -n mini-java-app

# Check rollout history
kubectl rollout history deployment/mini-java-app -n mini-java-app
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/mini-java-app -n mini-java-app

# Rollback to specific revision
kubectl rollout undo deployment/mini-java-app --to-revision=2 -n mini-java-app
```

### Resource Monitoring

```bash
# View resource usage
kubectl top pods -n mini-java-app
kubectl top nodes

# View detailed metrics
kubectl describe node <node-name>
```

---

## Security Considerations

### 1. Use Non-Root User
The Dockerfile creates and uses a non-root user (`appuser`) for running the application.

### 2. Secrets Management
- **Never** commit secrets to version control
- Use Kubernetes Secrets for sensitive data
- Consider using Google Secret Manager for centralized secret management

### 3. Network Policies
Implement network policies to restrict pod-to-pod communication:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: mini-java-app-netpol
  namespace: mini-java-app
spec:
  podSelector:
    matchLabels:
      app: mini-java-app
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
```

### 4. RBAC Configuration
Create service accounts with minimal required permissions:

```bash
kubectl create serviceaccount mini-java-app-sa -n mini-java-app
```

### 5. Image Scanning
Scan Docker images for vulnerabilities:

```bash
# Using gcloud
gcloud container images scan <image-uri>

# View scan results
gcloud container images list-tags <image-uri> --show-occurrences
```

### 6. TLS/SSL Configuration
Configure TLS for ingress:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: mini-java-app-ingress
  annotations:
    networking.gke.io/managed-certificates: "mini-java-app-cert"
spec:
  tls:
  - hosts:
    - mini-java-app.example.com
    secretName: mini-java-app-tls
```

---

## Technology-Specific Notes

### Spring Boot Configuration

#### 1. Actuator Endpoints
The application exposes Spring Boot Actuator endpoints:
- `/actuator/health` - Health check (used by Kubernetes probes)
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

#### 2. Profiles
Use Spring profiles for environment-specific configuration:
```bash
# Set profile via environment variable
SPRING_PROFILES_ACTIVE=production
```

#### 3. JVM Tuning
The Dockerfile sets JVM options optimized for containers:
```
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

Adjust based on your resource limits:
- For 1Gi memory limit: `-Xmx768m -Xms512m`
- For 2Gi memory limit: `-Xmx1536m -Xms1024m`

#### 4. Graceful Shutdown
Spring Boot supports graceful shutdown. Configure in `application.properties`:
```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

#### 5. Logging
Configure logging for containerized environments:
```properties
logging.level.root=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Maven Build Optimization

#### 1. Dependency Caching
The Dockerfile uses layer caching for dependencies:
```dockerfile
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests
```

#### 2. Skip Tests in Docker Build
Tests are skipped during Docker build for faster builds. Run tests separately:
```bash
mvn test
```

### Google Cloud Integration

#### 1. Cloud SQL Proxy
For Cloud SQL connections, add a sidecar container:
```yaml
- name: cloud-sql-proxy
  image: gcr.io/cloudsql-docker/gce-proxy:latest
  command:
  - "/cloud_sql_proxy"
  - "-instances=<PROJECT>:<REGION>:<INSTANCE>=tcp:3306"
```

#### 2. Workload Identity
Use Workload Identity for GCP service authentication:
```bash
# Create service account
gcloud iam service-accounts create mini-java-app-sa

# Bind to Kubernetes service account
gcloud iam service-accounts add-iam-policy-binding \
  mini-java-app-sa@<PROJECT>.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:<PROJECT>.svc.id.goog[mini-java-app/mini-java-app-sa]"
```

#### 3. GCS Access
Grant GCS permissions to service account:
```bash
gcloud projects add-iam-policy-binding <PROJECT> \
  --member="serviceAccount:mini-java-app-sa@<PROJECT>.iam.gserviceaccount.com" \
  --role="roles/storage.objectAdmin"
```

---

## Additional Resources

### Documentation
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Google Kubernetes Engine Documentation](https://cloud.google.com/kubernetes-engine/docs)
- [Kubernetes Documentation](https://kubernetes.io/docs/home/)
- [Docker Documentation](https://docs.docker.com/)

### Monitoring and Observability
- Set up Google Cloud Monitoring for GKE
- Configure log aggregation with Cloud Logging
- Use Spring Boot Actuator with Prometheus for metrics

### Support
For issues or questions:
1. Check application logs: `kubectl logs -f deployment/mini-java-app -n mini-java-app`
2. Review Kubernetes events: `kubectl get events -n mini-java-app`
3. Consult the troubleshooting section above

---

## Conclusion

This deployment guide provides comprehensive instructions for deploying the mini-java-app Spring Boot application to GCP GKE. Follow the steps carefully, and refer to the troubleshooting section for common issues. For production deployments, ensure all security considerations are implemented and external services are properly configured.

**Happy Deploying! 🚀**
