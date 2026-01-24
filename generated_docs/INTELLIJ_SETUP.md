# IntelliJ IDEA Setup Guide

## Quick Start

The project includes a pre-configured run configuration. Just follow these steps:

### Option 1: Use the Provided Run Configuration

1. **Open the project** in IntelliJ IDEA
2. **Select the run configuration**: 
   - Look for the dropdown in the top toolbar
   - Select **"HitorroExampleSpringBoot"**
3. **Run or Debug**:
   - Click the **Run** button (green triangle) or press `Shift+F10`
   - Click the **Debug** button (bug icon) or press `Shift+F9`

The configuration is already set up with:
- ✅ Java 23
- ✅ `HT_BIN` = `$PROJECT_DIR$/` (your project root)
- ✅ `HT_HOME` = `$PROJECT_DIR$/../hthome`
- ✅ Required JVM arguments
- ✅ Proper working directory

### Option 2: Create a New Run Configuration

If you need to create a new one or customize:

1. **Open Run Configurations**:
   - Click `Run` → `Edit Configurations...`
   - Or use the dropdown in toolbar → `Edit Configurations...`

2. **Add New Spring Boot Configuration**:
   - Click the `+` button
   - Select `Spring Boot`

3. **Configure Basic Settings**:
   ```
   Name: HitorroExampleSpringBoot
   Main class: com.hitorro.example.HitorroExampleApplication
   Module: hitorro-example-springboot
   ```

4. **Set JVM Options** (CRITICAL):
   ```
   -server
   -DHT_BIN=$PROJECT_DIR$/
   -DHT_HOME="$PROJECT_DIR$/../hthome"
   -Xmx2010M
   --add-opens java.base/java.lang=ALL-UNNAMED
   ```

5. **Configure Working Directory**:
   ```
   $PROJECT_DIR$/hitorro-example-springboot
   ```

6. **Set Java Version**:
   - Build and run: Java 23 (or your project JDK)

7. **Click Apply → OK**

## Debugging Configuration

### Debug with Full Logging

1. Select the **HitorroExampleSpringBoot** configuration
2. Click the **Debug** button (bug icon) or press `Shift+F9`
3. Set breakpoints as needed:
   - `HitorroExampleApplication.configureHitorroSystemProperties()` - See property setup
   - `JsonTypeSystemManager.afterPropertiesSet()` - See JVS initialization
   - Your controller methods

### Debug with Enhanced Logging

Add to `application.yml` (or create `application-debug.yml`):

```yaml
logging:
  level:
    root: INFO
    com.hitorro: DEBUG
    com.hitorro.spring: DEBUG
    com.hitorro.spring.autoconfigure: TRACE
    com.hitorro.jsontypesystem: DEBUG
```

Then run with debug profile:
- Add to VM options: `-Dspring.profiles.active=debug`

### Useful Breakpoints

Set breakpoints at these locations to understand the initialization flow:

1. **Property Configuration**:
   ```
   HitorroExampleApplication.main() - line 46
   HitorroExampleApplication.configureHitorroSystemProperties() - line 54
   ```

2. **Environment Processing**:
   ```
   HitorroEnvironmentPostProcessor.postProcessEnvironment() - line 44
   HitorroEnvironmentPostProcessor.configureHtBin() - line 52
   ```

3. **JVS Initialization**:
   ```
   JsonTypeSystemManager.afterPropertiesSet() - line 68
   JsonTypeSystem.getMe() - (in Hitorro framework)
   ```

4. **Your Application**:
   ```
   ExampleController methods
   Your service classes
   ```

## Environment Variables Method (Alternative)

If you prefer environment variables over system properties:

1. **Open Run Configurations**
2. **Select your configuration**
3. **Find "Environment variables"** section
4. **Add variables**:
   ```
   HT_BIN=/Users/chris/hitorro
   HT_HOME=/Users/chris/hthome
   ```

5. **Click Apply → OK**

The `EnvironmentPostProcessor` will pick these up automatically.

## Troubleshooting in IntelliJ

### Problem: Configuration not found

**Solution**: The run configuration file should be at:
```
.idea/runConfigurations/HitorroExampleSpringBoot.xml
```

If missing:
1. Copy from `idea/runConfigurations/HitorroExampleSpringBoot.run.xml`
2. Or create manually using Option 2 above

### Problem: "HT_BIN not configured" in debug mode

**Check these in order**:

1. **Verify VM options are set**:
   - Open Run Configuration
   - Check "VM options" field contains `-DHT_BIN=...`

2. **Check variable substitution**:
   - IntelliJ should expand `$PROJECT_DIR$`
   - View actual value: `Run` → `Edit Configurations...` → hover over the field

3. **Add explicit path** (if variable doesn't work):
   ```
   -DHT_BIN=/Users/chris/hitorro
   -DHT_HOME=/Users/chris/hthome
   ```

### Problem: Breakpoints not hit

**Possible causes**:

1. **Build not up to date**:
   - `Build` → `Rebuild Project`
   - Or enable "Build project" in run configuration

2. **Sources don't match**:
   - `File` → `Invalidate Caches / Restart...`
   - Select "Invalidate and Restart"

3. **Debug instead of Run**:
   - Make sure you click Debug (bug icon), not Run

### Problem: "Module not specified"

**Solution**:
1. Open Run Configuration
2. Set Module: `hitorro-example-springboot`
3. If not available, reimport Maven project:
   - Right-click `pom.xml` → `Maven` → `Reload Project`

## Advanced Debugging

### Debug with Remote JVM

To debug in a separate JVM process:

1. **Start with debug agent**:
   ```bash
   java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
        -DHT_BIN=/Users/chris/hitorro \
        -DHT_HOME=/Users/chris/hthome \
        -jar target/hitorro-example-springboot-*.jar
   ```

2. **Create Remote Debug Configuration**:
   - `Run` → `Edit Configurations...`
   - Click `+` → `Remote JVM Debug`
   - Name: `Remote Spring Boot Debug`
   - Host: `localhost`
   - Port: `5005`
   - Click OK

3. **Attach debugger**:
   - Select "Remote Spring Boot Debug"
   - Click Debug button

### Debug Spring Boot Startup

To debug very early in startup:

1. **Add to VM options**:
   ```
   -Xdebug
   -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005
   ```

2. **Application will wait** for debugger to attach before starting

3. **Attach from IntelliJ** using Remote Debug configuration

### Debug Test Cases

To debug integration tests:

1. **Right-click test class** in Project view
2. **Select "Debug 'TestClassName'"**
3. **Or configure specifically**:
   - `Run` → `Edit Configurations...`
   - `+` → `JUnit`
   - Add same VM options as main configuration

## Recommended Plugins

For better Spring Boot debugging:

- **Spring Boot Assistant** - Enhanced Spring support
- **Spring Tools** - Official Spring plugin
- **Request Mapper** - View all endpoints

Install via: `File` → `Settings` → `Plugins`

## Quick Reference Card

| Action | Shortcut | Description |
|--------|----------|-------------|
| Run | `Shift+F10` | Run selected configuration |
| Debug | `Shift+F9` | Debug selected configuration |
| Toggle Breakpoint | `Ctrl+F8` (Win/Linux)<br>`Cmd+F8` (Mac) | Add/remove breakpoint |
| Step Over | `F8` | Execute current line |
| Step Into | `F7` | Step into method |
| Step Out | `Shift+F8` | Step out of method |
| Resume | `F9` | Continue execution |
| Evaluate | `Alt+F8` | Evaluate expression |
| Show Execution Point | `Alt+F10` | Jump to current line |

## Viewing Configuration Status

Add this component to verify configuration at runtime:

```java
@Component
public class StartupLogger {
    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);
    
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("═══════════════════════════════════════");
        log.info("Hitorro Configuration Status:");
        log.info("  HT_BIN:  {}", System.getProperty("HT_BIN"));
        log.info("  HT_HOME: {}", System.getProperty("HT_HOME"));
        log.info("  Server:  http://localhost:8080");
        log.info("═══════════════════════════════════════");
    }
}
```

This will print configuration on startup and during debug sessions.

## Example Debug Session

Typical debug workflow:

1. **Set breakpoint** in `HitorroExampleApplication.main()` at line 46
2. **Click Debug** (Shift+F9)
3. **Step through** property configuration:
   - Watch `System.getProperty("HT_BIN")` before and after
   - Step into `configureHitorroSystemProperties()`
4. **Resume** to let Spring Boot start (F9)
5. **Set breakpoint** in `JsonTypeSystemManager.afterPropertiesSet()`
6. **Inspect variables**:
   - `htBin` - should show your path
   - `properties.getJvs()` - Spring configuration
7. **Continue debugging** your application logic

## Getting Help

If debugging issues persist:

1. Check IntelliJ logs: `Help` → `Show Log in Finder/Explorer`
2. Check Maven console output
3. Verify project builds: `Build` → `Build Project`
4. Check JDK configuration: `File` → `Project Structure` → `Project`
5. Review `TROUBLESHOOTING.md` for configuration issues
