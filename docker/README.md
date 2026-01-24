# Hitorro Example Spring Boot - Docker Deployment

This directory contains Docker configuration files for deploying the Hitorro Example Spring Boot application.

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Build and start the application
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### Using Build Scripts

```bash
# Build the Docker image
./docker-build.sh

# Run the container
./docker-run.sh

# Or specify a custom tag
./docker-build.sh v1.0.0
./docker-run.sh v1.0.0
```

### Manual Docker Commands

```bash
# Build from the parent directory
cd ..
docker build -f hitorro-example-springboot/Dockerfile -t hitorro-example-springboot:latest .

# Run the container
docker run -d \
  --name hitorro-app \
  -p 8080:8080 \
  -p 9000:9000 \
  -p 9022:9022 \
  -v hitorro-data:/var/lib/hitorro/data \
  -v hitorro-files:/opt/hitorro-app/data/files \
  hitorro-example-springboot:latest
```

## Accessing the Application

Once the container is running, you can access:

- **Web UI**: http://localhost:8080
- **Swagger API**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:/var/lib/hitorro/data/hitorrodb`
  - Username: `sa`
  - Password: `hitorro`
- **Actuator**: http://localhost:8080/actuator
- **Telnet CLI**: `telnet localhost 9000`
- **SSH CLI**: `ssh -p 9022 localhost`

## Configuration

### Environment Variables

You can customize the application behavior using environment variables:

```bash
docker run -d \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e LOGGING_LEVEL_COM_HITORRO=INFO \
  -e JAVA_OPTS="-Xmx4g -Xms1g" \
  hitorro-example-springboot:latest
```

### Custom Configuration File

Mount a custom `application.yml`:

```bash
docker run -d \
  -v /path/to/your/application.yml:/var/lib/hitorro/config/application.yml:ro \
  hitorro-example-springboot:latest
```

## Database Options

### H2 Database (Default)

The default configuration uses H2 file-based database for persistence. Data is stored in Docker volumes.

### PostgreSQL Database

To use PostgreSQL instead of H2:

```bash
# Use the PostgreSQL compose configuration
docker-compose -f docker-compose-postgres.yml up -d
```

## Volumes

The application uses the following Docker volumes for persistence:

- `hitorro-data` - Database files and application data
- `hitorro-files` - Uploaded files and content
- `hitorro-logs` - Application logs

### Managing Volumes

```bash
# List volumes
docker volume ls | grep hitorro

# Inspect a volume
docker volume inspect hitorro-data

# Backup data
docker run --rm -v hitorro-data:/data -v $(pwd):/backup alpine \
  tar czf /backup/hitorro-data-backup.tar.gz /data

# Restore data
docker run --rm -v hitorro-data:/data -v $(pwd):/backup alpine \
  tar xzf /backup/hitorro-data-backup.tar.gz -C /
```

## Troubleshooting

### View Logs

```bash
# Using docker-compose
docker-compose logs -f hitorro-app

# Using docker
docker logs -f hitorro-app
```

### Check Health

```bash
# Check container health status
docker ps --filter name=hitorro-app

# Check health endpoint
curl http://localhost:8080/actuator/health
```

### Access Container Shell

```bash
docker exec -it hitorro-app sh
```

### Clean Up

```bash
# Stop and remove containers
docker-compose down

# Remove volumes (WARNING: This deletes all data!)
docker-compose down -v

# Remove images
docker rmi hitorro-example-springboot:latest
```

## Production Deployment

### Resource Limits

The `docker-compose.yml` includes resource limits:
- CPU: 2 cores (limit), 0.5 cores (reservation)
- Memory: 3GB (limit), 512MB (reservation)

Adjust these based on your requirements:

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

### JVM Tuning

Adjust JVM options via environment variables:

```yaml
environment:
  - JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Security Considerations

1. **Change default passwords** in production
2. **Use secrets management** for sensitive data
3. **Enable SSL/TLS** for production deployments
4. **Restrict port exposure** as needed
5. **Regular security updates** of base images

### Networking

For production, consider using a reverse proxy (nginx, traefik) in front of the application:

```yaml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - hitorro-app
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build and Push Docker Image

on:
  push:
    branches: [ main ]
    tags: [ 'v*' ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Build Docker image
        run: ./hitorro-example-springboot/docker-build.sh ${{ github.ref_name }}
      
      - name: Push to Docker Hub
        run: |
          docker login -u ${{ secrets.DOCKER_USERNAME }} -p ${{ secrets.DOCKER_PASSWORD }}
          docker push hitorro-example-springboot:${{ github.ref_name }}
```

## Multi-Architecture Builds

Build for multiple architectures (AMD64, ARM64):

```bash
docker buildx create --use
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t hitorro-example-springboot:latest \
  -f hitorro-example-springboot/Dockerfile \
  --push \
  .
```

## Support

For issues or questions:
- Check the main documentation in the parent directory
- Review application logs
- Check Docker container status and health
