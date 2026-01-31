#!/bin/bash
# Quick script to generate and copy a new structured logger
#
# Usage: ./add-logger.sh <config_name> [logger_name]
# Example: ./add-logger.sh order_events OrderEventsLogger
# Example: ./add-logger.sh user_activity

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)" 
STRUCTURED_LOGGING_HOME="/Users/chris/code/warp_experiments/done/structured-logging"

CONFIG_NAME=$1
LOGGER_NAME=${2:-""}

if [ -z "$CONFIG_NAME" ]; then
    echo "❌ Usage: $0 <config_name.json> [logger_name]"
    echo ""
    echo "Arguments:"
    echo "   config_name  - Name of the JSON config file (without .json extension)"
    echo "   logger_name  - (Optional) Name of the logger class (auto-detected if not provided)"
    echo ""
    echo "Examples:"
    echo "   $0 order_events              (detects logger from config)"
    echo "   $0 order_events OrderEventsLogger  (explicit logger name)"
    echo ""
    exit 1
fi

CONFIG_FILE="$STRUCTURED_LOGGING_HOME/log-configs/${CONFIG_NAME}.json"

# Check if config exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "❌ Error: Config file not found: $CONFIG_FILE"
    echo ""
    echo "Usage: $0 <config_name.json> [logger_name]"
    echo ""
    echo "Available configs:"
    ls -1 "$STRUCTURED_LOGGING_HOME/log-configs/*.json" 2>/dev/null | xargs -n1 basename | sed 's/.json$/'
    exit 1
fi

# Extract logger name from config if not provided
if [ -z "$LOGGER_NAME" ]; then
    LOGGER_NAME=$(python3 -c "import json; print(json.load(open('$CONFIG_FILE'))['name'])" 2>/dev/null)
    LOGGER_NAME="${LOGGER_NAME}Logger"
fi

if [ -z "$LOGGER_NAME" ]; then
    echo "❌ Error: Could not auto-detect logger name from config."
    echo "   Please provide logger name as second argument."
    exit 1
fi

echo "🚀 Adding structured logger: $LOGGER_NAME"
echo "   Config: $CONFIG_NAME"
echo "   Logger: $LOGGER_NAME"
echo ""

# Step 1: Generate the logger
echo "📝 Step 1: Generating logger from config..."
cd "$STRUCTURED_LOGGING_HOME"
python3 generators/generate_loggers.py "log-configs/${CONFIG_NAME}.json" --lang java

if [ ! -f "java-logger/src/main/java/com/logging/generated/${LOGGER_NAME}.java" ]; then
    echo "❌ Error: Failed to generate logger"
    exit 1
fi

echo "✅ Generated: $LOGGER_NAME"
echo ""

# Step 2: Install library
echo "📦 Step 2: Installing structured-logging library..."
cd "$STRUCTURED_LOGGING_HOME/java-logger"
mvn install -DskipTests -q 
echo "✅ Library installed"
echo ""

# Step 3: Copy logger to Hitorro example
echo "📋 Step 3: Copying logger to Hitorro example..."
SOURCE="java-logger/src/main/java/com/logging/generated/${LOGGER_NAME}.java"
TARGET="$PROJECT_ROOT/src/main/java/com/hitorro/example/logging/${LOGGER_NAME}.java"

if [ ! -f "$SOURCE" ]; then
    echo "❌ Error: Generated logger not found: $SOURCE"
    exit 1
fi

cp "$SOURCE" "$TARGET"

# Update package and add annotations
sed -i.bak 's/package com.logging.generated;/package com.hitorro.example.logging;/g' "$TARGET"
rm "${TARGET}.bak"

# Add Spring annotations (if not present)
if ! grep -q "@Component" "$TARGET"; then
    awk '
        /^package com\.hitorro\.example\.logging;/ {
            print
            print "import org.springframework.stereotype.Component;"
            print "import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;"
            print
        }
        { print }
    ' "${TARGET}.tmp" > "${TARGET}" && cat "${TARGET}" >> "${TARGET}.tmp" && mv "${TARGET}.tmp" "${TARGET}"
fi

if ! grep -q "@Component" "$TARGET"; then
    sed -i.bak3 '/^public class /i\
@Component\
@ConditionalOnProperty(prefix = "hitorro.structured-logging", name = "enabled", havingValue = "true")\
' "${TARGET}"
    rm "${TARGET}.bak3" 2>/dev/null || true
fi

echo "✅ Copied and configured: $LOGGER_NAME"
echo ""

# Step 4: Check for topic creation script
SCRIPT_FILE="$STRUCTURED_LOGGING_HOME/scripts/create-topic-$(grep '"topic":' "$CONFIG_FILE" | sed 's/.*"topic": "\([^"]*\)".*/\1').sh"

if [ -f "$SCRIPT_FILE" ]; then
    echo "🔗 Step 4: Found topic creation script:"
    echo "   $SCRIPT_FILE"
    
    # Ask if user wants to create topic
    read -p "   Create Kafka topic now? [y/N] " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        bash "$SCRIPT_FILE"
        echo "✅ Topic created"
    else
        echo "   ⏭️  Skipped topic creation (run manually later)"
    fi
    echo ""
else
    echo "   ℹ️  No topic creation script found (will create manually)"
    echo ""
fi

# Step 5: Consumer config
CONSUMER_CONFIG="$STRUCTURED_LOGGING_HOME/spark-consumer/log-configs/${CONFIG_NAME}.json"

if [ -f "$CONSUMER_CONFIG" ]; then
    echo "📊 Step 5: Config already exists in Spark consumer directory"
    echo "   Location: $CONSUMER_CONFIG"
else
    echo "📊 Step 5: Copy config to Spark consumer directory for auto-discovery..."
    cp "$CONFIG_FILE" "$CONSUMER_CONFIG"
    echo "✅ Config copied to: $CONSUMER_CONFIG"
    echo ""
fi

# Step 6: Summary
echo "✅ Logger successfully added!"
echo ""
echo "📝 Next Steps:"
echo "   1. Create a DemoController for this logger:"
echo "      - Copy an existing controller as a template"
echo "      - Update package: com.hitorro.example.logging"
echo "      - Inject your new logger"
echo "      - Add endpoints to test logging"
echo ""
echo "   2. Restart services:"
echo "      - Restart HitorroExampleApplication"
echo "      - Restart Spark consumer: cd $STRUCTURED_LOGGING_HOME && ./start-consumer.sh"
echo ""
echo "   3. Test the logger:"
echo "      - curl http://localhost:8080/api/demo/..."
echo "      - Check Kafka: docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic <your-topic> --from-beginning --max-messages 1"
echo "      - Query Iceberg: SELECT * FROM local.analytics_logs.<your_table> ORDER BY timestamp DESC LIMIT 10"
echo ""
echo "   4. Verify:"
echo "      - Check for errors in application logs"
echo "      - Monitor Spark consumer logs: docker exec spark-master tail -f /opt/spark-data/consumer.log"
echo "      - Verify data in Iceberg tables using Trino"
echo ""

echo "📚 Documentation:"
echo "   - Full Guide: $PROJECT_ROOT/STRUCTURED_LOGGING_INTEGRATION.md"
echo "   - Structured Logging Project: $STRUCTURED_LOGGING_HOME"
echo "   - Adding Loggers Guide: $STRUCTURED_LOGGING_HOME/ADDING_NEW_LOGGERS.md"