# Hitorro Spring Boot Example - Configuration Guide

## Required System Properties

The Hitorro framework requires two essential system properties to be set:

> **Note**: The Spring Boot autoconfiguration now includes an `EnvironmentPostProcessor` that automatically
> configures these properties from environment variables or Spring configuration during early startup.

### HT_BIN
**Purpose**: Points to the root Hitorro installation directory containing configuration files and type definitions.

**Expected Directory Structure**:
```
${HT_BIN}/
├── config/
│   ├── types/
│   │   └── core/        # JSON type definitions
│   ├── services/        # Service configurations
│   └── ...
└── ...
```

### HT_HOME
**Purpose**: Points to the Hitorro home directory for runtime data, logs, and user-specific configurations.

**Expected Directory Structure**:
```
${HT_HOME}/
├── logs/
├── data/
├── cache/
└── ...
```

## Automatic Configuration

The `hitorro-spring-boot-starter` includes an `EnvironmentPostProcessor` that runs **very early** in the
Spring Boot startup process (before bean creation). It automatically configures `HT_BIN` and `HT_HOME` from:

1. JVM system properties (if already set)
2. Environment variables
3. Spring configuration properties

This means you typically only need to set environment variables or add to `application.yml` - the framework
handles the rest automatically.

## Configuration Methods

### Method 1: Environment Variables (Recommended)

Set environment variables before starting the application:

```bash
export HT_BIN=/Users/chris/hitorro
export HT_HOME=/Users/chris/hthome
mvn spring-boot:run
```

Or for a JAR file:
```bash
export HT_BIN=/Users/chris/hitorro
export HT_HOME=/Users/chris/hthome
java -jar hitorro-example-springboot.jar
```

### Method 2: JVM System Properties

Pass as JVM arguments:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DHT_BIN=/Users/chris/hitorro -DHT_HOME=/Users/chris/hthome"
```

Or for a JAR file:
```bash
java -DHT_BIN=/Users/chris/hitorro -DHT_HOME=/Users/chris/hthome -jar hitorro-example-springboot.jar
```

### Method 3: Application Configuration (Spring Boot)

Configure in `application.yml`:

```yaml
hitorro:
  jvs:
    # This sets HT_BIN internally
    type-definitions-path: /Users/chris/hitorro
```

**Note**: This only configures `HT_BIN` for JVS. `HT_HOME` still needs to be set via environment or system property.

### Method 4: IntelliJ IDEA Run Configuration

Use the provided run configuration `HitorroExampleSpringBoot` which includes:

**VM Options**:
```
-server 
-DHT_BIN=$PROJECT_DIR$/ 
-DHT_HOME="$PROJECT_DIR$/../hthome" 
-Xmx2010M 
--add-opens java.base/java.lang=ALL-UNNAMED
```

## Default Values

The `HitorroExampleApplication` class provides defaults if no configuration is found:

- **HT_BIN**: Defaults to `/Users/chris/hitorro`
- **HT_HOME**: Defaults to `/Users/chris/hthome`

**Warning**: These defaults are development-specific. Override them for your environment.

## Verification

To verify your configuration is correct, check the startup logs:

```
INFO  JsonTypeSystemManager : HT_BIN already configured: /Users/chris/hitorro
INFO  JsonTypeSystemManager : JsonTypeSystem initialized successfully
INFO  JsonTypeSystemManager : Type definitions path: /Users/chris/hitorro/config/types/core/
```

## Troubleshooting

### Problem: "HT_BIN not configured" warning

**Solution**: Set `HT_BIN` using one of the methods above.

### Problem: Type definitions not loading

**Cause**: `HT_BIN` doesn't point to correct directory or type definitions missing.

**Solution**:
1. Verify `HT_BIN` points to root directory (not `config/` subdirectory)
2. Check that `${HT_BIN}/config/types/core/*.json` exists
3. Ensure type definition files have correct JSON format

### Problem: Property resolution failures

**Cause**: `HT_HOME` not configured.

**Solution**: Set `HT_HOME` environment variable or system property.

## Production Deployment

For production environments:

1. **Set via Environment**:
   ```bash
   export HT_BIN=/opt/hitorro
   export HT_HOME=/var/lib/hitorro
   ```

2. **Create Systemd Service** (Linux):
   ```ini
   [Service]
   Environment="HT_BIN=/opt/hitorro"
   Environment="HT_HOME=/var/lib/hitorro"
   ExecStart=/usr/bin/java -jar /opt/hitorro-app.jar
   ```

3. **Docker Container**:
   ```dockerfile
   ENV HT_BIN=/app/hitorro
   ENV HT_HOME=/data/hitorro
   ```

   Or via docker-compose:
   ```yaml
   environment:
     - HT_BIN=/app/hitorro
     - HT_HOME=/data/hitorro
   ```

## Additional VM Arguments

The complete recommended VM arguments for optimal performance:

```
-server                                          # Server mode JVM
-DHT_BIN=/Users/chris/hitorro                   # Hitorro installation
-DHT_HOME=/Users/chris/hthome                   # Hitorro home directory
-Xmx2010M                                        # Maximum heap size
--add-opens java.base/java.lang=ALL-UNNAMED     # Required for reflection
```

## Configuration Validation

You can verify your configuration using the Actuator health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Look for Hitorro-related health indicators in the response.
