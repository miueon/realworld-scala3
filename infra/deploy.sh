#!/bin/bash

# Real World Scala3 GCP Enhanced Deployment Script
# Enhanced with interactive configuration, HTTPS automation, and DNS management

set -e

# Colors for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Interactive configuration functions
prompt_user() {
    local prompt="$1"
    local default="$2"
    local var_name="$3"
    
    if [ -n "$default" ]; then
        read -p "$prompt [$default]: " input
        eval "$var_name=\${input:-$default}"
    else
        read -p "$prompt: " input
        eval "$var_name=\"$input\""
    fi
}

confirm_action() {
    local prompt="$1"
    local response
    read -p "$prompt (y/N): " response
    case "$response" in
        [yY][eE][sS]|[yY]) return 0 ;;
        *) return 1 ;;
    esac
}

echo "🚀 Enhanced Real World Scala3 GCP Deployment Script"
echo "================================================================"

# Prerequisite checks
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    local missing_tools=()
    
    if ! command -v gcloud >/dev/null 2>&1; then
        missing_tools+=("gcloud CLI")
    fi
    
    if ! command -v kubectl >/dev/null 2>&1; then
        missing_tools+=("kubectl")
    fi
    
    if ! command -v pulumi >/dev/null 2>&1; then
        missing_tools+=("pulumi CLI")
    fi
    
    if ! command -v helm >/dev/null 2>&1; then
        missing_tools+=("helm")
    fi
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_info "Please install the missing tools and try again."
        log_info "Installation guides:"
        log_info "  gcloud: https://cloud.google.com/sdk/docs/install"
        log_info "  kubectl: https://kubernetes.io/docs/tasks/tools/"
        log_info "  pulumi: https://www.pulumi.com/docs/get-started/install/"
        log_info "  helm: https://helm.sh/docs/intro/install/"
        exit 1
    fi
    
    log_success "All prerequisites are installed"
}

# Interactive configuration
configure_deployment() {
    log_info "Starting interactive configuration..."
    echo ""
    
    # Project configuration
    echo "📋 GCP Project Configuration"
    echo "============================="
    
    # Get current gcloud project if available
    CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null || echo "")
    
    prompt_user "Enter GCP Project ID" "$CURRENT_PROJECT" PROJECT_ID
    
    if [ "$PROJECT_ID" == "your-project-id" ] || [ -z "$PROJECT_ID" ]; then
        log_error "Please provide a valid GCP Project ID"
        exit 1
    fi
    
    # Region configuration
    echo ""
    echo "🌍 Region Configuration"
    echo "======================"
    log_info "Popular regions:"
    log_info "  us-central1 (Iowa, USA)"
    log_info "  us-east1 (South Carolina, USA)"
    log_info "  europe-west1 (Belgium)"
    log_info "  asia-southeast1 (Singapore)"
    
    prompt_user "Enter GCP Region" "us-central1" REGION
    prompt_user "Enter GCP Zone" "${REGION}-a" ZONE
    
    # Domain configuration
    echo ""
    echo "🌐 Domain & SSL Configuration"
    echo "============================="
    prompt_user "Enter your domain name" "www.miueon-v.top" DOMAIN_NAME
    
    # Email for Let's Encrypt
    prompt_user "Enter email for Let's Encrypt certificates" "" LETSENCRYPT_EMAIL
    
    if [ -z "$LETSENCRYPT_EMAIL" ]; then
        log_error "Email is required for Let's Encrypt certificate generation"
        exit 1
    fi
    
    # Environment configuration
    echo ""
    echo "⚙️  Environment Configuration"
    echo "============================"
    ENVIRONMENT="prod"
    log_info "Environment: $ENVIRONMENT (production)"
    
    # Summary
    echo ""
    echo "📋 Deployment Summary"
    echo "===================="
    echo "   Project ID: $PROJECT_ID"
    echo "   Region: $REGION"
    echo "   Zone: $ZONE"
    echo "   Domain: $DOMAIN_NAME"
    echo "   Email: $LETSENCRYPT_EMAIL"
    echo "   Environment: $ENVIRONMENT"
    echo ""
    
    if ! confirm_action "Proceed with deployment?"; then
        log_warn "Deployment cancelled by user"
        exit 0
    fi
}

# GCP Authentication and setup
setup_gcp() {
    log_info "Setting up Google Cloud Platform..."
    
    # Check if already authenticated
    if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
        log_info "Authenticating with Google Cloud..."
        gcloud auth login --quiet
    else
        log_success "Already authenticated with Google Cloud"
    fi
    
    # Set project configuration
    log_info "Configuring GCP project settings..."
    gcloud config set project "$PROJECT_ID" --quiet
    gcloud config set compute/region "$REGION" --quiet
    gcloud config set compute/zone "$ZONE" --quiet
    
    # Verify project access
    if ! gcloud projects describe "$PROJECT_ID" >/dev/null 2>&1; then
        log_error "Cannot access project '$PROJECT_ID'. Please check project ID and permissions."
        exit 1
    fi
    
    log_success "GCP configuration completed"
}

# Enable required GCP APIs
enable_gcp_apis() {
    log_info "Enabling required Google Cloud APIs..."
    
    local required_apis=(
        "container.googleapis.com"
        "compute.googleapis.com"
        "sqladmin.googleapis.com"
        "redis.googleapis.com"
        "secretmanager.googleapis.com"
        "monitoring.googleapis.com"
        "logging.googleapis.com"
        "dns.googleapis.com"
        "certificatemanager.googleapis.com"
    )
    
    for api in "${required_apis[@]}"; do
        log_info "Enabling $api..."
        gcloud services enable "$api" --quiet
    done
    
    log_success "All required APIs enabled"
}

# Deploy infrastructure with Pulumi
deploy_infrastructure() {
    log_info "Deploying infrastructure with Pulumi..."
    
    local original_dir=$(pwd)
    cd /home/yuion/0.project/+Lab/scala/real-world-scala3/infra
    
    # Initialize or select stack
    if ! pulumi stack select "$ENVIRONMENT" 2>/dev/null; then
        log_info "Creating new Pulumi stack: $ENVIRONMENT"
        pulumi stack init "$ENVIRONMENT"
    fi
    
    # Set Pulumi configuration
    log_info "Configuring Pulumi stack..."
    pulumi config set gcp:project "$PROJECT_ID"
    pulumi config set gcp:region "$REGION"
    pulumi config set domain "$DOMAIN_NAME"
    
    # Deploy infrastructure
    log_info "Deploying infrastructure (this may take several minutes)..."
    if ! pulumi up --yes; then
        log_error "Infrastructure deployment failed"
        cd "$original_dir"
        exit 1
    fi
    
    cd "$original_dir"
    log_success "Infrastructure deployment completed"
}

# Get GKE cluster credentials
setup_kubectl() {
    log_info "Setting up kubectl access to GKE cluster..."
    
    local cluster_name
    cluster_name=$(pulumi stack output cluster-name -C /home/yuion/0.project/+Lab/scala/real-world-scala3/infra)
    
    if [ -z "$cluster_name" ]; then
        log_error "Could not retrieve cluster name from Pulumi"
        exit 1
    fi
    
    log_info "Getting credentials for cluster: $cluster_name"
    gcloud container clusters get-credentials "$cluster_name" --region "$REGION" --quiet
    
    # Verify cluster access
    if ! kubectl cluster-info >/dev/null 2>&1; then
        log_error "Cannot access Kubernetes cluster"
        exit 1
    fi
    
    log_success "Kubectl configured successfully"
}

# Install cert-manager for Let's Encrypt
install_cert_manager() {
    log_info "Installing cert-manager for HTTPS certificates..."
    
    # Add cert-manager Helm repository
    log_info "Adding cert-manager Helm repository..."
    helm repo add jetstack https://charts.jetstack.io --force-update
    helm repo update
    
    # Install cert-manager
    log_info "Installing cert-manager..."
    kubectl create namespace cert-manager --dry-run=client -o yaml | kubectl apply -f -
    
    helm upgrade --install cert-manager jetstack/cert-manager \
        --namespace cert-manager \
        --version v1.13.0 \
        --set installCRDs=true \
        --wait \
        --timeout=300s
    
    # Wait for cert-manager to be ready
    log_info "Waiting for cert-manager to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/cert-manager -n cert-manager
    kubectl wait --for=condition=available --timeout=300s deployment/cert-manager-webhook -n cert-manager
    kubectl wait --for=condition=available --timeout=300s deployment/cert-manager-cainjector -n cert-manager
    
    log_success "cert-manager installed successfully"
}

# Create Let's Encrypt ClusterIssuer
create_letsencrypt_issuer() {
    log_info "Creating Let's Encrypt ClusterIssuer..."
    
    cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: ${LETSENCRYPT_EMAIL}
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: gce
EOF

    log_success "Let's Encrypt ClusterIssuer created"
}

# Create or update ingress configuration
create_ingress_config() {
    log_info "Creating HTTPS ingress configuration..."
    
    # Create temporary ingress file with actual domain
    cat > /tmp/realworld-ingress.yaml <<EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: realworld-ingress
  annotations:
    kubernetes.io/ingress.class: "gce"
    kubernetes.io/ingress.global-static-ip-name: "realworld-lb-ip"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    kubernetes.io/ingress.allow-http: "false"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - ${DOMAIN_NAME}
    secretName: realworld-tls
  rules:
  - host: "${DOMAIN_NAME}"
    http:
      paths:
      - path: "/*"
        pathType: ImplementationSpecific
        backend:
          service:
            name: realworld-app
            port:
              number: 80
---
apiVersion: v1
kind: Service
metadata:
  name: realworld-app-nodeport
  labels:
    app: realworld-app
spec:
  type: NodePort
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: realworld
EOF

    log_success "Ingress configuration created"
}

# Setup DNS zone (commented for manual activation)
setup_dns_zone() {
    log_info "DNS Zone Setup (COMMENTED - Manual Activation Required)"
    
    cat > /tmp/dns-setup-commands.sh <<'EOF'
#!/bin/bash
# DNS Setup Commands - Uncomment and run when ready

# Create DNS zone
# gcloud dns managed-zones create realworld-zone \
#     --description="Real World App DNS Zone" \
#     --dns-name="${DOMAIN_NAME%.*}." \
#     --visibility=public

# Get name servers
# echo "Configure your domain registrar with these name servers:"
# gcloud dns managed-zones describe realworld-zone \
#     --format="value(nameServers[])"

# Create A record pointing to static IP
# STATIC_IP=$(pulumi stack output static-ip -C /home/yuion/0.project/+Lab/scala/real-world-scala3/infra)
# gcloud dns record-sets transaction start --zone=realworld-zone
# gcloud dns record-sets transaction add "${STATIC_IP}" \
#     --name="${DOMAIN_NAME}." \
#     --ttl=300 \
#     --type=A \
#     --zone=realworld-zone
# gcloud dns record-sets transaction execute --zone=realworld-zone

EOF

    chmod +x /tmp/dns-setup-commands.sh
    
    log_warn "DNS setup commands created at /tmp/dns-setup-commands.sh"
    log_warn "Uncomment and run these commands when ready to activate DNS"
    log_warn "Make sure to configure your domain registrar with the provided name servers"
}

# Deploy application
deploy_application() {
    log_info "Deploying Real World Scala3 application..."
    
    # Apply all YAML configurations
    log_info "Applying application configurations..."
    kubectl apply -f yamls/
    
    # Apply the dynamic ingress configuration
    log_info "Applying HTTPS ingress configuration..."
    kubectl apply -f /tmp/realworld-ingress.yaml
    
    log_success "Application deployment initiated"
}

# Wait for deployment and verify
verify_deployment() {
    log_info "Verifying deployment status..."
    
    # Wait for deployment to be ready
    log_info "Waiting for application deployment to be ready..."
    if ! kubectl wait --for=condition=available --timeout=600s deployment/realworld; then
        log_error "Application deployment failed to become ready"
        kubectl describe deployment/realworld
        kubectl logs -l app=realworld --tail=50
        exit 1
    fi
    
    # Wait for ingress to get IP
    log_info "Waiting for ingress to receive external IP..."
    local timeout=300
    local counter=0
    
    while [ $counter -lt $timeout ]; do
        local ingress_ip=$(kubectl get ingress realworld-ingress -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
        if [ -n "$ingress_ip" ] && [ "$ingress_ip" != "null" ]; then
            log_success "Ingress IP assigned: $ingress_ip"
            break
        fi
        
        echo -n "."
        sleep 5
        counter=$((counter + 5))
    done
    
    if [ $counter -ge $timeout ]; then
        log_warn "Ingress IP not assigned within timeout, but deployment may still be successful"
    fi
    
    # Check certificate status
    log_info "Checking SSL certificate status..."
    if kubectl get certificate realworld-tls >/dev/null 2>&1; then
        local cert_status=$(kubectl get certificate realworld-tls -o jsonpath='{.status.conditions[0].status}' 2>/dev/null)
        if [ "$cert_status" = "True" ]; then
            log_success "SSL certificate is ready"
        else
            log_warn "SSL certificate is still being provisioned"
            log_info "Certificate status: $(kubectl get certificate realworld-tls -o jsonpath='{.status.conditions[0].message}' 2>/dev/null)"
        fi
    fi
    
    log_success "Deployment verification completed"
}

# Display deployment results
show_deployment_results() {
    echo ""
    echo "🎉 Deployment Completed Successfully!"
    echo "================================================================"
    echo ""
    
    # Get external IP
    local external_ip
    external_ip=$(pulumi stack output static-ip -C /home/yuion/0.project/+Lab/scala/real-world-scala3/infra 2>/dev/null || echo "Not available")
    
    echo "📊 Deployment Summary:"
    echo "   Domain: https://$DOMAIN_NAME"
    echo "   External IP: $external_ip"
    echo "   Environment: $ENVIRONMENT"
    echo "   Project: $PROJECT_ID"
    echo "   Region: $REGION"
    echo ""
    
    echo "📋 Service Status:"
    kubectl get services --no-headers | while read line; do
        echo "   $line"
    done
    echo ""
    
    echo "🌐 Ingress Status:"
    kubectl get ingress --no-headers | while read line; do
        echo "   $line"
    done
    echo ""
    
    echo "🔒 Certificate Status:"
    if kubectl get certificate realworld-tls >/dev/null 2>&1; then
        kubectl get certificate realworld-tls --no-headers | while read line; do
            echo "   $line"
        done
    else
        echo "   Certificate not found (may still be provisioning)"
    fi
    echo ""
    
    echo "� Useful Commands:"
    echo "   View application logs:"
    echo "     kubectl logs -l app=realworld -f"
    echo ""
    echo "   Check certificate details:"
    echo "     kubectl describe certificate realworld-tls"
    echo ""
    echo "   Monitor ingress status:"
    echo "     kubectl describe ingress realworld-ingress"
    echo ""
    echo "   Scale application:"
    echo "     kubectl scale deployment realworld --replicas=3"
    echo ""
    
    echo "🚀 Next Steps:"
    echo "   1. DNS Setup: Run /tmp/dns-setup-commands.sh when ready"
    echo "   2. Configure domain registrar with Google Cloud DNS name servers"
    echo "   3. Wait for DNS propagation (usually 24-48 hours)"
    echo "   4. SSL certificate will be automatically provisioned after DNS setup"
    echo ""
    
    if [ "$external_ip" != "Not available" ]; then
        echo "   5. Test your application at: https://$DOMAIN_NAME"
        echo "      (after DNS configuration)"
    fi
    echo ""
    
    log_success "Enhanced deployment completed! 🎊"
}

# Error handling and cleanup
cleanup_on_error() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        log_error "Deployment failed with exit code $exit_code"
        log_info "Cleaning up temporary files..."
        rm -f /tmp/realworld-ingress.yaml
        rm -f /tmp/dns-setup-commands.sh
        
        log_info "For troubleshooting, check:"
        log_info "  - Pulumi logs: pulumi logs"
        log_info "  - Kubernetes events: kubectl get events --sort-by=.metadata.creationTimestamp"
        log_info "  - Application logs: kubectl logs -l app=realworld"
    fi
}

# Set up error handling
trap cleanup_on_error EXIT

# Main deployment flow
main() {
    log_info "Starting enhanced deployment process..."
    
    check_prerequisites
    configure_deployment
    
    echo ""
    log_info "🚀 Beginning deployment to Google Cloud Platform..."
    echo ""
    
    setup_gcp
    enable_gcp_apis
    deploy_infrastructure
    setup_kubectl
    install_cert_manager
    create_letsencrypt_issuer
    create_ingress_config
    setup_dns_zone
    deploy_application
    verify_deployment
    show_deployment_results
    
    # Clean up temporary files on success
    rm -f /tmp/realworld-ingress.yaml
    
    log_success "All operations completed successfully!"
}

# Run main function
main "$@"