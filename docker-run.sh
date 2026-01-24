#!/bin/bash
#
# Docker run script for Hitorro Example Spring Boot Application
#

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="hitorro-example-springboot"
IMAGE_TAG="${1:-latest}"
CONTAINER_NAME="hitorro-app"

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}Starting Hitorro Application${NC}"
echo -e "${GREEN}=====================================${NC}"
echo ""

# Check if container is already running
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo -e "${YELLOW}Stopping and removing existing container...${NC}"
    docker stop "${CONTAINER_NAME}" > /dev/null 2>&1 || true
    docker rm "${CONTAINER_NAME}" > /dev/null 2>&1 || true
fi

echo "Starting container: ${CONTAINER_NAME}"
echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""

# Run the container
docker run -d \
    --name "${CONTAINER_NAME}" \
    -p 8080:8080 \
    -p 9000:9000 \
    -p 9022:9022 \
    -e SPRING_PROFILES_ACTIVE=docker \
    -v hitorro-data:/var/lib/hitorro/data \
    -v hitorro-files:/opt/hitorro-app/data/files \
    -v hitorro-logs:/var/lib/hitorro/logs \
    "${IMAGE_NAME}:${IMAGE_TAG}"

echo ""
echo -e "${GREEN}Container started successfully!${NC}"
echo ""
echo "Application endpoints:"
echo "  - Web UI: http://localhost:8080"
echo "  - Swagger UI: http://localhost:8080/swagger-ui.html"
echo "  - H2 Console: http://localhost:8080/h2-console"
echo "  - Actuator: http://localhost:8080/actuator"
echo "  - Telnet CLI: telnet localhost 9000"
echo "  - SSH CLI: ssh -p 9022 localhost"
echo ""
echo "View logs:"
echo "  docker logs -f ${CONTAINER_NAME}"
echo ""
echo "Stop container:"
echo "  docker stop ${CONTAINER_NAME}"
echo ""
