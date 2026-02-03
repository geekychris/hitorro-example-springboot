# KV Store Integration Guide

## Overview

The Hitorro Spring Boot example now supports **dual storage** - documents can be indexed in Lucene for fast full-text search AND stored in RocksDB for complete JSON retrieval. This provides the best of both worlds:

- **Lucene**: Fast full-text search, faceting, and filtering
- **RocksDB KV Store**: Complete document storage with batch retrieval

### Key Features

✅ **Batch Operations** - Uses `FillBufferIterator` pattern for efficient batch fetching  
✅ **Transparent Integration** - Search results can optionally enrich from KV store  
✅ **Dual Indexing** - One API call indexes to both Lucene and RocksDB  
✅ **Latest RocksDB** - Uses RocksDB 10.4.2 with platform-specific native libraries  
✅ **Spring Boot Configuration** - Fully configurable via application properties  

## Architecture

### Storage Flow

```
Document → Index to Lucene (for search) 
        ↓
        → Store in RocksDB (full JSON)
```

### Search Flow

```
Query → Lucene Search (fast) → Document IDs
      ↓
      → Batch Fetch from RocksDB (50 at a time)
      ↓
      → Full JSON Documents
```

### FillBufferIterator Pattern

The integration uses the `FillBufferIterator` pattern to batch-fetch documents:

1. Search returns document IDs from Lucene
2. IDs are collected into batches (default: 50)
3. Each batch is fetched from RocksDB in a single operation
4. Results are streamed back to the client

This is **much more efficient** than fetching documents one-by-one!

## Configuration

### Application Properties

Add to `application.properties`:

```properties
# KV Store Configuration
hitorro.kvstore.path=/path/to/kvstore
hitorro.kvstore.compression=SNAPPY
hitorro.kvstore.writeBufferSize=67108864
hitorro.kvstore.maxOpenFiles=1000
```

### Compression Options

- `NONE` - No compression (fastest write, largest size)
- `SNAPPY` - Good balance (default, recommended)
- `LZ4` - Fast compression
- `LZ4HC` - High compression ratio
- `ZSTD` - Best compression, slower
- `ZLIB` - Good compression

## API Endpoints

### 1. Index Document to Both Systems

**Endpoint:** `POST /api/search/index/withkv`

**Description:** Indexes document to Lucene AND stores in KV store.

**Request:**
```bash
curl -X POST http://localhost:8080/api/search/index/withkv \
  -H "Content-Type: application/json" \
  -d '{
    "id": {"did": "doc123"},
    "title": {"mls": "Quick Brown Fox"},
    "content": {"mls": "The quick brown fox jumps over the lazy dog"},
    "tags": ["example", "test"]
  }'
```

**Response:**
```json
{
  "status": "success",
  "message": "Document indexed and stored",
  "indexName": "default",
  "documentId": "doc123",
  "storedInKV": true,
  "indexedInLucene": true
}
```

### 2. Batch Index with KV Store

**Endpoint:** `POST /api/search/index/batch/withkv`

**Description:** Batch indexes multiple documents to both systems efficiently.

**Request:**
```bash
curl -X POST http://localhost:8080/api/search/index/batch/withkv \
  -H "Content-Type: application/json" \
  -d '[
    {"id": {"did": "doc1"}, "title": {"mls": "First Document"}},
    {"id": {"did": "doc2"}, "title": {"mls": "Second Document"}},
    {"id": {"did": "doc3"}, "title": {"mls": "Third Document"}}
  ]'
```

**Response:**
```json
{
  "status": "success",
  "indexed": 3,
  "indexName": "default",
  "documentIds": ["doc1", "doc2", "doc3"],
  "storedInKV": true,
  "indexedInLucene": true
}
```

### 3. Search with KV Enrichment

**Endpoint:** `GET /api/search/query/withkv`

**Description:** Searches Lucene and fetches full documents from KV store in batches.

**Parameters:**
- `query` - Lucene query string
- `maxResults` - Maximum results to return (default: 10)
- `fetchFromKV` - Whether to fetch from KV store (default: true)
- `batchSize` - Batch size for KV fetches (default: 50)
- `facets` - Comma-separated facet fields (optional)

**Request:**
```bash
curl "http://localhost:8080/api/search/query/withkv?query=fox&maxResults=10&fetchFromKV=true&batchSize=50"
```

**Response:**
```json
{
  "query": "fox",
  "indexName": "default",
  "totalHits": 1,
  "fetchedFromKV": true,
  "returned": 1,
  "documents": [
    {
      "id": {"did": "doc123"},
      "title": {"mls": "Quick Brown Fox"},
      "content": {"mls": "The quick brown fox jumps over the lazy dog"},
      "tags": ["example", "test"]
    }
  ]
}
```

### 4. Direct KV Store Operations

#### Get Document

```bash
curl http://localhost:8080/api/kvstore/doc123
```

#### Put Document

```bash
curl -X POST http://localhost:8080/api/kvstore/doc456 \
  -H "Content-Type: application/json" \
  -d '{"title": "Direct Store", "data": "value"}'
```

#### Delete Document

```bash
curl -X DELETE http://localhost:8080/api/kvstore/doc456
```

#### Batch Put

```bash
curl -X POST http://localhost:8080/api/kvstore/batch \
  -H "Content-Type: application/json" \
  -d '{
    "key1": "{\"data\": \"value1\"}",
    "key2": "{\"data\": \"value2\"}",
    "key3": "{\"data\": \"value3\"}"
  }'
```

#### Get Statistics

```bash
curl http://localhost:8080/api/kvstore/stats
```

## Usage Examples

### Example 1: Index and Search with Full Documents

```bash
# 1. Index documents
curl -X POST http://localhost:8080/api/search/index/batch/withkv \
  -H "Content-Type: application/json" \
  -d '[
    {
      "id": {"did": "product1"},
      "name": {"mls": "Laptop Computer"},
      "description": {"mls": "High performance laptop with 16GB RAM"},
      "price": 1299.99,
      "category": "electronics"
    },
    {
      "id": {"did": "product2"},
      "name": {"mls": "Wireless Mouse"},
      "description": {"mls": "Ergonomic wireless mouse with precision tracking"},
      "price": 29.99,
      "category": "accessories"
    }
  ]'

# 2. Search - returns only indexed fields (fast)
curl "http://localhost:8080/api/search/query?query=laptop&maxResults=10"

# 3. Search with KV enrichment - returns FULL documents
curl "http://localhost:8080/api/search/query/withkv?query=laptop&maxResults=10&fetchFromKV=true"
```

### Example 2: High-Volume Batch Processing

```bash
# Generate test documents
for i in {1..1000}; do
  echo "{\"id\":{\"did\":\"doc$i\"},\"title\":{\"mls\":\"Document $i\"},\"content\":{\"mls\":\"Content for document number $i\"}}"
done | jq -s '.' > batch_docs.json

# Batch index
curl -X POST http://localhost:8080/api/search/index/batch/withkv \
  -H "Content-Type: application/json" \
  -d @batch_docs.json

# Search with large batch size for efficiency
curl "http://localhost:8080/api/search/query/withkv?query=*:*&maxResults=1000&batchSize=100"
```

### Example 3: Compare Search Modes

```bash
# Mode 1: Search only (fastest - no KV fetch)
time curl "http://localhost:8080/api/search/query?query=laptop"

# Mode 2: Search with KV fetch (full documents)
time curl "http://localhost:8080/api/search/query/withkv?query=laptop&fetchFromKV=true&batchSize=50"

# Mode 3: Individual KV fetches (slowest - don't do this!)
# For comparison only - the batch approach is MUCH faster
```

## Performance Tips

### 1. Batch Size Tuning

```properties
# Small batch (low memory, more round-trips)
?batchSize=10

# Medium batch (balanced - default)
?batchSize=50

# Large batch (fewer round-trips, more memory)
?batchSize=200
```

**Recommendation:** Start with 50, increase if you have large result sets and sufficient memory.

### 2. When to Use KV Store

✅ **Use KV Store When:**
- Need complete document content
- Documents have large fields not indexed
- Want to avoid storing everything in Lucene
- Need fast key-based retrieval

❌ **Don't Use KV Store When:**
- Only need search-indexed fields
- Doing facet-only queries
- Results are small and fully indexed

### 3. Compression Trade-offs

| Compression | Write Speed | Read Speed | Size | Use Case |
|-------------|-------------|------------|------|----------|
| NONE | Fastest | Fastest | Largest | SSDs, speed-critical |
| SNAPPY | Fast | Fast | Good | **Recommended default** |
| LZ4 | Fast | Fast | Good | Alternative to SNAPPY |
| ZSTD | Slower | Medium | Best | Cold storage, archives |

## Code Examples

### Using the Iterator Pattern Directly

```java
@Autowired
@Qualifier("documentStore")
private KVStore documentStore;

@Autowired
private IndexManager indexManager;

public void searchWithEnrichment() throws Exception {
    // 1. Search Lucene
    SearchResult result = indexManager.search("default", "laptop", 0, 100, null);
    
    // 2. Create enrichment iterator
    SearchResultIterator iterator = new SearchResultIterator(
        result.getDocuments(),
        documentStore,
        50  // batch size
    );
    
    // 3. Iterate over enriched documents
    AbstractIterator<JVS> enrichedDocs = iterator.iterator();
    while (enrichedDocs.hasNext()) {
        JVS fullDocument = enrichedDocs.next();
        // Process full document
        System.out.println(fullDocument.toJson());
    }
    enrichedDocs.close();
}
```

### Custom FillBufferHandler

```java
public class CustomDocumentFetcher implements FillBufferHandler<String, MyCustomType> {
    private final KVStore kvStore;
    
    public CustomDocumentFetcher(KVStore kvStore) {
        this.kvStore = kvStore;
    }
    
    @Override
    public void fill(GenericKeyValue<String, MyCustomType>[] buffer, int currentSize) throws Exception {
        // Collect keys
        List<byte[]> keys = new ArrayList<>();
        for (int i = 0; i < currentSize; i++) {
            keys.add(buffer[i].getKey().getBytes(StandardCharsets.UTF_8));
        }
        
        // Batch fetch
        Result<Map<byte[], byte[]>> result = kvStore.batchGet(keys);
        Map<byte[], byte[]> results = result.getValue();
        
        // Fill buffer with custom deserialization
        for (int i = 0; i < currentSize; i++) {
            byte[] key = buffer[i].getKey().getBytes(StandardCharsets.UTF_8);
            byte[] value = results.get(key);
            if (value != null) {
                MyCustomType obj = deserialize(value);
                buffer[i].setValue(obj);
            }
        }
    }
}
```

## Monitoring and Debugging

### Check KV Store Status

```bash
curl http://localhost:8080/api/kvstore/stats
```

### Monitor Batch Fetches

Enable debug logging in `application.properties`:

```properties
logging.level.com.hitorro.example.search.KVDocumentFetcher=DEBUG
logging.level.com.hitorro.example.search.SearchResultIterator=DEBUG
```

You'll see log messages like:
```
DEBUG KVDocumentFetcher: Batch fetch completed: 47 found, 3 missing/failed
DEBUG SearchResultIterator: Creating KV-enriched iterator for 100 search results (batch size: 50)
```

## Troubleshooting

### KV Store Not Available

**Error:** `503 Service Unavailable - KV store is not configured`

**Solution:** Ensure `KVStoreConfig` bean is initialized:
```properties
hitorro.kvstore.path=/tmp/hitorro-kvstore
```

### Native Library Issues

**Error:** `Failed to load RocksDB native library`

**Solution:** The `hitorro-kvstore` module includes automatic platform detection. If issues persist:
1. Check you're using version 3.0.1 or later
2. Verify RocksDB 10.4.2 platform-specific JARs are downloaded
3. See `hitorro-kvstore/ROCKSDB_VERSION_GUIDE.md`

### Memory Issues with Large Batches

**Error:** `OutOfMemoryError` during batch fetch

**Solution:** Reduce batch size:
```bash
curl "http://localhost:8080/api/search/query/withkv?query=*:*&batchSize=10"
```

## Advanced Topics

### Custom ID Field

By default, the system uses `id.did` as the document ID field. To use a different field:

```java
SearchResultIterator iterator = new SearchResultIterator(
    searchResults,
    documentStore,
    50,
    "_id"  // custom ID field
);
```

### Transaction Support

For transactional batch writes to KV store, see `hitorro-kvstore` documentation on enabling transactions.

### Replication

The underlying RocksDB store supports WAL-based replication. See `hitorro-kvstore/README.md` for replication setup.

## Summary

The KV store integration provides:

1. **Dual Storage**: Index in Lucene for search, store in RocksDB for full documents
2. **Batch Efficiency**: FillBufferIterator fetches documents in configurable batches
3. **Flexible Retrieval**: Choose indexed fields only OR full documents
4. **Production Ready**: Uses latest RocksDB with proper platform support
5. **Spring Boot Native**: Fully integrated with Spring configuration and lifecycle

Perfect for applications that need both fast search AND complete document retrieval!
