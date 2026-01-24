# 🚀 Quick Start - Port 6000 Range

## Solution for MinIO Port Conflict

Your MinIO container uses ports 9000-9001, so we use the **6000 port range** instead.

## One-Command Start

```bash
cd /Users/chris/hitorro/hitorro-example-springboot/docker_build
./run-port-6000.sh
```

This script will:
1. **Build** the Docker image if not present (10-15 min first time)
2. **Start** the container with 6000-range ports
3. **Wait** for application to be healthy
4. **Show** all access URLs

## Port Mappings

| Service | Your Port | Container Port |
|---------|-----------|----------------|
| HTTP (Web UI) | **8080** | 8080 |
| Telnet CLI | **6000** | 9000 |
| SSH CLI | **6022** | 9022 |

**Why 6000?** Avoids conflict with MinIO (9000-9001)

## Access Your Application

### Web Interface
- **Main UI**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Database**: http://localhost:8080/h2-console

### Command Line
```bash
# Telnet
telnet localhost 6000

# SSH  
ssh -p 6022 localhost
```

## If Build is Slow

The Docker build compiles **all 19 Hitorro modules** from source:

**Expected times:**
- First build: **10-15 minutes**
- Cached rebuild: **2-3 minutes**

**While waiting**, the build is:
1. Building React UI with Vite
2. Compiling 19 Java modules with Maven
3. Creating optimized runtime image

## Manual Alternative

If you want more control:

```bash
# 1. Build image (run once)
cd /Users/chris/hitorro/hitorro-example-springboot
docker build -f Dockerfile-with-ui -t hitorro-app:latest ..

# 2. Run container (reusable)
docker run -d \
  --name hitorro-app \
  -p 8080:8080 \
  -p 6000:9000 \
  -p 6022:9022 \
  -v hitorro-data:/var/lib/hitorro/data \
  hitorro-app:latest

# 3. Check health
curl http://localhost:8080/actuator/health
```

## Quick Commands

```bash
# Check if running
docker ps | grep hitorro

# View logs
docker logs -f hitorro-app

# Stop
docker stop hitorro-app

# Restart
./run-port-6000.sh
```

## Troubleshooting

**Build seems stuck?**
- It's compiling 19 modules - this is normal
- Check progress: `docker ps` (look for build containers)

**Port 6000 also taken?**
- Edit `run-port-6000.sh` and change to 7000 range

**Want different HTTP port?**
- Change `-p 8080:8080` to `-p 8081:8080`
- Then access at http://localhost:8081

## What's Included

✅ **All 19 Hitorro modules**
✅ **Spring Boot backend**
✅ **React UI** (Material Design)
✅ **REST API** with Swagger
✅ **H2 Database**
✅ **LibreOffice** for document transformation
✅ **CLI access** (Telnet & SSH)

## Summary

**Easiest way**:
```bash
./run-port-6000.sh
```

**Access at**: http://localhost:8080

**Ports**: 8080, 6000, 6022 (no conflicts!)

The build takes time because it's building everything from source. Once complete, subsequent starts are instant! 🎉
