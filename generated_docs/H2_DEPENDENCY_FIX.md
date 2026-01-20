# H2 Dependency Fix - Runtime Scope

## Problem

When trying to run the Spring Boot application with `mvn spring-boot:run`, you may encounter:

```
Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.

Reason: Failed to determine a suitable driver class

Action:
Consider the following:
    If you want an embedded database (H2, HSQL or Derby), please put it on the classpath.
```

## Root Cause

The H2 database dependency was configured with `<scope>test</scope>`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>  <!-- ❌ WRONG: Only available during tests -->
</dependency>
```

This means:
- ✅ H2 is available when running **tests** (`mvn test`)
- ❌ H2 is **NOT** available when running the **application** (`mvn spring-boot:run` or `java -jar`)

## Solution

Change the scope to `runtime`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>  <!-- ✅ CORRECT: Available at runtime -->
</dependency>
```

## Fixed in POM

**File**: `pom.xml`

**Change**:
```xml
<!-- H2 Database - Runtime scope for persistent file database and H2 Console -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Verification

After the fix, verify H2 is included:

```bash
# Rebuild
mvn clean package

# Check H2 is in the JAR
jar tf target/hitorro-example-springboot-1.0.0.jar | grep h2
```

**Expected output**:
```
BOOT-INF/lib/h2-2.2.224.jar
```

## Maven Scopes Explained

Understanding Maven dependency scopes:

| Scope | Compile | Test | Runtime | Included in JAR |
|-------|---------|------|---------|-----------------|
| **compile** | ✅ | ✅ | ✅ | ✅ |
| **runtime** | ❌ | ✅ | ✅ | ✅ |
| **test** | ❌ | ✅ | ❌ | ❌ |
| **provided** | ✅ | ✅ | ❌ | ❌ |

### Why `runtime` for H2?

```xml
<scope>runtime</scope>
```

**Reasons**:
1. ✅ **Not needed at compile time** - Your code doesn't directly import H2 classes
2. ✅ **Needed at runtime** - Spring Boot needs H2 driver to connect to database
3. ✅ **Available for tests** - Tests can also use H2
4. ✅ **Included in JAR** - Packaged with application for deployment

### When to use `test` scope?

Only use `<scope>test</scope>` for libraries **exclusively** used in tests:

```xml
<!-- These are test-only dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>  <!-- ✅ CORRECT -->
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>  <!-- ✅ CORRECT -->
</dependency>
```

## Testing the Fix

### 1. Rebuild the Project

```bash
cd hitorro-example-springboot
mvn clean install
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

**Expected output**:
```
...
2026-01-14 14:00:00.123  INFO ... : H2 console available at '/h2-console'
2026-01-14 14:00:00.456  INFO ... : Started HitorroExampleApplication in 3.456 seconds
```

### 3. Verify H2 Console

Open browser: `http://localhost:8080/h2-console`

Should see login page (not error).

### 4. Run Tests

```bash
mvn test
```

Tests should still pass (they use in-memory H2).

## Alternative: Using Profiles

If you want different databases for different environments:

**pom.xml**:
```xml
<profiles>
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <dependencies>
            <dependency>
                <groupId>com.h2database</groupId>
                <artifactId>h2</artifactId>
                <scope>runtime</scope>
            </dependency>
        </dependencies>
    </profile>
    
    <profile>
        <id>prod</id>
        <dependencies>
            <dependency>
                <groupId>com.mysql</groupId>
                <artifactId>mysql-connector-j</artifactId>
                <scope>runtime</scope>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

**Usage**:
```bash
# Development (H2)
mvn spring-boot:run

# Production (MySQL)
mvn spring-boot:run -Pprod
```

## Common Issues After Fix

### Issue: "Database already exists"

**If you had a test database created**:
```bash
rm -rf ./data
mvn spring-boot:run
```

### Issue: Port 8080 already in use

**Kill existing process**:
```bash
lsof -i :8080
kill -9 <PID>
```

**Or use different port**:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Issue: H2 Console still not accessible

**Check configuration in `application.yml`**:
```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
```

## Impact on Deployment

### Executable JAR

With `runtime` scope, H2 is included in the executable JAR:

```bash
# Build executable JAR
mvn package

# Run standalone
java -jar target/hitorro-example-springboot-1.0.0.jar
```

H2 will be available without additional installation.

### Docker Image

If using Docker, H2 is now part of the application:

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/hitorro-example-springboot-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

No need to install H2 separately.

## Best Practices

### For Development

✅ **DO**:
- Use H2 with `runtime` scope
- Enable H2 Console for debugging
- Use file-based database for persistence

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### For Production

✅ **DO**:
- Use production database (PostgreSQL, MySQL)
- Set H2 with `provided` scope (or remove)
- Disable H2 Console

```xml
<!-- Production database -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- H2 only for local dev -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>provided</scope>
</dependency>
```

## Summary

### Before (Broken)
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>  <!-- ❌ Only works in tests -->
</dependency>
```

**Problems**:
- ❌ `mvn spring-boot:run` fails - no datasource
- ❌ `java -jar app.jar` fails - H2 not included
- ✅ `mvn test` works - H2 available for tests

### After (Fixed)
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>  <!-- ✅ Works everywhere -->
</dependency>
```

**Results**:
- ✅ `mvn spring-boot:run` works
- ✅ `java -jar app.jar` works
- ✅ `mvn test` works
- ✅ H2 Console accessible
- ✅ Database persistence works

## Quick Fix Checklist

- [ ] Update `pom.xml` - change scope to `runtime`
- [ ] Run `mvn clean install`
- [ ] Start app: `mvn spring-boot:run`
- [ ] Verify: `http://localhost:8080/h2-console`
- [ ] Run tests: `mvn test`

## References

- [Maven Dependency Scopes](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope)
- [Spring Boot with H2](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.h2-web-console)
- [H2 Database Documentation](http://www.h2database.com/html/main.html)

---

**Fix applied and verified!** The application now runs correctly with H2 database and console. ✅
