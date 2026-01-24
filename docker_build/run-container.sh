#!/bin/bash
#
# Run the Hitorro Docker container
#

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

IMAGE_NAME="hitorro-complete:latest"
CONTAINER_NAME="hitorro-app"

echo -e "${BLUE}Starting Hitorro Container${NC}"
echo "=========================================="
echo ""

# Check if image exists
if ! docker images | grep -q "hitorro-complete"; then
    echo -e "${YELLOW}⚠ Image 'hitorro-complete:latest' not found!${NC}"
    echo "Please build the image first:"
    echo "  ./build-and-start.sh"
    exit 1
fi

# Stop old container if running
if docker ps -a | grep -q "$CONTAINER_NAME"; then
    echo -e "${YELLOW}Stopping old container...${NC}"
    docker stop "$CONTAINER_NAME" 2>/dev/null || true
    docker rm "$CONTAINER_NAME" 2>/dev/null || true
fi

echo -e "${GREEN}Starting new container...${NC}"
docker run -d \
  --name "$CONTAINER_NAME" \
  -p 8080:8080 \
  -p 9000:9000 \
  -p 9022:9022 \
  -v hitorro-data:/var/lib/hitorro/data \
  -v hitorro-files:/opt/hitorro-app/data/files \
  -v hitorro-logs:/var/lib/hitorro/logs \
  -e JAVA_OPTS="-Xmx2g -XX:+UseG1GC" \
  "$IMAGE_NAME"

echo ""
echo -e "${GREEN}✓ Container started!${NC}"
echo ""
echo "Waiting for application to start (30-60 seconds)..."
sleep 10

# Wait for health check
for i in {1..12}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Application is healthy!${NC}"
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
echo "  • Telnet CLI:      telnet localhost 9000"
echo "  • SSH CLI:         ssh -p 9022 localhost"
echo ""
echo "View logs:"
echo "  docker logs -f $CONTAINER_NAME"
echo ""
echo "Stop container:"
echo "  docker stop $CONTAINER_NAME"
echo ""
