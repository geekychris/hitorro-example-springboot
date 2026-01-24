# Setup Summary - HT_BIN and HT_HOME Configuration

## Problem

The `hitorro-example-springboot` application was not initializing the JSON Type System properly because the required system properties `HT_BIN` and `HT_HOME` were not being configured.

## Solution

Multiple fixes have been implemented to ensure proper configuration:

### 1. Application Class Updates

**File**: `src/main/java/com/hitorro/example/HitorroExampleApplication.java`

Added automatic configuration of system properties in the `main()` method:
- Sets `HT_BIN` if not already configured (defaults to `/Users/chris/hitorro`)
- Sets `HT_HOME` if not already configured (defaults to `/Users/chris/hthome`)
- Checks environment variables first before falling back to defaults

### 2. Application Configuration

**File**: `src/main/resources/application.yml`

Added JVS configuration section:
```yaml
hitorro:
  jvs:
    enabled: true
    nlp-enabled: false
    type-definitions-path: ${HT_BIN:/Users/chris/hitorro}
```

This configures the JSON Type System to use the proper type definitions path.

### 3. Run Script

**File**: `run.sh` (new)

Created a convenient shell script that:
- Automatically sets `HT_BIN` and `HT_HOME` from environment or defaults
- Verifies required directories exist
- Builds the application if needed
- Runs with all required JVM arguments
- Provides helpful status messages

**Usage**:
```bash
./run.sh              # Run with defaults
./run.sh --build      # Rebuild before running
```

### 4. IntelliJ Run Configuration

**File**: `idea/runConfigurations/HitorroExampleSpringBoot.run.xml` (new)

Created IntelliJ IDEA run configuration with proper VM options:
```
-server 
-DHT_BIN=$PROJECT_DIR$/ 
-DHT_HOME="$PROJECT_DIR$/../hthome" 
-Xmx2010M 
--add-opens java.base/java.lang=ALL-UNNAMED
```

### 5. Documentation

**Files**: `CONFIGURATION.md` (new), `README.md` (updated)

- Comprehensive configuration guide explaining all methods
- Troubleshooting section
- Production deployment guidance
- Updated README with quick start instructions

## Required Directory Structure

The system expects this structure:

```
/Users/chris/hitorro/              # HT_BIN
├── config/
│   ├── types/
│   │   └── core/                  # JSON type definitions
│   └── services/                  # Service configurations
└── ...

/Users/chris/hthome/               # HT_HOME
├── logs/
├── data/
└── cache/
```

## How to Use

### Method 1: Quick Start (Recommended)
```bash
cd hitorro-example-springboot
./run.sh
```

### Method 2: IntelliJ IDEA
1. Open project in IntelliJ
2. Select "HitorroExampleSpringBoot" run configuration
3. Click Run

### Method 3: Maven with Environment
```bash
export HT_BIN=/Users/chris/hitorro
export HT_HOME=/Users/chris/hthome
mvn spring-boot:run
```

### Method 4: JAR with System Properties
```bash
java -DHT_BIN=/Users/chris/hitorro \
     -DHT_HOME=/Users/chris/hthome \
     -Xmx2010M \
     --add-opens java.base/java.lang=ALL-UNNAMED \
     -jar target/hitorro-example-springboot-*.jar
```

## Verification

After starting the application, check the logs for:

```
INFO  JsonTypeSystemManager : HT_BIN already configured: /Users/chris/hitorro
INFO  JsonTypeSystemManager : JsonTypeSystem initialized successfully
INFO  JsonTypeSystemManager : Type definitions path: /Users/chris/hitorro/config/types/core/
```

## Configuration Priority

The system checks for configuration in this order:

1. **System Property**: `-DHT_BIN=...`
2. **Environment Variable**: `export HT_BIN=...`
3. **Spring Configuration**: `hitorro.jvs.type-definitions-path` (JVS only)
4. **Application Default**: Hardcoded in `HitorroExampleApplication.java`

## Related Files Changed

- ✅ `HitorroExampleApplication.java` - Added system property initialization
- ✅ `application.yml` - Added JVS configuration
- ✅ `run.sh` - New convenient run script
- ✅ `idea/runConfigurations/HitorroExampleSpringBoot.run.xml` - New IntelliJ config
- ✅ `CONFIGURATION.md` - New comprehensive configuration guide
- ✅ `README.md` - Updated with new run instructions

## Notes

- The defaults (`/Users/chris/hitorro`, `/Users/chris/hthome`) are development-specific
- For production, override via environment variables or system properties
- The `--add-opens` JVM flag is required for Hitorro's reflection operations
- Type definitions directory must exist for JVS to work properly
