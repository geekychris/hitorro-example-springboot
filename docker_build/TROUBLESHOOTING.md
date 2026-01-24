# 🔧 Troubleshooting Guide

## Common Issues and Solutions

### Issue: `build-and-start.sh` fails with "not found" error

**Symptom:**
```
ERROR: failed to calculate checksum...
"/2>/dev/null": not found
"/||": not found
```

**Cause:** Docker COPY commands don't support shell operators like `2>/dev/null || true`

**Solution:** ✅ **FIXED!** The Dockerfiles have been updated to remove these operators.

If you still see this error:
```bash
cd docker_build
git pull  # Get latest fixes
./build-and-start.sh
```

---

### Issue: Docker is not running

**Symptom:**
```
Error: Docker is not running
```

**Solution:**
1. Start Docker Desktop
2. Wait for Docker to fully start (whale icon should be steady)
3. Try again: `./build-and-start.sh`

---

### Issue: Port 8080 already in use

**Symptom:**
```
Error: bind: address already in use
```

**Solution:**
```bash
# Find what's using port 8080
lsof -i :8080

# Stop the process
kill -9 <PID>

# Or stop our container
./hitorro.sh stop

# Then start again
./hitorro.sh start-ui
```

---

### Issue: Build takes very long

**Normal:** First build takes 5-10 minutes (downloads dependencies)

**Too slow?**
```bash
# Check Docker resources
docker system df

# Clean up old images
docker system prune -a

# Rebuild
./build-and-start.sh
```

---

### Issue: Out of disk space

**Symptom:**
```
no space left on device
```

**Solution:**
```bash
# Clean Docker system
docker system prune -a

# Remove old volumes
./hitorro.sh clean-all

# Check disk space
df -h
```

---

### Issue: Container starts but UI doesn't load

**Check:**
1. Container is running:
   ```bash
   ./hitorro.sh status
   ```

2. View logs for errors:
   ```bash
   ./hitorro.sh logs
   ```

3. Check health endpoint:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

4. Try restarting:
   ```bash
   ./hitorro.sh restart
   ```

---

### Issue: Maven/Node build fails

**Solution:**
```bash
# Clean everything
./hitorro.sh clean-all

# Clear Docker build cache
docker builder prune -a

# Rebuild from scratch
./build-and-start.sh
```

---

### Issue: Permission denied

**Symptom:**
```
permission denied
```

**Solution:**
```bash
# Make scripts executable
chmod +x docker_build/*.sh

# Or run with bash
bash docker_build/build-and-start.sh
```

---

### Issue: Cannot connect to Docker daemon

**Symptom:**
```
Cannot connect to the Docker daemon
```

**Solution:**
1. Ensure Docker Desktop is running
2. Check Docker context:
   ```bash
   docker context ls
   docker context use default
   ```

---

## Debug Commands

### Check Docker Status
```bash
./diagnose.sh
```

### View Container Logs
```bash
./hitorro.sh logs
```

### Check Container Status
```bash
./hitorro.sh status
docker ps -a
```

### Inspect Container
```bash
docker inspect hitorro-app
```

### Access Container Shell
```bash
docker exec -it hitorro-app sh
```

### Test Build Manually
```bash
cd /Users/chris/hitorro/hitorro-example-springboot
docker build -f Dockerfile-with-ui -t test:latest ..
```

---

## Complete Reset

If nothing works, do a complete reset:

```bash
cd docker_build

# Stop everything
./hitorro.sh stop
docker-compose down 2>/dev/null || true

# Remove all Docker resources
./hitorro.sh clean-all

# Clean Docker system
docker system prune -a
docker volume prune

# Rebuild from scratch
./build-and-start.sh
```

---

## Get Help

### Quick Diagnostics
```bash
cd docker_build
./diagnose.sh
```

### View Detailed Logs
```bash
./hitorro.sh logs | tail -100
```

### Check System Resources
```bash
docker system df
docker stats --no-stream
```

---

## Known Issues

### LibreOffice Installation Takes Time
- **Normal:** LibreOffice download/install adds 2-3 minutes to build
- **Skip it:** Edit Dockerfile to remove LibreOffice if not needed

### Build Cache Not Working
- **Cause:** Dockerfile changes
- **Solution:** Normal, rebuild will re-cache layers

### React Build Fails
- **Check:** `frontend/package.json` exists
- **Solution:** Ensure `frontend/` directory is complete

---

## Prevention Tips

1. **Always pull latest**: `git pull` before building
2. **Use `./hitorro.sh`**: It has better error handling
3. **Check logs first**: `./hitorro.sh logs` shows issues
4. **Clean regularly**: `./hitorro.sh clean` saves space
5. **Use diagnostics**: `./diagnose.sh` before asking for help

---

## Still Stuck?

1. Run diagnostics: `./diagnose.sh`
2. Check logs: `./hitorro.sh logs`
3. Try complete reset (see above)
4. Check Docker Desktop resources (increase if needed)
5. Ensure you have 10GB+ free disk space

---

**Most issues are fixed by:**
```bash
./hitorro.sh clean-all
./build-and-start.sh
```
