# JAR FileSystem Demo - Spring Boot Controller

## Overview

The `FileSystemExampleController` has **fully working JAR filesystem endpoints** that demonstrate reading from JAR files.

## Prerequisites

The controller needs a JAR file configured. By default, it looks for the JAR file path from configuration:

```yaml
hitorro:
  filesystem:
    jar:
      enabled: true
      jar-path: ./example-resources.jar
```

## Available Endpoints

### 1. Check Status
```http
GET http://localhost:8080/api/filesystem/status
```

**Response**:
```json
{
  "localFileSystem": "available",
  "jarFileSystem": "available",
  "s3FileSystem": "not configured"
}
```

### 2. List All Files in JAR
```http
GET http://localhost:8080/api/filesystem/jar/list
```

**What it does**:
- Calls `jarFileSystem.listAllEntries()`
- Returns all files in the JAR (not directories)

**Response**:
```json
[
  {
    "file": "test.txt",
    "size": 15,
    "exists": true
  },
  {
    "file": "config.properties",
    "size": 20,
    "exists": true
  },
  {
    "file": "data/data.txt",
    "size": 21,
    "exists": true
  }
]
```

### 3. List Files in Directory
```http
GET http://localhost:8080/api/filesystem/jar/list?path=data
```

**What it does**:
- Calls `jarFileSystem.listDirectory("data")`
- Returns files in specific directory

**Response**:
```json
[
  {
    "file": "data/data.txt",
    "size": 21,
    "exists": true
  }
]
```

### 4. Read File from JAR
```http
GET http://localhost:8080/api/filesystem/jar/read/test.txt
```

**What it does**:
- Calls `jarFileSystem.getFile("test.txt")`
- Opens input stream and reads content
- Returns as plain text

**Response**:
```
Hello from JAR!
```

### 5. Read Nested File
```http
GET http://localhost:8080/api/filesystem/jar/read/data/data.txt
```

**Response**:
```
Data in subdirectory
```

## Controller Implementation

Here's what the controller does:

### List Files Endpoint
```java
@GetMapping("/jar/list")
public ResponseEntity<?> listJarFiles(@RequestParam(defaultValue = "/") String path) {
    if (jarFileSystem == null) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("JAR file system not configured");
    }
    
    try {
        // Use JarFileSystem's list methods
        JarFileFile[] fileArray;
        if (path.equals("/") || path.isEmpty()) {
            fileArray = jarFileSystem.listAllEntries();  // ✅
        } else {
            fileArray = jarFileSystem.listDirectory(path);  // ✅
        }
        
        List<Map<String, Object>> files = new ArrayList<>();
        if (fileArray != null) {
            for (JarFileFile file : fileArray) {
                Map<String, Object> fileInfo = new LinkedHashMap<>();
                fileInfo.put("file", file.toString());
                fileInfo.put("size", file.length());
                fileInfo.put("exists", file.exists());
                files.add(fileInfo);
            }
        }
        
        return ResponseEntity.ok(files);
    } catch (Exception e) {
        logger.error("Error listing JAR files", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error: " + e.getMessage());
    }
}
```

### Read File Endpoint
```java
@GetMapping("/jar/read/{path:.+}")
public ResponseEntity<String> readJarFile(@PathVariable String path) {
    if (jarFileSystem == null) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("JAR file system not configured");
    }
    
    try {
        JarFileFile file = jarFileSystem.getFile(path);  // ✅
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        try (InputStream is = file.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        }
    } catch (Exception e) {
        logger.error("Error reading JAR file", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error: " + e.getMessage());
    }
}
```

## How to Test

### Option 1: Use the provided HTTP test file

The project includes `filesystem-api-tests.http` with pre-configured requests:

```http
### List all files in JAR
GET http://localhost:8080/api/filesystem/jar/list

### Read file from JAR
GET http://localhost:8080/api/filesystem/jar/read/test.txt

### Read nested file
GET http://localhost:8080/api/filesystem/jar/read/data/data.txt
```

**In IntelliJ**: Open the file and click the ▶ icon next to any request.

### Option 2: Use curl

```bash
# Start the app
cd hitorro-example-springboot
mvn spring-boot:run

# In another terminal:

# Check status
curl http://localhost:8080/api/filesystem/status

# List all JAR files
curl http://localhost:8080/api/filesystem/jar/list

# Read file
curl http://localhost:8080/api/filesystem/jar/read/test.txt

# Read nested file
curl http://localhost:8080/api/filesystem/jar/read/data/data.txt
```

### Option 3: Use Swagger UI

1. Start the app: `mvn spring-boot:run`
2. Open browser: http://localhost:8080/swagger-ui.html
3. Navigate to "File System Example Controller"
4. Try the `/api/filesystem/jar/list` and `/api/filesystem/jar/read/{path}` endpoints

## Creating a Test JAR

The tests automatically create a JAR file at `./target/test-resources.jar` with:
- `test.txt` - Contains "Hello from JAR!"
- `config.properties` - Contains properties
- `data/data.txt` - Contains "Data in subdirectory"

To use it manually:
```bash
# Run tests to create the JAR
mvn test

# Copy to root for the app to use
cp target/test-resources.jar ./example-resources.jar

# Update application.yml
hitorro:
  filesystem:
    jar:
      enabled: true
      jar-path: ./example-resources.jar

# Run the app
mvn spring-boot:run
```

## Error Handling

The controller handles all error cases:

### JAR not configured
```
Status: 503 Service Unavailable
Body: "JAR file system not configured"
```

### File not found
```
Status: 404 Not Found
Body: (empty)
```

### Read error
```
Status: 500 Internal Server Error
Body: "Error: <message>"
```

## Test Results

All tests pass:
```
FileSystemControllerSimpleTest:
  ✅ testStatusShowsJarAvailable - Status shows JAR available
  ✅ testListJarFiles - Successfully lists all files
  ✅ testReadJarFile - Successfully reads file content
```

## Status

✅ **JAR filesystem endpoints working**  
✅ **Controller properly using JarFileSystem API**  
✅ **All tests passing**  
✅ **Error handling complete**  
✅ **Swagger documentation available**  

The JAR filesystem is **fully integrated and ready to use**! 🎉
