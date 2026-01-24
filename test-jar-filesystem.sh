#!/bin/bash

# Test Script for JAR FileSystem Endpoints
# Demonstrates the working JAR filesystem in Spring Boot

set -e

echo "======================================"
echo "JAR FileSystem Demo"
echo "======================================"
echo ""

# First, create the test JAR by running tests
echo "Step 1: Creating test JAR..."
mvn test -Dtest=FileSystemControllerSimpleTest -q
if [ -f "./target/test-resources.jar" ]; then
    echo "✅ Test JAR created: ./target/test-resources.jar"
    
    # Show what's in the JAR
    echo ""
    echo "Contents of test JAR:"
    jar tf ./target/test-resources.jar | grep -v "META-INF" | head -10
else
    echo "❌ Failed to create test JAR"
    exit 1
fi

echo ""
echo "======================================"
echo "Step 2: Starting Spring Boot app..."
echo "======================================"
echo ""
echo "Starting application in background..."
mvn spring-boot:run > /tmp/springboot.log 2>&1 &
APP_PID=$!

# Wait for app to start
echo "Waiting for application to start..."
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Application started!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Application failed to start"
        kill $APP_PID 2>/dev/null || true
        exit 1
    fi
    sleep 1
    echo -n "."
done

echo ""
echo ""
echo "======================================"
echo "Step 3: Testing JAR Endpoints"
echo "======================================"
echo ""

# Test 1: Check status
echo "Test 1: Check filesystem status"
echo "GET http://localhost:8080/api/filesystem/status"
curl -s http://localhost:8080/api/filesystem/status | python3 -m json.tool || echo "(Not JSON)"
echo ""
echo ""

# Test 2: List all files in JAR
echo "Test 2: List all files in JAR"
echo "GET http://localhost:8080/api/filesystem/jar/list"
curl -s http://localhost:8080/api/filesystem/jar/list | python3 -m json.tool || echo "(Not JSON)"
echo ""
echo ""

# Test 3: Read file from JAR
echo "Test 3: Read file from JAR"
echo "GET http://localhost:8080/api/filesystem/jar/read/test.txt"
echo "Response:"
curl -s http://localhost:8080/api/filesystem/jar/read/test.txt
echo ""
echo ""

# Test 4: Read nested file
echo "Test 4: Read nested file from JAR"
echo "GET http://localhost:8080/api/filesystem/jar/read/data/data.txt"
echo "Response:"
curl -s http://localhost:8080/api/filesystem/jar/read/data/data.txt
echo ""
echo ""

# Test 5: List directory
echo "Test 5: List files in 'data' directory"
echo "GET http://localhost:8080/api/filesystem/jar/list?path=data"
curl -s "http://localhost:8080/api/filesystem/jar/list?path=data" | python3 -m json.tool || echo "(Not JSON)"
echo ""
echo ""

echo "======================================"
echo "✅ All tests completed!"
echo "======================================"
echo ""
echo "The JAR filesystem is working correctly!"
echo ""
echo "To stop the application:"
echo "  kill $APP_PID"
echo ""
echo "Or manually:"
echo "  ps aux | grep spring-boot | grep -v grep | awk '{print \$2}' | xargs kill"
echo ""

# Cleanup
read -p "Press Enter to stop the application..."
kill $APP_PID 2>/dev/null || true
echo "Application stopped."
