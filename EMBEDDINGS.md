# Embedding Support in hitorro-index

> **⚠️ Lucene Version Requirement**: Full KNN search functionality requires Apache Lucene 9.12.0 or later. The current version (9.11.1) supports indexing vectors but has limitations in search. See [LUCENE_VERSION_NOTE.md](../hitorro-index/LUCENE_VERSION_NOTE.md) for details and solutions.

## Overview

The `hitorro-index` module now supports vector embeddings for semantic search alongside traditional text-based indexing. This enables:

- **KNN (K-Nearest Neighbors) exact search**: Precise similarity matching
- **ANN (Approximate Nearest Neighbors)**: Fast similarity search using HNSW graphs
- **Hybrid search**: Combined text + vector search with multiple merging strategies

## Architecture

### Core Components

1. **EmbeddingConfig**: Configuration for vector field (dimension, similarity function, HNSW parameters)
2. **VectorSimilarity**: Enum for similarity functions (COSINE, DOT_PRODUCT, EUCLIDEAN, MAXIMUM_INNER_PRODUCT)
3. **EmbeddingFieldType**: FLOAT_VECTOR (32-bit) or BYTE_VECTOR (8-bit quantized)
4. **EmbeddingSearchRequest**: Request object for pure vector search
5. **HybridSearchRequest**: Request object for combined text + vector search

### How It Works

1. **Indexing**: Documents can optionally include an `_embedding` field (configurable name) containing a float/List vector
2. **Storage**: Vectors stored using Lucene's `KnnFloatVectorField` or `KnnByteVectorField`
3. **Search**: HNSW (Hierarchical Navigable Small World) graphs provide fast ANN search
4. **Hybrid**: Results from text and vector search are merged using RRF, weighted sum, or max score

## Configuration

### Index Creation with Embeddings

```java
import com.hitorro.index.IndexManager;
import com.hitorro.index.config.IndexConfig;
import com.hitorro.index.embeddings.*;

// Create embedding configuration
EmbeddingConfig embeddingConfig = EmbeddingConfig.builder()
    .fieldName("_embedding")           // Field name in JVS documents
    .dimension(384)                     // Vector dimension (must match embedding model)
    .similarity(VectorSimilarity.COSINE) // Similarity function
    .fieldType(EmbeddingFieldType.FLOAT_VECTOR) // Use 32-bit floats
    .hnswM(16)                         // HNSW max connections (higher = better recall, more memory)
    .hnswEfConstruction(100)           // HNSW build depth (higher = better quality, slower indexing)
    .build();

// Create index config with embeddings
IndexConfig indexConfig = IndexConfig.builder()
    .filesystem("/path/to/index")
    .embeddings(embeddingConfig)
    .build();

// Create index
IndexManager indexManager = new IndexManager("en");
Type type = JsonTypeSystem.getMe().getType("core_sysobject");
indexManager.createIndex("my_index", indexConfig, type);
```

### Common Embedding Dimensions

| Model | Dimension | Best For |
|-------|-----------|----------|
| all-MiniLM-L6-v2 | 384 | Fast, general purpose |
| all-mpnet-base-v2 | 768 | Better quality, general purpose |
| BERT-base | 768 | Contextual understanding |
| OpenAI text-embedding-ada-002 | 1536 | High quality, commercial |
| OpenAI text-embedding-3-large | 3072 | Best quality, commercial |

### Similarity Functions

**COSINE** (recommended for most models):
- Measures angle between vectors
- Range: [-1, 1] where 1 = identical
- Best for: Normalized embeddings from most models
- Formula: `(A · B) / (||A|| * ||B||)`

**DOT_PRODUCT**:
- Measures projection magnitude
- Range: [-∞, +∞]
- Best for: Pre-normalized vectors (equivalent to cosine)
- Formula: `A · B`

**EUCLIDEAN**:
- Measures L2 distance
- Range: [0, +∞] where 0 = identical
- Best for: When absolute distance matters
- Formula: `sqrt(sum((A_i - B_i)^2))`

**MAXIMUM_INNER_PRODUCT**:
- Optimized dot product variant
- Range: [-∞, +∞]
- Best for: MIPS (Maximum Inner Product Search) use cases

## Indexing Documents

### Adding Embeddings to Documents

```java
import com.hitorro.jsontypesystem.JVS;
import java.util.List;

// Create document with embedding
JVS doc = JVS.read("{\"id\": {\"did\": \"doc1\"}, \"title\": \"Machine Learning\", \"content\": \"Introduction to ML\"}");

// Add embedding as List<Float>
List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, /* ... 381 more values ... */);
doc.set("_embedding", embedding);

// Or as float array
float[] embeddingArray = new float[384]; // ... populate array ...
doc.set("_embedding", embeddingArray);

// Index the document
indexManager.indexDocument("my_index", doc);
indexManager.commit("my_index");
```

### Batch Indexing

```java
List<JVS> documents = new ArrayList<>();

for (String text : texts) {
    JVS doc = JVS.read("{\"content\": \"" + text + "\"}");
    
    // Generate embedding (using your embedding model)
    float[] embedding = generateEmbedding(text); // Your embedding logic
    doc.set("_embedding", embedding);
    
    documents.add(doc);
}

indexManager.indexDocuments("my_index", documents);
indexManager.commit("my_index");
```

### Optional Embeddings

Embeddings are **optional** - documents without the `_embedding` field will still be indexed normally. This allows mixed collections:

```java
// Document with embedding
JVS doc1 = JVS.read("{\"title\": \"AI Article\"}");
doc1.set("_embedding", embedding);

// Document without embedding - still indexed for text search
JVS doc2 = JVS.read("{\"title\": \"Regular Article\"}");

indexManager.indexDocuments("my_index", Arrays.asList(doc1, doc2));
```

## Searching

### Vector Search (KNN/ANN)

```java
import com.hitorro.index.search.EmbeddingSearchRequest;
import com.hitorro.index.search.SearchResult;

// Query vector (from your embedding model)
float[] queryEmbedding = generateEmbedding("machine learning tutorial");

// Create search request
EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
    .queryVector(queryEmbedding)
    .k(10)  // Return top 10 nearest neighbors
    .build();

// Execute search
SearchResult result = indexManager.searchByEmbedding("my_index", request);

// Access results
for (JVS doc : result.getDocuments()) {
    System.out.println("Title: " + doc.get("title"));
    System.out.println("Similarity: " + doc.get("_similarity"));
}
```

### Filtered Vector Search

```java
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

// Create filter (only search within specific category)
TermQuery filter = new TermQuery(new Term("category", "technology"));

EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
    .queryVector(queryEmbedding)
    .k(10)
    .filter(filter)  // Pre-filter documents
    .build();

SearchResult result = indexManager.searchByEmbedding("my_index", request);
```

### Hybrid Search (Text + Vector)

Hybrid search combines traditional text search with semantic similarity:

```java
import com.hitorro.index.search.HybridSearchRequest;
import com.hitorro.index.search.HybridSearchRequest.CombinationStrategy;

String textQuery = "machine learning tutorial";
float[] queryEmbedding = generateEmbedding(textQuery);

HybridSearchRequest request = HybridSearchRequest.builder()
    .textQuery(textQuery)                    // Lucene query string
    .queryVector(queryEmbedding)             // Semantic vector
    .k(10)                                    // Top 10 from each search
    .strategy(CombinationStrategy.RERANK_RRF) // Merge strategy
    .build();

SearchResult result = indexManager.searchHybrid("my_index", request);
```

### Hybrid Search Strategies

**RERANK_RRF (Reciprocal Rank Fusion)** - Recommended:
- Combines rankings from both searches
- Formula: `score = 1/(60 + rank_text) + 1/(60 + rank_vector)`
- Best for: Balanced text + semantic search
- No parameter tuning needed

**MERGE_SUM_SCORE** - Weighted combination:
- Normalized scores with alpha weighting
- Formula: `score = alpha * textScore + (1-alpha) * vectorScore`
- Alpha parameter: 0.0 = pure vector, 1.0 = pure text, 0.5 = equal
- Best for: When you want to control text vs semantic importance

```java
HybridSearchRequest request = HybridSearchRequest.builder()
    .textQuery("artificial intelligence")
    .queryVector(queryEmbedding)
    .k(10)
    .strategy(CombinationStrategy.MERGE_SUM_SCORE)
    .alpha(0.7)  // 70% text, 30% vector
    .build();
```

**MERGE_MAX_SCORE** - Take maximum:
- Uses max score per document across both searches
- Formula: `score = max(textScore, vectorScore)`
- Best for: When either text or semantic match is sufficient

## REST API Usage

### POST /api/search/embedding

Search by embedding vector:

```bash
curl -X POST "http://localhost:8080/api/search/embedding?indexName=default" \
  -H "Content-Type: application/json" \
  -d '{
    "vector": [0.1, 0.2, 0.3, ...],
    "k": 10
  }'
```

Response:
```json
{
  "status": "success",
  "indexName": "default",
  "totalHits": 10,
  "k": 10,
  "vectorDimension": 384,
  "searchTimeMs": 15,
  "documents": [
    {
      "title": "Machine Learning Tutorial",
      "_similarity": 0.9234,
      ...
    }
  ]
}
```

### POST /api/search/hybrid

Hybrid text + vector search:

```bash
curl -X POST "http://localhost:8080/api/search/hybrid?indexName=default" \
  -H "Content-Type: application/json" \
  -d '{
    "textQuery": "machine learning",
    "vector": [0.1, 0.2, 0.3, ...],
    "k": 10,
    "strategy": "RERANK_RRF",
    "alpha": 0.5
  }'
```

Strategies: `RERANK_RRF`, `MERGE_SUM_SCORE`, `MERGE_MAX_SCORE`

## HNSW Parameter Tuning

HNSW (Hierarchical Navigable Small World) builds a graph for fast ANN search. Two parameters control the tradeoff:

### hnswM (Max Connections Per Node)

- **Range**: 2-96
- **Default**: 16
- **Recommended**: 4-48
- **Higher values**:
  - ✅ Better recall (more accurate results)
  - ❌ More memory usage
  - ❌ Slower indexing
- **Lower values**:
  - ✅ Less memory
  - ✅ Faster indexing
  - ❌ Lower recall

**Guidelines**:
- Small datasets (< 10K docs): M = 8-16
- Medium datasets (10K-1M docs): M = 16-32
- Large datasets (> 1M docs): M = 32-64

### hnswEfConstruction (Build Depth)

- **Range**: 1-3200
- **Default**: 100
- **Recommended**: 100-800
- **Higher values**:
  - ✅ Better graph quality → better recall
  - ❌ Slower indexing
- **Lower values**:
  - ✅ Faster indexing
  - ❌ Potentially lower recall

**Guidelines**:
- Development/testing: efConstruction = 100
- Production (standard): efConstruction = 200-400
- Production (high quality): efConstruction = 400-800

### Example Configurations

**Fast indexing (lower quality)**:
```java
EmbeddingConfig.builder()
    .hnswM(8)
    .hnswEfConstruction(100)
    .build();
```

**Balanced (recommended)**:
```java
EmbeddingConfig.builder()
    .hnswM(16)
    .hnswEfConstruction(200)
    .build();
```

**High quality (slower indexing)**:
```java
EmbeddingConfig.builder()
    .hnswM(32)
    .hnswEfConstruction(400)
    .build();
```

## FLOAT vs BYTE Vectors

### FLOAT_VECTOR (default)

- **Size**: 4 bytes per dimension
- **Precision**: Full 32-bit float precision
- **Best for**: Most use cases, especially when accuracy is critical
- **Memory (384 dims)**: 1.5 KB per document

```java
EmbeddingConfig.builder()
    .fieldType(EmbeddingFieldType.FLOAT_VECTOR)
    .build();
```

### BYTE_VECTOR (quantized)

- **Size**: 1 byte per dimension
- **Precision**: 8-bit quantized (values mapped to [-128, 127])
- **Best for**: Large-scale deployments, memory-constrained environments
- **Memory (384 dims)**: 384 bytes per document (4x smaller)
- **Tradeoff**: Slight loss in precision vs significant memory savings

```java
EmbeddingConfig.builder()
    .fieldType(EmbeddingFieldType.BYTE_VECTOR)
    .build();
```

Quantization automatically maps float range [-1, 1] to byte range [-128, 127]. Values outside [-1, 1] are clamped.

## Integration with Embedding Models

### OpenAI

```java
// Using OpenAI API
import com.openai.api.EmbeddingApi;

String text = "Machine learning tutorial";
EmbeddingResponse response = openai.embeddings().create(
    EmbeddingRequest.builder()
        .model("text-embedding-ada-002")
        .input(text)
        .build()
);

float[] embedding = response.getData().get(0).getEmbedding();

JVS doc = JVS.read("{\"content\": \"" + text + "\"}");
doc.set("_embedding", embedding);
```

### Ollama (Local)

```java
// Using Ollama with Spring AI
import org.springframework.ai.ollama.OllamaEmbeddingClient;

@Autowired
private OllamaEmbeddingClient embeddingClient;

String text = "Machine learning tutorial";
float[] embedding = embeddingClient.embed(text);

JVS doc = JVS.read("{\"content\": \"" + text + "\"}");
doc.set("_embedding", embedding);
```

### Sentence Transformers (Python → Java)

Generate embeddings in Python, index in Java:

Python:
```python
from sentence_transformers import SentenceTransformer
import json

model = SentenceTransformer('all-MiniLM-L6-v2')
texts = ["Machine learning tutorial", "Deep learning guide"]
embeddings = model.encode(texts)

# Save as JSON
data = [{"content": text, "embedding": emb.tolist()} 
        for text, emb in zip(texts, embeddings)]
        
with open('documents.json', 'w') as f:
    json.dump(data, f)
```

Java:
```java
// Load and index
ObjectMapper mapper = new ObjectMapper();
List<Map> documents = mapper.readValue(new File("documents.json"), 
                                       new TypeReference<List<Map>>(){});

for (Map doc : documents) {
    JVS jvs = new JVS();
    jvs.set("content", doc.get("content"));
    jvs.set("_embedding", doc.get("embedding"));
    indexManager.indexDocument("my_index", jvs);
}
```

## Performance Considerations

### Indexing Performance

- **HNSW graph building**: O(log N) per document, but impacted by M and efConstruction
- **Batch indexing recommended**: Use `indexDocuments()` for better throughput
- **Memory**: Consider BYTE_VECTOR for large collections (4x memory savings)

### Search Performance

- **ANN (Approximate)**: Very fast, typically < 10ms for millions of documents
- **KNN (Exact)**: Can be slow for large datasets without HNSW
- **HNSW provides**: ~95-99% recall at 10-100x speed vs exact KNN
- **Hybrid search**: ~2x cost of single search (parallel execution possible)

### Scaling Guidelines

| Documents | Dimension | M | efConstruction | Memory (FLOAT) | Search Time |
|-----------|-----------|---|----------------|----------------|-------------|
| 10K | 384 | 16 | 100 | ~60 MB | < 5ms |
| 100K | 384 | 16 | 200 | ~600 MB | < 10ms |
| 1M | 384 | 32 | 400 | ~6 GB | < 20ms |
| 10M | 384 | 32 | 400 | ~60 GB | < 50ms |

Switch to BYTE_VECTOR to reduce memory by 4x.

## Best Practices

1. **Choose the right similarity function**: COSINE works for most embedding models
2. **Match dimensions exactly**: Dimension mismatch will silently skip embeddings
3. **Normalize embeddings**: If using DOT_PRODUCT, ensure vectors are L2-normalized
4. **Tune HNSW carefully**: Start with defaults (M=16, efConstruction=100), increase if recall is insufficient
5. **Use hybrid search**: Often better than pure text or pure vector search
6. **Batch index**: Much faster than indexing one document at a time
7. **Test recall**: Validate HNSW parameters provide acceptable recall for your use case
8. **Monitor memory**: Large vector indexes can consume significant RAM

## Troubleshooting

### "Embedding search not supported"
- Index was not created with `EmbeddingConfig`
- Solution: Recreate index with embeddings configured

### "Dimension mismatch"
- Vector dimension doesn't match configured dimension
- Solution: Check embedding model output size matches `EmbeddingConfig.dimension()`

### Low recall / poor results
- HNSW parameters too low
- Wrong similarity function
- Solution: Increase M and efConstruction, verify similarity function matches embedding model

### Out of memory
- Vector index too large for available RAM
- Solution: Use BYTE_VECTOR, reduce M parameter, or increase JVM heap

### Slow indexing
- HNSW parameters too high
- Solution: Reduce efConstruction for faster indexing (accept slight quality loss)

## Examples

See `EmbeddingSearchTest.java` for comprehensive test examples covering:
- Basic vector search
- Filtered vector search
- Hybrid search with different strategies
- Configuration validation
- Different similarity functions

## Further Reading

- [Lucene Vector Search Documentation](https://lucene.apache.org/core/9_11_1/core/org/apache/lucene/search/KnnFloatVectorQuery.html)
- [HNSW Algorithm Paper](https://arxiv.org/abs/1603.09320)
- [Sentence Transformers](https://www.sbert.net/)
- [OpenAI Embeddings Guide](https://platform.openai.com/docs/guides/embeddings)
