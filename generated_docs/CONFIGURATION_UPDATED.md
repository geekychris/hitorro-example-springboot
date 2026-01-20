# Hitorro Configuration - Updated Best Practices

## Recommended Approach: Use application.yml ✅

The **best and cleanest way** to configure Hitorro is directly in your `application.yml`:

```yaml
hitorro:
  # Hitorro installation directory (contains config/, types/, etc.)
  ht-bin: /Users/chris/hitorro
  
  # Hitorro home directory (runtime data, logs, cache)  
  ht-home: /Users/chris/hthome
```

That's it! The `EnvironmentPostProcessor` automatically converts these to `HT_BIN` and `HT_HOME` system properties before any Hitorro components initialize.

## Why This Is Better

### Old Approach ❌
```bash
# Had to set system properties everywhere
java -DHT_BIN=/path -DHT_HOME=/path -jar app.jar

# Or environment variables
export HT_BIN=/path
export HT_HOME=/path
```

### New Approach ✅
```yaml
# Just configure once in application.yml
hitorro:
  ht-bin: /Users/chris/hitorro
  ht-home: /Users/chris/hthome
```

### Benefits

1. **Single Source of Truth**: Configuration lives with your application config
2. **Environment-Specific**: Use Spring profiles for different environments
3. **Property Placeholders**: Can use `${user.home}`, `${PROJECT_DIR}`, etc.
4. **IDE Integration**: IntelliJ/Eclipse see and autocomplete these properties
5. **No JVM Arguments Needed**: Just run `mvn spring-boot:run` or `java -jar`

## Configuration Priority

The system checks in this order:

1. **JVM System Property** (highest priority)
   ```bash
   -DHT_BIN=/override/path
   ```

2. **Environment Variable**
   ```bash
   export HT_BIN=/override/path
   ```

3. **Spring Configuration** (recommended) ⭐
   ```yaml
   hitorro:
     ht-bin: /Users/chris/hitorro
   ```

This means you can still override via system properties or environment variables when needed.

## Environment-Specific Configuration

### Development (application.yml)
```yaml
hitorro:
  ht-bin: /Users/chris/hitorro
  ht-home: /Users/chris/hthome
```

### Production (application-prod.yml)
```yaml
hitorro:
  ht-bin: /opt/hitorro
  ht-home: /var/lib/hitorro
```

### Docker (application-docker.yml)
```yaml
hitorro:
  ht-bin: /app/hitorro
  ht-home: /data/hitorro
```

Then run with: `java -jar app.jar --spring.profiles.active=prod`

## Using Property Placeholders

### User Home Directory
```yaml
hitorro:
  ht-bin: ${user.home}/hitorro
  ht-home: ${user.home}/hthome
```

### Environment Variables
```yaml
hitorro:
  ht-bin: ${HITORRO_INSTALL_DIR:/opt/hitorro}  # Default if not set
  ht-home: ${HITORRO_DATA_DIR:/var/lib/hitorro}
```

### Custom Properties
```yaml
app:
  base-path: /opt/myapp

hitorro:
  ht-bin: ${app.base-path}/hitorro
  ht-home: ${app.base-path}/data
```

## Complete Example

```yaml
# Hitorro Spring Boot Example - application.yml

server:
  port: 8080

spring:
  application:
    name: hitorro-example
  profiles:
    active: dev

# Hitorro Configuration
hitorro:
  enabled: true
  
  # Core paths - automatically converted to system properties
  ht-bin: ${user.home}/hitorro
  ht-home: ${user.home}/hthome
  
  # Services
  services:
    enabled: true
    db-init: false
  
  # JSON Type System  
  jvs:
    enabled: true
    nlp-enabled: false
  
  # DMS (Document Management System)
  dms:
    enabled: true
    session-scope: prototype
    db-init:
      enabled: true
      fail-on-error: false
      data-sets:
        - name: "stores"
          csv-file: "classpath:data/stores.csv"
          consumer: "com.hitorro.basedms.csvconsumers.StoreCSVConsumer"
  
  # REST endpoints
  rest:
    enabled: true
    base-path: /api/rest
  
  # Command endpoints
  commands:
    rest:
      enabled: true
      base-path: /api/commands
  
  # CLI  
  cli:
    native-enabled: true
    telnet-port: 5050
    ssh-port: 5022
```

## Running the Application

### With Maven
```bash
mvn spring-boot:run
```

### With JAR
```bash
java -jar target/hitorro-example-springboot-*.jar
```

### With Profile
```bash
java -jar app.jar --spring.profiles.active=prod
```

### Override if Needed
```bash
# Still works if you need to override
java -DHT_BIN=/special/path -jar app.jar
```

No JVM arguments needed! Configuration comes from `application.yml`.

## Verification

Look for these messages on startup:

```
✅ Good - From application.yml:
HT_BIN set from Spring configuration (hitorro.ht-bin): /Users/chris/hitorro
HT_HOME set from Spring configuration (hitorro.ht-home): /Users/chris/hthome
✓ HT_BIN already configured: /Users/chris/hitorro
✓ JsonTypeSystem initialized successfully

✅ Also Good - From environment:
HT_BIN set from environment variable: /Users/chris/hitorro
HT_HOME set from environment variable: /Users/chris/hthome

✅ Also Good - From system property:
HT_BIN already set via system property: /Users/chris/hitorro
HT_HOME already set via system property: /Users/chris/hthome
```

## IntelliJ IDEA

With this approach, you can simplify your run configuration:

### Before (Complex)
```
VM Options: -server -DHT_BIN=$PROJECT_DIR$/ -DHT_HOME="$PROJECT_DIR$/../hthome" -Xmx2010M --add-opens java.base/java.lang=ALL-UNNAMED
```

### After (Simple)
```
VM Options: -server -Xmx2010M --add-opens java.base/java.lang=ALL-UNNAMED
```

The `HT_BIN` and `HT_HOME` come from `application.yml` automatically!

## Migration from Old Approach

If you have existing configurations:

### Old: System Properties
```bash
-DHT_BIN=/Users/chris/hitorro -DHT_HOME=/Users/chris/hthome
```

### New: application.yml
```yaml
hitorro:
  ht-bin: /Users/chris/hitorro
  ht-home: /Users/chris/hthome
```

Remove the system properties - they're not needed anymore!

### Old: Environment Variables
```bash
export HT_BIN=/Users/chris/hitorro
export HT_HOME=/Users/chris/hthome
```

### New: application.yml
```yaml
hitorro:
  ht-bin: ${HT_BIN:/Users/chris/hitorro}    # Uses env var if set, else default
  ht-home: ${HT_HOME:/Users/chris/hthome}
```

Best of both worlds - environment variables still work as overrides!

## Docker/Kubernetes Deployment

### Option 1: Bake into image
```yaml
# application.yml in Docker image
hitorro:
  ht-bin: /app/hitorro
  ht-home: /data/hitorro
```

### Option 2: Environment variables (for flexibility)
```yaml
# application.yml
hitorro:
  ht-bin: ${HT_BIN}
  ht-home: ${HT_HOME}
```

```dockerfile
# Dockerfile
ENV HT_BIN=/app/hitorro
ENV HT_HOME=/data/hitorro
```

### Option 3: ConfigMap (Kubernetes)
```yaml
# ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: hitorro-config
data:
  application.yml: |
    hitorro:
      ht-bin: /opt/hitorro
      ht-home: /var/lib/hitorro
```

## Troubleshooting

### Check What's Configured

Add this to see resolved values:

```java
@Component
public class ConfigChecker {
    @Autowired
    private HitorroProperties properties;
    
    @PostConstruct
    public void check() {
        log.info("Config - ht-bin: {}", properties.getHtBin());
        log.info("Config - ht-home: {}", properties.getHtHome());
        log.info("System - HT_BIN: {}", System.getProperty("HT_BIN"));
        log.info("System - HT_HOME: {}", System.getProperty("HT_HOME"));
    }
}
```

### Property Not Resolved

If you see `${HT_BIN}` instead of actual path:

1. Check you're using Spring Boot's property resolution
2. Ensure no typos in property names
3. Try with quotes: `ht-bin: "${HT_BIN:/default/path}"`

### Still Want System Properties?

That's fine! The system still supports all methods:

```bash
# All of these work:
java -DHT_BIN=/path -jar app.jar                    # System property
HT_BIN=/path java -jar app.jar                      # Environment variable  
java -jar app.jar                                    # application.yml
```

## Summary

✅ **RECOMMENDED**: Configure in `application.yml`
```yaml
hitorro:
  ht-bin: /Users/chris/hitorro
  ht-home: /Users/chris/hthome
```

✅ **ALSO WORKS**: Environment variables (for overrides)
```bash
export HT_BIN=/Users/chris/hitorro
```

✅ **ALSO WORKS**: System properties (for overrides)
```bash
-DHT_BIN=/Users/chris/hitorro
```

**Best practice**: Use `application.yml` as the default, environment variables or system properties for overrides.
