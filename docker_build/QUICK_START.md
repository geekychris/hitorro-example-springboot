# 🚀 Hitorro Quick Start Guide

Get Hitorro running in ONE command!

## 🎯 Absolute Fastest Way

```bash
cd docker_build
./build-and-start.sh
```

That's it! Opens at http://localhost:8080

## Alternative: Two Commands

```bash
cd docker_build
./hitorro.sh build-ui
./hitorro.sh start-ui
```

## Step-by-Step

### 1️⃣ Build
```bash
cd docker_build
./hitorro.sh build-ui
```
⏱️ Takes 5-10 minutes first time (downloads dependencies)

### 2️⃣ Start
```bash
./hitorro.sh start-ui
```
⏱️ Takes 30-60 seconds

### 3️⃣ Access
Open your browser to:
- **Main UI**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html

## What You Get

✅ Document Management System  
✅ React Web Interface  
✅ File Upload (Drag & Drop)  
✅ Content Transformation  
✅ REST API  
✅ CLI Access (Telnet/SSH)  
✅ Database (H2)  
✅ Health Monitoring  

## Common Commands

```bash
# View logs
./hitorro.sh logs

# Stop application
./hitorro.sh stop

# Check status
./hitorro.sh status

# Restart
./hitorro.sh restart

# Complete cleanup
./hitorro.sh clean-all
```

## Troubleshooting

**Container won't start?**
```bash
./hitorro.sh logs
```

**Need fresh start?**
```bash
./hitorro.sh clean-all
./hitorro.sh build-ui
./hitorro.sh start-ui
```

**Port 8080 in use?**
```bash
lsof -i :8080  # See what's using it
```

## Next Steps

- 📖 Read `README.md` for detailed docs
- 🎨 Customize the UI (see `../frontend/`)
- 🔧 Configure settings (see `../docker/`)
- 📊 View metrics at http://localhost:8080/actuator

## Development Mode

For hot reload during development:

```bash
./hitorro.sh dev
```

This starts:
- Backend: http://localhost:8080
- Frontend: http://localhost:3000 (with hot reload)

## Need Help?

```bash
./hitorro.sh help
```

---

**That's it!** You're ready to use Hitorro DMS. 🎉
