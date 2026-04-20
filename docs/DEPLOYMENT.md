# Mini Java Application - Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Docker Deployment](#docker-deployment)
5. [Azure AKS Deployment](#azure-aks-deployment)
6. [Configuration Management](#configuration-management)
7. [Troubleshooting](#troubleshooting)
8. [Security Considerations](#security-considerations)
9. [Technology-Specific Notes](#technology-specific-notes)

---

## Overview

This guide provides comprehensive instructions for deploying the Mini Java Application, a Spring Boot-based Java 11 application with Maven build system. The application is containerized using Docker and can be deployed to Azure Kubernetes Service (AKS).

**Application Details:**
- **Technology Stack**: Java 11, Spring Boot 2.7.0, Maven
- **Build Tool**: Maven 3.9.4
- **Package Type**: JAR
- **Application Port**: 8080
- **Management Port**: 8081 (Spring Boot Actuator)
- **Health Endpoint**: `/actuator/health`
- **Base Image**: Amazon Corretto 11 (explicit)

**External Dependencies:**
- MySQL Database
- Redis Cache
- Azure Blob Storage
- RabbitMQ Message Broker
- External APIs (Payment Service, External API)

---

## Prerequisites

### Required Software

#### For Local Development:
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **Java JDK**: Version 11 or higher (for local builds)
- **Maven**: Version 3.6 or higher (optional, Docker build uses containerized Maven)

#### For Azure AKS Deployment:
- **Azure CLI**: Version 2.40 or higher
  ```bash
  # Install Azure CLI (Linux/macOS)
  curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
  
  # Install Azure CLI (Windows)
  # Download from: https://aka.ms/installazurecliwindows
  ```

- **kubectl**: Kubernetes command-line tool
  ```bash
  # Install kubectl (Linux/macOS)
  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
  chmod +x kubectl
  sudo mv kubectl /usr/local/bin/
  
  # Install kubectl (Windows)
  # Download from: https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/
  ```

- **Azure Subscription**: Active Azure subscription with permissions to create AKS clusters

### Azure Resources Setup

Before deploying to AKS, ensure you have:

1. **Azure Container Registry (ACR)** or **Docker Hub account**
2. **Azure AKS Cluster** (or permissions to create one)
3. **Azure Resource Group**

#### Create Azure Container Registry:
```bash
# Login to Azure
az login

# Create resource group
az group create --name myResourceGroup --location eastus

# Create Azure Container Registry
az acr create --resource-group myResourceGroup \
  --name myregistry --sku Basic

# Enable admin access (for authentication)
az acr update -n myregistry --admin-enabled true
```

#### Create Azure AKS Cluster:
```bash
# Create AKS cluster
az aks create \
  --resource-group myResourceGroup \
  --name myAKSCluster \
  --node-count 2 \
  --node-vm-size Standard_DS2_v2 \
  --enable-managed-identity \
  --attach-acr myregistry \
  --generate-ssh-keys

# Get AKS credentials
az aks get-credentials --resource-group myResourceGroup --name myAKSCluster
```

---

## Local Development Setup

### Using Docker Compose

Docker Compose provides the easiest way to run the application locally for development and testing.

#### Step 1: Configure Environment Variables

Create a `.env` file in the project root (optional, or use docker-compose.yml defaults):

```env
# Server Configuration
SERVER_PORT=8080
SERVER_HOST=0.0.0.0

# Database Configuration (point to your MySQL instance)
DATABASE_URL=jdbc:mysql://mysql-host:3306/mini_app_db
DB_USERNAME=root
DB_PASSWORD=password123
DB_HOST=mysql-host
DB_PORT=3306
DB_NAME=mini_app_db

# Redis Configuration (point to your Redis instance)
REDIS_HOST=redis-host
REDIS_PORT=6379
REDIS_PASSWORD=

# Azure Blob Storage
AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=...
AZURE_STORAGE_CONTAINER=mini-app-storage

# Security
JWT_SECRET=your-secret-key
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
```

#### Step 2: Start the Application

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

#### Step 3: Access the Application

- **Application**: http://localhost:8080/mini-app
- **Health Check**: http://localhost:8080/actuator/health
- **Actuator Info**: http://localhost:8080/actuator/info

---

## Docker Deployment

### Building the Docker Image

The application uses a multi-stage Dockerfile optimized for Java applications:

```bash
# Build the Docker image manually
docker build -t mini-java-app:latest .

# Build with specific tag
docker build -t mini-java-app:v1.0.0 .
```

### Using Build Scripts

The project includes automated build and push scripts for both Linux/macOS and Windows.

#### Linux/macOS:

```bash
# Make script executable
chmod +x scripts/build-push.sh

# Run the script
./scripts/build-push.sh
```

The script will prompt you to:
1. Select registry type (Azure ACR or Docker Hub)
2. Enter registry credentials
3. Enter image tag (defaults to 'latest')

#### Windows:

```cmd
# Run the script
scripts\build-push.bat
```

The script provides the same interactive prompts as the Linux version.

### Manual Push to Registry

#### Azure Container Registry:

```bash
# Login to ACR
az acr login --name myregistry

# Tag the image
docker tag mini-java-app:latest myregistry.azurecr.io/mini-java-app:latest

# Push to ACR
docker push myregistry.azurecr.io/mini-java-app:latest
```

#### Docker Hub:

```bash
# Login to Docker Hub
docker login

# Tag the image
docker tag mini-java-app:latest username/mini-java-app:latest

# Push to Docker Hub
docker push username/mini-java-app:latest
```

---

## Azure AKS Deployment

### Deployment Architecture

The application is deployed to Azure AKS with the following components:

- **Namespace**: `mini-java-app` (isolated namespace for the application)
- **Deployment**: 2 replicas with rolling update strategy
- **Service**: ClusterIP service exposing ports 80 (HTTP) and 8081 (management)
- **Ingress**: Azure Application Gateway Ingress Controller for external access
- **Health Probes**: Liveness and readiness probes using Spring Boot Actuator

### Prerequisites

1. **AKS Cluster**: Running and accessible
2. **kubectl**: Configured with AKS credentials
3. **Docker Image**: Built and pushed to a container registry
4. **External Services**: MySQL, Redis, RabbitMQ, Azure Blob Storage configured

### Deployment Steps

#### Option 1: Using Deployment Scripts (Recommended)

##### Linux/macOS:

```bash
# Make script executable
chmod +x scripts/deploy-image.sh

# Run the deployment script
./scripts/deploy-image.sh
```

##### Windows:

```cmd
# Run the deployment script
scripts\deploy-image.bat
```

The script will prompt you for:
1. Azure Resource Group name
2. AKS Cluster name
3. Docker image URI
4. Environment variables for external services:
   - Database connection details (URL, username, password, host, port, name)
   - Redis connection details (host, port, password)
   - External API URLs and keys
   - Payment service credentials
   - Azure Storage connection string
   - Security credentials (JWT secret, admin credentials)
   - Monitoring endpoint details
   - RabbitMQ connection details

#### Option 2: Manual Deployment

##### Step 1: Configure kubectl

```bash
# Get AKS credentials
az aks get-credentials --resource-group myResourceGroup --name myAKSCluster

# Verify connection
kubectl cluster-info
```

##### Step 2: Update Kubernetes Manifests

Edit the `kubernetes/deployment.yaml` file and replace placeholders:

- `{{IMAGE_URI}}`: Your Docker image URI (e.g., `myregistry.azurecr.io/mini-java-app:latest`)
- `{{DATABASE_URL}}`: Your MySQL connection string
- `{{DB_USERNAME}}`: Database username
- `{{DB_PASSWORD}}`: Database password
- `{{REDIS_HOST}}`: Redis host address
- `{{AZURE_STORAGE_CONNECTION_STRING}}`: Azure Storage connection string
- Other environment variables as needed

##### Step 3: Apply Kubernetes Manifests

```bash
# Create namespace
kubectl apply -f kubernetes/namespace.yaml

# Deploy application
kubectl apply -f kubernetes/deployment.yaml

# Create service
kubectl apply -f kubernetes/service.yaml

# Create ingress
kubectl apply -f kubernetes/ingress.yaml
```

##### Step 4: Verify Deployment

```bash
# Check deployment status
kubectl rollout status deployment/mini-java-app -n mini-java-app

# View pods
kubectl get pods -n mini-java-app

# View services
kubectl get svc -n mini-java-app

# View ingress
kubectl get ingress -n mini-java-app

# View logs
kubectl logs -f deployment/mini-java-app -n mini-java-app
```

### Accessing the Application

#### Internal Access (within cluster):

```
http://mini-java-app-service.mini-java-app.svc.cluster.local
```

#### External Access:

Get the ingress external IP:

```bash
kubectl get ingress mini-java-app-ingress -n mini-java-app
```

Access the application using the ingress host or IP address.

### Scaling the Application

```bash
# Scale to 3 replicas
kubectl scale deployment mini-java-app --replicas=3 -n mini-java-app

# Enable autoscaling (HPA)
kubectl autoscale deployment mini-java-app \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n mini-java-app
```

### Rolling Updates

```bash
# Update the image
kubectl set image deployment/mini-java-app \
  mini-java-app=myregistry.azurecr.io/mini-java-app:v2.0.0 \
  -n mini-java-app

# Check rollout status
kubectl rollout status deployment/mini-java-app -n mini-java-app

# Rollback if needed
kubectl rollout undo deployment/mini-java-app -n mini-java-app
```

---

## Configuration Management

### Environment Variables

The application uses environment variables for configuration. Key variables include:

#### Server Configuration:
- `SERVER_PORT`: Application port (default: 8080)
- `SERVER_HOST`: Bind address (default: 0.0.0.0)
- `SERVER_CONTEXT_PATH`: Application context path (default: /mini-app)

#### Database Configuration:
- `DATABASE_URL`: JDBC connection string
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `DB_HOST`: Database host
- `DB_PORT`: Database port (default: 3306)
- `DB_NAME`: Database name

#### Redis Configuration:
- `REDIS_HOST`: Redis server host
- `REDIS_PORT`: Redis server port (default: 6379)
- `REDIS_PASSWORD`: Redis password (optional)

#### Azure Blob Storage:
- `AZURE_STORAGE_CONNECTION_STRING`: Azure Storage connection string
- `AZURE_STORAGE_CONTAINER`: Container name (default: mini-app-storage)

#### Security:
- `JWT_SECRET`: JWT signing secret
- `ADMIN_USERNAME`: Admin username
- `ADMIN_PASSWORD`: Admin password
- `ENCRYPTION_KEY`: Encryption key for sensitive data

#### External Services:
- `EXTERNAL_API_URL`: External API base URL
- `EXTERNAL_API_KEY`: External API authentication key
- `PAYMENT_SERVICE_URL`: Payment service endpoint
- `PAYMENT_SERVICE_USERNAME`: Payment service username
- `PAYMENT_SERVICE_PASSWORD`: Payment service password

#### Messaging:
- `RABBITMQ_HOST`: RabbitMQ server host
- `RABBITMQ_PORT`: RabbitMQ server port (default: 5672)
- `RABBITMQ_USERNAME`: RabbitMQ username
- `RABBITMQ_PASSWORD`: RabbitMQ password

### Using Kubernetes Secrets

For sensitive data, use Kubernetes Secrets instead of plain environment variables:

```bash
# Create secret for database credentials
kubectl create secret generic db-credentials \
  --from-literal=username=root \
  --from-literal=password=password123 \
  -n mini-java-app

# Create secret for Azure Storage
kubectl create secret generic azure-storage \
  --from-literal=connection-string="DefaultEndpointsProtocol=https;..." \
  -n mini-java-app
```

Update `deployment.yaml` to use secrets:

```yaml
env:
- name: DB_USERNAME
  valueFrom:
    secretKeyRef:
      name: db-credentials
      key: username
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: db-credentials
      key: password
```

### Using ConfigMaps

For non-sensitive configuration:

```bash
# Create ConfigMap
kubectl create configmap app-config \
  --from-literal=server.port=8080 \
  --from-literal=logging.level=INFO \
  -n mini-java-app
```

---

## Troubleshooting

### Common Issues

#### 1. Pod Not Starting

**Symptoms**: Pods stuck in `Pending`, `CrashLoopBackOff`, or `Error` state

**Diagnosis**:
```bash
# Check pod status
kubectl get pods -n mini-java-app

# Describe pod for events
kubectl describe pod <pod-name> -n mini-java-app

# Check logs
kubectl logs <pod-name> -n mini-java-app

# Check previous logs if pod restarted
kubectl logs <pod-name> -n mini-java-app --previous
```

**Common Causes**:
- Insufficient resources (CPU/memory)
- Image pull errors (check registry credentials)
- Missing environment variables
- Database connection failures
- Health check failures

**Solutions**:
- Verify image URI is correct
- Check resource limits in deployment.yaml
- Verify all required environment variables are set
- Ensure external services (MySQL, Redis) are accessible
- Check network policies and firewall rules

#### 2. Database Connection Failures

**Symptoms**: Application logs show database connection errors

**Diagnosis**:
```bash
# Check database connectivity from pod
kubectl exec -it <pod-name> -n mini-java-app -- /bin/sh
# Inside pod:
# nc -zv mysql-host 3306
```

**Solutions**:
- Verify `DATABASE_URL`, `DB_HOST`, `DB_PORT` are correct
- Ensure database credentials are valid
- Check network connectivity between AKS and database
- Verify database firewall rules allow AKS IP ranges
- For Azure MySQL, add AKS subnet to firewall rules

#### 3. Health Check Failures

**Symptoms**: Pods restarting frequently, readiness probe failures

**Diagnosis**:
```bash
# Check health endpoint manually
kubectl exec -it <pod-name> -n mini-java-app -- curl http://localhost:8080/actuator/health
```

**Solutions**:
- Increase `initialDelaySeconds` in health probes (JVM startup time)
- Verify Spring Boot Actuator is enabled
- Check application logs for startup errors
- Ensure health endpoint is accessible

#### 4. Ingress Not Working

**Symptoms**: Cannot access application externally

**Diagnosis**:
```bash
# Check ingress status
kubectl get ingress -n mini-java-app
kubectl describe ingress mini-java-app-ingress -n mini-java-app

# Check ingress controller logs
kubectl logs -n kube-system -l app=ingress-azure
```

**Solutions**:
- Verify Azure Application Gateway Ingress Controller is installed
- Check ingress annotations are correct
- Verify DNS records point to ingress IP
- Check Azure Application Gateway backend health
- Ensure service is correctly exposing pods

#### 5. Out of Memory Errors

**Symptoms**: Pods killed with OOMKilled status

**Diagnosis**:
```bash
# Check pod events
kubectl describe pod <pod-name> -n mini-java-app | grep -A 5 "Last State"
```

**Solutions**:
- Increase memory limits in deployment.yaml
- Adjust JVM heap size (`JAVA_OPTS` environment variable)
- Reduce `-Xmx` value if it exceeds container memory limit
- Monitor memory usage: `kubectl top pods -n mini-java-app`

### Debugging Commands

```bash
# Get all resources in namespace
kubectl get all -n mini-java-app

# Describe deployment
kubectl describe deployment mini-java-app -n mini-java-app

# View events
kubectl get events -n mini-java-app --sort-by='.lastTimestamp'

# Execute commands in pod
kubectl exec -it <pod-name> -n mini-java-app -- /bin/sh

# Port forward for local testing
kubectl port-forward deployment/mini-java-app 8080:8080 -n mini-java-app

# View resource usage
kubectl top pods -n mini-java-app
kubectl top nodes

# Check service endpoints
kubectl get endpoints -n mini-java-app
```

### AKS-Specific Troubleshooting

#### Check AKS Cluster Health:
```bash
# Check node status
kubectl get nodes

# Check system pods
kubectl get pods -n kube-system

# Check AKS diagnostics
az aks show --resource-group myResourceGroup --name myAKSCluster
```

#### Check Azure Application Gateway:
```bash
# Get Application Gateway details
az network application-gateway show \
  --resource-group myResourceGroup \
  --name myAppGateway

# Check backend health
az network application-gateway show-backend-health \
  --resource-group myResourceGroup \
  --name myAppGateway
```

---

## Security Considerations

### Best Practices

1. **Use Secrets for Sensitive Data**
   - Never hardcode passwords or API keys
   - Use Kubernetes Secrets or Azure Key Vault
   - Rotate secrets regularly

2. **Network Security**
   - Use Network Policies to restrict pod-to-pod communication
   - Enable Azure Private Link for database connections
   - Use Azure Firewall or NSGs to control egress traffic

3. **Image Security**
   - Scan images for vulnerabilities (Azure Security Center, Trivy)
   - Use minimal base images (distroless, alpine)
   - Keep base images and dependencies updated
   - Use specific image tags (not `latest`)

4. **RBAC and Access Control**
   - Enable Azure AD integration for AKS
   - Use RBAC to limit access to namespaces
   - Follow principle of least privilege

5. **Pod Security**
   - Run containers as non-root user (already configured)
   - Use Pod Security Policies or Pod Security Standards
   - Set resource limits to prevent resource exhaustion
   - Enable read-only root filesystem where possible

6. **Monitoring and Logging**
   - Enable Azure Monitor for containers
   - Configure log aggregation (Azure Log Analytics)
   - Set up alerts for security events
   - Monitor for anomalous behavior

### Azure Key Vault Integration

For production deployments, integrate with Azure Key Vault:

```bash
# Install Azure Key Vault Provider for Secrets Store CSI Driver
helm repo add csi-secrets-store-provider-azure https://azure.github.io/secrets-store-csi-driver-provider-azure/charts
helm install csi csi-secrets-store-provider-azure/csi-secrets-store-provider-azure

# Create SecretProviderClass
kubectl apply -f - <<EOF
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: azure-kvname
  namespace: mini-java-app
spec:
  provider: azure
  parameters:
    usePodIdentity: "false"
    useVMManagedIdentity: "true"
    userAssignedIdentityID: "<identity-client-id>"
    keyvaultName: "<key-vault-name>"
    objects: |
      array:
        - |
          objectName: db-password
          objectType: secret
        - |
          objectName: jwt-secret
          objectType: secret
    tenantId: "<tenant-id>"
EOF
```

---

## Technology-Specific Notes

### Java 11 and Spring Boot

#### JVM Configuration

The application uses optimized JVM settings for containerized environments:

```bash
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UnlockExperimentalVMOptions"
```

**Key Settings**:
- `-Xmx512m`: Maximum heap size (adjust based on container memory)
- `-Xms256m`: Initial heap size
- `-XX:+UseContainerSupport`: Enable container awareness
- `-XX:MaxRAMPercentage=75.0`: Use 75% of container memory for heap

#### Spring Boot Actuator

Health checks use Spring Boot Actuator endpoints:

- **Health**: `/actuator/health` - Overall application health
- **Info**: `/actuator/info` - Application information
- **Metrics**: `/actuator/metrics` - Application metrics

Configure actuator in `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

#### Maven Build

The Dockerfile uses Maven for building:

```dockerfile
# Build stage
FROM maven:3.9.4-eclipse-temurin-11 AS builder
WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B
```

**Important**: The build uses system `mvn` command, not Maven wrapper (`mvnw`).

#### Dependency Caching

The multi-stage build optimizes dependency caching:
1. Copy `pom.xml` first
2. Download dependencies (cached layer)
3. Copy source code
4. Build application

This ensures dependencies are only re-downloaded when `pom.xml` changes.

### Azure-Specific Integrations

#### Azure Blob Storage

The application uses Azure Blob Storage for file operations:

```java
BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
    .connectionString(AZURE_STORAGE_CONNECTION_STRING)
    .buildClient();
```

Ensure `AZURE_STORAGE_CONNECTION_STRING` is configured.

#### Azure MySQL

For Azure Database for MySQL:
- Enable SSL: Add `?useSSL=true&requireSSL=true` to JDBC URL
- Configure firewall rules to allow AKS subnet
- Use Azure AD authentication for enhanced security

---

## Monitoring and Observability

### Azure Monitor Integration

Enable Azure Monitor for containers:

```bash
# Enable monitoring addon
az aks enable-addons \
  --resource-group myResourceGroup \
  --name myAKSCluster \
  --addons monitoring
```

### Prometheus and Grafana

Deploy Prometheus and Grafana for metrics:

```bash
# Add Prometheus Helm repo
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace
```

### Application Insights

Integrate with Azure Application Insights for APM:

1. Add Application Insights dependency to `pom.xml`
2. Configure instrumentation key
3. Enable auto-instrumentation

---

## Maintenance and Operations

### Backup and Disaster Recovery

1. **Database Backups**: Configure automated backups for Azure MySQL
2. **Configuration Backups**: Store Kubernetes manifests in Git
3. **Disaster Recovery Plan**: Document recovery procedures

### Updating the Application

1. Build new image with updated code
2. Push to registry with new tag
3. Update deployment with new image
4. Monitor rollout status
5. Rollback if issues occur

### Cost Optimization

- Use Azure Reserved Instances for AKS nodes
- Enable cluster autoscaler
- Use spot instances for non-critical workloads
- Monitor and optimize resource requests/limits

---

## Support and Resources

### Documentation Links

- [Azure AKS Documentation](https://docs.microsoft.com/en-us/azure/aks/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Docker Documentation](https://docs.docker.com/)

### Useful Commands Reference

```bash
# AKS Management
az aks list
az aks show --resource-group <rg> --name <cluster>
az aks get-credentials --resource-group <rg> --name <cluster>

# Kubernetes Operations
kubectl get all -n mini-java-app
kubectl logs -f deployment/mini-java-app -n mini-java-app
kubectl describe pod <pod-name> -n mini-java-app
kubectl exec -it <pod-name> -n mini-java-app -- /bin/sh
kubectl port-forward deployment/mini-java-app 8080:8080 -n mini-java-app

# Scaling
kubectl scale deployment mini-java-app --replicas=3 -n mini-java-app
kubectl autoscale deployment mini-java-app --cpu-percent=70 --min=2 --max=10 -n mini-java-app

# Updates and Rollbacks
kubectl set image deployment/mini-java-app mini-java-app=<new-image> -n mini-java-app
kubectl rollout status deployment/mini-java-app -n mini-java-app
kubectl rollout undo deployment/mini-java-app -n mini-java-app
```

---

## Conclusion

This deployment guide provides comprehensive instructions for deploying the Mini Java Application to Azure AKS. Follow the steps carefully, and refer to the troubleshooting section if you encounter issues.

For production deployments, ensure you:
- Use Kubernetes Secrets for sensitive data
- Enable monitoring and logging
- Configure autoscaling
- Implement proper backup and disaster recovery procedures
- Follow security best practices

For additional support, consult the Azure and Kubernetes documentation or contact your DevOps team.
