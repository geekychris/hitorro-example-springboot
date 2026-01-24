#!/bin/bash
#
# Build Hitorro backend-only Docker image
#

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Building Hitorro backend Docker image..."
cd "$PROJECT_ROOT"

docker build \
    -f Dockerfile \
    -t hitorro-example-springboot:latest \
    ..

echo "✓ Build complete: hitorro-example-springboot:latest"
