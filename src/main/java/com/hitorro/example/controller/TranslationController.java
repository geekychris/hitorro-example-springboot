/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitorro.example.service.MLSTranslationService;
import com.hitorro.example.service.MLSTranslationService.TranslationResult;
import com.hitorro.jsontypesystem.JVS;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for MLS (Multi-Language String) translation operations.
 * Uses AI/LLM to translate text fields in JVS objects to multiple languages.
 */
@RestController
@RequestMapping("/api/jvs")
@Tag(name = "Translation", description = "MLS field translation using AI/LLM")
public class TranslationController {
    
    private static final Logger logger = LoggerFactory.getLogger(TranslationController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final MLSTranslationService translationService;
    
    public TranslationController(MLSTranslationService translationService) {
        this.translationService = translationService;
    }
    
    /**
     * Translate MLS fields in a JVS object to multiple languages
     */
    @PostMapping("/translate")
    @Operation(
        summary = "Translate MLS fields",
        description = "Translates specified MLS (Multi-Language String) fields in a JVS object " +
                      "from a source language to one or more target languages using AI/LLM. " +
                      "Translations are added to the existing MLS array structure."
    )
    public ResponseEntity<TranslateResponse> translateJVS(
            @RequestBody @Parameter(description = "Translation request") TranslateRequest request) {
        
        logger.info("Translation request: {} fields from {} to {}", 
                   request.getMlsFields().size(), 
                   request.getSourceLanguage(), 
                   request.getTargetLanguages());
        
        try {
            // Validate request
            if (request.getJson() == null || request.getJson().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new TranslateResponse().withError("JSON input is required")
                );
            }
            
            if (request.getMlsFields() == null || request.getMlsFields().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new TranslateResponse().withError("At least one MLS field path is required")
                );
            }
            
            if (request.getTargetLanguages() == null || request.getTargetLanguages().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new TranslateResponse().withError("At least one target language is required")
                );
            }
            
            // Check if AI service is available
            if (!translationService.isAvailable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    new TranslateResponse().withError(
                        "AI service not available. Ensure hitorro.ai.enabled=true and Ollama is running."
                    )
                );
            }
            
            // Parse JSON to JVS
            JsonNode jsonNode = objectMapper.readTree(request.getJson());
            JVS jvs = new JVS(jsonNode);
            
            // Default source language to English
            String sourceLanguage = request.getSourceLanguage() != null 
                ? request.getSourceLanguage() 
                : "en";
            
            // Perform translation
            long startTime = System.currentTimeMillis();
            TranslationResult result = translationService.translateMLSFields(
                jvs,
                request.getMlsFields(),
                sourceLanguage,
                request.getTargetLanguages()
            );
            long duration = System.currentTimeMillis() - startTime;
            
            // Build response
            TranslateResponse response = new TranslateResponse();
            response.setSuccess(result.isSuccess());
            response.setOriginal(objectMapper.convertValue(jsonNode, Map.class));
            response.setTranslated(objectMapper.convertValue(result.getTranslatedJson(), Map.class));
            response.setSourceLanguage(sourceLanguage);
            response.setTargetLanguages(request.getTargetLanguages());
            response.setFieldTranslations(result.getFieldTranslations());
            response.setTranslationTimeMs(duration);
            
            logger.info("Translation completed in {}ms, success={}", duration, result.isSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Translation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new TranslateResponse().withError("Translation failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Get list of supported languages
     */
    @GetMapping("/translate/languages")
    @Operation(
        summary = "List supported languages",
        description = "Returns a map of ISO 639-1 language codes to language names for all supported languages"
    )
    public ResponseEntity<Map<String, String>> getSupportedLanguages() {
        return ResponseEntity.ok(translationService.getSupportedLanguages());
    }
    
    /**
     * Check if translation service is available
     */
    @GetMapping("/translate/status")
    @Operation(
        summary = "Check translation service status",
        description = "Returns whether the AI translation service is available"
    )
    public ResponseEntity<Map<String, Object>> getTranslationStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("available", translationService.isAvailable());
        status.put("supportedLanguages", translationService.getSupportedLanguages().keySet());
        return ResponseEntity.ok(status);
    }
    
    // DTO Classes
    
    public static class TranslateRequest {
        private String json;
        private List<String> mlsFields;
        private String sourceLanguage;
        private List<String> targetLanguages;
        
        public String getJson() { return json; }
        public void setJson(String json) { this.json = json; }
        
        public List<String> getMlsFields() { return mlsFields; }
        public void setMlsFields(List<String> mlsFields) { this.mlsFields = mlsFields; }
        
        public String getSourceLanguage() { return sourceLanguage; }
        public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
        
        public List<String> getTargetLanguages() { return targetLanguages; }
        public void setTargetLanguages(List<String> targetLanguages) { this.targetLanguages = targetLanguages; }
    }
    
    public static class TranslateResponse {
        private boolean success;
        private Map<String, Object> original;
        private Map<String, Object> translated;
        private String sourceLanguage;
        private List<String> targetLanguages;
        private List<MLSTranslationService.FieldTranslation> fieldTranslations;
        private long translationTimeMs;
        private String error;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public Map<String, Object> getOriginal() { return original; }
        public void setOriginal(Map<String, Object> original) { this.original = original; }
        
        public Map<String, Object> getTranslated() { return translated; }
        public void setTranslated(Map<String, Object> translated) { this.translated = translated; }
        
        public String getSourceLanguage() { return sourceLanguage; }
        public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
        
        public List<String> getTargetLanguages() { return targetLanguages; }
        public void setTargetLanguages(List<String> targetLanguages) { this.targetLanguages = targetLanguages; }
        
        public List<MLSTranslationService.FieldTranslation> getFieldTranslations() { return fieldTranslations; }
        public void setFieldTranslations(List<MLSTranslationService.FieldTranslation> fieldTranslations) { 
            this.fieldTranslations = fieldTranslations; 
        }
        
        public long getTranslationTimeMs() { return translationTimeMs; }
        public void setTranslationTimeMs(long translationTimeMs) { this.translationTimeMs = translationTimeMs; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public TranslateResponse withError(String error) {
            this.error = error;
            this.success = false;
            return this;
        }
    }
}
