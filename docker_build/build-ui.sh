#!/bin/bash
#
# Build Hitorro with React UI Docker image
#

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Building Hitorro with React UI..."
echo "This includes:"
echo "  1. React Frontend (Node.js build)"
echo "  2. Spring Boot Backend (Maven build)"
echo "  3. Runtime container"
echo ""
echo "This may take several minutes..."
echo ""

cd "$PROJECT_ROOT"

docker build \
    -f Dockerfile-with-ui \
    -t hitorro-example-springboot:ui-latest \
    ..

echo ""
echo "✓ Build complete: hitorro-example-springboot:ui-latest"
echo ""
echo "To run:"
echo "  docker run -p 8080:8080 hitorro-example-springboot:ui-latest"
echo ""
echo "Or use the master script:"
echo "  ./hitorro.sh start-ui"
