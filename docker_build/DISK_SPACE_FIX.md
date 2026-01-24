# 💾 Docker Build - Disk Space Issue

## The Problem

Docker build failed with:
```
write /build/hitorro-logdigest/target/hitorro-logdigest-3.0.0-jar-with-dependencies.jar: no space left on device
```

This means your system or Docker is running out of disk space during the build.

## Quick Fix

Clean up Docker resources:

```bash
# Check Docker disk usage
docker system df

# Clean up everything (removes all unused images, containers, volumes)
docker system prune -a --volumes -f

# Check disk space
df -h
```

## Manual Cleanup

If you want more control:

```bash
# Remove stopped containers
docker container prune -f

# Remove dangling images
docker image prune -f

# Remove all unused images (not just dangling)
docker image prune -a -f

# Remove unused volumes
docker volume prune -f

# Remove build cache
docker builder prune -a -f
```

## After Cleanup

Once you have space, retry the build:

```bash
cd /Users/chris/hitorro/hitorro-example-springboot/docker_build
./run-port-6000.sh
```

## How Much Space Needed?

The Hitorro Docker build needs:

- **Build process**: ~15-20 GB temporary space
- **Final image**: ~2.2 GB
- **Recommended free**: 30+ GB

## Check Disk Usage

```bash
# Check overall disk space
df -h

# Check Docker-specific usage
docker system df -v

# Find large Docker items
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | sort -k 3 -h
```

## Increase Docker Disk (If Needed)

If using Docker Desktop:

1. Open **Docker Desktop**
2. Go to **Settings** → **Resources** → **Disk image size**
3. Increase to at least **64 GB**
4. Click **Apply & Restart**

## Alternative: Clean System Disk

If your Mac's main disk is full:

```bash
# Check what's using space
du -sh ~/Library/Containers/com.docker.docker/Data/*

# Empty trash
rm -rf ~/.Trash/*

# Clean Homebrew cache
brew cleanup -s

# Clean Maven cache (if very large)
du -sh ~/.m2/repository
# Optionally: rm -rf ~/.m2/repository (will re-download dependencies)
```

## Prevention

To avoid this in the future:

```bash
# Regularly clean Docker
docker system prune -a -f

# Or set up automatic cleanup
# Add to ~/.zshrc:
alias docker-clean='docker system prune -a --volumes -f'
```

## If Still Failing

If cleanup doesn't help:

1. **Check available space**: `df -h`
2. **Increase Docker disk allocation** (Docker Desktop settings)
3. **Free up Mac disk space** (remove large files)
4. **Build locally instead**:
   ```bash
   cd /Users/chris/hitorro/hitorro-example-springboot
   mvn clean package -DskipTests
   java -jar target/*.jar
   ```

## Summary

**Quick fix**:
```bash
docker system prune -a --volumes -f
./run-port-6000.sh
```

**Need**: 30+ GB free space
**Image size**: ~2.2 GB
**Build temp**: ~15-20 GB
