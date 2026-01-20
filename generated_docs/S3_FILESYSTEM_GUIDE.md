# S3 FileSystem Controller Guide

## Overview

The `FileSystemExampleController` now includes **complete S3-compatible storage endpoints** that work with MinIO, AWS S3, Wasabi, DigitalOcean Spaces, and any S3-compatible storage.

## Available S3 Endpoints

### 1. List Files
```http
GET /api/filesystem/s3/list?path=/
```

Lists files in S3 bucket at the specified path.

**Parameters**:
- `path` (optional) - Directory path, default: "/"

**Response**:
```json
[
  {
    "name": "test.txt",
    "path": "test.txt",
    "size": 1234,
    "exists": true
  }
]
```

### 2. Read File
```http
GET /api/filesystem/s3/read/{path}
```

Reads file content from S3 and returns as text.

**Example**:
```http
GET /api/filesystem/s3/read/documents/report.txt
```

**Response**:
```
File content as text...
```

### 3. Write File
```http
POST /api/filesystem/s3/write
Content-Type: application/json

{
  "path": "documents/report.txt",
  "content": "Report content here..."
}
```

Writes a text file to S3.

**Response**:
```
File written successfully to S3: documents/report.txt
```

## Configuration

### Step 1: Enable S3 in application.yml

```yaml
hitorro:
  filesystem:
    s3:
      enabled: true
      endpoint: http://localhost:9000  # MinIO or S3-compatible endpoint
      bucket: test
      access-key: minioadmin
      secret-key: minioadmin
      ssl-enabled: false  # true for AWS S3
```

### Step 2: Start MinIO (for local testing)

```bash
docker run -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  quay.io/minio/minio server /data --console-address ":9001"
```

**MinIO Console**: http://localhost:9001
- Username: minioadmin
- Password: minioadmin

### Step 3: Create Bucket

Via MinIO Console:
1. Open http://localhost:9001
2. Login with credentials
3. Click "Create Bucket"
4. Enter name: "test"
5. Click "Create"

Or via mc command:
```bash
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/test
```

### Step 4: Start Application

```bash
cd hitorro-example-springboot
mvn spring-boot:run
```

## Testing

### Option 1: IntelliJ HTTP Client

Open `filesystem-api-tests.http` and run the S3 requests:

```http
### List S3 Files
GET http://localhost:8080/api/filesystem/s3/list

### Write File
POST http://localhost:8080/api/filesystem/s3/write
Content-Type: application/json

{
  "path": "test.txt",
  "content": "Hello S3!"
}

### Read File
GET http://localhost:8080/api/filesystem/s3/read/test.txt
```

### Option 2: curl Commands

```bash
# List files
curl http://localhost:8080/api/filesystem/s3/list | jq

# Write file
curl -X POST http://localhost:8080/api/filesystem/s3/write \
  -H "Content-Type: application/json" \
  -d '{"path":"test.txt","content":"Hello S3!"}'

# Read file
curl http://localhost:8080/api/filesystem/s3/read/test.txt

# Write JSON file
curl -X POST http://localhost:8080/api/filesystem/s3/write \
  -H "Content-Type: application/json" \
  -d '{"path":"config.json","content":"{\"version\":\"1.0\"}"}'
```

### Option 3: Swagger UI

1. Open http://localhost:8080/swagger-ui.html
2. Navigate to "File System" section
3. Try the `/api/filesystem/s3/*` endpoints

## Use Cases

### Store Configuration Files
```bash
curl -X POST http://localhost:8080/api/filesystem/s3/write \
  -H "Content-Type: application/json" \
  -d '{
    "path": "config/application.properties",
    "content": "server.port=8080\nspring.datasource.url=..."
  }'
```

### Store Log Files
```bash
curl -X POST http://localhost:8080/api/filesystem/s3/write \
  -H "Content-Type: application/json" \
  -d '{
    "path": "logs/app-2026-01-14.log",
    "content": "2026-01-14 INFO Application started\n..."
  }'
```

### Retrieve Reports
```bash
curl http://localhost:8080/api/filesystem/s3/read/reports/monthly-2026-01.txt
```

## S3-Compatible Services

The controller works with any S3-compatible storage:

### AWS S3
```yaml
hitorro:
  filesystem:
    s3:
      enabled: true
      endpoint: https://s3.amazonaws.com
      bucket: my-bucket
      access-key: AKIA...
      secret-key: ...
      ssl-enabled: true
```

### Wasabi
```yaml
hitorro:
  filesystem:
    s3:
      enabled: true
      endpoint: https://s3.wasabisys.com
      bucket: my-bucket
      access-key: ...
      secret-key: ...
      ssl-enabled: true
```

### DigitalOcean Spaces
```yaml
hitorro:
  filesystem:
    s3:
      enabled: true
      endpoint: https://nyc3.digitaloceanspaces.com
      bucket: my-space
      access-key: ...
      secret-key: ...
      ssl-enabled: true
```

### Backblaze B2
```yaml
hitorro:
  filesystem:
    s3:
      enabled: true
      endpoint: https://s3.us-west-002.backblazeb2.com
      bucket: my-bucket
      access-key: ...
      secret-key: ...
      ssl-enabled: true
```

## Error Handling

### S3 Not Configured
```
Status: 503 Service Unavailable
Body: "S3 file system not configured"
```

### File Not Found
```
Status: 404 Not Found
```

### S3 Error (permissions, network, etc.)
```
Status: 500 Internal Server Error
Body: "Error: <details>"
```

## Controller Implementation

The controller uses the `S3CompatibleFileSystem` class which supports:

- ✅ Any S3-compatible endpoint
- ✅ Custom SSL configuration
- ✅ Path-style access (required for MinIO)
- ✅ Full BaseFile API integration
- ✅ Proper error handling

## Integration with Hitorro BaseFile API

The S3 filesystem integrates seamlessly with Hitorro's abstract filesystem:

```java
// Same API as local or JAR filesystem!
BaseFile file = s3FileSystem.getFile("documents/report.txt");

// Write
try (OutputStream os = file.getOutputStream()) {
    os.write(content.getBytes());
}

// Read
try (InputStream is = file.getInputStream()) {
    String content = new String(is.readAllBytes());
}

// List
BaseFile[] files = directory.listFiles();
```

## Status

✅ **S3 endpoints implemented** - List, Read, Write  
✅ **OpenAPI documentation** - Swagger UI support  
✅ **HTTP test file updated** - IntelliJ HTTP Client  
✅ **Works with all S3-compatible storage**  
✅ **Error handling complete**  
✅ **Ready for production use**  

## Next Steps

1. Configure your S3-compatible storage in `application.yml`
2. Start the application
3. Test with the provided HTTP requests or Swagger UI
4. Integrate into your workflows

The S3 filesystem is **fully integrated and ready to use**! 🎉
