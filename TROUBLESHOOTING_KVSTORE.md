# KV Store Troubleshooting Guide

## Problem: Breakpoints Not Hit, Search Returns No Results

### Checklist

Run through these steps in order:

#### 1. Is the React UI Built?

The React changes won't work until you rebuild:

```bash
cd /Users/chris/hitorro/hitorro-example-springboot/react-app
npm run build
cp -r dist/* ../src/main/resources/static/
```

Then **restart Spring Boot** to load the new UI.

#### 2. Is the KV Store Enabled?

Check `src/main/resources/application.yml`:

```yaml
hitorro:
  kvstore:
    enabled: true  # <-- Must be true
```

#### 3. Is Spring Boot Running?

Restart it to pick up the new React UI and configuration:

```bash
cd /Users/chris/hitorro/hitorro-example-springboot
mvn spring-boot:run
```

Watch for these logs:
```
INFO  c.h.e.config.KVStoreConfig - Initializing RocksDB document store at: ./data/kvstore
INFO  c.h.e.config.KVStoreConfig - Document store initialized successfully
```

**If you DON'T see these**, the KV store isn't starting. Check for errors in the logs.

#### 4. Does the UI Show KV Store as Available?

Open http://localhost:8080, go to the **Search** tab.

Look at the stats panel. You should see:
```
KV Store: ✓ Available (in green)
```

If it shows "✗ Not Available", the UI can't reach the KV store. Check:

```bash
curl http://localhost:8080/api/kvstore/stats
# Should return: {"status":"available","type":"RocksDB",...}
```

#### 5. Are There Documents in the Index?

Check how many documents are indexed:

```bash
curl "http://localhost:8080/api/search/stats?indexName=default"
# Look at "numDocuments"
```

**If numDocuments = 0**, you need to index some documents first!

#### 6. Were Documents Indexed WITH KV Store?

This is **critical**: If documents were indexed BEFORE you enabled KV store, they're only in Lucene, not in RocksDB.

**Solution**: Clear the index and re-index with KV store enabled:

```bash
# Clear the index
curl -X DELETE "http://localhost:8080/api/search/index?indexName=default"

# Index a document to BOTH Lucene and KV store
curl -X POST "http://localhost:8080/api/search/index/withkv?indexName=default" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "test_doc",
    "id": {"did": "test001", "domain": "test"},
    "title": {"mls": {"text_en_s": "Test Document"}},
    "content": {"mls": {"text_en_s": "This is a test."}}
  }'
```

#### 7. Test the Full Flow

Run the test script:

```bash
cd /Users/chris/hitorro/hitorro-example-springboot
./test-kvstore-integration.sh
```

This will:
1. Check KV store status
2. Index a document to both Lucene and KV
3. Fetch directly from KV store
4. Search without KV enrichment
5. Search WITH KV enrichment

Watch the output. If any step fails, that's where the problem is.

#### 8. Set Breakpoints

**Before running the test**, set breakpoints in your IDE:

**For Indexing:**
- `SearchController.indexWithKVStore()` - Line ~207
- `KVStoreController.put()` - Line ~108

**For Searching:**
- `SearchController.searchWithKV()` - Line ~431
- `KVDocumentFetcher.fill()` - Line ~75

Then run the test script. Breakpoints should hit.

#### 9. Check Browser Console

Open browser dev tools (F12), go to the **Network** tab, and search.

Look for the request to:
```
/api/search/query/withkv?indexName=default&query=*:*&fetchFromKV=true&batchSize=50
```

**If you see `/api/search/query` instead** (without `/withkv`), the React UI isn't detecting KV store as available.

#### 10. Common Issues

**Issue: "KV store not configured" error**

Means `documentStore` bean is null in the controller.

**Fix:**
1. Verify `hitorro.kvstore.enabled: true` in `application.yml`
2. Restart Spring Boot
3. Check for bean creation errors in logs

**Issue: Search returns 0 documents**

Means either:
- No documents in the index
- Wrong query
- Documents indexed without KV store

**Fix:**
```bash
# Check document count
curl "http://localhost:8080/api/search/stats?indexName=default"

# Try the *:* query
curl "http://localhost:8080/api/search/query?indexName=default&query=*:*"

# If that returns docs but KV query doesn't, documents aren't in KV store
# Re-index them using the /withkv endpoint
```

**Issue: Breakpoint not hit in KVDocumentFetcher.fill()**

Possible causes:
1. Search returned 0 results (nothing to fetch)
2. `fetchFromKV` parameter is false
3. React UI checkbox "Fetch from KV store" is unchecked
4. Using multi-index search (not supported with KV yet)

**Fix:**
- Use single index search
- Ensure checkbox is checked in UI
- Use the test script which explicitly sets `fetchFromKV=true`

## Quick Diagnostic Commands

```bash
# 1. Is KV store available?
curl http://localhost:8080/api/kvstore/stats

# 2. How many documents in index?
curl "http://localhost:8080/api/search/stats?indexName=default"

# 3. Can I fetch a specific doc from KV?
# (Replace test001 with an actual document ID)
curl http://localhost:8080/api/kvstore/test001

# 4. Regular search (Lucene only)
curl "http://localhost:8080/api/search/query?indexName=default&query=*:*"

# 5. Search with KV enrichment
curl "http://localhost:8080/api/search/query/withkv?indexName=default&query=*:*&fetchFromKV=true&batchSize=50"
```

## Rebuild Everything from Scratch

If nothing works, start fresh:

```bash
cd /Users/chris/hitorro/hitorro-example-springboot

# 1. Stop Spring Boot (Ctrl+C)

# 2. Clean everything
rm -rf data/kvstore data/indexes target react-app/dist

# 3. Rebuild React UI
cd react-app
npm run build
cp -r dist/* ../src/main/resources/static/
cd ..

# 4. Rebuild and start Spring Boot
mvn clean spring-boot:run
```

Look for initialization logs. Once running:

```bash
# 5. Run the test script
./test-kvstore-integration.sh
```

## Expected Behavior

When everything is working correctly:

1. **Startup**: KV store initialization logs appear
2. **UI**: Shows "✓ Available" in green
3. **Index**: Button says "Index to Lucene + KV Store"
4. **Search**: Button says "Search with KV Enrichment"
5. **Breakpoints**: Hit when indexing and searching
6. **Results**: Full JSON documents returned with all fields

## Still Not Working?

Check these files for correctness:

```bash
# 1. React UI has KV integration
grep -n "useKVStore" react-app/src/pages/SearchPage.tsx
# Should show lines where useKVStore state is used

# 2. Config has KV enabled
grep -A5 "kvstore:" src/main/resources/application.yml
# Should show enabled: true

# 3. Controllers have KV endpoints
grep -n "/query/withkv" src/main/java/com/hitorro/example/controller/SearchController.java
# Should show the endpoint definition

# 4. KVStoreConfig is conditional
grep -n "ConditionalOnProperty" src/main/java/com/hitorro/example/config/KVStoreConfig.java
# Should show the annotation
```

## Debug Logging

Add this to `application.yml` for more verbose logging:

```yaml
logging:
  level:
    com.hitorro.example.config.KVStoreConfig: DEBUG
    com.hitorro.example.controller.SearchController: DEBUG
    com.hitorro.example.search: DEBUG
    com.hitorro.kvstore: DEBUG
```

Then restart and watch the logs during indexing and searching.
