#!/bin/bash
#
# Diagnostic script to troubleshoot build issues
#

echo "🔍 Hitorro Build Diagnostics"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check Docker
echo "1. Docker Status:"
if docker info > /dev/null 2>&1; then
    echo "   ✅ Docker is running"
    docker version --format '   Version: {{.Server.Version}}'
else
    echo "   ❌ Docker is NOT running"
    echo "   → Please start Docker Desktop"
    exit 1
fi
echo ""

# Check script location
echo "2. Current Directory:"
echo "   $(pwd)"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "   Script Dir: $SCRIPT_DIR"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
echo "   Project Root: $PROJECT_ROOT"
BUILD_CONTEXT="$(dirname "$PROJECT_ROOT")"
echo "   Build Context: $BUILD_CONTEXT"
echo ""

# Check required files
echo "3. Required Files:"
FILES=(
    "$PROJECT_ROOT/Dockerfile-with-ui"
    "$PROJECT_ROOT/frontend/package.json"
    "$PROJECT_ROOT/pom.xml"
    "$BUILD_CONTEXT/hitorro-util/pom.xml"
    "$BUILD_CONTEXT/hitorro-base/pom.xml"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $(basename "$file") exists"
    else
        echo "   ❌ Missing: $file"
    fi
done
echo ""

# Check Docker resources
echo "4. Docker Resources:"
AVAILABLE_DISK=$(df -h / | awk 'NR==2 {print $4}')
echo "   Available Disk: $AVAILABLE_DISK"

if command -v docker-compose &> /dev/null; then
    echo "   ✅ docker-compose installed"
else
    echo "   ⚠️  docker-compose not found (optional)"
fi
echo ""

# Check Node.js (optional, Docker will use its own)
echo "5. Build Tools (optional, Docker has its own):"
if command -v node &> /dev/null; then
    echo "   ✅ Node.js: $(node --version)"
else
    echo "   ⚠️  Node.js not installed locally (OK, Docker will use its own)"
fi

if command -v mvn &> /dev/null; then
    echo "   ✅ Maven: $(mvn --version | head -1)"
else
    echo "   ⚠️  Maven not installed locally (OK, Docker will use its own)"
fi
echo ""

# Check previous builds
echo "6. Existing Docker Images:"
if docker images hitorro-example-springboot --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep -v REPOSITORY; then
    echo "   (Found existing images)"
else
    echo "   No existing images"
fi
echo ""

# Check running containers
echo "7. Running Containers:"
if docker ps --filter "name=hitorro" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -v NAMES; then
    echo "   (Containers are running)"
else
    echo "   No running containers"
fi
echo ""

# Test build context
echo "8. Testing Build Context:"
cd "$BUILD_CONTEXT"
if [ -d "hitorro-example-springboot" ]; then
    echo "   ✅ Build context is correct"
    echo "   Directory structure:"
    ls -d hitorro-* 2>/dev/null | head -5 | sed 's/^/      /'
else
    echo "   ❌ Build context issue"
    echo "   Expected hitorro-example-springboot directory"
fi
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "💡 Next Steps:"
echo ""
echo "If all checks passed, try:"
echo "  ./hitorro.sh build-ui"
echo ""
echo "Or for more detailed error output:"
echo "  cd $PROJECT_ROOT"
echo "  docker build -f Dockerfile-with-ui -t hitorro:test .."
echo ""
echo "To see the last build error (if any):"
echo "  docker logs \$(docker ps -lq) 2>&1 | tail -50"
echo ""
