#!/bin/bash
#
# Hitorro Docker Master Control Script
# 
# This script provides a unified interface for building and running
# the Hitorro Example Spring Boot application with or without React UI.
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Configuration
IMAGE_NAME="hitorro-example-springboot"
CONTAINER_NAME="hitorro-app"

# Print banner
print_banner() {
    echo -e "${CYAN}"
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║                                                            ║"
    echo "║              🔥 HITORRO DOCKER CONTROL 🔥                 ║"
    echo "║                                                            ║"
    echo "║           Document Management System v1.0                  ║"
    echo "║                                                            ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Print usage
print_usage() {
    echo -e "${YELLOW}Usage:${NC}"
    echo "  $0 [command] [options]"
    echo ""
    echo -e "${YELLOW}Commands:${NC}"
    echo -e "  ${GREEN}build${NC}              Build Docker image (backend only)"
    echo -e "  ${GREEN}build-ui${NC}           Build Docker image with React UI"
    echo -e "  ${GREEN}start${NC}              Start the application"
    echo -e "  ${GREEN}start-ui${NC}           Start the application with React UI"
    echo -e "  ${GREEN}stop${NC}               Stop the application"
    echo -e "  ${GREEN}restart${NC}            Restart the application"
    echo -e "  ${GREEN}logs${NC}               View application logs"
    echo -e "  ${GREEN}status${NC}             Show container status"
    echo -e "  ${GREEN}clean${NC}              Remove containers and images"
    echo -e "  ${GREEN}clean-all${NC}          Remove containers, images, and volumes"
    echo -e "  ${GREEN}dev${NC}                Start in development mode (separate frontend/backend)"
    echo -e "  ${GREEN}compose-up${NC}         Start with docker-compose"
    echo -e "  ${GREEN}compose-up-ui${NC}      Start with docker-compose (with UI)"
    echo -e "  ${GREEN}compose-down${NC}       Stop docker-compose"
    echo -e "  ${GREEN}help${NC}               Show this help message"
    echo ""
    echo -e "${YELLOW}Examples:${NC}"
    echo "  $0 build-ui              # Build image with React UI"
    echo "  $0 start-ui              # Start with UI"
    echo "  $0 logs                  # View logs"
    echo "  $0 clean-all             # Complete cleanup"
    echo ""
}

# Check Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}Error: Docker is not running${NC}"
        echo "Please start Docker Desktop and try again."
        exit 1
    fi
}

# Build backend only
build_backend() {
    echo -e "${BLUE}Building Hitorro (backend only)...${NC}"
    cd "$PROJECT_ROOT"
    docker build -f Dockerfile -t "${IMAGE_NAME}:latest" ..
    echo -e "${GREEN}✓ Build complete: ${IMAGE_NAME}:latest${NC}"
}

# Build with UI
build_ui() {
    echo -e "${BLUE}Building Hitorro with React UI...${NC}"
    echo "This will take a few minutes..."
    cd "$PROJECT_ROOT"
    docker build -f Dockerfile-with-ui -t "${IMAGE_NAME}:ui-latest" ..
    echo -e "${GREEN}✓ Build complete: ${IMAGE_NAME}:ui-latest${NC}"
}

# Start backend only
start_backend() {
    echo -e "${BLUE}Starting Hitorro (backend only)...${NC}"
    
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
        "${IMAGE_NAME}:latest"
    
    echo -e "${GREEN}✓ Container started: ${CONTAINER_NAME}${NC}"
    print_access_info
}

# Start with UI
start_ui() {
    echo -e "${BLUE}Starting Hitorro with React UI...${NC}"
    
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
        "${IMAGE_NAME}:ui-latest"
    
    echo -e "${GREEN}✓ Container started: ${CONTAINER_NAME}${NC}"
    print_access_info_ui
}

# Stop containers
stop_containers() {
    echo -e "${BLUE}Stopping Hitorro...${NC}"
    
    if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        docker stop "${CONTAINER_NAME}"
        echo -e "${GREEN}✓ Container stopped${NC}"
    else
        echo -e "${YELLOW}No running container found${NC}"
    fi
}

# View logs
view_logs() {
    echo -e "${BLUE}Viewing logs (Ctrl+C to exit)...${NC}"
    docker logs -f "${CONTAINER_NAME}" 2>/dev/null || \
        echo -e "${RED}Container not running${NC}"
}

# Show status
show_status() {
    echo -e "${BLUE}Container Status:${NC}"
    echo ""
    docker ps -a --filter "name=${CONTAINER_NAME}" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || \
        echo -e "${YELLOW}No containers found${NC}"
    echo ""
    echo -e "${BLUE}Docker Images:${NC}"
    docker images "${IMAGE_NAME}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    echo ""
    echo -e "${BLUE}Docker Volumes:${NC}"
    docker volume ls | grep hitorro || echo -e "${YELLOW}No volumes found${NC}"
}

# Clean containers and images
clean() {
    echo -e "${YELLOW}Cleaning up containers and images...${NC}"
    
    # Stop and remove container
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        docker stop "${CONTAINER_NAME}" > /dev/null 2>&1 || true
        docker rm "${CONTAINER_NAME}" > /dev/null 2>&1 || true
        echo -e "${GREEN}✓ Container removed${NC}"
    fi
    
    # Remove images
    docker rmi "${IMAGE_NAME}:latest" > /dev/null 2>&1 || true
    docker rmi "${IMAGE_NAME}:ui-latest" > /dev/null 2>&1 || true
    echo -e "${GREEN}✓ Images removed${NC}"
}

# Clean everything including volumes
clean_all() {
    echo -e "${RED}WARNING: This will remove all data (containers, images, and volumes)${NC}"
    read -p "Are you sure? (yes/no): " -r
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        echo "Cancelled."
        exit 0
    fi
    
    clean
    
    # Remove volumes
    docker volume rm hitorro-data > /dev/null 2>&1 || true
    docker volume rm hitorro-files > /dev/null 2>&1 || true
    docker volume rm hitorro-logs > /dev/null 2>&1 || true
    echo -e "${GREEN}✓ Volumes removed${NC}"
    echo -e "${GREEN}✓ Complete cleanup done${NC}"
}

# Start in dev mode
start_dev() {
    echo -e "${BLUE}Starting in development mode...${NC}"
    echo ""
    echo -e "${YELLOW}This will start:${NC}"
    echo "  1. Backend on http://localhost:8080"
    echo "  2. Frontend on http://localhost:3000"
    echo ""
    echo -e "${CYAN}Starting backend...${NC}"
    cd "$PROJECT_ROOT"
    
    # Check if Maven is available
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}Maven not found. Please install Maven.${NC}"
        exit 1
    fi
    
    # Start backend in background
    mvn spring-boot:run > /dev/null 2>&1 &
    BACKEND_PID=$!
    echo -e "${GREEN}✓ Backend started (PID: $BACKEND_PID)${NC}"
    
    # Wait a bit for backend to start
    sleep 5
    
    # Start frontend
    echo -e "${CYAN}Starting frontend...${NC}"
    cd "$PROJECT_ROOT/frontend"
    
    if [ ! -d "node_modules" ]; then
        echo "Installing frontend dependencies..."
        npm install
    fi
    
    echo -e "${GREEN}✓ Starting Vite dev server...${NC}"
    npm run dev
}

# Docker compose up
compose_up() {
    echo -e "${BLUE}Starting with docker-compose...${NC}"
    cd "$PROJECT_ROOT"
    docker-compose up -d
    echo -e "${GREEN}✓ Services started${NC}"
    print_access_info
}

# Docker compose up with UI
compose_up_ui() {
    echo -e "${BLUE}Starting with docker-compose (UI)...${NC}"
    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose-with-ui.yml up -d
    echo -e "${GREEN}✓ Services started${NC}"
    print_access_info_ui
}

# Docker compose down
compose_down() {
    echo -e "${BLUE}Stopping docker-compose services...${NC}"
    cd "$PROJECT_ROOT"
    docker-compose down 2>/dev/null || docker-compose -f docker-compose-with-ui.yml down 2>/dev/null || true
    echo -e "${GREEN}✓ Services stopped${NC}"
}

# Print access information (backend only)
print_access_info() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                  APPLICATION STARTED                       ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}Access Points:${NC}"
    echo -e "  ${YELLOW}•${NC} Swagger UI:    ${BLUE}http://localhost:8080/swagger-ui.html${NC}"
    echo -e "  ${YELLOW}•${NC} H2 Console:    ${BLUE}http://localhost:8080/h2-console${NC}"
    echo -e "  ${YELLOW}•${NC} Actuator:      ${BLUE}http://localhost:8080/actuator${NC}"
    echo -e "  ${YELLOW}•${NC} REST API:      ${BLUE}http://localhost:8080/api/rest${NC}"
    echo -e "  ${YELLOW}•${NC} Telnet CLI:    ${BLUE}telnet localhost 9000${NC}"
    echo -e "  ${YELLOW}•${NC} SSH CLI:       ${BLUE}ssh -p 9022 localhost${NC}"
    echo ""
    echo -e "${YELLOW}View logs:${NC} $0 logs"
    echo ""
}

# Print access information (with UI)
print_access_info_ui() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║                  APPLICATION STARTED                       ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}Access Points:${NC}"
    echo -e "  ${YELLOW}★${NC} React UI:      ${MAGENTA}http://localhost:8080${NC} ${CYAN}(Main Interface)${NC}"
    echo -e "  ${YELLOW}•${NC} Swagger UI:    ${BLUE}http://localhost:8080/swagger-ui.html${NC}"
    echo -e "  ${YELLOW}•${NC} H2 Console:    ${BLUE}http://localhost:8080/h2-console${NC}"
    echo -e "  ${YELLOW}•${NC} Actuator:      ${BLUE}http://localhost:8080/actuator${NC}"
    echo -e "  ${YELLOW}•${NC} REST API:      ${BLUE}http://localhost:8080/api/rest${NC}"
    echo -e "  ${YELLOW}•${NC} Telnet CLI:    ${BLUE}telnet localhost 9000${NC}"
    echo -e "  ${YELLOW}•${NC} SSH CLI:       ${BLUE}ssh -p 9022 localhost${NC}"
    echo ""
    echo -e "${YELLOW}View logs:${NC} $0 logs"
    echo ""
}

# Main script logic
main() {
    print_banner
    
    COMMAND="${1:-help}"
    
    case "$COMMAND" in
        build)
            check_docker
            build_backend
            ;;
        build-ui)
            check_docker
            build_ui
            ;;
        start)
            check_docker
            start_backend
            ;;
        start-ui)
            check_docker
            start_ui
            ;;
        stop)
            check_docker
            stop_containers
            ;;
        restart)
            check_docker
            stop_containers
            sleep 2
            start_backend
            ;;
        logs)
            check_docker
            view_logs
            ;;
        status)
            check_docker
            show_status
            ;;
        clean)
            check_docker
            clean
            ;;
        clean-all)
            check_docker
            clean_all
            ;;
        dev)
            start_dev
            ;;
        compose-up)
            check_docker
            compose_up
            ;;
        compose-up-ui)
            check_docker
            compose_up_ui
            ;;
        compose-down)
            check_docker
            compose_down
            ;;
        help|--help|-h)
            print_usage
            ;;
        *)
            echo -e "${RED}Error: Unknown command '$COMMAND'${NC}"
            echo ""
            print_usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
