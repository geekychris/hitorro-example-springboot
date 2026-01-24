# Troubleshooting Guide - HT_BIN and HT_HOME Configuration

## Quick Diagnosis

Run this command to check your configuration:

```bash
java -DHT_BIN=/Users/chris/hitorro -DHT_HOME=/Users/chris/hthome \
     -jar target/hitorro-example-springboot-*.jar
```

Look for these log messages during startup:

### ✅ Success Indicators

```
HT_BIN configured: /Users/chris/hitorro
HT_HOME configured: /Users/chris/hthome
✓ HT_BIN already configured: /Users/chris/hitorro
✓ HT_HOME configured: /Users/chris/hthome
✓ JsonTypeSystem initialized successfully
  Type definitions path: /Users/chris/hitorro/config/types/core/
```

### ❌ Problem Indicators

```
WARNING: HT_BIN not configured
CRITICAL: HT_BIN not configured!
HT_BIN not configured. Type definitions may not load correctly.
```

---

## Common Problems and Solutions

### Problem 1: "HT_BIN not configured" during startup

**Symptoms**:
- Warning or error messages about HT_BIN
- JsonTypeSystem may fail to initialize
- Type definitions not found

**Cause**: System properties not set before Spring Boot initializes

**Solutions** (in order of preference):

#### Solution A: Use the run script (Easiest)
```bash
cd hitorro-example-springboot
./run.sh
```

#### Solution B: Set environment variables
```bash
export HT_BIN=/Users/chris/hitorro
export HT_HOME=/Users/chris/hthome
mvn spring-boot:run
```

#### Solution C: Pass as JVM arguments
```bash
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DHT_BIN=/Users/chris/hitorro -DHT_HOME=/Users/chris/hthome"
```

#### Solution D: Configure in application.yml
Add to `src/main/resources/application.yml`:
```yaml
hitorro:
  jvs:
    type-definitions-path: /Users/chris/hitorro
```

**Note**: The `EnvironmentPostProcessor` will automatically pick this up and set the system property.

---

### Problem 2: Properties set but still not picked up

**Symptoms**:
- You set `-DHT_BIN` but logs still show "not configured"
- Environment variables don't seem to work

**Diagnosis**:
Add this to see what Spring Boot sees:

```bash
java -DHT_BIN=/Users/chris/hitorro \
     -Dlogging.level.com.hitorro.spring.autoconfigure=DEBUG \
     -jar target/hitorro-example-springboot-*.jar
```

**Possible Causes**:

1. **Properties set after Spring starts**
   - ❌ Wrong: Setting in `@PostConstruct` or bean methods
   - ✅ Correct: Set in `main()` BEFORE `SpringApplication.run()`

2. **Incorrect property syntax**
   - ❌ Wrong: `--DHT_BIN` (double dash)
   - ✅ Correct: `-DHT_BIN` (single dash)

3. **Quotes or spaces in path**
   - Use quotes: `-DHT_BIN="/Users/chris/my folder/hitorro"`

---

### Problem 3: Type definitions not found

**Symptoms**:
```
Type definitions may not load correctly
JsonTypeSystem initialized but types missing
```

**Diagnosis**:
Check if the directory structure exists:

```bash
ls -la /Users/chris/hitorro/config/types/core/
```

Expected output should show JSON files:
```
article.json
user.json
...
```

**Solutions**:

1. **Verify HT_BIN points to ROOT directory**:
   - ✅ Correct: `HT_BIN=/Users/chris/hitorro`
   - ❌ Wrong: `HT_BIN=/Users/chris/hitorro/config`
   - ❌ Wrong: `HT_BIN=/Users/chris/hitorro/config/types`

2. **Create missing directories**:
   ```bash
   mkdir -p /Users/chris/hitorro/config/types/core
   ```

3. **Copy type definitions** (if you have them elsewhere):
   ```bash
   cp source/types/*.json /Users/chris/hitorro/config/types/core/
   ```

---

### Problem 4: Works in IDE but not from command line

**Symptoms**:
- Runs fine in IntelliJ IDEA
- Fails when running `mvn spring-boot:run` or JAR

**Cause**: IDE run configuration has VM options, but command line doesn't

**Solution**:
Always include VM options when running from command line:

```bash
# With Maven
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DHT_BIN=/Users/chris/hitorro -DHT_HOME=/Users/chris/hthome"

# With JAR
java -DHT_BIN=/Users/chris/hitorro \
     -DHT_HOME=/Users/chris/hthome \
     -jar target/hitorro-example-springboot-*.jar
```

Or use environment variables (works everywhere):
```bash
export HT_BIN=/Users/chris/hitorro
export HT_HOME=/Users/chris/hthome
```

---

### Problem 5: Environment variables not recognized

**Symptoms**:
- `export HT_BIN=...` doesn't work
- Variable shows in `echo $HT_BIN` but app doesn't see it

**Diagnosis**:
```bash
# Check if variable is exported
export | grep HT_BIN

# Should show:
declare -x HT_BIN="/Users/chris/hitorro"
```

**Solutions**:

1. **Make sure to use `export`**:
   ```bash
   # ❌ Wrong
   HT_BIN=/path/to/hitorro
   
   # ✅ Correct
   export HT_BIN=/path/to/hitorro
   ```

2. **Set in shell profile for persistence**:
   ```bash
   # Add to ~/.bashrc or ~/.zshrc
   echo 'export HT_BIN=/Users/chris/hitorro' >> ~/.bashrc
   echo 'export HT_HOME=/Users/chris/hthome' >> ~/.bashrc
   source ~/.bashrc
   ```

3. **Verify in new shell**:
   ```bash
   # Open new terminal and check
   echo $HT_BIN
   ```

---

### Problem 6: Configuration order issues

**Symptoms**:
- Sometimes works, sometimes doesn't
- Inconsistent behavior

**Root Cause**: Configuration being set too late in startup process

**The Fix** (already implemented):

1. **EnvironmentPostProcessor** runs FIRST (before any beans)
2. **Application main()** sets defaults BEFORE Spring starts
3. **JsonTypeSystemManager** validates during bean initialization

**Verification**:
Look for this order in logs:
```
1. HT_BIN configured: /Users/chris/hitorro          [main() or env var]
2. ✓ HT_BIN already configured: ...                  [EnvironmentPostProcessor]
3. ✓ JsonTypeSystem initialized successfully         [JsonTypeSystemManager]
```

---

## Testing Your Configuration

### Test 1: Check System Properties

Add this to your application:
```java
@Component
public class ConfigurationChecker {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationChecker.class);
    
    @PostConstruct
    public void checkConfiguration() {
        log.info("HT_BIN system property: {}", System.getProperty("HT_BIN"));
        log.info("HT_HOME system property: {}", System.getProperty("HT_HOME"));
        log.info("HT_BIN environment: {}", System.getenv("HT_BIN"));
        log.info("HT_HOME environment: {}", System.getenv("HT_HOME"));
    }
}
```

### Test 2: Verify Directory Structure

```bash
#!/bin/bash
echo "Checking Hitorro configuration..."

if [ -z "$HT_BIN" ]; then
    echo "❌ HT_BIN environment variable not set"
else
    echo "✓ HT_BIN=$HT_BIN"
    
    if [ -d "$HT_BIN/config/types/core" ]; then
        echo "✓ Type definitions directory exists"
        echo "  Files: $(ls -1 $HT_BIN/config/types/core/*.json 2>/dev/null | wc -l) JSON files"
    else
        echo "❌ Type definitions directory missing: $HT_BIN/config/types/core"
    fi
fi

if [ -z "$HT_HOME" ]; then
    echo "⚠ HT_HOME environment variable not set (optional)"
else
    echo "✓ HT_HOME=$HT_HOME"
fi
```

### Test 3: Run with Debug Logging

```bash
java -DHT_BIN=/Users/chris/hitorro \
     -DHT_HOME=/Users/chris/hthome \
     -Dlogging.level.com.hitorro=DEBUG \
     -Dlogging.level.com.hitorro.spring.autoconfigure=DEBUG \
     -jar target/hitorro-example-springboot-*.jar
```

---

## Environment-Specific Configuration

### Development (Local Machine)
```bash
# ~/.bashrc or ~/.zshrc
export HT_BIN=/Users/chris/hitorro
export HT_HOME=/Users/chris/hthome
```

### CI/CD Pipeline
```yaml
# .github/workflows/build.yml
env:
  HT_BIN: ${{ github.workspace }}/hitorro
  HT_HOME: ${{ github.workspace }}/hthome
```

### Docker Container
```dockerfile
ENV HT_BIN=/app/hitorro
ENV HT_HOME=/data/hitorro

# Or via docker-compose.yml
environment:
  - HT_BIN=/app/hitorro
  - HT_HOME=/data/hitorro
```

### Kubernetes
```yaml
env:
  - name: HT_BIN
    value: /opt/hitorro
  - name: HT_HOME
    value: /var/lib/hitorro
```

### Systemd Service
```ini
[Service]
Environment="HT_BIN=/opt/hitorro"
Environment="HT_HOME=/var/lib/hitorro"
```

---

## Getting Help

If problems persist:

1. **Enable debug logging**:
   ```yaml
   logging:
     level:
       com.hitorro: DEBUG
       com.hitorro.spring.autoconfigure: TRACE
   ```

2. **Check startup logs** for the configuration sequence

3. **Verify with the test script** from Test 2 above

4. **Review the complete initialization**:
   - Look for HT_BIN messages in console output
   - Check EnvironmentPostProcessor logs
   - Verify JsonTypeSystemManager initialization
   - Confirm type definitions path

5. **Common gotchas**:
   - Don't set properties in `@Configuration` classes (too late!)
   - Don't use `@Value("${HT_BIN}")` - use system properties
   - Paths with spaces need quotes: `"/path with spaces/"`
   - Windows paths use forward slashes: `C:/hitorro` not `C:\hitorro`
