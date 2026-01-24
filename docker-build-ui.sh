#!/bin/bash
#
# Docker build script for Hitorro Example Spring Boot Application WITH React UI
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="hitorro-example-springboot"
IMAGE_TAG="${1:-ui-latest}"
DOCKERFILE="Dockerfile-with-ui"
BUILD_CONTEXT=".."

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}Hitorro Docker Build Script (with React UI)${NC}"
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

# Check if Node.js is available (optional, Docker will handle it)
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${BLUE}Node.js version: ${NODE_VERSION}${NC}"
fi

echo ""
echo -e "${YELLOW}Building Docker image with React UI...${NC}"
echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}"
echo "Context: ${BUILD_CONTEXT}"
echo "Dockerfile: ${DOCKERFILE}"
echo ""
echo "This build includes:"
echo "  1. React Frontend (Node.js build)"
echo "  2. Spring Boot Backend (Maven build)"
echo "  3. Runtime container with both"
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
    echo -e "${BLUE}Access points:${NC}"
    echo "  - React UI:     http://localhost:8080"
    echo "  - API Docs:     http://localhost:8080/swagger-ui.html"
    echo "  - H2 Console:   http://localhost:8080/h2-console"
    echo "  - REST API:     http://localhost:8080/api/rest"
    echo "  - Actuator:     http://localhost:8080/actuator"
    echo "  - Telnet CLI:   telnet localhost 9000"
    echo "  - SSH CLI:      ssh -p 9022 localhost"
    echo ""
    echo -e "${YELLOW}To run the container:${NC}"
    echo "  docker run -p 8080:8080 -p 9000:9000 -p 9022:9022 ${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
    echo -e "${YELLOW}Or use docker-compose:${NC}"
    echo "  docker-compose -f docker-compose-with-ui.yml up"
    echo ""
    echo -e "${YELLOW}To tag for Docker Hub:${NC}"
    echo "  docker tag ${IMAGE_NAME}:${IMAGE_TAG} yourusername/${IMAGE_NAME}:${IMAGE_TAG}"
    echo "  docker push yourusername/${IMAGE_NAME}:${IMAGE_TAG}"
    echo ""
else
    echo ""
    echo -e "${RED}=====================================${NC}"
    echo -e "${RED}Build failed!${NC}"
    echo -e "${RED}=====================================${NC}"
    echo ""
    echo "Check the error messages above for details."
    echo ""
    exit 1
fi
