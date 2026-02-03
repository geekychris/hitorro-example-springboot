# KV Store Activation Guide

## Problem

The KV store code exists but **is not activated by default**. The `@ConditionalOnProperty` annotation on `KVStoreConfig` means the KV store bean will only be created when explicitly enabled in configuration.

## Current Status

✅ **Code exists and compiles** - All KV store integration code is present  
❌ **Not activated** - `hitorro.kvstore.enabled` is set to `true` but you need to verify RocksDB native libraries load  
❌ **Not used by React UI** - UI needs to be updated to use KV endpoints

## How to Activate

### 1. Configuration is Already Added

The `application.yml` now includes:

```yaml
hitorro:
  # KV Store Configuration - RocksDB-based document storage
  kvstore:
    enabled: true  # Enable KV store bean initialization
    path: ./data/kvstore  # Path to RocksDB database files
    compression: SNAPPY  # Compression type: NONE, SNAPPY, ZLIB, LZ4, ZSTD
    writeBufferSize: 67108864  # 64MB write buffer
    maxOpenFiles: 1000  # Maximum open file descriptors
```

### 2. Verify Startup

When you start the application with `mvn spring-boot:run`, you should see:

```
INFO  c.h.e.config.KVStoreConfig - Initializing RocksDB document store at: ./data/kvstore
INFO  c.h.e.config.KVStoreConfig - Document store initialized successfully
INFO  c.h.e.config.KVStoreConfig -   Path: ./data/kvstore
INFO  c.h.e.config.KVStoreConfig -   Compression: SNAPPY
INFO  c.h.e.config.KVStoreConfig -   Write Buffer: 67108864 bytes (64 MB)
```

**If you DON'T see these logs**, the KV store is not initializing. Check for errors.

### 3. Test KV Store is Working

#### Check Status Endpoint

```bash
curl http://localhost:8080/api/kvstore/stats
```

Expected response:
```json
{
  "status": "available",
  "type": "RocksDB",
  "isOpen": true,
  "latestSequenceNumber": 0
}
```

If you get `"status": "unavailable"`, the KV store bean was not created.

#### Test Put and Get

```bash
# Store a document
curl -X POST http://localhost:8080/api/kvstore/testdoc \
  -H "Content-Type: application/json" \
  -d '{"id": "testdoc", "title": "Test Document", "content": "Hello KV Store"}'

# Retrieve it
curl http://localhost:8080/api/kvstore/testdoc
```

Expected response:
```json
{
  "status": "success",
  "key": "testdoc",
  "document": {
    "id": "testdoc",
    "title": "Test Document",
    "content": "Hello KV Store"
  }
}
```

#### Test with Lucene Integration

```bash
# Index to BOTH Lucene and KV store
curl -X POST "http://localhost:8080/api/search/index/withkv?indexName=default" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "book",
    "id": {"did": "book001", "domain": "example"},
    "title": {"mls": {"text_en_s": "The Great Gatsby"}},
    "author": {"mls": {"text_en_s": "F. Scott Fitzgerald"}}
  }'

# Search with KV enrichment
curl "http://localhost:8080/api/search/query/withkv?indexName=default&query=Gatsby&fetchFromKV=true&batchSize=50"
```

The response should include the **full document** from KV store, not just indexed fields.

## Troubleshooting

### Issue: "KV store not configured" in API responses

**Cause:** The `documentStore` bean is null in controllers.

**Solution:**
1. Check `application.yml` has `hitorro.kvstore.enabled: true`
2. Restart the application
3. Check startup logs for KV store initialization messages
4. Look for any exceptions during bean creation

### Issue: RocksDB native library errors

**Error messages like:**
```
java.lang.UnsatisfiedLinkError: no rocksdbjavajni in java.library.path
```

**Cause:** RocksDB 10.4.2 requires platform-specific native libraries.

**Solution:**
The `hitorro-kvstore` module already includes platform-specific dependencies. If you still get errors:

1. Check your platform:
   ```bash
   mvn help:system | grep "os.name\|os.arch"
   ```

2. Verify the appropriate RocksDB JAR is in dependencies:
   ```bash
   mvn dependency:tree | grep rocksdb
   ```

3. You should see something like:
   ```
   [INFO] |  +- org.rocksdb:rocksdbjni:jar:10.4.2:compile
   [INFO] |  +- org.rocksdb:rocksdbjni:jar:osx:10.4.2:runtime
   ```

See `hitorro-kvstore/ROCKSDB_VERSION_GUIDE.md` for details.

### Issue: Breakpoints not hit in KV store code

**Possible causes:**

1. **KV store not enabled** - Check `application.yml`
2. **Using wrong endpoints** - Use `/api/search/index/withkv` not `/api/search/index`
3. **React UI not updated** - Default UI uses regular endpoints

**Debugging steps:**

1. Set breakpoint in `KVStoreConfig.documentStore()` method - should hit on startup
2. Set breakpoint in `KVStoreController.stats()` method
3. Call: `curl http://localhost:8080/api/kvstore/stats`
4. Breakpoint should hit

If `KVStoreConfig.documentStore()` breakpoint is **never hit**, the configuration class is not being loaded. Check:
- `@ConditionalOnProperty` is satisfied
- No bean creation exceptions in logs
- Spring is scanning the `com.hitorro.example.config` package

## React UI Integration

The React UI **does NOT use KV store by default**. To enable:

### Option 1: Manual API Testing

Use the Swagger UI to test KV endpoints:
```
http://localhost:8080/swagger-ui.html
```

Look for "KV Store" section with endpoints:
- `GET /api/kvstore/{key}`
- `POST /api/kvstore/{key}`
- `DELETE /api/kvstore/{key}`
- `POST /api/kvstore/batch`
- `GET /api/kvstore/stats`

### Option 2: Update React UI

Follow the instructions in `KVSTORE_UI_FEATURES.md` to add KV store UI controls to the Search page.

Key changes needed:
1. Add state for `useKVStore`, `fetchFromKV`, `kvBatchSize`
2. Query `/api/kvstore/stats` to check availability
3. Use `/api/search/index/withkv` when checkbox is enabled
4. Use `/api/search/query/withkv` with `fetchFromKV` parameter

## Verification Checklist

Before expecting KV store to work:

- [ ] `hitorro.kvstore.enabled: true` in `application.yml`
- [ ] Application starts without errors
- [ ] Startup logs show "Initializing RocksDB document store"
- [ ] `curl http://localhost:8080/api/kvstore/stats` returns `"status": "available"`
- [ ] Directory `./data/kvstore` exists after startup
- [ ] Can successfully PUT and GET a test document
- [ ] Using KV-specific endpoints (`/withkv` suffix)

## Performance Notes

When KV store is active:

**Indexing with KV storage:**
- Documents stored in BOTH Lucene (for search) and RocksDB (for retrieval)
- Slightly slower indexing (dual writes)
- More disk space (data stored twice)

**Searching with KV enrichment:**
- Search in Lucene (fast)
- Batch fetch from RocksDB (configurable batch size)
- Returns full documents (not just indexed fields)
- Slower than Lucene-only search but much more complete data

**Recommended settings:**
- Batch size: 50-100 for most use cases
- Use KV enrichment when you need complete documents
- Skip KV enrichment for facet-only or count queries

## Configuration Reference

All available properties:

```yaml
hitorro:
  kvstore:
    enabled: true              # Master switch (required)
    path: ./data/kvstore       # Database location
    compression: SNAPPY        # NONE, SNAPPY, ZLIB, LZ4, ZSTD
    writeBufferSize: 67108864  # 64MB default
    maxOpenFiles: 1000         # File descriptor limit (not used yet)
```

Default values are in `KVStoreConfig.java` via `@Value` annotations with defaults.
