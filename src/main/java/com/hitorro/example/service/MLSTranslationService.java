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
package com.hitorro.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitorro.basedms.transformer.ai.AIService;
import com.hitorro.basedms.transformer.ai.AIServiceRegistry;
import com.hitorro.jsontypesystem.JVS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for translating MLS (Multi-Language String) fields in JVS objects.
 * Uses the AIService abstraction for LLM-based translation.
 */
@Service
public class MLSTranslationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MLSTranslationService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Supported language codes with their display names
     */
    public static Map<String, String> SUPPORTED_LANGUAGES = new LinkedHashMap<>();
    static {
        SUPPORTED_LANGUAGES.put("en", "English");
        SUPPORTED_LANGUAGES.put("de", "German");
        SUPPORTED_LANGUAGES.put("es", "Spanish");
        SUPPORTED_LANGUAGES.put("fr", "French");
        SUPPORTED_LANGUAGES.put("it", "Italian");
        SUPPORTED_LANGUAGES.put("pt", "Portuguese");
        SUPPORTED_LANGUAGES.put("ja", "Japanese");
        SUPPORTED_LANGUAGES.put("zh", "Chinese (Simplified)");
        SUPPORTED_LANGUAGES.put("ko", "Korean");
        SUPPORTED_LANGUAGES.put("ar", "Arabic");
        SUPPORTED_LANGUAGES.put("ru", "Russian");
        SUPPORTED_LANGUAGES.put("nl", "Dutch");
        SUPPORTED_LANGUAGES.put("pl", "Polish");
        SUPPORTED_LANGUAGES.put("sv", "Swedish");
    }
    
    /**
     * Check if the translation service is available
     */
    public boolean isAvailable() {
        return AIServiceRegistry.isAvailable();
    }
    
    /**
     * Translate MLS fields in a JVS object
     * 
     * @param jvs The JVS object to translate
     * @param mlsFieldPaths List of field paths to MLS fields (e.g., "title", "description")
     * @param sourceLanguage ISO 639-1 source language code (e.g., "en")
     * @param targetLanguages List of ISO 639-1 target language codes (e.g., ["de", "es", "ja"])
     * @return TranslationResult containing the translated JVS and metadata
     */
    public TranslationResult translateMLSFields(JVS jvs, List<String> mlsFieldPaths, 
                                                 String sourceLanguage, List<String> targetLanguages) {
        
        AIService aiService = AIServiceRegistry.getInstance();
        if (aiService == null) {
            throw new IllegalStateException("AI service not available. Ensure hitorro.ai.enabled=true and Ollama is running.");
        }
        
        logger.info("Translating {} MLS fields from {} to {}", 
                   mlsFieldPaths.size(), sourceLanguage, targetLanguages);
        
        TranslationResult result = new TranslationResult();
        result.setSourceLanguage(sourceLanguage);
        result.setTargetLanguages(targetLanguages);
        
        List<FieldTranslation> fieldTranslations = new ArrayList<>();
        
        for (String fieldPath : mlsFieldPaths) {
            try {
                FieldTranslation fieldTranslation = translateField(
                    jvs, fieldPath, sourceLanguage, targetLanguages, aiService
                );
                if (fieldTranslation != null) {
                    fieldTranslations.add(fieldTranslation);
                }
            } catch (Exception e) {
                logger.error("Error translating field '{}': {}", fieldPath, e.getMessage());
                FieldTranslation errorField = new FieldTranslation();
                errorField.setFieldPath(fieldPath);
                errorField.setError(e.getMessage());
                fieldTranslations.add(errorField);
            }
        }
        
        result.setTranslatedJson(jvs.getJsonNode());
        result.setFieldTranslations(fieldTranslations);
        result.setSuccess(fieldTranslations.stream().noneMatch(f -> f.getError() != null));
        
        return result;
    }
    
    /**
     * Translate a single MLS field using JVS's built-in MLS accessors
     */
    private FieldTranslation translateField(JVS jvs, String fieldPath,
                                            String sourceLanguage, List<String> targetLanguages,
                                            AIService aiService) {
        
        // Use JVS's MLS accessor: fieldPath.mls[lang].clean or .text
        String mlsPath = fieldPath + ".mls[" + sourceLanguage + "]";
        
        // Try to get clean text first, fall back to text
        String sourceText = jvs.getString(mlsPath + ".clean");
        if (sourceText == null || sourceText.trim().isEmpty()) {
            sourceText = jvs.getString(mlsPath + ".text");
        }
        
        if (sourceText == null || sourceText.trim().isEmpty()) {
            logger.warn("No source text found for field '{}' in language '{}'", fieldPath, sourceLanguage);
            return null;
        }
        
        logger.debug("Translating field '{}': '{}' ({} chars)", 
                    fieldPath, sourceText.substring(0, Math.min(50, sourceText.length())), sourceText.length());
        
        FieldTranslation fieldTranslation = new FieldTranslation();
        fieldTranslation.setFieldPath(fieldPath);
        fieldTranslation.setSourceText(sourceText);
        
        Map<String, String> translations = new LinkedHashMap<>();
        
        // Translate to each target language
        for (String targetLang : targetLanguages) {
            if (targetLang.equals(sourceLanguage)) {
                continue; // Skip if target is same as source
            }
            
            try {
                logger.debug("Translating to {}", targetLang);
                String translatedText = aiService.translate(sourceText, sourceLanguage, targetLang);
                translations.put(targetLang, translatedText);
                
                // Set translation using JVS's MLS accessor
                jvs.set(fieldPath + ".mls[" + targetLang + "].text", translatedText);
                
            } catch (Exception e) {
                logger.error("Failed to translate to {}: {}", targetLang, e.getMessage());
                translations.put(targetLang, "[Translation failed: " + e.getMessage() + "]");
            }
        }
        
        fieldTranslation.setTranslations(translations);
        return fieldTranslation;
    }
    
    /**
     * Get list of supported languages
     */
    public Map<String, String> getSupportedLanguages() {
        return Collections.unmodifiableMap(SUPPORTED_LANGUAGES);
    }
    
    // DTO Classes
    
    public static class TranslationResult {
        private JsonNode translatedJson;
        private String sourceLanguage;
        private List<String> targetLanguages;
        private List<FieldTranslation> fieldTranslations;
        private boolean success;
        
        public JsonNode getTranslatedJson() { return translatedJson; }
        public void setTranslatedJson(JsonNode translatedJson) { this.translatedJson = translatedJson; }
        
        public String getSourceLanguage() { return sourceLanguage; }
        public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
        
        public List<String> getTargetLanguages() { return targetLanguages; }
        public void setTargetLanguages(List<String> targetLanguages) { this.targetLanguages = targetLanguages; }
        
        public List<FieldTranslation> getFieldTranslations() { return fieldTranslations; }
        public void setFieldTranslations(List<FieldTranslation> fieldTranslations) { this.fieldTranslations = fieldTranslations; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
    
    public static class FieldTranslation {
        private String fieldPath;
        private String sourceText;
        private Map<String, String> translations;
        private String error;
        
        public String getFieldPath() { return fieldPath; }
        public void setFieldPath(String fieldPath) { this.fieldPath = fieldPath; }
        
        public String getSourceText() { return sourceText; }
        public void setSourceText(String sourceText) { this.sourceText = sourceText; }
        
        public Map<String, String> getTranslations() { return translations; }
        public void setTranslations(Map<String, String> translations) { this.translations = translations; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
