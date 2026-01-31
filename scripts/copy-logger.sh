#!/bin/bash
# Copy generated structured logger to Hitorro example and add Spring annotations
#
# Usage: ./copy-logger.sh <LoggerName (e.g., UserActivityLogLogger)>

set -e

# Configuration
STRUCTURED_LOGGING_HOME="/Users/chris/code/warp_experiments/done/structured-logging"
HITORRO_EXAMPLE="/Users/chris/hitorro/hitorro-example-springboot"

LOGGER_NAME=$1

if [ -z "$LOGGER_NAME" ]; then
    echo "❌ Usage: $0 <Logger Name (e.g., UserActivityLogLogger)>"
    echo "   Example: $0 OrderEventsLogger"
    echo "   Example: $0 UserActivityLogLogger"
    exit 1
fi

SOURCE="$STRUCTURED_LOGGING_HOME/java-logger/src/main/java/com/logging/generated/${LOGGER_NAME}.java"
TARGET="$HITORRO_EXAMPLE/src/main/java/com/hitorro/example/logging/${LOGGER_NAME}.java"

# Check if source exists
if [ ! -f "$SOURCE" ]; then
    echo "❌ Error: Logger not found: $SOURCE"
    echo "   Make sure to run: python3 generators/generate_loggers.py log-configs/your_config.json"
    exit 1
fi

echo "📦 Copying $LOGGER_NAME from structured-logging to Hitorro example..."

# Copy the file
cp "$SOURCE" "$TARGET"
echo "✅ Copied $LOGGER_NAME to: $TARGET"

# Update package declaration
if grep -q "package com.logging.generated;" "$TARGET"; then
    sed -i.bak 's/package com.logging.generated;/package com.hitorro.example.logging;/g' "$TARGET"
    rm "${TARGET}.bak"
    echo "✅ Updated package to: com.hitorro.example.logging"
fi

# Check if already has Spring annotations
HAS_COMPONENT=$(grep -c "@Component" "$TARGET" || true)
HAS_CONDITIONAL=$(grep -c "@ConditionalOnProperty" "$TARGET" || true)

if [ "$HAS_COMPONENT" -eq 0 ] || [ "$HAS_CONDITIONAL" -eq 0 ]; then
    echo "🔧 Adding Spring annotations..."
    
    # Backup for rollback
    cp "$TARGET" "${TARGET}.bak2"
    
    # Add imports and annotations after package statement
    awk '
        /^package com\.hitorro\.example\.logging;/ {
            print
            print "import org.springframework.stereotype.Component;"
            print "import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;"
            print
        }
        { print }
    ' "${TARGET}.bak2" > "${TARGET}"
    
    # Add annotations after class declaration
    sed -i.bak3 '/^public class /i\
@Component\
@ConditionalOnProperty(prefix = "hitorro.structured-logging", name = "enabled", havingValue = "true")\
    ' "$TARGET"
    
    rm "${TARGET}.bak2" "${TARGET}.bak3" 2>/dev/null || true
    echo "✅ Added @Component and @ConditionalOnProperty annotations"
else
    echo "✅ Spring annotations already present"
fi

# Verify the changes
echo ""
echo "📋 Verification:"
echo "   Package: $(grep "^package" "$TARGET" | head -1)"
echo "   @Component: $(grep "@Component" "$TARGET" | head -1)"
echo "   @ConditionalOnProperty: $(grep "@ConditionalOnProperty" "$TARGET" | head -1)"
echo ""
echo "✨ Done! $LOGGER_NAME has been copied and configured for Hitorro example."
echo ""
echo "📝 Next steps:"
echo "   1. Create a DemoController for this logger"
echo "   2. Add endpoints to test logging functionality"
echo "   3. Restart HitorroExampleApplication to load the new logger"
echo "   4. Test with: curl -X POST http://localhost:8080/api/demo/..."