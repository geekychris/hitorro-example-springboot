# Hitorro Spring Boot Example Application

This is an example Spring Boot application demonstrating how to use the Hitorro framework within a Spring Boot application using the `hitorro-spring-boot-starter`.

## Features Demonstrated

- ✅ Automatic Hitorro service initialization
- ✅ Accessing Hitorro services via dependency injection
- ✅ REST endpoints for service information
- ✅ Command execution via HTTP (`/api/commands/execute`)
- ✅ Multiple CLI access modes (telnet, SSH, Actuator)
- ✅ Spring Boot Actuator integration
- ✅ Configuration via `application.yml`

## Prerequisites

- Java 21+
- Maven 3.8+
- Hitorro 3.0.0 installed in local Maven repository
- hitorro-spring-boot-starter 1.0.0 installed

## Building the Application

```bash
# From the hitorro-example-springboot directory
mvn clean package

# Or from the hitorro-all root
cd hitorro-all
mvn clean install -pl hitorro-example-springboot -am
```

## Running the Application

```bash
# Run with Maven
mvn spring-boot:run

# Or run the JAR directly
java -jar target/hitorro-example-springboot-1.0.0.jar
```

The application will start on `http://localhost:8080`

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

### Spring Boot Actuator Endpoints

```bash
# Health
curl http://localhost:8080/actuator/health

# Info
curl http://localhost:8080/actuator/info

# All endpoints
curl http://localhost:8080/actuator
```

## CLI Access

### Telnet CLI
```bash
telnet localhost 9000
```

### SSH CLI
```bash
ssh -p 9022 localhost
```

Both provide access to Hitorro's native command-line interface.

## Configuration

Edit `src/main/resources/application.yml` to customize:

- Server port
- Hitorro services configuration
- CLI ports
- DMS settings (if using)
- Logging levels
- Database connection (if using DMS)

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
