#!/bin/bash
#
# Clean up Hitorro Docker resources
#

CONTAINER_NAME="hitorro-app"
IMAGE_NAME="hitorro-example-springboot"

echo "Cleaning up Hitorro Docker resources..."

# Stop and remove container
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    docker stop "${CONTAINER_NAME}" > /dev/null 2>&1 || true
    docker rm "${CONTAINER_NAME}" > /dev/null 2>&1 || true
    echo "✓ Container removed"
fi

# Remove images
docker rmi "${IMAGE_NAME}:latest" > /dev/null 2>&1 || true
docker rmi "${IMAGE_NAME}:ui-latest" > /dev/null 2>&1 || true
echo "✓ Images removed"

# Optionally remove volumes
if [ "$1" == "--all" ] || [ "$1" == "-a" ]; then
    echo ""
    echo "WARNING: This will remove all data volumes!"
    read -p "Continue? (yes/no): " -r
    if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        docker volume rm hitorro-data > /dev/null 2>&1 || true
        docker volume rm hitorro-files > /dev/null 2>&1 || true
        docker volume rm hitorro-logs > /dev/null 2>&1 || true
        echo "✓ Volumes removed"
    fi
fi

echo "✓ Cleanup complete"
