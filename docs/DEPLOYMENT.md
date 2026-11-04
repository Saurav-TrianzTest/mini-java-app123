# Deployment Guide — MiniJavaContainer

## Prerequisites

### System Requirements
- Java 11 (JDK)
- Maven 3.8+
- Docker 20.10+
- AWS CLI 2
- kubectl 1.21+
- eksctl 0.99+

### AWS Permissions
The deploying user/role needs the following IAM permissions:
- `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:CompleteLayerUpload`, `ecr:UploadLayerPart`, `ecr:PutImage`, `ecr:InitiateLayerUpload`
- `eks:DescribeCluster`, `eks:AccessKubernetesCluster` (via `eks:DescribeCluster` and `eks:ListClusters`)
- `sts:GetCallerIdentity`

## Local Setup with Docker Compose

1. Ensure Docker is running.
2. Set required environment variables (credentials):
   ```bash
   export DB_HOST=localhost
   export DB_PORT=3306
   export DB_NAME=mini_app_db
   export DB_USERNAME=your_db_user
   export DB_PASSWORD=your_db_password
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   export EXTERNAL_API_URL=http://api.example.com:8080/v1
   export PAYMENT_SERVICE_URL=https://payment.internal.company.com/process
   ```
3. Build and run locally:
   ```bash
   docker-compose up --build
   ```
4. The application starts on port 8080 (APP_PORT) and the health check endpoint on port 8081 (HEALTH_PORT).
   - Health check: `curl http://localhost:8081/health`

## Build and Push to AWS ECR

### Using the build-push script (Linux/macOS)
```bash
./scripts/build-push.sh
```
The script will:
1. Ask you to select the target platform (AWS ECR or Docker Hub).
2. For AWS ECR: prompt for AWS region, registry URL, repository name, and image tag.
3. Log in to ECR, create the repository if it doesn't exist, build the Docker image, and push it.

### Using the build-push script (Windows)
```cmd
scripts\build-push.bat
```

### Manual build and push
```bash
# 1. Log in to AWS ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <registry-url>

# 2. Create ECR repository (if it doesn't exist)
aws ecr create-repository --repository-name mini-java-container --region us-east-1

# 3. Build the image
docker build -f Dockerfile -t <registry-url>/mini-java-container:latest .

# 4. Push the image
docker push <registry-url>/mini-java-container:latest
```

## AWS EKS Deployment

### EKS Cluster Setup

1. **Create an EKS cluster** (if you don't have one):
   ```bash
   eksctl create cluster --name <cluster-name> --region us-east-1 --nodegroup-name standard-workers --node-type t3.medium --nodes 2 --nodes-max 3
   ```

2. **Configure kubectl**:
   ```bash
   aws eks update-kubeconfig --region us-east-1 --name <cluster-name>
   ```

3. **Verify connectivity**:
   ```bash
   kubectl cluster-info
   ```

### Deploy the Application

1. **Set environment variables** for the application configuration:
   ```bash
   export DB_HOST=<your-db-host>
   export DB_PORT=3306
   export DB_NAME=mini_app_db
   export REDIS_HOST=<your-redis-host>
   export REDIS_PORT=6379
   export EXTERNAL_API_URL=<your-external-api-url>
   export PAYMENT_SERVICE_URL=<your-payment-service-url>
   export DB_USERNAME=<your-db-username>
   export DB_PASSWORD=<your-db-password>
   ```

2. **Run the deploy script** (Linux/macOS):
   ```bash
   ./scripts/deploy-image.sh
   ```
   The script will:
   - Ask for AWS region and EKS cluster name.
   - Ask for the full image URI.
   - Ask for each environment variable value (or press Enter to skip).
   - Ask for credential values (not echoed).
   - Configure kubectl for the EKS cluster.
   - Create the Kubernetes Secret for credentials.
   - Replace placeholders in deployment.yaml.
   - Apply namespace, deployment, service, and ingress manifests.
   - Wait for the deployment to be ready.
   - Show the deployment status and application URL.

3. **Run the deploy script** (Windows):
   ```cmd
   scripts\deploy-image.bat
   ```

### Manual Deployment

1. **Create the namespace**:
   ```bash
   kubectl apply -f kubernetes/namespace.yaml
   ```

2. **Create the Secret** for credentials:
   ```bash
   kubectl create secret generic mini-java-container-secrets -n mini-java-container \
     --from-literal=db-username=<your-db-username> \
     --from-literal=db-password=<your-db-password> \
     --dry-run=client -o yaml | kubectl apply -f -
   ```

3. **Replace placeholders in deployment.yaml**:
   ```bash
   sed -i.bak "s|{{IMAGE_URI}}|<registry-url>/mini-java-container:latest|g" kubernetes/deployment.yaml
   sed -i.bak "s|{{DB_HOST}}|<your-db-host>|g" kubernetes/deployment.yaml
   # ... replace other placeholders
   rm -f kubernetes/deployment.yaml.bak
   ```

4. **Apply the manifests**:
   ```bash
   kubectl apply -f kubernetes/deployment.yaml
   kubectl apply -f kubernetes/service.yaml
   kubectl apply -f kubernetes/ingress.yaml
   ```

5. **Wait for the deployment to be ready**:
   ```bash
   kubectl rollout status deployment/mini-java-container -n mini-java-container
   ```

## Kubernetes Manifests

### namespace.yaml
Creates a dedicated namespace `mini-java-container` for the application.

### deployment.yaml
- **Replicas**: 2 (for high availability)
- **Container**: `mini-java-container` with the image from ECR
- **Ports**: 8080 (application), 8081 (health check)
- **Environment variables**: Non-credential values use ConfigMap placeholders; credentials come from Kubernetes Secrets
- **Resources**: Requests (250m CPU, 512Mi memory), Limits (500m CPU, 1Gi memory)
- **Liveness probe**: HTTP GET on `/health` at port 8081 (30s initial delay, 30s interval)
- **Readiness probe**: HTTP GET on `/health` at port 8081 (15s initial delay, 10s interval)
- **Volumes**: ConfigMap for config, emptyDir for logs/uploads/temp

### service.yaml
- **Type**: ClusterIP
- **Port**: 80 → 8080 (container port)
- **Selector**: app: mini-java-container

### ingress.yaml
- **Annotations**: AWS Load Balancer Controller (ALB, internet-facing)
- **Host**: mini-java-container.example.com
- **Path**: / → service on port 80

## Configuration Management

### Environment Variables

| Variable | Description | Default | Source |
|---|---|---|---|
| `APP_PORT` | Application server port | 8080 | ConfigMap |
| `HEALTH_PORT` | Health check endpoint port | 8081 | ConfigMap |
| `CONFIG_PATH` | Configuration file path | /app/config/app.properties | ConfigMap |
| `LOG_PATH` | Log file path | /app/logs/mini-app.log | ConfigMap |
| `LOG_DIR` | Log directory | /app/logs | ConfigMap |
| `DB_HOST` | Database host | localhost | ConfigMap |
| `DB_PORT` | Database port | 3306 | ConfigMap |
| `DB_NAME` | Database name | mini_app_db | ConfigMap |
| `DB_USERNAME` | Database username | — | Secret |
| `DB_PASSWORD` | Database password | — | Secret |
| `REDIS_HOST` | Redis host | localhost | ConfigMap |
| `REDIS_PORT` | Redis port | 6379 | ConfigMap |
| `EXTERNAL_API_URL` | External API base URL | http://api.example.com:8080/v1 | ConfigMap |
| `PAYMENT_SERVICE_URL` | Payment service URL | https://payment.internal.company.com/process | ConfigMap |

### Secrets

Credentials are stored in a Kubernetes Secret named `mini-java-container-secrets`:
- `db-username`: Database username
- `db-password`: Database password

### ConfigMap

Non-credential configuration is stored in a ConfigMap named `mini-java-container-config`.

## Security Considerations

1. **Non-root user**: The container runs as a non-root user (UID 1001).
2. **Secrets management**: All credentials are stored in Kubernetes Secrets, never in plain text in the deployment manifests.
3. **Read-only config**: The config volume is mounted read-only.
4. **No package managers**: The runtime image contains no package managers (no apt-get, apk, etc.).
5. **Minimal base image**: The runtime uses `eclipse-temurin:11-jdk` as a minimal JVM base.
6. **Signal handling**: The ENTRYPOINT uses `sh -c "exec java ..."` so the JVM is PID 1 and receives SIGTERM for graceful shutdown.

## Troubleshooting

### Pod Failures
- **CrashLoopBackOff**: Check pod logs with `kubectl logs <pod-name> -n mini-java-container`.
- **Image pull errors**: Verify the ECR image URI and that you're logged in to ECR.
- **Secret not found**: Ensure the Secret was created before deploying.

### Service Issues
- **Service not accessible**: Check that the Service selector matches the pod labels.
- **Port mismatch**: Verify containerPort in deployment matches targetPort in service.

### Ingress Problems
- **Ingress not created**: Ensure the AWS Load Balancer Controller is installed on the EKS cluster.
- **Health check failures**: Verify the health check endpoint is accessible on port 8081.

### Scaling and Management

#### Horizontal Pod Autoscaler (HPA)
To enable auto-scaling based on CPU utilization:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mini-java-container-hpa
  namespace: mini-java-container
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mini-java-container
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
```

#### Rolling Updates
Update the image:
```bash
kubectl set image deployment/mini-java-container mini-java-container=<new-image-uri> -n mini-java-container
```

Monitor the rollout:
```bash
kubectl rollout status deployment/mini-java-container -n mini-java-container
```

#### Rollback
If a deployment fails:
```bash
kubectl rollout undo deployment/mini-java-container -n mini-java-container
```

## Java Notes

### JVM Settings
The container uses the following JVM settings via `JAVA_OPTS`:
- `-Xmx512m`: Maximum heap size of 512 MB
- `-Xms256m`: Initial heap size of 256 MB
- `-XX:+UseContainerSupport`: Enables container-aware JVM flags
- `-XX:MaxRAMPercentage=75.0`: Maximum RAM usage at 75% of the container limit

These settings are tuned for the container's resource limits:
- Memory limit: 1Gi (1024 MB)
- CPU limit: 500m (0.5 cores)
- Memory request: 512Mi
- CPU request: 250m (0.25 cores)

The `-Xmx512m` keeps the heap within the memory limit while leaving room for off-heap memory. The `-XX:MaxRAMPercentage=75.0` provides additional safety by limiting total JVM memory (including off-heap) to 75% of the container's memory limit.

### Application Type
The application is a standalone Java application (not a Spring Boot application) that:
- Uses `com.sun.net.httpserver.HttpServer` for the health check endpoint on port 8081
- Uses `java.net.ServerSocket` for the main application server on port 8080
- Reads configuration from environment variables
- Connects to MySQL, Redis, and external APIs via environment-variable-configured endpoints

### Health Check Endpoint
The application exposes a `/health` endpoint on port 8081 (configurable via `HEALTH_PORT` environment variable). The endpoint returns:
```json
{"status":"UP"}
```

This endpoint is used by Kubernetes liveness and readiness probes.

### Build Process
The Dockerfile uses a multi-stage build:
1. **Build stage**: Maven 3.9.4 with Eclipse Temurin 11 JDK compiles the project and resolves dependencies.
2. **Runtime stage**: Eclipse Temurin 11 JDK runs the application JAR with its dependencies on the classpath.

The JAR is built with `mvn clean package -DskipTests` and runtime dependencies are copied to `target/lib/` for the classpath.
