# H2 Database Configuration and Console Guide

## Overview

The Hitorro Spring Boot Example application uses **H2 Database** in file-based (persistent) mode, with the **H2 Console** enabled for easy database management and SQL queries.

## Database Configuration

### File-Based Persistent Database

The database is stored as files in the project directory:

**Location**: `./data/hitorrodb.*`

Files created:
- `hitorrodb.mv.db` - Main database file (MVStore format)
- `hitorrodb.trace.db` - Trace/log file (if enabled)

**Configuration** (`application.yml`):

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/hitorrodb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: hitorro
```

**Connection Parameters**:
- `file:./data/hitorrodb` - File-based database (persistent across restarts)
- `MODE=MySQL` - MySQL compatibility mode for Hitorro
- `DB_CLOSE_DELAY=-1` - Keep database open after last connection closes
- `DB_CLOSE_ON_EXIT=FALSE` - Don't close database on JVM exit

**Credentials**:
- Username: `sa`
- Password: `hitorro`

### Hibernate Settings

```yaml
jpa:
  hibernate:
    ddl-auto: update  # Preserves data across restarts
  show-sql: true     # Logs SQL statements
```

## H2 Console - Web UI

### Accessing the H2 Console

1. **Start the application**:
   ```bash
   cd hitorro-example-springboot
   mvn spring-boot:run
   ```

2. **Open H2 Console in browser**:
   ```
   http://localhost:8080/h2-console
   ```

3. **Login Screen Configuration**:
   ```
   Saved Settings:   Generic H2 (Embedded)
   Setting Name:     Generic H2 (Embedded)
   Driver Class:     org.h2.Driver
   JDBC URL:         jdbc:h2:file:./data/hitorrodb
   User Name:        sa
   Password:         hitorro
   ```

4. **Click "Connect"**

### H2 Console Features

#### 1. **SQL Query Execution**

Run SQL queries directly in the console:

```sql
-- View all tables
SELECT * FROM INFORMATION_SCHEMA.TABLES;

-- Query Hitorro DMS objects
SELECT * FROM sysobject;

-- View document content
SELECT * FROM content;

-- Check version history
SELECT * FROM sysobject WHERE r_version_label IS NOT NULL;
```

#### 2. **Table Browser**

- Left panel shows all tables
- Click table name to:
  - View table structure
  - Browse data
  - Generate SELECT statement

#### 3. **Schema Visualization**

Click on table name → "Schema" to see:
- Column definitions
- Primary keys
- Foreign keys
- Indexes

#### 4. **Data Export/Import**

**Export data**:
```sql
-- Export to CSV
CALL CSVWRITE('./data/export.csv', 'SELECT * FROM sysobject');

-- Export entire schema
SCRIPT TO './data/backup.sql';
```

**Import data**:
```sql
-- Import from CSV
CREATE TABLE temp_import AS SELECT * FROM CSVREAD('./data/import.csv');
```

#### 5. **Query History**

- Previous queries saved in session
- Use up/down arrows to navigate history
- "History" button shows all recent queries

### Common SQL Queries for Hitorro DMS

#### View All Documents

```sql
SELECT 
    r_object_id,
    object_name,
    r_version_label,
    r_creation_date,
    r_modify_date
FROM sysobject
ORDER BY r_modify_date DESC;
```

#### Find Documents by Type

```sql
SELECT * FROM sysobject WHERE r_object_type = 'dm_document';
```

#### Check Version Tree

```sql
SELECT 
    r_object_id,
    object_name,
    r_version_label,
    i_chronicle_id,
    i_antecedent_id
FROM sysobject
WHERE i_chronicle_id = 'your_chronicle_id'
ORDER BY r_version_label;
```

#### View Content Information

```sql
SELECT 
    c.r_object_id as content_id,
    c.full_format,
    c.page_count,
    s.object_name,
    s.r_version_label
FROM content c
JOIN sysobject s ON c.parent_id = s.r_object_id;
```

#### Search by Name

```sql
SELECT * FROM sysobject 
WHERE object_name LIKE '%test%'
ORDER BY r_creation_date DESC;
```

#### Count Objects by Type

```sql
SELECT 
    r_object_type,
    COUNT(*) as count
FROM sysobject
GROUP BY r_object_type;
```

## Configuration Options

### Enable Remote Access

To allow H2 Console access from other machines:

```yaml
spring:
  h2:
    console:
      settings:
        web-allow-others: true  # WARNING: Security risk!
```

**Security Note**: Only enable for development/testing. Use firewall rules in production.

### Custom Console Path

Change the H2 Console URL:

```yaml
spring:
  h2:
    console:
      path: /admin/h2  # Access at http://localhost:8080/admin/h2
```

### Disable Console

For production, disable the console:

```yaml
spring:
  h2:
    console:
      enabled: false
```

### Enable Trace Logging

For debugging database issues:

```yaml
spring:
  h2:
    console:
      settings:
        trace: true
```

## Database File Management

### Backup Database

**While application is running**:
```sql
-- In H2 Console
BACKUP TO './backups/hitorro-backup.zip';
```

**While application is stopped**:
```bash
# Copy database files
cp ./data/hitorrodb.mv.db ./backups/hitorrodb-$(date +%Y%m%d).mv.db
```

### Restore Database

1. Stop the application
2. Replace database files:
   ```bash
   cp ./backups/hitorrodb-20260114.mv.db ./data/hitorrodb.mv.db
   ```
3. Start the application

### Reset Database

To start fresh (deletes all data):

1. Stop the application
2. Delete database files:
   ```bash
   rm -f ./data/hitorrodb.mv.db
   rm -f ./data/hitorrodb.trace.db
   ```
3. Start the application (will create new empty database)

### View Database Size

```bash
du -h ./data/hitorrodb.mv.db
```

Or in H2 Console:
```sql
SELECT * FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME = 'info.FILE_SIZE';
```

## Testing with H2

### Test Configuration

Tests use in-memory database (separate from main app):

**File**: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL
```

Tests don't affect the persistent database file.

### Run Tests

```bash
mvn test
```

## Troubleshooting

### Issue: "Database already in use"

**Cause**: Another process has the database locked.

**Solution**:
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or use different port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Issue: Can't connect to H2 Console

**Checklist**:
1. ✅ Application is running
2. ✅ URL is correct: `http://localhost:8080/h2-console`
3. ✅ Console is enabled in `application.yml`
4. ✅ JDBC URL matches: `jdbc:h2:file:./data/hitorrodb`

**Verify in logs**:
```
H2 console available at '/h2-console'
```

### Issue: Data disappears after restart

**Check `hibernate.ddl-auto` setting**:
- ✅ Use `update` to preserve data
- ❌ Don't use `create` (drops and recreates tables)
- ❌ Don't use `create-drop` (drops tables on shutdown)

### Issue: Corrupted database

**Recover**:
```bash
# Stop application

# Try H2 recovery tool
java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar \
  org.h2.tools.Recover -dir ./data -db hitorrodb

# Creates hitorrodb.h2.sql file with recovered data
```

## Security Best Practices

### Development

- ✅ Use H2 Console for rapid development
- ✅ Set simple password (`hitorro`)
- ✅ Enable SQL logging

### Production

- ❌ **Never use H2 in production for critical data**
- ❌ Disable H2 Console (`enabled: false`)
- ❌ Don't expose H2 port externally
- ✅ Use PostgreSQL, MySQL, or other production database
- ✅ Use strong passwords
- ✅ Disable SQL logging (performance)

## Alternative: In-Memory Mode

For development where persistence isn't needed:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:hitorro;MODE=MySQL;DB_CLOSE_DELAY=-1
  jpa:
    hibernate:
      ddl-auto: create  # OK for in-memory
```

Data is lost when application stops.

## IntelliJ IDEA Integration

### Database Tool Window

IntelliJ can connect directly to H2 database:

1. **View → Tool Windows → Database**
2. **Click "+" → Data Source → H2**
3. **Configure**:
   - URL: `jdbc:h2:file:./data/hitorrodb`
   - User: `sa`
   - Password: `hitorro`
4. **Test Connection**
5. **Apply**

Now you can:
- Browse tables visually
- Run queries with autocomplete
- Export data
- Generate SQL
- View ER diagrams

## Useful H2 Commands

### System Information

```sql
-- H2 version
SELECT H2VERSION();

-- Database settings
SELECT * FROM INFORMATION_SCHEMA.SETTINGS;

-- Session info
SELECT * FROM INFORMATION_SCHEMA.SESSIONS;
```

### Performance

```sql
-- Table sizes
SELECT 
    TABLE_NAME,
    ROW_COUNT_ESTIMATE
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'PUBLIC'
ORDER BY ROW_COUNT_ESTIMATE DESC;

-- Analyze table
ANALYZE TABLE sysobject;
```

### Maintenance

```sql
-- Compact database (reclaim space)
SHUTDOWN COMPACT;

-- Defragment (in H2 Console, then restart app)
SHUTDOWN DEFRAG;
```

## Resources

- [H2 Database Documentation](http://www.h2database.com/html/main.html)
- [H2 Console Tutorial](http://www.h2database.com/html/tutorial.html)
- [Spring Boot H2 Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.h2-web-console)

## Quick Reference Card

| Task | URL/Command |
|------|-------------|
| **Access H2 Console** | `http://localhost:8080/h2-console` |
| **JDBC URL** | `jdbc:h2:file:./data/hitorrodb` |
| **Username** | `sa` |
| **Password** | `hitorro` |
| **Database Files** | `./data/hitorrodb.mv.db` |
| **View All Tables** | `SELECT * FROM INFORMATION_SCHEMA.TABLES;` |
| **Backup Database** | `BACKUP TO './backups/backup.zip';` |
| **Export to CSV** | `CALL CSVWRITE('file.csv', 'SELECT * FROM table');` |
| **Reset Database** | Stop app → `rm ./data/hitorrodb.mv.db` → Start app |

---

**Pro Tip**: Keep the H2 Console open in a separate browser tab while developing. You can run queries and inspect data in real-time as your application runs!
