# Azure Deployment Guide - Mini Java Application

## Prerequisites

### Required Azure Resources
1. Azure App Service or Azure Kubernetes Service (AKS)
2. Azure Database for MySQL
3. Azure Key Vault
4. Azure App Configuration (optional)
5. Azure Blob Storage
6. Azure Cache for Redis (optional)
7. Azure Application Insights (optional)

### Required Tools
- Azure CLI (`az`)
- Maven 3.6+
- Java 11+
- Docker (for containerization)

## Quick Start Deployment

### Option 1: Azure App Service (Recommended for Simple Deployments)

#### Step 1: Create Azure Resources

```bash
# Set variables
RESOURCE_GROUP="mini-app-rg"
LOCATION="eastus"
APP_NAME="mini-java-app-${RANDOM}"
MYSQL_SERVER="mini-app-mysql-${RANDOM}"
KEYVAULT_NAME="mini-app-kv-${RANDOM}"
STORAGE_ACCOUNT="miniappstorage${RANDOM}"

# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create MySQL database
az mysql flexible-server create \
  --resource-group $RESOURCE_GROUP \
  --name $MYSQL_SERVER \
  --location $LOCATION \
  --admin-user myadmin \
  --admin-password 'YourSecurePassword123!' \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --version 8.0.21 \
  --storage-size 32

# Create database
az mysql flexible-server db create \
  --resource-group $RESOURCE_GROUP \
  --server-name $MYSQL_SERVER \
  --database-name mini_app_db

# Create Key Vault
az keyvault create \
  --name $KEYVAULT_NAME \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION

# Create Storage Account
az storage account create \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku Standard_LRS

# Create Blob Container
az storage container create \
  --name app-data \
  --account-name $STORAGE_ACCOUNT
```

#### Step 2: Store Secrets in Key Vault

```bash
# Get storage connection string
STORAGE_CONNECTION=$(az storage account show-connection-string \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query connectionString -o tsv)

# Store secrets
az keyvault secret set --vault-name $KEYVAULT_NAME \
  --name DB-PASSWORD --value 'YourSecurePassword123!'

az keyvault secret set --vault-name $KEYVAULT_NAME \
  --name AZURE-STORAGE-CONNECTION-STRING --value "$STORAGE_CONNECTION"
```

#### Step 3: Build Application

```bash
# Build JAR
mvn clean package -DskipTests

# Verify JAR created
ls -lh target/mini-java-app-1.0.0.jar
```

#### Step 4: Create App Service

```bash
# Create App Service Plan
az appservice plan create \
  --name mini-app-plan \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku B1 \
  --is-linux

# Create Web App
az webapp create \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --plan mini-app-plan \
  --runtime "JAVA:11-java11"
```

#### Step 5: Enable Managed Identity

```bash
# Enable system-assigned managed identity
az webapp identity assign \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP

# Get the identity principal ID
PRINCIPAL_ID=$(az webapp identity show \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --query principalId -o tsv)

# Grant Key Vault access
az keyvault set-policy \
  --name $KEYVAULT_NAME \
  --object-id $PRINCIPAL_ID \
  --secret-permissions get list
```

#### Step 6: Configure Application Settings

```bash
# Configure database connection
az webapp config appsettings set \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings \
    SPRING_PROFILES_ACTIVE=azure \
    DB_HOST="${MYSQL_SERVER}.mysql.database.azure.com" \
    DB_PORT=3306 \
    DB_NAME=mini_app_db \
    DB_USERNAME=myadmin \
    AZURE_KEYVAULT_URI="https://${KEYVAULT_NAME}.vault.azure.net/" \
    AZURE_STORAGE_CONTAINER=app-data \
    USE_BLOB_STORAGE=true \
    LOG_LEVEL=INFO

# Configure firewall to allow Azure services
az mysql flexible-server firewall-rule create \
  --resource-group $RESOURCE_GROUP \
  --name $MYSQL_SERVER \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

#### Step 7: Deploy Application

```bash
# Deploy JAR to App Service
az webapp deploy \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --src-path target/mini-java-app-1.0.0.jar \
  --type jar

# Wait for deployment to complete
echo "Waiting for application to start..."
sleep 60

# Check application health
curl https://${APP_NAME}.azurewebsites.net/actuator/health
```

#### Step 8: View Logs

```bash
# Stream logs
az webapp log tail \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP

# Or download logs
az webapp log download \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --log-file app-logs.zip
```

### Option 2: Azure Kubernetes Service (AKS)

#### Step 1: Create AKS Cluster

```bash
# Create AKS cluster
az aks create \
  --resource-group $RESOURCE_GROUP \
  --name mini-app-aks \
  --node-count 2 \
  --node-vm-size Standard_B2s \
  --enable-managed-identity \
  --generate-ssh-keys

# Get credentials
az aks get-credentials \
  --resource-group $RESOURCE_GROUP \
  --name mini-app-aks
```

#### Step 2: Create Container Registry

```bash
# Create ACR
ACR_NAME="miniappacr${RANDOM}"
az acr create \
  --resource-group $RESOURCE_GROUP \
  --name $ACR_NAME \
  --sku Basic

# Attach ACR to AKS
az aks update \
  --resource-group $RESOURCE_GROUP \
  --name mini-app-aks \
  --attach-acr $ACR_NAME
```

#### Step 3: Build and Push Docker Image

```bash
# Build Docker image
docker build -t mini-java-app:1.0.0 .

# Tag for ACR
docker tag mini-java-app:1.0.0 ${ACR_NAME}.azurecr.io/mini-java-app:1.0.0

# Login to ACR
az acr login --name $ACR_NAME

# Push image
docker push ${ACR_NAME}.azurecr.io/mini-java-app:1.0.0
```

#### Step 4: Create Kubernetes Secrets

```bash
# Create secret for database credentials
kubectl create secret generic app-secrets \
  --from-literal=db-host="${MYSQL_SERVER}.mysql.database.azure.com" \
  --from-literal=db-username=myadmin \
  --from-literal=db-password='YourSecurePassword123!' \
  --from-literal=storage-connection-string="$STORAGE_CONNECTION"
```

#### Step 5: Deploy to AKS

```bash
# Update azure-deploy.yml with your ACR name
sed -i "s/\${CONTAINER_REGISTRY}/${ACR_NAME}.azurecr.io/g" azure-deploy.yml
sed -i "s/\${KEYVAULT_NAME}/${KEYVAULT_NAME}/g" azure-deploy.yml

# Apply deployment
kubectl apply -f azure-deploy.yml

# Check deployment status
kubectl get deployments
kubectl get pods
kubectl get services

# Get external IP
kubectl get service mini-java-app-service
```

#### Step 6: Verify Deployment

```bash
# Get service external IP
EXTERNAL_IP=$(kubectl get service mini-java-app-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Test health endpoint
curl http://${EXTERNAL_IP}/actuator/health

# Test application
curl http://${EXTERNAL_IP}/mini-app/
```

## Environment Variables Reference

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `azure` |
| `DB_HOST` | Database host | `myserver.mysql.database.azure.com` |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `mini_app_db` |
| `DB_USERNAME` | Database username | `myadmin` |
| `DB_PASSWORD` | Database password | Stored in Key Vault |
| `AZURE_KEYVAULT_URI` | Key Vault endpoint | `https://mykv.vault.azure.net/` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `8080` |
| `LOG_LEVEL` | Logging level | `INFO` |
| `USE_BLOB_STORAGE` | Enable blob storage | `false` |
| `AZURE_STORAGE_CONTAINER` | Blob container name | `app-data` |
| `REDIS_HOST` | Redis cache host | `localhost` |
| `REDIS_PORT` | Redis cache port | `6379` |

## Monitoring and Troubleshooting

### Health Checks

```bash
# Check application health
curl https://${APP_NAME}.azurewebsites.net/actuator/health

# Check detailed health
curl https://${APP_NAME}.azurewebsites.net/actuator/health/liveness
curl https://${APP_NAME}.azurewebsites.net/actuator/health/readiness
```

### View Metrics

```bash
# View metrics
curl https://${APP_NAME}.azurewebsites.net/actuator/metrics

# View specific metric
curl https://${APP_NAME}.azurewebsites.net/actuator/metrics/hikaricp.connections.active
```

### Common Issues

#### Issue 1: Database Connection Failed

**Symptoms**: Application fails to start, logs show connection errors

**Solution**:
```bash
# Check firewall rules
az mysql flexible-server firewall-rule list \
  --resource-group $RESOURCE_GROUP \
  --name $MYSQL_SERVER

# Add your IP if needed
az mysql flexible-server firewall-rule create \
  --resource-group $RESOURCE_GROUP \
  --name $MYSQL_SERVER \
  --rule-name AllowMyIP \
  --start-ip-address YOUR_IP \
  --end-ip-address YOUR_IP
```

#### Issue 2: Key Vault Access Denied

**Symptoms**: Application cannot read secrets from Key Vault

**Solution**:
```bash
# Verify managed identity has access
az keyvault show --name $KEYVAULT_NAME --query properties.accessPolicies

# Grant access if missing
az keyvault set-policy \
  --name $KEYVAULT_NAME \
  --object-id $PRINCIPAL_ID \
  --secret-permissions get list
```

#### Issue 3: Blob Storage Connection Failed

**Symptoms**: File operations fail

**Solution**:
```bash
# Verify connection string
az storage account show-connection-string \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP

# Update app settings
az webapp config appsettings set \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings AZURE_STORAGE_CONNECTION_STRING="$STORAGE_CONNECTION"
```

## Scaling

### Vertical Scaling (App Service)

```bash
# Scale up to higher tier
az appservice plan update \
  --name mini-app-plan \
  --resource-group $RESOURCE_GROUP \
  --sku P1V2
```

### Horizontal Scaling (App Service)

```bash
# Scale out to multiple instances
az appservice plan update \
  --name mini-app-plan \
  --resource-group $RESOURCE_GROUP \
  --number-of-workers 3
```

### Auto-scaling (AKS)

Auto-scaling is configured in `azure-deploy.yml`:
- Min replicas: 2
- Max replicas: 10
- CPU threshold: 70%
- Memory threshold: 80%

## Backup and Disaster Recovery

### Database Backup

```bash
# Enable automated backups (enabled by default)
az mysql flexible-server update \
  --resource-group $RESOURCE_GROUP \
  --name $MYSQL_SERVER \
  --backup-retention 7

# Manual backup
az mysql flexible-server backup create \
  --resource-group $RESOURCE_GROUP \
  --name $MYSQL_SERVER \
  --backup-name manual-backup-$(date +%Y%m%d)
```

### Blob Storage Backup

```bash
# Enable soft delete
az storage blob service-properties delete-policy update \
  --account-name $STORAGE_ACCOUNT \
  --enable true \
  --days-retained 7
```

## Cost Optimization

### Recommendations

1. **Use Burstable SKUs** for development/testing
2. **Enable auto-shutdown** for non-production environments
3. **Use Azure Reserved Instances** for production
4. **Monitor and optimize** connection pool sizes
5. **Use Azure Cost Management** for tracking

### Estimated Monthly Costs (East US)

| Resource | SKU | Estimated Cost |
|----------|-----|----------------|
| App Service | B1 | $13/month |
| MySQL | Standard_B1ms | $15/month |
| Key Vault | Standard | $0.03/10k ops |
| Blob Storage | Standard LRS | $0.02/GB |
| **Total** | | **~$30/month** |

## Security Best Practices

1. ✅ Use Managed Identity for Azure service authentication
2. ✅ Store all secrets in Azure Key Vault
3. ✅ Enable SSL/TLS for all connections
4. ✅ Use network security groups to restrict access
5. ✅ Enable Azure DDoS Protection
6. ✅ Implement API authentication and authorization
7. ✅ Regular security updates and patches
8. ✅ Enable Azure Security Center recommendations

## Next Steps

1. **Configure CI/CD Pipeline** using Azure DevOps or GitHub Actions
2. **Set up Application Insights** for advanced monitoring
3. **Implement API Gateway** using Azure API Management
4. **Configure CDN** for static content delivery
5. **Set up Azure Front Door** for global load balancing
6. **Implement disaster recovery** with geo-replication

## Support

For issues or questions:
- Review logs: `az webapp log tail`
- Check health: `/actuator/health`
- View metrics: `/actuator/metrics`
- Azure Support: https://azure.microsoft.com/support/

## Additional Resources

- [Azure App Service Documentation](https://docs.microsoft.com/azure/app-service/)
- [Azure Kubernetes Service Documentation](https://docs.microsoft.com/azure/aks/)
- [Spring Cloud Azure Documentation](https://spring.io/projects/spring-cloud-azure)
- [Azure Key Vault Documentation](https://docs.microsoft.com/azure/key-vault/)
