#!/bin/bash
#
# Run Hitorro with 6000 port range (avoids MinIO conflict)
#

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

IMAGE_NAME="hitorro-app:latest"
CONTAINER_NAME="hitorro-app"
DOCKERFILE="/Users/chris/hitorro/hitorro-example-springboot/Dockerfile-with-ui"
BUILD_CONTEXT="/Users/chris/hitorro"

echo -e "${BLUE}Starting Hitorro Container (6000 Port Range)${NC}"
echo "=========================================="
echo ""

# Check if image exists, if not build it
if ! docker images | grep -q "hitorro-app.*latest"; then
    echo -e "${YELLOW}Image not found, building...${NC}"
    echo "This will take 10-15 minutes on first build..."
    echo ""
    DOCKER_BUILDKIT=0 docker build -f "$DOCKERFILE" -t "$IMAGE_NAME" "$BUILD_CONTEXT"
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Build failed!${NC}"
        exit 1
    fi
    echo ""
    echo -e "${GREEN}✓ Build complete!${NC}"
    echo ""
fi

# Stop old container if running
if docker ps -a | grep -q "$CONTAINER_NAME"; then
    echo -e "${YELLOW}Stopping old container...${NC}"
    docker stop "$CONTAINER_NAME" 2>/dev/null || true
    docker rm "$CONTAINER_NAME" 2>/dev/null || true
fi

echo -e "${GREEN}Starting container with 6000 port range...${NC}"
echo ""
echo "Port mappings:"
echo "  • 8080 → 8080 (HTTP - Web UI & REST API)"
echo "  • 6000 → 9000 (Telnet CLI)"
echo "  • 6022 → 9022 (SSH CLI)"
echo ""

docker run -d \
  --name "$CONTAINER_NAME" \
  -p 8080:8080 \
  -p 6000:9000 \
  -p 6022:9022 \
  -v hitorro-data:/var/lib/hitorro/data \
  -v hitorro-files:/opt/hitorro-app/data/files \
  -v hitorro-logs:/var/lib/hitorro/logs \
  -e JAVA_OPTS="-Xmx2g -XX:+UseG1GC" \
  -e SPRING_PROFILES_ACTIVE=docker \
  "$IMAGE_NAME"

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Failed to start container!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Container started!${NC}"
echo ""
echo "Waiting for application to start (30-60 seconds)..."
sleep 15

# Wait for health check
HEALTHY=false
for i in {1..12}; do
    if curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP"; then
        echo ""
        echo -e "${GREEN}✓ Application is healthy!${NC}"
        HEALTHY=true
        break
    fi
    echo -n "."
    sleep 5
done

echo ""
echo "=========================================="
echo -e "${GREEN}Hitorro is Running!${NC}"
echo "=========================================="
echo ""
echo "Access points:"
echo "  • React UI:        http://localhost:8080"
echo "  • Swagger API:     http://localhost:8080/swagger-ui.html"
echo "  • H2 Console:      http://localhost:8080/h2-console"
echo "  • Actuator:        http://localhost:8080/actuator"
echo "  • REST API:        http://localhost:8080/api/rest/*"
echo "  • Telnet CLI:      telnet localhost 6000"
echo "  • SSH CLI:         ssh -p 6022 localhost"
echo ""
echo "View logs:"
echo "  docker logs -f $CONTAINER_NAME"
echo ""
echo "Stop container:"
echo "  docker stop $CONTAINER_NAME"
echo ""

if [ "$HEALTHY" = false ]; then
    echo -e "${YELLOW}⚠ Application may still be starting...${NC}"
    echo "Check logs with: docker logs -f hitorro-app"
fi
