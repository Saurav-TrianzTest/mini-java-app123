# McontainerMonoTest – Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Local Development with Docker Compose](#local-development-with-docker-compose)
5. [Building and Pushing the Docker Image](#building-and-pushing-the-docker-image)
6. [GCP GKE Deployment](#gcp-gke-deployment)
7. [Kubernetes Manifest Reference](#kubernetes-manifest-reference)
8. [Configuration & Environment Variables](#configuration--environment-variables)
9. [Health Checks & Monitoring](#health-checks--monitoring)
10. [Scaling & Management](#scaling--management)
11. [Troubleshooting](#troubleshooting)
12. [Security Considerations](#security-considerations)

---

## Overview

**Application**: McontainerMonoTest  
**Technology**: Java 11 / Spring Boot 2.7 / Maven  
**Package**: JAR (executable)  
**Application Port**: 8080  
**Health Endpoint**: `/actuator/health`  
**Target Platform**: GCP GKE (Google Kubernetes Engine)

This guide covers everything needed to build, containerize, and deploy the application to GCP GKE.

---

## Prerequisites

### Local Development
| Tool | Version | Purpose |
|------|---------|---------|
| Docker | 20.10+ | Build and run containers |
| Docker Compose | 2.x | Local multi-container orchestration |
| Java JDK | 11+ | Local development |
| Maven | 3.9+ | Build tool |

### GCP GKE Deployment
| Tool | Version | Purpose |
|------|---------|---------|
| gcloud CLI | Latest | GCP authentication and cluster management |
| kubectl | 1.25+ | Kubernetes cluster management |
| Docker | 20.10+ | Build and push images |

### GCP Setup
1. A GCP project with billing enabled
2. GKE API enabled: `gcloud services enable container.googleapis.com`
3. Artifact Registry API enabled: `gcloud services enable artifactregistry.googleapis.com`
4. Authenticated gcloud CLI: `gcloud auth login`

---

## Project Structure

```
McontainerMonoTest/
├── Dockerfile                    # Multi-stage Docker build
├── docker-compose.yml            # Local development compose file
├── .dockerignore                 # Docker build exclusions
├── pom.xml                       # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/test/
│       │   ├── MiniApp.java      # Main application class
│       │   └── DatabaseService.java
│       └── resources/
│           └── application.properties
├── kubernetes/
│   ├── namespace.yaml            # Kubernetes namespace
│   ├── deployment.yaml           # Application deployment
│   ├── service.yaml              # ClusterIP service
│   └── ingress.yaml              # GKE Ingress (HTTP/HTTPS)
├── scripts/
│   ├── build-push.sh             # Linux/macOS build & push
│   ├── build-push.bat            # Windows build & push
│   ├── deploy-image.sh           # Linux/macOS GKE deploy
│   └── deploy-image.bat          # Windows GKE deploy
└── docs/
    └── DEPLOYMENT.md             # This file
```

---

## Local Development with Docker Compose

### 1. Configure Environment Variables

Create a `.env` file in the project root (never commit this file):

```env
DB_HOST=your-database-host
DB_PORT=3306
DB_NAME=mini_app_db
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password
REDIS_HOST=your-redis-host
REDIS_PORT=6379
EXTERNAL_API_URL=http://api.example.com:8080/v1
PAYMENT_SERVICE_URL=https://payment.internal.company.com/process
```

### 2. Build and Start

```bash
# Build and start the application container
docker-compose up --build

# Run in detached mode
docker-compose up --build -d

# View logs
docker-compose logs -f mcontainermonotest

# Stop the application
docker-compose down
```

### 3. Verify the Application

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Check application context path
curl http://localhost:8080/mini-app/
```

---

## Building and Pushing the Docker Image

### Linux / macOS

```bash
# Make the script executable
chmod +x scripts/build-push.sh

# Run from project root
./scripts/build-push.sh
```

The script will prompt you to:
1. Select registry type (Google Artifact Registry or Docker Hub)
2. Enter registry credentials and details
3. Provide an image tag (defaults to `latest`)

### Windows

```cmd
scripts\build-push.bat
```

### Manual Build (without script)

```bash
# Build the image
docker build -t mcontainermonotest:latest .

# Tag for Google Artifact Registry
docker tag mcontainermonotest:latest \
  us-central1-docker.pkg.dev/MY_PROJECT/MY_REPO/mcontainermonotest:latest

# Push
docker push us-central1-docker.pkg.dev/MY_PROJECT/MY_REPO/mcontainermonotest:latest
```

---

## GCP GKE Deployment

### Step 1: Create a GKE Cluster (if not existing)

```bash
gcloud container clusters create mcontainermonotest-cluster \
  --zone us-central1-a \
  --num-nodes 2 \
  --machine-type e2-standard-2 \
  --project MY_GCP_PROJECT
```

### Step 2: Create Artifact Registry Repository

```bash
gcloud artifacts repositories create my-repo \
  --repository-format=docker \
  --location=us-central1 \
  --project MY_GCP_PROJECT
```

### Step 3: Build and Push the Image

```bash
./scripts/build-push.sh
# Select option 1 (Google Artifact Registry)
# Enter your GCP project, region, and repository name
```

### Step 4: Deploy to GKE

#### Linux / macOS
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows
```cmd
scripts\deploy-image.bat
```

The deploy script will:
1. Prompt for GCP project, zone, cluster name, and image URI
2. Prompt for application environment variables (database, Redis, external APIs)
3. Configure `kubectl` to connect to your GKE cluster
4. Apply Kubernetes manifests in order (namespace → deployment → service → ingress)
5. Wait for the deployment rollout to complete
6. Display the application access URL

### Step 5: Verify Deployment

```bash
# Check all resources in the namespace
kubectl get all -n mcontainermonotest

# Check pod logs
kubectl logs -l app=mcontainermonotest -n mcontainermonotest --tail=100

# Check health endpoint via port-forward
kubectl port-forward svc/mcontainermonotest-service 8080:80 -n mcontainermonotest
curl http://localhost:8080/actuator/health
```

---

## Kubernetes Manifest Reference

### namespace.yaml
Creates the `mcontainermonotest` namespace to isolate all application resources.

### deployment.yaml
- **Replicas**: 2 (for high availability)
- **Image**: Placeholder `{{IMAGE_URI}}` replaced by deploy script
- **Resources**: 250m CPU / 512Mi memory (requests); 500m CPU / 1Gi memory (limits)
- **Liveness Probe**: `GET /actuator/health` – starts after 60s, checks every 30s
- **Readiness Probe**: `GET /actuator/health` – starts after 30s, checks every 15s
- **Security**: Runs as non-root user (UID 1000)

### service.yaml
- **Type**: ClusterIP (internal cluster access)
- **Port**: 80 → 8080 (container port)

### ingress.yaml
- **Class**: `gce` (GKE native HTTP(S) Load Balancer)
- **Host**: `mcontainermonotest.example.com` (update to your actual domain)
- **Path**: `/` (all traffic routed to the application)

> **Note**: Update the host in `kubernetes/ingress.yaml` to your actual domain before deploying.

---

## Configuration & Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Application HTTP port |
| `SPRING_PROFILES_ACTIVE` | `docker` | Active Spring profile |
| `DB_HOST` | `localhost` | Database hostname |
| `DB_PORT` | `3306` | Database port |
| `DB_NAME` | `mini_app_db` | Database schema name |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | _(empty)_ | Database password |
| `REDIS_HOST` | `redis-service` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `EXTERNAL_API_URL` | `http://api.example.com:8080/v1` | External API base URL |
| `PAYMENT_SERVICE_URL` | `https://payment.internal.company.com/process` | Payment service URL |
| `LOG_DIR_PATH` | `/mnt/filestore/logs` | Log directory path |
| `CONFIG_FILE_PATH` | `/app/config/app.properties` | Config file path |
| `TZ` | `UTC` | Container timezone |
| `JAVA_OPTS` | _(JVM flags)_ | JVM tuning options |

### Using Kubernetes Secrets for Sensitive Values

```bash
# Create a secret for database credentials
kubectl create secret generic db-credentials \
  --from-literal=DB_USERNAME=myuser \
  --from-literal=DB_PASSWORD=mypassword \
  -n mcontainermonotest
```

Then reference in `deployment.yaml`:
```yaml
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: db-credentials
      key: DB_PASSWORD
```

---

## Health Checks & Monitoring

### Spring Boot Actuator Endpoints

| Endpoint | URL | Purpose |
|----------|-----|---------|
| Health | `GET /actuator/health` | Overall application health |

### JVM Monitoring

The application is configured with the following JVM flags for container awareness:
```
-Xms256m -Xmx512m
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:+UnlockExperimentalVMOptions
```

### Viewing Logs

```bash
# Stream logs from all pods
kubectl logs -l app=mcontainermonotest -n mcontainermonotest -f

# Logs from a specific pod
kubectl logs <pod-name> -n mcontainermonotest

# Previous container logs (after crash)
kubectl logs <pod-name> -n mcontainermonotest --previous
```

---

## Scaling & Management

### Manual Scaling

```bash
# Scale to 3 replicas
kubectl scale deployment mcontainermonotest \
  --replicas=3 -n mcontainermonotest
```

### Horizontal Pod Autoscaler (HPA)

```bash
kubectl autoscale deployment mcontainermonotest \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n mcontainermonotest
```

### Rolling Update

```bash
# Update the image
kubectl set image deployment/mcontainermonotest \
  mcontainermonotest=NEW_IMAGE_URI \
  -n mcontainermonotest

# Monitor rollout
kubectl rollout status deployment/mcontainermonotest -n mcontainermonotest
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/mcontainermonotest -n mcontainermonotest

# Rollback to a specific revision
kubectl rollout undo deployment/mcontainermonotest \
  --to-revision=2 -n mcontainermonotest

# View rollout history
kubectl rollout history deployment/mcontainermonotest -n mcontainermonotest
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Describe the pod for events
kubectl describe pod <pod-name> -n mcontainermonotest

# Check pod logs
kubectl logs <pod-name> -n mcontainermonotest

# Check resource availability
kubectl describe nodes
```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `ImagePullBackOff` | Wrong image URI or missing credentials | Verify image URI; create imagePullSecret |
| `CrashLoopBackOff` | Application startup failure | Check logs; verify env vars (DB_HOST, etc.) |
| `Pending` pods | Insufficient cluster resources | Scale up node pool or reduce resource requests |
| Health probe failing | App not ready / wrong endpoint | Increase `initialDelaySeconds`; verify `/actuator/health` |
| Database connection refused | Wrong DB_HOST or network policy | Verify DB_HOST env var; check VPC firewall rules |

### Ingress Not Getting IP

```bash
# Check ingress status
kubectl describe ingress mcontainermonotest-ingress -n mcontainermonotest

# GKE ingress provisioning can take 5-10 minutes
kubectl get ingress -n mcontainermonotest -w
```

### Port-Forward for Local Debugging

```bash
# Forward application port
kubectl port-forward svc/mcontainermonotest-service 8080:80 -n mcontainermonotest

# Test health endpoint
curl http://localhost:8080/actuator/health
```

---

## Security Considerations

1. **Non-root container**: The application runs as UID 1000 (non-root) for security.
2. **Secrets management**: Use Kubernetes Secrets or GCP Secret Manager for sensitive values (DB_PASSWORD, API keys).
3. **Network policies**: Consider adding Kubernetes NetworkPolicy to restrict pod-to-pod communication.
4. **Image scanning**: Scan Docker images with `gcloud artifacts docker images scan` before deployment.
5. **Workload Identity**: Use GKE Workload Identity to bind Kubernetes service accounts to GCP service accounts for secure access to GCP services.
6. **RBAC**: Apply least-privilege RBAC policies for the application's Kubernetes service account.
7. **TLS**: Configure GKE Managed Certificates for HTTPS on the Ingress.

### Enable GKE Managed Certificate (HTTPS)

```bash
# Create a ManagedCertificate resource
cat <<EOF | kubectl apply -f -
apiVersion: networking.gke.io/v1
kind: ManagedCertificate
metadata:
  name: mcontainermonotest-cert
  namespace: mcontainermonotest
spec:
  domains:
    - mcontainermonotest.example.com
EOF
```

---

## Java-Specific Notes

- **JVM Startup**: Spring Boot applications may take 30-60 seconds to start. The liveness probe has a 60-second initial delay to accommodate this.
- **Memory**: The JVM is configured with `-XX:+UseContainerSupport` and `-XX:MaxRAMPercentage=75.0` to respect container memory limits.
- **Graceful Shutdown**: The deployment has `terminationGracePeriodSeconds: 30` to allow in-flight requests to complete.
- **Spring Profile**: The `docker` profile is active by default in the container. Add `application-docker.properties` for Docker-specific overrides.
- **Actuator**: Spring Boot Actuator is included and exposes the `/actuator/health` endpoint used by Kubernetes probes.
