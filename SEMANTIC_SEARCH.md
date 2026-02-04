# Semantic Search with Ollama Integration

This document describes the semantic search functionality integrated into the Hitorro Spring Boot example application.

## Overview

The application now supports three types of search:
1. **Traditional Text Search** - Keyword-based Lucene search
2. **Semantic Search** - Vector similarity search using embeddings from Ollama
3. **Hybrid Search** - Combines both traditional and semantic search for best results

## Prerequisites

### Install Ollama

Download and install Ollama from [ollama.ai](https://ollama.ai)

**macOS:**
```bash
brew install ollama
```

**Linux:**
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

### Start Ollama Service

```bash
ollama serve
```

### Pull the Embedding Model

The application uses `nomic-embed-text` by default (768-dimensional embeddings):

```bash
ollama pull nomic-embed-text
```

Alternative models you can use:
- `all-minilm` (384 dimensions, faster)
- `mxbai-embed-large` (1024 dimensions, more accurate)

## Configuration

### Enable Embeddings in application.yml

```yaml
# Enable embedding support
embedding:
  enabled: true  # Set to true to enable semantic search
  fields:
    - title
    - description
  field-separator: ". "
  auto-detect-dimension: true
  dimension: 768  # Fallback if auto-detect fails

# Spring AI Ollama configuration (already configured under spring.ai.ollama)
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      embedding:
        options:
          model: nomic-embed-text
```

### Restart the Application

After enabling embeddings:
1. Stop the application if running
2. **Clear the index** (important - embeddings require index recreation)
3. Start the application
4. Re-index your documents

## How It Works

### 1. Indexing with Embeddings

When you index a document, the system:
1. Extracts text from configured fields (default: `title` and `description`)
2. Combines the text using the configured separator
3. Sends the text to Ollama to generate embeddings
4. Adds the embedding as a `_embedding` field to the document
5. Indexes the document with both traditional fields AND the vector

**Example:**
```bash
# Index a document (embedding generated automatically)
POST /api/search/index?indexName=default&generateEmbedding=true
Content-Type: application/json

{
  "id": { "domain": "sysobject", "did": "doc001" },
  "type": "core_sysobject",
  "title": { "mls": [{ "lang": "en", "text": "Apache Lucene Tutorial" }] },
  "description": { "mls": [{ "lang": "en", "text": "Learn about full-text search" }] }
}
```

The system automatically:
- Extracts: "Apache Lucene Tutorial. Learn about full-text search"
- Generates 768-dimensional embedding via Ollama
- Adds `_embedding` field to the document
- Indexes with both text and vector

### 2. Searching

#### Traditional Text Search
```bash
GET /api/search/query?query=title.mls:lucene&maxResults=10
```

#### Semantic Search (Vector Only)
```bash
POST /api/search/semantic?indexName=default
Content-Type: application/json

{
  "query": "documents about search engines",
  "mode": "SEMANTIC_ONLY",
  "k": 10
}
```

The query text is converted to an embedding and searches for similar vectors.

#### Hybrid Search (Recommended)
```bash
POST /api/search/semantic?indexName=default
Content-Type: application/json

{
  "query": "Apache Lucene search",
  "mode": "HYBRID",
  "k": 10,
  "strategy": "RERANK_RRF",
  "alpha": 0.5
}
```

**Hybrid Strategies:**
- `RERANK_RRF` - Reciprocal Rank Fusion (recommended, balanced approach)
- `WEIGHTED_SUM` - Weighted combination using alpha parameter
  - `alpha=0.0` - All weight on text search
  - `alpha=0.5` - Equal weight
  - `alpha=1.0` - All weight on semantic search
- `MAX_SCORE` - Takes maximum score from either search method

## Using the React UI

### 1. Start the Application

```bash
mvn spring-boot:run
```

### 2. Access the UI

Open http://localhost:8080 and navigate to the "Search" tab.

### 3. Check Ollama Status

The UI displays Ollama connection status at the top of the Semantic Search section:
- **Green** - Ollama available, semantic search enabled
- **Red** - Ollama unavailable, only text search available

### 4. Index Documents with Embeddings

1. Navigate to the "Index Document" section
2. Ensure "Generate Embeddings" is enabled (default)
3. Paste your JSON document
4. Click "Index Document"

The response will show `embeddingGenerated: true` if successful.

### 5. Perform Semantic Search

1. Scroll to "Semantic Search (Ollama)" section
2. Enter your search query (natural language)
3. Choose search mode:
   - **Text Only** - Traditional keyword search
   - **Semantic Only** - Pure vector similarity search
   - **Hybrid** - Combines both (recommended)
4. For Hybrid mode:
   - Select combination strategy (RRF recommended)
   - Adjust alpha slider if using Weighted Sum
5. Click "Search"

Results are displayed below with vector dimension, search time, and document details.

## Architecture

### Backend Components

**EmbeddingService** (`com.hitorro.example.service.EmbeddingService`)
- Manages Ollama connection
- Circuit breaker pattern (disables after 5 failures)
- Auto-detects embedding dimension
- Extracts and combines text from JVS documents
- Generates embeddings via Spring AI

**SearchController** (`com.hitorro.example.controller.SearchController`)
- Initializes index with embedding support if EmbeddingService available
- `/api/search/index` - Adds embeddings during document indexing
- `/api/search/semantic` - Semantic search endpoint with all modes

**HealthController** (`com.hitorro.example.controller.HealthController`)
- `/api/health/ollama` - Check Ollama status
- `/api/health/ollama/reset` - Reset failure counter

### Frontend Components

**OllamaStatus** (`react-app/src/components/OllamaStatus.tsx`)
- Displays Ollama connection status
- Auto-refreshes every 30 seconds
- Manual refresh button

**SearchPage** (`react-app/src/pages/SearchPage.tsx`)
- Semantic search form with mode selector
- Hybrid strategy controls
- Alpha slider for weighted sum
- Displays semantic search results

## Troubleshooting

### Ollama Not Available

**Symptom:** Red status in UI, "Ollama not available" message

**Solutions:**
1. Check if Ollama is running:
   ```bash
   curl http://localhost:11434/api/tags
   ```
2. Start Ollama service:
   ```bash
   ollama serve
   ```
3. Verify model is pulled:
   ```bash
   ollama list
   ```

### No Embeddings Generated

**Symptom:** `embeddingGenerated: false` in indexing response

**Causes:**
- Ollama not running
- Document has no title/description fields
- EmbeddingService disabled (`embedding.enabled=false`)

**Solutions:**
1. Enable embeddings in `application.yml`
2. Restart application
3. Ensure Ollama is running
4. Check document has configured fields (title, description)

### Index Has No Embedding Support

**Symptom:** "Embedding search not configured for index" error

**Cause:** Index was created before enabling embeddings

**Solution:**
1. Clear the index via UI or API:
   ```bash
   DELETE /api/search/index?indexName=default
   ```
2. Restart application (index recreated with embedding support)
3. Re-index documents

### Search Returns No Results

**Possible Causes:**
1. No documents indexed with embeddings
2. Query doesn't match indexed content semantically
3. Wrong search mode selected

**Solutions:**
- Verify documents were indexed with `embeddingGenerated: true`
- Try hybrid mode instead of semantic-only
- Try text-only mode to verify documents exist in index

## Performance Considerations

### Indexing Performance

**Embedding Generation:**
- Each document requires an Ollama API call (~50-200ms per document)
- Batch indexing processes sequentially (not parallelized)
- For large datasets, consider:
  - Pre-generating embeddings offline
  - Using faster models (all-minilm)
  - Increasing Ollama resources

**Optimization Tips:**
- Index in batches using `/api/search/index/batch`
- Use KV store with enrichment for frequently accessed documents
- Cache embeddings if re-indexing same documents

### Search Performance

**Vector Search:**
- HNSW provides approximate nearest neighbor (ANN) search
- Typical search time: 10-50ms for 10K documents
- Scales sub-linearly with document count

**Hybrid Search:**
- Combines two searches (text + vector)
- Typically 2-3x slower than single search mode
- RRF strategy has minimal overhead
- Weighted Sum requires score normalization (slightly slower)

**Optimization Tips:**
- Use smaller `k` values (10-20 sufficient for most use cases)
- RRF strategy is fastest for hybrid
- Consider semantic-only for pure similarity search

## API Reference

### POST /api/search/index
Index document with optional embedding generation.

**Query Parameters:**
- `indexName` (default: "default") - Index to use
- `generateEmbedding` (default: true) - Generate embedding if available

**Request Body:** JVS document JSON

**Response:**
```json
{
  "status": "success",
  "message": "Document indexed successfully",
  "indexName": "default",
  "embeddingGenerated": true,
  "documentId": "doc001"
}
```

### POST /api/search/semantic
Semantic search with multiple modes.

**Query Parameters:**
- `indexName` (default: "default") - Index to search

**Request Body:**
```json
{
  "query": "search query text",
  "mode": "HYBRID",
  "k": 10,
  "strategy": "RERANK_RRF",
  "alpha": 0.5
}
```

**Response:**
```json
{
  "status": "success",
  "indexName": "default",
  "query": "search query text",
  "searchMode": "hybrid",
  "k": 10,
  "totalHits": 5,
  "searchTimeMs": 42,
  "vectorDimension": 768,
  "strategy": "RERANK_RRF",
  "documents": [...]
}
```

### GET /api/health/ollama
Check Ollama availability.

**Response:**
```json
{
  "available": true,
  "url": "http://localhost:11434",
  "failureCount": 0
}
```

## Advanced Configuration

### Using Different Models

Edit `application.yml`:
```yaml
spring:
  ai:
    ollama:
      embedding:
        options:
          model: all-minilm  # Change model here
```

Update dimension fallback:
```yaml
embedding:
  dimension: 384  # all-minilm uses 384 dimensions
```

### Customizing Embedding Fields

Extract different fields for embedding:
```yaml
embedding:
  fields:
    - title
    - description
    - content
    - tags
  field-separator: ". "
```

### Using Multiple Indexes

Create specialized indexes for different document types:
```bash
POST /api/search/indexes?indexName=products&typeName=demo_product
POST /api/search/indexes?indexName=articles&typeName=core_article
```

Each index can have its own embedding configuration.

## Best Practices

1. **Always use hybrid search for production** - Combines benefits of both approaches
2. **Use RRF strategy as default** - Best balance of accuracy and performance
3. **Index with embeddings enabled** - Cannot add embeddings to existing index
4. **Monitor Ollama health** - Circuit breaker prevents cascade failures
5. **Tune k parameter** - 10-20 sufficient for most use cases
6. **Clear index when changing embedding config** - Dimension/model changes require reindex
7. **Use text search for exact matches** - Semantic search for concept similarity
8. **Batch index large datasets** - Reduces HTTP overhead

## Examples

### Example 1: Building a Product Search

```bash
# Index products with embeddings
POST /api/search/index?generateEmbedding=true
{
  "type": "product",
  "title": { "mls": [{ "lang": "en", "text": "Wireless Bluetooth Headphones" }] },
  "description": { "mls": [{ "lang": "en", "text": "High-quality audio with noise cancellation" }] }
}

# Search with natural language
POST /api/search/semantic
{
  "query": "headphones with good sound quality",
  "mode": "HYBRID",
  "k": 5,
  "strategy": "RERANK_RRF"
}
```

### Example 2: Document Discovery

```bash
# Index technical documentation
POST /api/search/index/batch?generateEmbedding=true
[
  { "title": {...}, "description": {...} },
  { "title": {...}, "description": {...} }
]

# Find similar documents
POST /api/search/semantic
{
  "query": "how to configure search engine",
  "mode": "SEMANTIC_ONLY",
  "k": 10
}
```

### Example 3: Hybrid Search with Customization

```bash
# Hybrid search favoring semantic similarity
POST /api/search/semantic
{
  "query": "machine learning algorithms",
  "mode": "HYBRID",
  "k": 20,
  "strategy": "WEIGHTED_SUM",
  "alpha": 0.8  // 80% weight on vector search
}
```

## References

- [Ollama Documentation](https://ollama.ai/docs)
- [Nomic Embed Text Model](https://ollama.ai/library/nomic-embed-text)
- [Spring AI Ollama Integration](https://docs.spring.io/spring-ai/reference/api/embeddings/ollama-embeddings.html)
- [Apache Lucene Vector Search](https://lucene.apache.org/core/9_12_0/core/org/apache/lucene/search/KnnFloatVectorQuery.html)
- [Reciprocal Rank Fusion (RRF)](https://plg.uwaterloo.ca/~gvcormac/cormacksigir09-rrf.pdf)

## Migration Guide

### Upgrading from Text-Only Search

1. **Backup your data** - Index will need to be recreated
2. **Install Ollama** - Follow prerequisites section
3. **Enable embeddings** - Update `application.yml`
4. **Restart application** - Index recreated automatically
5. **Re-index documents** - Use batch API for efficiency
6. **Update search calls** - Switch to `/api/search/semantic` endpoint
7. **Test thoroughly** - Verify results quality

### Rolling Back

To disable semantic search:
```yaml
embedding:
  enabled: false
```

Restart application. Index will be recreated without embedding support.
Existing text search will continue to work normally.
