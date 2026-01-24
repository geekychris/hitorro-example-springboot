# ✅ Final Instructions - Run with 6000 Port Range

## Current Status

The Docker image is being built. This takes **10-15 minutes** on first build (uses cached layers after that).

## Once Build Completes

Run the application with:

```bash
cd /Users/chris/hitorro/hitorro-example-springboot/docker_build
./run-port-6000.sh
```

This will:
1. Check if image exists (build if needed)
2. Stop any old container
3. Start new container with **6000 port range**
4. Wait for application to become healthy
5. Show you all access URLs

## Port Mappings (6000 Range - Avoids MinIO Conflict)

| Service | External Port | Internal Port | Access |
|---------|--------------|---------------|---------|
| **HTTP** | 8080 | 8080 | http://localhost:8080 |
| **Telnet CLI** | 6000 | 9000 | `telnet localhost 6000` |
| **SSH CLI** | 6022 | 9022 | `ssh -p 6022 localhost` |

## Access Points

Once running, access:

- **React UI**: http://localhost:8080
- **Swagger API**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:/var/lib/hitorro/data/hitorrodb`
  - Username: `sa`
  - Password: (empty)
- **Actuator**: http://localhost:8080/actuator
- **REST API**: http://localhost:8080/api/rest/*

### CLI Access (New Ports)

```bash
# Telnet CLI on port 6000
telnet localhost 6000

# SSH CLI on port 6022
ssh -p 6022 localhost
# (No authentication required in development mode)
```

## Manual Build Command

If you prefer to build manually:

```bash
cd /Users/chris/hitorro/hitorro-example-springboot

# Build the image
DOCKER_BUILDKIT=0 docker build \
  -f Dockerfile-with-ui \
  -t hitorro-app:latest \
  ..

# Run the container
docker run -d \
  --name hitorro-app \
  -p 8080:8080 \
  -p 6000:9000 \
  -p 6022:9022 \
  -v hitorro-data:/var/lib/hitorro/data \
  -v hitorro-files:/opt/hitorro-app/data/files \
  -v hitorro-logs:/var/lib/hitorro/logs \
  -e JAVA_OPTS="-Xmx2g -XX:+UseG1GC" \
  -e SPRING_PROFILES_ACTIVE=docker \
  hitorro-app:latest
```

## Check Build Progress

```bash
# Watch Docker build (if running in another terminal)
docker ps | grep -i build

# Check if image is ready
docker images | grep hitorro-app
```

## Troubleshooting

### Build Taking Long?
- **First build**: 10-15 minutes is normal (downloads dependencies, compiles all modules)
- **Subsequent builds**: 2-3 minutes (uses Docker cache)

### Container Exits Immediately?
```bash
# Check logs
docker logs hitorro-app

# Common fix: ensure data directory exists
docker volume inspect hitorro-data
```

### Port Still Conflicting?
```bash
# Check what's using ports
lsof -i :8080
lsof -i :6000
lsof -i :6022

# Use different ports if needed
docker run -d --name hitorro-app \
  -p 8081:8080 \
  -p 7000:9000 \
  -p 7022:9022 \
  hitorro-app:latest
```

### Can't Connect After Starting?
```bash
# Wait 30-60 seconds for startup
sleep 30
curl http://localhost:8080/actuator/health

# Check if running
docker ps | grep hitorro

# View startup logs
docker logs -f hitorro-app
```

## Stop/Restart

```bash
# Stop
docker stop hitorro-app

# Start again
docker start hitorro-app

# Restart
docker restart hitorro-app

# Remove and recreate
docker rm -f hitorro-app
./run-port-6000.sh
```

## Summary

✅ **Ports**: 8080 (HTTP), 6000 (Telnet), 6022 (SSH)
✅ **No conflicts** with MinIO (9000-9001)  
✅ **All features** available
✅ **Simple command**: `./run-port-6000.sh`

The build is completing now. Once done, the script will work perfectly! 🎉
