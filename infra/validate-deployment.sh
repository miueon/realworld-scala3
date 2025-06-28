#!/bin/bash

# Enhanced Deployment Validation Script
# Validates the deployment script without actually deploying

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

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

echo "🔍 Enhanced Deployment Script Validation"
echo "========================================"

# Check if deployment script exists and is executable
if [ -f "./deploy.sh" ]; then
    log_success "Deployment script found"
    if [ -x "./deploy.sh" ]; then
        log_success "Deployment script is executable"
    else
        log_warn "Deployment script is not executable - run: chmod +x deploy.sh"
    fi
else
    log_error "Deployment script not found"
    exit 1
fi

# Check for required tools
log_info "Checking prerequisites..."

tools=("gcloud" "kubectl" "pulumi" "helm")
missing_tools=()

for tool in "${tools[@]}"; do
    if command -v "$tool" >/dev/null 2>&1; then
        log_success "$tool is installed"
    else
        log_error "$tool is not installed"
        missing_tools+=("$tool")
    fi
done

# Check script syntax
log_info "Validating script syntax..."
if bash -n "./deploy.sh"; then
    log_success "Script syntax is valid"
else
    log_error "Script has syntax errors"
    exit 1
fi

# Check required files
log_info "Checking required files..."

required_files=(
    "Main.scala"
    "ingress.yaml"
    "yamls/realworld-deployment.yaml"
    "yamls/realworld-service.yaml"
    "yamls/postgres-deployment.yaml"
    "yamls/redis-deployment.yaml"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        log_success "$file exists"
    else
        log_error "$file is missing"
    fi
done

# Check YAML syntax
log_info "Validating YAML files..."
for yaml_file in yamls/*.yaml ingress.yaml; do
    if [ -f "$yaml_file" ]; then
        if command -v python3 >/dev/null 2>&1; then
            # Use safe_load_all for multi-document YAML files
            if python3 -c "import yaml; list(yaml.safe_load_all(open('$yaml_file')))" 2>/dev/null; then
                log_success "$yaml_file syntax is valid"
            else
                log_error "$yaml_file has invalid YAML syntax"
            fi
        else
            log_warn "Python3 not available, skipping YAML validation for $yaml_file"
        fi
    fi
done

# Summary
echo ""
echo "📋 Validation Summary"
echo "===================="

if [ ${#missing_tools[@]} -eq 0 ]; then
    log_success "All required tools are installed"
else
    log_error "Missing tools: ${missing_tools[*]}"
    echo ""
    echo "Installation guides:"
    echo "  gcloud: https://cloud.google.com/sdk/docs/install"
    echo "  kubectl: https://kubernetes.io/docs/tasks/tools/"
    echo "  pulumi: https://www.pulumi.com/docs/get-started/install/"
    echo "  helm: https://helm.sh/docs/intro/install/"
fi

echo ""
echo "🚀 Next Steps:"
echo "1. Ensure all missing tools are installed"
echo "2. Set up GCP authentication: gcloud auth login"
echo "3. Run the deployment: ./deploy.sh"
echo ""

log_info "Validation completed!"
