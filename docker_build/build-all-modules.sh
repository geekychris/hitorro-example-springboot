#!/bin/bash
#
# Helper script to build ALL Hitorro modules locally for testing
# This validates the build order works before trying in Docker
#

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Building ALL Hitorro Modules${NC}"
echo "======================================"
echo ""

cd /Users/chris/hitorro

echo -e "${YELLOW}Step 1: Building all non-Spring modules from parent POM...${NC}"
mvn clean install -Dmaven.test.skip=true -B -pl '!hitorro-spring-boot,!hitorro-example-springboot'

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Non-Spring modules built successfully${NC}"
else
    echo -e "${RED}✗ Failed to build non-Spring modules${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 2: Building Spring Boot modules...${NC}"
cd hitorro-spring-boot
mvn clean install -Dmaven.test.skip=true -B

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Spring Boot modules built successfully${NC}"
else
    echo -e "${RED}✗ Failed to build Spring Boot modules${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 3: Building example application...${NC}"
cd ../hitorro-example-springboot
mvn clean package -Dmaven.test.skip=true -B

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Example application built successfully${NC}"
else
    echo -e "${RED}✗ Failed to build example application${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}======================================"
echo "✓ ALL MODULES BUILT SUCCESSFULLY!"
echo "======================================${NC}"
echo ""
echo "JAR location: hitorro-example-springboot/target/*.jar"
echo ""
echo "To run:"
echo "  cd hitorro-example-springboot"
echo "  java -jar target/*.jar"
echo ""
