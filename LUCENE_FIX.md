# Lucene Backward Codecs Fix

## Problem

The SearchController was failing to initialize with the following error:

```
java.util.ServiceConfigurationError: org.apache.lucene.codecs.Codec: 
  org.apache.lucene.backward_codecs.lucene90.Lucene90Codec 
  Unable to get public no-arg constructor

Caused by: java.lang.ClassNotFoundException: 
  org.apache.lucene.codecs.lucene90.Lucene90SegmentInfoFormat
```

This prevented the Spring Boot application from starting.

## Root Cause

The `hitorro-index` module was missing the `lucene-backward-codecs` dependency. This library is required by Lucene to:

1. Read indexes created with older Lucene versions
2. Support codec compatibility across Lucene versions
3. Provide codec implementations like `Lucene90Codec`

Without this dependency, Lucene's ServiceLoader mechanism fails when trying to instantiate codec implementations during IndexWriter initialization.

## Solution

Added the missing dependency to `hitorro-index/pom.xml`:

```xml
<!-- Lucene Backward Codecs (required for reading older index formats) -->
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-backward-codecs</artifactId>
    <version>${lucene.version}</version>
</dependency>
```

## Steps Taken

1. **Added dependency** to `/Users/chris/hitorro/hitorro-index/pom.xml`:
   ```xml
   <dependency>
       <groupId>org.apache.lucene</groupId>
       <artifactId>lucene-backward-codecs</artifactId>
       <version>${lucene.version}</version>
   </dependency>
   ```

2. **Rebuilt hitorro-index**: `mvn clean install -DskipTests`

3. **Fixed version conflicts** in Spring Boot app by adding dependency management:
   - Added `lucene.version` property set to `9.11.1`
   - Added `<dependencyManagement>` section to force Lucene 9.11.1 across all dependencies
   - This resolved conflicts between hitorro-text-core (9.9.2) and hitorro-index (9.11.1)

4. **Fixed index initialization** in SearchController:
   - IndexWriter creates the index
   - Commit to persist empty index
   - Then create searcher (prevents "no segments file found" error)

5. **Rebuilt Spring Boot app**: `mvn clean package -U -DskipTests`

6. **Verified fix**: Application now starts successfully

## Verification

After the fix:
- ✅ SearchController initializes without errors
- ✅ Lucene index can be created at `/tmp/hitorro-lucene-index`
- ✅ Application starts and runs on port 8080
- ✅ Search API endpoints are available

## Testing the Search Feature

Once the application is running, you can test the search functionality:

1. **Navigate to Search tab** in the React UI at http://localhost:8080
2. **Load sample documents** using the "Load Sample Documents" button
3. **Search** using queries like:
   - `*:*` - Match all documents
   - `title.mls:lucene` - Search in title field
   - `type:core_sysobject` - Filter by type

Or use the REST API directly:

```bash
# Index a document
curl -X POST http://localhost:8080/api/search/index \
  -H "Content-Type: application/json" \
  -d '{
    "id": {"domain": "sysobject", "did": "doc001"},
    "type": "core_sysobject",
    "title": {"mls": [{"lang": "en", "text": "Test Document"}]}
  }'

# Search
curl "http://localhost:8080/api/search/query?query=*:*&maxResults=10"
```

## Related Documentation

- **SEARCH_EXAMPLE.md**: Complete guide to the search integration
- **AGENTS.md**: Architecture documentation including search patterns
- **hitorro-index/README.md**: Lucene integration module documentation

## Note on Database Warnings

You may see harmless warnings about `name_idx` already existing:
```
Index "name_idx" already exists
```

These are expected when using `hibernate.ddl-auto: update` with an existing database. The warnings can be safely ignored as documented in AGENTS.md.

---

**Fix applied**: 2026-02-01  
**Versions**: 
- hitorro-index: 3.0.0
- Lucene: 9.11.1
- Spring Boot: 3.2.2
