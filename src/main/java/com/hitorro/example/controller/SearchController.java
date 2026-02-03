package com.hitorro.example.controller;

import com.hitorro.index.config.IndexConfig;
import com.hitorro.index.indexer.JVSLuceneIndexWriter;
import com.hitorro.index.query.JVSQueryParser;
import com.hitorro.index.search.FacetResult;
import com.hitorro.index.search.JVSLuceneSearcher;
import com.hitorro.index.search.SearchResult;
import com.hitorro.index.stream.IndexerStream;
import com.hitorro.index.stream.SearchResponseStream;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.Type;
import com.hitorro.jsontypesystem.JsonTypeSystem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private JVSLuceneIndexWriter indexWriter;
    private JVSLuceneSearcher searcher;
    private Path indexPath;
    private IndexConfig config;
    private Type defaultType;
    
    @PostConstruct
    public void initializeIndex() {
        try {
            // Create index in temp directory for this example
            indexPath = Paths.get(System.getProperty("java.io.tmpdir"), "hitorro-lucene-index");
            logger.info("Initializing Lucene index at: {}", indexPath);
            
            // IndexConfig will automatically use FieldPatternAnalyzerWrapper
            // which selects analyzers based on field name suffixes (Solr-style)
            // Examples:
            //   *.text_en_s -> EnglishAnalyzer (with stemming, stop words)
            //   *.text_de_m -> GermanAnalyzer (with compound noun splitting, umlaut normalization)
            //   *.identifier_s -> KeywordAnalyzer (exact match, no tokenization)
            config = IndexConfig.builder()
                    .filesystem(indexPath)
                    .build();
            
            // Get default type for core_sysobject
            defaultType = JsonTypeSystem.getMe().getType("core_sysobject");
            
            // Create the index writer (creates index if doesn't exist)
            indexWriter = new JVSLuceneIndexWriter(config);
            
            // Commit to create an empty index
            indexWriter.commit();
            
            // Now create the searcher (index exists)
            searcher = new JVSLuceneSearcher(config, defaultType, "en");
            
            logger.info("Lucene index initialized successfully at: {}", indexPath);
        } catch (Exception e) {
            logger.error("Failed to initialize Lucene index", e);
            throw new RuntimeException("Failed to initialize search index", e);
        }
    }
    
    @PreDestroy
    public void closeIndex() {
        try {
            if (indexWriter != null) {
                indexWriter.close();
            }
            if (searcher != null) {
                searcher.close();
            }
            logger.info("Lucene index closed");
        } catch (Exception e) {
            logger.error("Error closing index", e);
        }
    }
    
    @PostMapping("/index")
    @Operation(
            summary = "Index a single document",
            description = "Indexes a single JVS document into the Lucene index"
    )
    @ApiResponse(responseCode = "200", description = "Document indexed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid document format")
    @ApiResponse(responseCode = "500", description = "Indexing error")
    public ResponseEntity<Map<String, Object>> indexDocument(
            @Parameter(description = "JVS document as JSON string")
            @RequestBody String jsonDocument) {
        try {
            JVS document = JVS.read(jsonDocument);
            indexWriter.indexDocument(document);
            indexWriter.commit();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Document indexed successfully");
            
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
            description = "Indexes a batch of JVS documents"
    )
    @ApiResponse(responseCode = "200", description = "Documents indexed successfully")
    public ResponseEntity<Map<String, Object>> indexBatch(
            @Parameter(description = "Array of JVS documents")
            @RequestBody List<String> jsonDocuments) {
        try {
            List<JVS> documents = new ArrayList<>();
            for (String jsonDoc : jsonDocuments) {
                documents.add(JVS.read(jsonDoc));
            }
            indexWriter.indexDocuments(documents);
            indexWriter.commit();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("indexed", documents.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error indexing batch", e);
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
            @Parameter(description = "NDJson stream of documents")
            @RequestBody String ndjsonStream) {
        try {
            java.io.InputStream inputStream = new java.io.ByteArrayInputStream(ndjsonStream.getBytes());
            
            IndexerStream indexerStream = new IndexerStream(indexWriter, 100, true);
            List<IndexerStream.IndexingResult> results = indexerStream.indexFromStream(inputStream)
                    .collectList()
                    .block();
            
            long indexed = results.stream().mapToLong(IndexerStream.IndexingResult::getSuccessCount).sum();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("indexed", indexed);
            
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
            @Parameter(description = "Lucene query string (supports fielded search like title.mls:fox)")
            @RequestParam String query,
            
            @Parameter(description = "Maximum number of results to return")
            @RequestParam(defaultValue = "10") int maxResults,
            
            @Parameter(description = "Comma-separated list of facet fields")
            @RequestParam(required = false) String facets) {
        try {
            searcher = searcher.refresh();
            
            List<String> facetList = facets != null && !facets.isEmpty()
                    ? Arrays.asList(facets.split(","))
                    : null;
            
            SearchResult result = searcher.search(query, 0, maxResults, facetList);
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
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
    
    @GetMapping(value = "/query/stream", produces = "application/x-ndjson")
    @Operation(
            summary = "Search with streaming NDJson response",
            description = "Returns search results as an NDJson stream"
    )
    @ApiResponse(responseCode = "200", description = "Stream started", 
                 content = @Content(mediaType = "application/x-ndjson"))
    public ResponseEntity<StreamingResponseBody> searchStream(
            @Parameter(description = "Lucene query string")
            @RequestParam String query,
            
            @Parameter(description = "Maximum number of results")
            @RequestParam(defaultValue = "10") int maxResults,
            
            @Parameter(description = "Comma-separated list of facet fields")
            @RequestParam(required = false) String facets) {
        
        StreamingResponseBody stream = outputStream -> {
            try {
                searcher = searcher.refresh();
                
                List<String> facetList = facets != null && !facets.isEmpty()
                        ? Arrays.asList(facets.split(","))
                        : null;
                
                SearchResult result = searcher.search(query, 0, maxResults, facetList);
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
    public ResponseEntity<Map<String, Object>> clearIndex() {
        try {
            indexWriter.deleteAll();
            indexWriter.commit();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Index cleared successfully");
            
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
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            searcher = searcher.refresh();
            // Use a simple query to get document count
            SearchResult countResult = searcher.search("*:*", 0, 1, null);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("numDocuments", countResult.getTotalHits());
            stats.put("indexPath", indexPath.toString());
            
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
    public ResponseEntity<Map<String, Object>> getIndexedFields() {
        try {
            searcher = searcher.refresh();
            
            // Get the index reader and collect all field names
            org.apache.lucene.index.IndexReader reader = searcher.getIndexSearcher().getIndexReader();
            Set<String> fieldNames = new HashSet<>();
            
            for (org.apache.lucene.index.LeafReaderContext context : reader.leaves()) {
                org.apache.lucene.index.LeafReader leafReader = context.reader();
                for (org.apache.lucene.index.FieldInfo fieldInfo : leafReader.getFieldInfos()) {
                    fieldNames.add(fieldInfo.name);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
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
}
