package com.hitorro.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitorro.example.search.SearchResultIterator;
import com.hitorro.example.service.EmbeddingService;
import com.hitorro.index.IndexManager;
import com.hitorro.index.config.IndexConfig;
import com.hitorro.index.embeddings.EmbeddingConfig;
import com.hitorro.index.embeddings.EmbeddingFieldType;
import com.hitorro.index.embeddings.VectorSimilarity;
import com.hitorro.index.indexer.JVSLuceneIndexWriter;
import com.hitorro.index.query.JVSQueryParser;
import com.hitorro.index.search.EmbeddingSearchRequest;
import com.hitorro.index.search.FacetResult;
import com.hitorro.index.search.HybridSearchRequest;
import com.hitorro.index.search.JVSLuceneSearcher;
import com.hitorro.index.search.SearchResult;
import com.hitorro.index.stream.IndexerStream;
import com.hitorro.index.stream.SearchResponseStream;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.JsonTypeSystem;
import com.hitorro.jsontypesystem.Type;
import com.hitorro.obj.core.solr.JVS2JVSEnrichMapper;
import com.hitorro.kvstore.KVStore;
import com.hitorro.kvstore.Result;
import com.hitorro.util.core.iterator.AbstractIterator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * REST Controller for Lucene-based search and indexing operations.
 * Demonstrates hitorro-index module integration with Spring Boot.
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Lucene-based document search and indexing")
public class SearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private IndexManager indexManager;
    private Path indexPath;
    private String defaultIndexName = "default";
    
    @Autowired(required = false)
    @Qualifier("documentStore")
    private KVStore documentStore;
    
    @Autowired(required = false)
    private EmbeddingService embeddingService;
    
    @PostConstruct
    public void initializeIndex() {
        try {
            // Create index manager with default language
            indexManager = new IndexManager("en");
            
            // Create index in temp directory for this example
            indexPath = Paths.get(System.getProperty("java.io.tmpdir"), "hitorro-lucene-indexes");
            logger.info("Initializing Lucene indexes at: {}", indexPath);
            
            // Check if embedding service is available and configure accordingly
            EmbeddingConfig embeddingConfig = null;
            if (embeddingService != null) {
                int dimension = embeddingService.getDimension();
                embeddingConfig = EmbeddingConfig.builder()
                        .fieldName("_embedding")  // Field name for embeddings in JVS documents
                        .dimension(dimension)
                        .similarity(VectorSimilarity.COSINE)
                        .fieldType(EmbeddingFieldType.FLOAT_VECTOR)
                        .build();
                logger.info("Embedding support enabled with dimension: {}", dimension);
            } else {
                logger.info("Embedding service not available - index created without embedding support");
            }
            
            // IndexConfig will automatically use FieldPatternAnalyzerWrapper
            // which selects analyzers based on field name suffixes (Solr-style)
            // Examples:
            //   *.text_en_s -> EnglishAnalyzer (with stemming, stop words)
            //   *.text_de_m -> GermanAnalyzer (with compound noun splitting, umlaut normalization)
            //   *.identifier_s -> KeywordAnalyzer (exact match, no tokenization)
            IndexConfig.Builder configBuilder = IndexConfig.builder()
                    .filesystem(indexPath.resolve(defaultIndexName));
            
            if (embeddingConfig != null) {
                configBuilder.embeddings(embeddingConfig);
            }
            
            IndexConfig config = configBuilder.build();
            
            // Get default type for core_sysobject
            Type defaultType = JsonTypeSystem.getMe().getType("core_sysobject");
            
            // Create default index
            indexManager.createIndex(defaultIndexName, config, defaultType);
            indexManager.commit(defaultIndexName);
            
            logger.info("Default Lucene index initialized successfully at: {}", indexPath.resolve(defaultIndexName));
        } catch (Exception e) {
            logger.error("Failed to initialize Lucene index", e);
            throw new RuntimeException("Failed to initialize search index", e);
        }
    }
    
    @PreDestroy
    public void closeIndex() {
        try {
            if (indexManager != null) {
                indexManager.close();
            }
            logger.info("Lucene indexes closed");
        } catch (Exception e) {
            logger.error("Error closing indexes", e);
        }
    }

    /**
     * Expose the IndexManager so diagnostics/controllers (e.g. lucene viewer) can
     * reuse the same writers/searchers without fighting for Lucene write locks.
     */
    public IndexManager getIndexManager() {
        return indexManager;
    }

    /**
     * Base filesystem path where this example app creates/manages its indexes.
     */
    public Path getIndexBasePath() {
        return indexPath;
    }
    
    
    @PostMapping("/index")
    @Operation(
            summary = "Index a single document",
            description = "Indexes a single JVS document into the Lucene index with optional embedding generation"
    )
    @ApiResponse(responseCode = "200", description = "Document indexed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid document format")
    @ApiResponse(responseCode = "500", description = "Indexing error")
    public ResponseEntity<Map<String, Object>> indexDocument(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "Generate embeddings if service available")
            @RequestParam(defaultValue = "true") boolean generateEmbedding,
            @Parameter(description = "JVS document as JSON string")
            @RequestBody String jsonDocument) {
        try {
            JVS document = JVS.read(jsonDocument);
            
            // Generate embedding out-of-band
            boolean embeddingAdded = false;
            float[] embedding = null;
            
            if (generateEmbedding && embeddingService != null && embeddingService.isAvailable()) {
                String text = embeddingService.extractTextForEmbedding(document);
                logger.info("Extracted text for embedding: '{}'", text != null && text.length() > 100 ? text.substring(0, 100) + "..." : text);
                
                if (text != null && !text.isEmpty()) {
                    embedding = embeddingService.generateEmbedding(text);
                    logger.info("Generated embedding: dimension={}", embedding != null ? embedding.length : "null");
                    embeddingAdded = (embedding != null);
                }
            }
            
            // Index with out-of-band embedding
            IndexManager.IndexHandle handle = indexManager.getIndex(indexName);
            if (handle != null) {
                handle.getWriter().indexDocument(document, embedding);
            } else {
                throw new IllegalArgumentException("Index '" + indexName + "' does not exist");
            }
            indexManager.commit(indexName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Document indexed successfully");
            response.put("indexName", indexName);
            response.put("embeddingGenerated", embeddingAdded);
            
            // Get document ID if available
            try {
                Object docId = document.get("id.did");
                response.put("documentId", docId != null ? docId.toString() : "unknown");
            } catch (Exception e) {
                response.put("documentId", "unknown");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error indexing document", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/index/batch")
    @Operation(
            summary = "Index multiple documents",
            description = "Indexes a batch of JVS documents with optional embedding generation"
    )
    @ApiResponse(responseCode = "200", description = "Documents indexed successfully")
    public ResponseEntity<Map<String, Object>> indexBatch(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "Generate embeddings if service available")
            @RequestParam(defaultValue = "true") boolean generateEmbedding,
            @Parameter(description = "Array of JVS documents")
            @RequestBody List<String> jsonDocuments) {
        
        logger.info("[batch] Received batch indexing request: indexName={}, generateEmbedding={}, documentCount={}, embeddingService={}, ollamaAvailable={}",
                indexName, generateEmbedding, jsonDocuments.size(),
                embeddingService != null ? "present" : "null",
                embeddingService != null ? embeddingService.isAvailable() : "N/A");
        
        try {
            List<JVS> documents = new ArrayList<>();
            int embeddingsGenerated = 0;
            
            IndexManager.IndexHandle handle = indexManager.getIndex(indexName);
            if (handle == null) {
                throw new IllegalArgumentException("Index '" + indexName + "' does not exist");
            }
            
            for (String jsonDoc : jsonDocuments) {
                JVS document = JVS.read(jsonDoc);
                
                // Generate embedding out-of-band
                float[] embedding = null;
                if (generateEmbedding && embeddingService != null && embeddingService.isAvailable()) {
                    String text = embeddingService.extractTextForEmbedding(document);
                    if (text != null && !text.isEmpty()) {
                        embedding = embeddingService.generateEmbedding(text);
                        if (embedding != null) {
                            embeddingsGenerated++;
                        }
                    }
                }
                
                // Index with out-of-band embedding
                handle.getWriter().indexDocument(document, embedding);
            }
            
            indexManager.commit(indexName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("indexed", documents.size());
            response.put("embeddingsGenerated", embeddingsGenerated);
            response.put("indexName", indexName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error indexing batch", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/index/withkv")
    @Operation(
            summary = "Index document to both Lucene and KV store",
            description = "Indexes a document into Lucene index and stores full JSON in KV store with optional enrichment and embedding generation"
    )
    @ApiResponse(responseCode = "200", description = "Document indexed and stored successfully")
    @ApiResponse(responseCode = "503", description = "KV store not available")
    public ResponseEntity<Map<String, Object>> indexWithKVStore(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "Generate embeddings if service available")
            @RequestParam(defaultValue = "true") boolean generateEmbedding,
            @Parameter(description = "Enrich document before storing in KV store")
            @RequestParam(defaultValue = "false") boolean enrichBeforeStore,
            @Parameter(description = "JVS document as JSON string")
            @RequestBody String jsonDocument) {
        try {
            if (documentStore == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "KV store not configured");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            JVS document = JVS.read(jsonDocument);
            
            // Check if JVS has a valid type
            if (document.getType() == null) {
                logger.warn("Document has null type, enrichment may fail. JSON: {}", 
                           jsonDocument.length() > 200 ? jsonDocument.substring(0, 200) + "..." : jsonDocument);
            }
            
            // Generate and add embedding if service is available and enabled
            boolean embeddingAdded = false;
            logger.info("[withkv] Embedding generation requested: generateEmbedding={}, embeddingService={}, available={}",
                    generateEmbedding, 
                    embeddingService != null ? "present" : "null",
                    embeddingService != null ? embeddingService.isAvailable() : "N/A");
            
            if (generateEmbedding && embeddingService != null && embeddingService.isAvailable()) {
                String text = embeddingService.extractTextForEmbedding(document);
                logger.info("[withkv] Extracted text for embedding: '{}'", text != null && text.length() > 100 ? text.substring(0, 100) + "..." : text);
                
                if (text != null && !text.isEmpty()) {
                    float[] embedding = embeddingService.generateEmbedding(text);
                    logger.info("[withkv] Generated embedding: dimension={}", embedding != null ? embedding.length : "null");
                    
                    if (embedding != null) {
                        // Convert to List<Float> for JVS.set() compatibility
                        java.util.List<Float> embeddingList = new java.util.ArrayList<>(embedding.length);
                        for (float val : embedding) {
                            embeddingList.add(val);
                        }
                        // Use JVS.set() API - adds embedding orthogonally, not in JSON
                        document.set("_embedding", embeddingList);
                        embeddingAdded = true;
                        logger.info("[withkv] Successfully added embedding as List via JVS.set() with dimension {}", embeddingList.size());
                    }
                } else {
                    logger.warn("[withkv] No text extracted from document for embedding generation");
                }
            } else {
                if (!generateEmbedding) logger.info("[withkv] Embedding generation disabled by request parameter");
                if (embeddingService == null) logger.warn("[withkv] EmbeddingService is null");
                if (embeddingService != null && !embeddingService.isAvailable()) logger.warn("[withkv] Ollama is not available");
            }
            
            // Extract document ID
            Object docIdObj = document.get("id.did");
            String docId;
            if (docIdObj != null) {
                // Remove quotes if present
                docId = docIdObj.toString().replaceAll("\"", "");
            } else {
                docId = UUID.randomUUID().toString();
            }
            
            // Optionally enrich document before storing - only if document has valid type
            JVS documentToStore = document;
            if (enrichBeforeStore && document.getType() != null) {
                logger.info("Enriching document before storing in KV store");
                JVS2JVSEnrichMapper enrichMapper = new JVS2JVSEnrichMapper();
                documentToStore = enrichMapper.apply(document);
                if (documentToStore == null) {
                    documentToStore = document; // Fallback to original
                }
            } else if (enrichBeforeStore && document.getType() == null) {
                logger.warn("Skipping enrichment for document without type. DocId: {}", docId);
            }
            
            // Store JSON in KV store (enriched or original)
            byte[] key = docId.getBytes(StandardCharsets.UTF_8);
            String jsonToStore = objectMapper.writeValueAsString(documentToStore.getJsonNode());
            byte[] value = jsonToStore.getBytes(StandardCharsets.UTF_8);
            Result<Void> storeResult = documentStore.put(key, value);
            
            if (!storeResult.isSuccess()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Failed to store in KV store: " + storeResult.getError().orElse("Unknown error"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
            
            // Index in Lucene
            indexManager.indexDocument(indexName, document);
            indexManager.commit(indexName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Document indexed and stored");
            response.put("indexName", indexName);
            response.put("documentId", docId);
            response.put("embeddingGenerated", embeddingAdded);
            response.put("storedInKV", true);
            response.put("indexedInLucene", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error indexing with KV store", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/index/batch/withkv")
    @Operation(
            summary = "Batch index to both Lucene and KV store",
            description = "Indexes documents into Lucene and stores full JSON in KV store using batch operations with optional enrichment and embedding generation"
    )
    @ApiResponse(responseCode = "200", description = "Documents indexed and stored successfully")
    public ResponseEntity<Map<String, Object>> batchIndexWithKVStore(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "Generate embeddings if service available")
            @RequestParam(defaultValue = "true") boolean generateEmbedding,
            @Parameter(description = "Enrich documents before storing in KV store")
            @RequestParam(defaultValue = "false") boolean enrichBeforeStore,
            @Parameter(description = "Array of JVS documents")
            @RequestBody List<String> jsonDocuments) {
        try {
            if (documentStore == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "KV store not configured");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            List<JVS> documents = new ArrayList<>();
            List<float[]> embeddings = new ArrayList<>();  // Store embeddings separately
            Map<byte[], byte[]> kvBatch = new HashMap<>();
            List<String> documentIds = new ArrayList<>();
            int embeddingsGenerated = 0;
            
            // Create enrichment mapper if needed
            JVS2JVSEnrichMapper enrichMapper = enrichBeforeStore ? new JVS2JVSEnrichMapper() : null;
            if (enrichBeforeStore) {
                logger.info("Enriching {} documents before storing in KV store", jsonDocuments.size());
            }
            
            IndexManager.IndexHandle handle = indexManager.getIndex(indexName);
            if (handle == null) {
                throw new IllegalArgumentException("Index '" + indexName + "' does not exist");
            }
            
            for (String jsonDoc : jsonDocuments) {
                JVS document = JVS.read(jsonDoc);
                
                // Check if JVS has a valid type - skip enrichment if not
                if (document.getType() == null) {
                    logger.warn("Document has null type, skipping enrichment. JSON: {}", 
                               jsonDoc.length() > 200 ? jsonDoc.substring(0, 200) + "..." : jsonDoc);
                }
                
                // Generate embedding OUT-OF-BAND (not in document JSON)
                float[] embedding = null;
                if (generateEmbedding && embeddingService != null && embeddingService.isAvailable()) {
                    String text = embeddingService.extractTextForEmbedding(document);
                    logger.info("[batch/withkv] Doc {} extracted text length: {}", 
                               document.get("id.did"), text != null ? text.length() : "null");
                    if (text != null && !text.isEmpty()) {
                        embedding = embeddingService.generateEmbedding(text);
                        logger.info("[batch/withkv] Doc {} generated embedding: dimension={}", 
                                   document.get("id.did"), embedding != null ? embedding.length : "null");
                        if (embedding != null) {
                            embeddingsGenerated++;
                        }
                    } else {
                        logger.warn("[batch/withkv] Doc {} - no text extracted for embedding", document.get("id.did"));
                    }
                } else {
                    logger.debug("[batch/withkv] Doc {} - embedding generation skipped (enabled={}, service={}, available={})",
                               document.get("id.did"), generateEmbedding, 
                               embeddingService != null, 
                               embeddingService != null ? embeddingService.isAvailable() : "N/A");
                }
                
                documents.add(document);
                embeddings.add(embedding);  // Store embedding separately (can be null)
                
                // Extract or generate document ID
                Object docIdObj = document.get("id.did");
                String docId;
                if (docIdObj != null) {
                    // Remove quotes if present
                    docId = docIdObj.toString().replaceAll("\"", "");
                } else {
                    docId = UUID.randomUUID().toString();
                }
                documentIds.add(docId);
                
                // Optionally enrich before storing - only if document has valid type
                JVS documentToStore = document;
                if (enrichBeforeStore && enrichMapper != null && document.getType() != null) {
                    documentToStore = enrichMapper.apply(document);
                    if (documentToStore == null) {
                        documentToStore = document; // Fallback
                    }
                } else if (enrichBeforeStore && document.getType() == null) {
                    logger.warn("Skipping enrichment for document without type: {}", docId);
                }
                
                // Prepare for batch KV store (enriched or original)
                byte[] key = docId.getBytes(StandardCharsets.UTF_8);
                String jsonToStore = objectMapper.writeValueAsString(documentToStore.getJsonNode());
                byte[] value = jsonToStore.getBytes(StandardCharsets.UTF_8);
                kvBatch.put(key, value);
            }
            
            // Batch store in KV store
            Result<Void> batchResult = documentStore.batchPut(kvBatch, false);
            if (!batchResult.isSuccess()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Failed to batch store in KV: " + batchResult.getError().orElse("Unknown error"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
            
            // Index in Lucene with out-of-band embeddings
            for (int i = 0; i < documents.size(); i++) {
                handle.getWriter().indexDocument(documents.get(i), embeddings.get(i));
            }
            indexManager.commit(indexName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("indexed", documents.size());
            response.put("embeddingsGenerated", embeddingsGenerated);
            response.put("indexName", indexName);
            response.put("documentIds", documentIds);
            response.put("storedInKV", true);
            response.put("indexedInLucene", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error batch indexing with KV store", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping(value = "/index/stream", consumes = "application/x-ndjson")
    @Operation(
            summary = "Index documents from NDJson stream",
            description = "Streams NDJson documents for indexing"
    )
    @ApiResponse(responseCode = "200", description = "Stream processed successfully")
    public ResponseEntity<Map<String, Object>> indexStream(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "NDJson stream of documents")
            @RequestBody String ndjsonStream) {
        try {
            java.io.InputStream inputStream = new java.io.ByteArrayInputStream(ndjsonStream.getBytes());
            
            IndexManager.IndexHandle handle = indexManager.getIndex(indexName);
            if (handle == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Index not found: " + indexName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            IndexerStream indexerStream = new IndexerStream(handle.getWriter(), 100, true);
            List<IndexerStream.IndexingResult> results = indexerStream.indexFromStream(inputStream)
                    .collectList()
                    .block();
            
            long indexed = results.stream().mapToLong(IndexerStream.IndexingResult::getSuccessCount).sum();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("indexed", indexed);
            response.put("indexName", indexName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error indexing stream", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/query")
    @Operation(
            summary = "Search indexed documents",
            description = "Performs a Lucene query against indexed documents with optional faceting"
    )
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid query syntax")
    public ResponseEntity<Map<String, Object>> search(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "Lucene query string (supports fielded search like title.mls:fox)")
            @RequestParam String query,
            
            @Parameter(description = "Maximum number of results to return")
            @RequestParam(defaultValue = "10") int maxResults,
            
            @Parameter(description = "Query language (ISO 639-1, defaults to 'en')")
            @RequestParam(defaultValue = "en") String lang,
            
            @Parameter(description = "Comma-separated list of facet fields")
            @RequestParam(required = false) String facets) {
        try {
            List<String> facetList = facets != null && !facets.isEmpty()
                    ? Arrays.asList(facets.split(","))
                    : null;

            SearchResult result = indexManager.search(indexName, query, 0, maxResults, facetList, lang);
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("indexName", indexName);
            response.put("totalHits", result.getTotalHits());
            
            // Convert JVS documents to maps for JSON serialization
            List<Object> docs = new ArrayList<>();
            for (JVS doc : result.getDocuments()) {
                docs.add(doc.getJsonNode());
            }
            response.put("documents", docs);
            
            if (result.hasFacets()) {
                Map<String, Object> facetData = new HashMap<>();
                for (Map.Entry<String, FacetResult> entry : result.getFacets().entrySet()) {
                    Map<String, Object> facetValues = new HashMap<>();
                    for (FacetResult.FacetValue fv : entry.getValue().getValues()) {
                        facetValues.put(fv.getValue(), fv.getCount());
                    }
                    facetData.put(entry.getKey(), facetValues);
                }
                response.put("facets", facetData);
            }
            
            return ResponseEntity.ok(response);
        } catch (ParseException e) {
            logger.error("Invalid query syntax", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Invalid query syntax: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error executing search", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/query/withkv")
    @Operation(
            summary = "Search with KV store enrichment",
            description = "Performs search and optionally fetches full documents from KV store in batches"
    )
    @ApiResponse(responseCode = "200", description = "Search completed with enrichment")
    @ApiResponse(responseCode = "400", description = "Invalid query syntax")
    public ResponseEntity<Map<String, Object>> searchWithKV(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "Lucene query string")
            @RequestParam String query,
            
            @Parameter(description = "Maximum number of results to return")
            @RequestParam(defaultValue = "10") int maxResults,
            
            @Parameter(description = "Query language (ISO 639-1, defaults to 'en')")
            @RequestParam(defaultValue = "en") String lang,
            
            @Parameter(description = "Fetch full documents from KV store")
            @RequestParam(defaultValue = "true") boolean fetchFromKV,
            
            @Parameter(description = "Batch size for KV store fetches")
            @RequestParam(defaultValue = "50") int batchSize,
            
            @Parameter(description = "Comma-separated list of facet fields")
            @RequestParam(required = false) String facets) {
        try {
            if (fetchFromKV && documentStore == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "KV store not configured, cannot fetch full documents");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            List<String> facetList = facets != null && !facets.isEmpty()
                    ? Arrays.asList(facets.split(","))
                    : null;
            
            SearchResult result = indexManager.search(indexName, query, 0, maxResults, facetList, lang);
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("indexName", indexName);
            response.put("totalHits", result.getTotalHits());
            response.put("fetchedFromKV", fetchFromKV);
            
            // Prepare documents - either from search or enriched from KV
            List<Object> docs = new ArrayList<>();
            
            if (fetchFromKV) {
                // Use SearchResultIterator with KV store enrichment
                logger.info("Fetching {} documents from KV store in batches of {}", 
                            result.getDocuments().size(), batchSize);
                
                SearchResultIterator iterator = new SearchResultIterator(
                        result.getDocuments(), 
                        documentStore, 
                        batchSize
                );
                
                AbstractIterator<JVS> enrichedDocs = iterator.iterator();
                int count = 0;
                while (enrichedDocs.hasNext()) {
                    JVS doc = enrichedDocs.next();
                    count++;
                    logger.info("Got document #{}: {}", count, (doc != null ? "not null" : "NULL"));
                    if (doc != null) {
                        docs.add(doc.getJsonNode());
                    }
                }
                enrichedDocs.close();
                logger.info("KV enrichment complete. Added {} documents out of {} fetched", docs.size(), count);
            } else {
                // Return search results as-is
                for (JVS doc : result.getDocuments()) {
                    docs.add(doc.getJsonNode());
                }
            }
            
            response.put("documents", docs);
            response.put("returned", docs.size());
            
            // Add facets if present
            if (result.hasFacets()) {
                Map<String, Object> facetData = new HashMap<>();
                for (Map.Entry<String, FacetResult> entry : result.getFacets().entrySet()) {
                    Map<String, Object> facetValues = new HashMap<>();
                    for (FacetResult.FacetValue fv : entry.getValue().getValues()) {
                        facetValues.put(fv.getValue(), fv.getCount());
                    }
                    facetData.put(entry.getKey(), facetValues);
                }
                response.put("facets", facetData);
            }
            
            return ResponseEntity.ok(response);
        } catch (ParseException e) {
            logger.error("Invalid query syntax", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Invalid query syntax: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error executing search with KV enrichment", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping(value = "/query/stream", produces = "application/x-ndjson")
    @Operation(
            summary = "Search with streaming NDJson response",
            description = "Returns search results as an NDJson stream"
    )
    @ApiResponse(responseCode = "200", description = "Stream started", 
                 content = @Content(mediaType = "application/x-ndjson"))
    public ResponseEntity<StreamingResponseBody> searchStream(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "Lucene query string")
            @RequestParam String query,
            
            @Parameter(description = "Maximum number of results")
            @RequestParam(defaultValue = "10") int maxResults,
            
            @Parameter(description = "Query language (ISO 639-1, defaults to 'en')")
            @RequestParam(defaultValue = "en") String lang,
            
            @Parameter(description = "Comma-separated list of facet fields")
            @RequestParam(required = false) String facets) {
        
        StreamingResponseBody stream = outputStream -> {
            try {
                List<String> facetList = facets != null && !facets.isEmpty()
                        ? Arrays.asList(facets.split(","))
                        : null;
                
                SearchResult result = indexManager.search(indexName, query, 0, maxResults, facetList, lang);
                String ndjson = SearchResponseStream.toNDJsonString(result);
                
                outputStream.write(ndjson.getBytes());
                outputStream.flush();
            } catch (Exception e) {
                logger.error("Error streaming search results", e);
                throw new IOException("Error streaming results", e);
            }
        };
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(stream);
    }
    
    @GetMapping("/facets")
    @Operation(
            summary = "Get available facet fields",
            description = "Returns a list of fields available for faceting based on indexed documents"
    )
    @ApiResponse(responseCode = "200", description = "Facet fields retrieved")
    public ResponseEntity<Map<String, Object>> getFacetFields() {
        try {
            // For now, return common facetable fields
            // In a real implementation, this could scan the index schema
            List<String> facetFields = Arrays.asList(
                    "type",
                    "id.domain",
                    "dates.created",
                    "dates.modified"
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("facetFields", facetFields);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting facet fields", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @DeleteMapping("/index")
    @Operation(
            summary = "Clear the index",
            description = "Deletes all documents from the index"
    )
    @ApiResponse(responseCode = "200", description = "Index cleared successfully")
    public ResponseEntity<Map<String, Object>> clearIndex(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName) {
        try {
            IndexManager.IndexHandle handle = indexManager.getIndex(indexName);
            if (handle == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Index not found: " + indexName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            handle.getWriter().deleteAll();
            indexManager.commit(indexName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Index cleared successfully");
            response.put("indexName", indexName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error clearing index", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/stats")
    @Operation(
            summary = "Get index statistics",
            description = "Returns statistics about the current index"
    )
    @ApiResponse(responseCode = "200", description = "Statistics retrieved")
    public ResponseEntity<Map<String, Object>> getStats(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            
            @Parameter(description = "Query language (ISO 639-1, defaults to 'en')")
            @RequestParam(defaultValue = "en") String lang) {
        try {
            // Use a simple query to get document count
            SearchResult countResult = indexManager.search(indexName, "*:*", 0, 1, null, lang);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("indexName", indexName);
            stats.put("numDocuments", countResult.getTotalHits());
            stats.put("indexPath", indexPath.resolve(indexName).toString());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting stats", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/fields")
    @Operation(
            summary = "List all indexed field names",
            description = "Returns all field names present in the Lucene index"
    )
    @ApiResponse(responseCode = "200", description = "Field names retrieved")
    public ResponseEntity<Map<String, Object>> getIndexedFields(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName) {
        try {
            IndexManager.IndexHandle handle = indexManager.getIndex(indexName);
            if (handle == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Index not found: " + indexName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            // Get the index reader and collect all field names
            org.apache.lucene.index.IndexReader reader = handle.getSearcher().getIndexSearcher().getIndexReader();
            Set<String> fieldNames = new HashSet<>();
            
            for (org.apache.lucene.index.LeafReaderContext context : reader.leaves()) {
                org.apache.lucene.index.LeafReader leafReader = context.reader();
                for (org.apache.lucene.index.FieldInfo fieldInfo : leafReader.getFieldInfos()) {
                    fieldNames.add(fieldInfo.name);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("indexName", indexName);
            response.put("fieldCount", fieldNames.size());
            response.put("fields", new ArrayList<>(fieldNames).stream().sorted().toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting field names", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/diagnostic")
    @Operation(
            summary = "Diagnostic information for type system integration",
            description = "Returns detailed diagnostic information about type system, field types, and execution builder"
    )
    @ApiResponse(responseCode = "200", description = "Diagnostic info retrieved")
    public ResponseEntity<Map<String, Object>> diagnostic() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. Check type system
            Type productType = JsonTypeSystem.getMe().getType("demo_product");
            result.put("type_exists", productType != null);
            
            if (productType != null) {
                result.put("type_name", productType.getName());
                
                // Check brand field
                com.hitorro.jsontypesystem.Field brandField = productType.getField("brand");
                result.put("brand_field_exists", brandField != null);
                
                if (brandField != null) {
                    result.put("brand_field_type", brandField.getType());
                    // Note: Field.getGroup() requires a group name parameter
                    // To properly debug groups, check in debugger with:
                    // Collection<Group> indexGroups = brandField.getGroup("index");
                    result.put("brand_field_class", brandField.getClass().getName());
                }
                
                // Check sku field
                com.hitorro.jsontypesystem.Field skuField = productType.getField("sku");
                result.put("sku_field_exists", skuField != null);
                if (skuField != null) {
                    result.put("sku_field_type", skuField.getType());
                }
                
                // Check price field  
                com.hitorro.jsontypesystem.Field priceField = productType.getField("price");
                result.put("price_field_exists", priceField != null);
                if (priceField != null) {
                    result.put("price_field_type", priceField.getType());
                }
            }
            
            // 2. Check field types configuration
            com.hitorro.index.config.LuceneFieldTypes lfts = com.hitorro.index.config.LuceneFieldTypes.getInstance();
            com.hitorro.index.config.LuceneFieldType identifierType = lfts.get("identifier");
            com.hitorro.index.config.LuceneFieldType textType = lfts.get("text");
            com.hitorro.index.config.LuceneFieldType longType = lfts.get("long");
            
            result.put("identifier_type_loaded", identifierType != null);
            result.put("text_type_loaded", textType != null);
            result.put("long_type_loaded", longType != null);
            
            if (identifierType != null) {
                Map<String, Object> identifierInfo = new HashMap<>();
                identifierInfo.put("indexed", identifierType.isIndexed());
                identifierInfo.put("stored", identifierType.isStored());
                identifierInfo.put("tokenized", identifierType.isTokenized());
                identifierInfo.put("indexType", identifierType.getIndexType());
                identifierInfo.put("docValues", identifierType.hasDocValues());
                result.put("identifier_type_details", identifierInfo);
            }
            
            // 3. Check execution builder
            if (productType != null) {
                try {
                    com.hitorro.util.core.events.cache.HashCache<Type, com.hitorro.jsontypesystem.executors.ExecutionBuilder> cache = 
                        Type.getExecBuilderCache("lucene", com.hitorro.index.indexer.LuceneExecutionBuilderMapper.me);
                    com.hitorro.jsontypesystem.executors.ExecutionBuilder builder = cache.get(productType);
                    result.put("execution_builder_created", builder != null);
                    
                    if (builder != null) {
                        result.put("execution_builder_class", builder.getClass().getName());
                        com.hitorro.jsontypesystem.executors.ExecutionNode root = builder.getExecutor();
                        result.put("execution_node_exists", root != null);
                    }
                } catch (Exception e) {
                    result.put("execution_builder_error", e.getMessage());
                    result.put("execution_builder_exception_class", e.getClass().getName());
                }
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error in diagnostic", e);
            result.put("diagnostic_error", e.getMessage());
            result.put("diagnostic_exception_class", e.getClass().getName());
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            result.put("stack_trace", sw.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    // ========== Multi-Index Operations ==========
    
    @PostMapping("/indexes")
    @Operation(
            summary = "Create a new index",
            description = "Creates a new named index with the specified type"
    )
    @ApiResponse(responseCode = "200", description = "Index created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid parameters")
    public ResponseEntity<Map<String, Object>> createIndex(
            @Parameter(description = "Name for the new index")
            @RequestParam String indexName,
            @Parameter(description = "Type name for the index (defaults to 'core_sysobject')")
            @RequestParam(defaultValue = "core_sysobject") String typeName) {
        try {
            // Check if index already exists
            if (indexManager.hasIndex(indexName)) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Index already exists: " + indexName);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Get type
            Type type = JsonTypeSystem.getMe().getType(typeName);
            if (type == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Type not found: " + typeName);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Create index config with embedding support if available
            IndexConfig.Builder configBuilder = IndexConfig.builder()
                    .filesystem(indexPath.resolve(indexName));
            
            // Add embedding config if EmbeddingService is available
            if (embeddingService != null && embeddingService.isAvailable()) {
                int dimension = embeddingService.getDimension();
                EmbeddingConfig embeddingConfig = EmbeddingConfig.builder()
                        .fieldName("_embedding")
                        .dimension(dimension)
                        .similarity(VectorSimilarity.COSINE)
                        .fieldType(EmbeddingFieldType.FLOAT_VECTOR)
                        .build();
                configBuilder.embeddings(embeddingConfig);
                logger.info("Creating index {} with embedding support (dimension: {})", indexName, dimension);
            } else {
                logger.info("Creating index {} without embedding support", indexName);
            }
            
            IndexConfig config = configBuilder.build();
            
            // Create the index
            indexManager.createIndex(indexName, config, type);
            indexManager.commit(indexName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Index created successfully");
            response.put("indexName", indexName);
            response.put("typeName", typeName);
            response.put("indexPath", indexPath.resolve(indexName).toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating index", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/indexes")
    @Operation(
            summary = "List all indexes",
            description = "Returns a list of all available index names"
    )
    @ApiResponse(responseCode = "200", description = "Index list retrieved")
    public ResponseEntity<Map<String, Object>> listIndexes() {
        try {
            Set<String> indexNames = indexManager.getIndexNames();
            
            Map<String, Object> response = new HashMap<>();
            response.put("indexes", new ArrayList<>(indexNames));
            response.put("count", indexNames.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error listing indexes", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/query/multi")
    @Operation(
            summary = "Search across multiple indexes",
            description = "Performs a Lucene query against multiple indexes simultaneously"
    )
    @ApiResponse(responseCode = "200", description = "Multi-index search completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid query syntax")
    public ResponseEntity<Map<String, Object>> searchMultiple(
            @Parameter(description = "Comma-separated list of index names to search")
            @RequestParam String indexes,
            @Parameter(description = "Lucene query string")
            @RequestParam String query,
            @Parameter(description = "Maximum number of results to return")
            @RequestParam(defaultValue = "10") int maxResults,
            
            @Parameter(description = "Query language (ISO 639-1, defaults to 'en')")
            @RequestParam(defaultValue = "en") String lang) {
        try {
            List<String> indexList = Arrays.asList(indexes.split(","));
            
            SearchResult result = indexManager.searchMultiple(indexList, query, 0, maxResults, lang);
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("indexes", indexList);
            response.put("totalHits", result.getTotalHits());
            
            // Convert JVS documents to maps for JSON serialization
            List<Object> docs = new ArrayList<>();
            for (JVS doc : result.getDocuments()) {
                docs.add(doc.getJsonNode());
            }
            response.put("documents", docs);
            
            return ResponseEntity.ok(response);
        } catch (ParseException e) {
            logger.error("Invalid query syntax", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Invalid query syntax: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error executing multi-index search", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    // ========== Embedding/Vector Search Operations ==========
    
    @PostMapping("/embedding")
    @Operation(
            summary = "Search by embedding vector (KNN/ANN)",
            description = "Performs vector similarity search using embeddings. Supports both exact KNN and approximate ANN search via HNSW."
    )
    @ApiResponse(responseCode = "200", description = "Vector search completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or embedding not configured")
    public ResponseEntity<Map<String, Object>> searchByEmbedding(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "Request body with vector and search parameters")
            @RequestBody Map<String, Object> requestBody) {
        try {
            // Extract parameters
            @SuppressWarnings("unchecked")
            List<Number> vectorList = (List<Number>) requestBody.get("vector");
            if (vectorList == null || vectorList.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Vector parameter is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Convert to float array
            float[] vector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                vector[i] = vectorList.get(i).floatValue();
            }
            
            int k = requestBody.containsKey("k") ? ((Number) requestBody.get("k")).intValue() : 10;
            
            // Build request
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryVector(vector)
                    .k(k)
                    .build();
            
            // Execute search
            SearchResult result = indexManager.searchByEmbedding(indexName, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("indexName", indexName);
            response.put("totalHits", result.getTotalHits());
            response.put("k", k);
            response.put("vectorDimension", vector.length);
            response.put("searchTimeMs", result.getSearchTimeMs());
            
            // Convert JVS documents to maps
            List<Object> docs = new ArrayList<>();
            for (JVS doc : result.getDocuments()) {
                docs.add(doc.getJsonNode());
            }
            response.put("documents", docs);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.error("Embedding search not configured", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error executing embedding search", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/semantic")
    @Operation(
            summary = "Semantic search using Ollama embeddings",
            description = "Performs text, semantic, or hybrid search by automatically generating embeddings from query text using Ollama."
    )
    @ApiResponse(responseCode = "200", description = "Semantic search completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "503", description = "Ollama not available or embeddings not enabled")
    public ResponseEntity<Map<String, Object>> searchSemantic(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            
            @Parameter(description = "Query language (ISO 639-1, defaults to 'en')")
            @RequestParam(defaultValue = "en") String lang,
            
            @Parameter(description = "Request body with query text and search parameters")
            @RequestBody Map<String, Object> requestBody) {
        try {
            // Check if embedding service is available
            if (embeddingService == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Embedding service not enabled. Set embedding.enabled=true in application.yml");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            // Extract query text
            String queryText = (String) requestBody.get("query");
            if (queryText == null || queryText.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "query parameter is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Extract search mode: TEXT_ONLY, SEMANTIC_ONLY, or HYBRID (default)
            String modeStr = requestBody.containsKey("mode") ? 
                    (String) requestBody.get("mode") : "HYBRID";
            
            int k = requestBody.containsKey("k") ? 
                    ((Number) requestBody.get("k")).intValue() : 10;
            
            // For hybrid search
            String strategyStr = requestBody.containsKey("strategy") ? 
                    (String) requestBody.get("strategy") : "RERANK_RRF";
            double alpha = requestBody.containsKey("alpha") ? 
                    ((Number) requestBody.get("alpha")).doubleValue() : 0.5;
            
            // Check Ollama availability for semantic modes
            if (!modeStr.equals("TEXT_ONLY")) {
                if (!embeddingService.isAvailable()) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("status", "error");
                    error.put("message", "Ollama is not available. Check that Ollama is running at the configured URL.");
                    error.put("ollamaRequired", true);
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
                }
            }
            
            SearchResult result;
            Map<String, Object> response = new HashMap<>();
            
            switch (modeStr) {
                case "TEXT_ONLY":
                    // Traditional text search only
                    result = indexManager.search(indexName, queryText, 0, k, null, lang);
                    response.put("searchMode", "text");
                    break;
                    
                case "SEMANTIC_ONLY":
                    // Pure vector search
                    float[] queryEmbedding = embeddingService.generateEmbedding(queryText);
                    if (queryEmbedding == null) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("status", "error");
                        error.put("message", "Failed to generate embedding from query text");
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                    }
                    
                    EmbeddingSearchRequest embeddingRequest = EmbeddingSearchRequest.builder()
                            .queryVector(queryEmbedding)
                            .k(k)
                            .build();
                    
                    result = indexManager.searchByEmbedding(indexName, embeddingRequest);
                    response.put("searchMode", "semantic");
                    response.put("vectorDimension", queryEmbedding.length);
                    break;
                    
                case "HYBRID":
                default:
                    // Hybrid: combine text and vector search
                    float[] hybridEmbedding = embeddingService.generateEmbedding(queryText);
                    if (hybridEmbedding == null) {
                        Map<String, Object> error = new HashMap<>();
                        error.put("status", "error");
                        error.put("message", "Failed to generate embedding for hybrid search");
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
                    }
                    
                    HybridSearchRequest.CombinationStrategy strategy;
                    try {
                        strategy = HybridSearchRequest.CombinationStrategy.valueOf(strategyStr);
                    } catch (IllegalArgumentException e) {
                        strategy = HybridSearchRequest.CombinationStrategy.RERANK_RRF;
                    }
                    
                    HybridSearchRequest hybridRequest = HybridSearchRequest.builder()
                            .textQuery(queryText)
                            .queryVector(hybridEmbedding)
                            .k(k)
                            .strategy(strategy)
                            .alpha(alpha)
                            .build();
                    
                    result = indexManager.searchHybrid(indexName, hybridRequest);
                    response.put("searchMode", "hybrid");
                    response.put("strategy", strategy.toString());
                    response.put("alpha", alpha);
                    response.put("vectorDimension", hybridEmbedding.length);
                    break;
            }
            
            // Build response
            response.put("status", "success");
            response.put("indexName", indexName);
            response.put("query", queryText);
            response.put("k", k);
            response.put("totalHits", result.getTotalHits());
            response.put("searchTimeMs", result.getSearchTimeMs());
            
            // Convert JVS documents to maps
            List<Object> docs = new ArrayList<>();
            for (JVS doc : result.getDocuments()) {
                docs.add(doc.getJsonNode());
            }
            response.put("documents", docs);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            logger.error("Semantic search not configured", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error executing semantic search", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/hybrid")
    @Operation(
            summary = "Hybrid search combining text and vector search (with pre-computed embeddings)",
            description = "Performs both traditional text search and vector similarity search using provided embeddings, then merges results using the specified strategy."
    )
    @ApiResponse(responseCode = "200", description = "Hybrid search completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or embedding not configured")
    public ResponseEntity<Map<String, Object>> searchHybrid(
            @Parameter(description = "Index name (defaults to 'default')")
            @RequestParam(defaultValue = "default") String indexName,
            @Parameter(description = "Request body with text query, vector, and merge strategy")
            @RequestBody Map<String, Object> requestBody) {
        try {
            // Extract text query
            String textQuery = (String) requestBody.get("textQuery");
            if (textQuery == null || textQuery.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "textQuery parameter is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Extract vector
            @SuppressWarnings("unchecked")
            List<Number> vectorList = (List<Number>) requestBody.get("vector");
            if (vectorList == null || vectorList.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "vector parameter is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Convert to float array
            float[] vector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                vector[i] = vectorList.get(i).floatValue();
            }
            
            // Extract other parameters
            int k = requestBody.containsKey("k") ? ((Number) requestBody.get("k")).intValue() : 10;
            
            String strategyStr = requestBody.containsKey("strategy") ? 
                    (String) requestBody.get("strategy") : "RERANK_RRF";
            HybridSearchRequest.CombinationStrategy strategy = 
                    HybridSearchRequest.CombinationStrategy.valueOf(strategyStr);
            
            double alpha = requestBody.containsKey("alpha") ? 
                    ((Number) requestBody.get("alpha")).doubleValue() : 0.5;
            
            // Build request
            HybridSearchRequest request = HybridSearchRequest.builder()
                    .textQuery(textQuery)
                    .queryVector(vector)
                    .k(k)
                    .strategy(strategy)
                    .alpha(alpha)
                    .build();
            
            // Execute search
            SearchResult result = indexManager.searchHybrid(indexName, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("indexName", indexName);
            response.put("textQuery", textQuery);
            response.put("strategy", strategy.toString());
            response.put("alpha", alpha);
            response.put("k", k);
            response.put("vectorDimension", vector.length);
            response.put("totalHits", result.getTotalHits());
            response.put("searchTimeMs", result.getSearchTimeMs());
            
            // Convert JVS documents to maps
            List<Object> docs = new ArrayList<>();
            for (JVS doc : result.getDocuments()) {
                docs.add(doc.getJsonNode());
            }
            response.put("documents", docs);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.error("Invalid hybrid search request", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error executing hybrid search", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
