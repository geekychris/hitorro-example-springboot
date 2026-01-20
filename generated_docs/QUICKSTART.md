# Quick Start Guide

Get the Hitorro Spring Boot example running in 5 minutes!

## Step 1: Prerequisites

Make sure you have:
- Java 21+ installed
- Maven 3.8+ installed
- Hitorro modules built and installed (`mvn clean install` from hitorro-all root)

## Step 2: Build the Example

```bash
cd hitorro-all/hitorro-example-springboot
mvn clean package
```

## Step 3: Run the Application

```bash
mvn spring-boot:run
```

Or:

```bash
java -jar target/hitorro-example-springboot-1.0.0.jar
```

You should see output like:
```
Initializing Hitorro ServiceContext
Hitorro ServiceContext initialized successfully
Initialized X services
Started HitorroExampleApplication in X seconds
```

## Step 4: Test the Endpoints

### Check Application Status
```bash
curl http://localhost:8080/api/example/status
```

Expected response:
```json
{
  "application": "Hitorro Spring Boot Example",
  "hitorroIntegration": "active",
  "serviceContextState": "Started",
  "initializedServices": ["basic", "rpc", ...],
  "serviceCount": X
}
```

### List Hitorro Services
```bash
curl http://localhost:8080/api/example/services
```

### Execute a Hitorro Command
```bash
curl -X POST http://localhost:8080/api/commands/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "help"}'
```

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

## Step 5: Try the CLI

### Telnet
```bash
telnet localhost 9000
```

Then type commands:
```
help
exit
```

### SSH
```bash
ssh -p 9022 localhost
```

## What's Next?

- Read the full [README.md](README.md) for detailed documentation
- Explore the example controller code
- Create your own REST endpoints
- Add custom Hitorro services
- Customize configuration in `application.yml`

## Common Issues

### Port Already in Use

Change ports in `src/main/resources/application.yml`:
```yaml
server:
  port: 8081  # Change Spring Boot port

hitorro:
  cli:
    telnet-port: 9001  # Change telnet port
    ssh-port: 9023     # Change SSH port
```

### Services Not Initializing

Check logs for errors:
```bash
# Look for initialization errors
grep ERROR target/*.log
```

Ensure all Hitorro dependencies are in local Maven repository:
```bash
# From hitorro-all root
mvn clean install
```

## Success!

If you can hit the endpoints and see JSON responses, you're successfully running Hitorro in Spring Boot!

🎉 You now have a working example of Hitorro integrated with Spring Boot.
