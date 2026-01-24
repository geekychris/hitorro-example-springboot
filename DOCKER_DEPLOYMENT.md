# Hitorro Example Spring Boot - Docker Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the Hitorro Example Spring Boot application using Docker.

## 📋 Prerequisites

- Docker Engine 20.10+ or Docker Desktop
- Docker Compose 2.0+ (included with Docker Desktop)
- At least 4GB RAM available for Docker
- 10GB free disk space

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

The simplest way to run the application:

```bash
cd hitorro-example-springboot

# Start the application
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f hitorro-app

# Stop the application
docker-compose down
```

### Option 2: Build Scripts

Use the provided convenience scripts:

```bash
cd hitorro-example-springboot

# Build the Docker image
./docker-build.sh

# Run the container
./docker-run.sh

# View logs
docker logs -f hitorro-app

# Stop the container
docker stop hitorro-app
```

### Option 3: Manual Docker Commands

For more control:

```bash
# Build the image (from project root)
cd /Users/chris/hitorro
docker build -f hitorro-example-springboot/Dockerfile -t hitorro-example-springboot:latest .

# Run the container
docker run -d \
  --name hitorro-app \
  -p 8080:8080 \
  -p 9000:9000 \
  -p 9022:9022 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -v hitorro-data:/var/lib/hitorro/data \
  -v hitorro-files:/opt/hitorro-app/data/files \
  -v hitorro-logs:/var/lib/hitorro/logs \
  hitorro-example-springboot:latest

# Stop the container
docker stop hitorro-app
docker rm hitorro-app
```

## 🌐 Accessing the Application

Once running, access these endpoints:

| Service | URL | Description |
|---------|-----|-------------|
| **Web UI** | http://localhost:8080 | Main application interface |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Interactive API documentation |
| **API Docs** | http://localhost:8080/api-docs | OpenAPI specification |
| **H2 Console** | http://localhost:8080/h2-console | Database admin interface |
| **Actuator** | http://localhost:8080/actuator | Health, metrics, and monitoring |
| **REST API** | http://localhost:8080/api/rest | DMS REST endpoints |
| **Commands** | http://localhost:8080/api/commands | Command execution API |
| **Telnet CLI** | `telnet localhost 9000` | Native Hitorro CLI |
| **SSH CLI** | `ssh -p 9022 localhost` | SSH-based CLI |

### H2 Database Console Credentials

- **JDBC URL**: `jdbc:h2:file:/var/lib/hitorro/data/hitorrodb`
- **Username**: `sa`
- **Password**: `hitorro`
- **Driver**: `org.h2.Driver`

## 📦 Files Created

### Core Docker Files

| File | Description |
|------|-------------|
| `Dockerfile` | Multi-stage Docker build configuration |
| `.dockerignore` | Files to exclude from build context |
| `docker-compose.yml` | H2 database configuration (default) |
| `docker-compose-postgres.yml` | PostgreSQL database configuration |
| `docker-build.sh` | Convenience script to build images |
| `docker-run.sh` | Convenience script to run containers |

### Configuration Files

| File | Description |
|------|-------------|
| `docker/application-docker.yml` | Spring config for Docker (H2) |
| `docker/application-docker-postgres.yml` | Spring config for PostgreSQL |
| `docker/csv/stores.csv` | Store definitions |
| `docker/csv/domaininfo.csv` | Domain metadata |
| `docker/postgres/init.sql` | PostgreSQL initialization script |
| `docker/README.md` | Detailed Docker documentation |

## 🗄️ Database Options

### Option A: H2 Database (Default)

File-based embedded database, perfect for development and testing.

**Pros:**
- Zero configuration
- No external dependencies
- Fast startup
- Easy backup/restore

**Cons:**
- Not recommended for production
- Single connection concurrency
- Limited SQL features

```bash
# Use default docker-compose.yml
docker-compose up -d
```

### Option B: PostgreSQL Database

Production-grade relational database.

**Pros:**
- Production-ready
- Full SQL support
- Multi-user concurrency
- Better performance at scale

**Cons:**
- Additional container
- More complex setup
- Requires database management

```bash
# Use PostgreSQL configuration
docker-compose -f docker-compose-postgres.yml up -d
```

#### PostgreSQL Connection Details

- **Host**: `postgres` (internal) or `localhost:5432` (external)
- **Database**: `hitorrodb`
- **Username**: `hitorro`
- **Password**: `hitorro_password`

## 💾 Data Persistence

### Docker Volumes

The application uses named volumes for data persistence:

| Volume | Purpose | Path |
|--------|---------|------|
| `hitorro-data` | Database files | `/var/lib/hitorro/data` |
| `hitorro-files` | Uploaded files & content | `/opt/hitorro-app/data/files` |
| `hitorro-logs` | Application logs | `/var/lib/hitorro/logs` |
| `postgres-data` | PostgreSQL data (if used) | `/var/lib/postgresql/data` |

### Volume Management

**List volumes:**
```bash
docker volume ls | grep hitorro
```

**Inspect volume:**
```bash
docker volume inspect hitorro-data
```

**Backup data:**
```bash
# Create backup
docker run --rm \
  -v hitorro-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/hitorro-data-backup.tar.gz /data

# Restore backup
docker run --rm \
  -v hitorro-data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/hitorro-data-backup.tar.gz -C /
```

**Delete volumes (WARNING: Data loss!):**
```bash
# Stop containers first
docker-compose down

# Delete all volumes
docker-compose down -v

# Or delete specific volume
docker volume rm hitorro-data
```

## ⚙️ Configuration

### Environment Variables

Customize behavior using environment variables:

**Common Variables:**

```yaml
environment:
  # Spring Profile
  - SPRING_PROFILES_ACTIVE=docker
  
  # Paths
  - HT_BIN=/opt/hitorro
  - HT_HOME=/var/lib/hitorro
  
  # Database (for PostgreSQL)
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/hitorrodb
  - SPRING_DATASOURCE_USERNAME=hitorro
  - SPRING_DATASOURCE_PASSWORD=hitorro_password
  
  # Logging
  - LOGGING_LEVEL_ROOT=INFO
  - LOGGING_LEVEL_COM_HITORRO=DEBUG
  
  # JVM Options
  - JAVA_OPTS=-Xmx2g -Xms512m -XX:+UseG1GC
```

### Custom Configuration File

Mount a custom `application.yml`:

```bash
docker run -d \
  -v /path/to/custom-application.yml:/var/lib/hitorro/config/application.yml:ro \
  hitorro-example-springboot:latest
```

Or in `docker-compose.yml`:

```yaml
volumes:
  - ./my-config.yml:/var/lib/hitorro/config/application.yml:ro
```

## 🔍 Monitoring & Debugging

### View Logs

**Using docker-compose:**
```bash
# All logs
docker-compose logs -f

# Specific service
docker-compose logs -f hitorro-app

# Last 100 lines
docker-compose logs --tail=100 hitorro-app
```

**Using docker:**
```bash
docker logs -f hitorro-app
docker logs --tail=100 hitorro-app
```

### Health Checks

**Container health:**
```bash
docker ps --filter name=hitorro-app
docker inspect hitorro-app | jq '.[0].State.Health'
```

**Application health endpoint:**
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health | jq .
```

### Access Container Shell

```bash
# Using docker-compose
docker-compose exec hitorro-app sh

# Using docker
docker exec -it hitorro-app sh

# As root (for troubleshooting)
docker exec -u root -it hitorro-app sh
```

### Monitor Resources

```bash
# Real-time stats
docker stats hitorro-app

# Detailed info
docker inspect hitorro-app
```

## 🔧 Troubleshooting

### Common Issues

**1. Port already in use**
```bash
# Find process using port
lsof -i :8080
netstat -an | grep 8080

# Change port in docker-compose.yml
ports:
  - "8081:8080"  # Use 8081 instead
```

**2. Out of memory**
```bash
# Increase memory limit
environment:
  - JAVA_OPTS=-Xmx4g -Xms1g
```

**3. Database connection failed**
```bash
# Check postgres is running
docker-compose ps postgres

# Check logs
docker-compose logs postgres

# Verify network
docker network inspect hitorro-network
```

**4. Build fails**
```bash
# Clear Docker cache
docker builder prune -a

# Rebuild without cache
docker-compose build --no-cache
```

**5. Container won't start**
```bash
# Check logs
docker logs hitorro-app

# Verify volumes
docker volume ls

# Check disk space
df -h
```

### Reset Everything

```bash
# Stop and remove containers
docker-compose down

# Remove volumes (WARNING: Data loss!)
docker-compose down -v

# Remove images
docker rmi hitorro-example-springboot:latest

# Rebuild from scratch
docker-compose up --build -d
```

## 🚀 Production Deployment

### Resource Tuning

**JVM Options:**
```yaml
environment:
  - JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError
```

**Container Limits:**
```yaml
deploy:
  resources:
    limits:
      cpus: '4.0'
      memory: 8G
    reservations:
      cpus: '1.0'
      memory: 2G
```

### Security

**1. Change default passwords**
```yaml
environment:
  - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
```

**2. Use Docker secrets**
```yaml
secrets:
  db_password:
    external: true

environment:
  - SPRING_DATASOURCE_PASSWORD=/run/secrets/db_password
```

**3. Enable HTTPS** (use reverse proxy)

**4. Restrict network access**
```yaml
ports:
  - "127.0.0.1:8080:8080"  # Local only
```

### Reverse Proxy (nginx)

Example `nginx.conf`:

```nginx
upstream hitorro {
    server hitorro-app:8080;
}

server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://hitorro;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 📊 Performance Optimization

### Database (PostgreSQL)

**Connection pooling:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

**Hibernate batch processing:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 20
        order_inserts: true
        order_updates: true
```

### File Storage

**Use external storage for large files:**
- AWS S3
- MinIO
- Network-attached storage (NAS)

## 🔄 Updates & Maintenance

### Update Application

```bash
# Pull latest code
git pull

# Rebuild image
docker-compose build hitorro-app

# Restart with new image
docker-compose up -d hitorro-app
```

### Database Migrations

```bash
# Backup before migration
./backup-data.sh

# Run migration
docker-compose exec hitorro-app java -jar app.jar --migrate

# Or use Flyway/Liquibase
```

## 📚 Additional Resources

- **Main Documentation**: `/Users/chris/hitorro/hitorro-spring-boot/README.md`
- **Module Documentation**: `/Users/chris/hitorro/generated_docs/`
- **Docker Documentation**: `docker/README.md`
- **API Documentation**: http://localhost:8080/swagger-ui.html (when running)

## 🆘 Support

If you encounter issues:

1. Check logs: `docker-compose logs -f`
2. Verify health: `curl http://localhost:8080/actuator/health`
3. Review container status: `docker-compose ps`
4. Check resources: `docker stats`
5. Consult troubleshooting section above

## 📝 Example Commands

```bash
# Complete workflow
cd hitorro-example-springboot
./docker-build.sh
docker-compose up -d
docker-compose logs -f

# Access application
open http://localhost:8080/swagger-ui.html

# Stop application
docker-compose down

# With data cleanup
docker-compose down -v
```
