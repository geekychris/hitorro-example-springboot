# Hitorro Spring Boot Example Application

This is an example Spring Boot application demonstrating how to use the Hitorro framework within a Spring Boot application using the `hitorro-spring-boot-starter`.

## Features Demonstrated

- ✅ Automatic Hitorro service initialization
- ✅ Accessing Hitorro services via dependency injection
- ✅ DMS operations with versioning support
- ✅ JSON Type System with NLP features
- ✅ REST endpoints for documents and queries
- ✅ **Native CLI (telnet/SSH)** - Interactive command line interface
- ✅ Command execution via HTTP (`/api/commands/execute`)
- ✅ **H2 Database with persistent file storage**
- ✅ **H2 Console web UI** for database management
- ✅ Spring Boot Actuator integration

## Prerequisites

- Java 21+
- Maven 3.8+
- Hitorro 3.0.0 installed in local Maven repository
- hitorro-spring-boot-starter 1.0.0 installed

## Dependencies

This example includes:
- `hitorro-spring-boot-starter` - Core Spring Boot integration
- `hitorro-basedms` - Document management system
- `hitorro-text-core` - Text processing & NLP (required for JSON type definitions)

> **Note**: If you see `ClassNotFoundException` for classes like `POSTokenizer`, ensure `hitorro-text-core` is in your dependencies. See [DEPENDENCIES.md](DEPENDENCIES.md) for details.

## 🚀 IntelliJ IDEA Quick Start

**Want to debug right away?** See **[QUICK_START_INTELLIJ.md](QUICK_START_INTELLIJ.md)**

1. Open project in IntelliJ
2. Select **"HitorroExampleSpringBoot"** from run configurations dropdown
3. Click Debug button (🐛) or press `Shift+F9`

That's it! The configuration is pre-configured with all required VM options.

## Building the Application

```bash
# From the hitorro-example-springboot directory
mvn clean package

# Or from the hitorro-all root
cd hitorro-all
mvn clean install -pl hitorro-example-springboot -am
```

## Running the Application

### ⭐ Recommended: Use application.yml (Simplest)

The **easiest way** - just configure once in `application.yml`:

```yaml
hitorro:
  ht-bin: /Users/chris/hitorro      # Your Hitorro installation
  ht-home: /Users/chris/hthome      # Your Hitorro home directory
```

Then run normally - no JVM arguments needed:

```bash
# With Maven
mvn spring-boot:run

# With JAR
java -jar target/hitorro-example-springboot-1.0.0.jar
```

The application will start on `http://localhost:8080`

> **✅ Best Practice**: Configure `hitorro.ht-bin` and `hitorro.ht-home` in `application.yml`. The framework automatically converts these to system properties. See [CONFIGURATION_UPDATED.md](CONFIGURATION_UPDATED.md) for details.

### Alternative Methods

<details>
<summary>Click to see other configuration methods</summary>

#### Option 1: Run Script
```bash
./run.sh
```

#### Option 2: Environment Variables
```bash
export HT_BIN=/Users/chris/hitorro
export HT_HOME=/Users/chris/hthome
mvn spring-boot:run
```

#### Option 3: System Properties  
```bash
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DHT_BIN=/Users/chris/hitorro -DHT_HOME=/Users/chris/hthome"
```

All methods work, but **application.yml is recommended** for simplicity.

</details>

## Available Endpoints

### Example Application Endpoints

#### Get Application Status
```bash
curl http://localhost:8080/api/example/status
```

Returns information about the application and initialized Hitorro services.

#### Get BasicService Information
```bash
curl http://localhost:8080/api/example/basic-service
```

Shows whether BasicService is available and initialized.

#### List All Hitorro Services
```bash
curl http://localhost:8080/api/example/services
```

Lists all initialized Hitorro services with their details.

#### Health Check
```bash
curl http://localhost:8080/api/example/health
```

Simple health check endpoint.

### Hitorro Command Endpoints

#### Execute a Command
```bash
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "help"}'
```

#### List Available Commands
```bash
curl http://localhost:8080/api/commands/list
```

### Native CLI (Telnet/SSH)

**Quick Start**: See **[CLI_QUICK_START.md](CLI_QUICK_START.md)**

```bash
# Connect via telnet
telnet localhost 5050

# Connect via SSH
ssh -p 5022 user@localhost
# Password: user
```

Try commands:
```
HitorroExample> help
HitorroExample> quit          # Exit CLI session
HitorroExample> uptime        # Application uptime
HitorroExample> memory        # Memory usage
HitorroExample> threads       # Thread info
HitorroExample> env.time      # Current time
```

**Note**: All `@CommandDef` annotated methods are automatically discovered and registered! See [COMMANDDEF_ANNOTATION_SUPPORT.md](../COMMANDDEF_ANNOTATION_SUPPORT.md) for details.

### Spring Boot Actuator Endpoints

```bash
# Health
curl http://localhost:8080/actuator/health

# Info
curl http://localhost:8080/actuator/info

# All endpoints
curl http://localhost:8080/actuator
```

## H2 Database Console

The application includes **H2 Console** - a web-based database management tool.

### Quick Access

1. **Start the application**: `mvn spring-boot:run`
2. **Open H2 Console**: `http://localhost:8080/h2-console`
3. **Login**:
   - JDBC URL: `jdbc:h2:file:./data/hitorrodb`
   - Username: `sa`
   - Password: `hitorro`

### Features

- ✅ **Persistent file database** - Data survives restarts
- ✅ **SQL query execution** - Run queries directly in browser
- ✅ **Table browser** - Explore schema and data
- ✅ **Export/Import** - CSV and SQL script support
- ✅ **Visual query builder** - Click-based query construction

**📖 Complete Guide**: See **[H2_DATABASE_GUIDE.md](H2_DATABASE_GUIDE.md)** for:
- Detailed H2 Console usage
- Common SQL queries for Hitorro DMS
- Backup/restore procedures
- Troubleshooting tips
- IntelliJ Database integration

### Database Location

**Files**: `./data/hitorrodb.mv.db`

The database persists between application restarts. To reset:
```bash
rm ./data/hitorrodb.mv.db
```

## CLI Access

### Telnet CLI
```bash
telnet localhost 5050
```

### SSH CLI
```bash
ssh -p 5022 user@localhost
```

Both provide access to Hitorro's native command-line interface.

## Configuration

Edit `src/main/resources/application.yml` to customize:

- Server port
- Hitorro services configuration
- H2 database settings (persistent file storage)
- CLI ports
- DMS settings
- Logging levels

### Important Configuration Notes

**DMS Store Configuration**: By default, Store CSV loading is disabled to avoid initialization errors. Stores are DMS content storage locations that require file system paths. See **[STORE_CONFIGURATION.md](STORE_CONFIGURATION.md)** if you need to:
- Store document content (PDFs, images, etc.)
- Configure content stores with file system paths
- Enable Store CSV initialization

**H2 Database**: The application uses persistent file-based H2 database. See **[H2_DATABASE_GUIDE.md](H2_DATABASE_GUIDE.md)** for complete database management documentation.

## Project Structure

```
hitorro-example-springboot/
├── src/
│   ├── main/
│   │   ├── java/com/hitorro/example/
│   │   │   ├── HitorroExampleApplication.java    # Main application class
│   │   │   └── controller/
│   │   │       └── ExampleController.java        # Example REST controller
│   │   └── resources/
│   │       └── application.yml                   # Configuration
│   └── test/
│       └── java/com/hitorro/example/             # Tests
├── pom.xml                                       # Maven configuration
└── README.md                                     # This file
```

## How It Works

### 1. Spring Boot Auto-Configuration

The `hitorro-spring-boot-starter` automatically:
- Initializes Hitorro's `ServiceContext`
- Registers all Hitorro services as Spring beans
- Sets up command endpoints
- Configures CLI access
- Bridges property systems

### 2. Accessing Hitorro Services

Inject `HitorroServiceFactory` to access Hitorro services:

```java
@RestController
public class MyController {
    
    @Autowired
    private HitorroServiceFactory serviceFactory;
    
    @GetMapping("/my-endpoint")
    public ResponseEntity<?> myEndpoint() {
        // Get a Hitorro service
        BasicService service = serviceFactory.getService(BasicService.class);
        
        // Use the service
        // ...
        
        return ResponseEntity.ok("Success");
    }
}
```

### 3. Using ServiceContext Directly

You can also inject `ServiceContext` directly:

```java
@Service
public class MyService {
    
    @Autowired
    private ServiceContext serviceContext;
    
    public void doSomething() {
        List<ServiceWrapper> services = serviceContext.getServices();
        // Work with services
    }
}
```

### 4. Executing Commands

Commands can be executed via:

1. **REST API**: `POST /api/commands/execute`
2. **Telnet**: Connect to port 9000
3. **SSH**: Connect to port 9022
4. **Actuator**: (if configured)

## Example Usage

### Accessing a Hitorro Service

```java
@Service
public class MyBusinessLogic {
    
    @Autowired
    private HitorroServiceFactory serviceFactory;
    
    public void processData() {
        // Check if service is available
        if (serviceFactory.isServiceAvailable(BasicService.class)) {
            BasicService basicService = serviceFactory.getService(BasicService.class);
            // Use the service
            // basicService.doSomething();
        }
    }
}
```

### Creating Custom Endpoints

```java
@RestController
@RequestMapping("/api/custom")
public class CustomController {
    
    @Autowired
    private HitorroServiceFactory serviceFactory;
    
    @GetMapping("/process")
    public ResponseEntity<?> process() {
        // Use Hitorro services
        // Process data
        // Return results
        return ResponseEntity.ok(results);
    }
}
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=HitorroExampleApplicationTests
```

## Troubleshooting

### Services Not Initializing

Check the logs for initialization errors:
```bash
tail -f logs/application.log
```

Ensure `hitorro.services.enabled=true` in `application.yml`.

### Port Already in Use

Change the server port in `application.yml`:
```yaml
server:
  port: 8081
```

Or CLI ports:
```yaml
hitorro:
  cli:
    telnet-port: 9001
    ssh-port: 9023
```

### Command Execution Fails

Commands are executed via `CommandSession.executeToLog()` which logs output.
Check the logs to see command results.

## Advanced Configuration

### Enabling DMS

If you need to use Hitorro's Document Management System:

1. Set `hitorro.dms.enabled=true` in `application.yml`
2. Configure database connection
3. Inject `DMSSessionFactory` to work with DMS

```java
@Service
public class DocumentService {
    
    @Autowired
    private DMSSessionFactory dmsFactory;
    
    @Transactional
    public void workWithDMS() {
        // DMS session automatically managed
        // Use unified ID system
    }
}
```

### Custom Service Registration

If you have custom Hitorro services:

```java
@Configuration
public class CustomHitorroConfig {
    
    @PostConstruct
    public void registerCustomServices() {
        ServiceContext sc = ServiceContext.getSC();
        sc.addModule("com.example.MyCustomService");
    }
}
```

## Production Considerations

1. **Logging**: Configure appropriate logging levels for production
2. **Security**: Add Spring Security for endpoint protection
3. **Monitoring**: Use Actuator metrics for monitoring
4. **Database**: Configure connection pooling for DMS if used
5. **CLI Access**: Consider disabling or securing CLI ports in production

## Next Steps

- Explore other Hitorro modules (text-core, analysis, etc.)
- Add Spring Security for authentication
- Implement custom commands
- Use DMS for document management
- Add Spring Cloud Config for distributed configuration

## Resources

- [Hitorro Framework](https://github.com/geekychris/hitorro-all)
- [Hitorro Spring Boot Integration](https://github.com/geekychris/hitorro-spring-boot)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

## License

MIT License - Same as Hitorro framework
