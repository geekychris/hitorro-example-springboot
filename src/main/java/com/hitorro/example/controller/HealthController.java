package com.hitorro.example.controller;

import com.hitorro.example.service.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoints for external services.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Health check endpoints for external services")
public class HealthController {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    @Autowired(required = false)
    private EmbeddingService embeddingService;
    
    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String embeddingModel;
    
    @Value("${embedding.enabled:false}")
    private boolean embeddingEnabled;
    
    /**
     * Check Ollama health and availability.
     */
    @GetMapping("/ollama")
    @Operation(summary = "Check Ollama Health", 
               description = "Check if Ollama is running and available for embedding generation")
    public ResponseEntity<Map<String, Object>> checkOllamaHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!embeddingEnabled) {
                response.put("status", "disabled");
                response.put("message", "Embedding support is disabled in configuration");
                response.put("enabled", false);
                return ResponseEntity.ok(response);
            }
            
            if (embeddingService == null) {
                response.put("status", "unavailable");
                response.put("message", "EmbeddingService bean not initialized");
                response.put("enabled", true);
                response.put("available", false);
                return ResponseEntity.ok(response);
            }
            
            // Check if Ollama is available
            boolean available = embeddingService.isAvailable();
            
            response.put("enabled", true);
            response.put("available", available);
            response.put("baseUrl", ollamaBaseUrl);
            response.put("model", embeddingModel);
            
            if (available) {
                response.put("status", "healthy");
                response.put("message", "Ollama is running and available");
                response.put("failureCount", embeddingService.getFailureCount());
                response.put("embeddingFields", embeddingService.getEmbeddingFields());
                
                // Try to get dimension
                try {
                    int dimension = embeddingService.getDimension();
                    response.put("dimension", dimension);
                } catch (Exception e) {
                    logger.warn("Could not get embedding dimension: {}", e.getMessage());
                }
                
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "unhealthy");
                response.put("message", "Ollama is not responding");
                response.put("failureCount", embeddingService.getFailureCount());
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error checking Ollama health", e);
            response.put("status", "error");
            response.put("message", "Error checking Ollama health: " + e.getMessage());
            response.put("enabled", embeddingEnabled);
            response.put("available", false);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Reset Ollama failure count (manual recovery).
     */
    @PostMapping("/ollama/reset")
    @Operation(summary = "Reset Ollama Failure Count",
               description = "Reset the Ollama failure counter to re-enable connection attempts")
    public ResponseEntity<Map<String, Object>> resetOllamaFailures() {
        Map<String, Object> response = new HashMap<>();
        
        if (embeddingService == null) {
            response.put("status", "error");
            response.put("message", "EmbeddingService not available");
            return ResponseEntity.badRequest().body(response);
        }
        
        embeddingService.resetFailureCount();
        response.put("status", "success");
        response.put("message", "Failure count reset");
        response.put("failureCount", 0);
        
        return ResponseEntity.ok(response);
    }
}
