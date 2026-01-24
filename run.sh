#!/bin/bash

# Hitorro Spring Boot Example - Run Script
# This script runs the example application with proper HT_BIN and HT_HOME configuration

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Get the script directory (hitorro-example-springboot)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Get the parent directory (hitorro project root)
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
# Default HT_HOME location
DEFAULT_HT_HOME="$( cd "$PROJECT_ROOT/.." && pwd )/hthome"

# Use environment variables if set, otherwise use defaults
HT_BIN_PATH="${HT_BIN:-$PROJECT_ROOT}"
HT_HOME_PATH="${HT_HOME:-$DEFAULT_HT_HOME}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Hitorro Spring Boot Example${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}Configuration:${NC}"
echo -e "  HT_BIN:  ${YELLOW}${HT_BIN_PATH}${NC}"
echo -e "  HT_HOME: ${YELLOW}${HT_HOME_PATH}${NC}"
echo ""

# Verify HT_BIN directory exists
if [ ! -d "$HT_BIN_PATH" ]; then
    echo -e "${YELLOW}Warning: HT_BIN directory does not exist: $HT_BIN_PATH${NC}"
    echo -e "${YELLOW}Creating directory...${NC}"
    mkdir -p "$HT_BIN_PATH"
fi

# Verify type definitions exist
TYPE_DEF_DIR="$HT_BIN_PATH/config/types/core"
if [ ! -d "$TYPE_DEF_DIR" ]; then
    echo -e "${YELLOW}Warning: Type definitions directory does not exist: $TYPE_DEF_DIR${NC}"
    echo -e "${YELLOW}You may need to create type definitions for JVS to work properly.${NC}"
fi

# Verify HT_HOME directory exists
if [ ! -d "$HT_HOME_PATH" ]; then
    echo -e "${YELLOW}Warning: HT_HOME directory does not exist: $HT_HOME_PATH${NC}"
    echo -e "${YELLOW}Creating directory...${NC}"
    mkdir -p "$HT_HOME_PATH"
fi

# Build the application if needed
if [ ! -f "$SCRIPT_DIR/target/hitorro-example-springboot-"*.jar ] || [ "$1" == "--build" ]; then
    echo -e "${BLUE}Building application...${NC}"
    cd "$SCRIPT_DIR"
    mvn clean package -DskipTests
    echo ""
fi

# Find the JAR file
JAR_FILE=$(find "$SCRIPT_DIR/target" -name "hitorro-example-springboot-*.jar" -not -name "*-sources.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo -e "${YELLOW}No JAR file found. Building...${NC}"
    cd "$SCRIPT_DIR"
    mvn clean package -DskipTests
    JAR_FILE=$(find "$SCRIPT_DIR/target" -name "hitorro-example-springboot-*.jar" -not -name "*-sources.jar" | head -n 1)
fi

if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}Error: Could not find JAR file after build!${NC}"
    exit 1
fi

echo -e "${GREEN}Starting Hitorro Spring Boot Example...${NC}"
echo -e "${BLUE}JAR: ${JAR_FILE}${NC}"
echo ""
echo -e "${BLUE}Application will be available at:${NC}"
echo -e "  ${GREEN}http://localhost:8080${NC}"
echo -e "  ${GREEN}http://localhost:8080/actuator/health${NC}"
echo -e "  ${GREEN}http://localhost:8080/api/commands${NC}"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

# Run the application with proper system properties
java \
    -server \
    -DHT_BIN="$HT_BIN_PATH" \
    -DHT_HOME="$HT_HOME_PATH" \
    -Xmx2010M \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    -jar "$JAR_FILE" \
    "$@"
