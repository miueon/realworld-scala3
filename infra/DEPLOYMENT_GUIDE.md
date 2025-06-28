# Enhanced Real World Scala3 Deployment Guide

## Overview

This enhanced deployment script provides:
- **Interactive Configuration**: Prompts for project settings, domain, and email
- **Automated HTTPS**: Let's Encrypt SSL certificates via cert-manager
- **DNS Management**: Google Cloud DNS setup (with commented activation)
- **Health Monitoring**: Deployment verification and status checking
- **Error Handling**: Comprehensive error recovery and rollback

## Prerequisites

### Required Tools
- **gcloud CLI**: [Installation Guide](https://cloud.google.com/sdk/docs/install)
- **kubectl**: [Installation Guide](https://kubernetes.io/docs/tasks/tools/)
- **pulumi CLI**: [Installation Guide](https://www.pulumi.com/docs/get-started/install/)
- **helm**: [Installation Guide](https://helm.sh/docs/intro/install/)

### GCP Setup
1. Create a GCP project or use existing one
2. Enable billing for the project
3. Ensure you have necessary IAM permissions:
   - Kubernetes Engine Admin
   - Compute Admin
   - DNS Administrator
   - Security Admin
   - Service Account Admin

## Quick Start

### 1. Run Enhanced Deployment
```bash
./deploy.sh
```

The script will interactively prompt for:
- **GCP Project ID**: Your Google Cloud project
- **Region/Zone**: Where to deploy (default: us-central1)
- **Domain Name**: Your domain (default: www.miueon-v.top)
- **Email**: For Let's Encrypt certificates

### 2. DNS Configuration (Manual Step)

After deployment, activate DNS:

```bash
# Run the generated DNS setup commands
/tmp/dns-setup-commands.sh
```

Uncomment and execute the commands in this file to:
- Create Google Cloud DNS zone
- Get name servers for your domain registrar
- Create DNS A record pointing to your static IP

### 3. Domain Registrar Configuration

Configure your domain registrar (where you bought www.miueon-v.top) to use Google Cloud DNS name servers provided by the setup script.

## Features

### 🔒 Automatic HTTPS
- **cert-manager** automatically installed
- **Let's Encrypt** ClusterIssuer configured
- **SSL certificates** automatically provisioned after DNS setup
- **HTTPS redirect** enforced

### 🌐 DNS Management
- **Google Cloud DNS** zone creation (commented)
- **Automatic A record** creation pointing to static IP
- **Name server** information for domain registrar setup

### 📊 Health Monitoring
- **Deployment status** verification
- **SSL certificate** status checking
- **Ingress IP** assignment monitoring
- **Comprehensive logging** with colored output

### 🛠️ Error Handling
- **Prerequisite checking** before deployment
- **Rollback capabilities** on failure
- **Detailed error messages** and troubleshooting hints
- **Cleanup** of temporary files

## Configuration Details

### Environment Variables (Optional)
Set these to skip interactive prompts:
```bash
export GCP_PROJECT_ID="your-project-id"
export GCP_REGION="us-central1"
export GCP_ZONE="us-central1-a"
export DOMAIN_NAME="www.miueon-v.top"
export LETSENCRYPT_EMAIL="your-email@example.com"
```

### Domain Configuration
The deployment is configured for **www.miueon-v.top** but can be changed interactively or by updating the default in the script.

## Post-Deployment

### Verify Deployment
```bash
# Check application status
kubectl get pods -l app=realworld

# Check services
kubectl get services

# Check ingress
kubectl get ingress

# Check SSL certificate
kubectl get certificate realworld-tls
```

### View Logs
```bash
# Application logs
kubectl logs -l app=realworld -f

# cert-manager logs
kubectl logs -n cert-manager -l app=cert-manager -f

# Ingress controller logs
kubectl logs -n kube-system -l app=gce-ingress-controller
```

### Scale Application
```bash
# Scale to 3 replicas
kubectl scale deployment realworld --replicas=3
```

## Troubleshooting

### Common Issues

1. **SSL Certificate Not Ready**
   ```bash
   kubectl describe certificate realworld-tls
   ```
   - Ensure DNS is properly configured
   - Check domain is pointing to the correct IP
   - Verify Let's Encrypt challenge completion

2. **Ingress Not Getting IP**
   ```bash
   kubectl describe ingress realworld-ingress
   ```
   - Check if static IP is reserved in GCP
   - Verify ingress controller is running

3. **Application Not Starting**
   ```bash
   kubectl describe pod -l app=realworld
   kubectl logs -l app=realworld
   ```
   - Check environment variables and secrets
   - Verify image availability

### DNS Propagation Check
```bash
# Check if domain resolves to your IP
nslookup www.miueon-v.top

# Check DNS propagation globally
# Use online tools like https://www.whatsmydns.net/
```

## Security Considerations

- **HTTPS Only**: HTTP traffic is redirected to HTTPS
- **Let's Encrypt**: Free, automated SSL certificates
- **Secret Management**: Uses Kubernetes secrets for sensitive data
- **Network Policies**: Consider implementing for additional security

## Cost Optimization

- **GKE Autopilot**: Consider using for automatic resource optimization
- **Preemptible Instances**: For development environments
- **Resource Limits**: Set appropriate CPU/memory limits

## Maintenance

### Update SSL Certificates
Certificates are automatically renewed by cert-manager, but you can force renewal:
```bash
kubectl delete certificate realworld-tls
# Certificate will be automatically recreated
```

### Update Application
```bash
# Update deployment image
kubectl set image deployment/realworld realworld=miueon/realworld-smithy4s:new-version

# Check rollout status
kubectl rollout status deployment/realworld
```

### Backup
Regular backups of:
- Kubernetes configurations
- Database (PostgreSQL)
- Application secrets

## Support

For issues:
1. Check deployment logs and Kubernetes events
2. Verify DNS configuration
3. Check GCP quotas and billing
4. Review cert-manager and ingress controller logs

## Architecture

```
Internet → Domain (www.miueon-v.top) → Google Cloud DNS → 
Load Balancer (Static IP) → GKE Ingress → Service → Pod(s)
                                      ↑
                              SSL Certificate (Let's Encrypt)
```

This enhanced deployment provides a production-ready setup with automated HTTPS and comprehensive monitoring.
