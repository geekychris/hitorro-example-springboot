# Utility Scripts for Adding Structured Loggers

This directory contains helper scripts to simplify the process of adding new structured loggers to the Hitorro Spring Boot example application.

## Scripts

### add-logger.sh

Automated script to generate, copy, and configure a new structured logger in one command.

**Usage:**
```bash
./add-logger.sh <config_name> [logger_name]
```

**Examples:**
```bash
# Auto-detect logger name from config
./add-logger.sh order_events

# Specify logger name explicitly
./add-logger.sh user_events UserEventsLogger
```

**What it does:**
1. Generates the Java logger from the JSON config using `generate_loggers.py`
2. Installs the updated `structured-logging-java` library
3. Copies the generated logger to the Hitorro example
4. Updates the package declaration from `com.logging.generated` to `com.hitorro.example.logging`
5. Adds `@Component` and `@ConditionalOnProperty` annotations
6. (Optional) Creates the Kafka topic
7. Copies the config to the Spark consumer directory for auto-discovery

### copy-logger.sh

Simple script to just copy and configure an already-generated logger.

**Usage:**
```bash
./copy-logger.sh <LoggerName>
```

**Example:**
```bash
./copy-logger.sh UserActivityLogLogger
./copy-logger.sh OrderEventsLogger
```

**What it does:**
1. Copies the logger from `structured-logging` project to Hitorro example
2. Updates package declaration
3. Adds Spring annotations (`@Component`, `@ConditionalOnProperty`)

## Workflow Comparison

### Manual Process (without scripts)

1. Create config file in `structured-logging/log-configs/`
2. Run `python3 generators/generate_loggers.py`
3. Find generated logger in `java-logger/src/main/java/com/logging/generated/`
4. Copy to `hitorro-example-springboot/src/main/java/com/hitorro/example/logging/`
5. Update package statement
6. Add `@Component` and `@ConditionalOnProperty`
7. Reinstall structured-logging library: `mvn install -DskipTests`
8. Create Kafka topic (or use generated script)
9. Copy config to Spark consumer
10. Restart services

**Time:** ~10-15 minutes with lots of manual steps

### Automated Process (with scripts)

```bash
# One command does everything:
./add-logger.sh order_events
```

**Time:** 1-2 minutes

## Prerequisites

- Scripts must be executable: `chmod +x *.sh`
- Python 3 must be available
- Maven must be available for library installation
- Config files must exist in `structured-logging/log-configs/`

## Common Issues

### "Logger not found" error

**Cause:** Config file doesn't exist or hasn't been generated yet.

**Fix:** 
```bash
# Check if config exists
ls /Users/chris/code/warp_experiments/done/structured-logging/log-configs/

# Generate logger first
python3 /Users/chris/code/warp_experiments/done/structured-logging/generators/generate_loggers.py \
  log-configs/your_config.json
```

### "Auto-detect logger name from config" fails

**Cause:** Config file has invalid JSON or missing `name` field.

**Fix:** Provide logger name explicitly or fix the config:
```bash
./add-logger.sh your_config YourCustomLogger
```

### Annotations not added

**Cause:** Script uses `sed` and `awk` which may fail on complex class structures.

**Fix:** Manually add these lines after the class declaration:
```java
@Component
@ConditionalOnProperty(prefix = "hitorro.structured-logging", name = "enabled", havingValue = "true")
```

## Example: Adding an "OrderEvents" Logger

1. **Create config:**
   ```bash
   # Edit or create the config
   vi /Users/chris/code/warp_experiments/done/structured-logging/log-configs/order_events.json
   ```

2. **Run the script:**
   ```bash
   cd /Users/chris/hitorro/hitorro-example-springboot
   ./scripts/add-logger.sh order_events
   ```

3. **Create demo controller:**
   ```bash
   # Copy from existing controllers
   cp src/main/java/com/hitorro/example/logging/UserActivityDemoController.java \
      src/main/java/com/hitorro/example/logging/OrderDemoController.java
   
   # Edit to match your logger and endpoints
   vi src/main/java/com/hitorro/example/logging/OrderDemoController.java
   ```

4. **Create topic:**
   ```bash
   bash /Users/chris/code/warp_experiments/done/structured-logging/scripts/create-topic-order-events.sh
   ```

5. **Restart app and test**

## Integration with the Structured Logging Repo

These scripts simplify the workflow described in:
- **Full Guide**: `../STRUCTURED_LOGGING_INTEGRATION.md`
- **Structured Logging Project**: `/Users/chris/code/warp_experiments/done/structured-logging`

The scripts bridge the gap between:
- Generating loggers in the structured-logging project
- Using them in Hitorro Spring Boot example
- Making them Spring-managed beans with conditional configuration

## Tips

1. **Keep the structured-logging project updated:**
   ```bash
   # After making changes to structured-logging
   cd /Users/chris/code/warp_experiments/done/structured-logging
   mvn install -DskipTests
   ```

2. **Test locally before committing:**
   - Start the app with `hitorro.structured-logging.enabled=true`
   - Test endpoints with curl
   - Check Kafka for messages
   - Verify data in Iceberg

3. **Use the generator documentation for help:**
   ```bash
   python3 /Users/chris/code/warp_experiments/done/structured-logging/generators/generate_loggers.py --help
   ```

4. **Validate your configs:**
   ```bash
   python -m json.tool your_config.json
   ```