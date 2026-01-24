# IntelliJ IDEA Debug Guide - Fixing SLF4J Conflicts

## Problem

When running tests in IntelliJ debugger, you see this error:

```
SLF4J(W): Class path contains multiple SLF4J providers.
SLF4J(W): Found provider [org.slf4j.reload4j.Reload4jServiceProvider]
SLF4J(W): Found provider [ch.qos.logback.classic.spi.LogbackServiceProvider]

java.lang.IllegalArgumentException: LoggerFactory is not a Logback LoggerContext but Logback is on the classpath.
```

## Root Cause

IntelliJ is building the test classpath directly and **not applying Maven dependency exclusions** from the POM.

When you run tests via Maven (`mvn test`), the exclusions work correctly. But IntelliJ's default behavior is to use its own dependency resolution, which may not respect exclusions immediately after POM changes.

## Solution: Reload Maven Project in IntelliJ

### Method 1: Reload via Maven Tool Window (Recommended)

1. **Open Maven Tool Window**
   - View → Tool Windows → Maven
   - Or click the "Maven" tab on the right side of IntelliJ

2. **Reload Project**
   - Click the **🔄 Reload All Maven Projects** button (top-left of Maven tool window)
   - Or right-click on the project → Maven → Reload Project

3. **Wait for Reload**
   - IntelliJ will re-import the project and rebuild the dependency tree
   - Watch the progress bar at the bottom

4. **Rebuild Project** (optional but recommended)
   - Build → Rebuild Project
   - This ensures IntelliJ's caches are cleared

### Method 2: Reimport via File Menu

1. **File → Invalidate Caches...**
   - Select "Invalidate and Restart"
   - This clears all IntelliJ caches

2. **After Restart**
   - File → New → Project from Existing Sources
   - Select the project root directory
   - Follow the import wizard

### Method 3: Command Line Rebuild + Reload

```bash
# In terminal
cd /Users/chris/hitorro/hitorro-example-springboot

# Clean and rebuild with Maven
mvn clean install -DskipTests

# Then reload in IntelliJ (Method 1)
```

## Verification Steps

### 1. Check External Libraries

After reloading, verify the dependencies:

1. **Open Project Structure**
   - File → Project Structure (⌘;)
   - Go to: Modules → hitorro-example-springboot → Dependencies

2. **Verify Exclusions Applied**
   - Look for these libraries in the list
   - They should **NOT** be present:
     - ❌ `log4j:log4j:1.2.17`
     - ❌ `slf4j-reload4j:2.0.11`
   - Only Logback should be present:
     - ✅ `ch.qos.logback:logback-classic`
     - ✅ `ch.qos.logback:logback-core`

### 2. Run a Test

1. **Open Test File**
   - `src/test/java/com/hitorro/example/springboot/HitorroJVSIntegrationTest.java`

2. **Right-Click on Test Class**
   - Select "Debug 'HitorroJVSIntegrationTest'"

3. **Check Console Output**
   - Should NOT see: "SLF4J(W): Class path contains multiple SLF4J providers"
   - Should see: "✓ JVSProperties initialized..." and other Hitorro logs

## Alternative: Use Maven Run Configuration

If reloading doesn't work, you can run tests through Maven directly from IntelliJ:

### Create Maven Run Configuration

1. **Run → Edit Configurations...**

2. **Click "+" → Maven**

3. **Configure**:
   - **Name**: `Test (Maven)`
   - **Working directory**: `/Users/chris/hitorro/hitorro-example-springboot`
   - **Command line**: `test -Dtest=HitorroJVSIntegrationTest`
   - **Runner**: Use Maven Wrapper if available, or bundled Maven

4. **Apply and Run**
   - This will execute tests through Maven (which respects exclusions)
   - Debug mode works too

## Understanding the Exclusions

The `pom.xml` now contains exclusions for all Hitorro dependencies:

```xml
<dependency>
    <groupId>com.hitorro</groupId>
    <artifactId>hitorro-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <exclusions>
        <exclusion>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
        </exclusion>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-reload4j</artifactId>
        </exclusion>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

These exclusions tell Maven: "Don't include these transitive dependencies from Hitorro modules."

## Why This Happens

### Maven Command Line
- Maven reads `pom.xml`
- Applies exclusions when building dependency tree
- Only includes non-excluded JARs in classpath
- ✅ Tests run successfully

### IntelliJ IDEA
- Caches dependency resolution for performance
- May not immediately pick up POM changes
- Uses its own dependency resolver (may differ from Maven)
- Needs explicit reload to sync with `pom.xml`

## Quick Checklist

- [ ] Made POM changes (added exclusions)
- [ ] Saved `pom.xml`
- [ ] Clicked **🔄 Reload All Maven Projects** in Maven tool window
- [ ] Waited for reload to complete
- [ ] Optional: Build → Rebuild Project
- [ ] Run test in debug mode
- [ ] Verify no SLF4J warnings in console

## If Still Not Working

### 1. Check IntelliJ Settings

**File → Settings → Build, Execution, Deployment → Build Tools → Maven**

Ensure:
- ☑ "Use Maven output directories" is checked
- ☑ "Delegate IDE build/run actions to Maven" is checked (optional, forces Maven execution)

### 2. Delete IntelliJ Files and Re-import

```bash
# Close IntelliJ
cd /Users/chris/hitorro/hitorro-example-springboot

# Delete IntelliJ files
rm -rf .idea
rm *.iml

# Delete target
mvn clean

# Reopen in IntelliJ
# IntelliJ will auto-detect Maven project and import
```

### 3. Use VM Options Override

As a temporary workaround, you can force SLF4J to use Logback:

1. **Edit Test Run Configuration**
   - Run → Edit Configurations...
   - Select your test configuration

2. **Add VM Options**:
   ```
   -Dlogback.configurationFile=src/test/resources/logback-test.xml
   -Dorg.slf4j.simpleLogger.defaultLogLevel=info
   ```

3. **Create logback-test.xml** if needed:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
    
    <logger name="com.hitorro" level="DEBUG"/>
</configuration>
```

## Success Indicators

When everything is working, you should see:

```
2026-01-14T13:14:28.722  INFO ... : ✓ HT_BIN already configured: /Users/chris/hitorro
2026-01-14T13:14:28.722  INFO ... : ✓ HT_HOME configured: /Users/chris/hthome
2026-01-14T13:14:28.750  INFO ... : ✓ JVSProperties initialized with 8 system args
2026-01-14T13:14:28.800  INFO ... : ✓ JsonTypeSystem initialized successfully
```

**No SLF4J warnings!** ✅

## Summary

The fix we applied to `pom.xml` works perfectly with Maven command line, but **IntelliJ needs to be reloaded** to pick up the new exclusions.

**Quick Fix**: 
1. Click 🔄 in Maven tool window
2. Wait for reload
3. Run/Debug test again

This is a common issue when modifying Maven dependencies in IntelliJ - always reload after POM changes!
