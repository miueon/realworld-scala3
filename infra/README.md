# Real World Scala3 - Google Cloud Platform Infrastructure

This directory contains the complete infrastructure-as-code setup for deploying the Real World Scala3 application on Google Cloud Platform using Pulumi and Kubernetes.

## 🏗️ Architecture Overview

The infrastructure includes:

- **GKE Cluster**: Production-ready Kubernetes cluster with auto-scaling and security hardening
- **Cloud SQL**: Regional PostgreSQL instance with automated backups and SSL encryption
- **Memorystore Redis**: Managed Redis instance for caching
- **VPC Networking**: Custom VPC with proper subnetting and firewall rules
- **Secret Management**: Google Secret Manager integration
- **Load Balancer**: Global HTTPS load balancer with SSL certificates
- **Monitoring**: Prometheus and Google Cloud Operations integration

## 📋 Prerequisites

1. **Google Cloud Project** with billing enabled
2. **Required CLI tools**:
   ```bash
   # Install gcloud CLI
   curl https://sdk.cloud.google.com | bash
   
   # Install kubectl
   gcloud components install kubectl
   
   # Install Pulumi
   curl -fsSL https://get.pulumi.com | sh
   ```

3. **Required APIs** (automatically enabled by deployment):
   - Container Engine API
   - Compute Engine API
   - Cloud SQL Admin API
   - Memorystore Redis API
   - Secret Manager API
   - Cloud Operations APIs

## 🚀 Quick Deployment

### 1. Set Environment Variables

```bash
export GCP_PROJECT_ID="your-project-id"
export GCP_REGION="us-central1"
export GCP_ZONE="us-central1-a"
```

### 2. Configure Pulumi Stack

```bash
cd infra/
pulumi stack init production
pulumi config set gcp:project $GCP_PROJECT_ID
pulumi config set gcp:region $GCP_REGION

# Set required secrets
pulumi config set --secret realworld-scala3:SC_POSTGRES_PASSWORD "your-secure-password"
pulumi config set --secret realworld-scala3:SC_ACCESS_TOKEN_KEY "your-jwt-secret-key"
pulumi config set --secret realworld-scala3:SC_PASSWORD_SALT "your-password-salt"
```

### 3. Deploy Infrastructure

```bash
./deploy.sh
```

Or manually:

```bash
# Deploy core infrastructure
pulumi up

# Deploy Kubernetes resources
kubectl apply -f ingress.yaml
kubectl apply -f monitoring.yaml
```

## 🔧 Infrastructure Components

### Core Infrastructure (`Main.scala`)

- **VPC Network**: Custom VPC with secondary IP ranges for pods and services
- **GKE Cluster**: Regional cluster with:
  - Private nodes with Workload Identity
  - Network policies enabled
  - Auto-scaling node pools (1-5 nodes)
  - Shielded GKE nodes with secure boot
- **Cloud SQL**: PostgreSQL 16 with:
  - Regional availability (High Availability)
  - Automated backups with point-in-time recovery
  - SSL/TLS encryption enforced
  - Performance insights enabled
- **Memorystore Redis**: Basic tier Redis 7.0 instance
- **Secret Manager**: Secure storage for application secrets

### Networking & Security

- **VPC Subnets**: 
  - Primary: `10.0.0.0/24` (nodes)
  - Secondary: `10.1.0.0/16` (pods)
  - Secondary: `10.2.0.0/16` (services)
- **Firewall Rules**: Allow HTTPS traffic to load balancer
- **Private Cluster**: Nodes without public IP addresses
- **Workload Identity**: Secure pod-to-GCP service authentication

### Load Balancing & SSL (`ingress.yaml`)

- **Global Load Balancer**: Google Cloud Load Balancer
- **SSL Certificates**: Google-managed SSL certificates
- **Static IP**: Reserved external IP address
- **HTTP to HTTPS redirect**: Automatic HTTPS enforcement

### Monitoring (`monitoring.yaml`)

- **Prometheus**: Kubernetes-native monitoring
- **Service Discovery**: Auto-discovery of application metrics
- **RBAC**: Proper service account and permissions
- **Google Cloud Operations**: Integrated logging and monitoring

## 🔐 Security Features

1. **Network Security**:
   - Private GKE nodes
   - VPC-native networking
   - Network policies enabled
   - Firewall rules restricting access

2. **Authentication & Authorization**:
   - Workload Identity for pod-to-GCP authentication
   - RBAC for Kubernetes resources
   - Service account best practices

3. **Data Security**:
   - Cloud SQL with SSL/TLS enforcement
   - Secret Manager for sensitive data
   - Encrypted persistent disks

4. **Container Security**:
   - Shielded GKE nodes
   - Container image vulnerability scanning
   - Security contexts for containers

## 📊 Monitoring & Observability

### Metrics Collection
- Application metrics via `/metrics` endpoint
- Kubernetes cluster metrics
- Node and pod resource metrics
- Custom business metrics

### Logging
- Application logs via Google Cloud Logging
- Kubernetes audit logs
- Container runtime logs

### Alerting
- Google Cloud Monitoring alerts
- Prometheus alerting rules
- SLA/SLI monitoring

## 🔄 Operational Procedures

### Scaling
```bash
# Scale application pods
kubectl scale deployment realworld-app --replicas=3

# Scale GKE nodes (auto-scaling is enabled)
gcloud container clusters resize realworld-cluster --num-nodes=3 --region=us-central1
```

### Updates
```bash
# Update application image
kubectl set image deployment/realworld-app realworld-app=ghcr.io/miueon/realworld-smithy4s:v1.2.0

# Update infrastructure
pulumi up
```

### Backup & Recovery
- **Database**: Automated daily backups with 7-day retention
- **Point-in-time recovery**: Available for the last 7 days
- **Application data**: Stored in Cloud SQL with regional replication

### Troubleshooting
```bash
# Check application logs
kubectl logs -l app=realworld-app -f

# Check infrastructure status
pulumi stack output

# Check cluster health
kubectl get nodes
kubectl get pods
kubectl get services
```

## 💰 Cost Optimization

- **Preemptible nodes**: Enabled for cost savings
- **Auto-scaling**: Scales down during low usage
- **Right-sizing**: Appropriately sized instances
- **Storage optimization**: SSD for performance, minimal sizes

Estimated monthly cost: ~$200-400 USD (varies by usage)

## 🚨 Production Considerations

1. **Domain Configuration**: Update `ingress.yaml` with your actual domain
2. **SSL Certificates**: Configure managed certificates for your domain
3. **Monitoring**: Set up alerting for critical metrics
4. **Backup Strategy**: Verify backup and recovery procedures
5. **Security Scanning**: Enable vulnerability scanning
6. **Access Control**: Implement proper IAM policies

## 📚 Documentation

- [Google Kubernetes Engine](https://cloud.google.com/kubernetes-engine/docs)
- [Cloud SQL for PostgreSQL](https://cloud.google.com/sql/docs/postgres)
- [Memorystore for Redis](https://cloud.google.com/memorystore/docs/redis)
- [Pulumi GCP Provider](https://www.pulumi.com/registry/packages/gcp/)

## 🆘 Support

For issues with:
- **Infrastructure**: Check Pulumi documentation and GCP status
- **Application**: Check application logs and Scala documentation
- **Kubernetes**: Check cluster events and pod status

Remember to always test infrastructure changes in a development environment first!