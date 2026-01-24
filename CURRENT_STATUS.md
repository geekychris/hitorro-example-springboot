# ✅ Hitorro Application - Current Status

## 🟢 Application is RUNNING Locally!

The Hitorro application is currently **running on your local machine** (not in Docker).

### Access Your Application

| Service | URL | Status |
|---------|-----|--------|
| **Main Application** | http://localhost:8090 | ✅ Running |
| **Swagger API Docs** | http://localhost:8090/swagger-ui.html | ✅ Available |
| **H2 Database Console** | http://localhost:8090/h2-console | ✅ Available |
| **Actuator Health** | http://localhost:8090/actuator/health | ✅ UP |
| **REST API** | http://localhost:8090/api/rest | ✅ Available |
| **Telnet CLI** | `telnet localhost 6000` | ✅ Port 6000 |
| **SSH CLI** | `ssh -p 6022 localhost` | ✅ Port 6022 |

### 🎯 Click These Links

**Start here:** http://localhost:8090/swagger-ui.html

This will show you all available REST APIs you can test!

## Port Configuration

As we discussed, using the **6000 port range** to avoid MinIO conflicts:

- **HTTP**: Port **8090** (instead of 8080 to avoid Trino conflict)
- **Telnet CLI**: Port **6000** (instead of 9000 which MinIO uses)
- **SSH CLI**: Port **6022** (instead of 9022)

## Why Not Docker?

The Docker container has a startup issue with the `ClusterService` component:

```
java.lang.NullPointerException: Cannot invoke "com.hitorro.jsontypesystem.Type.getName()" 
because "type" is null at com.hitorro.network.rpc.cluster.IDef.getInstanceStub
```

This is a type system initialization error that occurs in Docker but not when running locally. The local run works perfectly!

## How It Was Started

The application is running via Maven:

```bash
cd /Users/chris/hitorro/hitorro-example-springboot
mvn spring-boot:run -Dspring-boot.run.profiles=docker \
  -Dspring-boot.run.jvmArguments="-Dserver.port=8090 \
  -Dhitorro.cli.telnet-port=6000 \
  -Dhitorro.cli.ssh-port=6022"
```

Process ID: 67180

## How to Stop/Restart

### Stop the Application

```bash
# Find the process
ps aux | grep spring-boot:run | grep -v grep

# Kill it (replace PID with actual process ID)
kill 67180
```

### Start Again

```bash
cd /Users/chris/hitorro/hitorro-example-springboot
mvn spring-boot:run -Dspring-boot.run.profiles=docker \
  -Dspring-boot.run.jvmArguments="-Dserver.port=8090 \
  -Dhitorro.cli.telnet-port=6000 \
  -Dhitorro.cli.ssh-port=6022" &
```

## What to Explore

### 1. **Swagger UI** (Recommended!)
http://localhost:8090/swagger-ui.html

- See all REST endpoints
- Try uploading documents
- Test DMS operations
- Try content transformations

### 2. **Document Management REST API**
http://localhost:8090/api/rest/dms/documents

- GET - List all documents
- POST - Upload new document
- PUT - Update document
- DELETE - Delete document

### 3. **Transformer REST API**
http://localhost:8090/api/rest/transformer/transform

- Convert documents to different formats
- PDF, DOCX, HTML, TEXT, etc.

### 4. **File System REST API**
http://localhost:8090/api/rest/filesystem

- Browse file stores
- Upload/download files
- Manage directories

### 5. **H2 Database Console**
http://localhost:8090/h2-console

**Connection settings:**
- JDBC URL: `jdbc:h2:file:~/hthome/data/hitorrodb`
- Username: `sa`
- Password: `hitorro`

### 6. **CLI Access**

**Telnet** (Port 6000):
```bash
telnet localhost 6000
```

**SSH** (Port 6022):
```bash
ssh -p 6022 localhost
# Password: (configured in application.yml)
```

## Testing the API

### Upload a Document

```bash
curl -X POST http://localhost:8090/api/rest/dms/documents \
  -F "file=@/path/to/your/document.pdf" \
  -F "name=Test Document" \
  -F "description=Testing upload"
```

### List Documents

```bash
curl http://localhost:8090/api/rest/dms/documents | jq
```

### Get Document by ID

```bash
curl http://localhost:8090/api/rest/dms/documents/1 | jq
```

### Transform a Document

```bash
curl -X POST http://localhost:8090/api/rest/transformer/transform \
  -F "file=@document.docx" \
  -F "targetFormat=pdf" \
  --output result.pdf
```

## Health Check

```bash
curl http://localhost:8090/actuator/health | jq
```

Should return:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

## React UI

The React UI frontend files are ready in the `frontend/` directory, but they need to be built and served. To use the React UI:

```bash
# Build the React UI
cd frontend
npm install
npm run build

# The build output goes to frontend/dist/
# For production, serve these static files or use docker-compose-with-ui.yml
```

## Docker Status

The Docker image is built but the container crashes on startup due to the ClusterService initialization issue. We can fix this by:

1. ✅ **Current solution**: Running locally (works perfectly!)
2. 🔧 **Future fix**: Disable ClusterService properly in Docker or fix the type initialization order

## Summary

✅ **Application Status**: Running locally on port 8090  
✅ **Database**: H2 file-based, persisted in `~/hthome/data/`  
✅ **All APIs**: Available and functional  
✅ **CLI Ports**: Telnet (6000), SSH (6022) - no MinIO conflicts!  
✅ **Process**: Running as PID 67180  
🔧 **Docker**: Built but crashes (use local for now)  

## Next Steps

1. **Explore Swagger**: http://localhost:8090/swagger-ui.html
2. **Upload a test document**
3. **Try content transformation**
4. **Check the H2 database**
5. **Test CLI access via Telnet/SSH**

---

**Last Updated**: Just now  
**Status**: ✅ Running and ready to use!
