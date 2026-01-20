# Database Configuration for Hitorro Spring Boot

## Overview

When running Hitorro in Spring Boot, **Spring's DataSource is the single source of truth** for database configuration.

The Spring Boot integration automatically provides database configuration from `application.yml` to Hitorro's `HibernateService` through a **factory pattern**, eliminating the need to configure the database in two places.

### How It Works

1. **Configure once** in `application.yml` (Spring DataSource)
2. **`SpringDatabaseConfigProvider`** automatically provides this to Hitorro
3. **Both Spring JPA and Hitorro HibernateService** use the same database
4. **H2 Console** shows all tables from both frameworks

## Configuration

### Single Source of Truth: `application.yml`

Just configure your database in Spring Boot's `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/hitorrodb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: hitorro
```

That's it! Hitorro's `HibernateService` will automatically use this configuration.

### Switching to MySQL

To use MySQL instead:

1. **Install and start MySQL**:
   ```bash
   brew install mysql
   brew services start mysql
   mysql -u root
   CREATE DATABASE htcms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **Update `application.yml`**:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost/htcms?serverTimezone=UTC
       username: htcms
       password: htcms
       driver-class-name: com.mysql.cj.jdbc.Driver
   ```

That's all - both Spring and Hitorro will use MySQL automatically.

### How It Works (Factory Pattern)

The Spring Boot integration uses a **factory pattern** to provide database configuration:

1. **`DatabaseConfigProvider`** (in `hitorro-basedms`) - Abstract factory with default implementation that reads from `JVSProperties`
2. **`SpringDatabaseConfigProvider`** (in Spring Boot autoconfigure) - Spring implementation that provides configuration from Spring's `DataSource`
3. **`HibernateService`** - Uses `DatabaseConfigProvider.getProvider()` to get configuration

When running in Spring Boot:
- **Main database (`defaultdb`)** → Provided by Spring's DataSource
- **Test database (`testdb`)** → Falls back to `database.json` (for backward compatibility)
- **Other databases** → Falls back to `database.json`

## Using the H2 Console

Once both configurations point to the same H2 database:

1. **Start the application**:
   ```bash
   cd /Users/chris/hitorro/hitorro-example-springboot
   mvn spring-boot:run
   ```

2. **Access H2 Console**: http://localhost:8080/h2-console

3. **Connection Settings**:
   - **JDBC URL**: `jdbc:h2:file:./data/hitorrodb`
   - **User Name**: `sa`
   - **Password**: `hitorro`

4. **Click "Connect"**

### What You'll See

All DMS tables including:
- **Store** (4 stores: fs_store1, blob, link, unmanaged_store1)
- **DomainInfo** (category domains: images, links, attachments, etc.)
- **ContentType** (MIME types)
- **Content**, **Document**, **Post** (DMS entities)
- **NamedLongEntry** and other base tables

## Testing

Tests use a separate in-memory H2 database (`testdb`). The `SpringDatabaseConfigProvider` automatically falls back to `database.json` for the `testdb` key, so tests work without any special configuration.

```bash
# Run all DMS tests
mvn test -Dtest=HitorroDMSIntegrationTest
mvn test -Dtest=HitorroDMSDocumentVersioningTest
```

Tests use this entry from `${HT_BIN}/config/database.json`:

```json
{
  "testdb": {
    "password": "",
    "url": "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "username": "sa"
  }
}
```

## Troubleshooting

### H2 Console Shows Empty Database

**Problem**: The H2 console connects but shows no tables.

**Cause**: Database file path mismatch.

**Solution**: Make sure you're using the same path in the H2 console connection as in `application.yml`. For example, if `application.yml` has `jdbc:h2:file:./data/hitorrodb`, connect with that same URL.

### Tests Fail

**Problem**: Tests fail with database errors.

**Cause**: Missing `testdb` configuration in `database.json`.

**Solution**: Ensure `/Users/chris/hitorro/config/database.json` has a `testdb` entry for in-memory H2.

### Can't See New Data in H2 Console

**Problem**: Added data via application but don't see it in H2 console.

**Cause**: Need to refresh in H2 console.

**Solution**: Click "Disconnect" then "Connect" again in H2 console, or refresh the SQL query.

### Want to Use Standalone Hitorro (Non-Spring)

**Problem**: Running Hitorro without Spring Boot.

**Solution**: The factory pattern automatically falls back to the default `DatabaseConfigProvider` which reads from `database.json`. Just configure `database.json` as usual - no Spring needed.

## Architecture: Factory Pattern

The implementation uses the **Factory Pattern** for maximum flexibility:

```
┌─────────────────────────────────────┐
│   HibernateService                  │
│   (reads database config)           │
└──────────────┬──────────────────────┘
               │ uses
               ▼
┌─────────────────────────────��───────┐
│   DatabaseConfigProvider            │
│   (abstract factory)                │
└──────────────┬──────────────────────┘
               │
       ┌───────┴────────┐
       │                │
       ▼                ▼
┌──────────────┐  ┌──────────────────────┐
│   Default    │  │   Spring             │
│   Provider   │  │   Provider           │
│              │  │                      │
│ Reads from   │  │ Reads from           │
│ database.json│  │ Spring DataSource    │
└──────────────┘  └──────────────────────┘
```

**Benefits:**
- ✅ **Spring Boot**: Automatic - Spring's DataSource is used
- ✅ **Standalone Hitorro**: Works as before - `database.json` is used
- ✅ **Testing**: Flexible - can override per test if needed
- ✅ **No Code Changes**: Existing Hitorro apps work without modification

## Summary

✅ **Single Source of Truth**: Configure database once in `application.yml` (for Spring Boot)  
✅ **No Duplication**: Don't need to configure `database.json` for main database  
✅ **Tests Still Work**: Tests use `testdb` from `database.json`  
✅ **H2 Console Works**: Both Spring and Hitorro use same database automatically  
✅ **Backward Compatible**: Standalone Hitorro apps still use `database.json`  
✅ **Flexible**: Easy to switch between H2, MySQL, PostgreSQL, etc.  

For more information, see:
- **API Testing**: `API_TESTING_GUIDE.md`
- **H2 Console Setup**: This document
