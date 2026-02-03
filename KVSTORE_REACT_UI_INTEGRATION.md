# KV Store React UI Integration - Complete

## Summary

The React Search Page now **automatically uses the KV store** when it's available. All features are enabled by default for the best user experience.

## What Was Changed

### 1. Added State Variables
```typescript
const [useKVStore, setUseKVStore] = useState(true);       // Default: enabled
const [fetchFromKV, setFetchFromKV] = useState(true);     // Default: fetch from KV
const [kvBatchSize, setKvBatchSize] = useState(50);       // Default batch size
```

### 2. Added KV Store Status Query
- Queries `/api/kvstore/stats` on page load
- Returns `{ status: 'available' }` when KV store is running
- Gracefully handles when KV store is not configured

### 3. Updated Mutations

**Index Mutation:**
- Automatically uses `/api/search/index/withkv` when KV store is enabled
- Falls back to `/api/search/index` when unavailable

**Search Mutation:**
- Automatically uses `/api/search/query/withkv` with `fetchFromKV=true` and batch size
- Falls back to regular search when KV store unavailable

**Sample Documents:**
- Uses `/api/search/index/batch/withkv` when enabled
- Shows appropriate success message

### 4. Added UI Elements

**Stats Panel:**
- Shows KV Store status with green checkmark (✓ Available) or gray X (✗ Not Available)
- Uses HardDrive icon

**Index Section:**
- Checkbox to enable/disable KV store for indexing (default: ON)
- Only visible when KV store is available
- Button text changes to "Index to Lucene + KV Store" when enabled

**Search Section:**
- Blue-bordered "KV Store Options" panel (only visible when available)
- Checkbox to enable/disable fetching from KV store (default: ON)
- Dynamic help text explaining current mode
- Batch size input (1-200, default 50) with recommendations
- Button text changes to "Search with KV Enrichment" when enabled

**Max Results:**
- Increased from 100 to 1000 to support larger result sets

## User Experience

### Default Behavior (KV Store Available)

1. **Page loads** → Checks KV store status automatically
2. **Stats show** → "✓ Available" in green
3. **Index a document** → Goes to both Lucene and RocksDB (checkbox checked)
4. **Search** → Returns full documents from RocksDB in batches of 50

### Default Behavior (KV Store Not Available)

1. **Page loads** → KV store check returns unavailable
2. **Stats show** → "✗ Not Available" in gray
3. **No KV UI elements** → Checkboxes and options panel are hidden
4. **Everything works** → Regular Lucene indexing and search

### User Can Toggle

- **Uncheck "Store in KV Store"** → Index only to Lucene
- **Uncheck "Fetch from KV store"** → Search returns only indexed fields (faster)
- **Adjust batch size** → Tune performance for large result sets

## Testing the Integration

### 1. Start the Application

```bash
cd /Users/chris/hitorro/hitorro-example-springboot
mvn spring-boot:run
```

Look for:
```
INFO  c.h.e.config.KVStoreConfig - Initializing RocksDB document store at: ./data/kvstore
INFO  c.h.e.config.KVStoreConfig - Document store initialized successfully
```

### 2. Open the UI

```
http://localhost:8080
```

Navigate to the **Search** tab.

### 3. Verify KV Store Status

In the stats panel at the top, you should see:
- **KV Store: ✓ Available** (in green)

If you see "✗ Not Available", the KV store didn't initialize. Check:
- `application.yml` has `hitorro.kvstore.enabled: true`
- No errors in startup logs
- RocksDB native libraries loaded correctly

### 4. Test Indexing with KV Store

1. **Load Sample Documents** button (checkbox will be checked)
2. Alert should say: "Sample documents indexed to Lucene and KV store!"
3. Set a breakpoint in `KVStoreController.batchPut()` or `KVDocumentFetcher.fill()`

### 5. Test Searching with KV Enrichment

1. Enter query: `*:*` or `title.mls:lucene`
2. **KV Store Options panel** should be visible with:
   - ✓ Fetch full documents from KV store (batch mode)
   - Batch Size: 50
3. Click **Search with KV Enrichment**
4. Breakpoint in `KVDocumentFetcher.fill()` should hit
5. Results should show **complete JSON** from KV store

### 6. Compare Performance

**Test 1: With KV Enrichment (default)**
- Check "Fetch from KV store"
- Search for `*:*` with max results 100
- Note response time and document completeness

**Test 2: Without KV Enrichment**
- Uncheck "Fetch from KV store"
- Same search
- Should be faster but return only indexed fields

## What Happens Behind the Scenes

### When You Index a Document with KV Store Enabled

1. React calls `POST /api/search/index/withkv?indexName=default`
2. `SearchController.indexWithKVStore()` receives request
3. Document is:
   - Stored in RocksDB with document ID as key
   - Indexed in Lucene for search
4. Response confirms both operations succeeded

### When You Search with KV Enrichment

1. React calls `GET /api/search/query/withkv?query=*:*&fetchFromKV=true&batchSize=50`
2. `SearchController.searchWithKVEnrichment()`:
   - Searches Lucene for matching document IDs
   - Creates `SearchResultIterator` wrapping the search results
   - Chains `KVDocumentFetcher` using `FillBufferIterator` pattern
3. `KVDocumentFetcher.fill()`:
   - Collects 50 document IDs at a time (batch)
   - Calls `kvStore.batchGet(keys)` once for the batch
   - Parses JSON for each retrieved document
4. Response contains full documents with all fields

## Breakpoint Locations for Debugging

Set breakpoints here to see KV store in action:

### Initialization
- `KVStoreConfig.documentStore()` - Bean creation (hit once on startup)

### Indexing
- `SearchController.indexWithKVStore()` line 207 - When indexing single doc
- `SearchController.batchIndexWithKVStore()` line 266 - When batch indexing
- `KVStoreController.batchPut()` line 186 - Batch put operation

### Searching
- `SearchController.searchWithKVEnrichment()` line 422 - Search entry point
- `KVDocumentFetcher.fill()` line 75 - Batch fetch from KV store
- `RocksDBStore.batchGet()` - Low-level RocksDB operation

## Files Modified

```
react-app/src/pages/SearchPage.tsx - All KV store UI integration
src/main/resources/application.yml  - Added KV store configuration
src/main/java/.../config/KVStoreConfig.java - Made conditional on property
```

## Configuration

In `application.yml`:

```yaml
hitorro:
  kvstore:
    enabled: true              # Must be true for UI features to work
    path: ./data/kvstore       # RocksDB database location
    compression: SNAPPY        # Compression algorithm
    writeBufferSize: 67108864  # 64MB
```

## Troubleshooting

### Issue: KV Store shows as "Not Available"

**Check:**
1. Look for initialization logs on startup
2. Verify `hitorro.kvstore.enabled: true` in `application.yml`
3. Check for exceptions in logs
4. Verify directory `./data/kvstore` exists and is writable

**Test manually:**
```bash
curl http://localhost:8080/api/kvstore/stats
# Should return: {"status":"available",...}
```

### Issue: Breakpoints not hit during search

**Possible causes:**
1. "Fetch from KV store" checkbox is unchecked
2. Documents were indexed without KV store
3. Search is using multi-index mode (not supported with KV yet)

**Solution:**
- Ensure checkbox is checked
- Re-index documents with "Store in KV Store" enabled
- Use single index search

### Issue: Search returns incomplete documents

**Possible causes:**
1. Documents were indexed to Lucene only (not KV store)
2. "Fetch from KV store" is unchecked

**Solution:**
- Re-index with KV store checkbox enabled
- Check the checkbox in KV Store Options panel

## Performance Tuning

### Batch Size Guidelines

- **Small result sets (< 50 docs)**: 20-50
- **Medium result sets (50-200 docs)**: 50-100  
- **Large result sets (200+ docs)**: 100-200

### When to Use KV Enrichment

**Use KV enrichment when:**
- You need complete documents with all fields
- Displaying full document details
- Exporting data
- Verifying data integrity

**Skip KV enrichment when:**
- You only need facet counts
- Only displaying titles/summaries
- Performance is critical
- You only indexed specific fields

## Next Steps

The KV store is now fully integrated! To use it:

1. ✅ Start the application with `mvn spring-boot:run`
2. ✅ Open http://localhost:8080
3. ✅ Navigate to Search tab
4. ✅ Verify "✓ Available" in stats
5. ✅ Index some documents (checkbox will be checked by default)
6. ✅ Search with KV enrichment enabled (checkbox will be checked by default)
7. ✅ Set breakpoints and observe the flow

**Everything works by default!** The UI automatically uses KV store when available.
