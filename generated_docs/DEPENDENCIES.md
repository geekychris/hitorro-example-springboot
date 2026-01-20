# Hitorro Spring Boot Dependencies Guide

## Issue: Missing Class During JSON Type Initialization

**Problem**: During initialization, the JSON Type System attempts to load classes referenced in type definitions via IoC (Inversion of Control) pattern. If classes like `com.hitorro.jsontypesystem.dynamic.POSTokenizer` are not found, initialization fails.

**Root Cause**: Type definitions in `config/types/core_mlselem.json` reference classes from `hitorro-text-core` module, but this module wasn't included in the dependencies.

## Solution: Add Required Dependencies

### For Example Application

Add `hitorro-text-core` to your `pom.xml`:

```xml
<dependencies>
    <!-- Hitorro Spring Boot Starter -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Hitorro DMS (for document management features) -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-basedms</artifactId>
        <version>3.0.0</version>
    </dependency>
    
    <!-- Hitorro Text Core (for NLP and text processing) -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-text-core</artifactId>
        <version>3.0.0</version>
    </dependency>
</dependencies>
```

### For Spring Boot Starter

The starter now includes `hitorro-text-core` as an optional dependency:

```xml
<dependency>
    <groupId>com.hitorro</groupId>
    <artifactId>hitorro-text-core</artifactId>
    <optional>true</optional>
</dependency>
```

This means applications must explicitly include it if they use type definitions requiring text processing features.

## Hitorro Module Overview

### Core Modules

#### hitorro-util (Required)
- **Purpose**: Core utilities, JSON type system, property management
- **Contains**: `JsonTypeSystem`, `JVS`, core utilities
- **Always required**: Yes
- **Included by**: `hitorro-spring-boot-starter`

#### hitorro-base (Required)
- **Purpose**: Base services, networking, RPC
- **Contains**: `BasicService`, command framework, network utilities
- **Always required**: Yes
- **Included by**: `hitorro-spring-boot-starter`

### Optional Modules

#### hitorro-text-core (Optional - Recommended)
- **Purpose**: Text processing, NLP, tokenization
- **Contains**: `POSTokenizer`, text analysis, Lucene integration
- **Required if**: Using type definitions with NLP features
- **Included by**: Starter (optional), must be explicit in application
- **Classes referenced in types**:
  - `com.hitorro.jsontypesystem.dynamic.POSTokenizer`
  - Text analysis components

#### hitorro-basedms (Optional)
- **Purpose**: Document Management System
- **Contains**: `DMSSession`, `Document`, `Content`, versioning
- **Required if**: Using DMS features (documents, content storage, versioning)
- **Included by**: Starter (optional), must be explicit in application

#### hitorro-text-persistence (Optional)
- **Purpose**: Full-text indexing with Lucene
- **Contains**: Lucene indexers, search services
- **Required if**: Using full-text search capabilities
- **Must add explicitly**

#### hitorro-conversation (Optional)
- **Purpose**: Conversation and message processing
- **Required if**: Using chat/messaging features
- **Must add explicitly**

#### hitorro-analysis (Optional)
- **Purpose**: Statistical analysis, LDA, classification
- **Required if**: Using ML/analytics features
- **Must add explicitly**

## Complete Dependency Matrix

| Module | Purpose | Required By | Add Explicitly |
|--------|---------|-------------|----------------|
| `hitorro-util` | Core utilities & JSON types | Always | No (in starter) |
| `hitorro-base` | Base services | Always | No (in starter) |
| `hitorro-text-core` | Text/NLP processing | Type defs with NLP | **Yes** |
| `hitorro-basedms` | Document management | DMS features | **Yes** |
| `hitorro-text-persistence` | Full-text search | Search features | **Yes** |
| `hitorro-conversation` | Messaging | Chat features | **Yes** |
| `hitorro-analysis` | Analytics/ML | Analysis features | **Yes** |

## Type Definition Dependencies

Type definitions in `${HT_BIN}/config/types/core/` may reference classes from various modules:

### Example: core_mlselem.json

```json
{
  "fields": {
    "tokens": {
      "class": "com.hitorro.jsontypesystem.dynamic.POSTokenizer",
      ...
    }
  }
}
```

**Requires**: `hitorro-text-core` (contains `POSTokenizer`)

### How IoC Works

1. **Type Loading**: `JsonTypeSystem` loads type definitions from JSON files
2. **Class References**: JSON may specify `"class": "com.package.ClassName"`
3. **Dynamic Loading**: System attempts `Class.forName("com.package.ClassName")`
4. **Failure**: If class not on classpath → `ClassNotFoundException`

## Troubleshooting Class Not Found

### Error Symptoms

```
ClassNotFoundException: com.hitorro.jsontypesystem.dynamic.POSTokenizer
```

Or during startup:
```
ERROR: Failed to initialize type 'mlselem'
Caused by: class not found
```

### Diagnosis Steps

1. **Identify the missing class package**
   ```
   com.hitorro.jsontypesystem.dynamic.* → hitorro-text-core
   com.hitorro.basetext.* → hitorro-text-core
   com.hitorro.basedms.* → hitorro-basedms
   com.hitorro.analysis.* → hitorro-analysis
   ```

2. **Check your pom.xml**
   ```bash
   mvn dependency:tree | grep hitorro
   ```

3. **Verify module is included**
   ```bash
   # Check if JAR contains the class
   jar tf target/app.jar | grep POSTokenizer
   ```

### Common Fixes

#### Missing POSTokenizer or Text Processing Classes
```xml
<dependency>
    <groupId>com.hitorro</groupId>
    <artifactId>hitorro-text-core</artifactId>
    <version>3.0.0</version>
</dependency>
```

#### Missing DMS Classes
```xml
<dependency>
    <groupId>com.hitorro</groupId>
    <artifactId>hitorro-basedms</artifactId>
    <version>3.0.0</version>
</dependency>
```

#### Missing Indexer Classes
```xml
<dependency>
    <groupId>com.hitorro</groupId>
    <artifactId>hitorro-text-persistence</artifactId>
    <version>3.0.0</version>
</dependency>
```

## Minimal Configuration

### Minimal Setup (No NLP, No DMS)

```xml
<dependencies>
    <!-- Just the starter - basic functionality only -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

**Limitations**: Cannot use type definitions with NLP features, no DMS capabilities

### Standard Setup (Recommended)

```xml
<dependencies>
    <!-- Starter -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Text processing (for NLP type features) -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-text-core</artifactId>
        <version>3.0.0</version>
    </dependency>
    
    <!-- Document management (if needed) -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-basedms</artifactId>
        <version>3.0.0</version>
    </dependency>
</dependencies>
```

**Capabilities**: Full JSON Type System, NLP features, DMS if configured

### Full Setup (All Features)

```xml
<dependencies>
    <!-- Starter -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Text core & persistence -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-text-core</artifactId>
        <version>3.0.0</version>
    </dependency>
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-text-persistence</artifactId>
        <version>3.0.0</version>
    </dependency>
    
    <!-- DMS -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-basedms</artifactId>
        <version>3.0.0</version>
    </dependency>
    
    <!-- Analysis -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-analysis</artifactId>
        <version>3.0.0</version>
    </dependency>
    
    <!-- Conversation -->
    <dependency>
        <groupId>com.hitorro</groupId>
        <artifactId>hitorro-conversation</artifactId>
        <version>3.0.0</version>
    </dependency>
</dependencies>
```

**Capabilities**: Everything - full Hitorro framework

## Conditional Features

### Enable NLP Only If Module Present

```yaml
hitorro:
  jvs:
    enabled: true
    nlp-enabled: true  # Only enable if hitorro-text-core is present
```

The autoconfiguration uses `@ConditionalOnClass` to check:

```java
@ConditionalOnClass(name = "com.hitorro.jsontypesystem.dynamic.POSTokenizer")
@Bean
public NLPIntegrationManager nlpIntegrationManager() {
    // Only created if class is on classpath
}
```

### Enable DMS Only If Module Present

```yaml
hitorro:
  dms:
    enabled: true  # Only works if hitorro-basedms is present
```

## Verification

After adding dependencies, verify:

```bash
# Rebuild
mvn clean package

# Check dependencies
mvn dependency:tree

# Run application
mvn spring-boot:run
```

Look for successful initialization:
```
✓ JsonTypeSystem initialized successfully
✓ POSTokenizer available (if text-core included)
✓ DMS services initialized (if basedms included)
```

## Summary

**Key Points:**
1. ✅ **Always include** `hitorro-spring-boot-starter`
2. ✅ **Add `hitorro-text-core`** if using type definitions with NLP features
3. ✅ **Add `hitorro-basedms`** if using document management
4. ✅ **Add other modules** as needed for specific features
5. ✅ **Check type definitions** to see what classes they reference
6. ✅ **Use `mvn dependency:tree`** to verify modules are included

**Quick Fix for POSTokenizer Issue:**
```xml
<dependency>
    <groupId>com.hitorro</groupId>
    <artifactId>hitorro-text-core</artifactId>
    <version>3.0.0</version>
</dependency>
```
