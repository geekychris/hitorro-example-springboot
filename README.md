# Hitorro Spring Boot Example Application

A comprehensive example application demonstrating the integration of Hitorro framework with Spring Boot, including an interactive React UI for testing all features.

## Features

This example showcases:

- **Document Management System (DMS)** - Full CRUD operations, versioning, content management
- **Filesystem Crawler** - Import files/directories into DMS
- **JSON Type System (JVS)** - Type enrichment and field exploration with NLP features
- **Command Framework** - Execute `@CommandDef` annotated methods via REST and CLI
- **REST API** - Auto-generated REST endpoints for all services
- **Service Framework** - Hitorro service lifecycle and dependency management
- **Structured Logging** - Config-driven structured logging with Kafka integration (NEW!)
- **Interactive React UI** - Test all features through a modern web interface

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Node.js 18+ (for React UI)
- Kafka (optional, for structured logging)

### Running the Application

1. **Start the Spring Boot application**:
   ```bash
   cd hitorro-example-springboot
   mvn spring-boot:run
   ```

2. **Access the application**:
   - **React UI**: http://localhost:8080 (served by Spring Boot)
   - **H2 Console**: http://localhost:8080/h2-console
   - **Swagger UI**: http://localhost:8080/swagger-ui.html
   - **Actuator**: http://localhost:8080/actuator

3. **Access the CLI** (optional):
   ```bash
   telnet localhost 5050
   ```

## React UI Features

The React UI provides interactive tabs for testing all features:

### 📄 Document Management
- Create, read, update, delete documents
- Upload and download content with multiple renditions
- Create versions and view history
- Organize with containers (folders, forums)
- Tag with categories for advanced search
- Transform content (PDF to text, images to thumbnails)

### 📁 Filesystem Crawler
- Import files from server filesystem into DMS
- Recursive directory crawling
- Real-time progress tracking
- Support for various file types

### 🔧 Type System
- JSON enrichment with JVS2JVSEnrichMapper
- Browse available type definitions
- Explore fields with types and paths
- Interactive JSON viewer

### 💻 Commands
- Discover all @CommandDef methods
- Dynamic parameter forms
- Execute commands with results visualization
- Support for complex parameter types

### 🚀 REST Explorer
- Auto-discover all REST endpoints
- Test APIs interactively
- Streaming endpoint support
- View formatted responses

### 🔧 Services Explorer
- View all loaded Hitorro services
- Visualize service dependency hierarchy
- Inspect service details and status

### 📊 Structured Logging (NEW!)
- Log user activity events (login, logout, API access)
- Publish to Kafka topics for downstream processing
- Real-time demo with configurable parameters
- View log schema and Kafka configuration
- Check logging status and connectivity

## Structured Logging Setup

The application includes structured logging support with Kafka integration:

### Enable Structured Logging

1. **Start Kafka** (Docker):
   ```bash
   docker run -d --name zookeeper -p 2181:2181 \
     confluentinc/cp-zookeeper:latest \
     -e ZOOKEEPER_CLIENT_PORT=2181
   
   docker run -d --name kafka -p 9092:9092 \
     --link zookeeper \
     -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
     -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
     -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
     confluentinc/cp-kafka:latest
   ```

2. **Enable in application.yml**:
   ```yaml
   hitorro:
     structured-logging:
       enabled: true
       kafka-bootstrap-servers: localhost:9092
   ```

3. **Restart the application**

4. **Use the Structured Logging tab** in the React UI to:
   - Check logging status
   - Log user activity events
   - View responses in real-time

### Verify Logs in Kafka

```bash
# View logs
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning
```

### Log Schema

Logs are defined in `src/main/resources/log-configs/user-activity-log.json` and include:
- User ID and username
- Event type (LOGIN, LOGOUT, API_ACCESS)
- Timestamp and date
- IP address and user agent
- HTTP method, endpoint, status code
- Response time
- Custom metadata

See [STRUCTURED_LOGGING_DEMO.md](STRUCTURED_LOGGING_DEMO.md) for complete documentation.

## Configuration

### Database

The application uses H2 database by default (file-based, persistent):

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/hitorrodb
    username: sa
    password: hitorro
```

Access H2 Console at http://localhost:8080/h2-console

### Hitorro Configuration

Core Hitorro settings in `application.yml`:

```yaml
hitorro:
  enabled: true
  ht-bin: /Users/chris/hitorro      # Hitorro installation
  ht-home: /Users/chris/hthome      # Runtime data
  
  services:
    enabled: true
    load:
      - com.hitorro.basedms.db.HibernateService
      - com.hitorro.base.objects.BaseDMSService
      - com.hitorro.basedms.transformer.TransformerService
  
  dms:
    enabled: true
  
  jvs:
    enabled: true
    nlp-enabled: false
  
  cli:
    native-enabled: true
    telnet-port: 5050
```

## API Documentation

### Swagger UI

Interactive API documentation available at:
- http://localhost:8080/swagger-ui.html

### Key Endpoints

- **DMS**: `/api/dms/documents`, `/api/dms/containers`
- **Crawler**: `/api/dms/crawler/crawl`
- **Type System**: `/api/jvs/enrich`, `/api/jvs/types`
- **Commands**: `/api/commands/list`, `/api/commands/execute`
- **REST Explorer**: `/api/rest/endpoints`
- **Services**: `/api/services/list`
- **Structured Logging**: `/api/demo/login`, `/api/demo/logout`, `/api/demo/data`

## Development

### Building the Application

```bash
mvn clean package
```

### Building the React UI

```bash
cd react-app
npm install
npm run build
```

The built React app is automatically served by Spring Boot.

### Running Tests

```bash
mvn test
```

## Documentation

- **[STRUCTURED_LOGGING_DEMO.md](STRUCTURED_LOGGING_DEMO.md)** - Structured logging guide
- **[react-app/README.md](react-app/README.md)** - React UI documentation
- **[generated_docs/](generated_docs/)** - Additional documentation

## Project Structure

```
hitorro-example-springboot/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/hitorro/example/
│   │   │       ├── HitorroExampleApplication.java
│   │   │       └── logging/              # Structured logging
│   │   └── resources/
│   │       ├── application.yml
│   │       └── log-configs/              # Log schema definitions
│   └── test/
├── react-app/                            # React UI source
├── data/                                 # H2 database
├── docker/                               # Docker configurations
├── docker_build/                         # Docker build scripts
└── README.md
```

## Troubleshooting

### Application won't start

Check that:
- Java 21+ is installed: `java -version`
- Ports 8080, 5050 are available
- `ht-bin` and `ht-home` paths exist

### Structured logging not working

Check that:
- Kafka is running and accessible
- `hitorro.structured-logging.enabled=true`
- Application has been restarted

### React UI not loading

The React UI is served by Spring Boot. If you see 404 errors:
1. Build the React app: `cd react-app && npm run build`
2. Restart Spring Boot

## License

Copyright (c) 2006-2026 Chris Collins. See LICENSE file for details.
