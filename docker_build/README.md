# Docker Build Scripts

This directory contains scripts to build and run the Hitorro application in Docker.

## Quick Start

The easiest way to get started:

```bash
./run-port-6000.sh
```

This will:
1. Build the Docker image if needed (10-15 min first time)
2. Start the container with port 6000 range (avoids MinIO conflict)
3. Wait for application to be healthy
4. Show all access URLs

## Port Mappings

The default configuration uses these ports:

| Service | Port | Container Port |
|---------|------|----------------|
| HTTP (Web UI) | 8080 | 8080 |
| Telnet CLI | 6000 | 9000 |
| SSH CLI | 6022 | 9022 |

**Why 6000?** Standard ports 9000-9001 may conflict with MinIO or other services.

## Available Scripts

### Main Scripts

- **`run-port-6000.sh`** - Recommended: Run with 6000 port range ⭐
- **`build-and-start.sh`** - Build and start in one command
- **`build-all-modules.sh`** - Local Maven build (for testing)

### Individual Operations

- **`build-backend.sh`** - Build backend-only image
- **`build-ui.sh`** - Build with React UI
- **`start.sh`** - Start container
- **`stop.sh`** - Stop container
- **`clean.sh`** - Clean up Docker resources
- **`diagnose.sh`** - Run diagnostics

### Advanced

- **`hitorro.sh`** - Master control script (15 commands)
- **`run-container.sh`** - Run with standard ports (if no conflicts)

## Documentation

- **`README_PORT_6000.md`** - Quick start with 6000 ports
- **`FINAL_INSTRUCTIONS.md`** - Complete setup guide
- **`BUILD_SUCCESS.md`** - Deployment and configuration
- **`COMPLETE.md`** - Full feature list and architecture
- **`TROUBLESHOOTING.md`** - Common issues and solutions

## Access Points

Once running:

- **React UI**: http://localhost:8080
- **Swagger API**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
- **Actuator**: http://localhost:8080/actuator
- **Telnet**: `telnet localhost 6000`
- **SSH**: `ssh -p 6022 localhost`

## Manual Commands

### Build

```bash
cd /Users/chris/hitorro/hitorro-example-springboot
docker build -f Dockerfile-with-ui -t hitorro-app:latest ..
```

### Run

```bash
docker run -d \
  --name hitorro-app \
  -p 8080:8080 \
  -p 6000:9000 \
  -p 6022:9022 \
  -v hitorro-data:/var/lib/hitorro/data \
  hitorro-app:latest
```

### Check Status

```bash
# Container status
docker ps | grep hitorro

# Logs
docker logs -f hitorro-app

# Health
curl http://localhost:8080/actuator/health
```

## What's Included

The Docker image contains:

- ✅ All 19 Hitorro modules (compiled from source)
- ✅ Spring Boot application with auto-configuration
- ✅ React UI with Material Design
- ✅ REST API with Swagger documentation
- ✅ H2 database (with PostgreSQL support)
- ✅ LibreOffice for document transformation
- ✅ CLI access (Telnet & SSH)

## Build Time

- **First build**: 10-15 minutes (downloads dependencies, compiles all modules)
- **Cached rebuild**: 2-3 minutes (uses Docker layer cache)

## Volumes

Data is persisted in Docker volumes:

- `hitorro-data` - Database files
- `hitorro-files` - Uploaded documents
- `hitorro-logs` - Application logs

## Environment Variables

Customize behavior with:

```bash
-e JAVA_OPTS="-Xmx4g -XX:+UseG1GC"
-e SPRING_PROFILES_ACTIVE=docker
-e HITORRO_SERVICES_DB_INIT=false
```

## Support

For issues or questions, see:
- `TROUBLESHOOTING.md` - Common problems
- `FINAL_INSTRUCTIONS.md` - Complete guide
- Docker logs: `docker logs hitorro-app`
