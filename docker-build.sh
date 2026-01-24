#!/bin/bash
#
# Docker build script for Hitorro Example Spring Boot Application
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="hitorro-example-springboot"
IMAGE_TAG="${1:-latest}"
DOCKERFILE="Dockerfile"
BUILD_CONTEXT=".."

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}Hitorro Docker Build Script${NC}"
echo -e "${GREEN}=====================================${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Get the directory of this script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo -e "${YELLOW}Building Docker image...${NC}"
echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}"
echo "Context: ${BUILD_CONTEXT}"
echo "Dockerfile: ${DOCKERFILE}"
echo ""

# Build the Docker image
docker build \
    -f "${DOCKERFILE}" \
    -t "${IMAGE_NAME}:${IMAGE_TAG}" \
    "${BUILD_CONTEXT}"

BUILD_STATUS=$?

if [ $BUILD_STATUS -eq 0 ]; then
    echo ""
    echo -e "${GREEN}=====================================${NC}"
    echo -e "${GREEN}Build successful!${NC}"
    echo -e "${GREEN}=====================================${NC}"
    echo ""
    echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
    echo "To run the container:"
    echo "  docker run -p 8080:8080 ${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
    echo "Or use docker-compose:"
    echo "  docker-compose up"
    echo ""
    echo "To tag for Docker Hub:"
    echo "  docker tag ${IMAGE_NAME}:${IMAGE_TAG} yourusername/${IMAGE_NAME}:${IMAGE_TAG}"
    echo "  docker push yourusername/${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
else
    echo ""
    echo -e "${RED}=====================================${NC}"
    echo -e "${RED}Build failed!${NC}"
    echo -e "${RED}=====================================${NC}"
    exit 1
fi
