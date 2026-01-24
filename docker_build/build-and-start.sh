#!/bin/bash
#
# Build and Start Hitorro - One Command Solution
# 
# This script builds the Docker image and starts the container
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
WITH_UI="${1:-ui}"
IMAGE_NAME="hitorro-example-springboot"
CONTAINER_NAME="hitorro-app"

echo -e "${CYAN}"
cat << "EOF"
╔════════════════════════════════════════════════════════════╗
║                                                            ║
║         🔥 HITORRO BUILD & START (One Command) 🔥         ║
║                                                            ║
╚════════════════════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Check Docker
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Determine what to build
if [ "$WITH_UI" == "backend" ] || [ "$WITH_UI" == "backend-only" ]; then
    echo -e "${YELLOW}Building backend only...${NC}"
    DOCKERFILE="Dockerfile"
    IMAGE_TAG="latest"
    BUILD_MODE="backend"
else
    echo -e "${YELLOW}Building with React UI...${NC}"
    DOCKERFILE="Dockerfile-with-ui"
    IMAGE_TAG="ui-latest"
    BUILD_MODE="ui"
fi

echo ""
echo -e "${BLUE}Step 1/3: Building Docker Image${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ "$BUILD_MODE" == "ui" ]; then
    echo "This will build:"
    echo "  • React Frontend (Node.js)"
    echo "  • Spring Boot Backend (Maven)"
    echo "  • Runtime Container"
    echo ""
    echo "⏱️  This may take 5-10 minutes on first build..."
else
    echo "This will build:"
    echo "  • Spring Boot Backend (Maven)"
    echo "  • Runtime Container"
    echo ""
    echo "⏱️  This may take 3-5 minutes on first build..."
fi
echo ""

cd "$PROJECT_ROOT"

# Build the image
docker build \
    -f "$DOCKERFILE" \
    -t "${IMAGE_NAME}:${IMAGE_TAG}" \
    .. || {
        echo -e "${RED}✗ Build failed!${NC}"
        exit 1
    }

echo ""
echo -e "${GREEN}✓ Build complete!${NC}"
echo ""

echo -e "${BLUE}Step 2/3: Stopping Old Container${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Stop and remove old container if exists
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Stopping existing container..."
    docker stop "${CONTAINER_NAME}" > /dev/null 2>&1 || true
    docker rm "${CONTAINER_NAME}" > /dev/null 2>&1 || true
    echo -e "${GREEN}✓ Old container removed${NC}"
else
    echo "No existing container found"
fi

echo ""
echo -e "${BLUE}Step 3/3: Starting New Container${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Start new container
docker run -d \
    --name "${CONTAINER_NAME}" \
    -p 8080:8080 \
    -p 9000:9000 \
    -p 9022:9022 \
    -e SPRING_PROFILES_ACTIVE=docker \
    -v hitorro-data:/var/lib/hitorro/data \
    -v hitorro-files:/opt/hitorro-app/data/files \
    -v hitorro-logs:/var/lib/hitorro/logs \
    "${IMAGE_NAME}:${IMAGE_TAG}" || {
        echo -e "${RED}✗ Failed to start container!${NC}"
        exit 1
    }

echo -e "${GREEN}✓ Container started!${NC}"
echo ""

# Wait for application to start
echo "Waiting for application to start..."
sleep 10

# Check if container is still running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo -e "${RED}✗ Container stopped unexpectedly!${NC}"
    echo ""
    echo "View logs with: docker logs ${CONTAINER_NAME}"
    exit 1
fi

# Check health endpoint
echo "Checking health..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Application is healthy!${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${YELLOW}⚠ Health check timeout, but container is running${NC}"
    fi
    sleep 2
done

echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    SUCCESS! 🎉                             ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

if [ "$BUILD_MODE" == "ui" ]; then
    echo -e "${GREEN}📱 Access Points:${NC}"
    echo -e "  ${YELLOW}★${NC} React UI:      ${CYAN}http://localhost:8080${NC}"
    echo -e "  ${YELLOW}•${NC} Swagger API:   http://localhost:8080/swagger-ui.html"
    echo -e "  ${YELLOW}•${NC} H2 Console:    http://localhost:8080/h2-console"
    echo -e "  ${YELLOW}•${NC} Actuator:      http://localhost:8080/actuator"
    echo -e "  ${YELLOW}•${NC} REST API:      http://localhost:8080/api/rest"
else
    echo -e "${GREEN}📱 Access Points:${NC}"
    echo -e "  ${YELLOW}•${NC} Swagger API:   ${CYAN}http://localhost:8080/swagger-ui.html${NC}"
    echo -e "  ${YELLOW}•${NC} H2 Console:    http://localhost:8080/h2-console"
    echo -e "  ${YELLOW}•${NC} Actuator:      http://localhost:8080/actuator"
    echo -e "  ${YELLOW}•${NC} REST API:      http://localhost:8080/api/rest"
fi

echo ""
echo -e "${GREEN}🔧 Management:${NC}"
echo -e "  ${YELLOW}•${NC} View logs:     docker logs -f ${CONTAINER_NAME}"
echo -e "  ${YELLOW}•${NC} Stop:          ./hitorro.sh stop"
echo -e "  ${YELLOW}•${NC} Restart:       ./hitorro.sh restart"
echo -e "  ${YELLOW}•${NC} Status:        ./hitorro.sh status"
echo ""

# Auto-open browser (optional)
if command -v open &> /dev/null && [ "$BUILD_MODE" == "ui" ]; then
    read -t 5 -p "Open browser now? (y/N): " -n 1 -r || true
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Opening browser..."
        open http://localhost:8080
    fi
fi

echo ""
echo -e "${CYAN}Happy Document Managing! 🚀${NC}"
echo ""
