package com.hitorro.example.controller;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.kvstore.KVStore;
import com.hitorro.kvstore.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST Controller for direct KV store operations.
 * Provides access to the RocksDB-based document store.
 */
@RestController
@RequestMapping("/api/kvstore")
@Tag(name = "KV Store", description = "Direct operations on the RocksDB key-value store")
public class KVStoreController {
    
    private static final Logger logger = LoggerFactory.getLogger(KVStoreController.class);
    
    @Autowired(required = false)
    @Qualifier("documentStore")
    private KVStore documentStore;
    
    @GetMapping("/{key}")
    @Operation(
            summary = "Get document by key",
            description = "Retrieves a document from the KV store by its key"
    )
    @ApiResponse(responseCode = "200", description = "Document retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Document not found")
    @ApiResponse(responseCode = "503", description = "KV store not available")
    public ResponseEntity<Map<String, Object>> get(
            @Parameter(description = "Document key/ID")
            @PathVariable String key) {
        try {
            if (documentStore == null) {
                return unavailableResponse();
            }
            
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            Result<byte[]> result = documentStore.get(keyBytes);
            
            if (!result.isSuccess()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", result.getError().orElse("Unknown error"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
            
            Optional<byte[]> valueOpt = result.getValue();
            if (valueOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "not_found");
                error.put("key", key);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            byte[] value = valueOpt.get();
            if (value == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "not_found");
                error.put("key", key);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            // Parse JSON and return
            String json = new String(value, StandardCharsets.UTF_8);
            JVS document = JVS.read(json);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("key", key);
            response.put("document", document.getJsonNode());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting document from KV store", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/{key}")
    @Operation(
            summary = "Put document",
            description = "Stores a document in the KV store"
    )
    @ApiResponse(responseCode = "200", description = "Document stored successfully")
    public ResponseEntity<Map<String, Object>> put(
            @Parameter(description = "Document key/ID")
            @PathVariable String key,
            @Parameter(description = "JSON document")
            @RequestBody String jsonDocument) {
        try {
            if (documentStore == null) {
                return unavailableResponse();
            }
            
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] valueBytes = jsonDocument.getBytes(StandardCharsets.UTF_8);
            
            Result<Void> result = documentStore.put(keyBytes, valueBytes);
            
            if (!result.isSuccess()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", result.getError().orElse("Unknown error"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("key", key);
            response.put("size", valueBytes.length);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error putting document to KV store", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @DeleteMapping("/{key}")
    @Operation(
            summary = "Delete document",
            description = "Deletes a document from the KV store"
    )
    @ApiResponse(responseCode = "200", description = "Document deleted successfully")
    public ResponseEntity<Map<String, Object>> delete(
            @Parameter(description = "Document key/ID")
            @PathVariable String key) {
        try {
            if (documentStore == null) {
                return unavailableResponse();
            }
            
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            Result<Void> result = documentStore.delete(keyBytes);
            
            if (!result.isSuccess()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", result.getError().orElse("Unknown error"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("key", key);
            response.put("deleted", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting document from KV store", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/batch")
    @Operation(
            summary = "Batch put documents",
            description = "Stores multiple documents in the KV store using batch operation"
    )
    @ApiResponse(responseCode = "200", description = "Documents stored successfully")
    public ResponseEntity<Map<String, Object>> batchPut(
            @Parameter(description = "Map of key-value pairs (key -> JSON document)")
            @RequestBody Map<String, String> documents) {
        try {
            if (documentStore == null) {
                return unavailableResponse();
            }
            
            Map<byte[], byte[]> batch = new HashMap<>();
            for (Map.Entry<String, String> entry : documents.entrySet()) {
                byte[] key = entry.getKey().getBytes(StandardCharsets.UTF_8);
                byte[] value = entry.getValue().getBytes(StandardCharsets.UTF_8);
                batch.put(key, value);
            }
            
            Result<Void> result = documentStore.batchPut(batch, false);
            
            if (!result.isSuccess()) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", result.getError().orElse("Unknown error"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", documents.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error batch putting to KV store", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/stats")
    @Operation(
            summary = "Get KV store statistics",
            description = "Returns statistics about the KV store"
    )
    @ApiResponse(responseCode = "200", description = "Statistics retrieved")
    public ResponseEntity<Map<String, Object>> stats() {
        try {
            if (documentStore == null) {
                return unavailableResponse();
            }
            
            // Get basic stats
            Map<String, Object> stats = new HashMap<>();
            stats.put("status", "available");
            stats.put("type", "RocksDB");
            stats.put("isOpen", documentStore.isOpen());
            stats.put("latestSequenceNumber", documentStore.getLatestSequenceNumber());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting KV store stats", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    private ResponseEntity<Map<String, Object>> unavailableResponse() {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "unavailable");
        error.put("message", "KV store is not configured or initialized");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
}
