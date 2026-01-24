#!/bin/bash
#
# Stop Hitorro Docker container
#

CONTAINER_NAME="hitorro-app"

echo "Stopping Hitorro container..."

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    docker stop "${CONTAINER_NAME}"
    echo "✓ Container stopped"
else
    echo "No running container found"
fi
