#!/bin/bash

set -e

# =============================================================================
# Multi-Architecture Docker Build & Push Script
# Supports: linux/amd64, linux/arm64
# =============================================================================

# Configuration
IMAGE_NAME="ddingsh9/lee-yong-gwan-lee-service"
BUILDER_NAME="multiarch-builder"

# Colors for output
RED='\033[0;31m'˚
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Get version from argument or generate timestamp-based version
VERSION=${1:-$(date +%Y%m%d-%H%M%S)}

log_info "=============================================="
log_info "Building Docker Image: ${IMAGE_NAME}"
log_info "Version: ${VERSION}"
log_info "Platforms: linux/amd64, linux/arm64"
log_info "=============================================="

# Step 1: Gradle Build
log_info "Step 1/4: Building application with Gradle..."
./gradlew clean bootJar -x test --no-daemon
log_info "Gradle build completed!"

# Step 2: Setup Docker Buildx
log_info "Step 2/4: Setting up Docker Buildx..."

# Check if builder exists, create if not
if ! docker buildx inspect ${BUILDER_NAME} > /dev/null 2>&1; then
    log_info "Creating new buildx builder: ${BUILDER_NAME}"
    docker buildx create --name ${BUILDER_NAME} --driver docker-container --bootstrap
else
    log_info "Using existing buildx builder: ${BUILDER_NAME}"
fi

# Use the builder
docker buildx use ${BUILDER_NAME}

# Step 3: Build and Push Multi-Arch Image
log_info "Step 3/4: Building and pushing multi-architecture image..."

docker buildx build \
    --platform linux/amd64,linux/arm64 \
    --tag ${IMAGE_NAME}:${VERSION} \
    --tag ${IMAGE_NAME}:latest \
    --push \
    .

log_info "Multi-arch image pushed successfully!"

# Step 4: Verify
log_info "Step 4/4: Verifying pushed image..."
docker buildx imagetools inspect ${IMAGE_NAME}:${VERSION}

log_info "=============================================="
log_info "Build & Push Complete!"
log_info "=============================================="
log_info "Images pushed:"
log_info "  - ${IMAGE_NAME}:${VERSION}"
log_info "  - ${IMAGE_NAME}:latest"
log_info ""
log_info "To deploy on server, run:"
log_info "  docker pull ${IMAGE_NAME}:latest"
log_info "  docker-compose up -d"
log_info "=============================================="
