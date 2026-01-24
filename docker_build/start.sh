#!/bin/bash
#
# Start Hitorro Docker container
#

set -e

CONTAINER_NAME="hitorro-app"
IMAGE_NAME="hitorro-example-springboot"
IMAGE_TAG="${1:-latest}"

echo "Starting Hitorro container..."

# Stop existing container if running
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Stopping existing container..."
    docker stop "${CONTAINER_NAME}" > /dev/null 2>&1 || true
    docker rm "${CONTAINER_NAME}" > /dev/null 2>&1 || true
fi

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
    "${IMAGE_NAME}:${IMAGE_TAG}"

echo "✓ Container started: ${CONTAINER_NAME}"
echo ""
echo "Access at: http://localhost:8080"
echo "View logs: docker logs -f ${CONTAINER_NAME}"
