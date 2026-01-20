# H2 Console Configuration for Hitorro Example Application

## The Problem

When starting the HitorroExampleApplication and accessing the H2 console at `http://localhost:8080/h2-console`, the database appears empty with no tables visible.

## Root Cause

The issue occurs because **two separate database configurations exist**:

1. **Spring Boot JPA** (in `application.yml`)
   - Uses H2 database: `jdbc:h2:file:./data/hitorrodb`
   - Creates tables via Hibernate DDL auto
   
2. **Hitorro Service Framework** (HibernateService)
   - Reads config from `${HT_BIN}/config/database.json`
   - Was configured for MySQL: `jdbc:mysql://localhost/htcms`
   - Creates its own SessionFactory separate from Spring

**Result**: The Hitorro services (BaseDMSService, Store loading, etc.) create tables in their own database, while Spring Boot creates an empty H2 file. The H2 console connects to Spring's empty database.

## The Solution

Configure both Spring Boot and Hitorro service framework to use **the same H2 database**.

### Changes Made

#### 1. Updated `${HT_BIN}/config/database.json`

Added an `h2file` database configuration for H2 file-based storage:

```json
{
  "defaultdb": {
    "password": "htcms",
    "url": "jdbc:mysql://localhost/htcms?autoReconnect=true&useUnicode=yes&characterEncoding=UTF-8&serverTimezone=UTC",
    "username": "htcms"
  },
  "h2file": {
    "password": "hitorro",
    "url": "jdbc:h2:file:./data/hitorrodb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "username": "sa"
  },
  "testdb": {
    "password": "",
    "url": "jdbc:h2:mem:testdb;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "username": "sa"
  }
}
```

**Key points**:
- `defaultdb` remains MySQL (backward compatible)
- `h2file` is the new H2 file-based database config
- `testdb` is H2 in-memory for tests
- Uses `MODE=MySQL` for MySQL compatibility

#### 2. Updated `application.yml`

Added database override in hitorro-properties:

```yaml
hitorro-properties:
  network.disablelocalhostcheck: true
  rpc.enableclustermember: false
  defaultdb: h2file  # Use h2file database config
```

This tells HibernateService to use the `h2file` database configuration instead of the default MySQL.

### How to Access H2 Console

1. **Start the application**:
   ```bash
   cd /Users/chris/hitorro/hitorro-example-springboot
   mvn spring-boot:run
   ```

2. **Open H2 Console** in browser:
   ```
   http://localhost:8080/h2-console
   ```

3. **Connection Settings**:
   - **JDBC URL**: `jdbc:h2:file:./data/hitorrodb`
   - **User Name**: `sa`
   - **Password**: `hitorro`

4. **Click "Connect"**

### What You'll See

After connecting, you should see all Hitorro DMS tables:

**Core Tables**:
- `Store` - Content stores (file, blob, link)
- `DomainInfo` - Category domains
- `ContentType` - MIME types and content type definitions
- `Content` - Content files and renditions
- `Document`, `Post` - Versionable objects
- `NamedLongEntry` - Named counters

**Example Queries**:

```sql
-- List all stores
SELECT * FROM Store;

-- List all domain categories
SELECT * FROM DomainInfo;

-- Count content items
SELECT COUNT(*) FROM Content;

-- List all documents
SELECT guid, title, note FROM Document;
```

### Why MODE=MySQL?

The H2 database is configured with `MODE=MySQL` because:
- Hitorro's SQL queries and HQL were written for MySQL
- MySQL compatibility mode ensures SQL syntax works correctly
- Table and column naming conventions match MySQL expectations

### Troubleshooting

**If tables still don't appear**:

1. **Delete the old database**:
   ```bash
   rm -f ./data/hitorrodb.mv.db
   rm -f ./data/hitorrodb.trace.db
   ```

2. **Restart the application** - it will recreate the database with the correct schema

3. **Check the logs** for:
   ```
   === Hitorro Service Lifecycle: Starting ===
   ...
   IntegrationEventsContext.runEvent("stores")
   IntegrationEventsContext.runEvent("domaininfo")
   ```

4. **Verify database location**:
   ```bash
   ls -la ./data/hitorrodb*
   ```
   You should see `hitorrodb.mv.db` (the H2 database file)

### Alternative: Keep MySQL Configuration

If you prefer to use MySQL instead of H2:

1. **Install MySQL** and create database:
   ```sql
   CREATE DATABASE htcms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'htcms'@'localhost' IDENTIFIED BY 'htcms';
   GRANT ALL PRIVILEGES ON htcms.* TO 'htcms'@'localhost';
   ```

2. **Revert database.json** to MySQL config

3. **Update application.yml** to use MySQL instead of H2:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost/htcms?autoReconnect=true&useUnicode=yes&characterEncoding=UTF-8&serverTimezone=UTC
       username: htcms
       password: htcms
       driver-class-name: com.mysql.cj.jdbc.Driver
   ```

## Summary

The fix ensures that:
- ✅ Both Spring Boot and Hitorro service framework use the same database
- ✅ Tables created by HibernateService are visible in H2 console
- ✅ Integration events load data (stores, domaininfo) into the correct database
- ✅ All DMS functionality works with persistent data storage

After these changes, restart the application and the H2 console will show all tables with data! 🎉
