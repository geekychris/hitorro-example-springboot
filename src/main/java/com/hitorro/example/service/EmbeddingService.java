package com.hitorro.example.service;

import com.hitorro.jsontypesystem.JVS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for generating embeddings using Ollama.
 * Handles Ollama availability checks and embedding generation.
 */
@Service
@ConditionalOnProperty(prefix = "embedding", name = "enabled", havingValue = "true", matchIfMissing = false)
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final EmbeddingModel embeddingModel;
    private final List<String> embeddingFields;
    private final String fieldSeparator;
    private final boolean autoDetectDimension;
    private final int fallbackDimension;
    private final String ollamaBaseUrl;
    
    // Cache for dimension and availability
    private final AtomicReference<Integer> cachedDimension = new AtomicReference<>();
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private static final int MAX_FAILURES_BEFORE_DISABLE = 5;
    
    public EmbeddingService(
            EmbeddingModel embeddingModel,
            @Value("${embedding.fields:title,description}") String embeddingFieldsStr,
            @Value("${embedding.field-separator:. }") String fieldSeparator,
            @Value("${embedding.auto-detect-dimension:true}") boolean autoDetectDimension,
            @Value("${embedding.dimension:768}") int fallbackDimension,
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.embeddingModel = embeddingModel;
        // Parse comma-separated fields
        this.embeddingFields = Arrays.asList(embeddingFieldsStr.split(","));
        this.fieldSeparator = fieldSeparator;
        this.autoDetectDimension = autoDetectDimension;
        this.fallbackDimension = fallbackDimension;
        this.ollamaBaseUrl = ollamaBaseUrl;
        
        logger.info("EmbeddingService initialized with fields: {}, separator: '{}', dimension: {}", 
                    embeddingFields, fieldSeparator, autoDetectDimension ? "auto-detect" : fallbackDimension);
    }
    
    /**
     * Check if Ollama is available and responsive.
     */
    public boolean isAvailable() {
        if (failureCount.get() >= MAX_FAILURES_BEFORE_DISABLE) {
            logger.debug("Ollama marked as unavailable after {} consecutive failures", MAX_FAILURES_BEFORE_DISABLE);
            return false;
        }
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            String healthUrl = ollamaBaseUrl + "/api/tags";
            restTemplate.getForObject(healthUrl, String.class);
            failureCount.set(0);  // Reset failure count on success
            return true;
        } catch (Exception e) {
            int failures = failureCount.incrementAndGet();
            logger.warn("Ollama health check failed (attempt {}/{}): {}", 
                       failures, MAX_FAILURES_BEFORE_DISABLE, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get embedding dimension. Auto-detects on first call if enabled.
     */
    public int getDimension() {
        Integer cached = cachedDimension.get();
        if (cached != null) {
            return cached;
        }
        
        if (!autoDetectDimension) {
            cachedDimension.set(fallbackDimension);
            return fallbackDimension;
        }
        
        // Auto-detect by generating a test embedding
        try {
            float[] testEmbedding = generateEmbedding("test");
            if (testEmbedding != null && testEmbedding.length > 0) {
                int dimension = testEmbedding.length;
                cachedDimension.set(dimension);
                logger.info("Auto-detected embedding dimension: {}", dimension);
                return dimension;
            }
        } catch (Exception e) {
            logger.warn("Failed to auto-detect embedding dimension: {}", e.getMessage());
        }
        
        // Fallback
        cachedDimension.set(fallbackDimension);
        logger.info("Using fallback embedding dimension: {}", fallbackDimension);
        return fallbackDimension;
    }
    
    /**
     * Generate embedding for text.
     * Returns null if Ollama is unavailable or generation fails.
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            logger.debug("Empty text provided for embedding generation");
            return null;
        }
        
        if (!isAvailable()) {
            logger.debug("Ollama not available, skipping embedding generation");
            return null;
        }
        
        try {
            // Use embed() method which returns float[] directly
            float[] result = embeddingModel.embed(text);
            
            if (result != null && result.length > 0) {
                failureCount.set(0);  // Reset failure count on success
                logger.debug("Generated embedding with dimension: {}", result.length);
                return result;
            }
        } catch (Exception e) {
            int failures = failureCount.incrementAndGet();
            logger.error("Failed to generate embedding (attempt {}/{}): {}", 
                        failures, MAX_FAILURES_BEFORE_DISABLE, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract and combine configured fields from JVS document for embedding.
     */
    public String extractTextForEmbedding(JVS doc) {
        if (doc == null) {
            return null;
        }
        
        StringBuilder combined = new StringBuilder();
        
        for (String field : embeddingFields) {
            try {
                Object value = doc.get(field);
                if (value != null) {
                    String text = extractTextValue(value);
                    if (text != null && !text.trim().isEmpty()) {
                        if (combined.length() > 0) {
                            combined.append(fieldSeparator);
                        }
                        combined.append(text.trim());
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not extract field '{}' from document: {}", field, e.getMessage());
            }
        }
        
        String result = combined.toString().trim();
        logger.debug("Extracted text for embedding from fields {}: '{}'", embeddingFields, 
                    result.length() > 100 ? result.substring(0, 100) + "..." : result);
        return result.isEmpty() ? null : result;
    }
    
    /**
     * Extract text value from various object types.
     * Handles MLS (multilingual string) structures intelligently.
     */
    private String extractTextValue(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof String) {
            return (String) obj;
        }
        
        if (obj instanceof com.fasterxml.jackson.databind.JsonNode) {
            com.fasterxml.jackson.databind.JsonNode node = (com.fasterxml.jackson.databind.JsonNode) obj;
            
            if (node.isTextual()) {
                return node.asText();
            }
            
            // Handle MLS structures: { "mls": [ { "lang": "en", "text": "...", "clean": "..." } ] }
            if (node.isObject() && node.has("mls")) {
                return extractFromMLS(node.get("mls"));
            }
            
            // Handle array of MLS objects directly
            if (node.isArray()) {
                return extractFromMLS(node);
            }
            
            // For other complex nodes, try asText (may return empty string)
            String text = node.asText();
            return text.isEmpty() ? null : text;
        }
        
        return obj.toString();
    }
    
    /**
     * Extract text from MLS (multilingual string) array or object.
     * Prefers 'clean' field, falls back to 'text' field.
     * Combines all language variants.
     * 
     * Handles both formats:
     * - Array: {"mls": [{"lang": "en", "clean": "..."}]}
     * - Object: {"mls": {"clean": "...", "segmented": "..."}}
     */
    private String extractFromMLS(com.fasterxml.jackson.databind.JsonNode mlsNode) {
        if (mlsNode == null) {
            return null;
        }
        
        // If MLS is an object (not array), extract text directly from it
        if (mlsNode.isObject()) {
            String text = null;
            
            // Prefer 'clean' field (has clean text without extra formatting)
            if (mlsNode.has("clean")) {
                text = mlsNode.get("clean").asText();
            }
            // Fall back to 'text' field
            else if (mlsNode.has("text")) {
                text = mlsNode.get("text").asText();
            }
            // Fall back to 'segmented' if available
            else if (mlsNode.has("segmented")) {
                text = mlsNode.get("segmented").asText();
            }
            
            return (text != null && !text.trim().isEmpty()) ? text.trim() : null;
        }
        
        // Handle array format
        if (!mlsNode.isArray()) {
            return null;
        }
        
        StringBuilder combined = new StringBuilder();
        
        for (com.fasterxml.jackson.databind.JsonNode item : mlsNode) {
            if (item.isObject()) {
                String text = null;
                
                // Prefer 'clean' field (has clean text without extra formatting)
                if (item.has("clean")) {
                    text = item.get("clean").asText();
                }
                // Fall back to 'text' field
                else if (item.has("text")) {
                    text = item.get("text").asText();
                }
                // Fall back to 'segmented' if available
                else if (item.has("segmented")) {
                    text = item.get("segmented").asText();
                }
                
                if (text != null && !text.trim().isEmpty()) {
                    if (combined.length() > 0) {
                        combined.append(" ");
                    }
                    combined.append(text.trim());
                }
            }
        }
        
        String result = combined.toString().trim();
        return result.isEmpty() ? null : result;
    }
    
    /**
     * Generate embedding for a JVS document by extracting configured fields.
     * Returns null if extraction fails or Ollama is unavailable.
     */
    public float[] generateEmbeddingForDocument(JVS doc) {
        String text = extractTextForEmbedding(doc);
        if (text == null || text.trim().isEmpty()) {
            logger.debug("No text extracted from document for embedding");
            return null;
        }
        
        return generateEmbedding(text);
    }
    
    /**
     * Reset failure count (useful for health checks or manual recovery).
     */
    public void resetFailureCount() {
        failureCount.set(0);
        logger.info("Embedding service failure count reset");
    }
    
    /**
     * Get current failure count.
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Get configured embedding fields.
     */
    public List<String> getEmbeddingFields() {
        return embeddingFields;
    }
}
