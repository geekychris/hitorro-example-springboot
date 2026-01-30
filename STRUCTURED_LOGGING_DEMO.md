# Structured Logging Demo

This application includes a complete demonstration of **Hitorro Structured Logging** with Kafka integration.

## Quick Start

### 1. Start Kafka (Required)

Using Docker:

```bash
docker run -d --name zookeeper -p 2181:2181 confluentinc/cp-zookeeper:latest \
  -e ZOOKEEPER_CLIENT_PORT=2181

docker run -d --name kafka -p 9092:9092 \
  --link zookeeper \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest
```

Or use the docker-compose.yml in the project root.

### 2. Enable Structured Logging

Edit `src/main/resources/application.yml`:

```yaml
hitorro:
  structured-logging:
    enabled: true  # Change to true
    kafka-bootstrap-servers: localhost:9092
```

### 3. Start the Application

```bash
mvn spring-boot:run
```

### 4. Try the Demo

#### Option A: Use the React UI (Recommended)

1. Open the React app: `http://localhost:8080`
2. Click on the **"Structured Logging"** tab
3. Click "Check Status" to verify structured logging is enabled
4. Fill in user details (userId, username, sessionId)
5. Click the buttons to log different event types:
   - **Log LOGIN Event** - Logs user login
   - **Log LOGOUT Event** - Logs user logout
   - **Log API_ACCESS Event** - Logs API access
6. View the response in the UI

#### Option B: Use cURL Commands

#### Get Demo Info
```bash
curl http://localhost:8080/api/demo/info
```

#### Log a Login Event
```bash
curl -X POST http://localhost:8080/api/demo/login \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "username": "john.doe"
  }'
```

#### Log a Logout Event
```bash
curl -X POST http://localhost:8080/api/demo/logout \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "username": "john.doe",
    "sessionId": "sess-abc-123"
  }'
```

#### Log an API Access Event
```bash
curl "http://localhost:8080/api/demo/data?userId=user123"
```

## Verify Logs in Kafka

Check that logs are being published to Kafka:

```bash
# List topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Should see: hitorro-user-activity

# Consume logs
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning
```

You should see JSON log events like:

```json
{
  "user_id": "user123",
  "username": "john.doe",
  "event_type": "LOGIN",
  "event_date": "2026-01-28",
  "timestamp": "2026-01-28T20:15:30.123Z",
  "ip_address": "127.0.0.1",
  "user_agent": "curl/7.64.1",
  "topic": "user-events"
}
```

## Demo Components

### 1. Log Configuration
**File**: `src/main/resources/log-configs/user-activity-log.json`

Defines the schema for user activity logs including:
- Field definitions (user_id, event_type, timestamp, etc.)
- Kafka topic configuration
- Iceberg table configuration (for downstream processing)

### 2. UserActivityLogger
**File**: `src/main/java/com/hitorro/example/logging/UserActivityLogger.java`

Type-safe logger that extends `StructuredLogger` with methods:
- `logLogin()` - Log user login events
- `logLogout()` - Log user logout events
- `logApiAccess()` - Log API access events
- `logCustomEvent()` - Log custom events with metadata

### 3. Demo REST Controller
**File**: `src/main/java/com/hitorro/example/logging/UserActivityDemoController.java`

REST endpoints demonstrating structured logging:
- `POST /api/demo/login` - Simulate user login
- `POST /api/demo/logout` - Simulate user logout
- `GET /api/demo/data` - Simulate API access
- `GET /api/demo/info` - Get demo information

## Console Fallback Mode

If Kafka is not available, logs will fall back to console/SLF4J:

```
2026-01-28 ... INFO  StructuredLogger - [STRUCTURED_LOG:hitorro-user-activity] {"user_id":"user123",...}
```

This is enabled by default:

```yaml
hitorro:
  structured-logging:
    enable-console-fallback: true
```

## Creating Your Own Logger

### 1. Define Log Schema

Create `src/main/resources/log-configs/my-log.json`:

```json
{
  "name": "MyLog",
  "version": "1.0.0",
  "kafka": {
    "topic": "my-topic"
  },
  "fields": [
    {"name": "id", "type": "string", "required": true},
    {"name": "message", "type": "string", "required": true}
  ]
}
```

### 2. Create Logger Class

```java
@Component
public class MyLogger extends StructuredLogger {
    
    public MyLogger(StructuredLoggerProperties properties) {
        super(properties, "my-topic");
    }
    
    public void logMessage(String id, String message) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("message", message);
        publish(event);
    }
}
```

### 3. Use It

```java
@Autowired
private MyLogger myLogger;

public void doSomething() {
    myLogger.logMessage("123", "Something happened");
}
```

## Performance Tips

For high-throughput scenarios:

```yaml
hitorro:
  structured-logging:
    compression-type: snappy  # Enable compression
    batch-size: 32768  # Larger batches
    linger-ms: 50  # Allow batching time
    async-publishing: true  # Non-blocking
```

## Downstream Processing (Optional)

For production use, consider adding:

1. **Spark Consumer** - Read from Kafka, write to Iceberg tables
2. **Iceberg Tables** - Columnar storage with SQL access
3. **Trino** - Query logs with SQL

See the [structured-logger repository](https://github.com/geekychris/structured-logger) for complete setup.

## Documentation

For complete documentation, see:
- `hitorro-spring-boot/STRUCTURED_LOGGING.md` - Full documentation
- [geekychris/structured-logger](https://github.com/geekychris/structured-logger) - Original framework

## Troubleshooting

### Kafka Connection Errors

Check Kafka is running:
```bash
docker ps | grep kafka
```

Test connection:
```bash
docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### No Logs Appearing

1. Check structured logging is enabled in `application.yml`
2. Look for errors in application logs
3. Verify Kafka topic was created:
   ```bash
   docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
   ```

### Port Conflicts

If port 9092 is in use, change the Kafka port and update `application.yml`:

```yaml
hitorro:
  structured-logging:
    kafka-bootstrap-servers: localhost:9093  # Use different port
```
