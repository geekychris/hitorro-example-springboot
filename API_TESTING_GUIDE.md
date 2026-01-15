# API Testing Guide - Hitorro Example

## Overview

This guide covers **two powerful ways** to test and explore the Hitorro Spring Boot APIs:

1. **IntelliJ HTTP Client** - File-based API testing with scripts
2. **Swagger UI** - Interactive browser-based API explorer

## Method 1: IntelliJ HTTP Client

### Quick Start

1. **Open the HTTP file**
   ```
   hitorro-example-springboot/filesystem-api-tests.http
   ```

2. **Run a request**
   - Click the green ▶ icon next to any request
   - Or: Put cursor on a request and press `Alt+Enter` (Windows/Linux) or `Ctrl+Enter` (Mac)
   - Results appear in the Run window

3. **View response**
   - Status code, headers, and body shown in Run window
   - Test assertions run automatically
   - Response can be saved or formatted

### Features

✅ **60+ pre-configured API calls** covering:
- File system status checks
- List directory operations
- File read operations
- File write operations
- Complete workflows
- Error handling tests
- Real-world examples (CSV, logs, config files)

✅ **Variables** for easy configuration:
```http
@baseUrl = http://localhost:8080
@filesystemApi = {{baseUrl}}/api/filesystem
```

✅ **Automated tests** built into requests:
```http
> {%
    client.test("File written successfully", function() {
        client.assert(response.status === 200);
    });
%}
```

✅ **Examples included**:
- Write text files
- Write JSON/CSV/log files
- Read files
- List directories
- Create nested directories
- Handle special characters
- Error scenarios

### Example Requests

#### Check File System Status
```http
GET http://localhost:8080/api/filesystem/status
Accept: application/json
```

#### Write a File
```http
POST http://localhost:8080/api/filesystem/local/write
Content-Type: application/json

{
  "path": "test/hello.txt",
  "content": "Hello World!"
}
```

#### Read a File
```http
GET http://localhost:8080/api/filesystem/local/read/test/hello.txt
Accept: text/plain
```

#### List Files
```http
GET http://localhost:8080/api/filesystem/local/list?path=/test
Accept: application/json
```

### Running Multiple Requests

Execute requests in sequence:
1. Run first request (▶)
2. Wait for completion
3. Run next request (▶)

Or use the "Run all requests in file" option (⚙ menu).

### Tips

- **Comments**: Use `###` for section dividers
- **Variables**: Define at top, use with `{{variable}}`
- **Tests**: Add assertions to verify responses
- **Environments**: Create `.http.env.json` for different configs

## Method 2: Swagger UI (Interactive)

### Access Swagger UI

1. **Start the application**
   ```bash
   cd hitorro-example-springboot
   mvn spring-boot:run
   ```

2. **Open Swagger UI in browser**
   ```
   http://localhost:8080/swagger-ui.html
   ```

### Features

✅ **Interactive API Documentation**
- Browse all endpoints organized by tags
- View request/response schemas
- See example values
- Read detailed descriptions

✅ **Try It Out**
- Click "Try it out" on any endpoint
- Fill in parameters
- Click "Execute"
- See real responses immediately

✅ **Schema Browser**
- View all data models
- See required vs optional fields
- Understand data types
- Copy example JSON

✅ **Export Options**
- Download OpenAPI spec (JSON/YAML)
- Generate client code
- Import into Postman

### Using Swagger UI

#### 1. Browse Endpoints

Navigate the API by tags:
- **File System** - File operations
- **DMS** - Document management (if enabled)
- **Actuator** - Health and metrics

#### 2. Try an Endpoint

Example: Write a file

1. Expand **File System** section
2. Find `POST /api/filesystem/local/write`
3. Click "Try it out"
4. Edit the request body:
   ```json
   {
     "path": "swagger-test.txt",
     "content": "Created via Swagger UI!"
   }
   ```
5. Click "Execute"
6. View response below

#### 3. View Schemas

Click "Schemas" at bottom to see:
- `WriteRequest` - Structure for write operations
- Response models
- Error formats

### Swagger Endpoints

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **OpenAPI YAML**: http://localhost:8080/api-docs.yaml

### Configuration

Configured in `application.yml`:
```yaml
springdoc:
  api-docs:
    path: /api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operationsSorter: method
    tagsSorter: alpha
```

## Comparison

| Feature | IntelliJ HTTP Client | Swagger UI |
|---------|---------------------|------------|
| **Location** | IDE (IntelliJ IDEA) | Browser |
| **Setup** | Open .http file | Open URL |
| **Version Control** | ✅ Git-friendly | ❌ Browser only |
| **Automation** | ✅ Scripts & tests | ❌ Manual |
| **Sharing** | ✅ Commit to repo | ❌ Link only |
| **Documentation** | ⚠️ In comments | ✅ Auto-generated |
| **Interactive** | ⚠️ Run manually | ✅ Form-based |
| **Learning Curve** | Medium | Easy |
| **Best For** | CI/CD, automation | Exploration, demos |

## Recommendation

**Use both!**

- **Swagger UI** - Great for:
  - First-time exploration
  - Demos and presentations
  - Quick testing
  - Understanding API structure

- **IntelliJ HTTP Client** - Great for:
  - Automated testing
  - Version-controlled test suites
  - CI/CD integration
  - Team collaboration

## Additional Tools

### H2 Console

View database contents:
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/hitorrodb
Username: sa
Password: hitorro
```

### Actuator Endpoints

Health and metrics:
- Health: http://localhost:8080/actuator/health
- Info: http://localhost:8080/actuator/info
- Metrics: http://localhost:8080/actuator/metrics

### CLI Access

Telnet interface:
```bash
telnet localhost 5050
```

SSH interface:
```bash
ssh -p 5022 user@localhost
# Password: user
```

## Complete Testing Workflow

### 1. Start Application
```bash
cd hitorro-example-springboot
mvn spring-boot:run
```

### 2. Explore with Swagger
- Open http://localhost:8080/swagger-ui.html
- Browse available endpoints
- Try a few requests interactively

### 3. Test with HTTP Client
- Open `filesystem-api-tests.http` in IntelliJ
- Run requests sequentially
- Verify responses with built-in tests

### 4. Verify Results
- Check files: `ls -la data/files/test/`
- View in H2 Console (for DMS operations)
- Check logs for debugging

### 5. Clean Up
```bash
# Remove test files
rm -rf data/files/test/

# Reset database
rm -rf data/hitorrodb*
```

## Troubleshooting

### Application Not Starting

Check logs for:
- Port 8080 already in use
- Database connection issues
- Missing HT_BIN/HT_HOME configuration

### Swagger UI Not Loading

Verify:
- Application is running
- springdoc dependency in pom.xml
- Configuration in application.yml
- No conflicting Spring Security rules

### HTTP Requests Failing

Check:
- Application is running on http://localhost:8080
- Base URL is correct in .http file
- File system is enabled in application.yml

## Summary

✅ **IntelliJ HTTP Client file created**: `filesystem-api-tests.http`  
✅ **Swagger UI enabled**: http://localhost:8080/swagger-ui.html  
✅ **60+ API test examples** covering all scenarios  
✅ **Interactive documentation** for easy exploration  
✅ **Both tools ready to use** for comprehensive API testing  

Happy testing! 🚀
